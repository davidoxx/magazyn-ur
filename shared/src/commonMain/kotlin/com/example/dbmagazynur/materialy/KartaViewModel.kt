package com.example.dbmagazynur.materialy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dbmagazynur.data.BUCKET_ZDJEC
import com.example.dbmagazynur.data.komunikat
import com.example.dbmagazynur.data.nowaSciezkaZdjecia
import com.example.dbmagazynur.data.supabase
import com.example.dbmagazynur.maszyny.Maszyna
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val STRONA_HISTORII = 20

enum class Operacja(val funkcja: String, val tytul: String, val przycisk: String, val wynik: String) {
    WYDANIE("wydaj", "Wydanie", "Wydaj", "Wydano"),
    PRZYJECIE("przyjmij", "Przyjęcie", "Przyjmij", "Przyjęto"),
}

// Karta materiału z formularzem wydania i przyjęcia.
// Operacje idą w viewModelScope, żeby obrót ekranu nie przerwał ich w połowie
// (pracownik nie wiedziałby, czy wydanie się zapisało, i mógłby je powtórzyć).
class KartaViewModel : ViewModel() {
    var material by mutableStateOf<Material?>(null)
        private set
    var bladWczytania by mutableStateOf<String?>(null)
        private set
    var komunikat by mutableStateOf<String?>(null)
        private set

    // Otwarty formularz operacji (null = widać samą kartę)
    var operacja by mutableStateOf<Operacja?>(null)
        private set
    var iloscTekst by mutableStateOf("")
        private set
    var uwagi by mutableStateOf("")
        private set
    var maszyna by mutableStateOf<Maszyna?>(null)
        private set
    var operacjaTrwa by mutableStateOf(false)
        private set
    var bladOperacji by mutableStateOf<String?>(null)
        private set

    var wyborMaszyny by mutableStateOf(false)
        private set
    var maszyny by mutableStateOf<List<Maszyna>?>(null)
        private set
    var bladMaszyn by mutableStateOf<String?>(null)
        private set

    // Wysyłanie albo usuwanie zdjęcia
    var zdjecieTrwa by mutableStateOf(false)
        private set

    // Historia ruchów od najnowszych (null = jeszcze nie wczytana)
    var historia by mutableStateOf<List<Ruch>?>(null)
        private set
    var saStarsze by mutableStateOf(false)
        private set

    private var zmienionoStan = false
    private var wczytywanie: Job? = null
    private var wczytywanieStarszych: Job? = null
    private var wczytywanieMaszyn: Job? = null

    val otwarta: Boolean get() = material != null || bladWczytania != null

    // Pokazujemy od razu dane z wyszukiwania i dociągamy aktualny stan
    fun otworz(z: Material, komunikat: String? = null) {
        material = z
        bladWczytania = null
        this.komunikat = komunikat
        operacja = null
        wyborMaszyny = false
        zmienionoStan = false
        historia = null
        saStarsze = false
        wczytaj()
    }

    // Zwraca true, jeśli stan materiału się zmienił i wyniki wyszukiwania trzeba odświeżyć
    fun zamknij(): Boolean {
        wczytywanie?.cancel()
        wczytywanieStarszych?.cancel()
        material = null
        bladWczytania = null
        historia = null
        return zmienionoStan.also { zmienionoStan = false }
    }

    // Materiał i pierwsza strona historii, np. po wydaniu, żeby było widać, czy się zapisało
    fun wczytaj() {
        val id = material?.id ?: return
        wczytywanie?.cancel()
        wczytywanieStarszych?.cancel()
        wczytywanie = viewModelScope.launch {
            bladWczytania = null
            try {
                val aktualny = supabase.from("materialy")
                    .select { filter { eq("id", id) } }
                    .decodeSingleOrNull<Material>()
                if (aktualny == null) {
                    bladWczytania = "Nie ma takiego materiału. Mógł zostać usunięty."
                    return@launch
                }
                material = aktualny
                val strona = pobierzHistorie(id, od = 0)
                historia = strona.take(STRONA_HISTORII)
                saStarsze = strona.size > STRONA_HISTORII
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                bladWczytania = e.komunikat()
            }
        }
    }

    fun wczytajStarsze() {
        val id = material?.id ?: return
        val obecna = historia ?: return
        if (wczytywanieStarszych?.isActive == true) return
        wczytywanieStarszych = viewModelScope.launch {
            try {
                val strona = pobierzHistorie(id, od = obecna.size.toLong())
                // Nowy ruch w międzyczasie przesuwa strony, stąd usuwanie powtórzeń
                historia = (obecna + strona.take(STRONA_HISTORII)).distinctBy { it.id }
                saStarsze = strona.size > STRONA_HISTORII
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                bladWczytania = e.komunikat()
            }
        }
    }

    // Jeden wiersz więcej niż strona, żeby wiedzieć, czy są starsze
    private suspend fun pobierzHistorie(materialId: Long, od: Long): List<Ruch> =
        supabase.from("ruchy")
            .select(Columns.raw(KOLUMNY_HISTORII)) {
                filter { eq("material_id", materialId) }
                order("utworzono", Order.DESCENDING)
                order("id", Order.DESCENDING)
                range(od, od + STRONA_HISTORII)
            }
            .decodeList<Ruch>()

    fun komunikatPokazany() {
        komunikat = null
    }

