import { Box, LinearProgress, List, ListItemButton, TextField, Typography } from "@mui/material";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router";
import { komunikat } from "../logika/bledy";
import { odczytajKatalog, opisKatalogu, Wyszukiwarka, type PozycjaKatalogu, type WynikKatalogu } from "../logika/katalog";
import { dane, supabase } from "../supabase";
import { BladZPonow, Ekran, PasekZPowrotem, Podpowiedz, useKomunikat, useWstecz } from "../ui/wspolne";

const LIMIT_KATALOGU = 100;

interface GotowyKatalog {
  opis: string;
  wyszukiwarka: Wyszukiwarka;
}

// Katalog z prywatnego bucketu „katalog” (plik wgrywa administrator w panelu Supabase).
// Ok. 1,3 MB pobierane raz na otwarcie strony, potem szukanie w pamięci przeglądarki.
let wczytany: Promise<GotowyKatalog> | null = null;

function wczytajKatalog(): Promise<GotowyKatalog> {
  if (!wczytany) {
    const obietnica = (async () => {
      let plik: Blob;
      try {
        plik = await dane(supabase.storage.from("katalog").download("katalog.json"));
      } catch (e) {
        if (e instanceof Error && /not.?found/i.test(e.message)) {
          throw new Error("Katalogu części nie ma jeszcze w Supabase. Administrator wgrywa plik katalog.json w panelu (Storage → katalog).");
        }
        throw e;
      }
      let katalog;
      try {
        katalog = odczytajKatalog(JSON.parse(await plik.text()));
      } catch {
        throw new Error("Plik katalogu w Supabase jest uszkodzony albo pusty.");
      }
      return { opis: opisKatalogu(katalog), wyszukiwarka: new Wyszukiwarka(katalog.pozycje) };
    })();
    // Po błędzie następne wejście spróbuje jeszcze raz
    obietnica.catch(() => {
      if (wczytany === obietnica) wczytany = null;
    });
    wczytany = obietnica;
  }
  return wczytany;
}

// Katalog części z arkusza, jak w aplikacji, ale tylko po zalogowaniu (strona jest publiczna).
// Fraza jest w adresie (#/katalog?q=...), więc odświeżenie strony jej nie gubi.
export function KatalogCzesci() {
  const wstecz = useWstecz("/");
  const [parametry, ustawParametry] = useSearchParams();
  const fraza = parametry.get("q") ?? "";
  const [katalog, ustawKatalog] = useState<GotowyKatalog | null>(null);
  const [blad, ustawBlad] = useState<string | null>(null);
  const [proba, ustawProbe] = useState(0);
  const [wynik, ustawWynik] = useState<WynikKatalogu | null>(null);
  const [snackbar, pokaz] = useKomunikat();

  useEffect(() => {
    let aktualne = true;
    ustawBlad(null);
    wczytajKatalog().then(
      (k) => aktualne && ustawKatalog(k),
      (e) => aktualne && ustawBlad(komunikat(e)),
    );
    return () => {
      aktualne = false;
    };
  }, [proba]);

  // Szukamy w trakcie pisania, po chwili przerwy (jak w aplikacji)
  useEffect(() => {
    if (!katalog || !fraza.trim()) return ustawWynik(null);
    const czasomierz = setTimeout(() => ustawWynik(katalog.wyszukiwarka.szukaj(fraza, LIMIT_KATALOGU)), 150);
    return () => clearTimeout(czasomierz);
  }, [katalog, fraza]);

  async function kopiuj(sap: string) {
    try {
      await navigator.clipboard.writeText(sap);
      pokaz(`Skopiowano numer SAP ${sap}`);
    } catch {
      pokaz(`Numer SAP: ${sap}`);
    }
  }

  let tresc;
  if (blad) {
    tresc = <BladZPonow blad={blad} onPonow={() => ustawProbe(proba + 1)} />;
  } else if (!katalog) {
    tresc = (
      <>
        <LinearProgress />
        <Podpowiedz>Wczytuję katalog…</Podpowiedz>
      </>
    );
  } else if (!fraza.trim()) {
    tresc = (
      <>
        <Podpowiedz>
          Wpisz nazwę, numer SAP (pełny albo końcówkę, np. 1000), miejsce albo dostawcę. Spacje, myślniki, kropki i polskie znaki
          nie mają znaczenia, drobne literówki też są wybaczane.
        </Podpowiedz>
        <Podpowiedz>Dotknij pozycji, żeby skopiować numer SAP.</Podpowiedz>
        <Podpowiedz>Katalog: {katalog.opis}</Podpowiedz>
      </>
    );
  } else if (wynik?.pozycje.length === 0) {
    tresc = <Podpowiedz>Nic nie znaleziono.</Podpowiedz>;
  } else {
    tresc = (
      <List disablePadding>
        {wynik?.pozycje.map((p) => <WierszKatalogu key={p.sap} pozycja={p} onClick={() => void kopiuj(p.sap)} />)}
        {wynik && wynik.wszystkich > wynik.pozycje.length && (
          <Podpowiedz>
            Pokazano {LIMIT_KATALOGU} z {wynik.wszystkich}. Doprecyzuj wyszukiwanie.
          </Podpowiedz>
        )}
      </List>
    );
  }

  return (
    <Ekran pasek={<PasekZPowrotem tytul="Katalog części" onWstecz={wstecz} />}>
      <Box sx={{ px: 2, py: 1 }}>
        <TextField
          placeholder="Nazwa, numer SAP, miejsce albo dostawca"
          value={fraza}
          onChange={(e) => ustawParametry(e.target.value ? { q: e.target.value } : {}, { replace: true })}
          type="search"
          fullWidth
          autoFocus
          slotProps={{ htmlInput: { autoCapitalize: "none", autoCorrect: "off", spellCheck: false, enterKeyHint: "search" } }}
        />
      </Box>
      {tresc}
      {snackbar}
    </Ekran>
  );
}

function WierszKatalogu({ pozycja, onClick }: { pozycja: PozycjaKatalogu; onClick: () => void }) {
  const nazwa = pozycja.nazwa.trim() ? pozycja.nazwa : (pozycja.inneNazwy[0] ?? "");
  const inneNazwy = pozycja.inneNazwy.filter((n) => n !== nazwa);
  return (
    <ListItemButton divider onClick={onClick} sx={{ alignItems: "flex-start", gap: 2 }}>
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography>{nazwa || "(bez nazwy)"}</Typography>
        {inneNazwy.length > 0 && <Typography variant="body2">{inneNazwy.join(" · ")}</Typography>}
        <Typography variant="body2" color="text.secondary">
          {[`SAP ${pozycja.sap}`, pozycja.dostawcy.join(", ")].filter(Boolean).join(" · ")}
        </Typography>
      </Box>
      {/* Miejsce to główna informacja: gdzie leży część */}
      {pozycja.miejsca.length === 0 ? (
        <Typography variant="body2" color="text.secondary" align="right" sx={{ whiteSpace: "pre-line" }}>
          {"brak\nmiejsca"}
        </Typography>
      ) : (
        <Typography variant="subtitle1" color="primary" align="right" sx={{ maxWidth: 128, whiteSpace: "pre-line" }}>
          {pozycja.miejsca.join("\n")}
        </Typography>
      )}
    </ListItemButton>
  );
}
