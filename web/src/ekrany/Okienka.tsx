import { Button, Dialog, DialogActions, DialogContent, DialogTitle, LinearProgress, Stack, Typography } from "@mui/material";
import { useState } from "react";
import { komunikat } from "../logika/bledy";
import { useSesja } from "../sesja";
import { PoleHasla } from "../ui/wspolne";

export function ZmianaHaslaDialog({ onZamknij }: { onZamknij: () => void }) {
  const { ustawNoweHaslo } = useSesja();
  const [nowe, ustawNowe] = useState("");
  const [powtorzone, ustawPowtorzone] = useState("");
  const [blad, ustawBlad] = useState<string | null>(null);
  const [trwa, ustawTrwa] = useState(false);
  const [zmieniono, ustawZmieniono] = useState(false);

  async function zmien() {
    if (trwa) return;
    if (!nowe) return ustawBlad("Wpisz nowe hasło");
    if (nowe !== powtorzone) return ustawBlad("Hasła się różnią");
    ustawTrwa(true);
    ustawBlad(null);
    try {
      await ustawNoweHaslo(nowe);
      ustawZmieniono(true);
    } catch (e) {
      ustawBlad(komunikat(e));
    } finally {
      ustawTrwa(false);
    }
  }

  return (
    <Dialog open onClose={() => !trwa && onZamknij()} fullWidth maxWidth="xs">
      <DialogTitle>Zmiana hasła</DialogTitle>
      <DialogContent>
        {zmieniono ? (
          <Typography>Hasło zostało zmienione.</Typography>
        ) : (
          <Stack spacing={1.5} sx={{ pt: 1 }}>
            <PoleHasla label="Nowe hasło" value={nowe} onChange={(e) => (ustawNowe(e.target.value), ustawBlad(null))} disabled={trwa} autoComplete="new-password" fullWidth />
            <PoleHasla
              label="Powtórz nowe hasło"
              value={powtorzone}
              onChange={(e) => (ustawPowtorzone(e.target.value), ustawBlad(null))}
              onKeyDown={(e) => e.key === "Enter" && void zmien()}
              disabled={trwa}
              autoComplete="new-password"
              fullWidth
            />
            {blad && <Typography color="error">{blad}</Typography>}
            {trwa && <LinearProgress />}
          </Stack>
        )}
      </DialogContent>
      <DialogActions>
        {zmieniono ? (
          <Button onClick={onZamknij}>OK</Button>
        ) : (
          <>
            <Button onClick={onZamknij} disabled={trwa}>Anuluj</Button>
            <Button onClick={() => void zmien()} disabled={trwa}>Zmień</Button>
          </>
        )}
      </DialogActions>
    </Dialog>
  );
}

// Treść z shared/src/commonMain/composeResources/values/o_aplikacji.xml, wersja z gradle.properties
export function OAplikacjiDialog({ onZamknij }: { onZamknij: () => void }) {
  const dane = __O_APLIKACJI__;
  const pole = (etykieta: string, wartosc?: string) =>
    wartosc ? (
      <div>
        <Typography variant="caption" color="text.secondary">{etykieta}</Typography>
        <Typography>{wartosc}</Typography>
      </div>
    ) : null;
  return (
    <Dialog open onClose={onZamknij} fullWidth maxWidth="xs">
      <DialogTitle>{dane.nazwa || "Magazyn UR"}</DialogTitle>
      <DialogContent>
        <Stack spacing={1.5}>
          <Typography color="text.secondary">
            Wersja {__WERSJA__} ({__KOD_WERSJI__}) · strona internetowa
          </Typography>
          {dane.opis && <Typography>{dane.opis}</Typography>}
          {pole("Autor", dane.autor)}
          {pole("Kontakt", dane.kontakt)}
          {pole("Dział", dane.dzial)}
          {dane.prawa && <Typography variant="body2">{dane.prawa}</Typography>}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onZamknij}>Zamknij</Button>
      </DialogActions>
    </Dialog>
  );
}

// Potwierdzenie nieodwracalnej operacji (zaksięgowanie, anulowanie, usunięcie)
export function Potwierdzenie(p: { tytul: string; tresc?: string; przycisk: string; onTak: () => void; onNie: () => void }) {
  return (
    <Dialog open onClose={p.onNie} fullWidth maxWidth="xs">
      <DialogTitle>{p.tytul}</DialogTitle>
      {p.tresc && (
        <DialogContent>
          <Typography>{p.tresc}</Typography>
        </DialogContent>
      )}
      <DialogActions>
        <Button onClick={p.onNie}>Wróć</Button>
        <Button onClick={p.onTak}>{p.przycisk}</Button>
      </DialogActions>
    </Dialog>
  );
}
