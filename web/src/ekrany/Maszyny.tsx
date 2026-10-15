import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  LinearProgress,
  List,
  ListItemButton,
  ListItemText,
  Stack,
  Switch,
  TextField,
  Typography,
} from "@mui/material";
import { useMemo, useState } from "react";
import { komunikat } from "../logika/bledy";
import { daneMaszyny } from "../logika/dane";
import { pasujeDoFrazy } from "../logika/szukanie";
import { posortujMaszyny, type Maszyna } from "../logika/typy";
import { dane, supabase } from "../supabase";
import { BladZPonow, Ekran, PasekZPowrotem, Podpowiedz, useDane, useKomunikat, useWstecz } from "../ui/wspolne";
import { Potwierdzenie } from "./Okienka";

// Zarządzanie listą maszyn (poziom 3, pilnuje RLS).
// Maszyny z historią wydań się wycofuje, usunąć da się tylko nieużywane.
export function Maszyny() {
  const wstecz = useWstecz("/");
  const [snackbar, pokaz] = useKomunikat();
  const [fraza, ustawFraze] = useState("");
  const [edycja, ustawEdycje] = useState<Maszyna | "nowa" | null>(null);
  const maszyny = useDane(async () => posortujMaszyny(await dane<Maszyna[]>(supabase.from("maszyny").select("*"))), []);
  const widoczne = useMemo(
    () => (maszyny.dane ?? []).filter((m) => pasujeDoFrazy(`${m.nazwa} ${m.kod ?? ""}`, fraza)),
    [maszyny.dane, fraza],
  );

  return (
    <Ekran
      pasek={<PasekZPowrotem tytul="Maszyny" onWstecz={wstecz} akcje={<Button onClick={() => ustawEdycje("nowa")}>Dodaj</Button>} />}
    >
      <Box sx={{ px: 2, py: 1 }}>
        <TextField placeholder="Nazwa albo kod maszyny" value={fraza} onChange={(e) => ustawFraze(e.target.value)} fullWidth />
      </Box>
      {maszyny.laduje && <LinearProgress />}
      {maszyny.blad && !maszyny.dane && <BladZPonow blad={maszyny.blad} onPonow={maszyny.odswiez} />}
      {maszyny.dane?.length === 0 && <Podpowiedz>Lista maszyn jest pusta. Dodaj pierwszą maszynę.</Podpowiedz>}
      {!!maszyny.dane?.length && widoczne.length === 0 && <Podpowiedz>Nic nie znaleziono.</Podpowiedz>}
      <List disablePadding>
        {widoczne.map((m) => (
          <ListItemButton key={m.id} divider onClick={() => ustawEdycje(m)}>
            <ListItemText primary={m.nazwa} secondary={m.kod} />
            {!m.aktywna && <Typography color="error">wycofana</Typography>}
          </ListItemButton>
        ))}
      </List>
      {edycja && (
        <MaszynaDialog
          maszyna={edycja === "nowa" ? null : edycja}
          onZamknij={() => ustawEdycje(null)}
          onZapisano={(tresc) => (ustawEdycje(null), pokaz(tresc), maszyny.odswiez())}
        />
      )}
      {snackbar}
    </Ekran>
  );
}

function MaszynaDialog({ maszyna, onZamknij, onZapisano }: { maszyna: Maszyna | null; onZamknij: () => void; onZapisano: (t: string) => void }) {
  const [nazwa, ustawNazwe] = useState(maszyna?.nazwa ?? "");
  const [kod, ustawKod] = useState(maszyna?.kod ?? "");
  const [aktywna, ustawAktywna] = useState(maszyna?.aktywna ?? true);
  const [potwierdzUsuniecie, ustawPotwierdzUsuniecie] = useState(false);
  const [trwa, ustawTrwa] = useState(false);
  const [blad, ustawBlad] = useState<string | null>(null);

  async function wykonaj(sukces: string, operacja: () => Promise<unknown[] | null>, bladRls: string) {
    ustawTrwa(true);
    ustawBlad(null);
    try {
      // RLS po cichu pomija wiersze, do których nie ma uprawnień
      if (!(await operacja())?.length) throw new Error(bladRls);
      onZapisano(sukces);
    } catch (e) {
      ustawBlad(komunikat(e));
      ustawTrwa(false);
    }
  }

  function zapisz() {
    const d = daneMaszyny(nazwa, kod, aktywna);
    if (!d.ok) return ustawBlad(d.blad);
    const tabela = supabase.from("maszyny");
    void wykonaj(
      maszyna ? "Zapisano zmiany" : "Dodano maszynę",
      () => dane(maszyna ? tabela.update(d.wartosc).eq("id", maszyna.id).select() : tabela.insert(d.wartosc).select()),
      "Brak uprawnień albo maszyna została usunięta",
    );
  }

  function usun() {
    ustawPotwierdzUsuniecie(false);
    if (!maszyna) return;
    void wykonaj(
      "Usunięto maszynę",
      () => dane(supabase.from("maszyny").delete().eq("id", maszyna.id).select()),
      "Brak uprawnień albo maszyna została już usunięta",
    );
  }

  if (potwierdzUsuniecie) {
    return (
      <Potwierdzenie
        tytul="Usunąć maszynę?"
        tresc={`„${nazwa}” zniknie z listy. Usunąć da się tylko maszynę bez wydań, maszynę z historią trzeba wycofać.`}
        przycisk="Usuń"
        onTak={usun}
        onNie={() => ustawPotwierdzUsuniecie(false)}
      />
    );
  }

  return (
    <Dialog open onClose={() => !trwa && onZamknij()} fullWidth maxWidth="xs">
      <DialogTitle>{maszyna ? "Edycja maszyny" : "Nowa maszyna"}</DialogTitle>
      <DialogContent>
        <Stack spacing={1.5} sx={{ pt: 1 }}>
          <TextField label="Nazwa" value={nazwa} onChange={(e) => ustawNazwe(e.target.value)} disabled={trwa} fullWidth autoFocus />
          <TextField label="Kod w SAP (opcjonalnie)" value={kod} onChange={(e) => ustawKod(e.target.value)} disabled={trwa} fullWidth />
          <FormControlLabel
            control={<Switch checked={aktywna} onChange={(e) => ustawAktywna(e.target.checked)} disabled={trwa} />}
            label={
              <span>
                Aktywna
                <Typography variant="body2" color="text.secondary">
                  Wycofanej nie da się wybrać przy wydaniu
                </Typography>
              </span>
            }
          />
          {blad && <Typography color="error">{blad}</Typography>}
          {maszyna && (
            <Box>
              <Button color="error" disabled={trwa} onClick={() => ustawPotwierdzUsuniecie(true)}>
                Usuń maszynę
              </Button>
            </Box>
          )}
          {trwa && <LinearProgress />}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onZamknij} disabled={trwa}>Anuluj</Button>
        <Button onClick={zapisz} disabled={trwa}>{maszyna ? "Zapisz" : "Dodaj"}</Button>
      </DialogActions>
    </Dialog>
  );
}
