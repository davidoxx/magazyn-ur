package com.example.dbmagazynur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.dbmagazynur.data.komunikat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Hasła trzymamy tylko w pamięci dialogu (bez rememberSaveable), żeby nie trafiły do zapisanego stanu
@Composable
fun ZmianaHaslaDialog(
    onZmienHaslo: suspend (String) -> Unit,
    onZamknij: () -> Unit,
) {
    var nowe by remember { mutableStateOf("") }
    var powtorzone by remember { mutableStateOf("") }
    var blad by remember { mutableStateOf<String?>(null) }
    var trwa by remember { mutableStateOf(false) }
    var zmieniono by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun zmien() {
        when {
            trwa -> return
            nowe.isEmpty() -> blad = "Wpisz nowe hasło"
            nowe != powtorzone -> blad = "Hasła się różnią"
            else -> scope.launch {
                trwa = true
                blad = null
                try {
                    onZmienHaslo(nowe)
                    zmieniono = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    blad = e.komunikat()
                } finally {
                    trwa = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!trwa) onZamknij() },
        title = { Text("Zmiana hasła") },
        text = {
            if (zmieniono) {
                Text("Hasło zostało zmienione.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PoleHasla(
                        value = nowe,
                        onValueChange = { nowe = it; blad = null },
                        label = "Nowe hasło",
                        enabled = !trwa,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PoleHasla(
                        value = powtorzone,
                        onValueChange = { powtorzone = it; blad = null },
                        label = "Powtórz nowe hasło",
                        enabled = !trwa,
                        imeAction = ImeAction.Done,
                        onDone = ::zmien,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    blad?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            if (zmieniono) {
                TextButton(onClick = onZamknij) { Text("OK") }
            } else {
                TextButton(onClick = ::zmien, enabled = !trwa) { Text("Zmień") }
            }
        },
        dismissButton = {
            if (!zmieniono) {
                TextButton(onClick = onZamknij, enabled = !trwa) { Text("Anuluj") }
            }
        },
    )
}
