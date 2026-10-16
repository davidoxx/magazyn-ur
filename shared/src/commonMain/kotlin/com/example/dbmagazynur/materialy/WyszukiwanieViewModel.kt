package com.example.dbmagazynur.materialy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dbmagazynur.data.Ustawienia
import com.example.dbmagazynur.data.komunikat
import com.example.dbmagazynur.data.supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

const val LIMIT_WYNIKOW = 50

data class StanWyszukiwania(
    val wynikiDla: String? = null, // fraza, której dotyczą wyniki
    val wyniki: List<Material> = emptyList(),
    val saKolejne: Boolean = false, // wyników jest więcej niż LIMIT_WYNIKOW
    val szukam: Boolean = false,
    val blad: String? = null,
)

// proba rośnie przy „Spróbuj ponownie”, żeby ta sama fraza poszła jeszcze raz
private data class Zapytanie(val fraza: String, val proba: Int = 0)

@OptIn(FlowPreview::class)
class WyszukiwanieViewModel : ViewModel() {
    var fraza by mutableStateOf("")
        private set
    var stan by mutableStateOf(StanWyszukiwania())
        private set
    // Miniatury zdjęć obok nazw; wybór zostaje na urządzeniu po ponownym uruchomieniu
    var pokazZdjecia by mutableStateOf(Ustawienia.zdjeciaWWynikach)
        private set

    private val zapytania = MutableStateFlow(Zapytanie(""))

    init {
        // Szukamy w trakcie pisania, ale dopiero po chwili przerwy.
        // collectLatest przerywa starsze zapytanie, gdy przyjdzie nowe.
        viewModelScope.launch {
            zapytania.debounce(300).collectLatest { szukaj(it.fraza) }
        }
    }

    fun zmienFraze(wartosc: String) {
        fraza = wartosc
        val nowa = wartosc.trim()
        if (nowa != zapytania.value.fraza) {
            if (nowa.isNotEmpty()) stan = stan.copy(szukam = true)
            zapytania.value = Zapytanie(nowa)
        }
    }

    fun ustawPokazZdjecia(wartosc: Boolean) {
        pokazZdjecia = wartosc
        Ustawienia.zdjeciaWWynikach = wartosc
    }

    fun ponow() {
        stan = stan.copy(szukam = true, blad = null)
        zapytania.update { it.copy(proba = it.proba + 1) }
    }

    private suspend fun szukaj(fraza: String) {
        if (fraza.isEmpty()) {
            stan = StanWyszukiwania()
            return
        }
        stan = stan.copy(szukam = true, blad = null)
        stan = try {
            val lista = supabase.postgrest
                .rpc("szukaj_materialow", buildJsonObject { put("p_fraza", fraza) }) {
                    limit(LIMIT_WYNIKOW + 1L)
                }
                .decodeList<Material>()
            StanWyszukiwania(
                wynikiDla = fraza,
                wyniki = lista.take(LIMIT_WYNIKOW),
                saKolejne = lista.size > LIMIT_WYNIKOW,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            stan.copy(szukam = false, blad = e.komunikat())
        }
    }
}
