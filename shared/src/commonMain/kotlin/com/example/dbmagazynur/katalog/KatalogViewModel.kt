package com.example.dbmagazynur.katalog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dbmagazynur.data.Ustawienia
import com.example.dbmagazynur.data.supabase
import com.example.dbmagazynur.zasoby.Res
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val LIMIT_KATALOGU = 100

// Plik katalogu w zasobach Compose (composeResources/files), wspólny dla Androida i Windows
private const val PLIK = "files/katalog.json"

// Nowszy katalog wgrywa administrator w panelu Supabase (prywatny bucket, odczyt od poziomu 1)
private const val BUCKET_KATALOGU = "katalog"
private const val PLIK_W_SUPABASE = "katalog.json"

// Katalog części bez logowania. Wspólny dla wszystkich, więc jeden na aplikację.
// Pokazuje nowszy z dwóch: wbudowany albo pobrany wcześniej z Supabase (plikPobrany).
// Po zalogowaniu sprawdza, czy w Supabase jest inny plik, i wtedy go pobiera, więc
// nowy arkusz nie wymaga nowej wersji aplikacji, a pobrany działa potem bez internetu.
@OptIn(FlowPreview::class)
class KatalogViewModel(private val plikPobrany: File) : ViewModel() {
    var otwarty by mutableStateOf(false)
        private set
    var fraza by mutableStateOf("")
        private set
    // null = pusta fraza
    var wynik by mutableStateOf<WynikKatalogu?>(null)
        private set
    // Np. „Czesci zam 2508.xlsx z 08.10.2026 · 10 332 pozycji”, null w trakcie wczytywania
    var opis by mutableStateOf<String?>(null)
        private set
    var blad by mutableStateOf<String?>(null)
        private set

    private var katalog: Katalog? = null
    private var wyszukiwarka: Wyszukiwarka? = null
    private var wczytywanie: Job? = null
    private var sprawdzanie: Job? = null
    private val frazy = MutableStateFlow("")

    init {
        viewModelScope.launch {
            frazy.debounce(150).collectLatest { szukaj(it) }
        }
    }

    fun otworz() {
        otwarty = true
        when {
            wyszukiwarka == null && wczytywanie == null -> wczytaj()
            wczytywanie == null -> sprawdzNowszy()
        }
    }

    fun zamknij() {
        otwarty = false
    }

    fun zmienFraze(wartosc: String) {
        fraza = wartosc
        frazy.value = wartosc
    }

    // 1,3 MB JSON-a i indeks: kilkaset milisekund, więc poza wątkiem głównym i tylko raz
    private fun wczytaj() {
        wczytywanie = viewModelScope.launch {
            blad = null
            try {
                val najnowszy = withContext(Dispatchers.IO) {
                    nowszyKatalog(
                        alboNull { odczytajKatalog(plikPobrany.readBytes()) },
                        alboNull { odczytajKatalog(Res.readBytes(PLIK)) },
                    )
                }
                if (najnowszy == null) blad = "Nie udało się wczytać katalogu części" else ustaw(najnowszy)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                blad = "Nie udało się wczytać katalogu części"
            } finally {
                wczytywanie = null
            }
            sprawdzNowszy()
        }
    }

    // Tylko po zalogowaniu (bucket jest prywatny). Jedno małe zapytanie o listę plików;
    // sam katalog pobiera tylko wtedy, gdy plik w Supabase się zmienił. Błędy (brak
    // internetu, brak pliku, konto bez dostępu) pomija: zostaje katalog z urządzenia.
    private fun sprawdzNowszy() {
        if (sprawdzanie != null || supabase.auth.currentSessionOrNull() == null) return
        sprawdzanie = viewModelScope.launch {
            try {
                val bucket = supabase.storage.from(BUCKET_KATALOGU)
                val plik = bucket.list().firstOrNull { it.name == PLIK_W_SUPABASE } ?: return@launch
                val wersja = "${plik.id}|${plik.updatedAt}"
                if (wersja == Ustawienia.wersjaKatalogu && plikPobrany.isFile) return@launch

                val bajty = bucket.downloadAuthenticated(PLIK_W_SUPABASE)
                val nowy = withContext(Dispatchers.IO) {
                    odczytajKatalog(bajty).also { zapiszAtomowo(plikPobrany, bajty) }
                }
                Ustawienia.wersjaKatalogu = wersja
                if (nowszyKatalog(nowy, katalog) === nowy) ustaw(nowy)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            } finally {
                sprawdzanie = null
            }
        }
    }

    private suspend fun ustaw(nowy: Katalog) {
        val gotowa = withContext(Dispatchers.Default) { Wyszukiwarka(nowy.pozycje) }
        katalog = nowy
        wyszukiwarka = gotowa
        opis = opisKatalogu(nowy)
        blad = null
        szukaj(fraza)
    }

    private suspend fun szukaj(tekst: String) {
        val w = wyszukiwarka ?: return // po wczytaniu katalogu szukanie ruszy samo
        wynik = if (tekst.isBlank()) null else withContext(Dispatchers.Default) { w.szukaj(tekst, LIMIT_KATALOGU) }
    }
}

// Brak pliku albo uszkodzony plik to nie błąd, póki jest drugi katalog
private inline fun <T> alboNull(blok: () -> T): T? =
    try {
        blok()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
