package com.example.dbmagazynur.maszyny

import kotlinx.serialization.Serializable

// Wiersz tabeli maszyny: cel wydania (maszyna / miejsce funkcjonalne)
@Serializable
data class Maszyna(
    val id: Long,
    val nazwa: String,
    val kod: String? = null,
    val aktywna: Boolean = true,
)

fun Maszyna.opis(): String = if (kod.isNullOrBlank()) nazwa else "$nazwa ($kod)"
