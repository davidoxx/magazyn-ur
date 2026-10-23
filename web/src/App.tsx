import { Box, Button, Stack, Typography } from "@mui/material";
import type { ReactNode } from "react";
import { Navigate, Route, Routes } from "react-router";
import { FormularzMaterialuEdycja, FormularzMaterialuNowy, Glowny, Karta, KatalogCzesci, Logowanie, Maszyny, Operacja, Stacje, Uzytkownicy, WydaniaSap } from "./ekrany";
import { useSesja } from "./sesja";
import { Ladowanie } from "./ui/wspolne";

// Strona: te same ekrany co aplikacja, bez katalogu części z arkusza (dane firmy nie trafiają na publiczną stronę).
// Adresy w stylu #/material/12, więc „Wstecz” przeglądarki i telefonu działa jak w aplikacji.
export function App() {
  const { sesja, ponow, wyloguj } = useSesja();

  if (sesja.stan === "ladowanie") return <Ladowanie />;
  if (sesja.stan === "wylogowany") return <Logowanie />;
  if (sesja.stan === "blad") {
    return (
      <Stack spacing={2} sx={{ p: 3, alignItems: "center", textAlign: "center", pt: 10 }}>
        <Typography>{sesja.komunikat}</Typography>
        <Button variant="contained" onClick={ponow}>
          Spróbuj ponownie
        </Button>
        <Button variant="outlined" onClick={() => void wyloguj()}>
          Wyloguj
        </Button>
      </Stack>
    );
  }

  // Ukrywanie ekranów to tylko wygoda, uprawnienia sprawdza baza
  const tylko = (minimalny: number, ekran: ReactNode) =>
    sesja.poziom >= minimalny ? (
      ekran
    ) : (
      <Box sx={{ p: 3 }}>
        <Typography>Brak uprawnień</Typography>
      </Box>
    );

  return (
    <Routes>
      <Route path="/" element={<Glowny />} />
      <Route path="/material/:id" element={<Karta />} />
      <Route path="/material/:id/wydaj" element={<Operacja rodzaj="wydaj" />} />
      <Route path="/material/:id/przyjmij" element={<Operacja rodzaj="przyjmij" />} />
      <Route path="/material/:id/edycja" element={<FormularzMaterialuEdycja />} />
      <Route path="/nowy" element={<FormularzMaterialuNowy />} />
      <Route path="/katalog" element={<KatalogCzesci />} />
      <Route path="/sap" element={tylko(2, <WydaniaSap />)} />
      <Route path="/stacje" element={tylko(2, <Stacje />)} />
      <Route path="/maszyny" element={tylko(3, <Maszyny />)} />
      <Route path="/uzytkownicy" element={tylko(3, <Uzytkownicy />)} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
