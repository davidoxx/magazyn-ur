import {
  AppBar,
  Badge,
  BottomNavigation,
  BottomNavigationAction,
  Box,
  Button,
  Divider,
  IconButton,
  LinearProgress,
  List,
  ListItemButton,
  ListItemText,
  Menu,
  MenuItem,
  Paper,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Toolbar,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { komunikat } from "../logika/bledy";
import { formatujDate } from "../logika/formatowanie";
import { formatujIlosc } from "../logika/ilosc";
import { emailNaLogin } from "../logika/login";
import { statusRuchu, type Material, type OstatnieWydanie } from "../logika/typy";
import { useSesja } from "../sesja";
import { dane, supabase } from "../supabase";
import { IkonaDodaj, IkonaDoSap, IkonaWiecej } from "../ui/Ikony";
import { Ekran, Podpowiedz } from "../ui/wspolne";
import { OAplikacjiDialog, ZmianaHaslaDialog } from "./Okienka";

const LIMIT_WYNIKOW = 50;
const STRONA_WYDAN = 20;

type Filtr = "WSZYSTKIE" | "KOSZTOWY" | "SAP";

// Ekran główny: wyszukiwanie materiałów, ostatnie wydania i dolne menu.
// Fraza i filtr są w adresie (#/?q=...&f=...), więc „Wstecz” z karty wraca do wyników.
export function Glowny() {
  const { sesja, wyloguj } = useSesja();
  const navigate = useNavigate();
  const [parametry, ustawParametry] = useSearchParams();
  const fraza = parametry.get("q") ?? "";
  const filtr = (parametry.get("f") as Filtr | null) ?? "WSZYSTKIE";
  const [menu, ustawMenu] = useState<HTMLElement | null>(null);
  const [okienko, ustawOkienko] = useState<"haslo" | "o-aplikacji" | null>(null);
  const [liczbaDoSap, ustawLiczbeDoSap] = useState<number | null>(null);

  const login = sesja.stan === "zalogowany" ? sesja.login : "";
  const poziom = sesja.stan === "zalogowany" ? sesja.poziom : 0;
  // Ukrywanie wejść to tylko wygoda, uprawnienia sprawdza baza
  const lider = poziom >= 2;
  const administrator = poziom >= 3;

  function ustawParametr(nazwa: string, wartosc: string | null) {
    const nowe = new URLSearchParams(parametry);
    if (wartosc) nowe.set(nazwa, wartosc);
    else nowe.delete(nazwa);
    ustawParametry(nowe, { replace: true });
  }

  useEffect(() => {
    if (!lider) return;
    void supabase
      .from("wydania_do_sap")
      .select("*", { count: "exact", head: true })
      .then(({ count }) => ustawLiczbeDoSap(count ?? null));
  }, [lider]);

  const pasek = (
    <AppBar position="sticky" color="default" elevation={0}>
      <Toolbar>
        <Typography variant="h6" sx={{ flex: 1 }}>
          Magazyn UR
        </Typography>
        <Button onClick={() => void wyloguj()}>Wyloguj</Button>
        <IconButton edge="end" onClick={(e) => ustawMenu(e.currentTarget)} aria-label="Więcej">
          <IkonaWiecej />
        </IconButton>
        <Menu anchorEl={menu} open={menu !== null} onClose={() => ustawMenu(null)}>
          <MenuItem disabled>
            {login} · poziom {poziom}
          </MenuItem>
          {administrator && <MenuItem onClick={() => navigate("/maszyny")}>Maszyny</MenuItem>}
          {administrator && <MenuItem onClick={() => navigate("/uzytkownicy")}>Użytkownicy</MenuItem>}
          <MenuItem onClick={() => (ustawMenu(null), ustawOkienko("haslo"))}>Zmień hasło</MenuItem>
          <MenuItem onClick={() => (ustawMenu(null), ustawOkienko("o-aplikacji"))}>O aplikacji</MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
  );

  // Dolne menu to skróty do innych ekranów, a nie zakładki, więc nic nie jest zaznaczone
  const dol = (
    <Paper elevation={3} sx={{ position: "fixed", bottom: 0, left: 0, right: 0, pb: "env(safe-area-inset-bottom)" }}>
      <BottomNavigation showLabels value={false}>
        {lider && (
          <BottomNavigationAction
            label="Do zaksięgowania"
            onClick={() => navigate("/sap")}
            icon={
              <Badge color="error" badgeContent={liczbaDoSap && liczbaDoSap > 99 ? "99+" : liczbaDoSap} invisible={!liczbaDoSap}>
                <IkonaDoSap />
              </Badge>
            }
          />
        )}
        <BottomNavigationAction label="Dodaj materiał" onClick={() => navigate("/nowy")} icon={<IkonaDodaj />} />
      </BottomNavigation>
    </Paper>
  );

  return (
    <Ekran pasek={pasek} dol={dol}>
      <Box sx={{ px: 2, py: 1 }}>
        <TextField
          placeholder="Nazwa, numer SAP albo miejsce"
          value={fraza}
          onChange={(e) => ustawParametr("q", e.target.value)}
          type="search"
          fullWidth
          slotProps={{ htmlInput: { autoCapitalize: "none", autoCorrect: "off", spellCheck: false, enterKeyHint: "search" } }}
        />
      </Box>
      {fraza.trim() ? (
        <WynikiWyszukiwania fraza={fraza.trim()} />
      ) : (
        <OstatnieWydania filtr={filtr} onFiltr={(f) => ustawParametr("f", f === "WSZYSTKIE" ? null : f)} />
      )}
      {okienko === "haslo" && <ZmianaHaslaDialog onZamknij={() => ustawOkienko(null)} />}
      {okienko === "o-aplikacji" && <OAplikacjiDialog onZamknij={() => ustawOkienko(null)} />}
    </Ekran>
  );
}

function WynikiWyszukiwania({ fraza }: { fraza: string }) {
  const navigate = useNavigate();
  const [wynik, ustawWynik] = useState<{ dla: string; lista: Material[]; saKolejne: boolean } | null>(null);
  const [szukam, ustawSzukam] = useState(false);
  const [blad, ustawBlad] = useState<string | null>(null);
  const [proba, ustawProbe] = useState(0);

  // Szukamy w trakcie pisania, ale dopiero po chwili przerwy
  useEffect(() => {
    let aktualne = true;
    ustawSzukam(true);
    const czasomierz = setTimeout(async () => {
      try {
        const lista = await dane<Material[]>(supabase.rpc("szukaj_materialow", { p_fraza: fraza }).limit(LIMIT_WYNIKOW + 1));
        if (!aktualne) return;
        ustawWynik({ dla: fraza, lista: lista.slice(0, LIMIT_WYNIKOW), saKolejne: lista.length > LIMIT_WYNIKOW });
        ustawBlad(null);
      } catch (e) {
        if (aktualne) ustawBlad(komunikat(e));
      } finally {
        if (aktualne) ustawSzukam(false);
      }
    }, 300);
    return () => {
      aktualne = false;
      clearTimeout(czasomierz);
    };
  }, [fraza, proba]);

  return (
    <>
      <Box sx={{ height: 4 }}>{szukam && <LinearProgress />}</Box>
      {blad ? (
        <Box sx={{ p: 2 }}>
          <Typography color="error">{blad}</Typography>
          <Button onClick={() => ustawProbe(proba + 1)}>Spróbuj ponownie</Button>
        </Box>
      ) : wynik?.dla === fraza && wynik.lista.length === 0 ? (
        <>
          <Podpowiedz>Nic nie znaleziono.</Podpowiedz>
          <Button sx={{ mx: 1 }} onClick={() => navigate(`/nowy?nazwa=${encodeURIComponent(fraza)}`)}>
            Dodaj materiał „{fraza}”
          </Button>
        </>
      ) : (
        <List disablePadding>
          {wynik?.lista.map((m) => (
            <ListItemButton key={m.id} divider onClick={() => navigate(`/material/${m.id}`)}>
              <ListItemText
                primary={m.nazwa}
                secondary={[m.typ === "SAP" ? `SAP ${m.nr_sap}` : "Kosztowy", m.miejsce?.trim() && `miejsce ${m.miejsce}`]
                  .filter(Boolean)
                  .join(" · ")}
              />
              <Typography variant="subtitle1" color={m.ilosc === 0 ? "error" : undefined} sx={{ ml: 2, whiteSpace: "nowrap" }}>
                {formatujIlosc(m.ilosc)} {m.jednostka}
              </Typography>
            </ListItemButton>
          ))}
          {wynik?.saKolejne && <Podpowiedz>Pokazano pierwsze {LIMIT_WYNIKOW} wyników. Doprecyzuj wyszukiwanie.</Podpowiedz>}
        </List>
      )}
    </>
  );
}

function OstatnieWydania({ filtr, onFiltr }: { filtr: Filtr; onFiltr: (f: Filtr) => void }) {
  const navigate = useNavigate();
  const [lista, ustawListe] = useState<OstatnieWydanie[] | null>(null);
  const [saStarsze, ustawSaStarsze] = useState(false);
  const [laduje, ustawLaduje] = useState(false);
  const [blad, ustawBlad] = useState<string | null>(null);
  const [odswiezenie, ustawOdswiezenie] = useState(0);

  async function wczytaj(od: number, dotychczasowe: OstatnieWydanie[]) {
    ustawLaduje(true);
    ustawBlad(null);
    try {
      let zapytanie = supabase.from("ostatnie_wydania").select("*");
      if (filtr !== "WSZYSTKIE") zapytanie = zapytanie.eq("typ_materialu", filtr);
      // Jeden wiersz więcej niż strona, żeby wiedzieć, czy są starsze
      const strona = await dane<OstatnieWydanie[]>(
        zapytanie.order("utworzono", { ascending: false }).order("id", { ascending: false }).range(od, od + STRONA_WYDAN),
      );
      // Nowe wydanie w międzyczasie przesuwa strony, stąd usuwanie powtórzeń
      const znane = new Set(dotychczasowe.map((w) => w.id));
      ustawListe([...dotychczasowe, ...strona.slice(0, STRONA_WYDAN).filter((w) => !znane.has(w.id))]);
      ustawSaStarsze(strona.length > STRONA_WYDAN);
    } catch (e) {
      ustawBlad(komunikat(e));
    } finally {
      ustawLaduje(false);
    }
  }

  useEffect(() => {
    ustawListe(null);
    void wczytaj(0, []);
  }, [filtr, odswiezenie]);

  return (
    <>
      <Box sx={{ px: 2, pt: 1, display: "flex", alignItems: "center" }}>
        <Typography variant="subtitle1" sx={{ flex: 1 }}>
          Ostatnie wydania
        </Typography>
        <Button onClick={() => ustawOdswiezenie(odswiezenie + 1)} disabled={laduje}>
          Odśwież
        </Button>
      </Box>
      <Box sx={{ px: 2 }}>
        <ToggleButtonGroup exclusive fullWidth size="small" value={filtr} onChange={(_, f: Filtr | null) => f && onFiltr(f)}>
          <ToggleButton value="WSZYSTKIE">Wszystkie</ToggleButton>
          <ToggleButton value="KOSZTOWY">Kosztowe</ToggleButton>
          <ToggleButton value="SAP">SAP</ToggleButton>
        </ToggleButtonGroup>
      </Box>
      <Box sx={{ height: 4, mt: 1 }}>{laduje && <LinearProgress />}</Box>
      {blad && <Typography color="error" sx={{ px: 2 }}>{blad}</Typography>}
      {lista?.length === 0 && <Podpowiedz>Brak wydań.</Podpowiedz>}
      <List disablePadding>
        {lista?.map((w) => (
          <WierszWydania key={w.id} wydanie={w} onClick={() => navigate(`/material/${w.material.id}`)} />
        ))}
      </List>
      {saStarsze && (
        <Button sx={{ mx: 1 }} onClick={() => lista && void wczytaj(lista.length, lista)} disabled={laduje}>
          Pokaż starsze
        </Button>
      )}
    </>
  );
}

function WierszWydania({ wydanie, onClick }: { wydanie: OstatnieWydanie; onClick: () => void }) {
  const m = wydanie.material;
  const status = statusRuchu("WYDANIE", wydanie.anulowano, wydanie.zaksiegowano_sap, m.typ === "SAP");
  const anulowane = status === "ANULOWANY";
  return (
    <>
      <ListItemButton onClick={onClick} sx={{ alignItems: "flex-start", gap: 1.5 }}>
        <Typography
          variant="subtitle1"
          sx={{ width: 88, flexShrink: 0, textDecoration: anulowane ? "line-through" : undefined }}
          color={anulowane ? "text.secondary" : "text.primary"}
        >
          −{formatujIlosc(wydanie.ilosc)} {m.jednostka}
        </Typography>
        <Box sx={{ minWidth: 0 }}>
          <Typography>{m.nazwa}</Typography>
          {wydanie.maszyna && (
            <Typography variant="body2">
              → {wydanie.maszyna_kod ? `${wydanie.maszyna} (${wydanie.maszyna_kod})` : wydanie.maszyna}
            </Typography>
          )}
          <Typography variant="body2" color="text.secondary">
            {[formatujDate(wydanie.utworzono), emailNaLogin(wydanie.wydal), m.typ === "SAP" ? `SAP ${m.nr_sap}` : "kosztowy"].join(" · ")}
          </Typography>
          {wydanie.uwagi?.trim() && <Typography variant="body2">Uwagi: {wydanie.uwagi}</Typography>}
          <TekstStatusu status={status} />
        </Box>
      </ListItemButton>
      <Divider component="li" />
    </>
  );
}

export function TekstStatusu({ status, szczegoly }: { status: ReturnType<typeof statusRuchu>; szczegoly?: string }) {
  if (!status) return null;
  const [tekst, kolor] = {
    ANULOWANY: ["Anulowane", "error"],
    ZAKSIEGOWANY: ["Zaksięgowane w SAP", "text.secondary"],
    CZEKA_NA_SAP: ["Czeka na zaksięgowanie w SAP", "secondary"],
  }[status];
  return (
    <Typography variant="body2" color={kolor}>
      {tekst}
      {szczegoly ? ` ${szczegoly}` : ""}
    </Typography>
  );
}
