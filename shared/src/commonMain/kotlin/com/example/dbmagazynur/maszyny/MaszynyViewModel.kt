package com.example.dbmagazynur.maszyny

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

// Zarządzanie listą maszyn (poziom 3, pilnuje RLS).
// Maszyny z historią wydań się wycofuje (aktywna = false), usunąć da się tylko nieużywane.
class MaszynyViewModel : ViewModel() {
    var otwarta by mutableStateOf(false)
        private set
    var lista by mutableStateOf<List<Maszyna>?>(null)
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
    // null = nowa maszyna
    var edytowana by mutableStateOf<Maszyna?>(null)
        private set
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

    fun wczytaj() {
        val poprzednie = wczytywanieJob
        wczytywanieJob = viewModelScope.launch {
            poprzednie?.cancelAndJoin()
            wczytywanie = true
            blad = null
            try {
                lista = posortujMaszyny(supabase.from("maszyny").select().decodeList<Maszyna>())
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

    fun dodaj() {
        wypelnij(null, "", "", true)
    }

    fun edytuj(maszyna: Maszyna) {
        wypelnij(maszyna, maszyna.nazwa, maszyna.kod.orEmpty(), maszyna.aktywna)
    }

    private fun wypelnij(maszyna: Maszyna?, nazwa: String, kod: String, aktywna: Boolean) {
        edytowana = maszyna
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
        val maszyna = edytowana
        val dane = daneMaszyny(nazwa, kod, aktywna).getOrElse {
            bladFormularza = it.message
            return
        }
        wykonaj(if (maszyna == null) "Dodano maszynę" else "Zapisano zmiany") {
            val tabela = supabase.from("maszyny")
            val wynik = if (maszyna == null) {
                tabela.insert(dane) { select() }
            } else {
                tabela.update(dane) {
                    filter { eq("id", maszyna.id) }
                    select()
                }
            }.decodeList<Maszyna>()
            // RLS po cichu pomija wiersze, do których nie ma uprawnień
            if (wynik.isEmpty()) throw IllegalStateException("Brak uprawnień albo maszyna została usunięta")
        }
    }

    fun usun() {
        val maszyna = edytowana ?: return
        if (trwa) return
        wykonaj("Usunięto maszynę") {
            val usuniete = supabase.from("maszyny")
                .delete {
                    filter { eq("id", maszyna.id) }
                    select()
                }
                .decodeList<Maszyna>()
            if (usuniete.isEmpty()) throw IllegalStateException("Brak uprawnień albo maszyna została już usunięta")
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
