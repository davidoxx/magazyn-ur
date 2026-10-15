-- =====================================================================
--  007 – widok ostatnich wydań (ekran główny aplikacji)
--
--  Wszystkie wydania, również zaksięgowane i anulowane, z materiałem,
--  osobą i maszyną. Kolumna typ_materialu służy do filtrowania
--  (Wszystkie / Kosztowe / SAP), a material to cały wiersz materiału,
--  żeby aplikacja mogła od razu otworzyć jego kartę.
--  Działa z uprawnieniami wywołującego, więc obowiązuje RLS (poziom 1+).
--
--  Można go uruchomić wielokrotnie.
-- =====================================================================

create or replace view public.ostatnie_wydania
with (security_invoker = true) as
select r.id,
       r.utworzono,
       r.ilosc,
       r.uwagi,
       r.zaksiegowano_sap,
       r.anulowano,
       m.typ       as typ_materialu,
       to_jsonb(m) as material,
       u.nazwa     as wydal,
       ma.kod      as maszyna_kod,
       ma.nazwa    as maszyna
from public.ruchy r
join public.materialy   m  on m.id = r.material_id
join public.uzytkownicy u  on u.id = r.uzytkownik
left join public.maszyny ma on ma.id = r.maszyna_id
where r.typ = 'WYDANIE';

revoke all on table public.ostatnie_wydania from anon, authenticated;
grant select on public.ostatnie_wydania to authenticated;

-- Data API od razu widzi nowy widok
notify pgrst, 'reload schema';
