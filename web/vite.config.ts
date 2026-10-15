/// <reference types="vitest/config" />
import { readFileSync, existsSync } from "node:fs";
import { resolve } from "node:path";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { VitePWA } from "vite-plugin-pwa";

// Strona korzysta z tych samych plików co aplikacja na Androida i program na Windows:
// klucze z local.properties (lokalnie) albo ze zmiennych GitHub (publikacja),
// wersja z gradle.properties, dane „O aplikacji” z zasobów Compose.
const projekt = resolve(import.meta.dirname, "..");

function wlasciwosci(plik: string): Record<string, string> {
  const sciezka = resolve(projekt, plik);
  if (!existsSync(sciezka)) return {};
  const wynik: Record<string, string> = {};
  for (const linia of readFileSync(sciezka, "utf8").split(/\r?\n/)) {
    const m = /^\s*([\w.]+)\s*=\s*(.*)$/.exec(linia);
    // Plik properties Javy: "\:" to dwukropek, "\\" to ukośnik
    if (m) wynik[m[1]] = m[2].trim().replace(/\\(.)/g, "$1");
  }
  return wynik;
}

function oAplikacji(): Record<string, string> {
  const xml = readFileSync(resolve(projekt, "shared/src/commonMain/composeResources/values/o_aplikacji.xml"), "utf8");
  const encje: Record<string, string> = { "&amp;": "&", "&lt;": "<", "&gt;": ">", "&quot;": '"', "&apos;": "'" };
  return Object.fromEntries(
    [...xml.matchAll(/<string name="o_aplikacji_(\w+)">([^<]*)<\/string>/g)].map(([, klucz, wartosc]) => [
      klucz,
      wartosc.replace(/&\w+;/g, (e) => encje[e] ?? e),
    ]),
  );
}

const lokalne = wlasciwosci("local.properties");
const gradle = wlasciwosci("gradle.properties");
const supabaseUrl = process.env.VITE_SUPABASE_URL || lokalne.SUPABASE_URL;
const supabaseKey = process.env.VITE_SUPABASE_KEY || lokalne.SUPABASE_KEY;

export default defineConfig(({ command }) => {
  if (command === "build" && (!supabaseUrl || !supabaseKey)) {
    throw new Error(
      "Brak adresu i klucza Supabase: ustaw VITE_SUPABASE_URL i VITE_SUPABASE_KEY " +
        "albo SUPABASE_URL i SUPABASE_KEY w local.properties.",
    );
  }
  return {
    // Na GitHub Pages strona jest w podfolderze (https://login.github.io/repozytorium/)
    base: process.env.BASE_PATH || "/",
    define: {
      __WERSJA__: JSON.stringify(gradle["magazyn.wersja"] ?? "?"),
      __KOD_WERSJI__: JSON.stringify(gradle["magazyn.kodWersji"] ?? "?"),
      __O_APLIKACJI__: JSON.stringify(oAplikacji()),
      "import.meta.env.VITE_SUPABASE_URL": JSON.stringify(supabaseUrl ?? ""),
      "import.meta.env.VITE_SUPABASE_KEY": JSON.stringify(supabaseKey ?? ""),
    },
    plugins: [
      react(),
      VitePWA({
        registerType: "autoUpdate",
        includeAssets: ["ikona-180.png"],
        manifest: {
          name: "Magazyn UR",
          short_name: "Magazyn UR",
          description: "Obsługa magazynu materiałów: wyszukiwanie, wydania, przyjęcia i księgowanie w SAP",
          lang: "pl",
          display: "standalone",
          start_url: ".",
          scope: ".",
          background_color: "#fffbfe",
          theme_color: "#6650a4",
          icons: [
            { src: "ikona-192.png", sizes: "192x192", type: "image/png" },
            { src: "ikona-512.png", sizes: "512x512", type: "image/png" },
            { src: "ikona-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
          ],
        },
        // W pamięci podręcznej tylko sama strona; dane zawsze z Supabase
        workbox: { globPatterns: ["**/*.{js,css,html,png,svg}"] },
      }),
    ],
    // MUI i Supabase razem to ok. 240 kB po kompresji; dzielenie na części nic tu nie da
    build: { chunkSizeWarningLimit: 1000 },
    test: { environment: "node" },
  };
});
