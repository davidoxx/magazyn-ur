package com.example.dbmagazynur.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dbmagazynur.sesja.emailNaLogin
import com.example.dbmagazynur.ui.theme.DBMagazynURTheme
import com.example.dbmagazynur.uzytkownicy.POZIOMY
import com.example.dbmagazynur.uzytkownicy.Uzytkownik
import com.example.dbmagazynur.uzytkownicy.nazwaPoziomu
import com.example.dbmagazynur.uzytkownicy.opisPoziomu

// Lista kont (poziom 3)
@Composable
fun UzytkownicyScreen(
    lista: List<Uzytkownik>?,
    mojeId: String?,
    wczytywanie: Boolean,
    blad: String?,
    komunikat: String?,
    onKomunikatPokazany: () -> Unit,
    onPonow: () -> Unit,
    onDodaj: () -> Unit,
    onWybierz: (Uzytkownik) -> Unit,
    onWstecz: () -> Unit,
    onWyloguj: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(komunikat) {
        if (komunikat != null) {
            snackbar.showSnackbar(komunikat)
            onKomunikatPokazany()
        }
    }

    Scaffold(
        topBar = { PasekZPowrotem("Użytkownicy", onWstecz, onWyloguj) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onDodaj) { Text("Załóż konto") }
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (wczytywanie) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                blad != null && lista == null ->
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(blad, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onPonow) { Text("Spróbuj ponownie") }
                    }
                lista == null -> {}
                else ->
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                        items(lista, key = { it.id }) { uzytkownik ->
                            ListItem(
                                headlineContent = { Text(emailNaLogin(uzytkownik.nazwa)) },
                                supportingContent = {
                                    Text(
                                        nazwaPoziomu(uzytkownik.poziom),
                                        color = if (uzytkownik.poziom == 0) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                },
                                trailingContent = if (uzytkownik.id == mojeId) {
                                    { Text("Ty", color = MaterialTheme.colorScheme.primary) }
                                } else {
                                    null
                                },
                                modifier = Modifier.clickable { onWybierz(uzytkownik) },
                            )
                            HorizontalDivider()
                        }
                    }
            }
        }
    }
}

@Composable
fun NoweKontoDialog(
    login: String,
    haslo: String,
    poziom: Int,
    trwa: Boolean,
    blad: String?,
    onLoginChange: (String) -> Unit,
    onHasloChange: (String) -> Unit,
    onPoziomChange: (Int) -> Unit,
    onZaloz: () -> Unit,
    onZamknij: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!trwa) onZamknij() },
        title = { Text("Nowe konto") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = login,
                    onValueChange = onLoginChange,
                    label = { Text("Login") },
                    suffix = { Text("@magazyn.local") },
                    singleLine = true,
                    enabled = !trwa,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
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
                    modifier = Modifier.fillMaxWidth(),
                )
                WyborPoziomu(poziom, enabled = !trwa, onPoziomChange)
                blad?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (trwa) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = onZaloz, enabled = !trwa) { Text("Załóż") } },
        dismissButton = { TextButton(onClick = onZamknij, enabled = !trwa) { Text("Anuluj") } },
    )
}

@Composable
fun EdycjaUzytkownikaDialog(
    uzytkownik: Uzytkownik,
    toJa: Boolean,
    poziom: Int,
    haslo: String,
    trwa: Boolean,
    blad: String?,
    onPoziomChange: (Int) -> Unit,
    onHasloChange: (String) -> Unit,
    onZapiszPoziom: () -> Unit,
    onUstawHaslo: () -> Unit,
    onZamknij: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!trwa) onZamknij() },
        title = { Text(emailNaLogin(uzytkownik.nazwa)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                if (toJa) {
                    Text(
                        "To Twoje konto. Własnego poziomu nie zmienisz, " +
                            "a hasło zmienisz w menu „Zmień hasło”.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                WyborPoziomu(poziom, enabled = !trwa && !toJa, onPoziomChange)
                OutlinedButton(
                    onClick = onZapiszPoziom,
                    enabled = !trwa && !toJa && poziom != uzytkownik.poziom,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Zapisz poziom") }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                PoleHasla(
                    value = haslo,
                    onValueChange = onHasloChange,
                    label = "Nowe hasło",
                    enabled = !trwa && !toJa,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = onUstawHaslo,
                    enabled = !trwa && !toJa,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Ustaw nowe hasło") }

                blad?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (trwa) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = onZamknij, enabled = !trwa) { Text("Zamknij") } },
    )
}

@Composable
private fun WyborPoziomu(poziom: Int, enabled: Boolean, onPoziomChange: (Int) -> Unit) {
    Column(Modifier.selectableGroup()) {
        Text("Poziom", style = MaterialTheme.typography.labelLarge)
        POZIOMY.forEach { p ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = poziom == p,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onPoziomChange(p) },
                    )
                    .padding(vertical = 4.dp),
            ) {
                RadioButton(selected = poziom == p, onClick = null, enabled = enabled)
                Column(Modifier.padding(start = 8.dp)) {
                    Text("$p – ${nazwaPoziomu(p)}")
                    Text(
                        opisPoziomu(p),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UzytkownicyScreenPreview() {
    DBMagazynURTheme {
        UzytkownicyScreen(
            lista = listOf(
                Uzytkownik("1", "admin@firma.pl", 3),
                Uzytkownik("2", "anna.nowak", 2),
                Uzytkownik("3", "jan.kowalski", 1),
                Uzytkownik("4", "piotr.zielinski", 0),
            ),
            mojeId = "1",
            wczytywanie = false,
            blad = null,
            komunikat = null,
            onKomunikatPokazany = {},
            onPonow = {},
            onDodaj = {},
            onWybierz = {},
            onWstecz = {},
            onWyloguj = {},
        )
    }
}
