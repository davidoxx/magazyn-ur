import {
  AppBar,
  Box,
  Button,
  Card,
  CardActionArea,
  CircularProgress,
  Dialog,
  IconButton,
  InputAdornment,
  LinearProgress,
  List,
  ListItemButton,
  ListItemText,
  Stack,
  TextField,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
} from "@mui/material";
import { useEffect, useMemo, useRef, useState } from "react";
import { useParams } from "react-router";
import { komunikat } from "../logika/bledy";
import { formatujIlosc, parsujIlosc } from "../logika/ilosc";
import { pasujeDoFrazy } from "../logika/szukanie";
import { opisMaszyny, type Maszyna } from "../logika/typy";
import { dane, supabase } from "../supabase";
import { IkonaWstecz } from "../ui/Ikony";
import { BladZPonow, Ekran, Ladowanie, PasekZPowrotem, Podpowiedz, useDane, useWstecz, zostawKomunikat } from "../ui/wspolne";
import { pobierzMaterial } from "./Karta";

const RODZAJE = {
  wydaj: { tytul: "Wydanie", przycisk: "Wydaj", wynik: "Wydano" },
  przyjmij: { tytul: "Przyjęcie", przycisk: "Przyjmij", wynik: "Przyjęto" },
} as const;

// Formularz wydania albo przyjęcia jednego materiału
export function Operacja({ rodzaj }: { rodzaj: keyof typeof RODZAJE }) {
  const id = Number(useParams().id);
  const wstecz = useWstecz(`/material/${id}`);
  const material = useDane(() => pobierzMaterial(id), [id]);
  const [iloscTekst, ustawIlosc] = useState("");
  const [uwagi, ustawUwagi] = useState("");
  const [maszyna, ustawMaszyne] = useState<Maszyna | null>(null);
  const [wyborMaszyny, ustawWyborMaszyny] = useState(false);
  const [trwa, ustawTrwa] = useState(false);
  const [blad, ustawBlad] = useState<string | null>(null);
  const opis = RODZAJE[rodzaj];
  const m = material.dane;

  async function wykonaj() {
    if (!m || trwa) return;
    const ilosc = parsujIlosc(iloscTekst);
    if (!ilosc.ok) return ustawBlad(ilosc.blad);
    // Wygoda: baza i tak odrzuci wydanie SAP bez maszyny
    if (rodzaj === "wydaj" && m.typ === "SAP" && !maszyna) return ustawBlad("Wybierz maszynę. Przy materiale SAP jest obowiązkowa.");
    ustawTrwa(true);
    ustawBlad(null);
    try {
      await dane(
        supabase.rpc(rodzaj, {
          p_material_id: m.id,
          p_ilosc: ilosc.wartosc,
          ...(rodzaj === "wydaj" ? { p_maszyna_id: maszyna?.id ?? null } : {}),
          p_uwagi: uwagi.trim() || null,
        }),
      );
      zostawKomunikat(`${opis.wynik} ${formatujIlosc(ilosc.wartosc)} ${m.jednostka}`);
      wstecz();
    } catch (e) {
      ustawBlad(komunikat(e));
      ustawTrwa(false);
    }
  }

  return (
    <Ekran pasek={<PasekZPowrotem tytul={opis.tytul} onWstecz={() => !trwa && wstecz()} />}>
      {material.blad && <BladZPonow blad={material.blad} onPonow={material.odswiez} />}
      {!m && material.laduje && <Ladowanie />}
      {m && (
        <Stack spacing={2} sx={{ p: 2 }} component="form" onSubmit={(e) => (e.preventDefault(), void wykonaj())}>
          <div>
            <Typography variant="h6">{m.nazwa}</Typography>
            <Typography color="text.secondary">
              Na stanie: {formatujIlosc(m.ilosc)} {m.jednostka}
            </Typography>
          </div>
          <TextField
            label="Ilość"
            value={iloscTekst}
            onChange={(e) => (ustawIlosc(e.target.value), ustawBlad(null))}
            disabled={trwa}
            autoFocus
            fullWidth
            slotProps={{
              htmlInput: { inputMode: "decimal" },
              input: { endAdornment: <InputAdornment position="end">{m.jednostka}</InputAdornment> },
            }}
          />
          {rodzaj === "wydaj" && (
            <Card variant="outlined">
              <CardActionArea disabled={trwa} onClick={() => ustawWyborMaszyny(true)} sx={{ p: 2 }}>
                <Typography variant="caption" color="text.secondary">
                  {m.typ === "SAP" ? "Maszyna (wymagana)" : "Maszyna (opcjonalnie)"}
                </Typography>
                <Typography>{maszyna ? opisMaszyny(maszyna) : "Wybierz maszynę"}</Typography>
              </CardActionArea>
              {maszyna && (
                <Box sx={{ px: 1, pb: 1 }}>
                  <Button size="small" disabled={trwa} onClick={() => ustawMaszyne(null)}>
                    Usuń maszynę
                  </Button>
                </Box>
              )}
            </Card>
          )}
          <TextField label="Uwagi (opcjonalnie)" value={uwagi} onChange={(e) => ustawUwagi(e.target.value)} disabled={trwa} multiline minRows={2} fullWidth />
          {blad && <Typography color="error">{blad}</Typography>}
          <Button type="submit" variant="contained" size="large" disabled={trwa} fullWidth>
            {trwa ? <CircularProgress size={22} color="inherit" /> : opis.przycisk}
          </Button>
        </Stack>
      )}
      {wyborMaszyny && (
        <WyborMaszyny onWybierz={(w) => (ustawMaszyne(w), ustawBlad(null), ustawWyborMaszyny(false))} onZamknij={() => ustawWyborMaszyny(false)} />
      )}
    </Ekran>
  );
}

