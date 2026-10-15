import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControlLabel,
  InputAdornment,
  LinearProgress,
  List,
  ListItemButton,
  ListItemText,
  Radio,
  RadioGroup,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useState } from "react";
import { komunikat, komunikatFunkcji } from "../logika/bledy";
import { emailNaLogin, normalizujLogin } from "../logika/login";
import { nazwaPoziomu, opisPoziomu, POZIOMY, posortujUzytkownikow, type Uzytkownik } from "../logika/typy";
import { useSesja } from "../sesja";
import { dane, supabase } from "../supabase";
import { BladZPonow, Ekran, PasekZPowrotem, PoleHasla, useDane, useKomunikat, useWstecz } from "../ui/wspolne";

// Zakładanie kont i reset haseł robi Edge Function „uzytkownicy” (z kluczem secret)
async function wywolajFunkcje(tresc: Record<string, unknown>) {
  const { error } = await supabase.functions.invoke("uzytkownicy", { body: tresc });
  if (error) throw new Error(await komunikatFunkcji(error));
}

// Zarządzanie kontami (poziom 3). Poziom zmienia RPC ustaw_poziom.
export function Uzytkownicy() {
  const { sesja } = useSesja();
  const mojeId = sesja.stan === "zalogowany" ? sesja.userId : null;
  const wstecz = useWstecz("/");
  const [snackbar, pokaz] = useKomunikat();
  const [okienko, ustawOkienko] = useState<Uzytkownik | "nowe" | null>(null);
  const uzytkownicy = useDane(async () => posortujUzytkownikow(await dane<Uzytkownik[]>(supabase.from("uzytkownicy").select("id,nazwa,poziom"))), []);
  const gotowe = (tresc: string) => (ustawOkienko(null), pokaz(tresc), uzytkownicy.odswiez());

  return (
    <Ekran
      pasek={<PasekZPowrotem tytul="Użytkownicy" onWstecz={wstecz} akcje={<Button onClick={() => ustawOkienko("nowe")}>Załóż konto</Button>} />}
    >
      {uzytkownicy.laduje && <LinearProgress />}
      {uzytkownicy.blad && !uzytkownicy.dane && <BladZPonow blad={uzytkownicy.blad} onPonow={uzytkownicy.odswiez} />}
      <List disablePadding>
        {uzytkownicy.dane?.map((u) => (
          <ListItemButton key={u.id} divider onClick={() => ustawOkienko(u)}>
            <ListItemText
              primary={emailNaLogin(u.nazwa)}
              secondary={<Typography variant="body2" color={u.poziom === 0 ? "error" : "text.secondary"}>{nazwaPoziomu(u.poziom)}</Typography>}
            />
            {u.id === mojeId && <Typography color="primary">Ty</Typography>}
          </ListItemButton>
        ))}
      </List>
      {okienko === "nowe" && <NoweKonto onZamknij={() => ustawOkienko(null)} onGotowe={gotowe} />}
      {okienko && okienko !== "nowe" && (
        <EdycjaUzytkownika uzytkownik={okienko} toJa={okienko.id === mojeId} onZamknij={() => ustawOkienko(null)} onGotowe={gotowe} />
      )}
      {snackbar}
    </Ekran>
  );
}

function WyborPoziomu({ poziom, onZmiana, disabled }: { poziom: number; onZmiana: (p: number) => void; disabled: boolean }) {
  return (
    <div>
      <Typography variant="subtitle2">Poziom</Typography>
      <RadioGroup value={poziom} onChange={(e) => onZmiana(Number(e.target.value))}>
        {POZIOMY.map((p) => (
          <FormControlLabel
            key={p}
            value={p}
            disabled={disabled}
            control={<Radio />}
            label={
              <span>
                {p} – {nazwaPoziomu(p)}
                <Typography variant="body2" color="text.secondary">
                  {opisPoziomu(p)}
                </Typography>
              </span>
            }
          />
        ))}
      </RadioGroup>
    </div>
  );
}

