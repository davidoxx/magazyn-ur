package com.example.dbmagazynur.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dbmagazynur.data.pasujeDoFrazy
import com.example.dbmagazynur.maszyny.Maszyna
import com.example.dbmagazynur.ui.theme.DBMagazynURTheme

// Wybór maszyny do wydania. Lista jest krótka, więc filtrujemy ją na telefonie.
@Composable
fun WyborMaszynyScreen(
    maszyny: List<Maszyna>?,
    blad: String?,
    onPonow: () -> Unit,
    onWybierz: (Maszyna) -> Unit,
    onWstecz: () -> Unit,
    onWyloguj: () -> Unit,
) {
    var fraza by rememberSaveable { mutableStateOf("") }
    val widoczne = remember(maszyny, fraza) {
        maszyny.orEmpty().filter { pasujeDoFrazy("${it.nazwa} ${it.kod.orEmpty()}", fraza) }
    }

    Scaffold(topBar = { PasekZPowrotem("Wybierz maszynę", onWstecz, onWyloguj) }) { innerPadding ->
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

            when {
                blad != null && maszyny == null ->
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(blad, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onPonow) { Text("Spróbuj ponownie") }
                    }
                maszyny == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                maszyny.isEmpty() ->
                    Podpowiedz("Lista maszyn jest pusta. Maszyny dodaje administrator.")
                widoczne.isEmpty() -> Podpowiedz("Nic nie znaleziono.")
                else ->
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(widoczne, key = { it.id }) { maszyna ->
                            ListItem(
                                headlineContent = { Text(maszyna.nazwa) },
                                supportingContent = maszyna.kod?.let { kod -> { Text(kod) } },
                                modifier = Modifier.clickable { onWybierz(maszyna) },
                            )
                            HorizontalDivider()
                        }
                    }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WyborMaszynyScreenPreview() {
    DBMagazynURTheme {
        WyborMaszynyScreen(
            maszyny = listOf(
                Maszyna(1, "Prasa krawędziowa 2", kod = "PL01-L2-PR02"),
                Maszyna(2, "Tokarka CNC", kod = "PL01-L1-TK01"),
                Maszyna(3, "Wózek widłowy"),
            ),
            blad = null,
            onPonow = {},
            onWybierz = {},
            onWstecz = {},
            onWyloguj = {},
        )
    }
}
