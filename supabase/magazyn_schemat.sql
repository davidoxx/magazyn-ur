-- =====================================================================
--  MAGAZYN – schemat bazy danych dla Supabase
--  Uruchom całość jednorazowo w SQL Editor na pustym projekcie.
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. UŻYTKOWNICY
--    Login i hasła obsługuje Supabase Auth (tabela auth.users).
--    Tutaj trzymamy tylko nazwę i poziom uprawnień (0–3).
-- ---------------------------------------------------------------------
create table public.uzytkownicy (
  id         uuid primary key references auth.users(id) on delete cascade,
  nazwa      text not null unique,
  poziom     smallint not null default 0 check (poziom between 0 and 3),
  utworzono  timestamptz not null default now()
);

-- Poziom zalogowanego użytkownika (0, jeśli nie ma profilu)
create function public.moj_poziom()
returns smallint
language sql stable security definer set search_path = ''
as $$
  select coalesce(
    (select poziom from public.uzytkownicy where id = auth.uid()), 0
  )::smallint;
$$;

-- Każde nowe konto w Supabase Auth dostaje automatycznie profil z poziomem 0
create function public.nowy_uzytkownik()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
  insert into public.uzytkownicy (id, nazwa)
  values (new.id, coalesce(new.raw_user_meta_data->>'nazwa', new.email));
  return new;
end;
$$;

create trigger po_utworzeniu_konta
  after insert on auth.users
  for each row execute function public.nowy_uzytkownik();


-- ---------------------------------------------------------------------
-- 2. MATERIAŁY
-- ---------------------------------------------------------------------
create table public.materialy (
  id         bigint generated always as identity primary key,
  nazwa      text not null,
  opis       text,
  typ        text not null check (typ in ('SAP', 'KOSZTOWY')),
  nr_sap     text unique,
  miejsce    text,                                   -- miejsce magazynowe
  ilosc      numeric(12,3) not null default 0 check (ilosc >= 0),
  jednostka  text not null default 'szt',
  zdjecie    text,                                   -- ścieżka pliku w Storage (bucket "zdjecia")
  utworzyl   uuid default auth.uid() references public.uzytkownicy(id),
  utworzono  timestamptz not null default now(),
  -- materiał SAP musi mieć numer SAP, kosztowy nie może go mieć
  constraint nr_sap_zgodny_z_typem check ((typ = 'SAP') = (nr_sap is not null))
);

create index materialy_miejsce_idx on public.materialy (miejsce);

create extension if not exists unaccent with schema extensions;

-- Wyszukiwanie bez względu na wielkość liter i polskie znaki ("sruba m8"
-- znajdzie "Śruba imbusowa M8x20"). Każde słowo frazy musi wystąpić w nazwie,
-- numerze SAP, miejscu albo opisie. Działa z uprawnieniami wywołującego (RLS).
-- Słownik podany z nazwą schematu, bo przy pustym search_path
-- jednoargumentowe unaccent() go nie znajdzie.
create function public.szukaj_materialow(p_fraza text)
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


-- ---------------------------------------------------------------------
-- 2a. MASZYNY (cel wydania: maszyna / miejsce funkcjonalne)
--     Listę edytuje poziom 3. Wycofaną maszynę się dezaktywuje,
--     żeby historia wydań została.
-- ---------------------------------------------------------------------
create table public.maszyny (
  id         bigint generated always as identity primary key,
  nazwa      text not null,
  kod        text unique,                     -- miejsce funkcjonalne / numer urządzenia w SAP
  aktywna    boolean not null default true,   -- wycofana znika z listy wyboru
  utworzono  timestamptz not null default now()
);


