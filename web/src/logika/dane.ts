import { parsujIlosc } from "./ilosc";
import type { TypMaterialu } from "./typy";
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
