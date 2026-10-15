import { Box, Button, CircularProgress, Dialog, Divider, LinearProgress, Stack, Typography } from "@mui/material";
import { useEffect, useRef, useState, type ChangeEvent } from "react";
import { useNavigate, useParams } from "react-router";
import { komunikat } from "../logika/bledy";
import { formatujDate } from "../logika/formatowanie";
import { formatujIlosc } from "../logika/ilosc";
import { emailNaLogin } from "../logika/login";
import { KOLUMNY_HISTORII, opisMaszyny, statusRuchu, type Material, type Ruch } from "../logika/typy";
import { BUCKET_ZDJEC, nowaSciezkaZdjecia, przygotujZdjecie } from "../logika/zdjecia";
import { dane, supabase } from "../supabase";
import { BladZPonow, Ekran, Ladowanie, PasekZPowrotem, useDane, useKomunikat, useWstecz } from "../ui/wspolne";
import { TekstStatusu } from "./Glowny";
import { Potwierdzenie } from "./Okienka";

const STRONA_HISTORII = 20;

export const pobierzMaterial = async (id: number) => {
  const m = await dane<Material | null>(supabase.from("materialy").select("*").eq("id", id).maybeSingle());
  if (!m) throw new Error("Nie ma takiego materiału. Mógł zostać usunięty.");
  return m;
};

export function Karta() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const wstecz = useWstecz("/");
  const [snackbar, pokaz] = useKomunikat();
  const material = useDane(() => pobierzMaterial(id), [id]);
  const m = material.dane;

  return (
    <Ekran pasek={<PasekZPowrotem tytul="Materiał" onWstecz={wstecz} />}>
      {material.blad && <BladZPonow blad={material.blad} onPonow={material.odswiez} />}
      {!m && material.laduje && <Ladowanie />}
      {m && (
        <Stack spacing={1.5} sx={{ p: 2 }}>
          <SekcjaZdjecia material={m} onZmiana={material.odswiez} pokaz={pokaz} />
          <Typography variant="h5">{m.nazwa}</Typography>
          <Typography color="text.secondary">{m.typ === "SAP" ? `Materiał SAP · ${m.nr_sap}` : "Materiał kosztowy"}</Typography>
          <Pole etykieta="Miejsce" wartosc={m.miejsce?.trim() || "—"} />
          <Pole etykieta="Stan" wartosc={`${formatujIlosc(m.ilosc)} ${m.jednostka}`} duze />
          {m.opis?.trim() && <Pole etykieta="Opis" wartosc={m.opis} />}
          {m.ilosc <= 0 && <Typography color="error">Brak na stanie</Typography>}
          <Stack direction="row" spacing={1.5}>
            <Button variant="contained" fullWidth disabled={m.ilosc <= 0} onClick={() => navigate(`/material/${m.id}/wydaj`)}>
              Wydaj
            </Button>
            <Button variant="outlined" fullWidth onClick={() => navigate(`/material/${m.id}/przyjmij`)}>
              Przyjmij
            </Button>
          </Stack>
          <Box>
            <Button onClick={() => navigate(`/material/${m.id}/edycja`)}>Edytuj dane materiału</Button>
          </Box>
          <Divider />
          <Typography variant="subtitle1">Historia</Typography>
          <Historia material={m} />
        </Stack>
      )}
      {snackbar}
    </Ekran>
  );
}

function Pole({ etykieta, wartosc, duze }: { etykieta: string; wartosc: string; duze?: boolean }) {
  return (
    <div>
      <Typography variant="caption" color="text.secondary">
        {etykieta}
      </Typography>
      <Typography variant={duze ? "h4" : "body1"}>{wartosc}</Typography>
    </div>
  );
}

// Bucket jest prywatny, więc zdjęcie pokazujemy przez podpisany link (ważny godzinę)
const linki = new Map<string, Promise<string>>();
function podpisanyLink(sciezka: string): Promise<string> {
  if (!linki.has(sciezka)) {
    const link = dane(supabase.storage.from(BUCKET_ZDJEC).createSignedUrl(sciezka, 3600)).then((d) => d.signedUrl);
    link.catch(() => linki.delete(sciezka));
    linki.set(sciezka, link);
  }
  return linki.get(sciezka)!;
}

// Sprzątanie Storage: nieudane usunięcie zostawia tylko zbędny plik
async function usunPlikPoCichu(sciezka: string) {
  await supabase.storage.from(BUCKET_ZDJEC).remove([sciezka]).catch(() => undefined);
}

