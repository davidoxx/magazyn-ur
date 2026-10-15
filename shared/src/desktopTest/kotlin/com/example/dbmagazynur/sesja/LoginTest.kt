package com.example.dbmagazynur.sesja

import org.junit.Assert.assertEquals
import org.junit.Test

class LoginTest {
    @Test
    fun loginDostajeDomeneMagazynu() {
        assertEquals("jan.kowalski@magazyn.local", loginNaEmail("  Jan.Kowalski "))
    }

    @Test
    fun pelnyEmailZostajeBezDomeny() {
        assertEquals("admin@firma.pl", loginNaEmail(" Admin@Firma.pl"))
    }

    @Test
    fun emailNaLoginUsuwaTylkoDomeneMagazynu() {
        assertEquals("jan.kowalski", emailNaLogin("jan.kowalski@magazyn.local"))
        assertEquals("admin@firma.pl", emailNaLogin("admin@firma.pl"))
    }
}
