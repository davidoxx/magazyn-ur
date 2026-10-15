package com.example.dbmagazynur.katalog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dbmagazynur.data.jsonBazy
import com.example.dbmagazynur.zasoby.Res
import java.util.Locale
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

// Katalog części bez logowania. Wspólny dla wszystkich, więc jeden na aplikację.
@OptIn(FlowPreview::class)
class KatalogViewModel : ViewModel() {
    var otwarty by mutableStateOf(false)
        private set
    var fraza by mutableStateOf("")
        private set
    // null = pusta fraza
    var wynik by mutableStateOf<WynikKatalogu?>(null)
        private set
    // Np. „Czesci zam 2508.xlsx · 10 332 pozycji”, null w trakcie wczytywania
    var opis by mutableStateOf<String?>(null)
        private set
    var blad by mutableStateOf<String?>(null)
        private set

    private var wyszukiwarka: Wyszukiwarka? = null
    private var wczytywanie: Job? = null
    private val frazy = MutableStateFlow("")

    init {
        viewModelScope.launch {
            frazy.debounce(150).collectLatest { szukaj(it) }
        }
    }

    fun otworz() {
        otwarty = true
        if (wyszukiwarka == null && wczytywanie == null) wczytaj()
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
                val tekst = Res.readBytes(PLIK).decodeToString()
                val gotowa = withContext(Dispatchers.Default) {
                    val katalog = jsonBazy.decodeFromString<Katalog>(tekst)
                    opisKatalogu(katalog) to Wyszukiwarka(katalog.pozycje)
                }
                wyszukiwarka = gotowa.second
                opis = gotowa.first
                szukaj(fraza)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                blad = "Nie udało się wczytać katalogu części"
            } finally {
                wczytywanie = null
            }
        }
    }

    private suspend fun szukaj(tekst: String) {
        val w = wyszukiwarka ?: return // po wczytaniu katalogu szukanie ruszy samo
        wynik = if (tekst.isBlank()) null else withContext(Dispatchers.Default) { w.szukaj(tekst, LIMIT_KATALOGU) }
    }

    private fun opisKatalogu(katalog: Katalog) =
        "${katalog.zrodlo} · ${"%,d".format(Locale.forLanguageTag("pl-PL"), katalog.pozycje.size)} pozycji"
}
