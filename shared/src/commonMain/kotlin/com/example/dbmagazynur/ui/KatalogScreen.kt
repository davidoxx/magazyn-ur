package com.example.dbmagazynur.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dbmagazynur.katalog.LIMIT_KATALOGU
import com.example.dbmagazynur.katalog.PozycjaKatalogu
import com.example.dbmagazynur.katalog.WynikKatalogu
import com.example.dbmagazynur.ui.theme.DBMagazynURTheme
import kotlinx.coroutines.launch

// Katalog części z arkusza: działa bez logowania i bez internetu.
// onWyloguj = null, gdy nikt nie jest zalogowany.
@Composable
fun KatalogScreen(
    fraza: String,
    opis: String?,
    wynik: WynikKatalogu?,
    blad: String?,
    onFrazaChange: (String) -> Unit,
    onWstecz: () -> Unit,
    onWyloguj: (() -> Unit)?,
) {
    val klawiatura = LocalSoftwareKeyboardController.current
    val schowek = LocalClipboard.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun kopiuj(sap: String) {
        scope.launch {
            schowek.setClipEntry(wpisDoSchowka(sap))
            if (!systemPokazujeSkopiowanie) snackbar.showSnackbar("Skopiowano numer SAP $sap")
        }
    }

    Scaffold(
        topBar = { PasekZPowrotem("Katalog części", onWstecz, onWyloguj) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            OutlinedTextField(
                value = fraza,
                onValueChange = onFrazaChange,
                placeholder = { Text("Nazwa, numer SAP, miejsce albo dostawca") },
                singleLine = true,
                trailingIcon = if (fraza.isNotEmpty()) {
                    { TextButton(onClick = { onFrazaChange("") }) { Text("Wyczyść") } }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { klawiatura?.hide() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when {
                blad != null -> Text(
                    blad,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                opis == null -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Podpowiedz("Wczytuję katalog…")
                }
                wynik == null -> Column {
                    Podpowiedz(
                        "Wpisz nazwę, numer SAP (pełny albo końcówkę, np. 1000), miejsce albo dostawcę. " +
                            "Spacje, myślniki, kropki i polskie znaki nie mają znaczenia, " +
                            "drobne literówki też są wybaczane.",
                    )
                    Podpowiedz("Dotknij pozycji, żeby skopiować numer SAP.")
                    Podpowiedz("Katalog: $opis")
                }
                wynik.pozycje.isEmpty() -> Podpowiedz("Nic nie znaleziono.")
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(wynik.pozycje, key = { it.sap }) { pozycja ->
                        WierszKatalogu(pozycja, onClick = { kopiuj(pozycja.sap) })
                        HorizontalDivider()
                    }
                    if (wynik.wszystkich > wynik.pozycje.size) {
                        item {
                            Podpowiedz(
                                "Pokazano $LIMIT_KATALOGU z ${wynik.wszystkich}. Doprecyzuj wyszukiwanie.",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WierszKatalogu(pozycja: PozycjaKatalogu, onClick: () -> Unit) {
    val nazwa = pozycja.nazwa.ifBlank { pozycja.inneNazwy.firstOrNull().orEmpty() }
    val inneNazwy = pozycja.inneNazwy.filter { it != nazwa }

    ListItem(
        headlineContent = { Text(nazwa.ifBlank { "(bez nazwy)" }) },
        supportingContent = {
            Column {
                if (inneNazwy.isNotEmpty()) {
                    Text(
                        inneNazwy.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    buildList {
                        add("SAP ${pozycja.sap}")
                        if (pozycja.dostawcy.isNotEmpty()) add(pozycja.dostawcy.joinToString(", "))
                    }.joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            // Miejsce to główna informacja: gdzie leży część
            if (pozycja.miejsca.isEmpty()) {
                Text(
                    "brak\nmiejsca",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            } else {
                Text(
                    pozycja.miejsca.joinToString("\n"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = 128.dp),
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun KatalogScreenPreview() {
    DBMagazynURTheme {
        KatalogScreen(
            fraza = "ecorail2",
            opis = "Czesci zam 2508.xlsx z 08.10.2026 · 10 332 pozycji",
            wynik = WynikKatalogu(
                listOf(
                    PozycjaKatalogu(
                        "538000012100", "Zasilacz 1-fazowy EcoRail-2 10ADC 85135",
                        miejsca = listOf("F1"), dostawcy = listOf("Murrelektronic"),
                    ),
                    PozycjaKatalogu(
                        "538000002641", "Bezpiecznik 80A 115kA 500V WT-00",
                        miejsca = listOf("R5A2 F2"), inneNazwy = listOf("Bezpiecznik 80A 115kA 500V"),
                        dostawcy = listOf("ETI"),
                    ),
                    PozycjaKatalogu(
                        "538000011617", "Ramie 8UM404-00-117",
                        miejsca = listOf("S4F F1", "R101B F1"), dostawcy = listOf("Cloos"),
                    ),
                ),
                wszystkich = 3,
            ),
            blad = null,
            onFrazaChange = {},
            onWstecz = {},
            onWyloguj = null,
        )
    }
}
