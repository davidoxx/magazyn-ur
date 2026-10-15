package com.example.dbmagazynur.materialy

import com.example.dbmagazynur.data.jsonBazy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MaterialTest {
    @Test
    fun iloscPoPolsku() {
        assertEquals("12", formatujIlosc(12.0))
        assertEquals("12,5", formatujIlosc(12.5))
        assertEquals("0,125", formatujIlosc(0.125))
        assertEquals("1250", formatujIlosc(1250.0))
    }

    @Test
    fun wierszZBazyZDodatkowymiKolumnami() {
        // Tak PostgREST zwraca wiersz z szukaj_materialow (numeric jako 12.500)
        val wiersz = """
            {"id":7,"nazwa":"Śruba imbusowa M8x20","opis":null,"typ":"SAP",
             "nr_sap":"10023456","miejsce":"R1-A3","ilosc":12.500,"jednostka":"szt",
             "zdjecie":null,"utworzyl":"6f1c0d2e-0000-0000-0000-000000000000",
             "utworzono":"2026-10-08T10:00:00+00:00"}
        """.trimIndent()

        val material = jsonBazy.decodeFromString<Material>(wiersz)

        assertEquals(7L, material.id)
        assertEquals(TypMaterialu.SAP, material.typ)
        assertEquals("10023456", material.nrSap)
        assertEquals(12.5, material.ilosc, 0.0)
        assertNull(material.opis)
    }
}