-- ---------------------------------------------------------------------
-- 3. RUCHY MAGAZYNOWE (wydania i przyjęcia w jednej tabeli)
-- ---------------------------------------------------------------------
create table public.ruchy (
  id                bigint generated always as identity primary key,
  material_id       bigint not null references public.materialy(id),
  typ               text not null check (typ in ('WYDANIE', 'PRZYJECIE')),
  ilosc             numeric(12,3) not null check (ilosc > 0),
  uwagi             text,
  uzytkownik        uuid not null references public.uzytkownicy(id),
  utworzono         timestamptz not null default now(),
  zaksiegowano_sap  timestamptz,                     -- kiedy lider wydał w SAP
  zaksiegowal       uuid references public.uzytkownicy(id),
  anulowano         timestamptz,                     -- błędne wydanie, towar wrócił na stan
  anulowal          uuid references public.uzytkownicy(id),
  maszyna_id        bigint references public.maszyny(id)  -- na co poszło wydanie
);

create index ruchy_material_idx  on public.ruchy (material_id);
create index ruchy_utworzono_idx on public.ruchy (utworzono);
create index ruchy_maszyna_idx   on public.ruchy (maszyna_id);

-- Lista dla lidera zmiany: wydania materiałów SAP czekające na zaksięgowanie
create view public.wydania_do_sap
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

-- Ekran główny: wszystkie wydania (też zaksięgowane i anulowane) z materiałem,
-- osobą i maszyną. typ_materialu do filtrowania, material = cały wiersz materiału.
create view public.ostatnie_wydania
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

-- Typu materiału nie można zmienić, jeśli ma niezaksięgowane, nieanulowane
-- wydania: przy SAP -> KOSZTOWY zniknęłyby z listy lidera, a przy
-- KOSZTOWY -> SAP dawne wydania kosztowe trafiłyby na tę listę.
create function public.blokada_zmiany_typu()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
  if exists (select 1 from public.ruchy r
              where r.material_id = old.id
                and r.typ = 'WYDANIE'
                and r.zaksiegowano_sap is null
                and r.anulowano is null) then
    if old.typ = 'SAP' then
      raise exception 'Nie można zmienić typu: materiał ma wydania czekające na zaksięgowanie w SAP. Najpierw je zaksięguj albo anuluj.';
    else
      raise exception 'Nie można zmienić typu na SAP: materiał ma już wydania jako kosztowy i trafiłyby one na listę do zaksięgowania. Dodaj go jako nowy materiał SAP.';
    end if;
  end if;
  return new;
end;
$$;

create trigger przed_zmiana_typu
  before update of typ on public.materialy
  for each row
  when (old.typ <> new.typ)
  execute function public.blokada_zmiany_typu();


-- ---------------------------------------------------------------------
-- 4. OPERACJE (aplikacja wywołuje je przez supabase.Rpc)
--    Stan magazynu zmienia się WYŁĄCZNIE przez te funkcje.
-- ---------------------------------------------------------------------

