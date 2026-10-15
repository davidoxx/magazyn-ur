package com.example.dbmagazynur.maszyny

import java.text.Collator
import java.util.Locale
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Dane z formularza do zapisu w tabeli maszyny (tylko kolumny z grantów).
// Pusty kod idzie jako null: kolumna jest unikalna, a nulli może być wiele.
fun daneMaszyny(nazwa: String, kod: String, aktywna: Boolean): Result<JsonObject> {
    val czystaNazwa = nazwa.trim()
    if (czystaNazwa.isEmpty()) return Result.failure(IllegalArgumentException("Wpisz nazwę maszyny"))
    return Result.success(
        buildJsonObject {
            put("nazwa", czystaNazwa)
            put("kod", kod.trim().ifEmpty { null })
            put("aktywna", aktywna)
        },
    )
}

// Aktywne najpierw, potem alfabetycznie po polsku (baza sortuje „Ś” za „Z”)
fun posortujMaszyny(maszyny: List<Maszyna>): List<Maszyna> {
    val polski = Collator.getInstance(Locale.forLanguageTag("pl-PL"))
    return maszyny.sortedWith(compareBy<Maszyna> { !it.aktywna }.thenBy(polski) { it.nazwa })
}
