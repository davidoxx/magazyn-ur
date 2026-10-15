package com.example.dbmagazynur.katalog

import com.example.dbmagazynur.data.doPorownania

private val ROZDZIELACZE = Regex("[^\\p{L}\\p{N}]+")
private val PELNY_SAP = Regex("538\\d{9}")

// Słowa bez wielkości liter, polskich znaków i znaków rozdzielających:
// "Matryca 1586947 + TiN" -> [matryca, 1586947, tin]
fun slowa(tekst: String): List<String> =
    doPorownania(tekst).split(ROZDZIELACZE).filter { it.isNotEmpty() }

// Wyszukiwarka odporna na sposób zapisu. „EcoRail2”, „ecorail 2” i „EcoRail-2”
// to dla niej to samo, podobnie „3,969” i „3.969”. Działa w pamięci telefonu.
class Wyszukiwarka(pozycje: List<PozycjaKatalogu>) {

    private class Wpis(val pozycja: PozycjaKatalogu) {
        // Każde pole zbite w ciąg liter i cyfr, pola rozdzielone '|',
        // żeby fraza nie skleiła końca nazwy z początkiem miejsca
        val zbite: String
        val slowa: List<String>

        init {
            val pola = buildList {
                add(pozycja.sap)
                add(pozycja.nazwa)
                addAll(pozycja.inneNazwy)
                addAll(pozycja.miejsca)
                addAll(pozycja.dostawcy)
            }.map { slowa(it) }
            zbite = pola.joinToString("|") { it.joinToString("") }
            slowa = pola.flatten().distinct()
        }

        val sapZbity: String = slowa(pozycja.sap).joinToString("")

        // Końcówka numeru: 538000001000 -> 1000 (tak numerują arkusze Magazyn)
        val koncowka: Long? = pozycja.sap.takeIf { PELNY_SAP.matches(it) }?.substring(3)?.toLong()
    }

    private val wpisy = pozycje.map(::Wpis)
    private val wszystkieSlowa: Set<String> = wpisy.flatMapTo(HashSet()) { it.slowa }

    val liczbaPozycji: Int get() = wpisy.size

    fun szukaj(fraza: String, limit: Int): WynikKatalogu {
        val tokeny = slowa(fraza)
        if (tokeny.isEmpty()) return WynikKatalogu(emptyList(), 0)
        val zbitaFraza = tokeny.joinToString("")
        val liczba = zbitaFraza.takeIf { it.all(Char::isDigit) }?.toLongOrNull()

        val trafienia = ArrayList<Pair<Wpis, Int>>()
        for (wpis in wpisy) {
            val punkty = punktyDokladne(wpis, tokeny, zbitaFraza, liczba)
            if (punkty > 0) trafienia += wpis to punkty
        }
        // Literówki sprawdzamy dopiero, gdy dokładnych trafień jest mało, żeby nie zaśmiecać wyników
        if (trafienia.size < PROG_LITEROWEK) {
            val znalezione = trafienia.mapTo(HashSet()) { it.first }
            trafienia += zLiterowkami(tokeny).filter { it.first !in znalezione }
        }

        trafienia.sortByDescending { it.second }
        return WynikKatalogu(trafienia.take(limit).map { it.first.pozycja }, trafienia.size)
    }

    private fun punktyDokladne(wpis: Wpis, tokeny: List<String>, zbitaFraza: String, liczba: Long?): Int {
        if (liczba != null) {
            if (wpis.sapZbity == zbitaFraza) return 1000
            if (wpis.koncowka == liczba) return 900
        }
        val poczatekPola = wpis.zbite.startsWith(zbitaFraza) || "|$zbitaFraza" in wpis.zbite
        if (poczatekPola) return 650
        if (zbitaFraza in wpis.zbite) return 600
        if (tokeny.all { it in wpis.zbite }) {
            // Premia za słowa trafione od początku: „lozysko” lepiej niż „...lozyskowy”
            return 300 + 10 * tokeny.count { t -> wpis.slowa.any { it.startsWith(t) } }
        }
        return 0
    }

    // Każde słowo frazy musi wystąpić dokładnie albo z literówką w którymś słowie pozycji
    private fun zLiterowkami(tokeny: List<String>): List<Pair<Wpis, Int>> {
        val podobne: List<Map<String, Int>?> = tokeny.map { token ->
            val dozwolone = dozwolonaRoznica(token)
            if (dozwolone == 0) {
                null
            } else {
                wszystkieSlowa.mapNotNull { slowo ->
                    roznicaZPoczatkiem(token, slowo, dozwolone)?.let { slowo to it }
                }.toMap()
            }
        }
        if (podobne.all { it == null }) return emptyList()

        val wynik = ArrayList<Pair<Wpis, Int>>()
        for (wpis in wpisy) {
            var suma = 0
            val pasuje = tokeny.indices.all { i ->
                if (tokeny[i] in wpis.zbite) return@all true
                val roznice = podobne[i] ?: return@all false
                val najmniejsza = wpis.slowa.mapNotNull { roznice[it] }.minOrNull() ?: return@all false
                suma += najmniejsza
                true
            }
            if (pasuje && suma > 0) wynik += wpis to (100 - 10 * suma)
        }
        return wynik
    }

    companion object {
        private const val PROG_LITEROWEK = 10

        // Krótkich słów i numerów katalogowych nie poprawiamy: „6204” to nie „6240”
        fun dozwolonaRoznica(token: String): Int = when {
            token.any(Char::isDigit) -> 0
            token.length < 4 -> 0
            token.length < 8 -> 1
            else -> 2
        }

        // Różnica z całym słowem albo z jego początkiem (ktoś nie dopisał końcówki)
        fun roznicaZPoczatkiem(token: String, slowo: String, dozwolone: Int): Int? {
            if (slowo.length < token.length - dozwolone) return null
            val kandydaci = listOf(slowo, slowo.take(token.length), slowo.take(token.length + 1)).distinct()
            return kandydaci.minOf { odleglosc(token, it, dozwolone) }.takeIf { it <= dozwolone }
        }

        // Odległość edycyjna z przestawieniem sąsiednich liter („czjunik” -> „czujnik” to 1).
        // Przerywa, gdy wiadomo już, że przekroczy limit.
        fun odleglosc(a: String, b: String, limit: Int): Int {
            if (kotlin.math.abs(a.length - b.length) > limit) return limit + 1
            var przedPoprzedni = IntArray(b.length + 1)
            var poprzedni = IntArray(b.length + 1) { it }
            var biezacy = IntArray(b.length + 1)
            for (i in 1..a.length) {
                biezacy[0] = i
                var minimumWiersza = i
                for (j in 1..b.length) {
                    val koszt = if (a[i - 1] == b[j - 1]) 0 else 1
                    var d = minOf(poprzedni[j] + 1, biezacy[j - 1] + 1, poprzedni[j - 1] + koszt)
                    if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                        d = minOf(d, przedPoprzedni[j - 2] + 1)
                    }
                    biezacy[j] = d
                    minimumWiersza = minOf(minimumWiersza, d)
                }
                if (minimumWiersza > limit) return limit + 1
                val wolny = przedPoprzedni
                przedPoprzedni = poprzedni
                poprzedni = biezacy
                biezacy = wolny
            }
            return poprzedni[b.length]
        }
    }
}
