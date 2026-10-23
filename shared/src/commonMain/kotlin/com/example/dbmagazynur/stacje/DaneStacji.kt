package com.example.dbmagazynur.stacje

import java.text.Collator
import java.util.Locale
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Dane z formularza do zapisu w tabeli stacje (tylko kolumny z grantów).
// Pusty kod idzie jako null: kolumna jest unikalna, a nulli może być wiele.
fun daneStacji(obszar: String?, nazwa: String, kod: String, aktywna: Boolean): Result<JsonObject> {
    if (obszar !in OBSZARY) return Result.failure(IllegalArgumentException("Wybierz F1, F2 albo F3"))
    val czystaNazwa = nazwa.trim()
    if (czystaNazwa.isEmpty()) return Result.failure(IllegalArgumentException("Wpisz nazwę stacji"))
    return Result.success(
        buildJsonObject {
            put("obszar", obszar)
            put("nazwa", czystaNazwa)
            put("kod", kod.trim().ifEmpty { null })
            put("aktywna", aktywna)
        },
    )
}

// Według obszaru, aktywne najpierw, potem alfabetycznie po polsku (baza sortuje „Ś” za „Z”)
fun posortujStacje(stacje: List<Stacja>): List<Stacja> {
    val polski = Collator.getInstance(Locale.forLanguageTag("pl-PL"))
    return stacje.sortedWith(
        compareBy<Stacja> { it.obszar }.thenBy { !it.aktywna }.thenBy(polski) { it.nazwa },
    )
}
