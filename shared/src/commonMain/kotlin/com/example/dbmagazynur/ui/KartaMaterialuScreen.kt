package com.example.dbmagazynur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dbmagazynur.data.formatujDate
import com.example.dbmagazynur.maszyny.Maszyna
import com.example.dbmagazynur.maszyny.opis
import com.example.dbmagazynur.materialy.KartaViewModel
import com.example.dbmagazynur.materialy.Material
import com.example.dbmagazynur.materialy.Operacja
import com.example.dbmagazynur.materialy.Osoba
import com.example.dbmagazynur.materialy.Ruch
import com.example.dbmagazynur.materialy.StatusRuchu
import com.example.dbmagazynur.materialy.TypMaterialu
import com.example.dbmagazynur.materialy.TypRuchu
import com.example.dbmagazynur.materialy.formatujIlosc
import com.example.dbmagazynur.materialy.status
import com.example.dbmagazynur.sesja.emailNaLogin
import com.example.dbmagazynur.ui.theme.DBMagazynURTheme

// Karta materiału wraz z formularzem operacji i wyborem maszyny (ekrany nakładane na siebie)
@Composable
fun KartaMaterialu(
    vm: KartaViewModel,
    onEdytuj: (Material) -> Unit,
    onZamknij: () -> Unit,
    onWyloguj: () -> Unit,
) {
    val material = vm.material
    val operacja = vm.operacja

    when {
        material != null && operacja != null && vm.wyborMaszyny -> {
            Wstecz(onBack = vm::zamknijWyborMaszyny)
            WyborMaszynyScreen(
                maszyny = vm.maszyny,
                blad = vm.bladMaszyn,
                onPonow = vm::wczytajMaszyny,
                onWybierz = vm::wybierzMaszyne,
                onWstecz = vm::zamknijWyborMaszyny,
                onWyloguj = onWyloguj,
            )
        }
        material != null && operacja != null -> {
            Wstecz(onBack = vm::zamknijOperacje)
            OperacjaScreen(
                operacja = operacja,
                material = material,
                iloscTekst = vm.iloscTekst,
                uwagi = vm.uwagi,
                maszyna = vm.maszyna,
                trwa = vm.operacjaTrwa,
                blad = vm.bladOperacji,
                onIloscChange = vm::zmienIlosc,
                onUwagiChange = vm::zmienUwagi,
                onWybierzMaszyne = vm::pokazWyborMaszyny,
                onUsunMaszyne = { vm.wybierzMaszyne(null) },
                onWykonaj = vm::wykonaj,
                onWstecz = vm::zamknijOperacje,
                onWyloguj = onWyloguj,
            )
        }
        else -> {
            Wstecz(onBack = onZamknij)
            KartaScreen(
                material = material,
                zdjecieTrwa = vm.zdjecieTrwa,
                onZdjecie = vm::ustawZdjecie,
                onUsunZdjecie = vm::usunZdjecie,
                onKomunikat = vm::pokazKomunikat,
                historia = vm.historia,
                saStarsze = vm.saStarsze,
                onWczytajStarsze = vm::wczytajStarsze,
                blad = vm.bladWczytania,
                komunikat = vm.komunikat,
                onKomunikatPokazany = vm::komunikatPokazany,
                onPonow = vm::wczytaj,
                onOperacja = vm::otworzOperacje,
                onEdytuj = onEdytuj,
                onWstecz = onZamknij,
                onWyloguj = onWyloguj,
            )
        }
    }
}

