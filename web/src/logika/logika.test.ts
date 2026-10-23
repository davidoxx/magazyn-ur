import { describe, expect, test } from "vitest";
import { daneMaszyny, daneMaterialu, daneStacji, parametryNowegoMaterialu, zmianyStacji } from "./dane";
import { formatujDate, odmiana } from "./formatowanie";
import { formatujIlosc, parsujIlosc } from "./ilosc";
import { emailNaLogin, loginNaEmail, normalizujLogin } from "./login";
import { doPorownania, pasujeDoFrazy } from "./szukanie";
import { opisStacji, posortujMaszyny, posortujStacje, posortujUzytkownikow, statusRuchu, type Stacja } from "./typy";
import { rozmiarDocelowy } from "./zdjecia";
import type { Wynik } from "./wynik";

const blad = (w: Wynik<unknown>) => (w.ok ? null : w.blad);
const wartosc = <T>(w: Wynik<T>) => (w.ok ? w.wartosc : undefined);

describe("logowanie", () => {
  test("login dostaje domenę magazynu, pełny e-mail zostaje", () => {
    expect(loginNaEmail("  Jan.Kowalski ")).toBe("jan.kowalski@magazyn.local");
    expect(loginNaEmail(" Admin@Firma.pl")).toBe("admin@firma.pl");
    expect(emailNaLogin("jan.kowalski@magazyn.local")).toBe("jan.kowalski");
    expect(emailNaLogin("admin@firma.pl")).toBe("admin@firma.pl");
  });

  test("reguła loginu nowego konta", () => {
    expect(wartosc(normalizujLogin("  Jan.Kowalski "))).toBe("jan.kowalski");
    expect(blad(normalizujLogin("  "))).toBe("Wpisz login");
    expect(blad(normalizujLogin("jan@magazyn.local"))).toBe("Wpisz sam login, bez @ i domeny");
    for (const zly of ["łukasz", "jan kowalski", ".jan", "j", "a".repeat(51)]) {
      expect(blad(normalizujLogin(zly))).toMatch(/^Login: 2–50 znaków/);
    }
  });
});

describe("ilość", () => {
  test("przecinek i kropka, najwyżej 3 miejsca po przecinku", () => {
    expect(wartosc(parsujIlosc(" 2,5 "))).toBe(2.5);
    expect(wartosc(parsujIlosc("2.5"))).toBe(2.5);
    expect(wartosc(parsujIlosc("0,125"))).toBe(0.125);
    // Zera na końcu nie liczą się jako miejsca po przecinku
    expect(wartosc(parsujIlosc("1,5000"))).toBe(1.5);
    expect(blad(parsujIlosc("0,0005"))).toBe("Najwyżej 3 miejsca po przecinku");
  });

  test("błędne ilości", () => {
    expect(blad(parsujIlosc("  "))).toBe("Wpisz ilość");
    expect(blad(parsujIlosc("2,5,1"))).toBe("Nieprawidłowa ilość");
    expect(blad(parsujIlosc("abc"))).toBe("Nieprawidłowa ilość");
    expect(blad(parsujIlosc("0"))).toBe("Ilość musi być większa od zera");
    expect(blad(parsujIlosc("-3"))).toBe("Ilość nie może być ujemna");
    expect(blad(parsujIlosc("1000000000"))).toBe("Za duża ilość");
    expect(wartosc(parsujIlosc("0", true))).toBe(0);
  });

  test("ilość po polsku", () => {
    expect(formatujIlosc(12)).toBe("12");
    expect(formatujIlosc(12.5)).toBe("12,5");
    expect(formatujIlosc(0.125)).toBe("0,125");
    expect(formatujIlosc(1250)).toBe("1250");
  });
});