async function zapiszSciezke(materialId: number, sciezka: string | null) {
  const zapisany = await dane<Material | null>(
    supabase.from("materialy").update({ zdjecie: sciezka }).eq("id", materialId).select().maybeSingle(),
  );
  if (!zapisany) throw new Error("Nie ma takiego materiału. Mógł zostać usunięty.");
}

function SekcjaZdjecia({ material, onZmiana, pokaz }: { material: Material; onZmiana: () => void; pokaz: (t: string) => void }) {
  const aparat = useRef<HTMLInputElement>(null);
  const galeria = useRef<HTMLInputElement>(null);
  const [link, ustawLink] = useState<string | null>(null);
  const [bladZdjecia, ustawBladZdjecia] = useState(false);
  const [trwa, ustawTrwa] = useState(false);
  const [pelnyEkran, ustawPelnyEkran] = useState(false);
  const [potwierdz, ustawPotwierdz] = useState(false);
  const sciezka = material.zdjecie;

  useEffect(() => {
    ustawLink(null);
    ustawBladZdjecia(false);
    if (sciezka) podpisanyLink(sciezka).then(ustawLink, () => ustawBladZdjecia(true));
  }, [sciezka]);

  // Wysyła nowe zdjęcie pod nową nazwą, zapisuje ścieżkę w materiale i dopiero wtedy usuwa stary plik
  async function ustawZdjecie(e: ChangeEvent<HTMLInputElement>) {
    const plik = e.target.files?.[0];
    e.target.value = "";
    if (!plik || trwa) return;
    ustawTrwa(true);
    const nowa = nowaSciezkaZdjecia(material.id);
    try {
      const jpeg = await przygotujZdjecie(plik);
      await dane(supabase.storage.from(BUCKET_ZDJEC).upload(nowa, jpeg, { contentType: "image/jpeg" }));
      try {
        await zapiszSciezke(material.id, nowa);
      } catch (blad) {
        await usunPlikPoCichu(nowa); // nie zostawiamy pliku, do którego nic nie prowadzi
        throw blad;
      }
      if (sciezka) await usunPlikPoCichu(sciezka);
      pokaz("Zapisano zdjęcie");
      onZmiana();
    } catch (blad) {
      pokaz(komunikat(blad));
    } finally {
      ustawTrwa(false);
    }
  }

  async function usunZdjecie() {
    ustawPotwierdz(false);
    if (!sciezka || trwa) return;
    ustawTrwa(true);
    try {
      await zapiszSciezke(material.id, null);
      await usunPlikPoCichu(sciezka);
      pokaz("Usunięto zdjęcie");
      onZmiana();
    } catch (blad) {
      pokaz(komunikat(blad));
    } finally {
      ustawTrwa(false);
    }
  }

  return (
    <div>
      {sciezka && (
        <Box
          onClick={() => link && ustawPelnyEkran(true)}
          sx={{ height: 240, borderRadius: 3, bgcolor: "action.hover", display: "flex", alignItems: "center", justifyContent: "center", overflow: "hidden", cursor: "zoom-in" }}
        >
          {bladZdjecia ? (
            <Typography color="text.secondary">Nie udało się wczytać zdjęcia</Typography>
          ) : link ? (
            <img src={link} alt="Zdjęcie materiału" style={{ maxWidth: "100%", maxHeight: "100%", objectFit: "contain" }} />
          ) : (
            <CircularProgress />
          )}
        </Box>
      )}
      <input ref={aparat} type="file" accept="image/*" capture="environment" hidden onChange={ustawZdjecie} />
      <input ref={galeria} type="file" accept="image/*" hidden onChange={ustawZdjecie} />
      <Stack direction="row" sx={{ flexWrap: "wrap" }}>
        <Button disabled={trwa} onClick={() => aparat.current?.click()}>
          {sciezka ? "Nowe zdjęcie" : "Zrób zdjęcie"}
        </Button>
        <Button disabled={trwa} onClick={() => galeria.current?.click()}>
          Z galerii
        </Button>
        {sciezka && (
          <Button color="error" disabled={trwa} onClick={() => ustawPotwierdz(true)}>
            Usuń
          </Button>
        )}
      </Stack>
      {trwa && <LinearProgress />}
      {pelnyEkran && link && (
        <Dialog open fullScreen onClose={() => ustawPelnyEkran(false)}>
          <Box onClick={() => ustawPelnyEkran(false)} sx={{ bgcolor: "#000", height: "100%", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <img src={link} alt="Zdjęcie materiału" style={{ maxWidth: "100%", maxHeight: "100%", objectFit: "contain" }} />
          </Box>
        </Dialog>
      )}
      {potwierdz && <Potwierdzenie tytul="Usunąć zdjęcie?" przycisk="Usuń" onTak={() => void usunZdjecie()} onNie={() => ustawPotwierdz(false)} />}
    </div>
  );
}

// Historia ruchów od najnowszych, po 20; pokazuje też, czy wydanie się zapisało po zerwanym połączeniu
function Historia({ material }: { material: Material }) {
  const [lista, ustawListe] = useState<Ruch[] | null>(null);
  const [saStarsze, ustawSaStarsze] = useState(false);
  const [laduje, ustawLaduje] = useState(false);
  const [blad, ustawBlad] = useState<string | null>(null);

  async function wczytaj(od: number, dotychczasowe: Ruch[]) {
    ustawLaduje(true);
    ustawBlad(null);
    try {
      // Jeden wiersz więcej niż strona, żeby wiedzieć, czy są starsze
      const strona = await dane<Ruch[]>(
        supabase
          .from("ruchy")
          .select(KOLUMNY_HISTORII)
          .eq("material_id", material.id)
          .order("utworzono", { ascending: false })
          .order("id", { ascending: false })
          .range(od, od + STRONA_HISTORII)
          // Typy Supabase nie rozczytują sklejonej listy kolumn z dołączeniami
          .overrideTypes<Ruch[], { merge: false }>(),
      );
      const znane = new Set(dotychczasowe.map((r) => r.id));
      ustawListe([...dotychczasowe, ...strona.slice(0, STRONA_HISTORII).filter((r) => !znane.has(r.id))]);
      ustawSaStarsze(strona.length > STRONA_HISTORII);
    } catch (e) {
      ustawBlad(komunikat(e));
    } finally {
      ustawLaduje(false);
    }
  }

  // Także po każdej zmianie materiału (np. nowym zdjęciu)
  useEffect(() => {
    void wczytaj(0, []);
  }, [material]);

  if (!lista) return blad ? <Typography color="error">{blad}</Typography> : <LinearProgress />;
  if (lista.length === 0) return <Typography color="text.secondary">Brak wydań i przyjęć.</Typography>;
  return (
    <div>
      {lista.map((r) => (
        <WierszRuchu key={r.id} ruch={r} material={material} />
      ))}
      {blad && <Typography color="error">{blad}</Typography>}
      {saStarsze && (
        <Button disabled={laduje} onClick={() => void wczytaj(lista.length, lista)}>
          Pokaż starsze
        </Button>
      )}
    </div>
  );
}

function WierszRuchu({ ruch, material }: { ruch: Ruch; material: Material }) {
  const status = statusRuchu(ruch.typ, ruch.anulowano, ruch.zaksiegowano_sap, material.typ === "SAP");
  const anulowany = status === "ANULOWANY";
  const kto = (o: { nazwa: string } | null) => (o ? ` · ${emailNaLogin(o.nazwa)}` : "");
  const szczegoly =
    status === "ANULOWANY" ? `${formatujDate(ruch.anulowano!)}${kto(ruch.kto_anulowal)}`
    : status === "ZAKSIEGOWANY" ? `${formatujDate(ruch.zaksiegowano_sap!)}${kto(ruch.kto_ksiegowal)}`
    : undefined;
  return (
    <>
      <Stack direction="row" spacing={1.5} sx={{ py: 1 }}>
        <Typography
          variant="subtitle1"
          sx={{ width: 96, flexShrink: 0, textDecoration: anulowany ? "line-through" : undefined }}
          color={anulowany ? "text.secondary" : ruch.typ === "PRZYJECIE" ? "primary" : "text.primary"}
        >
          {ruch.typ === "WYDANIE" ? "−" : "+"}
          {formatujIlosc(ruch.ilosc)} {material.jednostka}
        </Typography>
        <Box sx={{ minWidth: 0 }}>
          <Typography>
            {ruch.typ === "PRZYJECIE" ? "Przyjęcie" : ruch.maszyna ? `Wydanie → ${opisMaszyny(ruch.maszyna)}` : "Wydanie"}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {formatujDate(ruch.utworzono)} · {ruch.kto ? emailNaLogin(ruch.kto.nazwa) : "?"}
          </Typography>
          {ruch.uwagi?.trim() && <Typography variant="body2">Uwagi: {ruch.uwagi}</Typography>}
          <TekstStatusu status={status} szczegoly={szczegoly} />
        </Box>
      </Stack>
      <Divider />
    </>
  );
}

