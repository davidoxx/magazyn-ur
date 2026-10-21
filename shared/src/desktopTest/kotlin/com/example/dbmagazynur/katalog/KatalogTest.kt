package com.example.dbmagazynur.katalog

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class KatalogTest {
    private fun katalog(utworzono: String, liczba: Int = 1) =
        Katalog("Czesci zam 2508.xlsx", utworzono, List(liczba) { PozycjaKatalogu("53800000%04d".format(it)) })

    @Test
    fun nowszyWedlugDaty() {
        val pobrany = katalog("2026-10-21")
        val wbudowany = katalog("2026-10-08")
        assertSame(pobrany, nowszyKatalog(pobrany, wbudowany))
        // Aplikacja z nowszym wbudowanym katalogiem nie wraca do starego pobranego
        assertSame(wbudowany, nowszyKatalog(katalog("2026-09-01"), wbudowany))
        // Ta sama data: pobrany (wgrany po zbudowaniu aplikacji)
        val tenSamDzien = katalog("2026-10-08")
        assertSame(tenSamDzien, nowszyKatalog(tenSamDzien, wbudowany))
        assertSame(wbudowany, nowszyKatalog(null, wbudowany))
        assertNull(nowszyKatalog(null, null))
    }

    @Test
    fun odczytOdrzucaPustyIZlyPlik() {
        val dobry = """{"zrodlo":"a.xlsx","utworzono":"2026-10-21","pozycje":[{"sap":"538000001000","miejsca":["S10A F1"]}]}"""
        assertEquals("S10A F1", odczytajKatalog(dobry.toByteArray()).pozycje.single().miejsca.single())
        assertThrows(IllegalArgumentException::class.java) {
            odczytajKatalog("""{"zrodlo":"a.xlsx","utworzono":"2026-10-21","pozycje":[]}""".toByteArray())
        }
        assertThrows(Exception::class.java) { odczytajKatalog("<html>".toByteArray()) }
    }

    @Test
    fun opisZDataILiczbaPozycji() {
        // Java formatuje tysiące po polsku spacją nierozdzielającą
        assertEquals("Czesci zam 2508.xlsx z 08.10.2026 · 10 332 pozycji", opisKatalogu(katalog("2026-10-08", 10_332)))
    }

    @Test
    fun zapisPodmieniaPlik() {
        val folder = Files.createTempDirectory("katalog").toFile()
        val plik = File(folder, "podfolder/katalog.json")
        zapiszAtomowo(plik, "stary".toByteArray())
        zapiszAtomowo(plik, "nowy".toByteArray())
        assertEquals("nowy", plik.readText())
        assertEquals(listOf("katalog.json"), plik.parentFile.list()!!.toList())
        folder.deleteRecursively()
    }
}
