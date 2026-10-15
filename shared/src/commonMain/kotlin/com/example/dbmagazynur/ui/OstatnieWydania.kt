package com.example.dbmagazynur.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.dbmagazynur.data.formatujDate
import com.example.dbmagazynur.materialy.Material
import com.example.dbmagazynur.materialy.StatusRuchu
import com.example.dbmagazynur.materialy.TypMaterialu
import com.example.dbmagazynur.materialy.formatujIlosc
import com.example.dbmagazynur.sesja.emailNaLogin
import com.example.dbmagazynur.wydania.FiltrWydan
import com.example.dbmagazynur.wydania.OstatnieWydanie
import com.example.dbmagazynur.wydania.StanOstatnichWydan

// Sekcja „Ostatnie wydania” w liście ekranu głównego
@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.ostatnieWydania(
    stan: StanOstatnichWydan,
    onFiltr: (FiltrWydan) -> Unit,
    onOdswiez: () -> Unit,
    onStarsze: () -> Unit,
    onMaterial: (Material) -> Unit,
) {
    item(key = "naglowek-wydan") {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ostatnie wydania", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onOdswiez, enabled = !stan.wczytywanie) { Text("Odśwież") }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                FiltrWydan.entries.forEachIndexed { i, filtr ->
                    SegmentedButton(
                        selected = stan.filtr == filtr,
                        onClick = { onFiltr(filtr) },
                        shape = SegmentedButtonDefaults.itemShape(i, FiltrWydan.entries.size),
                    ) { Text(filtr.etykieta) }
                }
            }
        }
    }

    item(key = "stan-wydan") {
        Column(Modifier.padding(top = 8.dp)) {
            if (stan.wczytywanie) LinearProgressIndicator(Modifier.fillMaxWidth())
            stan.blad?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (stan.wydania?.isEmpty() == true) Podpowiedz("Brak wydań.")
        }
    }

    items(stan.wydania.orEmpty(), key = { "wydanie-${it.id}" }) { wydanie ->
        WierszOstatniegoWydania(wydanie, onClick = { onMaterial(wydanie.material) })
        HorizontalDivider()
    }

    if (stan.saStarsze) {
        item(key = "starsze-wydania") {
            TextButton(
                onClick = onStarsze,
                enabled = !stan.wczytywanie,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) { Text("Pokaż starsze") }
        }
    }
}

@Composable
private fun WierszOstatniegoWydania(wydanie: OstatnieWydanie, onClick: () -> Unit) {
    val status = wydanie.status
    val anulowane = status == StatusRuchu.ANULOWANY
    val material = wydanie.material

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            "−${formatujIlosc(wydanie.ilosc)} ${material.jednostka}",
            style = MaterialTheme.typography.titleMedium,
            color = if (anulowane) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (anulowane) TextDecoration.LineThrough else null,
            modifier = Modifier.width(88.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(material.nazwa)
            if (wydanie.maszyna != null) {
                Text(
                    "→ " + if (wydanie.maszynaKod.isNullOrBlank()) wydanie.maszyna else "${wydanie.maszyna} (${wydanie.maszynaKod})",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                listOf(
                    formatujDate(wydanie.utworzono),
                    emailNaLogin(wydanie.wydal),
                    if (material.typ == TypMaterialu.SAP) "SAP ${material.nrSap}" else "kosztowy",
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            wydanie.uwagi?.takeIf { it.isNotBlank() }?.let {
                Text("Uwagi: $it", style = MaterialTheme.typography.bodySmall)
            }
            when (status) {
                StatusRuchu.ANULOWANY -> Text(
                    "Anulowane",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                StatusRuchu.ZAKSIEGOWANY -> Text(
                    "Zaksięgowane w SAP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusRuchu.CZEKA_NA_SAP -> Text(
                    "Czeka na zaksięgowanie w SAP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                null -> {}
            }
        }
    }
}
