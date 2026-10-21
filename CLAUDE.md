# Magazyn UR – aplikacja na Androida, program na Windows i strona (PWA)

Aplikacja do obsługi magazynu materiałów: wyszukiwanie, wydania, przyjęcia
i lista wydań do zaksięgowania w SAP. Korzysta z niej kilka osób. Ta sama aplikacja
działa na telefonie z Androidem i jako program na Windows (wspólny kod Kotlin),
a także jako strona na GitHub Pages (osobny kod w React; katalog części tylko po zalogowaniu).
Wszystkie trzy korzystają z tego samego projektu Supabase.

## Stack
- Kotlin, Android Studio, interfejs w Compose Multiplatform (na Androidzie to Jetpack Compose)
- Backend: Supabase (PostgreSQL, Auth, Storage, Data API), bez własnego serwera i API
- Biblioteka: supabase-kt (`io.github.jan-tennert.supabase`: BOM, `auth-kt`, `postgrest-kt`,
  `storage-kt`, `functions-kt`, `coil3-integration`) plus silnik Ktor:
  `ktor-client-android` na Androidzie, `ktor-client-okhttp` na Windows
- Zdjęcia: Coil 3 (`coil-compose`, `coil-network-ktor3`), wersja zgodna z Compose Multiplatform
  (Skiko) i nie niższa niż wymagana przez `coil3-integration`
- ViewModel: `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`; w kodzie wspólnym
  zawsze `viewModel { Klasa() }` (bez refleksji)
- Ustawienia na urządzeniu: `multiplatform-settings-no-arg` (`data/Ustawienia.kt`; Android:
  SharedPreferences, Windows: rejestr przez java.util.prefs, klucze z przedrostkiem `magazyn_ur.`)
- Interfejs i komunikaty po polsku

## Struktura projektu
- `shared` – prawie cały kod: modele, logika, ViewModele, ekrany, motyw, zasoby
  (`src/commonMain`). Kod wspólny może używać bibliotek Javy (java.text, java.time…),
  bo obie platformy działają na JVM; przy dodaniu innej platformy trzeba to rozdzielić.
  - `src/androidMain` i `src/desktopMain`: to, co różni platformy (`ui/Platforma.kt`:
    „Wstecz”, schowek, wybór zdjęcia; przetwarzanie zdjęć; kolory systemu).
  - `src/desktopMain/.../Main.kt`: okno programu na Windows (Esc = „Wstecz”).
  - Testy: `src/desktopTest` (JUnit, `:shared:desktopTest`).
  - Zasoby Compose: `src/commonMain/composeResources` (ikony, katalog, „O aplikacji”),
    w kodzie `com.example.dbmagazynur.zasoby.Res`.
- `app` – tylko Android: `MainActivity`, manifest (FileProvider dla aparatu), ikona aplikacji.
- Wersja wspólna dla obu: `magazyn.wersja` i `magazyn.kodWersji` w `gradle.properties`.
- Windows: uruchomienie `gradlew :shared:run`, instalator `gradlew :shared:packageMsi`
  (`shared/build/compose/binaries/main/msi`), folder z programem `:shared:createDistributable`.
  Pakuje pełny JDK Temurin 25 pobierany przez Gradle (JBR z Android Studio nie ma jpackage).
  Moduły Javy w instalatorze: `modules(...)` w `shared/build.gradle.kts`
  (`jdk.localedata` potrzebny do polskiego formatu liczb i sortowania).

## Strona (PWA, folder `web/`)
- React + TypeScript + Vite, MUI (Material), `@supabase/supabase-js`, `react-router`
  (`HashRouter`: adresy `#/material/12`, „Wstecz” przeglądarki działa jak w aplikacji),
  `vite-plugin-pwa` (instalacja na ekranie głównym). Testy: Vitest (`npm --prefix web test`).
- Te same ekrany co aplikacja. Strona i repozytorium są publiczne, więc katalogu części nie ma
  w kodzie: ekran „Katalog części” (tylko po zalogowaniu) pobiera `katalog.json` z prywatnego
  bucketu `katalog`. Wyszukiwarka przepisana w `web/src/logika/katalog.ts`; zmieniaj ją razem
  z `katalog/Wyszukiwarka.kt` (te same testy w `katalog.test.ts`).