function NoweKonto({ onZamknij, onGotowe }: { onZamknij: () => void; onGotowe: (t: string) => void }) {
  const [login, ustawLogin] = useState("");
  const [haslo, ustawHaslo] = useState("");
  const [poziom, ustawPoziom] = useState(1);
  const [trwa, ustawTrwa] = useState(false);
  const [blad, ustawBlad] = useState<string | null>(null);

  async function zaloz() {
    const nowy = normalizujLogin(login);
    if (!nowy.ok) return ustawBlad(nowy.blad);
    if (!haslo) return ustawBlad("Wpisz hasło");
    ustawTrwa(true);
    ustawBlad(null);
    try {
      await wywolajFunkcje({ akcja: "utworz", login: nowy.wartosc, haslo, poziom });
      onGotowe(`Założono konto ${nowy.wartosc}`);
    } catch (e) {
      ustawBlad(komunikat(e));
      ustawTrwa(false);
    }
  }

  return (
    <Dialog open onClose={() => !trwa && onZamknij()} fullWidth maxWidth="xs">
      <DialogTitle>Nowe konto</DialogTitle>
      <DialogContent>
        <Stack spacing={1.5} sx={{ pt: 1 }}>
          <TextField
            label="Login"
            value={login}
            onChange={(e) => ustawLogin(e.target.value)}
            disabled={trwa}
            fullWidth
            slotProps={{
              htmlInput: { autoCapitalize: "none", autoCorrect: "off", spellCheck: false },
              input: { endAdornment: <InputAdornment position="end">@magazyn.local</InputAdornment> },
            }}
          />
          <PoleHasla label="Hasło" value={haslo} onChange={(e) => ustawHaslo(e.target.value)} disabled={trwa} autoComplete="new-password" fullWidth />
          <WyborPoziomu poziom={poziom} onZmiana={ustawPoziom} disabled={trwa} />
          {blad && <Typography color="error">{blad}</Typography>}
          {trwa && <LinearProgress />}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onZamknij} disabled={trwa}>Anuluj</Button>
        <Button onClick={() => void zaloz()} disabled={trwa}>Załóż</Button>
      </DialogActions>
    </Dialog>
  );
}

function EdycjaUzytkownika(p: { uzytkownik: Uzytkownik; toJa: boolean; onZamknij: () => void; onGotowe: (t: string) => void }) {
  const u = p.uzytkownik;
  const [poziom, ustawPoziom] = useState(u.poziom);
  const [haslo, ustawHaslo] = useState("");
  const [trwa, ustawTrwa] = useState(false);
  const [blad, ustawBlad] = useState<string | null>(null);
  const login = emailNaLogin(u.nazwa);

  async function wykonaj(sukces: string, operacja: () => Promise<unknown>) {
    ustawTrwa(true);
    ustawBlad(null);
    try {
      await operacja();
      p.onGotowe(sukces);
    } catch (e) {
      ustawBlad(komunikat(e));
      ustawTrwa(false);
    }
  }

  return (
    <Dialog open onClose={() => !trwa && p.onZamknij()} fullWidth maxWidth="xs">
      <DialogTitle>{login}</DialogTitle>
      <DialogContent>
        <Stack spacing={1.5}>
          {p.toJa && (
            <Typography color="text.secondary">
              To Twoje konto. Własnego poziomu nie zmienisz, a hasło zmienisz w menu „Zmień hasło”.
            </Typography>
          )}
          <WyborPoziomu poziom={poziom} onZmiana={ustawPoziom} disabled={trwa || p.toJa} />
          <Button
            variant="outlined"
            disabled={trwa || p.toJa || poziom === u.poziom}
            onClick={() =>
              // Baza pilnuje poziomu 3 i blokuje zmianę własnego poziomu
              void wykonaj(`${login}: ${nazwaPoziomu(poziom)}`, () => dane(supabase.rpc("ustaw_poziom", { p_uzytkownik: u.id, p_poziom: poziom })))
            }
          >
            Zapisz poziom
          </Button>
          <Divider />
          <PoleHasla label="Nowe hasło" value={haslo} onChange={(e) => ustawHaslo(e.target.value)} disabled={trwa || p.toJa} autoComplete="new-password" fullWidth />
          <Button
            variant="outlined"
            disabled={trwa || p.toJa}
            onClick={() =>
              haslo
                ? void wykonaj(`Ustawiono nowe hasło dla ${login}`, () => wywolajFunkcje({ akcja: "resetuj_haslo", uzytkownik_id: u.id, haslo }))
                : ustawBlad("Wpisz nowe hasło")
            }
          >
            Ustaw nowe hasło
          </Button>
          {blad && <Typography color="error">{blad}</Typography>}
          {trwa && <LinearProgress />}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={p.onZamknij} disabled={trwa}>Zamknij</Button>
      </DialogActions>
    </Dialog>
  );
}
