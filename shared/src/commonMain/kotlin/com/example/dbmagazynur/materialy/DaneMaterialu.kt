package com.example.dbmagazynur.materialy

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Dane z formularza do zapisu w tabeli materialy. Budujemy JSON ręcznie:
// - wysyłamy tylko kolumny, które aplikacja może zapisywać (granty w bazie),
// - puste pola idą jako null, żeby dało się je wyczyścić przy edycji.
// iloscTekst = null przy edycji (stan zmieniają tylko wydaj i przyjmij).
fun daneMaterialu(
    nazwa: String,
    typ: TypMaterialu?,
    nrSap: String,
    miejsce: String,
    jednostka: String,
    opis: String,
    iloscTekst: String?,
): Result<JsonObject> {
    val czystaNazwa = nazwa.trim()
    if (czystaNazwa.isEmpty()) return blad("Wpisz nazwę")
    if (typ == null) return blad("Wybierz typ: SAP albo kosztowy")
    val czystyNrSap = nrSap.trim()
    if (typ == TypMaterialu.SAP && czystyNrSap.isEmpty()) return blad("Wpisz numer SAP")
    val czystaJednostka = jednostka.trim()
    if (czystaJednostka.isEmpty()) return blad("Wpisz jednostkę")
    val ilosc = iloscTekst?.takeIf { it.isNotBlank() }?.let {
        parsujIlosc(it, dopuscZero = true).getOrElse { e -> return Result.failure(e) }
    }

    return Result.success(
        buildJsonObject {
            put("nazwa", czystaNazwa)
            put("typ", typ.name)
            // Baza wymaga: SAP ma numer, kosztowy nie ma
            put("nr_sap", if (typ == TypMaterialu.SAP) czystyNrSap else null)
            put("miejsce", miejsce.trim().ifEmpty { null })
            put("jednostka", czystaJednostka)
            put("opis", opis.trim().ifEmpty { null })
            if (ilosc != null) put("ilosc", ilosc)
        },
    )
}

// Parametry funkcji dodaj_material: te same pola z przedrostkiem p_ i numer zamówienia.
// Nowy materiał idzie przez funkcję, żeby numer trafił do wpisu „utworzenie” w historii.
fun parametryNowegoMaterialu(dane: JsonObject, nrZamowienia: String): JsonObject =
    buildJsonObject {
        dane.forEach { (kolumna, wartosc) -> put("p_$kolumna", wartosc) }
        put("p_nr_zamowienia", nrZamowienia.trim().ifEmpty { null })
    }

private fun blad(komunikat: String): Result<JsonObject> =
    Result.failure(IllegalArgumentException(komunikat))
