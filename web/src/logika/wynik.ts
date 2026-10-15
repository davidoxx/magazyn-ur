// Wynik walidacji: wartość albo polski komunikat dla użytkownika
export type Wynik<T> = { ok: true; wartosc: T } | { ok: false; blad: string };

export const ok = <T>(wartosc: T): Wynik<T> => ({ ok: true, wartosc });
export const blad = <T = never>(komunikat: string): Wynik<T> => ({ ok: false, blad: komunikat });
