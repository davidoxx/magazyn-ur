/// <reference types="vite/client" />
/// <reference types="vite-plugin-pwa/client" />

// Wstawiane przez vite.config.ts z plików projektu
declare const __WERSJA__: string;
declare const __KOD_WERSJI__: string;
declare const __O_APLIKACJI__: Record<string, string>;

interface ImportMetaEnv {
  readonly VITE_SUPABASE_URL: string;
  readonly VITE_SUPABASE_KEY: string;
}
