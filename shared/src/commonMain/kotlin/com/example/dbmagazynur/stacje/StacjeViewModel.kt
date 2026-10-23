package com.example.dbmagazynur.stacje

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dbmagazynur.data.komunikat
import com.example.dbmagazynur.data.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

// Zarządzanie lokalizacjami funkcjonalnymi (poziom 2+, pilnuje RLS).
// Stacje przypisane do materiałów się wycofuje (aktywna = false), usunąć da się tylko nieużywane.
class StacjeViewModel : ViewModel() {
    var otwarta by mutableStateOf(false)
        private set
    // Wszystkie stacje; ekran pokazuje te z wybranego obszaru
    var lista by mutableStateOf<List<Stacja>?>(null)
        private set
    var obszar by mutableStateOf(OBSZARY.first())
        private set
    var wczytywanie by mutableStateOf(false)
        private set
    var blad by mutableStateOf<String?>(null)
        private set
    var komunikat by mutableStateOf<String?>(null)
        private set

    // Okienko dodawania / edycji
    var formularzOtwarty by mutableStateOf(false)
        private set
    // null = nowa stacja
    var edytowana by mutableStateOf<Stacja?>(null)
        private set
    var obszarStacji by mutableStateOf<String?>(null)
    var nazwa by mutableStateOf("")
    var kod by mutableStateOf("")
    var aktywna by mutableStateOf(true)
    var potwierdzUsuniecie by mutableStateOf(false)
    var trwa by mutableStateOf(false)
        private set
    var bladFormularza by mutableStateOf<String?>(null)
        private set

    private var wczytywanieJob: Job? = null

    fun otworz() {
        otwarta = true
        komunikat = null
        wczytaj()
    }

    fun zamknij() {
        if (trwa) return
        formularzOtwarty = false
        otwarta = false
    }

    fun wybierzObszar(wybrany: String) {
        obszar = wybrany
    }

    fun wczytaj() {
        val poprzednie = wczytywanieJob
        wczytywanieJob = viewModelScope.launch {
            poprzednie?.cancelAndJoin()
            wczytywanie = true
            blad = null
            try {
                lista = posortujStacje(supabase.from("stacje").select().decodeList<Stacja>())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                blad = e.komunikat()
            } finally {
                wczytywanie = false
            }
        }
    }

    fun komunikatPokazany() {
        komunikat = null
    }

    // Nowa stacja od razu w obszarze, który jest otwarty na liście
    fun dodaj() {
        wypelnij(null, obszar, "", "", true)
    }

    fun edytuj(stacja: Stacja) {
        wypelnij(stacja, stacja.obszar, stacja.nazwa, stacja.kod.orEmpty(), stacja.aktywna)
    }

    private fun wypelnij(stacja: Stacja?, obszar: String, nazwa: String, kod: String, aktywna: Boolean) {
        edytowana = stacja
        obszarStacji = obszar
        this.nazwa = nazwa
        this.kod = kod
        this.aktywna = aktywna
        potwierdzUsuniecie = false
        bladFormularza = null
        formularzOtwarty = true
    }

    fun zamknijFormularz() {
        if (!trwa) formularzOtwarty = false
    }

    fun zapisz() {
        if (trwa) return
        val stacja = edytowana
        val dane = daneStacji(obszarStacji, nazwa, kod, aktywna).getOrElse {
            bladFormularza = it.message
            return
        }
        wykonaj(if (stacja == null) "Dodano stację" else "Zapisano zmiany") {
            val tabela = supabase.from("stacje")
            val wynik = if (stacja == null) {
                tabela.insert(dane) { select() }
            } else {
                tabela.update(dane) {
                    filter { eq("id", stacja.id) }
                    select()
                }
            }.decodeList<Stacja>()
            // RLS po cichu pomija wiersze, do których nie ma uprawnień
            if (wynik.isEmpty()) throw IllegalStateException("Brak uprawnień albo stacja została usunięta")
            // Lista pokazuje obszar zapisanej stacji (mogła zostać przeniesiona)
            obszarStacji?.let { obszar = it }
        }
    }

    fun usun() {
        val stacja = edytowana ?: return
        if (trwa) return
        wykonaj("Usunięto stację") {
            val usuniete = supabase.from("stacje")
                .delete {
                    filter { eq("id", stacja.id) }
                    select()
                }
                .decodeList<Stacja>()
            if (usuniete.isEmpty()) throw IllegalStateException("Brak uprawnień albo stacja została już usunięta")
        }
    }

    private fun wykonaj(sukces: String, operacja: suspend () -> Unit) {
        viewModelScope.launch {
            trwa = true
            bladFormularza = null
            try {
                operacja()
                formularzOtwarty = false
                komunikat = sukces
                wczytaj()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                bladFormularza = e.komunikat()
                potwierdzUsuniecie = false
            } finally {
                trwa = false
            }
        }
    }
}
