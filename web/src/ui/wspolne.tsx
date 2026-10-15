import {
  AppBar,
  Box,
  Button,
  CircularProgress,
  Container,
  IconButton,
  InputAdornment,
  Snackbar,
  TextField,
  Toolbar,
  Typography,
  type TextFieldProps,
} from "@mui/material";
import { useCallback, useEffect, useReducer, useState, type ReactNode } from "react";
import { useLocation, useNavigate } from "react-router";
import { komunikat } from "../logika/bledy";
import { useSesja } from "../sesja";
import { IkonaWstecz } from "./Ikony";

// Ekran o szerokości telefonu, także na dużym monitorze
export function Ekran({ pasek, dol, children }: { pasek: ReactNode; dol?: ReactNode; children: ReactNode }) {
  return (
    <Box sx={{ minHeight: "100dvh", display: "flex", flexDirection: "column" }}>
      {pasek}
      <Container maxWidth="sm" disableGutters sx={{ flex: 1, pb: dol ? 9 : 2 }}>
        {children}
      </Container>
      {dol}
    </Box>
  );
}

// Pasek ekranów podrzędnych: strzałka wstecz i zawsze dostępne „Wyloguj”
export function PasekZPowrotem({ tytul, onWstecz, akcje }: { tytul: string; onWstecz: () => void; akcje?: ReactNode }) {
  const { wyloguj } = useSesja();
  return (
    <AppBar position="sticky" color="default" elevation={0}>
      <Toolbar>
        <IconButton edge="start" onClick={onWstecz} aria-label="Wstecz">
          <IkonaWstecz />
        </IconButton>
        <Typography variant="h6" sx={{ flex: 1, ml: 1 }} noWrap>
          {tytul}
        </Typography>
        {akcje}
        <Button onClick={() => void wyloguj()}>Wyloguj</Button>
      </Toolbar>
    </AppBar>
  );
}

export function Podpowiedz({ children }: { children: ReactNode }) {
  return (
    <Typography variant="body2" color="text.secondary" sx={{ p: 2 }}>
      {children}
    </Typography>
  );
}

export function Ladowanie() {
  return (
    <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
      <CircularProgress />
    </Box>
  );
}

export function BladZPonow({ blad, onPonow }: { blad: string; onPonow: () => void }) {
  return (
    <Box sx={{ p: 2 }}>
      <Typography color="error">{blad}</Typography>
      <Button onClick={onPonow}>Spróbuj ponownie</Button>
    </Box>
  );
}

// Pole hasła z „Pokaż / Ukryj”
export function PoleHasla(p: Omit<TextFieldProps, "type">) {
  const [pokaz, ustawPokaz] = useState(false);
  return (
    <TextField
      {...p}
      type={pokaz ? "text" : "password"}
      slotProps={{
        input: {
          endAdornment: (
            <InputAdornment position="end">
              <Button size="small" onClick={() => ustawPokaz(!pokaz)}>
                {pokaz ? "Ukryj" : "Pokaż"}
              </Button>
            </InputAdornment>
          ),
        },
      }}
    />
  );
}

// „Wstecz”: do poprzedniego ekranu, a gdy strona otwarta wprost z adresu – do wskazanego
export function useWstecz(domyslnie: string) {
  const navigate = useNavigate();
  const location = useLocation();
  return useCallback(
    () => (location.key === "default" ? navigate(domyslnie, { replace: true }) : navigate(-1)),
    [navigate, location.key, domyslnie],
  );
}

// Dane z Supabase z obsługą wczytywania, błędu i odświeżania
export function useDane<T>(pobierz: () => Promise<T>, zaleznosci: unknown[]) {
  const [stan, ustaw] = useState<{ dane?: T; blad?: string; laduje: boolean }>({ laduje: true });
  const [licznik, odswiez] = useReducer((x: number) => x + 1, 0);
  useEffect(() => {
    let aktualne = true;
    ustaw((s) => ({ ...s, laduje: true, blad: undefined }));
    pobierz().then(
      (dane) => aktualne && ustaw({ dane, laduje: false }),
      (e) => aktualne && ustaw((s) => ({ ...s, laduje: false, blad: komunikat(e) })),
    );
    return () => {
      aktualne = false;
    };
  }, [...zaleznosci, licznik]);
  return { ...stan, odswiez };
}

// Komunikat dla następnego ekranu, np. „Wydano 4 szt” po powrocie na kartę materiału
let oczekujacyKomunikat: string | null = null;

export function zostawKomunikat(tresc: string) {
  oczekujacyKomunikat = tresc;
}

function odbierzKomunikat(): string | null {
  const tresc = oczekujacyKomunikat;
  oczekujacyKomunikat = null;
  return tresc;
}

// Krótki komunikat na dole ekranu (jak Snackbar w aplikacji)
export function useKomunikat() {
  const [tresc, ustawTresc] = useState<string | null>(odbierzKomunikat);
  const element = (
    <Snackbar
      open={tresc !== null}
      message={tresc}
      autoHideDuration={4000}
      onClose={() => ustawTresc(null)}
      anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      sx={{ mb: 7 }}
    />
  );
  return [element, ustawTresc] as const;
}
