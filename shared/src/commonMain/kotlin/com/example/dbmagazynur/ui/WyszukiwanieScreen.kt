package com.example.dbmagazynur.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.dbmagazynur.data.BUCKET_ZDJEC
import com.example.dbmagazynur.zasoby.Res
import com.example.dbmagazynur.zasoby.ic_do_sap
import com.example.dbmagazynur.zasoby.ic_dodaj
import com.example.dbmagazynur.zasoby.ic_katalog
import com.example.dbmagazynur.zasoby.ic_more_vert
import org.jetbrains.compose.resources.painterResource
import com.example.dbmagazynur.materialy.LIMIT_WYNIKOW
import com.example.dbmagazynur.materialy.Material
import com.example.dbmagazynur.materialy.StanWyszukiwania
import com.example.dbmagazynur.materialy.TypMaterialu
import com.example.dbmagazynur.materialy.formatujIlosc
import com.example.dbmagazynur.ui.theme.DBMagazynURTheme
import com.example.dbmagazynur.wydania.FiltrWydan
import com.example.dbmagazynur.wydania.StanOstatnichWydan
import io.github.jan.supabase.storage.authenticatedStorageItem

// Ekran główny: wyszukiwanie materiałów
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WyszukiwanieScreen(
    login: String,
    poziom: Int,
    fraza: String,
    stan: StanWyszukiwania,
    onFrazaChange: (String) -> Unit,
    onPonow: () -> Unit,
    // Miniatury zdjęć obok nazw w wynikach
    pokazZdjecia: Boolean,
    onPokazZdjecia: (Boolean) -> Unit,
    onWybierzMaterial: (Material) -> Unit,
    // Nazwa podpowiedziana z frazy, gdy nic nie znaleziono
    onDodajMaterial: (String) -> Unit,
    // Ostatnie wydania, widoczne przy pustym polu wyszukiwania
    ostatnieWydania: StanOstatnichWydan,
    onFiltrWydan: (FiltrWydan) -> Unit,
    onOdswiezWydania: () -> Unit,
    onStarszeWydania: () -> Unit,
    // Tylko dla lidera (poziom 2+); null ukrywa wejście na listę
    liczbaDoSap: Int?,
    onWydaniaSap: (() -> Unit)?,
    // Tylko dla administratora (poziom 3)
    onMaszyny: (() -> Unit)?,
    onUzytkownicy: (() -> Unit)?,
    onKatalog: () -> Unit,
    onZmienHaslo: suspend (String) -> Unit,
    onWyloguj: () -> Unit,
) {
    var zmianaHasla by rememberSaveable { mutableStateOf(false) }
    var oAplikacji by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val klawiatura = LocalSoftwareKeyboardController.current

    if (zmianaHasla) {
        ZmianaHaslaDialog(onZmienHaslo = onZmienHaslo, onZamknij = { zmianaHasla = false })
    }
    if (oAplikacji) {
        OAplikacjiDialog(onZamknij = { oAplikacji = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Magazyn UR") },
                actions = {
                    TextButton(onClick = onWyloguj) { Text("Wyloguj") }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(painterResource(Res.drawable.ic_more_vert), contentDescription = "Więcej")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("$login · poziom $poziom") },
                                onClick = {},
                                enabled = false,
                            )
                            if (onMaszyny != null) {
                                DropdownMenuItem(
                                    text = { Text("Maszyny") },
                                    onClick = {
                                        menu = false
                                        onMaszyny()
                                    },
                                )
                            }
                            if (onUzytkownicy != null) {
                                DropdownMenuItem(
                                    text = { Text("Użytkownicy") },
                                    onClick = {
                                        menu = false
                                        onUzytkownicy()
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Zmień hasło") },
                                onClick = {
                                    menu = false
                                    zmianaHasla = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("O aplikacji") },
                                onClick = {
                                    menu = false
                                    oAplikacji = true
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            DolneMenu(
                liczbaDoSap = liczbaDoSap,
                onKatalog = onKatalog,
                onWydaniaSap = onWydaniaSap,
                onDodajMaterial = { onDodajMaterial("") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            OutlinedTextField(
                value = fraza,
                onValueChange = onFrazaChange,
                placeholder = { Text("Nazwa, numer SAP albo miejsce") },
                singleLine = true,
                trailingIcon = if (fraza.isNotEmpty()) {
                    { TextButton(onClick = { onFrazaChange("") }) { Text("Wyczyść") } }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { klawiatura?.hide() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )

            // Zdjęcia są pobierane z internetu, więc użytkownik włącza je sam
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .toggleable(value = pokazZdjecia, onValueChange = onPokazZdjecia, role = Role.Checkbox)
                    .padding(end = 12.dp),
            ) {
                Checkbox(checked = pokazZdjecia, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                Text("Pokaż zdjęcia w wynikach", style = MaterialTheme.typography.bodyMedium)
            }

            // Ta sama wysokość z paskiem i bez, żeby lista nie skakała
            if (stan.szukam) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                Spacer(Modifier.height(4.dp))
            }

            when {
                fraza.isBlank() ->
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                        ostatnieWydania(
                            stan = ostatnieWydania,
                            onFiltr = onFiltrWydan,
                            onOdswiez = onOdswiezWydania,
                            onStarsze = onStarszeWydania,
                            onMaterial = onWybierzMaterial,
                        )
                    }
                stan.blad != null ->
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stan.blad, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onPonow) { Text("Spróbuj ponownie") }
                    }
                stan.wynikiDla == fraza.trim() && stan.wyniki.isEmpty() ->
                    Column {
                        Podpowiedz("Nic nie znaleziono.")
                        TextButton(
                            onClick = { onDodajMaterial(fraza.trim()) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) { Text("Dodaj materiał „${fraza.trim()}”") }
                    }
                else ->
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(stan.wyniki, key = { it.id }) { material ->
                            WierszMaterialu(
                                material,
                                pokazZdjecie = pokazZdjecia,
                                onClick = { onWybierzMaterial(material) },
                            )
                            HorizontalDivider()
                        }
                        if (stan.saKolejne) {
                            item {
                                Podpowiedz("Pokazano pierwsze $LIMIT_WYNIKOW wyników. Doprecyzuj wyszukiwanie.")
                            }
                        }
                    }
            }
        }
    }
}

// Dolne menu ekranu głównego. Pozycje to skróty do innych ekranów, a nie zakładki,
// więc żadna nie jest zaznaczona. „Do zaksięgowania” tylko dla lidera (poziom 2+).
@Composable
private fun DolneMenu(
    liczbaDoSap: Int?,
    onKatalog: () -> Unit,
    onWydaniaSap: (() -> Unit)?,
    onDodajMaterial: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = false,
            onClick = onKatalog,
            icon = { Icon(painterResource(Res.drawable.ic_katalog), contentDescription = null) },
            label = { EtykietaMenu("Katalog części") },
        )
        if (onWydaniaSap != null) {
            NavigationBarItem(
                selected = false,
                onClick = onWydaniaSap,
                icon = {
                    BadgedBox(
                        badge = {
                            if (liczbaDoSap != null && liczbaDoSap > 0) {
                                Badge { Text(if (liczbaDoSap > 99) "99+" else "$liczbaDoSap") }
                            }
                        },
                    ) {
                        Icon(painterResource(Res.drawable.ic_do_sap), contentDescription = null)
                    }
                },
                label = { EtykietaMenu("Do zaksięgowania") },
            )
        }
        NavigationBarItem(
            selected = false,
            onClick = onDodajMaterial,
            icon = { Icon(painterResource(Res.drawable.ic_dodaj), contentDescription = null) },
            label = { EtykietaMenu("Dodaj materiał") },
        )
    }
}

@Composable
private fun EtykietaMenu(tekst: String) {
    Text(tekst, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun WierszMaterialu(material: Material, pokazZdjecie: Boolean, onClick: () -> Unit) {
    val szczegoly = buildList {
        add(if (material.typ == TypMaterialu.SAP) "SAP ${material.nrSap}" else "Kosztowy")
        material.miejsce?.takeIf { it.isNotBlank() }?.let { add("miejsce $it") }
    }.joinToString(" · ")
    val ilosc = @Composable {
        Text(
            "${formatujIlosc(material.ilosc)} ${material.jednostka}",
            style = MaterialTheme.typography.titleMedium,
            color = if (material.ilosc == 0.0) MaterialTheme.colorScheme.error else Color.Unspecified,
        )
    }

    if (pokazZdjecie) {
        // Ze zdjęciem wiersz jest dwa razy wyższy. Ilość stoi pod nazwą,
        // żeby przy dużym zdjęciu na wąskim telefonie nazwa miała miejsce.
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Miniatura(material.zdjecie)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(material.nazwa, style = MaterialTheme.typography.bodyLarge)
                Text(
                    szczegoly,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ilosc()
            }
        }
    } else {
        ListItem(
            headlineContent = { Text(material.nazwa) },
            supportingContent = { Text(szczegoly) },
            trailingContent = ilosc,
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

// To samo zdjęcie co na karcie materiału, więc karta pokaże je od razu z pamięci podręcznej
@Composable
private fun Miniatura(sciezka: String?) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(144.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (sciezka != null) {
            AsyncImage(
                model = authenticatedStorageItem(BUCKET_ZDJEC, sciezka),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                "Brak zdjęcia",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WyszukiwanieScreenPreview() {
    DBMagazynURTheme {
        WyszukiwanieScreen(
            login = "jan.kowalski",
            poziom = 1,
            fraza = "sruba",
            stan = StanWyszukiwania(
                wynikiDla = "sruba",
                wyniki = listOf(
                    Material(1, "Śruba imbusowa M8x20", typ = TypMaterialu.SAP, nrSap = "10023456",
                        miejsce = "R1-A3", ilosc = 120.0, jednostka = "szt"),
                    Material(2, "Śruba zamkowa M10x60", typ = TypMaterialu.KOSZTOWY,
                        miejsce = "R2-B1", ilosc = 0.0, jednostka = "szt"),
                    Material(3, "Pręt gwintowany M8", typ = TypMaterialu.KOSZTOWY,
                        ilosc = 2.5, jednostka = "m"),
                ),
            ),
            onFrazaChange = {},
            onPonow = {},
            pokazZdjecia = true,
            onPokazZdjecia = {},
            onWybierzMaterial = {},
            onDodajMaterial = {},
            ostatnieWydania = StanOstatnichWydan(),
            onFiltrWydan = {},
            onOdswiezWydania = {},
            onStarszeWydania = {},
            liczbaDoSap = 3,
            onWydaniaSap = {},
            onMaszyny = null,
            onUzytkownicy = null,
            onKatalog = {},
            onZmienHaslo = {},
            onWyloguj = {},
        )
    }
}
