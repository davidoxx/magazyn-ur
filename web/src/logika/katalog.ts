import { doPorownania } from "./szukanie";

// Katalog części z arkusza Excel (plik tworzy narzedzia/katalog_z_excela.py).
// Strona pobiera go z prywatnego bucketu „katalog” w Supabase, tylko po zalogowaniu.
// Wyszukiwarka działa tak samo jak w aplikacji (katalog/Wyszukiwarka.kt); zmieniaj obie naraz.
export interface PozycjaKatalogu {
  sap: string;
  // Krótki tekst z SAP (arkusz Data)
  nazwa: string;
  // Zwykle jedno, ale ten sam numer bywa w kilku miejscach
  miejsca: string[];
  // Pełniejsze nazwy z arkuszy Magazyn
  inneNazwy: string[];
  dostawcy: string[];
}

export interface Katalog {
  zrodlo: string;
  // Data utworzenia pliku, RRRR-MM-DD
  utworzono: string;
  pozycje: PozycjaKatalogu[];
}

export interface WynikKatalogu {
  pozycje: PozycjaKatalogu[];
  wszystkich: number;
}

const tekst = (w: unknown) => (typeof w === "string" ? w : "");
const lista = (w: unknown) => (Array.isArray(w) ? w.filter((x): x is string => typeof x === "string") : []);

// Puste listy plik pomija (jak w aplikacji: wartości domyślne). Pusty albo zły plik to błąd.
export function odczytajKatalog(json: unknown): Katalog {
  const k = json as { zrodlo?: unknown; utworzono?: unknown; pozycje?: unknown } | null;
  if (!k || !Array.isArray(k.pozycje)) throw new Error("To nie jest plik katalogu");
  const pozycje = (k.pozycje as Record<string, unknown>[])
    .filter((p) => p && typeof p.sap === "string")
    .map((p) => ({
      sap: p.sap as string,
      nazwa: tekst(p.nazwa),
      miejsca: lista(p.miejsca),
      inneNazwy: lista(p.inneNazwy),
      dostawcy: lista(p.dostawcy),
    }));
  if (pozycje.length === 0) throw new Error("Katalog bez pozycji");
  return { zrodlo: tekst(k.zrodlo), utworzono: tekst(k.utworzono), pozycje };
}

const LICZBA = new Intl.NumberFormat("pl-PL");

// Np. „Czesci zam 2508.xlsx z 08.10.2026 · 10 332 pozycji”
export function opisKatalogu(k: Katalog): string {
  const d = /^(\d{4})-(\d{2})-(\d{2})$/.exec(k.utworzono);
  const data = d ? `${d[3]}.${d[2]}.${d[1]}` : k.utworzono;
  return `${k.zrodlo} z ${data} · ${LICZBA.format(k.pozycje.length)} pozycji`;
}

const ROZDZIELACZE = /[^\p{L}\p{N}]+/u;
const PELNY_SAP = /^538\d{9}$/;
const TYLKO_CYFRY = /^\d+$/;
const PROG_LITEROWEK = 10;

// Słowa bez wielkości liter, polskich znaków i znaków rozdzielających:
// "Matryca 1586947 + TiN" -> [matryca, 1586947, tin]
export function slowa(t: string): string[] {
  return doPorownania(t).split(ROZDZIELACZE).filter(Boolean);
}

interface Wpis {
  pozycja: PozycjaKatalogu;
  // Każde pole zbite w ciąg liter i cyfr, pola rozdzielone '|',
  // żeby fraza nie skleiła końca nazwy z początkiem miejsca
  zbite: string;
  slowa: string[];
  sapZbity: string;
  // Końcówka numeru: 538000001000 -> 1000 (tak numerują arkusze Magazyn)
  koncowka: number | null;
}

// Wyszukiwarka odporna na sposób zapisu. „EcoRail2”, „ecorail 2” i „EcoRail-2”
// to dla niej to samo, podobnie „3,969” i „3.969”. Działa w pamięci przeglądarki.
export class Wyszukiwarka {
  private readonly wpisy: Wpis[];
  private readonly wszystkieSlowa: Set<string>;

  constructor(pozycje: PozycjaKatalogu[]) {
    this.wpisy = pozycje.map((pozycja) => {
      const pola = [pozycja.sap, pozycja.nazwa, ...pozycja.inneNazwy, ...pozycja.miejsca, ...pozycja.dostawcy].map(slowa);
      return {
        pozycja,
        zbite: pola.map((s) => s.join("")).join("|"),
        slowa: [...new Set(pola.flat())],
        sapZbity: slowa(pozycja.sap).join(""),
        koncowka: PELNY_SAP.test(pozycja.sap) ? Number(pozycja.sap.slice(3)) : null,
      };
    });
    this.wszystkieSlowa = new Set(this.wpisy.flatMap((w) => w.slowa));
  }

