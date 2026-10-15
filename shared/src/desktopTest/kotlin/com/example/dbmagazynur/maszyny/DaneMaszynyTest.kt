package com.example.dbmagazynur.maszyny

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class DaneMaszynyTest {
    @Test
    fun walidacjaIPusteKody() {
        assertEquals("Wpisz nazwę maszyny", daneMaszyny("  ", "X1", true).exceptionOrNull()?.message)

        val json = daneMaszyny(" Tokarka CNC ", "  ", aktywna = false).getOrThrow()
        assertEquals(JsonPrimitive("Tokarka CNC"), json["nazwa"])
        // Pusty kod jako null, bo kolumna kod jest unikalna
        assertEquals(JsonNull, json["kod"])
        assertEquals(JsonPrimitive(false), json["aktywna"])
    }

    @Test
    fun aktywneNajpierwPotemAlfabetyczniePoPolsku() {
        val posortowane = posortujMaszyny(
            listOf(
                Maszyna(1, "Żuraw", aktywna = true),
                Maszyna(2, "Śrutownica", aktywna = true),
                Maszyna(3, "Tokarka", aktywna = true),
                Maszyna(4, "Agregat", aktywna = false),
                Maszyna(5, "Spawarka", aktywna = true),
            ),
        )
        assertEquals(listOf("Spawarka", "Śrutownica", "Tokarka", "Żuraw", "Agregat"), posortowane.map { it.nazwa })
    }
}
