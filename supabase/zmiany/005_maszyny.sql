-- =====================================================================
--  005 – maszyny jako cel wydania
--
--  Wydanie idzie na maszynę / miejsce funkcjonalne wybierane z listy.
--  Przy materiale SAP maszyna jest obowiązkowa (lider potrzebuje jej do
--  zaksięgowania), przy kosztowym opcjonalna. Listę maszyn edytuje poziom 3.
--  Wycofaną maszynę się dezaktywuje (aktywna = false), żeby została historia.
--
--  Można go uruchomić wielokrotnie.
-- =====================================================================

create table if not exists public.maszyny (
  id         bigint generated always as identity primary key,
  nazwa      text not null,
  kod        text unique,                     -- miejsce funkcjonalne / numer urządzenia w SAP
  aktywna    boolean not null default true,   -- wycofana znika z listy wyboru
  utworzono  timestamptz not null default now()
);

alter table public.ruchy
  add column if not exists maszyna_id bigint references public.maszyny(id);

create index if not exists ruchy_maszyna_idx on public.ruchy (maszyna_id);

-- Stara wersja bez maszyny musi zniknąć, inaczej dałoby się nią
-- wydać materiał SAP bez maszyny
drop function if exists public.wydaj(bigint, numeric, text);

create or replace function public.wydaj(
  p_material_id bigint,
  p_ilosc       numeric,
  p_maszyna_id  bigint default null,
  p_uwagi       text   default null
)
returns bigint
language plpgsql security definer set search_path = ''
as $$
declare
  v_typ text;
  v_id  bigint;
begin
  if public.moj_poziom() < 1 then
    raise exception 'Brak uprawnień';
  end if;
  if p_ilosc is null or p_ilosc <= 0 then
    raise exception 'Ilość musi być większa od zera';
  end if;

  select typ into v_typ
    from public.materialy
   where id = p_material_id
     for update;
  if not found then
    raise exception 'Nie ma takiego materiału';
  end if;

  if p_maszyna_id is null then
    if v_typ = 'SAP' then
      raise exception 'Przy wydaniu materiału SAP wybierz maszynę';
    end if;
  elsif not exists (select 1 from public.maszyny
                     where id = p_maszyna_id and aktywna) then
    raise exception 'Nie ma takiej maszyny albo jest wycofana';
  end if;

  update public.materialy
     set ilosc = ilosc - p_ilosc
   where id = p_material_id and ilosc >= p_ilosc;
  if not found then
    raise exception 'Za mało materiału na stanie';
  end if;

  insert into public.ruchy (material_id, typ, ilosc, uwagi, uzytkownik, maszyna_id)
  values (p_material_id, 'WYDANIE', p_ilosc, p_uwagi, auth.uid(), p_maszyna_id)
  returning id into v_id;

  return v_id;
end;
$$;

revoke execute on function public.wydaj(bigint, numeric, bigint, text) from public, anon, authenticated;
grant execute on function public.wydaj(bigint, numeric, bigint, text) to authenticated;

-- Lista dla lidera dostaje maszynę (nowe kolumny tylko na końcu widoku)
create or replace view public.wydania_do_sap
with (security_invoker = true) as
select r.id,
       r.utworzono,
       m.nr_sap,
       m.nazwa   as material,
       r.ilosc,
       m.jednostka,
       u.nazwa   as wydal,
       r.uwagi,
       ma.kod    as maszyna_kod,
       ma.nazwa  as maszyna
from public.ruchy r
join public.materialy   m  on m.id = r.material_id
join public.uzytkownicy u  on u.id = r.uzytkownik
left join public.maszyny ma on ma.id = r.maszyna_id
where r.typ = 'WYDANIE'
  and m.typ = 'SAP'
  and r.zaksiegowano_sap is null
  and r.anulowano is null;

-- Uprawnienia: czytają wszyscy aktywni, zmienia poziom 3
alter table public.maszyny enable row level security;

revoke all on table public.maszyny from anon, authenticated;
grant select, delete                    on public.maszyny to authenticated;
grant insert (nazwa, kod, aktywna)      on public.maszyny to authenticated;
grant update (nazwa, kod, aktywna)      on public.maszyny to authenticated;

drop policy if exists "maszyny: odczyt"    on public.maszyny;
drop policy if exists "maszyny: dodawanie" on public.maszyny;
drop policy if exists "maszyny: edycja"    on public.maszyny;
drop policy if exists "maszyny: usuwanie"  on public.maszyny;

create policy "maszyny: odczyt" on public.maszyny
  for select to authenticated
  using ((select public.moj_poziom()) >= 1);

create policy "maszyny: dodawanie" on public.maszyny
  for insert to authenticated
  with check ((select public.moj_poziom()) >= 3);

create policy "maszyny: edycja" on public.maszyny
  for update to authenticated
  using ((select public.moj_poziom()) >= 3)
  with check ((select public.moj_poziom()) >= 3);

create policy "maszyny: usuwanie" on public.maszyny
  for delete to authenticated
  using ((select public.moj_poziom()) >= 3);

-- Data API od razu widzi zmiany
notify pgrst, 'reload schema';
