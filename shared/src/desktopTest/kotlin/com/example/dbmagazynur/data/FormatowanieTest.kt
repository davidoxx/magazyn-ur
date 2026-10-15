package com.example.dbmagazynur.data

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatowanieTest {
    private fun wydan(n: Int) = odmiana(n, "wydanie", "wydania", "wydań")

    @Test
    fun odmianaPoLiczbie() {
        assertEquals("wydań", wydan(0))
        assertEquals("wydanie", wydan(1))
        assertEquals("wydania", wydan(2))
        assertEquals("wydania", wydan(4))
        assertEquals("wydań", wydan(5))
        assertEquals("wydań", wydan(12))
        assertEquals("wydań", wydan(14))
        assertEquals("wydania", wydan(22))
        assertEquals("wydań", wydan(112))
    }

    @Test
    fun dataZBazyWCzasiePolskim() {
        val warszawa = ZoneId.of("Europe/Warsaw")
        // Format, w jakim PostgREST zwraca timestamptz
        assertEquals("08.10.2026 14:32", formatujDate("2026-10-08T12:32:10.123456+00:00", warszawa))
        assertEquals("15.01.2026 09:00", formatujDate("2026-01-15T08:00:00+00:00", warszawa))
        // Nieznany format pokazujemy tak, jak przyszedł
        assertEquals("wczoraj", formatujDate("wczoraj", warszawa))
    }
}
