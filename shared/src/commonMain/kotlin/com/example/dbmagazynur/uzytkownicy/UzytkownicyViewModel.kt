package com.example.dbmagazynur.uzytkownicy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dbmagazynur.data.bladZFunkcji
import com.example.dbmagazynur.data.komunikat
import com.example.dbmagazynur.data.supabase
import com.example.dbmagazynur.sesja.emailNaLogin
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Zarządzanie kontami (poziom 3). Poziom zmienia RPC ustaw_poziom,
// a zakładanie kont i reset haseł robi Edge Function „uzytkownicy” z kluczem secret.
class UzytkownicyViewModel : ViewModel() {
    var otwarta by mutableStateOf(false)
        private set
    var lista by mutableStateOf<List<Uzytkownik>?>(null)
        private set
    var wczytywanie by mutableStateOf(false)
        private set
    var blad by mutableStateOf<String?>(null)
        private set
    var komunikat by mutableStateOf<String?>(null)
        private set

    // Okienko zakładania konta albo edycji wybranego użytkownika
    var noweKonto by mutableStateOf(false)
        private set
    var wybrany by mutableStateOf<Uzytkownik?>(null)
        private set
    var login by mutableStateOf("")
    var haslo by mutableStateOf("")
    var poziom by mutableIntStateOf(1)
    var trwa by mutableStateOf(false)
        private set
    var bladOkienka by mutableStateOf<String?>(null)
        private set

    private var wczytywanieJob: Job? = null

    fun otworz() {
        otwarta = true
        komunikat = null
        wczytaj()
    }

    fun zamknij() {
        if (trwa) return
        zamknijOkienko()
        otwarta = false
    }

    fun wczytaj() {
        val poprzednie = wczytywanieJob
        wczytywanieJob = viewModelScope.launch {
            poprzednie?.cancelAndJoin()
            wczytywanie = true
            blad = null
            try {
                lista = posortujUzytkownikow(supabase.from("uzytkownicy").select().decodeList<Uzytkownik>())
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
        wybrany = null
        login = ""
        haslo = ""
        poziom = 1
        bladOkienka = null
        noweKonto = true
    }

    fun wybierz(uzytkownik: Uzytkownik) {
        noweKonto = false
        haslo = ""
        poziom = uzytkownik.poziom
        bladOkienka = null
        wybrany = uzytkownik
    }

    // Hasło czyścimy, żeby nie zostało w pamięci po zamknięciu okienka
    fun zamknijOkienko() {
        if (trwa) return
        noweKonto = false
        wybrany = null
        haslo = ""
    }

    fun zalozKonto() {
        val nowyLogin = normalizujLogin(login).getOrElse {
            bladOkienka = it.message
            return
        }
        if (haslo.isEmpty()) {
            bladOkienka = "Wpisz hasło"
            return
        }
        val dane = buildJsonObject {
            put("akcja", "utworz")
            put("login", nowyLogin)
            put("haslo", haslo)
            put("poziom", poziom)
        }
        wykonaj("Założono konto $nowyLogin") { wywolajFunkcje(dane) }
    }

    fun zapiszPoziom() {
        val uzytkownik = wybrany ?: return
        val nowy = poziom
        wykonaj("${emailNaLogin(uzytkownik.nazwa)}: ${nazwaPoziomu(nowy)}") {
            // Baza pilnuje poziomu 3 i blokuje zmianę własnego poziomu
            supabase.postgrest.rpc(
                "ustaw_poziom",
                buildJsonObject {
                    put("p_uzytkownik", uzytkownik.id)
                    put("p_poziom", nowy)
                },
            )
        }
    }

    fun ustawHaslo() {
        val uzytkownik = wybrany ?: return
        if (haslo.isEmpty()) {
            bladOkienka = "Wpisz nowe hasło"
            return
        }
        val dane = buildJsonObject {
            put("akcja", "resetuj_haslo")
            put("uzytkownik_id", uzytkownik.id)
            put("haslo", haslo)
        }
        wykonaj("Ustawiono nowe hasło dla ${emailNaLogin(uzytkownik.nazwa)}") { wywolajFunkcje(dane) }
    }

    private suspend fun wywolajFunkcje(dane: JsonObject) {
        try {
            supabase.functions.invoke("uzytkownicy", body = dane)
        } catch (e: RestException) {
            // Komunikat z funkcji albo podpowiedź, gdy funkcja nie jest gotowa w Supabase
            throw IllegalStateException(
                bladZFunkcji(e.error) ?: when (e) {
                    is NotFoundRestException -> "Funkcja „uzytkownicy” nie jest wdrożona w Supabase"
                    is UnauthorizedRestException ->
                        "Supabase odrzucił token. W ustawieniach funkcji wyłącz weryfikację JWT, " +
                            "funkcja sprawdza token sama."
                    else -> e.error
                },
            )
        }
    }

    private fun wykonaj(sukces: String, operacja: suspend () -> Unit) {
        if (trwa) return
        viewModelScope.launch {
            trwa = true
            bladOkienka = null
            try {
                operacja()
                trwa = false
                zamknijOkienko()
                komunikat = sukces
                wczytaj()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                bladOkienka = e.komunikat()
            } finally {
                trwa = false
            }
        }
    }
}
