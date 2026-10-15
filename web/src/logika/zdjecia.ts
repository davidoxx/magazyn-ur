// Prywatny bucket Storage na zdjęcia materiałów
export const BUCKET_ZDJEC = "zdjecia";

// Darmowy plan ma 1 GB na pliki: dłuższy bok 1280 px i JPEG 80% to zwykle 150–250 kB
const MAKS_BOK = 1280;
const JAKOSC_JPEG = 0.8;

// Każde zdjęcie dostaje nową nazwę (jak w aplikacji), żeby pamięć podręczna nie pokazywała starego
export function nowaSciezkaZdjecia(materialId: number): string {
  return `materialy/${materialId}/${crypto.randomUUID()}.jpg`;
}

export function rozmiarDocelowy(szerokosc: number, wysokosc: number, maksBok = MAKS_BOK): [number, number] {
  const dluzszy = Math.max(szerokosc, wysokosc);
  if (dluzszy <= maksBok) return [szerokosc, wysokosc];
  const skala = maksBok / dluzszy;
  return [Math.max(1, Math.round(szerokosc * skala)), Math.max(1, Math.round(wysokosc * skala))];
}

// Zmniejsza i kompresuje zdjęcie z aparatu albo z pliku przed wysłaniem.
// Przeglądarka sama obraca zdjęcie zgodnie z EXIF, a iPhone zamienia HEIC na JPEG.
export async function przygotujZdjecie(plik: File): Promise<Blob> {
  let obraz: ImageBitmap;
  try {
    obraz = await createImageBitmap(plik, { imageOrientation: "from-image" });
  } catch {
    throw new Error("Nie udało się odczytać zdjęcia");
  }
  const [szerokosc, wysokosc] = rozmiarDocelowy(obraz.width, obraz.height);
  const plotno = document.createElement("canvas");
  plotno.width = szerokosc;
  plotno.height = wysokosc;
  const kontekst = plotno.getContext("2d");
  if (!kontekst) throw new Error("Nie udało się przygotować zdjęcia");
  // JPEG nie ma przezroczystości: przezroczyste tło z PNG robi się białe
  kontekst.fillStyle = "#fff";
  kontekst.fillRect(0, 0, szerokosc, wysokosc);
  kontekst.imageSmoothingQuality = "high";
  kontekst.drawImage(obraz, 0, 0, szerokosc, wysokosc);
  obraz.close();
  return new Promise((gotowe, porazka) =>
    plotno.toBlob(
      (blob) => (blob ? gotowe(blob) : porazka(new Error("Nie udało się przygotować zdjęcia"))),
      "image/jpeg",
      JAKOSC_JPEG,
    ),
  );
}
