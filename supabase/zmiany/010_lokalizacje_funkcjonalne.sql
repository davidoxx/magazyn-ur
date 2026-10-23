-- =====================================================================
--  010 – lokalizacje funkcjonalne (stacje) materiałów
--
--  Stacja, do której materiał jest przeznaczony. Stacje są podzielone na
--  obszary F1, F2 i F3; listę edytuje poziom 2+. Materiał może mieć kilka
--  stacji (tabela materialy_stacje), przypisuje je każdy z poziomem 1+
--  przy dodawaniu i edycji materiału.
--  To osobna lista od maszyn, na które idą wydania (te zostają bez zmian).
--
--  Stacji przypisanej do materiałów nie da się usunąć, tylko wycofać
--  (aktywna = false); wycofanej nie da się przypisać do kolejnych materiałów.
--
--  Można go uruchomić wielokrotnie.
-- =====================================================================

create table if not exists public.stacje (
  id         bigint generated always as identity primary key,
  obszar     text not null check (obszar in ('F1', 'F2', 'F3')),
  nazwa      text not null,
  kod        text unique,                     -- lokalizacja funkcjonalna w SAP (opcjonalna)
  aktywna    boolean not null default true,   -- wycofana znika z listy wyboru
  utworzono  timestamptz not null default now(),
  constraint stacje_nazwa_w_obszarze unique (obszar, nazwa)
);

create table if not exists public.materialy_stacje (
  material_id bigint not null references public.materialy(id) on delete cascade,
  stacja_id   bigint not null references public.stacje(id),
  primary key (material_id, stacja_id)
);

create index if not exists materialy_stacje_stacja_idx on public.materialy_stacje (stacja_id);


-- Poziom 1+: zmiana stacji materiału. Dostaje tylko zmiany (dodane i usunięte),
-- więc dwie osoby edytujące ten sam materiał nie kasują sobie nawzajem stacji.
-- Nowo dodana stacja musi być aktywna; już przypisana wycofana może zostać.
create or replace function public.zmien_stacje_materialu(
  p_material_id bigint,
  p_dodaj       bigint[] default null,
  p_usun        bigint[] default null
)
returns void
language plpgsql security definer set search_path = ''
as $$
declare
  v_dodaj bigint[] := coalesce(p_dodaj, '{}');
  v_usun  bigint[] := coalesce(p_usun, '{}');
begin
  if public.moj_poziom() < 1 then
    raise exception 'Brak uprawnień';
  end if;
  if not exists (select 1 from public.materialy where id = p_material_id) then
    raise exception 'Nie ma takiego materiału';
  end if;
  if exists (select 1
               from unnest(v_dodaj) as s(id)
              where not exists (select 1 from public.materialy_stacje ms
                                 where ms.material_id = p_material_id and ms.stacja_id = s.id)
                and not exists (select 1 from public.stacje st
                                 where st.id = s.id and st.aktywna)) then
    raise exception 'Nie ma takiej stacji albo jest wycofana';
  end if;

  delete from public.materialy_stacje
   where material_id = p_material_id
     and stacja_id = any(v_usun);

  insert into public.materialy_stacje (material_id, stacja_id)
  select distinct p_material_id, s.id
    from unnest(v_dodaj) as s(id)
  on conflict do nothing;
end;
$$;


-- dodaj_material dostaje stacje. Stara wersja musi zniknąć, inaczej
-- Data API nie wiedziałoby, którą funkcję wywołać.
drop function if exists public.dodaj_material(text, text, text, text, text, text, numeric, text);

create or replace function public.dodaj_material(
  p_nazwa         text,
  p_typ           text,
  p_nr_sap        text     default null,
  p_miejsce       text     default null,
  p_jednostka     text     default 'szt',
  p_opis          text     default null,
  p_ilosc         numeric  default 0,
  p_nr_zamowienia text     default null,
  p_stacje        bigint[] default null
)
returns public.materialy
language plpgsql security definer set search_path = ''
as $$
declare
  v_material public.materialy;
begin
  if public.moj_poziom() < 1 then
    raise exception 'Brak uprawnień';
  end if;
  if p_ilosc is not null and p_ilosc < 0 then
    raise exception 'Stan początkowy nie może być ujemny';
  end if;

  insert into public.materialy (nazwa, opis, typ, nr_sap, miejsce, ilosc, jednostka)
  values (p_nazwa, p_opis, p_typ, p_nr_sap, p_miejsce, coalesce(p_ilosc, 0),
          coalesce(p_jednostka, 'szt'))
  returning * into v_material;

  update public.ruchy
     set nr_zamowienia = nullif(btrim(p_nr_zamowienia), '')
   where material_id = v_material.id
     and typ = 'UTWORZENIE';

  if p_stacje is not null then
    perform public.zmien_stacje_materialu(v_material.id, p_dodaj => p_stacje);
  end if;

  return v_material;
end;
$$;


alter table public.stacje           enable row level security;
alter table public.materialy_stacje enable row level security;

revoke all on table public.stacje, public.materialy_stacje from anon, authenticated;

grant select, delete                       on public.stacje           to authenticated;
grant insert (obszar, nazwa, kod, aktywna) on public.stacje           to authenticated;
grant update (obszar, nazwa, kod, aktywna) on public.stacje           to authenticated;
-- Przypisania zmienia tylko zmien_stacje_materialu i dodaj_material
grant select                               on public.materialy_stacje to authenticated;

drop policy if exists "stacje: odczyt" on public.stacje;
create policy "stacje: odczyt" on public.stacje
  for select to authenticated
  using ((select public.moj_poziom()) >= 1);

drop policy if exists "stacje: dodawanie" on public.stacje;
create policy "stacje: dodawanie" on public.stacje
  for insert to authenticated
  with check ((select public.moj_poziom()) >= 2);

drop policy if exists "stacje: edycja" on public.stacje;
create policy "stacje: edycja" on public.stacje
  for update to authenticated
  using ((select public.moj_poziom()) >= 2)
  with check ((select public.moj_poziom()) >= 2);

drop policy if exists "stacje: usuwanie" on public.stacje;
create policy "stacje: usuwanie" on public.stacje
  for delete to authenticated
  using ((select public.moj_poziom()) >= 2);

drop policy if exists "materialy_stacje: odczyt" on public.materialy_stacje;
create policy "materialy_stacje: odczyt" on public.materialy_stacje
  for select to authenticated
  using ((select public.moj_poziom()) >= 1);

revoke execute on function
  public.zmien_stacje_materialu(bigint, bigint[], bigint[]),
  public.dodaj_material(text, text, text, text, text, text, numeric, text, bigint[])
from public, anon, authenticated;

grant execute on function public.zmien_stacje_materialu(bigint, bigint[], bigint[]) to authenticated;
grant execute on function public.dodaj_material(text, text, text, text, text, text, numeric, text, bigint[])
  to authenticated;

-- Data API od razu widzi nowe tabele i funkcje
notify pgrst, 'reload schema';
