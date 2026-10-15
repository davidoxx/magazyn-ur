-- =====================================================================
--  001 – uprawnienia do tabel i kolumn
--
--  Supabase domyślnie nadaje rolom anon i authenticated WSZYSTKIE
--  uprawnienia do nowych tabel w schemacie public. Granty na kolumny ze
--  schematu tylko dokładały się do pełnego UPDATE na tabeli, więc
--  użytkownik z poziomem 1 mógł zmienić materialy.ilosc bezpośrednio
--  przez Data API (z pominięciem funkcji wydaj/przyjmij i bez wpisu
--  w ruchach). Mógł też podać własne utworzyl/utworzono przy dodawaniu.
--
--  Skrypt odbiera wszystko i nadaje jeszcze raz tylko to, co potrzebne.
--  Można go uruchomić wielokrotnie.
-- =====================================================================

-- REVOKE ALL na tabeli odbiera też uprawnienia do kolumn,
-- dlatego wszystkie granty są poniżej nadawane od nowa.
revoke all on table
  public.uzytkownicy,
  public.materialy,
  public.ruchy,
  public.wydania_do_sap
from anon, authenticated;

grant select                       on public.uzytkownicy    to authenticated;
grant select, delete               on public.materialy      to authenticated;
grant insert (nazwa, opis, typ, nr_sap, miejsce, ilosc, jednostka, zdjecie)
                                   on public.materialy      to authenticated;
grant update (nazwa, opis, typ, nr_sap, miejsce, jednostka, zdjecie)
                                   on public.materialy      to authenticated;
grant select                       on public.ruchy          to authenticated;
grant select                       on public.wydania_do_sap to authenticated;


-- ---------------------------------------------------------------------
-- Sprawdzenie (uruchom osobno po skrypcie).
-- Dla authenticated mają zostać tylko: materialy DELETE i SELECT,
-- ruchy SELECT, uzytkownicy SELECT, wydania_do_sap SELECT.
-- Dla anon nie ma być nic.
--
-- select table_name, grantee, privilege_type
--   from information_schema.role_table_grants
--  where table_schema = 'public'
--    and grantee in ('anon', 'authenticated')
--  order by table_name, grantee, privilege_type;
-- ---------------------------------------------------------------------
