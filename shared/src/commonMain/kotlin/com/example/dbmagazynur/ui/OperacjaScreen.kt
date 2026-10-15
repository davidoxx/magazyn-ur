package com.example.dbmagazynur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dbmagazynur.maszyny.Maszyna
import com.example.dbmagazynur.maszyny.opis
import com.example.dbmagazynur.materialy.Material
import com.example.dbmagazynur.materialy.Operacja
import com.example.dbmagazynur.materialy.TypMaterialu
import com.example.dbmagazynur.materialy.formatujIlosc
import com.example.dbmagazynur.ui.theme.DBMagazynURTheme

// Formularz wydania albo przyjęcia jednego materiału
@Composable
fun OperacjaScreen(
    operacja: Operacja,
    material: Material,
    iloscTekst: String,
    uwagi: String,
    maszyna: Maszyna?,
    trwa: Boolean,
    blad: String?,
    onIloscChange: (String) -> Unit,
    onUwagiChange: (String) -> Unit,
    onWybierzMaszyne: () -> Unit,
    onUsunMaszyne: () -> Unit,
    onWykonaj: () -> Unit,
    onWstecz: () -> Unit,
    onWyloguj: () -> Unit,
) {
    val poleIlosci = remember { FocusRequester() }
    LaunchedEffect(Unit) { poleIlosci.requestFocus() }

    Scaffold(topBar = { PasekZPowrotem(operacja.tytul, onWstecz, onWyloguj) }) { innerPadding ->
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
            Column {
                Text(material.nazwa, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Na stanie: ${formatujIlosc(material.ilosc)} ${material.jednostka}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = iloscTekst,
                onValueChange = onIloscChange,
                label = { Text("Ilość") },
                suffix = { Text(material.jednostka) },
                singleLine = true,
                enabled = !trwa,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(poleIlosci),
            )

            if (operacja == Operacja.WYDANIE) {
                PoleMaszyny(
                    maszyna = maszyna,
                    wymagana = material.typ == TypMaterialu.SAP,
                    enabled = !trwa,
                    onWybierz = onWybierzMaszyne,
                    onUsun = onUsunMaszyne,
                )
            }

            OutlinedTextField(
                value = uwagi,
                onValueChange = onUwagiChange,
                label = { Text("Uwagi (opcjonalnie)") },
                enabled = !trwa,
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            blad?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = onWykonaj,
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
                    Text(operacja.przycisk)
                }
            }
        }
    }
}

@Composable
private fun PoleMaszyny(
    maszyna: Maszyna?,
    wymagana: Boolean,
    enabled: Boolean,
    onWybierz: () -> Unit,
    onUsun: () -> Unit,
) {
    OutlinedCard(onClick = onWybierz, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            overlineContent = { Text(if (wymagana) "Maszyna (wymagana)" else "Maszyna (opcjonalnie)") },
            headlineContent = { Text(maszyna?.opis() ?: "Wybierz maszynę") },
            trailingContent = if (maszyna != null) {
                { TextButton(onClick = onUsun, enabled = enabled) { Text("Usuń") } }
            } else {
                null
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OperacjaScreenPreview() {
    DBMagazynURTheme {
        OperacjaScreen(
            operacja = Operacja.WYDANIE,
            material = Material(
                1, "Śruba imbusowa M8x20", typ = TypMaterialu.SAP, nrSap = "10023456",
                miejsce = "R1-A3", ilosc = 120.0, jednostka = "szt",
            ),
            iloscTekst = "4",
            uwagi = "",
            maszyna = Maszyna(3, "Prasa krawędziowa 2", kod = "PL01-L2-PR02"),
            trwa = false,
            blad = null,
            onIloscChange = {},
            onUwagiChange = {},
            onWybierzMaszyne = {},
            onUsunMaszyne = {},
            onWykonaj = {},
            onWstecz = {},
            onWyloguj = {},
        )
    }
}
