import { blad, ok, type Wynik } from "./wynik";

// Konta pracowników zakładamy na techniczne adresy w tej domenie
const DOMENA = "@magazyn.local";

// Pracownik wpisuje sam login. Pełny e-mail (konto administratora) zostaje bez domeny.
export function loginNaEmail(login: string): string {
  const znormalizowany = login.trim().toLowerCase();
  return znormalizowany.includes("@") ? znormalizowany : znormalizowany + DOMENA;
}

export function emailNaLogin(email: string): string {
  return email.endsWith(DOMENA) ? email.slice(0, -DOMENA.length) : email;
}

// Ta sama reguła co w Edge Function „uzytkownicy” i w aplikacji (normalizujLogin)
const LOGIN = /^[a-z0-9][a-z0-9._-]{1,49}$/;

export function normalizujLogin(tekst: string): Wynik<string> {
  const login = tekst.trim().toLowerCase();
  if (!login) return blad("Wpisz login");
  if (login.includes("@")) return blad("Wpisz sam login, bez @ i domeny");
  if (!LOGIN.test(login)) {
    return blad("Login: 2–50 znaków, małe litery bez polskich znaków, cyfry, kropka, myślnik lub podkreślenie");
  }
  return ok(login);
}
