package com.example.dbmagazynur.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dbmagazynur.materialy.TypMaterialu
import com.example.dbmagazynur.ui.theme.DBMagazynURTheme

private val CZESTE_JEDNOSTKI = listOf("szt", "m", "kg", "l", "kpl", "opak")

// Dodawanie (nowy = true) i edycja danych materiału. Stanu przy edycji
// nie ma, bo zmieniają go tylko wydania i przyjęcia.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularzMaterialuScreen(
    nowy: Boolean,
    nazwa: String,
    typ: TypMaterialu?,
    nrSap: String,
    miejsce: String,
    jednostka: String,
    iloscTekst: String,
    opis: String,
    trwa: Boolean,
    blad: String?,
    onNazwaChange: (String) -> Unit,
    onTypChange: (TypMaterialu) -> Unit,
    onNrSapChange: (String) -> Unit,
    onMiejsceChange: (String) -> Unit,
    onJednostkaChange: (String) -> Unit,
    onIloscChange: (String) -> Unit,
    onOpisChange: (String) -> Unit,
    onZapisz: () -> Unit,
    onWstecz: () -> Unit,
    onWyloguj: () -> Unit,
) {
    Scaffold(
        topBar = { PasekZPowrotem(if (nowy) "Nowy materiał" else "Edycja materiału", onWstecz, onWyloguj) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Typ", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    TypMaterialu.entries.forEachIndexed { i, wartosc ->
                        SegmentedButton(
                            selected = typ == wartosc,
                            onClick = { onTypChange(wartosc) },
                            enabled = !trwa,
                            shape = SegmentedButtonDefaults.itemShape(i, TypMaterialu.entries.size),
                        ) {
                            Text(if (wartosc == TypMaterialu.SAP) "SAP" else "Kosztowy")
                        }
                    }
                }
            }

            if (typ == TypMaterialu.SAP) {
                OutlinedTextField(
                    value = nrSap,
                    onValueChange = onNrSapChange,
                    label = { Text("Numer SAP") },
                    singleLine = true,
                    enabled = !trwa,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = miejsce,
                onValueChange = onMiejsceChange,
                label = { Text("Miejsce magazynowe (opcjonalnie)") },
                singleLine = true,
                enabled = !trwa,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = jednostka,
                    onValueChange = onJednostkaChange,
                    label = { Text("Jednostka") },
                    singleLine = true,
                    enabled = !trwa,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    CZESTE_JEDNOSTKI.forEach { j ->
                        FilterChip(
                            selected = jednostka.trim() == j,
                            onClick = { onJednostkaChange(j) },
                            enabled = !trwa,
                            label = { Text(j) },
                        )
                    }
                }
            }

            if (nowy) {
                OutlinedTextField(
                    value = iloscTekst,
                    onValueChange = onIloscChange,
                    label = { Text("Stan początkowy (opcjonalnie)") },
                    suffix = { Text(jednostka.trim()) },
                    singleLine = true,
                    enabled = !trwa,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = opis,
                onValueChange = onOpisChange,
                label = { Text("Opis (opcjonalnie)") },
                enabled = !trwa,
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            blad?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = onZapisz,
                enabled = !trwa,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (trwa) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(if (nowy) "Dodaj materiał" else "Zapisz zmiany")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FormularzMaterialuScreenPreview() {
    DBMagazynURTheme {
        FormularzMaterialuScreen(
            nowy = true,
            nazwa = "Śruba imbusowa M8x20",
            typ = TypMaterialu.SAP,
            nrSap = "10023456",
            miejsce = "R1-A3",
            jednostka = "szt",
            iloscTekst = "",
            opis = "",
            trwa = false,
            blad = null,
            onNazwaChange = {},
            onTypChange = {},
            onNrSapChange = {},
            onMiejsceChange = {},
            onJednostkaChange = {},
            onIloscChange = {},
            onOpisChange = {},
            onZapisz = {},
            onWstecz = {},
            onWyloguj = {},
        )
    }
}
