import { createClient } from "@supabase/supabase-js";
import { BUCKET_ZDJEC } from "./logika/zdjecia";

// Ten sam projekt Supabase co aplikacja na Androida i program na Windows.
// Klucz publishable jest jawny z założenia; dane chroni logowanie i RLS w bazie.
export const supabase = createClient(import.meta.env.VITE_SUPABASE_URL, import.meta.env.VITE_SUPABASE_KEY);

// Wynik zapytania albo wyjątek z błędem Supabase (komunikat() zamienia go na polski tekst)
export async function dane<T>(zapytanie: PromiseLike<{ data: T | null; error: unknown }>): Promise<T> {
  const { data, error } = await zapytanie;
  if (error) throw error;
  return data as T;
}

// Bucket jest prywatny, więc zdjęcia pokazujemy przez podpisane linki ważne godzinę.
// Link zapamiętujemy na 50 minut, a prośby z jednej chwili (np. miniatury w wynikach)
// idą do Storage jednym zapytaniem.
const WAZNOSC_LINKU_S = 3600;
const ODNOW_LINK_PO_MS = 50 * 60 * 1000;
const linki = new Map<string, { link: Promise<string>; od: number }>();
let oczekujace: { sciezka: string; ok: (link: string) => void; blad: (e: unknown) => void }[] = [];

export function podpisanyLink(sciezka: string): Promise<string> {
  const zapamietany = linki.get(sciezka);
  if (zapamietany && Date.now() - zapamietany.od < ODNOW_LINK_PO_MS) return zapamietany.link;

  const link = new Promise<string>((ok, blad) => {
    if (oczekujace.length === 0) setTimeout(() => void wyslijOczekujace());
    oczekujace.push({ sciezka, ok, blad });
  });
  // Po błędzie następna prośba spróbuje jeszcze raz
  link.catch(() => {
    if (linki.get(sciezka)?.link === link) linki.delete(sciezka);
  });
  linki.set(sciezka, { link, od: Date.now() });
  return link;
}

async function wyslijOczekujace() {
  const paczka = oczekujace;
  oczekujace = [];
  try {
    const wynik = await dane(supabase.storage.from(BUCKET_ZDJEC).createSignedUrls(paczka.map((p) => p.sciezka), WAZNOSC_LINKU_S));
    for (const p of paczka) {
      const link = wynik.find((w) => w.path === p.sciezka)?.signedUrl;
      if (link) p.ok(link);
      else p.blad(new Error("Nie udało się wczytać zdjęcia"));
    }
  } catch (e) {
    for (const p of paczka) p.blad(e);
  }
}