    fun pokazKomunikat(tresc: String) {
        komunikat = tresc
    }

    // Wysyła nowe zdjęcie pod nową nazwą, zapisuje ścieżkę w materiale i dopiero
    // wtedy usuwa stary plik. przygotuj zależy od platformy (aparat, galeria, plik z dysku)
    // i zwraca zmniejszony JPEG.
    fun ustawZdjecie(przygotuj: suspend () -> ByteArray) {
        val m = material ?: return
        if (zdjecieTrwa) return
        viewModelScope.launch {
            zdjecieTrwa = true
            val bucket = supabase.storage.from(BUCKET_ZDJEC)
            val nowa = nowaSciezkaZdjecia(m.id)
            try {
                val dane = withContext(Dispatchers.Default) { przygotuj() }
                bucket.upload(nowa, dane) { contentType = ContentType.Image.JPEG }
                try {
                    zapiszSciezkeZdjecia(m.id, nowa)
                } catch (e: Exception) {
                    usunPlikPoCichu(nowa) // nie zostawiamy pliku, do którego nic nie prowadzi
                    throw e
                }
                m.zdjecie?.let { usunPlikPoCichu(it) }
                komunikat = "Zapisano zdjęcie"
                wczytaj()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                komunikat = e.komunikat()
            } finally {
                zdjecieTrwa = false
            }
        }
    }

    fun usunZdjecie() {
        val m = material ?: return
        val stare = m.zdjecie ?: return
        if (zdjecieTrwa) return
        viewModelScope.launch {
            zdjecieTrwa = true
            try {
                zapiszSciezkeZdjecia(m.id, null)
                usunPlikPoCichu(stare)
                komunikat = "Usunięto zdjęcie"
                wczytaj()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                komunikat = e.komunikat()
            } finally {
                zdjecieTrwa = false
            }
        }
    }

    private suspend fun zapiszSciezkeZdjecia(materialId: Long, sciezka: String?) {
        val zapisany = supabase.from("materialy")
            .update(buildJsonObject { put("zdjecie", sciezka) }) {
                filter { eq("id", materialId) }
                select()
            }
            .decodeSingleOrNull<Material>()
        if (zapisany == null) throw IllegalStateException("Nie ma takiego materiału. Mógł zostać usunięty.")
        material = zapisany
    }

    // Sprzątanie Storage: nieudane usunięcie zostawia tylko zbędny plik
    private suspend fun usunPlikPoCichu(sciezka: String) {
        try {
            supabase.storage.from(BUCKET_ZDJEC).delete(listOf(sciezka))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    fun otworzOperacje(nowa: Operacja) {
        operacja = nowa
        iloscTekst = ""
        uwagi = ""
        maszyna = null
        bladOperacji = null
    }

    fun zamknijOperacje() {
        if (operacjaTrwa) return
        operacja = null
        wyborMaszyny = false
        // Po błędzie połączenia historia pokaże, czy operacja jednak się zapisała
        wczytaj()
    }

    fun zmienIlosc(wartosc: String) {
        iloscTekst = wartosc
        bladOperacji = null
    }

    fun zmienUwagi(wartosc: String) {
        uwagi = wartosc
    }

    // Lista wczytywana przy każdym otwarciu, żeby było widać maszyny dodane w trakcie zmiany
    fun pokazWyborMaszyny() {
        wyborMaszyny = true
        wczytajMaszyny()
    }

    fun zamknijWyborMaszyny() {
        wyborMaszyny = false
    }

    fun wybierzMaszyne(wybrana: Maszyna?) {
        maszyna = wybrana
        wyborMaszyny = false
        bladOperacji = null
    }

    fun wczytajMaszyny() {
        wczytywanieMaszyn?.cancel()
        wczytywanieMaszyn = viewModelScope.launch {
            bladMaszyn = null
            try {
                maszyny = supabase.from("maszyny")
                    .select {
                        filter { eq("aktywna", true) }
                        order("nazwa", Order.ASCENDING)
                    }
                    .decodeList<Maszyna>()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                bladMaszyn = e.komunikat()
            }
        }
    }

    fun wykonaj() {
        val rodzaj = operacja ?: return
        val m = material ?: return
        if (operacjaTrwa) return
        val ilosc = parsujIlosc(iloscTekst).getOrElse {
            bladOperacji = it.message
            return
        }
        // Wygoda: baza i tak odrzuci wydanie SAP bez maszyny
        if (rodzaj == Operacja.WYDANIE && m.typ == TypMaterialu.SAP && maszyna == null) {
            bladOperacji = "Wybierz maszynę. Przy materiale SAP jest obowiązkowa."
            return
        }
        viewModelScope.launch {
            operacjaTrwa = true
            bladOperacji = null
            try {
                supabase.postgrest.rpc(
                    rodzaj.funkcja,
                    buildJsonObject {
                        put("p_material_id", m.id)
                        put("p_ilosc", ilosc)
                        if (rodzaj == Operacja.WYDANIE) put("p_maszyna_id", maszyna?.id)
                        put("p_uwagi", uwagi.trim().ifEmpty { null })
                    },
                )
                zmienionoStan = true
                komunikat = "${rodzaj.wynik} ${formatujIlosc(ilosc.toDouble())} ${m.jednostka}"
                operacja = null
                wczytaj()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                bladOperacji = e.komunikat()
            } finally {
                operacjaTrwa = false
            }
        }
    }
}
