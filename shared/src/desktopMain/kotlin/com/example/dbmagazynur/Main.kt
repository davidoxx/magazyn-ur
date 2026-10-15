package com.example.dbmagazynur

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.dbmagazynur.ui.DyspozytorWstecz
import com.example.dbmagazynur.ui.LocalDyspozytorWstecz
import com.example.dbmagazynur.ui.MagazynAplikacja
import java.awt.Dimension

// Program na Windows: te same ekrany co na telefonie, w oknie o proporcjach telefonu
// (można je powiększyć). Esc działa jak „Wstecz”.
fun main() = application {
    val dyspozytor = remember { DyspozytorWstecz() }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Magazyn UR",
        state = rememberWindowState(width = 480.dp, height = 880.dp),
        // onKeyEvent, a nie onPreviewKeyEvent: menu i okienka dostają Esc pierwsze
        onKeyEvent = { zdarzenie ->
            zdarzenie.type == KeyEventType.KeyDown && zdarzenie.key == Key.Escape && dyspozytor.wstecz()
        },
    ) {
        window.minimumSize = Dimension(380, 560)
        CompositionLocalProvider(LocalDyspozytorWstecz provides dyspozytor) {
            MagazynAplikacja()
        }
    }
}
