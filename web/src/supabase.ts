import { createClient } from "@supabase/supabase-js";

// Ten sam projekt Supabase co aplikacja na Androida i program na Windows.
// Klucz publishable jest jawny z założenia; dane chroni logowanie i RLS w bazie.
export const supabase = createClient(import.meta.env.VITE_SUPABASE_URL, import.meta.env.VITE_SUPABASE_KEY);

// Wynik zapytania albo wyjątek z błędem Supabase (komunikat() zamienia go na polski tekst)
export async function dane<T>(zapytanie: PromiseLike<{ data: T | null; error: unknown }>): Promise<T> {
  const { data, error } = await zapytanie;
  if (error) throw error;
  return data as T;
}