- Logika jest przepisana w `web/src/logika` (login, ilości, daty, dane formularzy, statusy,
  komunikaty błędów). Zmieniając te reguły w Kotlinie, zmień je też w TypeScripcie (i testy).
- Lokalnie: `npm --prefix web run dev` (Claude: konfiguracja „strona” w `.claude/launch.json`);
  klucze z `local.properties`, wersja z `gradle.properties`, „O aplikacji” z
  `shared/.../composeResources/values/o_aplikacji.xml` (czyta je `web/vite.config.ts`).
- Publikacja: `.github/workflows/strona.yml` po wysłaniu zmian na gałąź `main`
  (testy, budowa, GitHub Pages). Adres i klucz publishable: zmienne repozytorium
  `SUPABASE_URL` i `SUPABASE_KEY` (Settings → Secrets and variables → Actions → Variables).
- Edge Function `uzytkownicy` odpowiada nagłówkami CORS (wymaga ich przeglądarka).

## Repozytorium
- Publiczne na GitHubie. Do gita nie trafia (`.gitignore`): `local.properties` (klucze),
  `katalog.json` (katalog części z SAP – dane firmy) i pliki Excel. Po pobraniu repozytorium
  katalog trzeba wygenerować skryptem, inaczej aplikacja pokaże błąd katalogu, a
  `KatalogPlikTest` nie przejdzie.

## Baza danych
Pełny schemat: @supabase/magazyn_schemat.sql

Schemat uruchamiam ręcznie w SQL Editor w panelu Supabase. Nie zmieniaj go bez
aktualizacji tego pliku. Każdą zmianę w bazie przygotuj jako osobny skrypt SQL
do uruchomienia w SQL Editor: `supabase/zmiany/NNN_opis.sql` (kolejny numer),
i nanieś ją też na pełny schemat.

Zasady, których pilnuje baza (aplikacja ma je tylko odzwierciedlać):
- Stan materiału (`materialy.ilosc`) zmienia się wyłącznie przez funkcje RPC `wydaj`
  i `przyjmij`. Aplikacja nie ma uprawnień do bezpośredniej zmiany `ilosc`.
  Ilość początkową można podać przy dodawaniu materiału.
- Nowy materiał aplikacja dodaje przez RPC `dodaj_material(p_nazwa, p_typ, p_nr_sap, p_miejsce,
  p_jednostka, p_opis, p_ilosc, p_nr_zamowienia)`. Trigger `po_dodaniu_materialu` zapisuje
  w `ruchy` wpis `UTWORZENIE` (ilosc = stan początkowy, może być 0) przy każdym dodaniu
  przez zalogowanego użytkownika (z panelu Supabase bez wpisu). Trigger
  `przed_usunieciem_materialu` usuwa ten wpis, żeby materiał bez innej historii dało się usunąć.
- Numer zamówienia (`ruchy.nr_zamowienia`, opcjonalny): przy przyjęciu
  `przyjmij(p_material_id, p_ilosc, p_uwagi, p_nr_zamowienia)` i przy dodaniu materiału;
  widać go w historii na karcie.
- Materiał typu `SAP` musi mieć `nr_sap`, materiał `KOSZTOWY` nie może go mieć.
- Typu materiału nie można zmienić (w żadną stronę), jeśli ma wydania
  niezaksięgowane i nieanulowane (trigger `przed_zmiana_typu`). Materiał kosztowy
  z historią wydań nie zostanie więc materiałem SAP; trzeba dodać nowy.
- Wydanie idzie na maszynę z tabeli `maszyny`:
  `wydaj(p_material_id, p_ilosc, p_maszyna_id, p_uwagi)`. Przy materiale `SAP` maszyna
  jest obowiązkowa, przy `KOSZTOWY` opcjonalna. Maszyny wycofanej (`aktywna = false`)
  nie da się wybrać. Listę maszyn edytuje tylko poziom 3.
- Wydania materiałów SAP lider zmiany księguje w SAP, a potem oznacza w aplikacji
  funkcją `zaksieguj_w_sap` (wtedy znikają z widoku `wydania_do_sap`).
- Ostatnie wydania na ekranie głównym: widok `ostatnie_wydania` (wszystkie wydania,
  `typ_materialu` do filtrowania, `material` = cały wiersz materiału jako JSON).
- `anuluj_wydanie` zwraca towar na stan i działa tylko dla wydań
  jeszcze niezaksięgowanych w SAP.
