package com.example.dbmagazynur.materialy

import com.example.dbmagazynur.data.jsonBazy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuchTest {
    @Test
    fun wierszZDolaczonymiOsobamiIMaszyna() {
        // Tak PostgREST zwraca wiersz dla KOLUMNY_HISTORII
        val wiersz = """
            {"id":12,"typ":"WYDANIE","ilosc":4.000,"uwagi":null,
             "utworzono":"2026-10-08T12:32:10.5+00:00","zaksiegowano_sap":null,"anulowano":null,
             "kto":{"nazwa":"jan.kowalski@magazyn.local"},"kto_ksiegowal":null,"kto_anulowal":null,
             "maszyna":{"id":3,"nazwa":"Prasa krawędziowa 2","kod":"PL01-L2-PR02"}}
        """.trimIndent()

        val ruch = jsonBazy.decodeFromString<Ruch>(wiersz)

        assertEquals(TypRuchu.WYDANIE, ruch.typ)
        assertEquals("jan.kowalski@magazyn.local", ruch.kto?.nazwa)
        assertEquals("PL01-L2-PR02", ruch.maszyna?.kod)
        assertNull(ruch.ktoKsiegowal)
    }

    @Test
    fun utworzenieZNumeremZamowienia() {
        val wiersz = """
            {"id":1,"typ":"UTWORZENIE","ilosc":0,"uwagi":null,"nr_zamowienia":"4500012345",
             "utworzono":"2026-10-08T12:32:10+00:00","zaksiegowano_sap":null,"anulowano":null,
             "kto":{"nazwa":"jan.kowalski@magazyn.local"},"kto_ksiegowal":null,"kto_anulowal":null,
             "maszyna":null}
        """.trimIndent()

        val ruch = jsonBazy.decodeFromString<Ruch>(wiersz)

        assertEquals(TypRuchu.UTWORZENIE, ruch.typ)
        assertEquals("4500012345", ruch.nrZamowienia)
        // Utworzenie nie czeka na zaksięgowanie, także przy materiale SAP
        assertNull(ruch.status(materialSap = true))
    }

    private fun ruch(
        typ: TypRuchu = TypRuchu.WYDANIE,
        zaksiegowano: String? = null,
        anulowano: String? = null,
    ) = Ruch(1, typ, 1.0, utworzono = "2026-10-08T12:00:00+00:00",
        zaksiegowanoSap = zaksiegowano, anulowano = anulowano)

    @Test
    fun status() {
        assertEquals(StatusRuchu.CZEKA_NA_SAP, ruch().status(materialSap = true))
        assertEquals(StatusRuchu.ZAKSIEGOWANY, ruch(zaksiegowano = "x").status(materialSap = true))
        assertEquals(StatusRuchu.ANULOWANY, ruch(anulowano = "x").status(materialSap = true))
        // Kosztowe i przyjęcia nie czekają na SAP
        assertNull(ruch().status(materialSap = false))
        assertNull(ruch(TypRuchu.PRZYJECIE).status(materialSap = true))
        assertEquals(StatusRuchu.ANULOWANY, ruch(anulowano = "x").status(materialSap = false))
    }
}
