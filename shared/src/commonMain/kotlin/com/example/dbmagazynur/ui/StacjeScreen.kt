package com.example.dbmagazynur.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dbmagazynur.data.pasujeDoFrazy
import com.example.dbmagazynur.stacje.OBSZARY
import com.example.dbmagazynur.stacje.Stacja
import com.example.dbmagazynur.ui.theme.DBMagazynURTheme

// Lista lokalizacji funkcjonalnych do zarządzania (poziom 2+), osobno dla F1, F2 i F3
@Composable
fun StacjeScreen(
    lista: List<Stacja>?,
    obszar: String,
    wczytywanie: Boolean,
    blad: String?,
    komunikat: String?,
    onObszar: (String) -> Unit,
    onKomunikatPokazany: () -> Unit,
    onPonow: () -> Unit,
    onDodaj: () -> Unit,
    onEdytuj: (Stacja) -> Unit,
    onWstecz: () -> Unit,
    onWyloguj: () -> Unit,
) {
    var fraza by rememberSaveable { mutableStateOf("") }
    val widoczne = remember(lista, obszar, fraza) {
        lista.orEmpty().filter { it.obszar == obszar && pasujeDoFrazy("${it.nazwa} ${it.kod.orEmpty()}", fraza) }
    }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(komunikat) {
        if (komunikat != null) {
            snackbar.showSnackbar(komunikat)
            onKomunikatPokazany()
        }
    }

    Scaffold(
        topBar = { PasekZPowrotem("Lokalizacje funkcjonalne", onWstecz, onWyloguj) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onDodaj) { Text("Dodaj stację") }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
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
            if (wczytywanie) LinearProgressIndicator(Modifier.fillMaxWidth())

            when {
                blad != null && lista == null ->
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(blad, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onPonow) { Text("Spróbuj ponownie") }
                    }
                lista == null -> {}
                lista.none { it.obszar == obszar } -> Podpowiedz("W $obszar nie ma jeszcze stacji. Dodaj pierwszą.")
                widoczne.isEmpty() -> Podpowiedz("Nic nie znaleziono.")
                else ->
                    // Dolny margines, żeby przycisk „Dodaj stację” nie zasłaniał ostatniej pozycji
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                        items(widoczne, key = { it.id }) { stacja ->
                            ListItem(
                                headlineContent = { Text(stacja.nazwa) },
                                supportingContent = stacja.kod?.let { kod -> { Text(kod) } },
                                trailingContent = if (!stacja.aktywna) {
                                    { Text("wycofana", color = MaterialTheme.colorScheme.error) }
                                } else {
                                    null
                                },
                                modifier = Modifier.clickable { onEdytuj(stacja) },
                            )
                            HorizontalDivider()
                        }
                    }
            }
        }
    }
}

// Przełącznik F1 / F2 / F3
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WyborObszaru(
    obszar: String?,
    onObszar: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        OBSZARY.forEachIndexed { i, wartosc ->
            SegmentedButton(
                selected = obszar == wartosc,
                onClick = { onObszar(wartosc) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(i, OBSZARY.size),
            ) { Text(wartosc) }
        }
    }
}

// Okienko dodawania i edycji stacji, z usuwaniem dla stacji nieprzypisanych do materiałów
@Composable
fun StacjaDialog(
    nowa: Boolean,
    obszar: String?,
    nazwa: String,
    kod: String,
    aktywna: Boolean,
    potwierdzUsuniecie: Boolean,
    trwa: Boolean,
    blad: String?,
    onObszarChange: (String) -> Unit,
    onNazwaChange: (String) -> Unit,
    onKodChange: (String) -> Unit,
    onAktywnaChange: (Boolean) -> Unit,
    onPotwierdzUsuniecie: (Boolean) -> Unit,
    onZapisz: () -> Unit,
    onUsun: () -> Unit,
    onZamknij: () -> Unit,
) {
    if (potwierdzUsuniecie) {
        AlertDialog(
            onDismissRequest = { if (!trwa) onPotwierdzUsuniecie(false) },
            title = { Text("Usunąć stację?") },
            text = {
                Text(
                    "„$nazwa” zniknie z listy. Usunąć da się tylko stację nieprzypisaną do materiałów, " +
                        "przypisaną trzeba wycofać.",
                )
            },
            confirmButton = { TextButton(onClick = onUsun, enabled = !trwa) { Text("Usuń") } },
            dismissButton = {
                TextButton(onClick = { onPotwierdzUsuniecie(false) }, enabled = !trwa) { Text("Wróć") }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = { if (!trwa) onZamknij() },
        title = { Text(if (nowa) "Nowa stacja" else "Edycja stacji") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WyborObszaru(obszar = obszar, onObszar = onObszarChange, enabled = !trwa)
                OutlinedTextField(
                    value = nazwa,
                    onValueChange = onNazwaChange,
                    label = { Text("Nazwa") },
                    singleLine = true,
                    enabled = !trwa,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = kod,
                    onValueChange = onKodChange,
                    label = { Text("Kod w SAP (opcjonalnie)") },
                    singleLine = true,
                    enabled = !trwa,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Aktywna")
                        Text(
                            "Wycofanej nie da się przypisać do kolejnych materiałów",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = aktywna, onCheckedChange = onAktywnaChange, enabled = !trwa)
                }
                blad?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!nowa) {
                    TextButton(onClick = { onPotwierdzUsuniecie(true) }, enabled = !trwa) {
                        Text("Usuń stację", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onZapisz, enabled = !trwa) { Text(if (nowa) "Dodaj" else "Zapisz") }
        },
        dismissButton = { TextButton(onClick = onZamknij, enabled = !trwa) { Text("Anuluj") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun StacjeScreenPreview() {
    DBMagazynURTheme {
        StacjeScreen(
            lista = listOf(
                Stacja(1, "F1", "Stacja 10 – zgrzewanie", kod = "PL01-F1-ST10"),
                Stacja(2, "F1", "Stacja 20 – klejenie"),
                Stacja(3, "F1", "Stacja 30", aktywna = false),
                Stacja(4, "F2", "Prasa 1"),
            ),
            obszar = "F1",
            wczytywanie = false,
            blad = null,
            komunikat = null,
            onObszar = {},
            onKomunikatPokazany = {},
            onPonow = {},
            onDodaj = {},
            onEdytuj = {},
            onWstecz = {},
            onWyloguj = {},
        )
    }
}