  szukaj(fraza: string, limit: number): WynikKatalogu {
    const tokeny = slowa(fraza);
    if (tokeny.length === 0) return { pozycje: [], wszystkich: 0 };
    const zbitaFraza = tokeny.join("");
    const liczba = TYLKO_CYFRY.test(zbitaFraza) && Number.isSafeInteger(Number(zbitaFraza)) ? Number(zbitaFraza) : null;

    const trafienia: [Wpis, number][] = [];
    for (const wpis of this.wpisy) {
      const punkty = punktyDokladne(wpis, tokeny, zbitaFraza, liczba);
      if (punkty > 0) trafienia.push([wpis, punkty]);
    }
    // Literówki sprawdzamy dopiero, gdy dokładnych trafień jest mało, żeby nie zaśmiecać wyników
    if (trafienia.length < PROG_LITEROWEK) {
      const znalezione = new Set(trafienia.map(([w]) => w));
      trafienia.push(...this.zLiterowkami(tokeny).filter(([w]) => !znalezione.has(w)));
    }

    // sort jest stabilny: przy równych punktach zostaje kolejność z pliku (jak w aplikacji)
    trafienia.sort((a, b) => b[1] - a[1]);
    return { pozycje: trafienia.slice(0, limit).map(([w]) => w.pozycja), wszystkich: trafienia.length };
  }

  // Każde słowo frazy musi wystąpić dokładnie albo z literówką w którymś słowie pozycji
  private zLiterowkami(tokeny: string[]): [Wpis, number][] {
    const podobne = tokeny.map((token) => {
      const dozwolone = dozwolonaRoznica(token);
      if (dozwolone === 0) return null;
      const roznice = new Map<string, number>();
      for (const slowo of this.wszystkieSlowa) {
        const r = roznicaZPoczatkiem(token, slowo, dozwolone);
        if (r !== null) roznice.set(slowo, r);
      }
      return roznice;
    });
    if (podobne.every((p) => p === null)) return [];

    const wynik: [Wpis, number][] = [];
    for (const wpis of this.wpisy) {
      let suma = 0;
      const pasuje = tokeny.every((token, i) => {
        if (wpis.zbite.includes(token)) return true;
        const roznice = podobne[i];
        if (!roznice) return false;
        let najmniejsza: number | null = null;
        for (const s of wpis.slowa) {
          const r = roznice.get(s);
          if (r !== undefined && (najmniejsza === null || r < najmniejsza)) najmniejsza = r;
        }
        if (najmniejsza === null) return false;
        suma += najmniejsza;
        return true;
      });
      if (pasuje && suma > 0) wynik.push([wpis, 100 - 10 * suma]);
    }
    return wynik;
  }
}

function punktyDokladne(wpis: Wpis, tokeny: string[], zbitaFraza: string, liczba: number | null): number {
  if (liczba !== null) {
    if (wpis.sapZbity === zbitaFraza) return 1000;
    if (wpis.koncowka === liczba) return 900;
  }
  if (wpis.zbite.startsWith(zbitaFraza) || wpis.zbite.includes(`|${zbitaFraza}`)) return 650;
  if (wpis.zbite.includes(zbitaFraza)) return 600;
  if (tokeny.every((t) => wpis.zbite.includes(t))) {
    // Premia za słowa trafione od początku: „lozysko” lepiej niż „...lozyskowy”
    return 300 + 10 * tokeny.filter((t) => wpis.slowa.some((s) => s.startsWith(t))).length;
  }
  return 0;
}

// Krótkich słów i numerów katalogowych nie poprawiamy: „6204” to nie „6240”
export function dozwolonaRoznica(token: string): number {
  if (/\d/.test(token)) return 0;
  if (token.length < 4) return 0;
  if (token.length < 8) return 1;
  return 2;
}

// Różnica z całym słowem albo z jego początkiem (ktoś nie dopisał końcówki)
export function roznicaZPoczatkiem(token: string, slowo: string, dozwolone: number): number | null {
  if (slowo.length < token.length - dozwolone) return null;
  const kandydaci = new Set([slowo, slowo.slice(0, token.length), slowo.slice(0, token.length + 1)]);
  let min = Infinity;
  for (const k of kandydaci) min = Math.min(min, odleglosc(token, k, dozwolone));
  return min <= dozwolone ? min : null;
}

// Odległość edycyjna z przestawieniem sąsiednich liter („czjunik” -> „czujnik” to 1).
// Przerywa, gdy wiadomo już, że przekroczy limit.
export function odleglosc(a: string, b: string, limit: number): number {
  if (Math.abs(a.length - b.length) > limit) return limit + 1;
  let przedPoprzedni = new Array<number>(b.length + 1).fill(0);
  let poprzedni = Array.from({ length: b.length + 1 }, (_, i) => i);
  let biezacy = new Array<number>(b.length + 1).fill(0);
  for (let i = 1; i <= a.length; i++) {
    biezacy[0] = i;
    let minimumWiersza = i;
    for (let j = 1; j <= b.length; j++) {
      const koszt = a[i - 1] === b[j - 1] ? 0 : 1;
      let d = Math.min(poprzedni[j] + 1, biezacy[j - 1] + 1, poprzedni[j - 1] + koszt);
      if (i > 1 && j > 1 && a[i - 1] === b[j - 2] && a[i - 2] === b[j - 1]) d = Math.min(d, przedPoprzedni[j - 2] + 1);
      biezacy[j] = d;
      minimumWiersza = Math.min(minimumWiersza, d);
    }
    if (minimumWiersza > limit) return limit + 1;
    [przedPoprzedni, poprzedni, biezacy] = [poprzedni, biezacy, przedPoprzedni];
  }
  return poprzedni[b.length];
}
