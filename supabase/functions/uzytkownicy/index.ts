// =====================================================================
//  Edge Function „uzytkownicy”: zakładanie kont i reset haseł innym.
//  Wywołuje ją aplikacja (poziom 3). Funkcja sama sprawdza token i poziom
//  wywołującego, bo działa z kluczem secret, który omija RLS.
//
//  Klucz secret: sekret SECRET_KEY (Edge Functions -> Secrets), a gdy go nie ma,
//  stary klucz service_role, który Supabase wstrzykuje sam.
//
//  Żądanie (POST, JSON):
//    { "akcja": "utworz", "login": "jan.kowalski", "haslo": "...", "poziom": 1 }
//    { "akcja": "resetuj_haslo", "uzytkownik_id": "<uuid>", "haslo": "..." }
//  Odpowiedź: 200 { "id": "<uuid>" } albo błąd { "blad": "polski komunikat" }.
// =====================================================================

import { createClient } from "npm:@supabase/supabase-js@2";

// Konta pracowników mają techniczne adresy, aplikacja dokleja tę domenę do loginu
const DOMENA = "@magazyn.local";
// Musi się zgadzać z regułą w aplikacji (uzytkownicy/Uzytkownik.kt, normalizujLogin)
const LOGIN = /^[a-z0-9][a-z0-9._-]{1,49}$/;

const kluczSecret = Deno.env.get("SECRET_KEY") ?? Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
if (!kluczSecret) throw new Error("Brak sekretu SECRET_KEY w ustawieniach Edge Functions");

const admin = createClient(Deno.env.get("SUPABASE_URL")!, kluczSecret, {
  auth: { persistSession: false, autoRefreshToken: false },
});

type Dane = Record<string, unknown>;

// Strona (PWA) woła funkcję z przeglądarki, która wymaga zgody CORS.
// Dowolne źródło jest bezpieczne: dostęp daje token w nagłówku, nie ciasteczka.
const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

function odpowiedz(status: number, tresc: Dane): Response {
  return new Response(JSON.stringify(tresc), {
    status,
    headers: { ...CORS, "Content-Type": "application/json; charset=utf-8" },
  });
}

function blad(status: number, komunikat: string): Response {
  return odpowiedz(status, { blad: komunikat });
}

// Błędy Supabase Auth po polsku
function komunikatAuth(error: { code?: string; message: string }): string {
  switch (error.code) {
    case "email_exists":
    case "user_already_exists":
      return "Taki login już istnieje";
    case "weak_password":
      return "Hasło jest za słabe albo za krótkie";
    case "user_not_found":
      return "Nie ma takiego użytkownika";
    case "unexpected_failure":
      // Najczęściej trigger tworzący profil: nazwa w tabeli uzytkownicy musi być unikalna
      return "Nie udało się założyć konta. Sprawdź, czy ktoś nie ma już takiej nazwy.";
    default:
      return error.message;
  }
}

async function utworz(dane: Dane): Promise<Response> {
  const login = String(dane.login ?? "").trim().toLowerCase();
  const haslo = String(dane.haslo ?? "");
  const poziom = Number(dane.poziom ?? 1);

  if (!LOGIN.test(login)) {
    return blad(400, "Login: 2–50 znaków, małe litery bez polskich znaków, cyfry, kropka, myślnik lub podkreślenie");
  }
  if (!haslo) return blad(400, "Wpisz hasło");
  if (!Number.isInteger(poziom) || poziom < 0 || poziom > 3) return blad(400, "Poziom musi być od 0 do 3");

  // Trigger po_utworzeniu_konta zakłada profil z poziomem 0 i nazwą z metadanych
  const { data, error } = await admin.auth.admin.createUser({
    email: login + DOMENA,
    password: haslo,
    email_confirm: true,
    user_metadata: { nazwa: login },
  });
  if (error || !data.user) return blad(400, komunikatAuth(error ?? { message: "Nie udało się założyć konta" }));
  const id = data.user.id;

  if (poziom > 0) {
    const { error: bladPoziomu } = await admin.from("uzytkownicy").update({ poziom }).eq("id", id);
    if (bladPoziomu) {
      // Bez konta w połowie: usuwamy je, administrator spróbuje jeszcze raz
      await admin.auth.admin.deleteUser(id);
      return blad(500, "Nie udało się ustawić poziomu, konto nie zostało założone");
    }
  }
  return odpowiedz(200, { id });
}

async function resetujHaslo(dane: Dane, wywolujacy: string): Promise<Response> {
  const id = String(dane.uzytkownik_id ?? "");
  const haslo = String(dane.haslo ?? "");

  if (!id) return blad(400, "Nie wybrano użytkownika");
  if (id === wywolujacy) return blad(400, "Własne hasło zmień w menu „Zmień hasło”");
  if (!haslo) return blad(400, "Wpisz nowe hasło");

  const { error } = await admin.auth.admin.updateUserById(id, { password: haslo });
  if (error) return blad(400, komunikatAuth(error));
  return odpowiedz(200, { id });
}

Deno.serve(async (req) => {
  // Zapytanie przeglądarki „czy wolno?” przed właściwym wywołaniem.
  // Odsyłamy nagłówki, o które pyta, bo supabase-js dokłada czasem własne.
  if (req.method === "OPTIONS") {
    const naglowki = req.headers.get("Access-Control-Request-Headers");
    return new Response("ok", {
      headers: { ...CORS, ...(naglowki ? { "Access-Control-Allow-Headers": naglowki } : {}) },
    });
  }
  if (req.method !== "POST") return blad(405, "Dozwolone tylko żądania POST");

  // Kto woła: token zalogowanego użytkownika, który dokleja aplikacja
  const token = req.headers.get("Authorization")?.replace(/^Bearer\s+/i, "");
  if (!token) return blad(401, "Brak logowania");
  const { data: zalogowany, error: bladTokenu } = await admin.auth.getUser(token);
  if (bladTokenu || !zalogowany.user) return blad(401, "Sesja wygasła. Zaloguj się ponownie.");

  // Tylko poziom 3; klucz secret omija RLS, więc sprawdzamy to tutaj
  const { data: profil, error: bladProfilu } = await admin
    .from("uzytkownicy")
    .select("poziom")
    .eq("id", zalogowany.user.id)
    .maybeSingle();
  if (bladProfilu) {
    // Zły klucz (publishable nie ma dostępu do tabel) albo service_role bez praw (006)
    console.error("Odczyt profilu:", bladProfilu);
    return blad(
      500,
      "Funkcja nie może odczytać tabeli uzytkownicy. Sprawdź, czy sekret SECRET_KEY " +
        "to klucz secret (sb_secret_…) i czy uruchomiony jest skrypt 006. Szczegóły w logach funkcji.",
    );
  }
  if (!profil) return blad(403, "Twoje konto nie ma profilu w tabeli uzytkownicy");
  if (profil.poziom < 3) return blad(403, "Brak uprawnień: potrzebny poziom 3");

  let dane: Dane;
  try {
    dane = await req.json();
  } catch {
    return blad(400, "Nieprawidłowe dane");
  }
  if (typeof dane !== "object" || dane === null) return blad(400, "Nieprawidłowe dane");

  switch (dane.akcja) {
    case "utworz":
      return await utworz(dane);
    case "resetuj_haslo":
      return await resetujHaslo(dane, zalogowany.user.id);
    default:
      return blad(400, "Nieznana akcja");
  }
});
