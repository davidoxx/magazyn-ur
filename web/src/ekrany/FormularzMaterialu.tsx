import { Button, Chip, CircularProgress, InputAdornment, Stack, TextField, ToggleButton, ToggleButtonGroup, Typography } from "@mui/material";
import { useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router";
import { komunikat } from "../logika/bledy";
import { daneMaterialu, parametryNowegoMaterialu } from "../logika/dane";
import type { Material, TypMaterialu } from "../logika/typy";
import { dane, supabase } from "../supabase";
import { BladZPonow, Ekran, Ladowanie, PasekZPowrotem, useDane, useWstecz, zostawKomunikat } from "../ui/wspolne";
import { pobierzMaterial } from "./Karta";

const CZESTE_JEDNOSTKI = ["szt", "m", "kg", "l", "kpl", "opak"];

// Edycja: /material/:id/edycja, dodawanie: /nowy (?nazwa= z wyszukiwania)
export function EdycjaMaterialu() {
  const id = Number(useParams().id);
  const material = useDane(() => pobierzMaterial(id), [id]);
  const wstecz = useWstecz(`/material/${id}`);
  if (material.blad) {
    return (
      <Ekran pasek={<PasekZPowrotem tytul="Edycja materiału" onWstecz={wstecz} />}>
        <BladZPonow blad={material.blad} onPonow={material.odswiez} />
      </Ekran>
    );
  }
  if (!material.dane) {
    return (
      <Ekran pasek={<PasekZPowrotem tytul="Edycja materiału" onWstecz={wstecz} />}>
        <Ladowanie />
      </Ekran>
    );
  }
  return <FormularzMaterialu edytowany={material.dane} />;
}

export function NowyMaterial() {
  const [parametry] = useSearchParams();
  return <FormularzMaterialu edytowany={null} nazwa={parametry.get("nazwa") ?? ""} />;
}

// Stanu przy edycji nie ma, bo zmieniają go tylko wydania i przyjęcia
function FormularzMaterialu({ edytowany, nazwa: poczatkowaNazwa = "" }: { edytowany: Material | null; nazwa?: string }) {
  const navigate = useNavigate();
  const wstecz = useWstecz(edytowany ? `/material/${edytowany.id}` : "/");
  const nowy = edytowany === null;
  const [nazwa, ustawNazwe] = useState(edytowany?.nazwa ?? poczatkowaNazwa);
  // Typ bez wartości domyślnej: pomyłka SAP / kosztowy jest kosztowna
  const [typ, ustawTyp] = useState<TypMaterialu | null>(edytowany?.typ ?? null);
  const [nrSap, ustawNrSap] = useState(edytowany?.nr_sap ?? "");
  const [miejsce, ustawMiejsce] = useState(edytowany?.miejsce ?? "");
  const [jednostka, ustawJednostke] = useState(edytowany?.jednostka ?? "szt");
  const [iloscTekst, ustawIlosc] = useState("");
  // Tylko przy dodawaniu (opcjonalny); trafia do historii jako część wpisu „utworzenie”
  const [nrZamowienia, ustawNrZamowienia] = useState("");
  const [opis, ustawOpis] = useState(edytowany?.opis ?? "");
  const [trwa, ustawTrwa] = useState(false);
  const [blad, ustawBlad] = useState<string | null>(null);

  useEffect(() => ustawBlad(null), [nazwa, typ, nrSap, jednostka, iloscTekst]);

  async function zapisz() {
    if (trwa) return;
    const wynik = daneMaterialu({ nazwa, typ, nrSap, miejsce, jednostka, opis, iloscTekst: nowy ? iloscTekst : null });
    if (!wynik.ok) return ustawBlad(wynik.blad);
    ustawTrwa(true);
    try {
      const zapisany = await dane<Material | null>(
        edytowany
          ? supabase.from("materialy").update(wynik.wartosc).eq("id", edytowany.id).select().maybeSingle()
          : supabase.rpc("dodaj_material", parametryNowegoMaterialu(wynik.wartosc, nrZamowienia)),
      );
      if (!zapisany) throw new Error("Nie ma takiego materiału. Mógł zostać usunięty.");
      zostawKomunikat(nowy ? "Dodano materiał" : "Zapisano zmiany");
      // Nowy: formularz zamienia się w kartę; edycja: powrót na kartę
      if (nowy) navigate(`/material/${zapisany.id}`, { replace: true });
      else wstecz();
    } catch (e) {
      ustawBlad(komunikat(e));
      ustawTrwa(false);
    }
  }

  const bezPoprawek = { autoCapitalize: "none", autoCorrect: "off", spellCheck: false };
  return (
    <Ekran pasek={<PasekZPowrotem tytul={nowy ? "Nowy materiał" : "Edycja materiału"} onWstecz={() => !trwa && wstecz()} />}>
      <Stack spacing={2} sx={{ p: 2 }} component="form" onSubmit={(e) => (e.preventDefault(), void zapisz())}>
        <TextField label="Nazwa" value={nazwa} onChange={(e) => ustawNazwe(e.target.value)} disabled={trwa} fullWidth />
        <div>
          <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
            Typ
          </Typography>
          <ToggleButtonGroup exclusive fullWidth value={typ} disabled={trwa} onChange={(_, t: TypMaterialu | null) => t && ustawTyp(t)}>
            <ToggleButton value="SAP">SAP</ToggleButton>
            <ToggleButton value="KOSZTOWY">Kosztowy</ToggleButton>
          </ToggleButtonGroup>
        </div>
        {typ === "SAP" && (
          <TextField label="Numer SAP" value={nrSap} onChange={(e) => ustawNrSap(e.target.value)} disabled={trwa} fullWidth slotProps={{ htmlInput: bezPoprawek }} />
        )}
        <TextField label="Miejsce magazynowe (opcjonalnie)" value={miejsce} onChange={(e) => ustawMiejsce(e.target.value)} disabled={trwa} fullWidth slotProps={{ htmlInput: bezPoprawek }} />
        <div>
          <TextField label="Jednostka" value={jednostka} onChange={(e) => ustawJednostke(e.target.value)} disabled={trwa} fullWidth slotProps={{ htmlInput: bezPoprawek }} />
          <Stack direction="row" spacing={1} sx={{ mt: 1, overflowX: "auto" }}>
            {CZESTE_JEDNOSTKI.map((j) => (
              <Chip key={j} label={j} disabled={trwa} color={jednostka.trim() === j ? "primary" : "default"} onClick={() => ustawJednostke(j)} />
            ))}
          </Stack>
        </div>
        {nowy && (
          <TextField
            label="Stan początkowy (opcjonalnie)"
            value={iloscTekst}
            onChange={(e) => ustawIlosc(e.target.value)}
            disabled={trwa}
            fullWidth
            slotProps={{
              htmlInput: { inputMode: "decimal" },
              input: { endAdornment: <InputAdornment position="end">{jednostka.trim()}</InputAdornment> },
            }}
          />
        )}
        {nowy && (
          <TextField
            label="Numer zamówienia (opcjonalnie)"
            value={nrZamowienia}
            onChange={(e) => ustawNrZamowienia(e.target.value)}
            disabled={trwa}
            fullWidth
            slotProps={{ htmlInput: bezPoprawek }}
          />
        )}
        <TextField label="Opis (opcjonalnie)" value={opis} onChange={(e) => ustawOpis(e.target.value)} disabled={trwa} multiline minRows={2} fullWidth />
        {blad && <Typography color="error">{blad}</Typography>}
        <Button type="submit" variant="contained" size="large" disabled={trwa} fullWidth>
          {trwa ? <CircularProgress size={22} color="inherit" /> : nowy ? "Dodaj materiał" : "Zapisz zmiany"}
        </Button>
      </Stack>
    </Ekran>
  );
}
