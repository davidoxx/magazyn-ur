-- =====================================================================
--  006 – uprawnienia dla Edge Function „uzytkownicy”
--
--  Funkcja działa z kluczem secret, czyli jako rola service_role. Gdy w projekcie
--  jest wyłączone automatyczne udostępnianie nowych tabel w Data API,
--  service_role nie dostaje praw do tabel i funkcja nie odczyta poziomu
--  wywołującego. Nadajemy jawnie tylko to, czego funkcja potrzebuje:
--  odczyt profilu i ustawienie poziomu nowemu kontu.
--
--  Można go uruchomić wielokrotnie.
-- =====================================================================

grant usage on schema public to service_role;
grant select on public.uzytkownicy to service_role;
grant update (poziom) on public.uzytkownicy to service_role;

notify pgrst, 'reload schema';


-- ---------------------------------------------------------------------
-- Sprawdzenie (obie kolumny mają mieć true):
--
-- select has_table_privilege('service_role', 'public.uzytkownicy', 'SELECT')            as odczyt,
--        has_column_privilege('service_role', 'public.uzytkownicy', 'poziom', 'UPDATE') as zmiana_poziomu;
-- ---------------------------------------------------------------------
