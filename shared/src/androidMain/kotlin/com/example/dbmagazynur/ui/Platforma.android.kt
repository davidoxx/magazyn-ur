package com.example.dbmagazynur.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalContext
import com.example.dbmagazynur.data.plikNaZdjecieZAparatu
import com.example.dbmagazynur.data.przygotujZdjecie

@Composable
actual fun Wstecz(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}

actual fun wpisDoSchowka(tekst: String): ClipEntry = ClipEntry(ClipData.newPlainText("Numer SAP", tekst))

actual val systemPokazujeSkopiowanie: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@Composable
actual fun PrzyciskiZdjecia(
    maZdjecie: Boolean,
    enabled: Boolean,
    onWybrano: (suspend () -> ByteArray) -> Unit,
    onBlad: (String) -> Unit,
) {
    val context = LocalContext.current
    // Kontekst aplikacji: przetwarzanie zdjęcia może trwać dłużej niż życie aktywności
    val resolver = context.applicationContext.contentResolver

    val aparat = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { zrobione ->
        if (zrobione) {
            val uri = plikNaZdjecieZAparatu(context)
            onWybrano { przygotujZdjecie(resolver, uri) }
        }
    }
    // Systemowy wybór zdjęć: bez uprawnień do całej galerii
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onWybrano { przygotujZdjecie(resolver, uri) }
    }

    TextButton(
        onClick = {
            try {
                aparat.launch(plikNaZdjecieZAparatu(context))
            } catch (_: ActivityNotFoundException) {
                onBlad("Brak aplikacji aparatu na tym telefonie")
            }
        },
        enabled = enabled,
    ) { Text(if (maZdjecie) "Nowe zdjęcie" else "Zrób zdjęcie") }
    TextButton(
        onClick = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        enabled = enabled,
    ) { Text("Z galerii") }
}
