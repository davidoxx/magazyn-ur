package com.example.dbmagazynur.stacje

import com.example.dbmagazynur.data.jsonBazy
import com.example.dbmagazynur.materialy.Material
import com.example.dbmagazynur.materialy.parametryNowegoMaterialu
import com.example.dbmagazynur.materialy.zmianyStacji
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StacjaTest {
    private fun blad(wynik: Result<*>) = wynik.exceptionOrNull()?.message

    @Test
    fun daneStacji() {
        assertEquals("Wybierz F1, F2 albo F3", blad(daneStacji(null, "Stacja 10", "", true)))
        assertEquals("Wybierz F1, F2 albo F3", blad(daneStacji("F4", "Stacja 10", "", true)))
        assertEquals("Wpisz nazwę stacji", blad(daneStacji("F1", "  ", "", true)))
        val json = daneStacji("F2", " Stacja 10 ", "  ", false).getOrThrow()
        assertEquals(JsonPrimitive("F2"), json["obszar"])
        assertEquals(JsonPrimitive("Stacja 10"), json["nazwa"])
        // Pusty kod jako null: kolumna jest unikalna
        assertEquals("null", json["kod"].toString())
        assertEquals(JsonPrimitive(false), json["aktywna"])
    }

    @Test
    fun sortowanieIOpis() {
        val stacje = listOf(
            Stacja(1, "F2", "Prasa"),
            Stacja(2, "F1", "Zgrzewarka"),
            Stacja(3, "F1", "Stara", aktywna = false),
            Stacja(4, "F1", "Śrubowanie", kod = "PL01-F1-ST40"),
        )
        // Obszar, aktywne najpierw, „Ś” po „S”, a nie za „Z”
        assertEquals(listOf(4L, 2L, 3L, 1L), posortujStacje(stacje).map { it.id })
        assertEquals("F1 · Śrubowanie (PL01-F1-ST40)", stacje[3].opis())
        assertEquals("F2 · Prasa", stacje[0].opis())
    }

    @Test
    fun materialZeStacjami() {
        // Tak PostgREST zwraca materiał dla KOLUMNY_MATERIALU
        val wiersz = """
            {"id":5,"nazwa":"Chwytak","typ":"KOSZTOWY","ilosc":2,"jednostka":"szt",
             "stacje":[{"id":1,"obszar":"F1","nazwa":"Stacja 10","kod":null,"aktywna":true}]}
        """.trimIndent()
        val material = jsonBazy.decodeFromString<Material>(wiersz)
        assertEquals("Stacja 10", material.stacje.single().nazwa)
        // Wyniki wyszukiwania nie dołączają stacji
        assertEquals(emptyList<Stacja>(), jsonBazy.decodeFromString<Material>("""{"id":5,"nazwa":"X","typ":"SAP","ilosc":0,"jednostka":"szt"}""").stacje)
    }

    @Test
    fun parametryNowegoMaterialuZeStacjami() {
        val dane = JsonObject(mapOf("nazwa" to JsonPrimitive("Chwytak")))
        assertEquals(JsonArray(listOf(JsonPrimitive(1L), JsonPrimitive(2L))), parametryNowegoMaterialu(dane, "", listOf(1, 2, 1))["p_stacje"])
        // Bez stacji parametr pominięty (domyślny w bazie)
        assertNull(parametryNowegoMaterialu(dane, "")["p_stacje"])
    }

    @Test
    fun zmianyStacjiTylkoRoznice() {
        val zmiany = zmianyStacji(7, przed = listOf(1, 2), po = listOf(2, 3))!!
        assertEquals(JsonPrimitive(7L), zmiany["p_material_id"])
        assertEquals(JsonArray(listOf(JsonPrimitive(3L))), zmiany["p_dodaj"])
        assertEquals(JsonArray(listOf(JsonPrimitive(1L))), zmiany["p_usun"])
        // Bez zmian (także inna kolejność) nie ma wywołania
        assertNull(zmianyStacji(7, przed = listOf(1, 2), po = listOf(2, 1)))
    }
}
