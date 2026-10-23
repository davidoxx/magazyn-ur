import { parsujIlosc } from "./ilosc";
import { OBSZARY, type TypMaterialu } from "./typy";
import { blad, ok, type Wynik } from "./wynik";

export interface ZapisMaterialu {
  nazwa: string;
  typ: TypMaterialu;
  nr_sap: string | null;
  miejsce: string | null;
  jednostka: string;
  opis: string | null;
  ilosc?: number;
}

// Dane z formularza do zapisu w tabeli materialy:
// - wysyłamy tylko kolumny, które aplikacja może zapisywać (granty w bazie),
// - puste pola idą jako null, żeby dało się je wyczyścić przy edycji.
// iloscTekst = null przy edycji (stan zmieniają tylko wydaj i przyjmij).
export function daneMaterialu(p: {
  nazwa: string;
  typ: TypMaterialu | null;
  nrSap: string;
  miejsce: string;
  jednostka: string;
  opis: string;
  iloscTekst: string | null;
}): Wynik<ZapisMaterialu> {
  const nazwa = p.nazwa.trim();
  if (!nazwa) return blad("Wpisz nazwę");
  if (!p.typ) return blad("Wybierz typ: SAP albo kosztowy");
  const nrSap = p.nrSap.trim();
  if (p.typ === "SAP" && !nrSap) return blad("Wpisz numer SAP");
  const jednostka = p.jednostka.trim();
  if (!jednostka) return blad("Wpisz jednostkę");

  const dane: ZapisMaterialu = {
    nazwa,
    typ: p.typ,
    // Baza wymaga: SAP ma numer, kosztowy nie ma
    nr_sap: p.typ === "SAP" ? nrSap : null,
    miejsce: p.miejsce.trim() || null,
    jednostka,
    opis: p.opis.trim() || null,
  };
  if (p.iloscTekst?.trim()) {
    const ilosc = parsujIlosc(p.iloscTekst, true);
    if (!ilosc.ok) return ilosc;
    dane.ilosc = ilosc.wartosc;
  }
  return ok(dane);
}

// Parametry funkcji dodaj_material: te same pola z przedrostkiem p_, numer zamówienia i stacje.
// Nowy materiał idzie przez funkcję, żeby numer trafił do wpisu „utworzenie” w historii.
export function parametryNowegoMaterialu(dane: ZapisMaterialu, nrZamowienia: string, stacje: number[] = []): Record<string, unknown> {
  const parametry: Record<string, unknown> = Object.fromEntries(Object.entries(dane).map(([kolumna, wartosc]) => [`p_${kolumna}`, wartosc]));
  parametry.p_nr_zamowienia = nrZamowienia.trim() || null;
  if (stacje.length) parametry.p_stacje = [...new Set(stacje)];
  return parametry;
}

// Parametry zmien_stacje_materialu przy edycji: tylko różnice względem stacji z chwili
// otwarcia formularza, więc nie skasuje stacji dodanych w międzyczasie przez kogoś innego.
// null = nic się nie zmieniło.
export function zmianyStacji(materialId: number, przed: number[], po: number[]) {
  const p_dodaj = [...new Set(po)].filter((id) => !przed.includes(id));
  const p_usun = [...new Set(przed)].filter((id) => !po.includes(id));
  if (!p_dodaj.length && !p_usun.length) return null;
  return { p_material_id: materialId, p_dodaj, p_usun };
}

// Dane z formularza do zapisu w tabeli stacje (tylko kolumny z grantów).
// Pusty kod idzie jako null: kolumna jest unikalna, a nulli może być wiele.
export function daneStacji(
  obszar: string | null,
  nazwa: string,
  kod: string,
  aktywna: boolean,
): Wynik<{ obszar: string; nazwa: string; kod: string | null; aktywna: boolean }> {
  if (!obszar || !(OBSZARY as readonly string[]).includes(obszar)) return blad("Wybierz F1, F2 albo F3");
  const czystaNazwa = nazwa.trim();
  if (!czystaNazwa) return blad("Wpisz nazwę stacji");
  return ok({ obszar, nazwa: czystaNazwa, kod: kod.trim() || null, aktywna });
}

// Dane z formularza do zapisu w tabeli maszyny (tylko kolumny z grantów).
// Pusty kod idzie jako null: kolumna jest unikalna, a nulli może być wiele.
export function daneMaszyny(
  nazwa: string,
  kod: string,
  aktywna: boolean,
): Wynik<{ nazwa: string; kod: string | null; aktywna: boolean }> {
  const czystaNazwa = nazwa.trim();
  if (!czystaNazwa) return blad("Wpisz nazwę maszyny");
  return ok({ nazwa: czystaNazwa, kod: kod.trim() || null, aktywna });
}
