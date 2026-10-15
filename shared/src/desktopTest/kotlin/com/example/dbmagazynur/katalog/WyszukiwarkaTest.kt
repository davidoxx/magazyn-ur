package com.example.dbmagazynur.katalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WyszukiwarkaTest {
    private val wyszukiwarka = Wyszukiwarka(
        listOf(
            PozycjaKatalogu("538000001000", "Zabezpieczenie silnika 3RV1021-4AA15", listOf("S10A F1")),
            PozycjaKatalogu("538000011000", "Czujnik indukcyjny BES 516-327-S4-C", listOf("KLT")),
            PozycjaKatalogu(
                "538000005425", "Mechanizm srubowo toczny 32x32Rx3,969-3", listOf("F1"),
                inneNazwy = listOf("Mechanizm śrubowo toczny R151039990"), dostawcy = listOf("Bosch-Rexroth"),
            ),
            PozycjaKatalogu("538000012128", "Matryca 1586947 + TiN", listOf("F1")),
            PozycjaKatalogu("538000012100", "Zasilacz 1-fazowy EcoRail-2 10ADC 85135", listOf("F1")),
            PozycjaKatalogu("538000000008", "lozysko do PF5 6001ZZ", listOf("S16A F1"), inneNazwy = listOf("Łożysko do PF5")),
            PozycjaKatalogu("538000WO0001", "Olej VG46 (205L) SW 46", listOf("OLEJOWNIA")),
        ),
    )

    private fun sap(fraza: String) = wyszukiwarka.szukaj(fraza, 10).pozycje.map { it.sap }

    @Test
    fun spacjeIZnakiRozdzielajaceNieMajaZnaczenia() {
        assertEquals("538000012100", sap("EcoRail2").first())
        assertEquals("538000012100", sap("ecorail 2").first())
        assertEquals("538000012128", sap("Matryca 1586947+TiN").first())
        assertEquals("538000012128", sap("matryca1586947 tin").first())
        assertEquals("538000011000", sap("bes 516 327").first())
        // Przecinek zamiast kropki i odwrotnie
        assertEquals("538000005425", sap("3.969").first())
    }

    @Test
    fun polskieZnakiIInneNazwy() {
        assertEquals("538000000008", sap("Łożysko 6001zz").first())
        assertEquals("538000005425", sap("srubowo toczny bosch").first())
    }

    @Test
    fun numerSapPelnyAlboKoncowka() {
        assertEquals("538000001000", sap("538000001000").first())
        // Końcówka jak w arkuszach Magazyn ma pierwszeństwo przed numerami, które ją tylko zawierają
        assertEquals(listOf("538000001000", "538000011000"), sap("1000"))
        assertEquals("538000WO0001", sap("538000wo0001").first())
    }

    @Test
    fun miejsceIDostawca() {
        assertEquals(listOf("538000000008"), sap("s16a"))
        assertEquals(listOf("538000005425"), sap("rexroth"))
    }

    @Test
    fun literowkiWSlowachAleNieWNumerach() {
        assertEquals("538000001000", sap("zabezpieczene silnika").first())
        // Przestawione litery
        assertEquals("538000011000", sap("czjunik").first())
        // W numerach katalogowych literówek nie poprawiamy
        assertTrue(sap("1586974").isEmpty())
    }

    @Test
    fun frazaNieLaczyNazwyZMiejscem() {
        // „...TiN” i miejsce „F1” nie tworzą razem „tinf1”
        assertTrue(sap("tinf1").isEmpty())
    }

    @Test
    fun odlegloscEdycyjna() {
        assertEquals(1, Wyszukiwarka.odleglosc("czujnk", "czujnik", 2))
        assertEquals(1, Wyszukiwarka.odleglosc("czjunik", "czujnik", 2))
        assertEquals(3, Wyszukiwarka.odleglosc("abc", "xyzabc", 2))
    }
}
