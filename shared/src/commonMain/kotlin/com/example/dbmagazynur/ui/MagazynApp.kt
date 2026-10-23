package com.example.dbmagazynur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.LocalPlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import com.example.dbmagazynur.data.zbudujImageLoader
import com.example.dbmagazynur.katalog.KatalogViewModel
import com.example.dbmagazynur.katalog.plikPobranegoKatalogu
import com.example.dbmagazynur.maszyny.MaszynyViewModel
import com.example.dbmagazynur.materialy.FormularzMaterialuViewModel
import com.example.dbmagazynur.materialy.KartaViewModel
import com.example.dbmagazynur.materialy.WyszukiwanieViewModel
import com.example.dbmagazynur.sap.WydaniaSapViewModel
import com.example.dbmagazynur.sesja.Sesja
import com.example.dbmagazynur.sesja.SesjaViewModel
import com.example.dbmagazynur.ui.theme.DBMagazynURTheme
import com.example.dbmagazynur.uzytkownicy.UzytkownicyViewModel
import com.example.dbmagazynur.wydania.OstatnieWydaniaViewModel
import com.example.dbmagazynur.stacje.StacjeViewModel

// Punkt wejścia wspólny dla Androida (MainActivity) i Windows (Main.kt)
@Composable
fun MagazynAplikacja() {
    setSingletonImageLoaderFactory { context -> zbudujImageLoader(context) }
    DBMagazynURTheme {
        MagazynApp()
    }
}