- Funkcje zgłaszają błędy (`raise exception`) z polskimi komunikatami.
  Pokazuj je użytkownikowi.
- Wyszukiwanie materiałów: RPC `szukaj_materialow(p_fraza)`, bez względu na wielkość
  liter i polskie znaki. Każde słowo frazy musi wystąpić w nazwie, nr SAP, miejscu
  albo opisie. Działa z uprawnieniami wywołującego, więc obowiązuje RLS.

## Poziomy uprawnień (`uzytkownicy.poziom`)
| Poziom | Uprawnienia |
|---|---|
| 0 | brak dostępu; konto nieaktywne (tak „usuwa się” pracownika z historią) |
| 1 | wyszukiwanie, wydawanie (`wydaj`), przyjęcia (`przyjmij`), dodawanie i edycja materiałów, zmiana własnego hasła |
| 2 | jak 1, plus `zaksieguj_w_sap` i `anuluj_wydanie` |
| 3 | jak 2, plus `ustaw_poziom`, `wyczysc_historie`, usuwanie materiałów bez historii, edycja listy maszyn, zakładanie kont i reset haseł innym |

Ukrywanie przycisków zależnie od poziomu to tylko wygoda. Uprawnienia sprawdza
baza (RLS i funkcje), więc nie przenoś tej logiki wyłącznie do aplikacji.

## Logowanie
- Supabase Auth, e-mail i hasło. Użytkownik wpisuje tylko login, a aplikacja dokleja
  `@magazyn.local` (konta zakładane w panelu z opcją Auto Confirm User).
  Login: `trim()` i `lowercase()`. Jeśli wpisany tekst zawiera `@`, użyj go bez zmian,
  bo konto administratora jest założone na prawdziwy e-mail.
- Po zalogowaniu wywołaj RPC `moj_poziom()`. Przy poziomie 0 pokaż „Konto nieaktywne”
  i wywołaj `signOut()`.
- Sesję zapisuje i odświeża supabase-kt. Przy starcie obserwuj `auth.sessionStatus`
  i poczekaj, aż minie `Initializing`: `Authenticated` otwiera ekran główny,
  `NotAuthenticated` ekran logowania.
- Przycisk „Wyloguj” dostępny zawsze. Zmiana własnego hasła: `auth.updateUser { password = ... }`.
- Nie ma resetu hasła mailem (adresy są techniczne). Zakładanie kont i reset haseł
  innym robi Edge Function `uzytkownicy` (`supabase/functions/uzytkownicy/index.ts`),
  wywoływana przez poziom 3. Wdrażam ją ręcznie w panelu (Edge Functions), tak jak SQL.
  - Klucz secret bierze z sekretu `SECRET_KEY` (Edge Functions → Secrets), a gdy go nie ma,
    ze zmiennej `SUPABASE_SERVICE_ROLE_KEY`, którą Supabase wstrzykuje sam.
  - Sama sprawdza token i poziom 3 wywołującego (klucz secret omija RLS).
  - Akcje: `utworz` (login, hasło, poziom; nazwa w `uzytkownicy` = login) i
    `resetuj_haslo` (nie dla własnego konta). Błędy zwraca jako `{"blad": "..."}`.
  - Reguła loginu jest w funkcji i w aplikacji (`normalizujLogin`); zmieniaj obie naraz.
- Poziom zmienia RPC `ustaw_poziom`. Aplikacja czyta poziom przy logowaniu i przy starcie,
  więc zmiana działa u pracownika po ponownym uruchomieniu aplikacji (RLS od razu).

## Konfiguracja i bezpieczeństwo
- `SUPABASE_URL` i `SUPABASE_KEY` (klucz publishable) trzymaj w `local.properties`.
  Gradle generuje z nich klasę `Konfiguracja` (`shared/build.gradle.kts`, zadanie
  `generujKonfiguracje`), wspólną dla Androida i Windows. `local.properties` nie trafia do gita.
- Nigdy nie umieszczaj w aplikacji klucza secret (dawniej service_role).
- Treść okienka „O aplikacji” (menu ⋮):
  `shared/src/commonMain/composeResources/values/o_aplikacji.xml`; wersja z `gradle.properties`.

## Zdjęcia materiałów
- Prywatny bucket Storage `zdjecia`. Ścieżka pliku w `materialy.zdjecie`:
  `materialy/<id>/<losowy UUID>.jpg`. Każde nowe zdjęcie dostaje nową nazwę, a stary
  plik jest usuwany, bo pamięć podręczna Coila jest kluczowana ścieżką (przy stałej
  nazwie telefony pokazywałyby stare zdjęcie).