// Wybór maszyny do wydania. Lista jest krótka, więc filtrujemy ją na urządzeniu.
// Wczytywana przy każdym otwarciu, żeby było widać maszyny dodane w trakcie zmiany.
function WyborMaszyny({ onWybierz, onZamknij }: { onWybierz: (m: Maszyna) => void; onZamknij: () => void }) {
  const maly = useMediaQuery(useTheme().breakpoints.down("sm"));
  const [fraza, ustawFraze] = useState("");
  const maszyny = useDane(
    () => dane<Maszyna[]>(supabase.from("maszyny").select("*").eq("aktywna", true).order("nazwa")),
    [],
  );
  const widoczne = useMemo(
    () => (maszyny.dane ?? []).filter((m) => pasujeDoFrazy(`${m.nazwa} ${m.kod ?? ""}`, fraza)),
    [maszyny.dane, fraza],
  );
  const zamknij = useRef(onZamknij);
  zamknij.current = onZamknij;
  useEffect(() => {
    // „Wstecz” w przeglądarce zamyka okno wyboru zamiast cofać formularz
    history.pushState({ wyborMaszyny: true }, "");
    const obsluga = () => zamknij.current();
    window.addEventListener("popstate", obsluga);
    return () => window.removeEventListener("popstate", obsluga);
  }, []);

  const zamknijPrzyciskiem = () => history.back();

  return (
    <Dialog open fullScreen={maly} fullWidth maxWidth="sm" onClose={zamknijPrzyciskiem}>
      <AppBar position="sticky" color="default" elevation={0}>
        <Toolbar>
          <IconButton edge="start" onClick={zamknijPrzyciskiem} aria-label="Wstecz">
            <IkonaWstecz />
          </IconButton>
          <Typography variant="h6" sx={{ ml: 1 }}>
            Wybierz maszynę
          </Typography>
        </Toolbar>
      </AppBar>
      <Box sx={{ px: 2, py: 1 }}>
        <TextField
          placeholder="Nazwa albo kod maszyny"
          value={fraza}
          onChange={(e) => ustawFraze(e.target.value)}
          fullWidth
          slotProps={{ htmlInput: { autoCapitalize: "none", autoCorrect: "off" } }}
        />
      </Box>
      {maszyny.laduje && <LinearProgress />}
      {maszyny.blad && !maszyny.dane && <BladZPonow blad={maszyny.blad} onPonow={maszyny.odswiez} />}
      {maszyny.dane?.length === 0 && <Podpowiedz>Lista maszyn jest pusta. Maszyny dodaje administrator.</Podpowiedz>}
      {maszyny.dane && maszyny.dane.length > 0 && widoczne.length === 0 && <Podpowiedz>Nic nie znaleziono.</Podpowiedz>}
      <List disablePadding>
        {widoczne.map((m) => (
          <ListItemButton
            key={m.id}
            divider
            onClick={() => {
              onWybierz(m);
              history.back();
            }}
          >
            <ListItemText primary={m.nazwa} secondary={m.kod} />
          </ListItemButton>
        ))}
      </List>
    </Dialog>
  );
}