@Composable
private fun KartaScreen(
    material: Material?,
    zdjecieTrwa: Boolean,
    onZdjecie: (suspend () -> ByteArray) -> Unit,
    onUsunZdjecie: () -> Unit,
    onKomunikat: (String) -> Unit,
    historia: List<Ruch>?,
    saStarsze: Boolean,
    onWczytajStarsze: () -> Unit,
    blad: String?,
    komunikat: String?,
    onKomunikatPokazany: () -> Unit,
    onPonow: () -> Unit,
    onOperacja: (Operacja) -> Unit,
    onEdytuj: (Material) -> Unit,
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
        topBar = { PasekZPowrotem("Materiał", onWstecz, onWyloguj) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (blad != null) {
                Text(blad, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onPonow) { Text("Spróbuj ponownie") }
            }
            if (material == null) {
                if (blad == null) CircularProgressIndicator()
                return@Column
            }

            SekcjaZdjecia(
                sciezka = material.zdjecie,
                trwa = zdjecieTrwa,
                onWybrano = onZdjecie,
                onUsun = onUsunZdjecie,
                onBlad = onKomunikat,
            )
            Text(material.nazwa, style = MaterialTheme.typography.headlineSmall)
            Text(
                if (material.typ == TypMaterialu.SAP) "Materiał SAP · ${material.nrSap}" else "Materiał kosztowy",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PoleKarty("Miejsce", material.miejsce?.takeIf { it.isNotBlank() } ?: "—")
            PoleKarty("Stan", "${formatujIlosc(material.ilosc)} ${material.jednostka}", duze = true)
            material.opis?.takeIf { it.isNotBlank() }?.let { PoleKarty("Opis", it) }

            val brakNaStanie = material.ilosc <= 0.0
            if (brakNaStanie) {
                Text("Brak na stanie", color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onOperacja(Operacja.WYDANIE) },
                    enabled = !brakNaStanie,
                    modifier = Modifier.weight(1f),
                ) { Text("Wydaj") }
                OutlinedButton(
                    onClick = { onOperacja(Operacja.PRZYJECIE) },
                    modifier = Modifier.weight(1f),
                ) { Text("Przyjmij") }
            }
            TextButton(onClick = { onEdytuj(material) }) { Text("Edytuj dane materiału") }

            HorizontalDivider()
            Text("Historia", style = MaterialTheme.typography.titleMedium)
            when {
                historia == null -> if (blad == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                historia.isEmpty() -> Text(
                    "Brak wydań i przyjęć.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Column {
                    historia.forEach { ruch ->
                        WierszRuchu(ruch, material.jednostka, materialSap = material.typ == TypMaterialu.SAP)
                        HorizontalDivider()
                    }
                    if (saStarsze) {
                        TextButton(onClick = onWczytajStarsze) { Text("Pokaż starsze") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WierszRuchu(ruch: Ruch, jednostka: String, materialSap: Boolean) {
    val status = ruch.status(materialSap)
    val anulowany = status == StatusRuchu.ANULOWANY
    val znak = if (ruch.typ == TypRuchu.WYDANIE) "−" else "+"

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            "$znak${formatujIlosc(ruch.ilosc)} $jednostka",
            style = MaterialTheme.typography.titleMedium,
            color = when {
                anulowany -> MaterialTheme.colorScheme.onSurfaceVariant
                ruch.typ == TypRuchu.PRZYJECIE -> MaterialTheme.colorScheme.primary
                else -> Color.Unspecified
            },
            textDecoration = if (anulowany) TextDecoration.LineThrough else null,
            modifier = Modifier.width(96.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    ruch.typ == TypRuchu.PRZYJECIE -> "Przyjęcie"
                    ruch.maszyna != null -> "Wydanie → ${ruch.maszyna.opis()}"
                    else -> "Wydanie"
                },
            )
            Text(
                "${formatujDate(ruch.utworzono)} · ${ruch.kto?.nazwa?.let(::emailNaLogin) ?: "?"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ruch.uwagi?.takeIf { it.isNotBlank() }?.let {
                Text("Uwagi: $it", style = MaterialTheme.typography.bodySmall)
            }
            when (status) {
                StatusRuchu.ANULOWANY -> Text(
                    "Anulowane ${ruch.anulowano?.let(::formatujDate).orEmpty()}" +
                        (ruch.ktoAnulowal?.let { " · ${emailNaLogin(it.nazwa)}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                StatusRuchu.ZAKSIEGOWANY -> Text(
                    "Zaksięgowane w SAP ${ruch.zaksiegowanoSap?.let(::formatujDate).orEmpty()}" +
                        (ruch.ktoKsiegowal?.let { " · ${emailNaLogin(it.nazwa)}" } ?: ""),
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

@Composable
private fun PoleKarty(etykieta: String, wartosc: String, duze: Boolean = false) {
    Column {
        Text(
            etykieta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            wartosc,
            style = if (duze) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KartaScreenPreview() {
    DBMagazynURTheme {
        KartaScreen(
            material = Material(
                1, "Śruba imbusowa M8x20", opis = "Ocynk, DIN 912", typ = TypMaterialu.SAP,
                nrSap = "10023456", miejsce = "R1-A3", ilosc = 120.0, jednostka = "szt",
            ),
            zdjecieTrwa = false,
            onZdjecie = {},
            onUsunZdjecie = {},
            onKomunikat = {},
            historia = listOf(
                Ruch(
                    3, TypRuchu.WYDANIE, 4.0, utworzono = "2026-10-08T12:32:10+00:00",
                    kto = Osoba("jan.kowalski@magazyn.local"),
                    maszyna = Maszyna(1, "Prasa krawędziowa 2", kod = "PL01-L2-PR02"),
                ),
                Ruch(
                    2, TypRuchu.WYDANIE, 2.0, uwagi = "Pomyłka", utworzono = "2026-10-07T09:10:00+00:00",
                    anulowano = "2026-10-07T09:20:00+00:00", kto = Osoba("jan.kowalski@magazyn.local"),
                    ktoAnulowal = Osoba("anna.nowak@magazyn.local"),
                ),
                Ruch(1, TypRuchu.PRZYJECIE, 50.0, utworzono = "2026-10-06T07:00:00+00:00",
                    kto = Osoba("anna.nowak@magazyn.local")),
            ),
            saStarsze = true,
            onWczytajStarsze = {},
            blad = null,
            komunikat = null,
            onKomunikatPokazany = {},
            onPonow = {},
            onOperacja = {},
            onEdytuj = {},
            onWstecz = {},
            onWyloguj = {},
        )
    }
}
