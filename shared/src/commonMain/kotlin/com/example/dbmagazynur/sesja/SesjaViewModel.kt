package com.example.dbmagazynur.sesja

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dbmagazynur.data.komunikat
import com.example.dbmagazynur.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface Sesja {
    // Supabase wczytuje zapisaną sesję albo sprawdzamy poziom konta
    data object Ladowanie : Sesja
    data object Wylogowany : Sesja
    data class Zalogowany(val userId: String?, val login: String, val poziom: Int) : Sesja
    data class Blad(val komunikat: String) : Sesja
}

private const val BRAK_POLACZENIA = "Brak połączenia z serwerem. Sprawdź internet."

class SesjaViewModel : ViewModel() {
    private val auth = supabase.auth

    var sesja by mutableStateOf<Sesja>(Sesja.Ladowanie)
        private set

    var login by mutableStateOf("")
        private set
    var haslo by mutableStateOf("")
        private set
    var bladLogowania by mutableStateOf<String?>(null)
        private set
    var logowanieTrwa by mutableStateOf(false)
        private set

    init {
        // Sesję zapisuje i odświeża supabase-kt, tu tylko reagujemy na jej stan
        viewModelScope.launch {
            auth.sessionStatus.collectLatest { status ->
                when (status) {
                    SessionStatus.Initializing -> sesja = Sesja.Ladowanie
                    is SessionStatus.NotAuthenticated -> sesja = Sesja.Wylogowany
                    is SessionStatus.Authenticated -> sprawdzPoziom(status.session)
                    // Nie udało się odświeżyć tokenu (zwykle brak sieci). Zalogowany
                    // pracuje dalej, a supabase-kt sam ponawia odświeżanie.
                    is SessionStatus.RefreshFailure ->
                        if (sesja !is Sesja.Zalogowany) sesja = Sesja.Blad(BRAK_POLACZENIA)
                }
            }
        }
    }

    // Wywoływane po zalogowaniu i po wczytaniu zapisanej sesji przy starcie,
    // żeby konto dezaktywowane w międzyczasie (poziom 0) nie weszło dalej.
    private suspend fun sprawdzPoziom(session: UserSession) {
        val user = session.user
        val obecna = sesja
        // Zwykłe odświeżenie tokenu tego samego użytkownika: poziom już znamy
        if (obecna is Sesja.Zalogowany && user != null && obecna.userId == user.id) return

        sesja = Sesja.Ladowanie
        val poziom = try {
            supabase.postgrest.rpc("moj_poziom").decodeAs<Int>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            sesja = Sesja.Blad(e.komunikat())
            return
        }

        if (poziom < 1) {
            bladLogowania = "Konto nieaktywne"
            haslo = ""
            wyloguj()
        } else {
            sesja = Sesja.Zalogowany(user?.id, emailNaLogin(user?.email.orEmpty()), poziom)
        }
    }

    fun zmienLogin(wartosc: String) {
        login = wartosc
        bladLogowania = null
    }

    fun zmienHaslo(wartosc: String) {
        haslo = wartosc
        bladLogowania = null
    }

    fun zaloguj() {
        if (logowanieTrwa) return
        if (login.isBlank() || haslo.isEmpty()) {
            bladLogowania = "Wpisz login i hasło"
            return
        }
        viewModelScope.launch {
            logowanieTrwa = true
            bladLogowania = null
            try {
                auth.signInWith(Email) {
                    email = loginNaEmail(login)
                    password = haslo
                }
                // Dalej prowadzi sessionStatus: Authenticated -> sprawdzPoziom
                haslo = ""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                bladLogowania = e.komunikat()
            } finally {
                logowanieTrwa = false
            }
        }
    }

    // Rzuca wyjątek przy błędzie; komunikat pokazuje dialog zmiany hasła
    suspend fun ustawNoweHaslo(noweHaslo: String) {
        auth.updateUser { password = noweHaslo }
    }

    // Osobna korutyna: wylogowanie zmienia sessionStatus, a collectLatest
    // przerwałby je, gdyby szło wewnątrz obsługi statusu.
    fun wyloguj() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Bez sieci signOut nie usuwa sesji z telefonu, więc robimy to sami
                auth.clearSession()
            }
        }
    }

    fun ponow() {
        viewModelScope.launch {
            sesja = Sesja.Ladowanie
            try {
                // Udane odświeżenie daje Authenticated -> sprawdzPoziom
                auth.refreshCurrentSession()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Jeśli sesja wygasła, supabase-kt już ją usunął i pokazujemy logowanie
                if (auth.currentSessionOrNull() != null) sesja = Sesja.Blad(e.komunikat())
            }
        }
    }
}
