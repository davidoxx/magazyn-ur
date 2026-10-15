package com.example.dbmagazynur.uzytkownicy

import com.example.dbmagazynur.data.bladZFunkcji
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UzytkownikTest {
    private fun blad(login: String) = normalizujLogin(login).exceptionOrNull()?.message

    @Test
    fun loginZnormalizowanyJakPrzyLogowaniu() {
        assertEquals("jan.kowalski", normalizujLogin("  Jan.Kowalski ").getOrThrow())
        assertEquals("a_b-2", normalizujLogin("a_b-2").getOrThrow())
    }

    @Test
    fun bledneLoginy() {
        assertEquals("Wpisz login", blad("  "))
        assertEquals("Wpisz sam login, bez @ i domeny", blad("jan@magazyn.local"))
        val zlyZnak = "Login: 2–50 znaków, małe litery bez polskich znaków, cyfry, kropka, myślnik lub podkreślenie"
        assertEquals(zlyZnak, blad("łukasz"))
        assertEquals(zlyZnak, blad("jan kowalski"))
        assertEquals(zlyZnak, blad(".jan"))
        assertEquals(zlyZnak, blad("j"))
        assertEquals(zlyZnak, blad("a".repeat(51)))
    }

    @Test
    fun aktywniNajpierwAlfabetycznie() {
        val posortowani = posortujUzytkownikow(
            listOf(
                Uzytkownik("1", "zenon", 1),
                Uzytkownik("2", "anna@magazyn.local", 2),
                Uzytkownik("3", "bartek", 0),
                Uzytkownik("4", "łucja", 3),
            ),
        )
        assertEquals(listOf("2", "4", "1", "3"), posortowani.map { it.id })
    }

    @Test
    fun komunikatZFunkcji() {
        assertEquals("Taki login już istnieje", bladZFunkcji("""{"blad":"Taki login już istnieje"}"""))
        // Odpowiedź bramki Supabase, a nie naszej funkcji
        assertNull(bladZFunkcji("""{"code":"NOT_FOUND","message":"Requested function was not found"}"""))
        assertNull(bladZFunkcji("Internal Server Error"))
    }
}
