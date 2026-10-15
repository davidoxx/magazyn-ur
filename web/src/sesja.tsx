import type { Session } from "@supabase/supabase-js";
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { komunikat } from "./logika/bledy";
import { emailNaLogin, loginNaEmail } from "./logika/login";
import { supabase } from "./supabase";

export type Sesja =
  | { stan: "ladowanie" }
  | { stan: "wylogowany" }
  | { stan: "zalogowany"; userId: string; login: string; poziom: number }
  | { stan: "blad"; komunikat: string };

interface KontekstSesji {
  sesja: Sesja;
  bladLogowania: string | null;
  zaloguj: (login: string, haslo: string) => Promise<void>;
  wyloguj: () => Promise<void>;
  ustawNoweHaslo: (haslo: string) => Promise<void>;
  ponow: () => void;
}

const Kontekst = createContext<KontekstSesji | null>(null);

export function useSesja(): KontekstSesji {
  const k = useContext(Kontekst);
  if (!k) throw new Error("useSesja poza SesjaProvider");
  return k;
}

// Supabase JS trzyma sesję w localStorage pod tym kluczem (domyślna nazwa)
const kluczSesji = () => `sb-${new URL(import.meta.env.VITE_SUPABASE_URL).hostname.split(".")[0]}-auth-token`;

// Wylogowanie tylko tego urządzenia. Bez sieci Supabase nie usuwa sesji, więc robimy to sami.
async function wylogujUrzadzenie() {
  const { error } = await supabase.auth.signOut({ scope: "local" });
  if (error) {
    localStorage.removeItem(kluczSesji());
    window.location.reload();
  }
}

export function SesjaProvider({ children }: { children: ReactNode }) {
  const [sesja, ustawSesje] = useState<Sesja>({ stan: "ladowanie" });
  const [bladLogowania, ustawBladLogowania] = useState<string | null>(null);
  // Użytkownik, którego poziom już sprawdziliśmy (odświeżenie tokenu nie wymaga ponownego sprawdzenia)
  const sprawdzony = useRef<string | null>(null);

  // Po zalogowaniu i przy starcie z zapisaną sesją, żeby konto dezaktywowane
  // w międzyczasie (poziom 0) nie weszło dalej
  const sprawdzPoziom = useCallback(async (s: Session) => {
    if (sprawdzony.current === s.user.id) return;
    ustawSesje({ stan: "ladowanie" });
    const { data, error } = await supabase.rpc("moj_poziom");
    if (error) {
      ustawSesje({ stan: "blad", komunikat: komunikat(error) });
      return;
    }
    const poziom = Number(data ?? 0);
    if (poziom < 1) {
      ustawBladLogowania("Konto nieaktywne");
      await wylogujUrzadzenie();
      return;
    }
    sprawdzony.current = s.user.id;
    ustawSesje({ stan: "zalogowany", userId: s.user.id, login: emailNaLogin(s.user.email ?? ""), poziom });
  }, []);

  useEffect(() => {
    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_zdarzenie, s) => {
      // Kolejne wywołania Supabase nie mogą iść wprost z tej funkcji (blokada w supabase-js)
      setTimeout(() => {
        if (!s) {
          sprawdzony.current = null;
          ustawSesje({ stan: "wylogowany" });
        } else {
          void sprawdzPoziom(s);
        }
      }, 0);
    });
    return () => subscription.unsubscribe();
  }, [sprawdzPoziom]);

  const zaloguj = useCallback(async (login: string, haslo: string) => {
    if (!login.trim() || !haslo) {
      ustawBladLogowania("Wpisz login i hasło");
      return;
    }
    ustawBladLogowania(null);
    const { error } = await supabase.auth.signInWithPassword({ email: loginNaEmail(login), password: haslo });
    // Dalej prowadzi onAuthStateChange: zalogowanie -> sprawdzenie poziomu
    if (error) ustawBladLogowania(komunikat(error));
  }, []);

  const ponow = useCallback(() => {
    sprawdzony.current = null;
    ustawSesje({ stan: "ladowanie" });
    void supabase.auth.getSession().then(({ data }) => {
      if (data.session) void sprawdzPoziom(data.session);
      else ustawSesje({ stan: "wylogowany" });
    });
  }, [sprawdzPoziom]);

  const ustawNoweHaslo = useCallback(async (haslo: string) => {
    const { error } = await supabase.auth.updateUser({ password: haslo });
    if (error) throw error;
  }, []);

  const wartosc = useMemo<KontekstSesji>(
    () => ({ sesja, bladLogowania, zaloguj, wyloguj: wylogujUrzadzenie, ustawNoweHaslo, ponow }),
    [sesja, bladLogowania, zaloguj, ustawNoweHaslo, ponow],
  );
  return <Kontekst.Provider value={wartosc}>{children}</Kontekst.Provider>;
}
