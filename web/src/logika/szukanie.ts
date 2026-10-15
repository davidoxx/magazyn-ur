// Tekst bez wielkości liter i polskich znaków, tak jak szukaj_materialow w bazie.
// „ł” nie rozkłada się na literę i znak, więc zamieniamy je osobno.
export function doPorownania(tekst: string): string {
  return tekst.toLowerCase().normalize("NFD").replace(/\p{Mn}+/gu, "").replaceAll("ł", "l");
}

// Każde słowo frazy musi wystąpić w tekście (szukanie po stronie urządzenia)
export function pasujeDoFrazy(tekst: string, fraza: string): boolean {
  const gdzie = doPorownania(tekst);
  return doPorownania(fraza)
    .split(/\s+/)
    .filter(Boolean)
    .every((slowo) => gdzie.includes(slowo));
}

const POLSKI = new Intl.Collator("pl-PL");

// Alfabetycznie po polsku („Ś” po „S”, nie po „Z”)
export function porownajPoPolsku(a: string, b: string): number {
  return POLSKI.compare(a, b);
}
