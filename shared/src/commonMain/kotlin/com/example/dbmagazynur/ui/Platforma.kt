package com.example.dbmagazynur.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry

// To, co różni telefon z Androidem od programu na Windows.
// Implementacje: androidMain/…/Platforma.android.kt i desktopMain/…/Platforma.desktop.kt

// „Wstecz”: na Androidzie przycisk albo gest systemowy, na Windows klawisz Esc
@Composable
expect fun Wstecz(onBack: () -> Unit)

// Tekst do schowka w formacie danej platformy
expect fun wpisDoSchowka(tekst: String): ClipEntry

// Od Androida 13 system sam pokazuje, że coś skopiowano; na Windows trzeba to zrobić samemu
expect val systemPokazujeSkopiowanie: Boolean

// Przyciski wyboru nowego zdjęcia: na telefonie aparat i galeria, na Windows plik z dysku.
// onWybrano dostaje funkcję zwracającą zdjęcie gotowe do wysłania (zmniejszony JPEG);
// wywołuje się ją poza wątkiem głównym.
@Composable
expect fun PrzyciskiZdjecia(
    maZdjecie: Boolean,
    enabled: Boolean,
    onWybrano: (suspend () -> ByteArray) -> Unit,
    onBlad: (String) -> Unit,
)
