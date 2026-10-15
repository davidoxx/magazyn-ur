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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.dbmagazynur.maszyny.Maszyna
import com.example.dbmagazynur.ui.theme.DBMagazynURTheme

// Lista maszyn do zarządzania (poziom 3)
@Composable
fun MaszynyScreen(
    lista: List<Maszyna>?,
    wczytywanie: Boolean,
    blad: String?,
    komunikat: String?,
    onKomunikatPokazany: () -> Unit,
    onPonow: () -> Unit,
    onDodaj: () -> Unit,
    onEdytuj: (Maszyna) -> Unit,
    onWstecz: () -> Unit,
    onWyloguj: () -> Unit,
) {
    var fraza by rememberSaveable { mutableStateOf("") }
    val widoczne = remember(lista, fraza) {
        lista.orEmpty().filter { pasujeDoFrazy("${it.nazwa} ${it.kod.orEmpty()}", fraza) }
    }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(komunikat) {
        if (komunikat != null) {
            snackbar.showSnackbar(komunikat)
            onKomunikatPokazany()
        }
    }

    Scaffold(
        topBar = { PasekZPowrotem("Maszyny", onWstecz, onWyloguj) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onDodaj) { Text("Dodaj maszynę") }
        },
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
                onValueChange = { fraza = it },
                placeholder = { Text("Nazwa albo kod maszyny") },
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
                lista.isEmpty() -> Podpowiedz("Lista maszyn jest pusta. Dodaj pierwszą maszynę.")
                widoczne.isEmpty() -> Podpowiedz("Nic nie znaleziono.")
                else ->
                    // Dolny margines, żeby przycisk „Dodaj maszynę” nie zasłaniał ostatniej pozycji
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                        items(widoczne, key = { it.id }) { maszyna ->
                            ListItem(
                                headlineContent = { Text(maszyna.nazwa) },
                                supportingContent = maszyna.kod?.let { kod -> { Text(kod) } },
                                trailingContent = if (!maszyna.aktywna) {
                                    { Text("wycofana", color = MaterialTheme.colorScheme.error) }
                                } else {
                                    null
                                },
                                modifier = Modifier.clickable { onEdytuj(maszyna) },
                            )
                            HorizontalDivider()
                        }
                    }
            }
        }
    }
}

// Okienko dodawania i edycji maszyny, z usuwaniem dla maszyn bez historii
@Composable
fun MaszynaDialog(
    nowa: Boolean,
    nazwa: String,
    kod: String,
    aktywna: Boolean,
    potwierdzUsuniecie: Boolean,
    trwa: Boolean,
    blad: String?,
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
            title = { Text("Usunąć maszynę?") },
            text = {
                Text(
                    "„$nazwa” zniknie z listy. Usunąć da się tylko maszynę bez wydań, " +
                        "maszynę z historią trzeba wycofać.",
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
        title = { Text(if (nowa) "Nowa maszyna" else "Edycja maszyny") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            "Wycofanej nie da się wybrać przy wydaniu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = aktywna, onCheckedChange = onAktywnaChange, enabled = !trwa)
                }
                blad?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!nowa) {
                    TextButton(onClick = { onPotwierdzUsuniecie(true) }, enabled = !trwa) {
                        Text("Usuń maszynę", color = MaterialTheme.colorScheme.error)
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
private fun MaszynyScreenPreview() {
    DBMagazynURTheme {
        MaszynyScreen(
            lista = listOf(
                Maszyna(1, "Prasa krawędziowa 2", kod = "PL01-L2-PR02"),
                Maszyna(2, "Tokarka CNC", kod = "PL01-L1-TK01"),
                Maszyna(3, "Wiertarka kolumnowa", aktywna = false),
            ),
            wczytywanie = false,
            blad = null,
            komunikat = null,
            onKomunikatPokazany = {},
            onPonow = {},
            onDodaj = {},
            onEdytuj = {},
            onWstecz = {},
            onWyloguj = {},
        )
    }
}
