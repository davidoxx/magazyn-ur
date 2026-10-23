import { FunctionsFetchError, FunctionsHttpError, isAuthError, isAuthWeakPasswordError } from "@supabase/supabase-js";

const BRAK_POLACZENIA = "Brak połączenia z serwerem. Sprawdź internet.";

// Błąd sieci zgłoszony przez przeglądarkę (Chrome, Firefox, Safari mają różne treści)
function bladSieci(tresc: string): boolean {
  return /failed to fetch|networkerror|load failed|network request failed/i.test(tresc);
}

interface BladPostgrest {
  code: string;
  message: string;
  details: unknown;
}

function czyBladPostgrest(e: unknown): e is BladPostgrest {
  return typeof e === "object" && e !== null && "code" in e && "message" in e && "details" in e;
}

// Komunikat błędu do pokazania użytkownikowi. Funkcje w bazie zgłaszają błędy
// po polsku (raise exception), więc ich treść pokazujemy bez zmian.
export function komunikat(e: unknown): string {
  if (isAuthWeakPasswordError(e)) {
    if (e.reasons.includes("pwned")) return "To hasło wyciekło w internecie. Wybierz inne.";
    if (e.reasons.includes("length")) return "Hasło jest za krótkie.";
    return "Hasło jest za słabe. Użyj liter różnej wielkości i cyfr.";
  }
  if (isAuthError(e)) {
    switch (e.code) {
      case "invalid_credentials":
        return "Nieprawidłowy login lub hasło";
      case "same_password":
        return "Nowe hasło musi być inne niż obecne.";
      // Przy włączonej w Supabase opcji „Secure password change”
      // hasło można zmienić tylko krótko po zalogowaniu
      case "reauthentication_needed":
        return "Wyloguj się, zaloguj ponownie i od razu zmień hasło.";
    }
    return bladSieci(e.message) ? BRAK_POLACZENIA : e.message;
  }
  // Ograniczenia tabel zgłasza sam PostgreSQL, po angielsku (kody SQLSTATE)
  if (czyBladPostgrest(e)) {
    if (e.code === "23505" && e.message.includes("nr_sap")) return "Materiał z tym numerem SAP już istnieje";
    if (e.code === "23505" && e.message.includes("maszyny_kod")) return "Maszyna z tym kodem już istnieje";
    if (e.code === "23505" && e.message.includes("stacje_nazwa_w_obszarze")) return "Stacja o tej nazwie już jest w tym obszarze";
    if (e.code === "23505" && e.message.includes("stacje_kod")) return "Stacja z tym kodem już istnieje";
    if (e.code === "23505") return "Taki wpis już istnieje";
    if (e.code === "23503" && e.message.includes('"maszyny"')) {
      return "Tej maszyny nie można usunąć, bo ma wydania w historii. Wycofaj ją zamiast tego.";
    }
    if (e.code === "23503" && e.message.includes('"stacje"')) {
      return "Tej stacji nie można usunąć, bo jest przypisana do materiałów. Wycofaj ją zamiast tego.";
    }
    if (e.code === "23503") return "Nie można usunąć, bo jest używane w historii ruchów.";
    if (e.code === "42501") return "Brak uprawnień";
    return bladSieci(e.message) ? BRAK_POLACZENIA : e.message;
  }
  if (e instanceof Error) return bladSieci(e.message) ? BRAK_POLACZENIA : e.message;
  return "Nieznany błąd";
}

// Edge Function „uzytkownicy” odpowiada {"blad": "..."}; treść trzeba odczytać z odpowiedzi
export async function komunikatFunkcji(e: unknown): Promise<string> {
  if (e instanceof FunctionsHttpError) {
    const odpowiedz = e.context as Response;
    try {
      const tresc = await odpowiedz.clone().json();
      if (typeof tresc?.blad === "string") return tresc.blad;
    } catch {
      // nie JSON – niżej komunikat według statusu
    }
    if (odpowiedz.status === 404) return "Funkcja „uzytkownicy” nie jest wdrożona w Supabase";
    if (odpowiedz.status === 401) {
      return "Supabase odrzucił token. W ustawieniach funkcji wyłącz weryfikację JWT, funkcja sprawdza token sama.";
    }
    return `Błąd funkcji (${odpowiedz.status})`;
  }
  if (e instanceof FunctionsFetchError) {
    // Także wtedy, gdy funkcja nie zezwala na wywołania z przeglądarki (CORS)
    return "Nie udało się połączyć z funkcją „uzytkownicy”. Sprawdź internet i czy funkcja ma najnowszą wersję (CORS).";
  }
  return komunikat(e);
}