@Composable
fun MagazynApp(vm: SesjaViewModel = viewModel { SesjaViewModel() }) {
    // Katalog części działa bez logowania i bez internetu, więc przykrywa każdy ekran
    val kontekst = LocalPlatformContext.current
    val katalog: KatalogViewModel = viewModel { KatalogViewModel(plikPobranegoKatalogu(kontekst)) }
    if (katalog.otwarty) {
        Wstecz(onBack = katalog::zamknij)
        KatalogScreen(
            fraza = katalog.fraza,
            opis = katalog.opis,
            wynik = katalog.wynik,
            blad = katalog.blad,
            onFrazaChange = katalog::zmienFraze,
            onWstecz = katalog::zamknij,
            onWyloguj = if (vm.sesja is Sesja.Zalogowany) vm::wyloguj else null,
        )
        return
    }

    when (val sesja = vm.sesja) {
        Sesja.Ladowanie -> EkranLadowania()
        Sesja.Wylogowany -> LogowanieScreen(
            login = vm.login,
            haslo = vm.haslo,
            blad = vm.bladLogowania,
            trwa = vm.logowanieTrwa,
            onLoginChange = vm::zmienLogin,
            onHasloChange = vm::zmienHaslo,
            onZaloguj = vm::zaloguj,
            onKatalog = katalog::otworz,
        )
        is Sesja.Zalogowany -> {
            // Osobne ViewModele dla każdego konta, żeby na wspólnym telefonie
            // następna osoba nie zobaczyła wyników ani karty poprzedniej
            val wyszukiwanie: WyszukiwanieViewModel = viewModel(key = "wyszukiwanie-${sesja.userId}") { WyszukiwanieViewModel() }
            val karta: KartaViewModel = viewModel(key = "karta-${sesja.userId}") { KartaViewModel() }
            val sap: WydaniaSapViewModel = viewModel(key = "sap-${sesja.userId}") { WydaniaSapViewModel() }
            val formularz: FormularzMaterialuViewModel = viewModel(key = "formularz-${sesja.userId}") { FormularzMaterialuViewModel() }
            val maszyny: MaszynyViewModel = viewModel(key = "maszyny-${sesja.userId}") { MaszynyViewModel() }
            val stacje: StacjeViewModel = viewModel(key = "stacje-${sesja.userId}") { StacjeViewModel() }
            val uzytkownicy: UzytkownicyViewModel = viewModel(key = "uzytkownicy-${sesja.userId}") { UzytkownicyViewModel() }
            val ostatnie: OstatnieWydaniaViewModel = viewModel(key = "ostatnie-${sesja.userId}") { OstatnieWydaniaViewModel() }
            // Ukrywanie wejść to tylko wygoda, uprawnienia sprawdza baza
            val lider = sesja.poziom >= 2
            val administrator = sesja.poziom >= 3

            // Po zapisie formularza pokazujemy kartę zapisanego materiału
            val zapisano = formularz.zapisano
            LaunchedEffect(zapisano) {
                if (zapisano != null) {
                    karta.otworz(
                        zapisano.material,
                        komunikat = if (zapisano.nowy) "Dodano materiał" else "Zapisano zmiany",
                    )
                    wyszukiwanie.ponow()
                    formularz.zapisanoObsluzone()
                }
            }

            if (sap.otwarta) {
                Wstecz { if (sap.zamknij()) wyszukiwanie.ponow() }
                WydaniaSapScreen(
                    wydania = sap.wydania,
                    wczytywanie = sap.wczytywanie,
                    blad = sap.blad,
                    zaznaczone = sap.zaznaczone,
                    trwa = sap.trwa,
                    komunikat = sap.komunikat,
                    onKomunikatPokazany = sap::komunikatPokazany,
                    onOdswiez = sap::wczytaj,
                    onPrzelacz = sap::przelacz,
                    onZaznaczWszystkie = sap::zaznaczWszystkie,
                    onZaksieguj = sap::zaksieguj,
                    onAnuluj = sap::anuluj,
                    // Anulowanie zwraca towar na stan, więc wyniki wyszukiwania są nieaktualne
                    onWstecz = { if (sap.zamknij()) wyszukiwanie.ponow() },
                    onWyloguj = vm::wyloguj,
                )
            } else if (uzytkownicy.otwarta) {
                Wstecz(onBack = uzytkownicy::zamknij)
                UzytkownicyScreen(
                    lista = uzytkownicy.lista,
                    mojeId = sesja.userId,
                    wczytywanie = uzytkownicy.wczytywanie,
                    blad = uzytkownicy.blad,
                    komunikat = uzytkownicy.komunikat,
                    onKomunikatPokazany = uzytkownicy::komunikatPokazany,
                    onPonow = uzytkownicy::wczytaj,
                    onDodaj = uzytkownicy::dodaj,
                    onWybierz = uzytkownicy::wybierz,
                    onWstecz = uzytkownicy::zamknij,
                    onWyloguj = vm::wyloguj,
                )
                val wybrany = uzytkownicy.wybrany
                if (uzytkownicy.noweKonto) {
                    NoweKontoDialog(
                        login = uzytkownicy.login,
                        haslo = uzytkownicy.haslo,
                        poziom = uzytkownicy.poziom,
                        trwa = uzytkownicy.trwa,
                        blad = uzytkownicy.bladOkienka,
                        onLoginChange = { uzytkownicy.login = it },
                        onHasloChange = { uzytkownicy.haslo = it },
                        onPoziomChange = { uzytkownicy.poziom = it },
                        onZaloz = uzytkownicy::zalozKonto,
                        onZamknij = uzytkownicy::zamknijOkienko,
                    )
                } else if (wybrany != null) {
                    EdycjaUzytkownikaDialog(
                        uzytkownik = wybrany,
                        toJa = wybrany.id == sesja.userId,
                        poziom = uzytkownicy.poziom,
                        haslo = uzytkownicy.haslo,
                        trwa = uzytkownicy.trwa,
                        blad = uzytkownicy.bladOkienka,
                        onPoziomChange = { uzytkownicy.poziom = it },
                        onHasloChange = { uzytkownicy.haslo = it },
                        onZapiszPoziom = uzytkownicy::zapiszPoziom,
                        onUstawHaslo = uzytkownicy::ustawHaslo,
                        onZamknij = uzytkownicy::zamknijOkienko,
                    )
                }
            } else if (maszyny.otwarta) {
                Wstecz(onBack = maszyny::zamknij)
                MaszynyScreen(
                    lista = maszyny.lista,
                    wczytywanie = maszyny.wczytywanie,
                    blad = maszyny.blad,
                    komunikat = maszyny.komunikat,
                    onKomunikatPokazany = maszyny::komunikatPokazany,
                    onPonow = maszyny::wczytaj,
                    onDodaj = maszyny::dodaj,
                    onEdytuj = maszyny::edytuj,
                    onWstecz = maszyny::zamknij,
                    onWyloguj = vm::wyloguj,
                )
                if (maszyny.formularzOtwarty) {
                    MaszynaDialog(
                        nowa = maszyny.edytowana == null,
                        nazwa = maszyny.nazwa,
                        kod = maszyny.kod,
                        aktywna = maszyny.aktywna,
                        potwierdzUsuniecie = maszyny.potwierdzUsuniecie,
                        trwa = maszyny.trwa,
                        blad = maszyny.bladFormularza,
                        onNazwaChange = { maszyny.nazwa = it },
                        onKodChange = { maszyny.kod = it },
                        onAktywnaChange = { maszyny.aktywna = it },
                        onPotwierdzUsuniecie = { maszyny.potwierdzUsuniecie = it },
                        onZapisz = maszyny::zapisz,
                        onUsun = maszyny::usun,
                        onZamknij = maszyny::zamknijFormularz,
                    )
                }
            } else if (stacje.otwarta) {
                Wstecz(onBack = stacje::zamknij)
                StacjeScreen(
                    lista = stacje.lista,
                    obszar = stacje.obszar,
                    wczytywanie = stacje.wczytywanie,
                    blad = stacje.blad,
                    komunikat = stacje.komunikat,
                    onObszar = stacje::wybierzObszar,
                    onKomunikatPokazany = stacje::komunikatPokazany,
                    onPonow = stacje::wczytaj,
                    onDodaj = stacje::dodaj,
                    onEdytuj = stacje::edytuj,
                    onWstecz = stacje::zamknij,
                    onWyloguj = vm::wyloguj,
                )
                if (stacje.formularzOtwarty) {
                    StacjaDialog(
                        nowa = stacje.edytowana == null,
                        obszar = stacje.obszarStacji,
                        nazwa = stacje.nazwa,
                        kod = stacje.kod,
                        aktywna = stacje.aktywna,
                        potwierdzUsuniecie = stacje.potwierdzUsuniecie,
                        trwa = stacje.trwa,
                        blad = stacje.bladFormularza,
                        onObszarChange = { stacje.obszarStacji = it },
                        onNazwaChange = { stacje.nazwa = it },
                        onKodChange = { stacje.kod = it },
                        onAktywnaChange = { stacje.aktywna = it },
                        onPotwierdzUsuniecie = { stacje.potwierdzUsuniecie = it },
                        onZapisz = stacje::zapisz,
                        onUsun = stacje::usun,
                        onZamknij = stacje::zamknijFormularz,
                    )
                }
            } else if (formularz.otwarty && formularz.wyborStacji) {
                Wstecz(onBack = formularz::wsteczWyboruStacji)
                WyborStacjiScreen(
                    obszar = formularz.obszarWyboru,
                    stacje = formularz.dostepneStacje,
                    wybrane = formularz.stacje,
                    blad = formularz.bladStacji,
                    onObszar = formularz::wybierzObszar,
                    onPonow = formularz::wczytajStacje,
                    onPrzelacz = formularz::przelaczStacje,
                    onWstecz = formularz::wsteczWyboruStacji,
                    onGotowe = formularz::zamknijWyborStacji,
                    onWyloguj = vm::wyloguj,
                )
            } else if (formularz.otwarty) {
                Wstecz(onBack = formularz::zamknij)
                FormularzMaterialuScreen(
                    nowy = formularz.edytowany == null,
                    nazwa = formularz.nazwa,
                    typ = formularz.typ,
                    nrSap = formularz.nrSap,
                    miejsce = formularz.miejsce,
                    jednostka = formularz.jednostka,
                    iloscTekst = formularz.iloscTekst,
                    nrZamowienia = formularz.nrZamowienia,
                    stacje = formularz.stacje,
                    opis = formularz.opis,
                    trwa = formularz.trwa,
                    blad = formularz.blad,
                    onNazwaChange = { formularz.nazwa = it },
                    onTypChange = { formularz.typ = it },
                    onNrSapChange = { formularz.nrSap = it },
                    onMiejsceChange = { formularz.miejsce = it },
                    onJednostkaChange = { formularz.jednostka = it },
                    onIloscChange = { formularz.iloscTekst = it },
                    onNrZamowieniaChange = { formularz.nrZamowienia = it },
                    onWybierzStacje = formularz::pokazWyborStacji,
                    onUsunStacje = formularz::usunStacje,
                    onOpisChange = { formularz.opis = it },
                    onZapisz = formularz::zapisz,
                    onWstecz = formularz::zamknij,
                    onWyloguj = vm::wyloguj,
                )
            } else if (karta.otwarta) {
                KartaMaterialu(
                    vm = karta,
                    onEdytuj = formularz::otworzEdycje,
                    // Po wydaniu lub przyjęciu lista wyników pokazuje stary stan
                    onZamknij = { if (karta.zamknij()) wyszukiwanie.ponow() },
                    onWyloguj = vm::wyloguj,
                )
            } else {
                // Licznik i ostatnie wydania odświeżane przy każdym powrocie na ekran główny
                LaunchedEffect(Unit) {
                    ostatnie.odswiez()
                    if (lider) sap.odswiezLiczbe()
                }
                WyszukiwanieScreen(
                    login = sesja.login,
                    poziom = sesja.poziom,
                    fraza = wyszukiwanie.fraza,
                    stan = wyszukiwanie.stan,
                    onFrazaChange = wyszukiwanie::zmienFraze,
                    onPonow = wyszukiwanie::ponow,
                    pokazZdjecia = wyszukiwanie.pokazZdjecia,
                    onPokazZdjecia = wyszukiwanie::ustawPokazZdjecia,
                    onWybierzMaterial = { karta.otworz(it) },
                    onDodajMaterial = formularz::otworzNowy,
                    ostatnieWydania = ostatnie.stan,
                    onFiltrWydan = ostatnie::ustawFiltr,
                    onOdswiezWydania = ostatnie::odswiez,
                    onStarszeWydania = ostatnie::wczytajStarsze,
                    liczbaDoSap = sap.liczba,
                    onWydaniaSap = if (lider) sap::otworz else null,
                    onStacje = if (lider) stacje::otworz else null,
                    onMaszyny = if (administrator) maszyny::otworz else null,
                    onUzytkownicy = if (administrator) uzytkownicy::otworz else null,
                    onKatalog = katalog::otworz,
                    onZmienHaslo = vm::ustawNoweHaslo,
                    onWyloguj = vm::wyloguj,
                )
            }
        }
        is Sesja.Blad -> EkranBledu(
            komunikat = sesja.komunikat,
            onPonow = vm::ponow,
            onKatalog = katalog::otworz,
            onWyloguj = vm::wyloguj,
        )
    }
}

@Composable
private fun EkranLadowania() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun EkranBledu(komunikat: String, onPonow: () -> Unit, onKatalog: () -> Unit, onWyloguj: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(komunikat)
            Button(onClick = onPonow) { Text("Spróbuj ponownie") }
            // Katalog jest w aplikacji, więc działa też bez internetu
            OutlinedButton(onClick = onKatalog) { Text("Katalog części") }
            OutlinedButton(onClick = onWyloguj) { Text("Wyloguj") }
        }
    }
}
