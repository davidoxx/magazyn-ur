package com.example.dbmagazynur.sap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dbmagazynur.data.komunikat
import com.example.dbmagazynur.data.odmiana
import com.example.dbmagazynur.data.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private fun wydan(liczba: Int) = odmiana(liczba, "wydanie", "wydania", "wydań")

// Lista lidera zmiany: wydania materiałów SAP czekające na zaksięgowanie.
// Uprawnienia (poziom 2+) sprawdza baza w zaksieguj_w_sap i anuluj_wydanie.
class WydaniaSapViewModel : ViewModel() {
    var otwarta by mutableStateOf(false)
        private set
    // Liczba czekających wydań na ekran główny (null = nieznana)
    var liczba by mutableStateOf<Int?>(null)
        private set
    var wydania by mutableStateOf<List<WydanieDoSap>?>(null)
        private set
    var wczytywanie by mutableStateOf(false)
        private set
    var blad by mutableStateOf<String?>(null)
        private set
    var zaznaczone by mutableStateOf(emptySet<Long>())
        private set
    // Trwa księgowanie albo anulowanie
    var trwa by mutableStateOf(false)
        private set
    var komunikat by mutableStateOf<String?>(null)
        private set

    private var zmienionoStan = false
    private var wczytywanieJob: Job? = null

    fun otworz() {
        otwarta = true
        zaznaczone = emptySet()
        komunikat = null
        zmienionoStan = false
        wczytaj()
    }

    // Zwraca true, jeśli anulowanie zmieniło stany materiałów (wyniki wyszukiwania do odświeżenia).
    // W trakcie operacji lista zostaje otwarta.
    fun zamknij(): Boolean {
        if (trwa) return false
        otwarta = false
        return zmienionoStan.also { zmienionoStan = false }
    }

    fun odswiezLiczbe() {
        viewModelScope.launch {
            liczba = try {
                supabase.from("wydania_do_sap")
                    .select {
                        head = true
                        count(Count.EXACT)
                    }
                    .countOrNull()
                    ?.toInt()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }

    fun wczytaj() {
        val poprzednie = wczytywanieJob
        wczytywanieJob = viewModelScope.launch {
            poprzednie?.cancelAndJoin()
            wczytywanie = true
            blad = null
            try {
                val lista = supabase.from("wydania_do_sap")
                    .select { order("utworzono", Order.ASCENDING) }
                    .decodeList<WydanieDoSap>()
                wydania = lista
                liczba = lista.size
                // Zaznaczenie zostaje tylko na wydaniach, które nadal są na liście
                zaznaczone = zaznaczone intersect lista.map { it.id }.toSet()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                blad = e.komunikat()
            } finally {
                wczytywanie = false
            }
        }
    }

    fun przelacz(id: Long) {
        zaznaczone = if (id in zaznaczone) zaznaczone - id else zaznaczone + id
    }

    fun zaznaczWszystkie(zaznacz: Boolean) {
        zaznaczone = if (zaznacz) wydania.orEmpty().map { it.id }.toSet() else emptySet()
    }

    fun komunikatPokazany() {
        komunikat = null
    }

    fun zaksieguj() {
        val ids = zaznaczone.toList()
        if (ids.isEmpty() || trwa) return
        viewModelScope.launch {
            trwa = true
            try {
                val zaksiegowano = supabase.postgrest
                    .rpc(
                        "zaksieguj_w_sap",
                        buildJsonObject { putJsonArray("p_ids") { ids.forEach { add(it) } } },
                    )
                    .decodeAs<Int>()
                komunikat = if (zaksiegowano == ids.size) {
                    "Zaksięgowano $zaksiegowano ${wydan(zaksiegowano)}"
                } else {
                    "Zaksięgowano $zaksiegowano z ${ids.size}. " +
                        "Pozostałe ktoś już zaksięgował albo anulował."
                }
                zaznaczone = emptySet()
                wczytaj()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                komunikat = e.komunikat()
            } finally {
                trwa = false
            }
        }
    }

    fun anuluj(id: Long) {
        if (trwa) return
        viewModelScope.launch {
            trwa = true
            try {
                supabase.postgrest.rpc("anuluj_wydanie", buildJsonObject { put("p_id", id) })
                zmienionoStan = true
                komunikat = "Wydanie anulowane, towar wrócił na stan"
                zaznaczone = zaznaczone - id
                wczytaj()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                komunikat = e.komunikat()
            } finally {
                trwa = false
            }
        }
    }
}
