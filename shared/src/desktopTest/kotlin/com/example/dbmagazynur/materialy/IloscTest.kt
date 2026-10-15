package com.example.dbmagazynur.materialy

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class IloscTest {
    private fun blad(tekst: String) = parsujIlosc(tekst).exceptionOrNull()?.message

    @Test
    fun przecinekIKropka() {
        assertEquals(BigDecimal("2.5"), parsujIlosc(" 2,5 ").getOrThrow())
        assertEquals(BigDecimal("2.5"), parsujIlosc("2.5").getOrThrow())
        assertEquals(BigDecimal("3"), parsujIlosc("3").getOrThrow())
    }

    @Test
    fun trzyMiejscaPoPrzecinkuToMaksimum() {
        assertEquals(BigDecimal("0.125"), parsujIlosc("0,125").getOrThrow())
        // Zera na końcu nie liczą się jako miejsca po przecinku
        assertEquals(BigDecimal("1.5000"), parsujIlosc("1,5000").getOrThrow())
        assertEquals("Najwyżej 3 miejsca po przecinku", blad("0,0005"))
    }

    @Test
    fun bledneIlosci() {
        assertEquals("Wpisz ilość", blad("  "))
        assertEquals("Nieprawidłowa ilość", blad("2,5,1"))
        assertEquals("Nieprawidłowa ilość", blad("abc"))
        assertEquals("Ilość musi być większa od zera", blad("0"))
        assertEquals("Ilość nie może być ujemna", blad("-3"))
        assertEquals("Za duża ilość", blad("1000000000"))
    }

    @Test
    fun zeroTylkoGdyDozwolone() {
        assertEquals(BigDecimal("0"), parsujIlosc("0", dopuscZero = true).getOrThrow())
        assertEquals("Ilość nie może być ujemna", parsujIlosc("-1", dopuscZero = true).exceptionOrNull()?.message)
    }
}
