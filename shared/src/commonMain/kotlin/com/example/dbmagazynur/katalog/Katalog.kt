package com.example.dbmagazynur.katalog

import com.example.dbmagazynur.data.jsonBazy
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.Serializable

// Katalog części z arkusza Excel, wbudowany w aplikację (composeResources/files/katalog.json).
// Plik tworzy narzedzia/katalog_z_excela.py. Działa bez logowania i bez internetu.
// Nowszy wgrywa administrator do Supabase (bucket „katalog”), aplikacja pobiera go po zalogowaniu.
@Serializable
data class Katalog(
    val zrodlo: String,
    // Data utworzenia pliku, RRRR-MM-DD
    val utworzono: String,
    val pozycje: List<PozycjaKatalogu>,
)

// Pusty albo zły plik (np. wgrany przez pomyłkę) nie może zastąpić działającego katalogu
fun odczytajKatalog(bajty: ByteArray): Katalog {
    val katalog = jsonBazy.decodeFromString<Katalog>(bajty.decodeToString())
    require(katalog.pozycje.isNotEmpty()) { "Katalog bez pozycji" }
    return katalog
}

// Nowszy z dwóch według daty utworzenia; przy tej samej dacie pierwszy
// (pobrany z Supabase, bo wgrany po zbudowaniu aplikacji)
fun nowszyKatalog(pierwszy: Katalog?, drugi: Katalog?): Katalog? = when {
    pierwszy == null -> drugi
    drugi != null && drugi.utworzono > pierwszy.utworzono -> drugi
    else -> pierwszy
}

private val FORMAT_DATY_KATALOGU = DateTimeFormatter.ofPattern("dd.MM.yyyy")

// Np. „Czesci zam 2508.xlsx z 08.10.2026 · 10 332 pozycji”
fun opisKatalogu(katalog: Katalog): String {
    val data = runCatching { LocalDate.parse(katalog.utworzono).format(FORMAT_DATY_KATALOGU) }.getOrDefault(katalog.utworzono)
    val liczba = "%,d".format(Locale.forLanguageTag("pl-PL"), katalog.pozycje.size)
    return "${katalog.zrodlo} z $data · $liczba pozycji"
}

@Serializable
data class PozycjaKatalogu(
    val sap: String,
    // Krótki tekst z SAP (arkusz Data)
    val nazwa: String = "",
    // Zwykle jedno, ale ten sam numer bywa w kilku miejscach
    val miejsca: List<String> = emptyList(),
    // Pełniejsze nazwy z arkuszy Magazyn
    val inneNazwy: List<String> = emptyList(),
    val dostawcy: List<String> = emptyList(),
)

data class WynikKatalogu(val pozycje: List<PozycjaKatalogu>, val wszystkich: Int)
