import {
  AppBar,
  Box,
  Button,
  Card,
  CardActionArea,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  LinearProgress,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Stack,
  Switch,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
} from "@mui/material";
import { useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router";
import { komunikat } from "../logika/bledy";
import { daneStacji } from "../logika/dane";
import { odmiana } from "../logika/formatowanie";
import { pasujeDoFrazy } from "../logika/szukanie";
import { OBSZARY, posortujStacje, type Stacja } from "../logika/typy";
import { dane, supabase } from "../supabase";
import { IkonaWstecz } from "../ui/Ikony";
import { BladZPonow, Ekran, PasekZPowrotem, Podpowiedz, useDane, useKomunikat, useWstecz } from "../ui/wspolne";
import { Potwierdzenie } from "./Okienka";

// Przełącznik F1 / F2 / F3
function WyborObszaru({ obszar, onObszar, disabled }: { obszar: string | null; onObszar: (o: string) => void; disabled?: boolean }) {
  return (
    <ToggleButtonGroup exclusive fullWidth size="small" value={obszar} disabled={disabled} onChange={(_, o: string | null) => o && onObszar(o)}>
      {OBSZARY.map((o) => (
        <ToggleButton key={o} value={o}>
          {o}
        </ToggleButton>
      ))}
    </ToggleButtonGroup>
  );
}

// Zarządzanie lokalizacjami funkcjonalnymi (poziom 2+, pilnuje RLS), osobno dla F1, F2 i F3.
// Stacje przypisane do materiałów się wycofuje, usunąć da się tylko nieużywane.
// Obszar jest w adresie (#/stacje?obszar=F2), więc „Wstecz” z innego ekranu do niego wraca.
export function Stacje() {
  const wstecz = useWstecz("/");
  const [snackbar, pokaz] = useKomunikat();
  const [parametry, ustawParametry] = useSearchParams();
  const obszar = parametry.get("obszar") ?? OBSZARY[0];
  const [fraza, ustawFraze] = useState("");
  const [edycja, ustawEdycje] = useState<Stacja | "nowa" | null>(null);
  const stacje = useDane(async () => posortujStacje(await dane<Stacja[]>(supabase.from("stacje").select("*"))), []);
  const zObszaru = useMemo(() => (stacje.dane ?? []).filter((s) => s.obszar === obszar), [stacje.dane, obszar]);
  const widoczne = useMemo(() => zObszaru.filter((s) => pasujeDoFrazy(`${s.nazwa} ${s.kod ?? ""}`, fraza)), [zObszaru, fraza]);
  const ustawObszar = (o: string) => ustawParametry({ obszar: o }, { replace: true });

  return (
    <Ekran
      pasek={
        <PasekZPowrotem tytul="Lokalizacje funkcjonalne" onWstecz={wstecz} akcje={<Button onClick={() => ustawEdycje("nowa")}>Dodaj</Button>} />
      }
    >
      <Stack spacing={1} sx={{ px: 2, py: 1 }}>
        <WyborObszaru obszar={obszar} onObszar={ustawObszar} />
        <TextField placeholder="Nazwa albo kod stacji" value={fraza} onChange={(e) => ustawFraze(e.target.value)} fullWidth />
      </Stack>
      {stacje.laduje && <LinearProgress />}
      {stacje.blad && !stacje.dane && <BladZPonow blad={stacje.blad} onPonow={stacje.odswiez} />}
      {stacje.dane && zObszaru.length === 0 && <Podpowiedz>W {obszar} nie ma jeszcze stacji. Dodaj pierwszą.</Podpowiedz>}
      {zObszaru.length > 0 && widoczne.length === 0 && <Podpowiedz>Nic nie znaleziono.</Podpowiedz>}
      <List disablePadding>
        {widoczne.map((s) => (
          <ListItemButton key={s.id} divider onClick={() => ustawEdycje(s)}>
            <ListItemText primary={s.nazwa} secondary={s.kod} />
            {!s.aktywna && <Typography color="error">wycofana</Typography>}
          </ListItemButton>
        ))}
      </List>
      {edycja && (
        <StacjaDialog
          stacja={edycja === "nowa" ? null : edycja}
          obszarNowej={obszar}
          onZamknij={() => ustawEdycje(null)}
          onZapisano={(tresc, obszarZapisanej) => {
            ustawEdycje(null);
            pokaz(tresc);
            // Lista pokazuje obszar zapisanej stacji (mogła zostać przeniesiona)
            if (obszarZapisanej) ustawObszar(obszarZapisanej);
            stacje.odswiez();
          }}
        />
      )}
      {snackbar}
    </Ekran>
  );
}

function StacjaDialog(p: {
  stacja: Stacja | null;
  obszarNowej: string;
  onZamknij: () => void;
  onZapisano: (tresc: string, obszar: string | null) => void;
}) {
  const { stacja } = p;
  const [obszar, ustawObszar] = useState<string | null>(stacja?.obszar ?? p.obszarNowej);
  const [nazwa, ustawNazwe] = useState(stacja?.nazwa ?? "");
  const [kod, ustawKod] = useState(stacja?.kod ?? "");
  const [aktywna, ustawAktywna] = useState(stacja?.aktywna ?? true);
  const [potwierdzUsuniecie, ustawPotwierdzUsuniecie] = useState(false);
  const [trwa, ustawTrwa] = useState(false);
  const [blad, ustawBlad] = useState<string | null>(null);

  async function wykonaj(sukces: string, operacja: () => Promise<unknown[] | null>, bladRls: string, obszarPo: string | null) {
    ustawTrwa(true);
    ustawBlad(null);
    try {
      // RLS po cichu pomija wiersze, do których nie ma uprawnień
      if (!(await operacja())?.length) throw new Error(bladRls);
      p.onZapisano(sukces, obszarPo);
    } catch (e) {
      ustawBlad(komunikat(e));
      ustawTrwa(false);
    }
  }

  function zapisz() {
    const d = daneStacji(obszar, nazwa, kod, aktywna);
    if (!d.ok) return ustawBlad(d.blad);
    const tabela = supabase.from("stacje");
    void wykonaj(
      stacja ? "Zapisano zmiany" : "Dodano stację",
      () => dane(stacja ? tabela.update(d.wartosc).eq("id", stacja.id).select() : tabela.insert(d.wartosc).select()),
      "Brak uprawnień albo stacja została usunięta",
      d.wartosc.obszar,
    );
  }

  function usun() {
    ustawPotwierdzUsuniecie(false);
    if (!stacja) return;
    void wykonaj(
      "Usunięto stację",
      () => dane(supabase.from("stacje").delete().eq("id", stacja.id).select()),
      "Brak uprawnień albo stacja została już usunięta",
      null,
    );
  }

  if (potwierdzUsuniecie) {
    return (
      <Potwierdzenie
        tytul="Usunąć stację?"
        tresc={`„${nazwa}” zniknie z listy. Usunąć da się tylko stację nieprzypisaną do materiałów, przypisaną trzeba wycofać.`}
        przycisk="Usuń"
        onTak={usun}
        onNie={() => ustawPotwierdzUsuniecie(false)}
      />
    );
  }

  return (
    <Dialog open onClose={() => !trwa && p.onZamknij()} fullWidth maxWidth="xs">
      <DialogTitle>{stacja ? "Edycja stacji" : "Nowa stacja"}</DialogTitle>
      <DialogContent>
        <Stack spacing={1.5} sx={{ pt: 1 }}>
          <WyborObszaru obszar={obszar} onObszar={ustawObszar} disabled={trwa} />
          <TextField label="Nazwa" value={nazwa} onChange={(e) => ustawNazwe(e.target.value)} disabled={trwa} fullWidth autoFocus />
          <TextField label="Kod w SAP (opcjonalnie)" value={kod} onChange={(e) => ustawKod(e.target.value)} disabled={trwa} fullWidth />
          <FormControlLabel
            control={<Switch checked={aktywna} onChange={(e) => ustawAktywna(e.target.checked)} disabled={trwa} />}
            label={
              <span>
                Aktywna
                <Typography variant="body2" color="text.secondary">
                  Wycofanej nie da się przypisać do kolejnych materiałów
                </Typography>
              </span>
            }
          />
          {blad && <Typography color="error">{blad}</Typography>}
          {stacja && (
            <Box>
              <Button color="error" disabled={trwa} onClick={() => ustawPotwierdzUsuniecie(true)}>
                Usuń stację
              </Button>
            </Box>
          )}
          {trwa && <LinearProgress />}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={p.onZamknij} disabled={trwa}>
          Anuluj
        </Button>
        <Button onClick={zapisz} disabled={trwa}>
          {stacja ? "Zapisz" : "Dodaj"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

// Wybór lokalizacji funkcjonalnych materiału: najpierw F1 / F2 / F3, potem lista stacji
// z wybranego obszaru. Zaznaczenie od razu dodaje stację do formularza, ponowne ją usuwa.
// „Wstecz” (też w przeglądarce) z listy wraca do wyboru obszaru, a z niego zamyka okno.
export function WyborStacji({ wybrane, onPrzelacz, onZamknij }: { wybrane: Stacja[]; onPrzelacz: (s: Stacja) => void; onZamknij: () => void }) {
  const maly = useMediaQuery(useTheme().breakpoints.down("sm"));
  const [obszar, ustawObszar] = useState<string | null>(null);
  const obszarTeraz = useRef(obszar);
  obszarTeraz.current = obszar;
  const zamknij = useRef(onZamknij);
  zamknij.current = onZamknij;
  const zamykanie = useRef(false);

  useEffect(() => {
    history.pushState({ wyborStacji: true }, "");
    const obsluga = () => {
      if (obszarTeraz.current && !zamykanie.current) ustawObszar(null);
      else zamknij.current();
    };
    window.addEventListener("popstate", obsluga);
    return () => window.removeEventListener("popstate", obsluga);
  }, []);

  function wybierzObszar(o: string) {
    if (!obszar) history.pushState({ wyborStacji: o }, "");
    ustawObszar(o);
  }

  function gotowe() {
    zamykanie.current = true;
    history.go(obszar ? -2 : -1);
  }

  return (
    <Dialog open fullScreen={maly} fullWidth maxWidth="sm" onClose={gotowe}>
      <AppBar position="sticky" color="default" elevation={0}>
        <Toolbar>
          <IconButton edge="start" onClick={() => history.back()} aria-label="Wstecz">
            <IkonaWstecz />
          </IconButton>
          <Typography variant="h6" sx={{ ml: 1 }}>
            {obszar ? `Stacje ${obszar}` : "Lokalizacja funkcjonalna"}
          </Typography>
        </Toolbar>
      </AppBar>
      {obszar ? (
        <ListaStacji key={obszar} obszar={obszar} wybrane={wybrane} onObszar={ustawObszar} onPrzelacz={onPrzelacz} />
      ) : (
        <Stack spacing={1.5} sx={{ p: 2 }}>
          {OBSZARY.map((o) => {
            const liczba = wybrane.filter((s) => s.obszar === o).length;
            return (
              <Card key={o} variant="outlined">
                <CardActionArea onClick={() => wybierzObszar(o)} sx={{ p: 2 }}>
                  <Typography variant="h5">{o}</Typography>
                  {liczba > 0 && <Typography color="text.secondary">wybrane: {liczba}</Typography>}
                </CardActionArea>
              </Card>
            );
          })}
        </Stack>
      )}
      <Box sx={{ p: 2, position: "sticky", bottom: 0, bgcolor: "background.paper", mt: "auto" }}>
        <Button variant="contained" fullWidth onClick={gotowe}>
          {wybrane.length ? `Gotowe · ${wybrane.length} ${odmiana(wybrane.length, "stacja", "stacje", "stacji")}` : "Gotowe"}
        </Button>
      </Box>
    </Dialog>
  );
}

function ListaStacji(p: { obszar: string; wybrane: Stacja[]; onObszar: (o: string) => void; onPrzelacz: (s: Stacja) => void }) {
  const [fraza, ustawFraze] = useState("");
  // Lista wczytywana przy każdym wejściu, żeby było widać stacje dodane w trakcie zmiany
  const stacje = useDane(
    async () => posortujStacje(await dane<Stacja[]>(supabase.from("stacje").select("*").eq("obszar", p.obszar).eq("aktywna", true))),
    [p.obszar],
  );
  const widoczne = useMemo(() => (stacje.dane ?? []).filter((s) => pasujeDoFrazy(`${s.nazwa} ${s.kod ?? ""}`, fraza)), [stacje.dane, fraza]);
  const wybraneId = new Set(p.wybrane.map((s) => s.id));

  return (
    <>
      <Stack spacing={1} sx={{ px: 2, py: 1 }}>
        <WyborObszaru obszar={p.obszar} onObszar={p.onObszar} />
        <TextField
          placeholder="Nazwa albo kod stacji"
          value={fraza}
          onChange={(e) => ustawFraze(e.target.value)}
          fullWidth
          slotProps={{ htmlInput: { autoCapitalize: "none", autoCorrect: "off" } }}
        />
      </Stack>
      {stacje.laduje && <LinearProgress />}
      {stacje.blad && !stacje.dane && <BladZPonow blad={stacje.blad} onPonow={stacje.odswiez} />}
      {stacje.dane?.length === 0 && (
        <Podpowiedz>W {p.obszar} nie ma stacji. Stacje dodaje lider w menu „Lokalizacje funkcjonalne”.</Podpowiedz>
      )}
      {!!stacje.dane?.length && widoczne.length === 0 && <Podpowiedz>Nic nie znaleziono.</Podpowiedz>}
      <List disablePadding>
        {widoczne.map((s) => (
          <ListItemButton key={s.id} divider onClick={() => p.onPrzelacz(s)}>
            <ListItemIcon>
              <Checkbox edge="start" checked={wybraneId.has(s.id)} tabIndex={-1} disableRipple />
            </ListItemIcon>
            <ListItemText primary={s.nazwa} secondary={s.kod} />
          </ListItemButton>
        ))}
      </List>
    </>
  );
}
