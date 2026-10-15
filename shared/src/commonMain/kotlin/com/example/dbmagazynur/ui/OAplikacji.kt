package com.example.dbmagazynur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dbmagazynur.Konfiguracja
import com.example.dbmagazynur.ui.theme.DBMagazynURTheme
import com.example.dbmagazynur.zasoby.Res
import com.example.dbmagazynur.zasoby.o_aplikacji_autor
import com.example.dbmagazynur.zasoby.o_aplikacji_dzial
import com.example.dbmagazynur.zasoby.o_aplikacji_kontakt
import com.example.dbmagazynur.zasoby.o_aplikacji_nazwa
import com.example.dbmagazynur.zasoby.o_aplikacji_opis
import com.example.dbmagazynur.zasoby.o_aplikacji_prawa
import org.jetbrains.compose.resources.stringResource

// Okienko „O aplikacji”. Treść jest w composeResources/values/o_aplikacji.xml,
// wersja w gradle.properties.
@Composable
fun OAplikacjiDialog(onZamknij: () -> Unit) {
    val autor = stringResource(Res.string.o_aplikacji_autor)
    val kontakt = stringResource(Res.string.o_aplikacji_kontakt)
    val dzial = stringResource(Res.string.o_aplikacji_dzial)
    val prawa = stringResource(Res.string.o_aplikacji_prawa)

    AlertDialog(
        onDismissRequest = onZamknij,
        title = { Text(stringResource(Res.string.o_aplikacji_nazwa)) },
        text = {
            // Zaznaczanie, żeby dało się skopiować np. adres e-mail
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Wersja ${Konfiguracja.WERSJA} (${Konfiguracja.KOD_WERSJI})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(Res.string.o_aplikacji_opis))
                    PoleOAplikacji("Autor", autor)
                    PoleOAplikacji("Kontakt", kontakt)
                    PoleOAplikacji("Dział", dzial)
                    if (prawa.isNotBlank()) {
                        Text(prawa, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onZamknij) { Text("Zamknij") } },
    )
}

@Composable
private fun PoleOAplikacji(etykieta: String, wartosc: String) {
    if (wartosc.isBlank()) return
    Column(Modifier) {
        Text(
            etykieta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(wartosc)
    }
}

@Preview
@Composable
private fun OAplikacjiDialogPreview() {
    DBMagazynURTheme {
        OAplikacjiDialog(onZamknij = {})
    }
}
