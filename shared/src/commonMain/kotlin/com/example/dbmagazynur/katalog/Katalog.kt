package com.example.dbmagazynur.katalog

import kotlinx.serialization.Serializable

// Katalog części z arkusza Excel, wbudowany w aplikację (assets/katalog.json).
// Plik tworzy narzedzia/katalog_z_excela.py. Działa bez logowania i bez internetu.
@Serializable
data class Katalog(
    val zrodlo: String,
    val utworzono: String,
    val pozycje: List<PozycjaKatalogu>,
)

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