describe("formatowanie", () => {
  test("odmiana po liczbie", () => {
    const wydan = (n: number) => odmiana(n, "wydanie", "wydania", "wydań");
    expect([0, 1, 2, 4, 5, 12, 14, 22, 112].map(wydan)).toEqual([
      "wydań", "wydanie", "wydania", "wydania", "wydań", "wydań", "wydań", "wydania", "wydań",
    ]);
  });

  test("data z bazy w czasie polskim", () => {
    expect(formatujDate("2026-10-08T12:32:10.123456+00:00", "Europe/Warsaw")).toBe("08.10.2026 14:32");
    expect(formatujDate("2026-01-15T08:00:00+00:00", "Europe/Warsaw")).toBe("15.01.2026 09:00");
    expect(formatujDate("wczoraj", "Europe/Warsaw")).toBe("wczoraj");
  });
});

describe("szukanie maszyn", () => {
  test("bez wielkości liter i polskich znaków, każde słowo musi wystąpić", () => {
    expect(doPorownania("ŻÓŁĆ Łódź")).toBe("zolc lodz");
    const maszyna = "Prasa krawędziowa 2 PL01-L2-PR02";
    expect(pasujeDoFrazy(maszyna, "prasa krawedziowa")).toBe(true);
    expect(pasujeDoFrazy(maszyna, "pr02  prasa")).toBe(true);
    expect(pasujeDoFrazy(maszyna, "")).toBe(true);
    expect(pasujeDoFrazy(maszyna, "prasa tokarka")).toBe(false);
  });

  test("aktywne najpierw, potem alfabetycznie po polsku", () => {
    const maszyny = ["Żuraw", "Śrutownica", "Tokarka", "Spawarka"].map((nazwa, id) => ({ id, nazwa, kod: null, aktywna: true }));
    const wynik = posortujMaszyny([...maszyny, { id: 9, nazwa: "Agregat", kod: null, aktywna: false }]);
    expect(wynik.map((m) => m.nazwa)).toEqual(["Spawarka", "Śrutownica", "Tokarka", "Żuraw", "Agregat"]);
  });

  test("użytkownicy: aktywni najpierw alfabetycznie", () => {
    const wynik = posortujUzytkownikow([
      { id: "1", nazwa: "zenon", poziom: 1 },
      { id: "2", nazwa: "anna@magazyn.local", poziom: 2 },
      { id: "3", nazwa: "bartek", poziom: 0 },
      { id: "4", nazwa: "łucja", poziom: 3 },
    ]);
    expect(wynik.map((u) => u.id)).toEqual(["2", "4", "1", "3"]);
  });
});

