package com.example.dbmagazynur.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dbmagazynur.zasoby.Res
import com.example.dbmagazynur.zasoby.ic_more_vert
import org.jetbrains.compose.resources.painterResource
import com.example.dbmagazynur.data.formatujDate
import com.example.dbmagazynur.data.odmiana
import com.example.dbmagazynur.materialy.formatujIlosc
import com.example.dbmagazynur.sap.WydanieDoSap
import com.example.dbmagazynur.sesja.emailNaLogin
import com.example.dbmagazynur.ui.theme.DBMagazynURTheme

// Lista lidera: zaznacza wydania wpisane już do SAP i oznacza je jako zaksięgowane
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WydaniaSapScreen(
    wydania: List<WydanieDoSap>?,
    wczytywanie: Boolean,
    blad: String?,
    zaznaczone: Set<Long>,
    trwa: Boolean,
    komunikat: String?,
    onKomunikatPokazany: () -> Unit,
    onOdswiez: () -> Unit,
    onPrzelacz: (Long) -> Unit,
    onZaznaczWszystkie: (Boolean) -> Unit,
    onZaksieguj: () -> Unit,
    onAnuluj: (Long) -> Unit,
    onWstecz: () -> Unit,
    onWyloguj: () -> Unit,
) {
    var potwierdzKsiegowanie by rememberSaveable { mutableStateOf(false) }
    var anulujId by rememberSaveable { mutableStateOf<Long?>(null) }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(komunikat) {
        if (komunikat != null) {
            snackbar.showSnackbar(komunikat)
            onKomunikatPokazany()
        }
    }

    if (potwierdzKsiegowanie) {
        val n = zaznaczone.size
        AlertDialog(
            onDismissRequest = { potwierdzKsiegowanie = false },
            title = { Text("Zaksięgować $n ${odmiana(n, "wydanie", "wydania", "wydań")}?") },
            text = { Text("Oznacz tylko wydania wpisane już do SAP. Znikną z tej listy i nie da się tego cofnąć w aplikacji.") },
            confirmButton = {
                TextButton(onClick = {
                    potwierdzKsiegowanie = false
                    onZaksieguj()
                }) { Text("Zaksięguj") }
            },
            dismissButton = { TextButton(onClick = { potwierdzKsiegowanie = false }) { Text("Wróć") } },
        )
    }

    wydania?.find { it.id == anulujId }?.let { wydanie ->
        AlertDialog(
            onDismissRequest = { anulujId = null },
            title = { Text("Anulować wydanie?") },
            text = {
                Text(
                    "${formatujIlosc(wydanie.ilosc)} ${wydanie.jednostka} „${wydanie.material}” " +
                        "wróci na stan. Tego nie da się cofnąć.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    anulujId = null
                    onAnuluj(wydanie.id)
                }) { Text("Tak, anuluj wydanie") }
            },
            dismissButton = { TextButton(onClick = { anulujId = null }) { Text("Nie") } },
        )
    }

    Scaffold(
        topBar = { PasekZPowrotem("Do zaksięgowania w SAP", onWstecz, onWyloguj) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!wydania.isNullOrEmpty()) {
                BottomAppBar {
                    val wszystkie = zaznaczone.size == wydania.size
                    TextButton(onClick = { onZaznaczWszystkie(!wszystkie) }, enabled = !trwa) {
                        Text(if (wszystkie) "Odznacz wszystkie" else "Zaznacz wszystkie")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { potwierdzKsiegowanie = true },
                        enabled = zaznaczone.isNotEmpty() && !trwa,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        if (trwa) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = LocalContentColor.current,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Zaksięguj (${zaznaczone.size})")
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = wczytywanie && wydania != null,
            onRefresh = onOdswiez,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Wszystko w LazyColumn, żeby pociągnięcie w dół odświeżało też pusty ekran
            LazyColumn(Modifier.fillMaxSize()) {
                if (blad != null) {
                    item {
                        Column(Modifier.padding(16.dp)) {
                            Text(blad, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onOdswiez) { Text("Spróbuj ponownie") }
                        }
                    }
                }
                when {
                    wydania == null ->
                        if (blad == null) item { Podpowiedz("Wczytuję…") }
                    wydania.isEmpty() ->
                        item { Podpowiedz("Wszystkie wydania są zaksięgowane.") }
                    else ->
                        items(wydania, key = { it.id }) { wydanie ->
                            WierszWydania(
                                wydanie = wydanie,
                                zaznaczone = wydanie.id in zaznaczone,
                                aktywne = !trwa,
                                onPrzelacz = { onPrzelacz(wydanie.id) },
                                onAnuluj = { anulujId = wydanie.id },
                            )
                            HorizontalDivider()
                        }
                }
            }
        }
    }
}

@Composable
private fun WierszWydania(
    wydanie: WydanieDoSap,
    zaznaczone: Boolean,
    aktywne: Boolean,
    onPrzelacz: () -> Unit,
    onAnuluj: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val maszyna = when {
        wydanie.maszyna == null -> "maszyna: brak"
        wydanie.maszynaKod.isNullOrBlank() -> wydanie.maszyna
        else -> "${wydanie.maszynaKod} ${wydanie.maszyna}"
    }

    ListItem(
        leadingContent = {
            Checkbox(checked = zaznaczone, onCheckedChange = { onPrzelacz() }, enabled = aktywne)
        },
        headlineContent = {
            Text(
                "${formatujIlosc(wydanie.ilosc)} ${wydanie.jednostka} · ${wydanie.material}",
                fontWeight = FontWeight.SemiBold,
            )
        },
        supportingContent = {
            Column {
                Text("SAP ${wydanie.nrSap} → $maszyna")
                Text("${formatujDate(wydanie.utworzono)} · ${emailNaLogin(wydanie.wydal)}")
                wydanie.uwagi?.takeIf { it.isNotBlank() }?.let { Text("Uwagi: $it") }
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menu = true }, enabled = aktywne) {
                    Icon(painterResource(Res.drawable.ic_more_vert), contentDescription = "Więcej")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Anuluj wydanie") },
                        onClick = {
                            menu = false
                            onAnuluj()
                        },
                    )
                }
            }
        },
        modifier = Modifier.clickable(enabled = aktywne, onClick = onPrzelacz),
    )
}

@Preview(showBackground = true)
@Composable
private fun WydaniaSapScreenPreview() {
    DBMagazynURTheme {
        WydaniaSapScreen(
            wydania = listOf(
                WydanieDoSap(
                    1, "2026-10-08T12:32:10+00:00", "10023456", "Śruba imbusowa M8x20", 4.0, "szt",
                    "jan.kowalski@magazyn.local", maszynaKod = "PL01-L2-PR02", maszyna = "Prasa krawędziowa 2",
                ),
                WydanieDoSap(
                    2, "2026-10-08T13:05:00+00:00", "10031177", "Łożysko 6204 2RS", 2.0, "szt",
                    "anna.nowak@magazyn.local", uwagi = "Awaria łożyska",
                    maszynaKod = "PL01-L1-TK01", maszyna = "Tokarka CNC",
                ),
            ),
            wczytywanie = false,
            blad = null,
            zaznaczone = setOf(1L),
            trwa = false,
            komunikat = null,
            onKomunikatPokazany = {},
            onOdswiez = {},
            onPrzelacz = {},
            onZaznaczWszystkie = {},
            onZaksieguj = {},
            onAnuluj = {},
            onWstecz = {},
            onWyloguj = {},
        )
    }
}
