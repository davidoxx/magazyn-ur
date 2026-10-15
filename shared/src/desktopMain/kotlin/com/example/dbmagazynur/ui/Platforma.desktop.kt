package com.example.dbmagazynur.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import com.example.dbmagazynur.data.przygotujZdjecie
import java.awt.FileDialog
import java.awt.Frame
import java.awt.datatransfer.StringSelection
import java.io.File

// „Wstecz” na Windows: klawisz Esc obsługuje ostatnio otwarty ekran (Main.kt)
class DyspozytorWstecz {
    private val obslugi = mutableListOf<() -> Unit>()

    fun dodaj(obsluga: () -> Unit) {
        obslugi += obsluga
    }

    fun usun(obsluga: () -> Unit) {
        obslugi -= obsluga
    }

    // Zwraca true, jeśli któryś ekran obsłużył „Wstecz”
    fun wstecz(): Boolean {
        val ostatnia = obslugi.lastOrNull() ?: return false
        ostatnia()
        return true
    }
}

val LocalDyspozytorWstecz = staticCompositionLocalOf<DyspozytorWstecz?> { null }

@Composable
actual fun Wstecz(onBack: () -> Unit) {
    val dyspozytor = LocalDyspozytorWstecz.current ?: return
    val aktualna by rememberUpdatedState(onBack)
    DisposableEffect(dyspozytor) {
        val obsluga = { aktualna() }
        dyspozytor.dodaj(obsluga)
        onDispose { dyspozytor.usun(obsluga) }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
actual fun wpisDoSchowka(tekst: String): ClipEntry = ClipEntry(StringSelection(tekst))

actual val systemPokazujeSkopiowanie: Boolean = false

@Composable
actual fun PrzyciskiZdjecia(
    maZdjecie: Boolean,
    enabled: Boolean,
    onWybrano: (suspend () -> ByteArray) -> Unit,
    onBlad: (String) -> Unit,
) {
    TextButton(
        onClick = {
            val okno = FileDialog(null as Frame?, "Wybierz zdjęcie materiału", FileDialog.LOAD).apply {
                // Na Windows filtr plików ustawia się wzorcem nazwy
                file = "*.jpg;*.jpeg;*.png;*.bmp;*.gif"
                isVisible = true // czeka, aż użytkownik wybierze plik albo zamknie okno
            }
            val nazwa = okno.file
            if (nazwa != null) {
                val plik = File(okno.directory, nazwa)
                onWybrano { przygotujZdjecie(plik) }
            }
        },
        enabled = enabled,
    ) { Text(if (maZdjecie) "Nowe zdjęcie z dysku" else "Dodaj zdjęcie z dysku") }
}