-- Poziom 1+: wydanie materiału na maszynę.
-- Przy materiale SAP maszyna jest obowiązkowa, przy kosztowym opcjonalna.
create function public.wydaj(
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

-- Poziom 1+: uzupełnienie zapasu
create function public.przyjmij(p_material_id bigint, p_ilosc numeric, p_uwagi text default null)
returns bigint
language plpgsql security definer set search_path = ''
as $$
declare
  v_id bigint;
begin
  if public.moj_poziom() < 1 then
    raise exception 'Brak uprawnień';
  end if;
  if p_ilosc is null or p_ilosc <= 0 then
    raise exception 'Ilość musi być większa od zera';
  end if;

  update public.materialy
     set ilosc = ilosc + p_ilosc
   where id = p_material_id;
  if not found then
    raise exception 'Nie ma takiego materiału';
  end if;

  insert into public.ruchy (material_id, typ, ilosc, uwagi, uzytkownik)
  values (p_material_id, 'PRZYJECIE', p_ilosc, p_uwagi, auth.uid())
  returning id into v_id;

  return v_id;
end;
$$;

-- Poziom 2+: oznaczenie wydań jako zaksięgowanych w SAP (znikają z listy lidera).
-- Przyjmuje listę id, więc lider może zatwierdzić kilka wydań naraz.
create function public.zaksieguj_w_sap(p_ids bigint[])
returns integer
language plpgsql security definer set search_path = ''
as $$
declare
  v_liczba integer;
begin
  if public.moj_poziom() < 2 then
    raise exception 'Brak uprawnień';
  end if;

  update public.ruchy r
     set zaksiegowano_sap = now(),
         zaksiegowal      = auth.uid()
    from public.materialy m
   where r.id = any(p_ids)
     and m.id = r.material_id
     and m.typ = 'SAP'
     and r.typ = 'WYDANIE'
     and r.zaksiegowano_sap is null
     and r.anulowano is null;

  get diagnostics v_liczba = row_count;
  return v_liczba;
end;
$$;

-- Poziom 2+: anulowanie błędnego wydania (towar wraca na stan).
-- Nie da się anulować wydania już zaksięgowanego w SAP.
create function public.anuluj_wydanie(p_id bigint)
returns void
language plpgsql security definer set search_path = ''
as $$
declare
  v_material bigint;
  v_ilosc    numeric;
begin
  if public.moj_poziom() < 2 then
    raise exception 'Brak uprawnień';
  end if;

  update public.ruchy
     set anulowano = now(),
         anulowal  = auth.uid()
   where id = p_id
     and typ = 'WYDANIE'
     and anulowano is null
     and zaksiegowano_sap is null
  returning material_id, ilosc into v_material, v_ilosc;

  if not found then
    raise exception 'Tego wydania nie można anulować';
  end if;

  update public.materialy
     set ilosc = ilosc + v_ilosc
   where id = v_material;
end;
$$;

-- Poziom 3: zmiana poziomu uprawnień innego użytkownika
create function public.ustaw_poziom(p_uzytkownik uuid, p_poziom integer)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
  if public.moj_poziom() < 3 then
    raise exception 'Brak uprawnień';
  end if;
  if p_uzytkownik = auth.uid() then
    raise exception 'Nie możesz zmienić własnego poziomu';
  end if;

  update public.uzytkownicy
     set poziom = p_poziom
   where id = p_uzytkownik;

  if not found then
    raise exception 'Nie ma takiego użytkownika';
  end if;
end;
$$;

-- Poziom 3: czyszczenie historii starszej niż podana data (po zrobieniu kopii!).
-- Nigdy nie usuwa wydań SAP, które czekają jeszcze na zaksięgowanie.
create function public.wyczysc_historie(p_starsze_niz timestamptz)
returns integer
language plpgsql security definer set search_path = ''
as $$
declare
  v_liczba integer;
begin
  if public.moj_poziom() < 3 then
    raise exception 'Brak uprawnień';
  end if;

  delete from public.ruchy r
   where r.utworzono < p_starsze_niz
     and not (
           r.typ = 'WYDANIE'
       and r.zaksiegowano_sap is null
       and r.anulowano is null
       and exists (select 1 from public.materialy m
                    where m.id = r.material_id and m.typ = 'SAP')
     );

  get diagnostics v_liczba = row_count;
  return v_liczba;
end;
$$;


-- ---------------------------------------------------------------------
-- 5. UPRAWNIENIA I RLS
-- ---------------------------------------------------------------------
alter table public.uzytkownicy enable row level security;
alter table public.materialy   enable row level security;
alter table public.maszyny     enable row level security;
alter table public.ruchy       enable row level security;

-- Tabele: tylko zalogowani. Kolumny "ilosc" nie da się edytować bezpośrednio.
-- Supabase domyślnie daje anon i authenticated pełne uprawnienia do nowych
-- tabel, więc najpierw je odbieramy, inaczej granty na kolumny nic nie dają.
revoke all on table
  public.uzytkownicy,
  public.materialy,
  public.maszyny,
  public.ruchy,
  public.wydania_do_sap,
  public.ostatnie_wydania
from anon, authenticated;

grant select                       on public.uzytkownicy    to authenticated;
grant select, delete               on public.materialy      to authenticated;
grant insert (nazwa, opis, typ, nr_sap, miejsce, ilosc, jednostka, zdjecie)
                                   on public.materialy      to authenticated;
grant update (nazwa, opis, typ, nr_sap, miejsce, jednostka, zdjecie)
                                   on public.materialy      to authenticated;
grant select, delete               on public.maszyny        to authenticated;
grant insert (nazwa, kod, aktywna) on public.maszyny        to authenticated;
grant update (nazwa, kod, aktywna) on public.maszyny        to authenticated;
grant select                       on public.ruchy          to authenticated;
grant select                       on public.wydania_do_sap to authenticated;
grant select                       on public.ostatnie_wydania to authenticated;

-- Edge Function „uzytkownicy” działa jako service_role (klucz secret). Jawnie, bo
-- projekt może mieć wyłączone automatyczne udostępnianie nowych tabel w Data API.
grant usage  on schema public                   to service_role;
grant select on public.uzytkownicy              to service_role;
grant update (poziom) on public.uzytkownicy     to service_role;

-- Funkcje: najpierw odbieramy wszystkim, potem dajemy tylko zalogowanym
revoke execute on function
  public.moj_poziom(),
  public.nowy_uzytkownik(),
  public.blokada_zmiany_typu(),
  public.szukaj_materialow(text),
  public.wydaj(bigint, numeric, bigint, text),
  public.przyjmij(bigint, numeric, text),
  public.zaksieguj_w_sap(bigint[]),
  public.anuluj_wydanie(bigint),
  public.ustaw_poziom(uuid, integer),
  public.wyczysc_historie(timestamptz)
from public, anon, authenticated;

grant execute on function public.moj_poziom()                        to authenticated;
grant execute on function public.szukaj_materialow(text)             to authenticated;
grant execute on function public.wydaj(bigint, numeric, bigint, text) to authenticated;
grant execute on function public.przyjmij(bigint, numeric, text)     to authenticated;
grant execute on function public.zaksieguj_w_sap(bigint[])           to authenticated;
grant execute on function public.anuluj_wydanie(bigint)              to authenticated;
grant execute on function public.ustaw_poziom(uuid, integer)         to authenticated;
grant execute on function public.wyczysc_historie(timestamptz)       to authenticated;

-- Użytkownik zawsze widzi swój profil (aplikacja sprawdza poziom po zalogowaniu),
-- a od poziomu 1 widzi też pozostałych.
create policy "uzytkownicy: odczyt" on public.uzytkownicy
  for select to authenticated
  using (id = auth.uid() or (select public.moj_poziom()) >= 1);

create policy "materialy: odczyt" on public.materialy
  for select to authenticated
  using ((select public.moj_poziom()) >= 1);

create policy "materialy: dodawanie" on public.materialy
  for insert to authenticated
  with check ((select public.moj_poziom()) >= 1);

create policy "materialy: edycja" on public.materialy
  for update to authenticated
  using ((select public.moj_poziom()) >= 1)
  with check ((select public.moj_poziom()) >= 1);

create policy "materialy: usuwanie" on public.materialy
  for delete to authenticated
  using ((select public.moj_poziom()) >= 3);

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

create policy "ruchy: odczyt" on public.ruchy
  for select to authenticated
  using ((select public.moj_poziom()) >= 1);


-- ---------------------------------------------------------------------
-- 6. ZDJĘCIA (Supabase Storage, prywatny bucket "zdjecia")
-- ---------------------------------------------------------------------
insert into storage.buckets (id, name, public)
values ('zdjecia', 'zdjecia', false);

create policy "zdjecia: odczyt" on storage.objects
  for select to authenticated
  using (bucket_id = 'zdjecia' and (select public.moj_poziom()) >= 1);

create policy "zdjecia: dodawanie" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'zdjecia' and (select public.moj_poziom()) >= 1);

create policy "zdjecia: podmiana" on storage.objects
  for update to authenticated
  using (bucket_id = 'zdjecia' and (select public.moj_poziom()) >= 1);

create policy "zdjecia: usuwanie" on storage.objects
  for delete to authenticated
  using (bucket_id = 'zdjecia' and (select public.moj_poziom()) >= 1);


-- ---------------------------------------------------------------------
-- 7. PIERWSZY ADMINISTRATOR
--    Załóż swoje konto w Authentication -> Users, a potem uruchom
--    (nazwa na start = adres e-mail):
--
--    update public.uzytkownicy set poziom = 3 where nazwa = 'twoj@email.pl';
-- ---------------------------------------------------------------------
