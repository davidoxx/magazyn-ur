import { emailNaLogin } from "./login";
import { porownajPoPolsku } from "./szukanie";

// Wiersze tabel i widoków w bazie (nazwy pól jak kolumny)

export type TypMaterialu = "SAP" | "KOSZTOWY";

export interface Material {
  id: number;
  nazwa: string;
  opis: string | null;
  typ: TypMaterialu;
  nr_sap: string | null;
  miejsce: string | null;
  ilosc: number;
  jednostka: string;
  zdjecie: string | null;
  // Lokalizacje funkcjonalne; tylko gdy zapytanie je dołącza (KOLUMNY_MATERIALU)
  stacje?: Stacja[];
}

// Obszary, na które dzielą się stacje (w bazie: check obszar in ('F1', 'F2', 'F3'))
export const OBSZARY = ["F1", "F2", "F3"] as const;

// Wiersz tabeli stacje: lokalizacja funkcjonalna, do której materiał jest przeznaczony.
// Osobna lista od maszyn, na które idą wydania.
export interface Stacja {
  id: number;
  obszar: string;
  nazwa: string;
  kod: string | null;
  aktywna: boolean;
}

// Materiał z przypisanymi stacjami (przez tabelę materialy_stacje)
export const KOLUMNY_MATERIALU = "*,stacje(id,obszar,nazwa,kod,aktywna)";

// „F1 · Stacja 10 (PL01-F1-ST10)”
export const opisStacji = (s: Pick<Stacja, "obszar" | "nazwa" | "kod">) => `${s.obszar} · ${s.kod ? `${s.nazwa} (${s.kod})` : s.nazwa}`;

// Według obszaru, aktywne najpierw, potem alfabetycznie po polsku
export function posortujStacje(lista: Stacja[]): Stacja[] {
  return [...lista].sort(
    (a, b) => a.obszar.localeCompare(b.obszar) || Number(!a.aktywna) - Number(!b.aktywna) || porownajPoPolsku(a.nazwa, b.nazwa),
  );
}

export interface Maszyna {
  id: number;
  nazwa: string;
  kod: string | null;
  aktywna: boolean;
}

export const opisMaszyny = (m: Pick<Maszyna, "nazwa" | "kod">) => (m.kod ? `${m.nazwa} (${m.kod})` : m.nazwa);

// UTWORZENIE: wpis przy dodaniu materiału, ilosc = stan początkowy (może być 0)
export type TypRuchu = "WYDANIE" | "PRZYJECIE" | "UTWORZENIE";

// Wiersz tabeli ruchy z dołączonymi osobami i maszyną (KOLUMNY_HISTORII)
export interface Ruch {
  id: number;
  typ: TypRuchu;
  ilosc: number;
  uwagi: string | null;
  // Przy przyjęciu i utworzeniu materiału (opcjonalny)
  nr_zamowienia: string | null;
  utworzono: string;
  zaksiegowano_sap: string | null;
  anulowano: string | null;
  kto: { nazwa: string } | null;
  kto_ksiegowal: { nazwa: string } | null;
  kto_anulowal: { nazwa: string } | null;
  maszyna: Pick<Maszyna, "id" | "nazwa" | "kod"> | null;
}

// ruchy ma trzy klucze obce do uzytkownicy, więc każde dołączenie wskazuje swój klucz
export const KOLUMNY_HISTORII =
  "id,typ,ilosc,uwagi,nr_zamowienia,utworzono,zaksiegowano_sap,anulowano," +
  "kto:uzytkownicy!ruchy_uzytkownik_fkey(nazwa)," +
  "kto_ksiegowal:uzytkownicy!ruchy_zaksiegowal_fkey(nazwa)," +
  "kto_anulowal:uzytkownicy!ruchy_anulowal_fkey(nazwa)," +
  "maszyna:maszyny(id,nazwa,kod)";

export type StatusRuchu = "ANULOWANY" | "ZAKSIEGOWANY" | "CZEKA_NA_SAP" | null;

// Status ważny dla pracownika i lidera. Księgowanie w SAP dotyczy tylko wydań materiałów SAP.
export function statusRuchu(
  typ: TypRuchu,
  anulowano: string | null,
  zaksiegowanoSap: string | null,
  materialSap: boolean,
): StatusRuchu {
  if (anulowano) return "ANULOWANY";
  if (typ !== "WYDANIE") return null;
  if (zaksiegowanoSap) return "ZAKSIEGOWANY";
  return materialSap ? "CZEKA_NA_SAP" : null;
}

// Wiersz widoku ostatnie_wydania: material to cały wiersz materiału (to_jsonb)
export interface OstatnieWydanie {
  id: number;
  utworzono: string;
  ilosc: number;
  uwagi: string | null;
  zaksiegowano_sap: string | null;
  anulowano: string | null;
  material: Material;
  wydal: string;
  maszyna_kod: string | null;
  maszyna: string | null;
}

// Wiersz widoku wydania_do_sap
export interface WydanieDoSap {
  id: number;
  utworzono: string;
  nr_sap: string;
  material: string;
  ilosc: number;
  jednostka: string;
  wydal: string;
  uwagi: string | null;
  maszyna_kod: string | null;
  maszyna: string | null;
}

export interface Uzytkownik {
  id: string;
  nazwa: string;
  poziom: number;
}

export const POZIOMY = [0, 1, 2, 3] as const;

export function nazwaPoziomu(poziom: number): string {
  return ["Nieaktywne", "Pracownik", "Lider zmiany", "Administrator"][poziom] ?? `Poziom ${poziom}`;
}

export function opisPoziomu(poziom: number): string {
  return (
    [
      "brak dostępu, historia zostaje",
      "wyszukiwanie, wydania, przyjęcia, materiały",
      "jak pracownik, plus księgowanie w SAP i anulowanie wydań",
      "jak lider, plus użytkownicy i maszyny",
    ][poziom] ?? ""
  );
}

// Aktywni najpierw, potem alfabetycznie po polsku
export function posortujUzytkownikow(lista: Uzytkownik[]): Uzytkownik[] {
  return [...lista].sort(
    (a, b) => Number(a.poziom === 0) - Number(b.poziom === 0) || porownajPoPolsku(emailNaLogin(a.nazwa), emailNaLogin(b.nazwa)),
  );
}

// Aktywne najpierw, potem alfabetycznie po polsku
export function posortujMaszyny(lista: Maszyna[]): Maszyna[] {
  return [...lista].sort((a, b) => Number(!a.aktywna) - Number(!b.aktywna) || porownajPoPolsku(a.nazwa, b.nazwa));
}
