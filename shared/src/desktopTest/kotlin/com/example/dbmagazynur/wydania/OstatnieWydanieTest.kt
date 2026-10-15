package com.example.dbmagazynur.wydania

import com.example.dbmagazynur.data.jsonBazy
import com.example.dbmagazynur.materialy.StatusRuchu
import com.example.dbmagazynur.materialy.TypMaterialu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OstatnieWydanieTest {
    // Tak PostgREST zwraca wiersz widoku ostatnie_wydania: material to obiekt z to_jsonb
    private fun wiersz(typ: String, nrSap: String?, zaksiegowano: String? = null, anulowano: String? = null) = """
        {"id":41,"utworzono":"2026-10-09T06:15:00.12+00:00","ilosc":4.000,"uwagi":null,
         "zaksiegowano_sap":${zaksiegowano?.let { "\"$it\"" }},"anulowano":${anulowano?.let { "\"$it\"" }},
         "typ_materialu":"$typ",
         "material":{"id":7,"nazwa":"Śruba imbusowa M8x20","opis":null,"typ":"$typ",
                     "nr_sap":${nrSap?.let { "\"$it\"" }},"miejsce":"R1-A3","ilosc":116.000,
                     "jednostka":"szt","zdjecie":null,"utworzyl":null,"utworzono":"2026-10-01T08:00:00+00:00"},
         "wydal":"jan.kowalski@magazyn.local","maszyna_kod":"PL01-L2-PR02","maszyna":"Prasa krawędziowa 2"}
    """.trimIndent()

    private fun wydanie(json: String) = jsonBazy.decodeFromString<OstatnieWydanie>(json)

    @Test
    fun wierszZMaterialemDoOtwarciaKarty() {
        val w = wydanie(wiersz("SAP", "10023456"))
        assertEquals(7L, w.material.id)
        assertEquals(TypMaterialu.SAP, w.material.typ)
        assertEquals(116.0, w.material.ilosc, 0.0)
        assertEquals("Prasa krawędziowa 2", w.maszyna)
    }

    @Test
    fun status() {
        assertEquals(StatusRuchu.CZEKA_NA_SAP, wydanie(wiersz("SAP", "10023456")).status)
        assertEquals(StatusRuchu.ZAKSIEGOWANY, wydanie(wiersz("SAP", "10023456", zaksiegowano = "2026-10-09T07:00:00+00:00")).status)
        assertEquals(StatusRuchu.ANULOWANY, wydanie(wiersz("KOSZTOWY", null, anulowano = "2026-10-09T07:00:00+00:00")).status)
        assertNull(wydanie(wiersz("KOSZTOWY", null)).status)
    }
}
