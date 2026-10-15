package com.example.dbmagazynur.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZdjeciaTest {
    @Test
    fun dluzszyBokDoLimituZZachowaniemProporcji() {
        assertEquals(1280 to 960, rozmiarDocelowy(4000, 3000, 1280))
        assertEquals(960 to 1280, rozmiarDocelowy(3000, 4000, 1280))
        // Małych zdjęć nie powiększamy
        assertEquals(800 to 600, rozmiarDocelowy(800, 600, 1280))
        // Skrajne proporcje nie dają zerowego boku
        assertEquals(1280 to 1, rozmiarDocelowy(5000, 1, 1280))
    }

    @Test
    fun kazdeZdjecieMaNowaSciezke() {
        val pierwsza = nowaSciezkaZdjecia(7)
        assertTrue(pierwsza.matches(Regex("materialy/7/[0-9a-f-]{36}\\.jpg")))
        assertNotEquals(pierwsza, nowaSciezkaZdjecia(7))
    }
}
