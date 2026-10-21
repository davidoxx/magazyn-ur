import { existsSync, readFileSync } from "node:fs";
import { describe, expect, test } from "vitest";
import { odczytajKatalog, odleglosc, opisKatalogu, Wyszukiwarka, type PozycjaKatalogu } from "./katalog";

// Te same przypadki co WyszukiwarkaTest.kt w aplikacji
const p = (sap: string, nazwa: string, miejsca: string[], inne: Partial<PozycjaKatalogu> = {}): PozycjaKatalogu => ({
  sap,
  nazwa,
  miejsca,
  inneNazwy: [],
  dostawcy: [],
  ...inne,
});

const wyszukiwarka = new Wyszukiwarka([
  p("538000001000", "Zabezpieczenie silnika 3RV1021-4AA15", ["S10A F1"]),
  p("538000011000", "Czujnik indukcyjny BES 516-327-S4-C", ["KLT"]),
  p("538000005425", "Mechanizm srubowo toczny 32x32Rx3,969-3", ["F1"], {
    inneNazwy: ["Mechanizm śrubowo toczny R151039990"],
    dostawcy: ["Bosch-Rexroth"],
  }),
  p("538000012128", "Matryca 1586947 + TiN", ["F1"]),
  p("538000012100", "Zasilacz 1-fazowy EcoRail-2 10ADC 85135", ["F1"]),
  p("538000000008", "lozysko do PF5 6001ZZ", ["S16A F1"], { inneNazwy: ["Łożysko do PF5"] }),
  p("538000WO0001", "Olej VG46 (205L) SW 46", ["OLEJOWNIA"]),
]);

const sap = (fraza: string) => wyszukiwarka.szukaj(fraza, 10).pozycje.map((x) => x.sap);

describe("wyszukiwarka katalogu", () => {
  test("spacje i znaki rozdzielające nie mają znaczenia", () => {
    expect(sap("EcoRail2")[0]).toBe("538000012100");
    expect(sap("ecorail 2")[0]).toBe("538000012100");
    expect(sap("Matryca 1586947+TiN")[0]).toBe("538000012128");
    expect(sap("matryca1586947 tin")[0]).toBe("538000012128");
    expect(sap("bes 516 327")[0]).toBe("538000011000");
    // Przecinek zamiast kropki i odwrotnie
    expect(sap("3.969")[0]).toBe("538000005425");
  });

  test("polskie znaki i inne nazwy", () => {
    expect(sap("Łożysko 6001zz")[0]).toBe("538000000008");
    expect(sap("srubowo toczny bosch")[0]).toBe("538000005425");
  });

  test("numer SAP pełny albo końcówka", () => {
    expect(sap("538000001000")[0]).toBe("538000001000");
    // Końcówka jak w arkuszach Magazyn ma pierwszeństwo przed numerami, które ją tylko zawierają
    expect(sap("1000")).toEqual(["538000001000", "538000011000"]);
    expect(sap("538000wo0001")[0]).toBe("538000WO0001");
  });

  test("miejsce i dostawca", () => {
    expect(sap("s16a")).toEqual(["538000000008"]);
    expect(sap("rexroth")).toEqual(["538000005425"]);
  });

  test("literówki w słowach, ale nie w numerach", () => {
    expect(sap("zabezpieczene silnika")[0]).toBe("538000001000");
    // Przestawione litery
    expect(sap("czjunik")[0]).toBe("538000011000");
    // W numerach katalogowych literówek nie poprawiamy
    expect(sap("1586974")).toEqual([]);
  });

  test("fraza nie łączy nazwy z miejscem", () => {
    // „...TiN” i miejsce „F1” nie tworzą razem „tinf1”
    expect(sap("tinf1")).toEqual([]);
  });

  test("odległość edycyjna", () => {
    expect(odleglosc("czujnk", "czujnik", 2)).toBe(1);
    expect(odleglosc("czjunik", "czujnik", 2)).toBe(1);
    expect(odleglosc("abc", "xyzabc", 2)).toBe(3);
  });
});

describe("plik katalogu", () => {
  test("odczyt z wartościami domyślnymi, pusty i zły plik odrzucone", () => {
    const k = odczytajKatalog({ zrodlo: "a.xlsx", utworzono: "2026-10-21", pozycje: [{ sap: "538000001000", miejsca: ["S10A F1"] }] });
    expect(k.pozycje[0]).toEqual({ sap: "538000001000", nazwa: "", miejsca: ["S10A F1"], inneNazwy: [], dostawcy: [] });
    expect(() => odczytajKatalog({ zrodlo: "a.xlsx", utworzono: "2026-10-21", pozycje: [] })).toThrow();
    expect(() => odczytajKatalog({ blad: "nie ten plik" })).toThrow();
  });

  test("opis z datą i liczbą pozycji", () => {
    const k = { zrodlo: "Czesci zam 2508.xlsx", utworzono: "2026-10-08", pozycje: Array.from({ length: 10_332 }, (_, i) => p(String(i), "", [])) };
    expect(opisKatalogu(k)).toBe("Czesci zam 2508.xlsx z 08.10.2026 · 10 332 pozycji");
  });

  // Prawdziwy plik jest tylko na komputerze (nie trafia do gita), więc w GitHub Actions test jest pomijany
  const PLIK = new URL("../../../shared/src/commonMain/composeResources/files/katalog.json", import.meta.url);
  test.skipIf(!existsSync(PLIK))("prawdziwy katalog: te same wyniki co w aplikacji (KatalogPlikTest)", () => {
    const katalog = odczytajKatalog(JSON.parse(readFileSync(PLIK, "utf8")));
    const w = new Wyszukiwarka(katalog.pozycje);
    const pierwszy = (fraza: string) => w.szukaj(fraza, 10).pozycje[0];
    expect(katalog.pozycje.length).toBeGreaterThan(10_000);
    expect(pierwszy("Matryca 1586947+TiN").sap).toBe("538000012128");
    expect(pierwszy("Matryca 1586947 + TiN").sap).toBe("538000012128");
    expect(pierwszy("EcoRail2").nazwa).toContain("EcoRail-2");
    expect(pierwszy("EcoRail 2").nazwa).toContain("EcoRail-2");
    expect(pierwszy("1000").sap).toBe("538000001000");
    expect(pierwszy("538000001000").miejsca).toEqual(["S10A F1"]);

    const frazy = ["lozysko", "czujnik indukcyjny bes", "1000", "zabezpieczene silnika", "s3c f1"];
    const start = performance.now();
    for (const f of frazy) w.szukaj(f, 100);
    const ms = (performance.now() - start) / frazy.length;
    console.log(`Średni czas szukania w ${katalog.pozycje.length} pozycjach: ${ms.toFixed(0)} ms`);
    expect(ms).toBeLessThan(500);
  });
});
