package com.example.dbmagazynur.data

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMAT_DATY = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

// Polska odmiana po liczbie: 1 wydanie, 2 wydania, 5 wydań, 22 wydania
fun odmiana(liczba: Int, jeden: String, kilka: String, wiele: String): String = when {
    liczba == 1 -> jeden
    liczba % 10 in 2..4 && liczba % 100 !in 12..14 -> kilka
    else -> wiele
}

// Data z bazy (timestamptz, np. "2026-10-08T12:32:10.123+00:00") w czasie telefonu
fun formatujDate(zBazy: String, strefa: ZoneId = ZoneId.systemDefault()): String =
    runCatching { OffsetDateTime.parse(zBazy).atZoneSameInstant(strefa).format(FORMAT_DATY) }
        .getOrDefault(zBazy)
