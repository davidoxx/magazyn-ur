import { blad, ok, type Wynik } from "./wynik";

// Największa ilość, jaką zmieści numeric(12,3) w bazie
const MAKS_ILOSC = 999_999_999.999;

// Ilość wpisana przez pracownika: "2,5" albo "2.5". Baza trzyma 3 miejsca
// po przecinku i więcej by po cichu zaokrągliła, więc odrzucamy to tutaj.
// Zero ma sens tylko jako stan początkowy nowego materiału.
export function parsujIlosc(tekst: string, dopuscZero = false): Wynik<number> {
  const znormalizowany = tekst.trim().replaceAll(",", ".");
  if (!znormalizowany) return blad("Wpisz ilość");
  const m = /^([+-]?)(\d+)(?:\.(\d*))?$|^([+-]?)\.(\d+)$/.exec(znormalizowany);
  if (!m) return blad("Nieprawidłowa ilość");
  const ilosc = Number(znormalizowany);
  const ulamek = (m[3] ?? m[5] ?? "").replace(/0+$/, "");
  if (ilosc < 0) return blad("Ilość nie może być ujemna");
  if (ilosc === 0 && !dopuscZero) return blad("Ilość musi być większa od zera");
  if (ulamek.length > 3) return blad("Najwyżej 3 miejsca po przecinku");
  if (ilosc > MAKS_ILOSC) return blad("Za duża ilość");
  return ok(ilosc);
}

const FORMAT_ILOSCI = new Intl.NumberFormat("pl-PL", { maximumFractionDigits: 3, useGrouping: false });

// Ilość po polsku: "12", "12,5", "0,125"
export function formatujIlosc(ilosc: number): string {
  return FORMAT_ILOSCI.format(ilosc);
}
