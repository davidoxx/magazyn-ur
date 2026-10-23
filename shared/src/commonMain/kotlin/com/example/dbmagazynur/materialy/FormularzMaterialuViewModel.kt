package com.example.dbmagazynur.materialy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dbmagazynur.data.komunikat
import com.example.dbmagazynur.data.supabase
import com.example.dbmagazynur.stacje.Stacja
import com.example.dbmagazynur.stacje.posortujStacje
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class Zapisano(val material: Material, val nowy: Boolean)

// Dodawanie i edycja materiału (poziom 1+, pilnuje RLS)
class FormularzMaterialuViewModel : ViewModel() {
    var otwarty by mutableStateOf(false)
        private set
    // null = nowy materiał
    var edytowany by mutableStateOf<Material?>(null)
        private set

    // Pola formularza zmienia bezpośrednio ekran
    var nazwa by mutableStateOf("")
    var typ by mutableStateOf<TypMaterialu?>(null)
    var nrSap by mutableStateOf("")
    var miejsce by mutableStateOf("")
    var jednostka by mutableStateOf("")
    var iloscTekst by mutableStateOf("")
    var opis by mutableStateOf("")
    // Tylko przy dodawaniu (opcjonalny); trafia do historii jako część wpisu „utworzenie”
    var nrZamowienia by mutableStateOf("")
    // Lokalizacje funkcjonalne (stacje), do których materiał jest przeznaczony
    var stacje by mutableStateOf<List<Stacja>>(emptyList())
        private set

    // Wybór stacji: najpierw F1 / F2 / F3 (obszarWyboru = null), potem lista stacji z obszaru
    var wyborStacji by mutableStateOf(false)
        private set
    var obszarWyboru by mutableStateOf<String?>(null)
        private set
    // Aktywne stacje wybranego obszaru (null = wczytywanie)
    var dostepneStacje by mutableStateOf<List<Stacja>?>(null)
        private set
    var bladStacji by mutableStateOf<String?>(null)
        private set
    private var wczytywanieStacji: Job? = null

    var trwa by mutableStateOf(false)
        private set
    var blad by mutableStateOf<String?>(null)
        private set
    // Wynik zapisu dla ekranu nadrzędnego (otwiera kartę materiału)
    var zapisano by mutableStateOf<Zapisano?>(null)
        private set

    // Typ bez wartości domyślnej: pomyłka SAP / kosztowy jest kosztowna
    fun otworzNowy(nazwa: String = "") {
        wypelnij(null, nazwa, null, "", "", "szt", "")
    }

    fun otworzEdycje(material: Material) {
        wypelnij(
            material, material.nazwa, material.typ, material.nrSap.orEmpty(),
            material.miejsce.orEmpty(), material.jednostka, material.opis.orEmpty(),
        )
    }

    private fun wypelnij(
        material: Material?,
        nazwa: String,
        typ: TypMaterialu?,
        nrSap: String,
        miejsce: String,
        jednostka: String,
        opis: String,
    ) {
        edytowany = material
        this.nazwa = nazwa
        this.typ = typ
        this.nrSap = nrSap
        this.miejsce = miejsce
        this.jednostka = jednostka
        this.opis = opis
        iloscTekst = ""
        nrZamowienia = ""
        stacje = posortujStacje(material?.stacje.orEmpty())
        wyborStacji = false
        blad = null
        otwarty = true
    }

    fun zamknij() {
        if (!trwa) otwarty = false
    }

    fun pokazWyborStacji() {
        obszarWyboru = null
        dostepneStacje = null
        bladStacji = null
        wyborStacji = true
    }

    fun zamknijWyborStacji() {
        wczytywanieStacji?.cancel()
        wyborStacji = false
    }

    // „Wstecz” z listy stacji wraca do wyboru F1 / F2 / F3
    fun wsteczWyboruStacji() {
        if (obszarWyboru == null) {
            zamknijWyborStacji()
        } else {
            wczytywanieStacji?.cancel()
            obszarWyboru = null
        }
    }

    fun wybierzObszar(obszar: String) {
        obszarWyboru = obszar
        wczytajStacje()
    }

    // Lista wczytywana przy każdym wejściu, żeby było widać stacje dodane w trakcie zmiany
    fun wczytajStacje() {
        val obszar = obszarWyboru ?: return
        wczytywanieStacji?.cancel()
        wczytywanieStacji = viewModelScope.launch {
            dostepneStacje = null
            bladStacji = null
            try {
                dostepneStacje = posortujStacje(
                    supabase.from("stacje")
                        .select {
                            filter {
                                eq("obszar", obszar)
                                eq("aktywna", true)
                            }
                        }
                        .decodeList<Stacja>(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                bladStacji = e.komunikat()
            }
        }
    }

    // Zaznaczenie na liście dodaje stację, ponowne ją odznacza
    fun przelaczStacje(stacja: Stacja) {
        stacje = if (stacje.any { it.id == stacja.id }) {
            stacje.filterNot { it.id == stacja.id }
        } else {
            posortujStacje(stacje + stacja)
        }
    }

    fun usunStacje(stacja: Stacja) {
        stacje = stacje.filterNot { it.id == stacja.id }
    }

    fun zapisanoObsluzone() {
        zapisano = null
    }

    fun zapisz() {
        if (trwa) return
        val material = edytowany
        val dane = daneMaterialu(
            nazwa, typ, nrSap, miejsce, jednostka, opis,
            iloscTekst = if (material == null) iloscTekst else null,
        ).getOrElse {
            blad = it.message
            return
        }
        viewModelScope.launch {
            trwa = true
            blad = null
            try {
                val wybraneStacje = stacje.map { it.id }
                val wynik = if (material == null) {
                    supabase.postgrest
                        .rpc("dodaj_material", parametryNowegoMaterialu(dane, nrZamowienia, wybraneStacje))
                        .decodeAs<Material>()
                } else {
                    val zapisany = supabase.from("materialy").update(dane) {
                        filter { eq("id", material.id) }
                        select()
                    }.decodeSingleOrNull<Material>()
                    // Stacje osobnym wywołaniem i tylko po zmianie
                    val zmiany = zmianyStacji(material.id, material.stacje.map { it.id }, wybraneStacje)
                    if (zapisany != null && zmiany != null) supabase.postgrest.rpc("zmien_stacje_materialu", zmiany)
                    zapisany
                }

                if (wynik == null) {
                    blad = "Nie ma takiego materiału. Mógł zostać usunięty."
                } else {
                    otwarty = false
                    zapisano = Zapisano(wynik, nowy = material == null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                blad = e.komunikat()
            } finally {
                trwa = false
            }
        }
    }
}
