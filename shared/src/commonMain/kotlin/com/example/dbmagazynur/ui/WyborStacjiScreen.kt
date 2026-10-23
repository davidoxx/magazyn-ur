package com.example.dbmagazynur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dbmagazynur.data.odmiana
import com.example.dbmagazynur.data.pasujeDoFrazy
import com.example.dbmagazynur.stacje.OBSZARY
import com.example.dbmagazynur.stacje.Stacja
import com.example.dbmagazynur.ui.theme.DBMagazynURTheme

// Wybór lokalizacji funkcjonalnych materiału: najpierw F1 / F2 / F3, potem lista stacji
// z wybranego obszaru. Zaznaczenie od razu dodaje stację do formularza, ponowne ją usuwa.
@Composable
fun WyborStacjiScreen(
    obszar: String?,
    // Aktywne stacje wybranego obszaru (null = wczytywanie)
    stacje: List<Stacja>?,
    wybrane: List<Stacja>,
    blad: String?,
    onObszar: (String) -> Unit,
    onPonow: () -> Unit,
    onPrzelacz: (Stacja) -> Unit,
    onWstecz: () -> Unit,
    onGotowe: () -> Unit,
    onWyloguj: () -> Unit,
) {
    var fraza by rememberSaveable { mutableStateOf("") }
    val widoczne = remember(stacje, fraza) {
        stacje.orEmpty().filter { pasujeDoFrazy("${it.nazwa} ${it.kod.orEmpty()}", fraza) }
    }
    val wybraneId = remember(wybrane) { wybrane.mapTo(HashSet()) { it.id } }

    Scaffold(
        topBar = {
            PasekZPowrotem(if (obszar == null) "Lokalizacja funkcjonalna" else "Stacje $obszar", onWstecz, onWyloguj)
        },
        bottomBar = {
            Button(
                onClick = onGotowe,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                Text(
                    if (wybrane.isEmpty()) {
                        "Gotowe"
                    } else {
                        "Gotowe · ${wybrane.size} ${odmiana(wybrane.size, "stacja", "stacje", "stacji")}"
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            if (obszar == null) {
                WyborObszaruDuzy(wybrane = wybrane, onObszar = onObszar)
                return@Column
            }

            WyborObszaru(
                obszar = obszar,
                onObszar = onObszar,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
            OutlinedTextField(
                value = fraza,
                onValueChange = { fraza = it },
                placeholder = { Text("Nazwa albo kod stacji") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when {
                blad != null && stacje == null ->
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(blad, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onPonow) { Text("Spróbuj ponownie") }
                    }
                stacje == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                stacje.isEmpty() ->
                    Podpowiedz("W $obszar nie ma stacji. Stacje dodaje lider w menu „Lokalizacje funkcjonalne”.")
                widoczne.isEmpty() -> Podpowiedz("Nic nie znaleziono.")
                else ->
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(widoczne, key = { it.id }) { stacja ->
                            val zaznaczona = stacja.id in wybraneId
                            ListItem(
                                leadingContent = { Checkbox(checked = zaznaczona, onCheckedChange = null) },
                                headlineContent = { Text(stacja.nazwa) },
                                supportingContent = stacja.kod?.let { kod -> { Text(kod) } },
                                modifier = Modifier.toggleable(
                                    value = zaznaczona,
                                    onValueChange = { onPrzelacz(stacja) },
                                    role = Role.Checkbox,
                                ),
                            )
                            HorizontalDivider()
                        }
                    }
            }
        }
    }
}

// Pierwszy krok: duże przyciski F1, F2, F3 z liczbą już wybranych stacji
@Composable
private fun WyborObszaruDuzy(wybrane: List<Stacja>, onObszar: (String) -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OBSZARY.forEach { obszar ->
            val liczba = wybrane.count { it.obszar == obszar }
            OutlinedCard(onClick = { onObszar(obszar) }, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(obszar, style = MaterialTheme.typography.headlineSmall) },
                    supportingContent = if (liczba > 0) {
                        { Text("wybrane: $liczba") }
                    } else {
                        null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WyborStacjiScreenPreview() {
    val stacje = listOf(
        Stacja(1, "F1", "Stacja 10 – zgrzewanie", kod = "PL01-F1-ST10"),
        Stacja(2, "F1", "Stacja 20 – klejenie"),
        Stacja(3, "F1", "Stacja 30 – kontrola"),
    )
    DBMagazynURTheme {
        WyborStacjiScreen(
            obszar = "F1",
            stacje = stacje,
            wybrane = listOf(stacje[0]),
            blad = null,
            onObszar = {},
            onPonow = {},
            onPrzelacz = {},
            onWstecz = {},
            onGotowe = {},
            onWyloguj = {},
        )
    }
}
