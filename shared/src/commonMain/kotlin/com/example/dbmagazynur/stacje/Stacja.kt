package com.example.dbmagazynur.stacje

import kotlinx.serialization.Serializable

// Obszary, na które dzielą się stacje (w bazie: check obszar in ('F1', 'F2', 'F3'))
val OBSZARY = listOf("F1", "F2", "F3")

// Wiersz tabeli stacje: lokalizacja funkcjonalna, do której materiał jest przeznaczony.
// Osobna lista od maszyn, na które idą wydania.
@Serializable
data class Stacja(
    val id: Long,
    val obszar: String,
    val nazwa: String,
    val kod: String? = null,
    val aktywna: Boolean = true,
)

// Kolumny stacji dołączanych do materiału: "*,stacje($KOLUMNY_STACJI)" (przez materialy_stacje)
const val KOLUMNY_STACJI = "id,obszar,nazwa,kod,aktywna"

// „Stacja 10 (PL01-F1-ST10)”
fun Stacja.nazwaZKodem(): String = if (kod.isNullOrBlank()) nazwa else "$nazwa ($kod)"

// „F1 · Stacja 10 (PL01-F1-ST10)”
fun Stacja.opis(): String = "$obszar · ${nazwaZKodem()}"
