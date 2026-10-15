package com.example.dbmagazynur.wydania

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dbmagazynur.data.komunikat
import com.example.dbmagazynur.data.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

private const val STRONA = 20

// Ostatnie wydania na ekranie głównym, z podziałem na wszystkie, kosztowe i SAP
class OstatnieWydaniaViewModel : ViewModel() {
    var stan by mutableStateOf(StanOstatnichWydan())
        private set

    private var wczytywanieJob: Job? = null

    fun ustawFiltr(filtr: FiltrWydan) {
        if (filtr == stan.filtr) return
        stan = stan.copy(filtr = filtr, wydania = null, saStarsze = false)
        odswiez()
    }

    fun odswiez() = wczytaj(od = 0)

    fun wczytajStarsze() {
        if (!stan.wczytywanie) wczytaj(od = stan.wydania?.size ?: 0)
    }

    private fun wczytaj(od: Int) {
        val filtr = stan.filtr
        val poprzednie = wczytywanieJob
        wczytywanieJob = viewModelScope.launch {
            poprzednie?.cancelAndJoin()
            stan = stan.copy(wczytywanie = true, blad = null)
            try {
                // Jeden wiersz więcej niż strona, żeby wiedzieć, czy są starsze
                val strona = supabase.from("ostatnie_wydania")
                    .select {
                        filter { filtr.typMaterialu?.let { eq("typ_materialu", it) } }
                        order("utworzono", Order.DESCENDING)
                        order("id", Order.DESCENDING)
                        range(od.toLong(), (od + STRONA).toLong())
                    }
                    .decodeList<OstatnieWydanie>()
                val dotychczasowe = if (od == 0) emptyList() else stan.wydania.orEmpty()
                stan = stan.copy(
                    // Nowe wydanie w międzyczasie przesuwa strony, stąd usuwanie powtórzeń
                    wydania = (dotychczasowe + strona.take(STRONA)).distinctBy { it.id },
                    saStarsze = strona.size > STRONA,
                    wczytywanie = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                stan = stan.copy(blad = e.komunikat(), wczytywanie = false)
            }
        }
    }
}
