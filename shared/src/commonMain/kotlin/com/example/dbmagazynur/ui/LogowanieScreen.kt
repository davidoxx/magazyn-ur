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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dbmagazynur.ui.theme.DBMagazynURTheme

@Composable
fun LogowanieScreen(
    login: String,
    haslo: String,
    blad: String?,
    trwa: Boolean,
    onLoginChange: (String) -> Unit,
    onHasloChange: (String) -> Unit,
    onZaloguj: () -> Unit,
    onKatalog: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Magazyn UR", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = login,
                onValueChange = onLoginChange,
                label = { Text("Login") },
                singleLine = true,
                enabled = !trwa,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            PoleHasla(
                value = haslo,
                onValueChange = onHasloChange,
                label = "Hasło",
                enabled = !trwa,
                imeAction = ImeAction.Done,
                onDone = onZaloguj,
                modifier = Modifier.fillMaxWidth(),
            )

            if (blad != null) {
                Text(blad, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = onZaloguj,
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
                    Text("Zaloguj")
                }
            }

            OutlinedButton(onClick = onKatalog, modifier = Modifier.fillMaxWidth()) {
                Text("Katalog części bez logowania")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LogowanieScreenPreview() {
    DBMagazynURTheme {
        LogowanieScreen(
            login = "jan.kowalski",
            haslo = "",
            blad = "Nieprawidłowy login lub hasło",
            trwa = false,
            onLoginChange = {},
            onHasloChange = {},
            onZaloguj = {},
            onKatalog = {},
        )
    }
}
