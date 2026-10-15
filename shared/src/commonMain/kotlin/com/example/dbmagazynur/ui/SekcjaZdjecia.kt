package com.example.dbmagazynur.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.example.dbmagazynur.data.BUCKET_ZDJEC
import io.github.jan.supabase.storage.authenticatedStorageItem

// Zdjęcie na karcie materiału: podgląd, powiększenie, wybór nowego (zależnie od platformy) i usuwanie
@Composable
fun SekcjaZdjecia(
    sciezka: String?,
    trwa: Boolean,
    onWybrano: (suspend () -> ByteArray) -> Unit,
    onUsun: () -> Unit,
    onBlad: (String) -> Unit,
) {
    var pelnyEkran by rememberSaveable { mutableStateOf(false) }
    var potwierdzUsuniecie by rememberSaveable { mutableStateOf(false) }

    if (sciezka != null) {
        val model = authenticatedStorageItem(BUCKET_ZDJEC, sciezka)
        SubcomposeAsyncImage(
            model = model,
            contentDescription = "Zdjęcie materiału",
            contentScale = ContentScale.Fit,
            loading = { Wysrodkowane { CircularProgressIndicator() } },
            error = {
                Wysrodkowane {
                    Text("Nie udało się wczytać zdjęcia", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { pelnyEkran = true },
        )

        if (pelnyEkran) {
            Dialog(
                onDismissRequest = { pelnyEkran = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                AsyncImage(
                    model = model,
                    contentDescription = "Zdjęcie materiału",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable { pelnyEkran = false },
                )
            }
        }
    }

    if (potwierdzUsuniecie) {
        AlertDialog(
            onDismissRequest = { potwierdzUsuniecie = false },
            title = { Text("Usunąć zdjęcie?") },
            confirmButton = {
                TextButton(onClick = {
                    potwierdzUsuniecie = false
                    onUsun()
                }) { Text("Usuń") }
            },
            dismissButton = { TextButton(onClick = { potwierdzUsuniecie = false }) { Text("Wróć") } },
        )
    }

    Column {
        Row {
            PrzyciskiZdjecia(
                maZdjecie = sciezka != null,
                enabled = !trwa,
                onWybrano = onWybrano,
                onBlad = onBlad,
            )
            if (sciezka != null) {
                TextButton(onClick = { potwierdzUsuniecie = true }, enabled = !trwa) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (trwa) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun Wysrodkowane(tresc: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { tresc() }
}
