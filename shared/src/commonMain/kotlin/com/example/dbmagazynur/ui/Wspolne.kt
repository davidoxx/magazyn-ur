package com.example.dbmagazynur.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dbmagazynur.zasoby.Res
import com.example.dbmagazynur.zasoby.ic_arrow_back
import org.jetbrains.compose.resources.painterResource

// Pasek ekranów podrzędnych: strzałka wstecz i zawsze dostępne „Wyloguj”
// (onWyloguj = null tylko na ekranach bez logowania, np. katalog części)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasekZPowrotem(tytul: String, onWstecz: () -> Unit, onWyloguj: (() -> Unit)?) {
    TopAppBar(
        title = { Text(tytul) },
        navigationIcon = {
            IconButton(onClick = onWstecz) {
                Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = "Wstecz")
            }
        },
        actions = {
            if (onWyloguj != null) TextButton(onClick = onWyloguj) { Text("Wyloguj") }
        },
    )
}

@Composable
fun Podpowiedz(tekst: String) {
    Text(
        tekst,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}