describe("dane formularzy", () => {
  const dane = (zmiany: Partial<Parameters<typeof daneMaterialu>[0]> = {}) =>
    daneMaterialu({ nazwa: "Śruba M8", typ: "SAP", nrSap: "10023456", miejsce: "", jednostka: "szt", opis: "", iloscTekst: null, ...zmiany });

  test("wymagane pola", () => {
    expect(blad(dane({ nazwa: "  " }))).toBe("Wpisz nazwę");
    expect(blad(dane({ typ: null }))).toBe("Wybierz typ: SAP albo kosztowy");
    expect(blad(dane({ nrSap: " " }))).toBe("Wpisz numer SAP");
    expect(blad(dane({ jednostka: "" }))).toBe("Wpisz jednostkę");
    expect(blad(dane({ iloscTekst: "1,0001" }))).toBe("Najwyżej 3 miejsca po przecinku");
  });

  test("kosztowy bez numeru SAP, puste pola jako null, bez ilości przy edycji", () => {
    expect(wartosc(dane({ typ: "KOSZTOWY" }))).toMatchObject({ typ: "KOSZTOWY", nr_sap: null });
    const w = wartosc(dane({ nazwa: "  Śruba M8 ", miejsce: " " }));
    expect(w).toMatchObject({ nazwa: "Śruba M8", miejsce: null, opis: null });
    expect(w && "ilosc" in w).toBe(false);
    expect(wartosc(dane({ iloscTekst: "2,5" }))?.ilosc).toBe(2.5);
    expect(wartosc(dane({ iloscTekst: "0" }))?.ilosc).toBe(0);
  });

  test("parametry funkcji dodaj_material", () => {
    const w = wartosc(dane({ iloscTekst: "5" }))!;
    const p = parametryNowegoMaterialu(w, " 4500012345 ");
    expect(p).toMatchObject({ p_nazwa: "Śruba M8", p_nr_sap: "10023456", p_miejsce: null, p_ilosc: 5, p_nr_zamowienia: "4500012345" });
    // Tylko parametry funkcji
    expect(Object.keys(p).filter((k) => !k.startsWith("p_"))).toEqual([]);
    expect(parametryNowegoMaterialu(wartosc(dane())!, "  ").p_nr_zamowienia).toBeNull();
  });

  test("stacje nowego materiału i zmiany przy edycji", () => {
    const w = wartosc(dane())!;
    expect(parametryNowegoMaterialu(w, "", [1, 2, 1]).p_stacje).toEqual([1, 2]);
    expect("p_stacje" in parametryNowegoMaterialu(w, "")).toBe(false);
    expect(zmianyStacji(7, [1, 2], [2, 3])).toEqual({ p_material_id: 7, p_dodaj: [3], p_usun: [1] });
    // Bez zmian (także inna kolejność) nie ma wywołania
    expect(zmianyStacji(7, [1, 2], [2, 1])).toBeNull();
  });

  test("stacja: obszar, nazwa, pusty kod jako null", () => {
    expect(blad(daneStacji(null, "Stacja 10", "", true))).toBe("Wybierz F1, F2 albo F3");
    expect(blad(daneStacji("F4", "Stacja 10", "", true))).toBe("Wybierz F1, F2 albo F3");
    expect(blad(daneStacji("F1", "  ", "", true))).toBe("Wpisz nazwę stacji");
    expect(wartosc(daneStacji("F2", " Stacja 10 ", "  ", false))).toEqual({ obszar: "F2", nazwa: "Stacja 10", kod: null, aktywna: false });
  });

  test("stacje: sortowanie i opis", () => {
    const s = (id: number, obszar: string, nazwa: string, aktywna = true, kod: string | null = null): Stacja => ({ id, obszar, nazwa, kod, aktywna });
    const lista = [s(1, "F2", "Prasa"), s(2, "F1", "Zgrzewarka"), s(3, "F1", "Stara", false), s(4, "F1", "Śrubowanie", true, "PL01-F1-ST40")];
    // Obszar, aktywne najpierw, „Ś” po „S”, a nie za „Z”
    expect(posortujStacje(lista).map((x) => x.id)).toEqual([4, 2, 3, 1]);
    expect(opisStacji(lista[3])).toBe("F1 · Śrubowanie (PL01-F1-ST40)");
    expect(opisStacji(lista[0])).toBe("F2 · Prasa");
  });

  test("maszyna: pusty kod jako null", () => {
    expect(blad(daneMaszyny("  ", "X1", true))).toBe("Wpisz nazwę maszyny");
    expect(wartosc(daneMaszyny(" Tokarka CNC ", "  ", false))).toEqual({ nazwa: "Tokarka CNC", kod: null, aktywna: false });
  });
});

describe("status i zdjęcia", () => {
  test("status ruchu", () => {
    expect(statusRuchu("WYDANIE", null, null, true)).toBe("CZEKA_NA_SAP");
    expect(statusRuchu("WYDANIE", null, "x", true)).toBe("ZAKSIEGOWANY");
    expect(statusRuchu("WYDANIE", "x", null, true)).toBe("ANULOWANY");
    expect(statusRuchu("WYDANIE", null, null, false)).toBeNull();
    expect(statusRuchu("PRZYJECIE", null, null, true)).toBeNull();
    // Utworzenie nie czeka na zaksięgowanie, także przy materiale SAP
    expect(statusRuchu("UTWORZENIE", null, null, true)).toBeNull();
  });

  test("dłuższy bok do limitu z zachowaniem proporcji", () => {
    expect(rozmiarDocelowy(4000, 3000, 1280)).toEqual([1280, 960]);
    expect(rozmiarDocelowy(3000, 4000, 1280)).toEqual([960, 1280]);
    expect(rozmiarDocelowy(800, 600, 1280)).toEqual([800, 600]);
    expect(rozmiarDocelowy(5000, 1, 1280)).toEqual([1280, 1]);
  });
});
