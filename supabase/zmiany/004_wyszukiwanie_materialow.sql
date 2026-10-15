-- =====================================================================
--  004 – wyszukiwanie materiałów
--
--  Szukanie bez względu na wielkość liter i polskie znaki ("sruba m8"
--  znajdzie "Śruba imbusowa M8x20"). Każde słowo frazy musi wystąpić
--  w nazwie, numerze SAP, miejscu albo opisie.
--  Funkcja działa z uprawnieniami wywołującego, więc obowiązuje RLS (poziom 1+).
--  Można go uruchomić wielokrotnie.
-- =====================================================================

create extension if not exists unaccent with schema extensions;

-- Słownik podany z nazwą schematu, bo przy pustym search_path
-- jednoargumentowe unaccent() go nie znajdzie
create or replace function public.szukaj_materialow(p_fraza text)
returns setof public.materialy
language sql stable security invoker set search_path = ''
as $$
  with slowa as (
    select slowo
      from regexp_split_to_table(
             lower(extensions.unaccent('extensions.unaccent', coalesce(p_fraza, ''))),
             '\s+') as slowo
     where slowo <> ''
  )
  select m.*
    from public.materialy m
   where not exists (
           select 1
             from slowa s
            where strpos(lower(extensions.unaccent('extensions.unaccent',
                    concat_ws(' ', m.nazwa, m.nr_sap, m.miejsce, m.opis))), s.slowo) = 0)
   order by m.nazwa, m.id;
$$;

revoke execute on function public.szukaj_materialow(text) from public, anon, authenticated;
grant execute on function public.szukaj_materialow(text) to authenticated;

-- Data API od razu widzi nową funkcję
notify pgrst, 'reload schema';


-- ---------------------------------------------------------------------
-- Sprawdzenie (uruchom osobno):
--
-- select extensions.unaccent('extensions.unaccent', 'Śruba łożysko ŻÓŁĆ');
-- -- oczekiwane: Sruba lozysko ZOLC
--
-- select nazwa, nr_sap, miejsce from public.szukaj_materialow('sruba');
-- -- SQL Editor działa jako właściciel tabel, więc widzi wszystko mimo RLS
-- ---------------------------------------------------------------------