- Przed wysłaniem zmniejszaj i kompresuj zdjęcie (darmowy plan ma 1 GB na pliki):
  dłuższy bok 1280 px, JPEG 80%. Android: aparat albo galeria (ImageDecoder, obraca wg EXIF);
  Windows: plik z dysku (ImageIO; JPG, PNG, BMP, GIF, bez obracania wg EXIF).
- Bucket jest prywatny, więc do wyświetlania używaj podpisanych linków
  albo pobierania jako zalogowany użytkownik (tak robi `coil3-integration`).
  Strona: `podpisanyLink()` w `web/src/supabase.ts` (prośby z jednej chwili jednym zapytaniem
  `createSignedUrls`, link ważny godzinę, odnawiany po 50 minutach).
- Miniatury w wynikach wyszukiwania: pole „Pokaż zdjęcia w wynikach” pod wyszukiwarką,
  domyślnie wyłączone (pobiera całe zdjęcia; darmowy plan nie ma transformacji obrazów).
  Wybór zostaje na urządzeniu (`Ustawienia.zdjeciaWWynikach`), na stronie w `localStorage`.

## Katalog części (bez logowania)
- Arkusz Excel z SAP wbudowany w aplikację i program:
  `shared/src/commonMain/composeResources/files/katalog.json`.
  Działa bez logowania i bez internetu (wejście z ekranu logowania, z ekranu
  „Brak połączenia” i z menu). Nie ma w nim stanów, tylko numer SAP, nazwy, miejsca
  i dostawców. Nie ma związku z tabelą `materialy`.
- Nowy arkusz: `python narzedzia/katalog_z_excela.py "<plik.xlsx>"`, potem wgraj ten plik
  w panelu Supabase (Storage → `katalog` → `katalog.json`, zmiana 009). Strona pokazuje go od razu,
  a aplikacja i program pobierają go po zalogowaniu; nowa wersja aplikacji tylko po to, żeby
  zaktualizować katalog wbudowany.
- Aplikacja przy otwarciu katalogu (gdy ktoś jest zalogowany) sprawdza listę plików w buckecie
  i pobiera `katalog.json`, gdy zmieniło się jego id lub data (`Ustawienia.wersjaKatalogu`).
  Zapisuje go w `plikPobranegoKatalogu` (Android: pliki aplikacji, Windows: `%LOCALAPPDATA%\MagazynUR`),
  więc działa potem bez logowania i internetu. Pokazuje nowszy z wbudowanego i pobranego
  (pole `utworzono`; przy tej samej dacie pobrany). Błędy pobierania pomija. Arkusz „Data”: A pełny SAP, B krótki tekst, C miejsce (tylko te pozycje
  trafiają do katalogu). Arkusze „Magazyn*”: A końcówka numeru (pełny = „538” +
  końcówka dopełniona zerami do 12 znaków), B pełniejsza nazwa, C dostawca; służą
  do wyszukiwania i są pokazywane obok nazwy z SAP.
- Wyszukiwarka (`katalog/Wyszukiwarka.kt`): bez znaczenia wielkość liter, polskie
  znaki, spacje i znaki rozdzielające („EcoRail2” = „EcoRail-2”, „3,969” = „3.969”);
  numer SAP pełny albo sama końcówka; literówki w słowach (nie w numerach), tylko
  gdy dokładnych trafień jest mało. Testy na prawdziwym pliku: `KatalogPlikTest`.

## Ustalenia
- Materiał leży w jednym miejscu magazynowym (`materialy.miejsce`) i ma jeden stan.
- Wydanie idzie na maszynę / miejsce funkcjonalne wybierane z listy w bazie
  (obowiązkowe dla materiałów SAP, opcjonalne dla kosztowych). Listę edytuje poziom 3
  w aplikacji.

## Otwarte kwestie
Nie zakładaj odpowiedzi. Jeśli zadanie od nich zależy, zapytaj.
- Czy przyjęcia materiałów SAP też trzeba księgować w SAP?
- Kto robi korektę stanu po inwentaryzacji?
- Czy telefon będzie osobisty, czy wspólny? Przy wspólnym potrzebne jest
  automatyczne wylogowanie po bezczynności.
