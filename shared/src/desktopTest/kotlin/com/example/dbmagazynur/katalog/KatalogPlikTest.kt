package com.example.dbmagazynur.katalog

import com.example.dbmagazynur.data.jsonBazy
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Prawdziwy katalog z zasobów Compose (tworzy go narzedzia/katalog_z_excela.py)
class KatalogPlikTest {
    private val katalog = jsonBazy.decodeFromString<Katalog>(File("src/commonMain/composeResources/files/katalog.json").readText())
    private val wyszukiwarka = Wyszukiwarka(katalog.pozycje)

    private fun pierwszy(fraza: String) = wyszukiwarka.szukaj(fraza, 10).pozycje.first()

    @Test
    fun przykladyZArkusza() {
        assertTrue(katalog.pozycje.size > 10_000)
        assertEquals("538000012128", pierwszy("Matryca 1586947+TiN").sap)
        assertEquals("538000012128", pierwszy("Matryca 1586947 + TiN").sap)
        assertTrue(pierwszy("EcoRail2").nazwa.contains("EcoRail-2"))
        assertTrue(pierwszy("EcoRail 2").nazwa.contains("EcoRail-2"))
        assertEquals("538000001000", pierwszy("1000").sap)
        assertEquals("S10A F1", pierwszy("538000001000").miejsca.single())
    }

    @Test
    fun ksztaltDanych() {
        // Numery są unikalne (lista w aplikacji używa ich jako kluczy)
        assertEquals(katalog.pozycje.size, katalog.pozycje.map { it.sap }.toSet().size)
        // Śmieci z eksportu (";22A;;;;;;") usunięte
        assertTrue(katalog.pozycje.flatMap { it.miejsca }.none { ';' in it || it.isBlank() })
        // Ten sam numer w dwóch miejscach
        assertEquals(listOf("S4F F1", "R101B F1"), katalog.pozycje.single { it.sap == "538000011617" }.miejsca)
    }

    @Test
    fun czasSzukania() {
        val frazy = listOf("lozysko", "czujnik indukcyjny bes", "1000", "zabezpieczene silnika", "s3c f1")
        val start = System.nanoTime()
        frazy.forEach { wyszukiwarka.szukaj(it, 100) }
        val ms = (System.nanoTime() - start) / 1_000_000 / frazy.size
        println("Średni czas szukania w ${katalog.pozycje.size} pozycjach: $ms ms")
        assertTrue("Szukanie trwa za długo: $ms ms", ms < 500)
    }
}
