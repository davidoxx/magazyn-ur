package com.example.dbmagazynur.materialy

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DaneMaterialuTest {
    private fun dane(
        nazwa: String = "Śruba M8",
        typ: TypMaterialu? = TypMaterialu.SAP,
        nrSap: String = "10023456",
        miejsce: String = "",
        jednostka: String = "szt",
        opis: String = "",
        iloscTekst: String? = null,
    ) = daneMaterialu(nazwa, typ, nrSap, miejsce, jednostka, opis, iloscTekst)

    private fun blad(wynik: Result<*>) = wynik.exceptionOrNull()?.message

    @Test
    fun wymaganePola() {
        assertEquals("Wpisz nazwę", blad(dane(nazwa = "  ")))
        assertEquals("Wybierz typ: SAP albo kosztowy", blad(dane(typ = null)))
        assertEquals("Wpisz numer SAP", blad(dane(nrSap = " ")))
        assertEquals("Wpisz jednostkę", blad(dane(jednostka = "")))
        assertEquals("Najwyżej 3 miejsca po przecinku", blad(dane(iloscTekst = "1,0001")))
    }

    @Test
    fun kosztowyBezNumeruSap() {
        // Numer wpisany przed zmianą typu na kosztowy nie trafia do bazy
        val json = dane(typ = TypMaterialu.KOSZTOWY, nrSap = "10023456").getOrThrow()
        assertEquals(JsonNull, json["nr_sap"])
        assertEquals(JsonPrimitive("KOSZTOWY"), json["typ"])
    }

    @Test
    fun pustePolaJakoNullIBezIlosciPrzyEdycji() {
        val json = dane(nazwa = "  Śruba M8 ", miejsce = " ", opis = "").getOrThrow()
        assertEquals(JsonPrimitive("Śruba M8"), json["nazwa"])
        // null, a nie brak pola: inaczej przy edycji nie dałoby się wyczyścić opisu
        assertEquals(JsonNull, json["miejsce"])
        assertEquals(JsonNull, json["opis"])
        assertFalse("ilosc" in json)
    }

    @Test
    fun stanPoczatkowy() {
        assertEquals(JsonPrimitive(2.5.toBigDecimal()), dane(iloscTekst = "2,5").getOrThrow()["ilosc"])
        assertEquals(JsonPrimitive(0.toBigDecimal()), dane(iloscTekst = "0").getOrThrow()["ilosc"])
        assertFalse("ilosc" in dane(iloscTekst = "  ").getOrThrow())
    }

    @Test
    fun parametryNowegoMaterialu() {
        val json = parametryNowegoMaterialu(dane(iloscTekst = "5").getOrThrow(), nrZamowienia = " 4500012345 ")
        assertEquals(JsonPrimitive("Śruba M8"), json["p_nazwa"])
        assertEquals(JsonPrimitive("10023456"), json["p_nr_sap"])
        assertEquals(JsonNull, json["p_miejsce"])
        assertEquals(JsonPrimitive("4500012345"), json["p_nr_zamowienia"])
        assertEquals(5.0, json["p_ilosc"].toString().toDouble(), 0.0)
        // Tylko parametry funkcji dodaj_material
        assertEquals(emptyList<String>(), json.keys.filterNot { it.startsWith("p_") })
        assertEquals(JsonNull, parametryNowegoMaterialu(dane().getOrThrow(), "  ")["p_nr_zamowienia"])
    }
}
