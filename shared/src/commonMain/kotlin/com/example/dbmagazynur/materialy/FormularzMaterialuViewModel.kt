package com.example.dbmagazynur.materialy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dbmagazynur.data.komunikat
import com.example.dbmagazynur.data.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
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
        blad = null
        otwarty = true
    }

    fun zamknij() {
        if (!trwa) otwarty = false
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
                val wynik = if (material == null) {
                    supabase.postgrest
                        .rpc("dodaj_material", parametryNowegoMaterialu(dane, nrZamowienia))
                        .decodeAs<Material>()
                } else {
                    supabase.from("materialy").update(dane) {
                        filter { eq("id", material.id) }
                        select()
                    }.decodeSingleOrNull<Material>()
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
