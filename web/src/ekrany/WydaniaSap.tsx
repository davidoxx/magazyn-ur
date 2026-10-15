import {
  Box,
  Button,
  Checkbox,
  CircularProgress,
  IconButton,
  LinearProgress,
  List,
  ListItem,
  ListItemButton,
  Menu,
  MenuItem,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import { useState } from "react";
import { komunikat } from "../logika/bledy";
import { formatujDate, odmiana } from "../logika/formatowanie";
import { formatujIlosc } from "../logika/ilosc";
import { emailNaLogin } from "../logika/login";
import type { WydanieDoSap } from "../logika/typy";
import { dane, supabase } from "../supabase";
import { IkonaWiecej } from "../ui/Ikony";
import { BladZPonow, Ekran, PasekZPowrotem, Podpowiedz, useDane, useKomunikat, useWstecz } from "../ui/wspolne";
import { Potwierdzenie } from "./Okienka";

const wydan = (n: number) => odmiana(n, "wydanie", "wydania", "wydań");

// Lista lidera: zaznacza wydania wpisane już do SAP i oznacza je jako zaksięgowane.
// Uprawnienia (poziom 2+) sprawdza baza w zaksieguj_w_sap i anuluj_wydanie.
export function WydaniaSap() {
  const wstecz = useWstecz("/");
  const [snackbar, pokaz] = useKomunikat();
  const wydania = useDane(
    () => dane<WydanieDoSap[]>(supabase.from("wydania_do_sap").select("*").order("utworzono", { ascending: true })),
    [],
  );
  const [zaznaczone, ustawZaznaczone] = useState<Set<number>>(new Set());
  const [trwa, ustawTrwa] = useState(false);
  const [potwierdzKsiegowanie, ustawPotwierdzKsiegowanie] = useState(false);
  const [doAnulowania, ustawDoAnulowania] = useState<WydanieDoSap | null>(null);
  const lista = wydania.dane;
  // Zaznaczenie zostaje tylko na wydaniach, które nadal są na liście
  const wybrane = lista?.filter((w) => zaznaczone.has(w.id)).map((w) => w.id) ?? [];
  const wszystkie = !!lista?.length && wybrane.length === lista.length;

  function przelacz(id: number) {
    const nowe = new Set(zaznaczone);
    if (nowe.has(id)) nowe.delete(id);
    else nowe.add(id);
    ustawZaznaczone(nowe);
  }

  async function zaksieguj() {
    ustawPotwierdzKsiegowanie(false);
    if (!wybrane.length || trwa) return;
    ustawTrwa(true);
    try {
      const n = Number(await dane(supabase.rpc("zaksieguj_w_sap", { p_ids: wybrane })));
      pokaz(
        n === wybrane.length
          ? `Zaksięgowano ${n} ${wydan(n)}`
          : `Zaksięgowano ${n} z ${wybrane.length}. Pozostałe ktoś już zaksięgował albo anulował.`,
      );
      ustawZaznaczone(new Set());
      wydania.odswiez();
    } catch (e) {
      pokaz(komunikat(e));
    } finally {
      ustawTrwa(false);
    }
  }

  async function anuluj(w: WydanieDoSap) {
    ustawDoAnulowania(null);
    if (trwa) return;
    ustawTrwa(true);
    try {
      await dane(supabase.rpc("anuluj_wydanie", { p_id: w.id }));
      pokaz("Wydanie anulowane, towar wrócił na stan");
      wydania.odswiez();
    } catch (e) {
      pokaz(komunikat(e));
    } finally {
      ustawTrwa(false);
    }
  }

  const dol = lista?.length ? (
    <Paper elevation={3} sx={{ position: "fixed", bottom: 0, left: 0, right: 0, pb: "env(safe-area-inset-bottom)" }}>
      <Stack direction="row" sx={{ maxWidth: 600, mx: "auto", p: 1, alignItems: "center" }}>
        <Button disabled={trwa} onClick={() => ustawZaznaczone(wszystkie ? new Set() : new Set(lista.map((w) => w.id)))}>
          {wszystkie ? "Odznacz wszystkie" : "Zaznacz wszystkie"}
        </Button>
        <Box sx={{ flex: 1 }} />
        <Button variant="contained" disabled={!wybrane.length || trwa} onClick={() => ustawPotwierdzKsiegowanie(true)}>
          {trwa ? <CircularProgress size={20} color="inherit" /> : `Zaksięguj (${wybrane.length})`}
        </Button>
      </Stack>
    </Paper>
  ) : undefined;

  return (
    <Ekran
      pasek={
        <PasekZPowrotem
          tytul="Do zaksięgowania w SAP"
          onWstecz={wstecz}
          akcje={<Button disabled={wydania.laduje || trwa} onClick={wydania.odswiez}>Odśwież</Button>}
        />
      }
      dol={dol}
    >
      {wydania.laduje && <LinearProgress />}
      {wydania.blad && <BladZPonow blad={wydania.blad} onPonow={wydania.odswiez} />}
      {lista?.length === 0 && <Podpowiedz>Wszystkie wydania są zaksięgowane.</Podpowiedz>}
      <List disablePadding>
        {lista?.map((w) => (
          <WierszWydania
            key={w.id}
            wydanie={w}
            zaznaczone={zaznaczone.has(w.id)}
            aktywne={!trwa}
            onPrzelacz={() => przelacz(w.id)}
            onAnuluj={() => ustawDoAnulowania(w)}
          />
        ))}
      </List>
      {potwierdzKsiegowanie && (
        <Potwierdzenie
          tytul={`Zaksięgować ${wybrane.length} ${wydan(wybrane.length)}?`}
          tresc="Oznacz tylko wydania wpisane już do SAP. Znikną z tej listy i nie da się tego cofnąć w aplikacji."
          przycisk="Zaksięguj"
          onTak={() => void zaksieguj()}
          onNie={() => ustawPotwierdzKsiegowanie(false)}
        />
      )}
      {doAnulowania && (
        <Potwierdzenie
          tytul="Anulować wydanie?"
          tresc={`${formatujIlosc(doAnulowania.ilosc)} ${doAnulowania.jednostka} „${doAnulowania.material}” wróci na stan. Tego nie da się cofnąć.`}
          przycisk="Tak, anuluj wydanie"
          onTak={() => void anuluj(doAnulowania)}
          onNie={() => ustawDoAnulowania(null)}
        />
      )}
      {snackbar}
    </Ekran>
  );
}

function WierszWydania(p: { wydanie: WydanieDoSap; zaznaczone: boolean; aktywne: boolean; onPrzelacz: () => void; onAnuluj: () => void }) {
  const w = p.wydanie;
  const [menu, ustawMenu] = useState<HTMLElement | null>(null);
  const maszyna = !w.maszyna ? "maszyna: brak" : w.maszyna_kod ? `${w.maszyna_kod} ${w.maszyna}` : w.maszyna;
  return (
    <ListItem
      disablePadding
      divider
      secondaryAction={
        <>
          <IconButton edge="end" disabled={!p.aktywne} onClick={(e) => ustawMenu(e.currentTarget)} aria-label="Więcej">
            <IkonaWiecej />
          </IconButton>
          <Menu anchorEl={menu} open={menu !== null} onClose={() => ustawMenu(null)}>
            <MenuItem onClick={() => (ustawMenu(null), p.onAnuluj())}>Anuluj wydanie</MenuItem>
          </Menu>
        </>
      }
    >
      <ListItemButton disabled={!p.aktywne} onClick={p.onPrzelacz} sx={{ alignItems: "flex-start", pr: 7 }}>
        <Checkbox checked={p.zaznaczone} tabIndex={-1} disableRipple sx={{ mt: -0.5, ml: -1 }} />
        <Box>
          <Typography sx={{ fontWeight: 600 }}>
            {formatujIlosc(w.ilosc)} {w.jednostka} · {w.material}
          </Typography>
          <Typography variant="body2">
            SAP {w.nr_sap} → {maszyna}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {formatujDate(w.utworzono)} · {emailNaLogin(w.wydal)}
          </Typography>
          {w.uwagi?.trim() && <Typography variant="body2">Uwagi: {w.uwagi}</Typography>}
        </Box>
      </ListItemButton>
    </ListItem>
  );
}
