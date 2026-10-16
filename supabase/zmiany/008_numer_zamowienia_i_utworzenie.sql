-- =====================================================================
--  008 – numer zamówienia i utworzenie materiału w historii
--
--  - ruchy.nr_zamowienia: opcjonalny numer zamówienia przy przyjęciu
--    i przy dodaniu materiału; widać go w historii na karcie.
--  - Nowy typ ruchu UTWORZENIE: dodanie materiału trafia do historii
--    (ilosc = stan początkowy, może być 0). Wpis robi trigger, więc działa
--    przy każdym sposobie dodania, także ze starszej wersji aplikacji.
--    Dodanie w panelu Supabase (bez zalogowanego użytkownika) nie ma wpisu.
--  - dodaj_material(): dodaje materiał i dopisuje numer zamówienia do wpisu.
--  - Materiał, którego jedyną historią jest utworzenie, nadal da się usunąć.
--
--  Starsza wersja aplikacji nie zna typu UTWORZENIE: na karcie nowych
--  materiałów pokaże błąd historii, dopóki nie zostanie zaktualizowana.
--
--  Można go uruchomić wielokrotnie.
-- =====================================================================

alter table public.ruchy
  add column if not exists nr_zamowienia text;

alter table public.ruchy drop constraint if exists ruchy_typ_check;
alter table public.ruchy
  add constraint ruchy_typ_check check (typ in ('WYDANIE', 'PRZYJECIE', 'UTWORZENIE'));

-- Wydanie i przyjęcie muszą mieć ilość, utworzenie może mieć stan początkowy 0
alter table public.ruchy drop constraint if exists ruchy_ilosc_check;
alter table public.ruchy
  add constraint ruchy_ilosc_check check (ilosc > 0 or (typ = 'UTWORZENIE' and ilosc >= 0));


-- Przyjęcie z numerem zamówienia. Stara wersja musi zniknąć, inaczej
-- Data API nie wiedziałoby, którą funkcję wywołać.
drop function if exists public.przyjmij(bigint, numeric, text);

create or replace function public.przyjmij(
  p_material_id   bigint,
  p_ilosc         numeric,
  p_uwagi         text default null,
  p_nr_zamowienia text default null
)
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

  insert into public.ruchy (material_id, typ, ilosc, uwagi, uzytkownik, nr_zamowienia)
  values (p_material_id, 'PRZYJECIE', p_ilosc, p_uwagi, auth.uid(),
          nullif(btrim(p_nr_zamowienia), ''))
  returning id into v_id;

  return v_id;
end;
$$;


-- Każde dodanie materiału przez aplikację zostawia wpis w historii
create or replace function public.po_dodaniu_materialu()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
  -- utworzyl = auth.uid(); w panelu Supabase nie ma zalogowanego użytkownika
  if new.utworzyl is not null then
    insert into public.ruchy (material_id, typ, ilosc, uzytkownik)
    values (new.id, 'UTWORZENIE', new.ilosc, new.utworzyl);
  end if;
  return new;
end;
$$;

drop trigger if exists po_dodaniu_materialu on public.materialy;
create trigger po_dodaniu_materialu
  after insert on public.materialy
  for each row execute function public.po_dodaniu_materialu();


-- Usuwanie materiału bez historii (poziom 3): sam wpis o utworzeniu nie blokuje.
-- Gdy są inne ruchy, klucz obcy i tak przerwie usuwanie (razem z tym delete).
create or replace function public.przed_usunieciem_materialu()
returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
  delete from public.ruchy
   where material_id = old.id
     and typ = 'UTWORZENIE';
  return old;
end;
$$;

drop trigger if exists przed_usunieciem_materialu on public.materialy;
create trigger przed_usunieciem_materialu
  before delete on public.materialy
  for each row execute function public.przed_usunieciem_materialu();


-- Poziom 1+: dodanie materiału z numerem zamówienia (wpis w historii robi trigger)
create or replace function public.dodaj_material(
  p_nazwa         text,
  p_typ           text,
  p_nr_sap        text    default null,
  p_miejsce       text    default null,
  p_jednostka     text    default 'szt',
  p_opis          text    default null,
  p_ilosc         numeric default 0,
  p_nr_zamowienia text    default null
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

  return v_material;
end;
$$;


revoke execute on function
  public.przyjmij(bigint, numeric, text, text),
  public.po_dodaniu_materialu(),
  public.przed_usunieciem_materialu(),
  public.dodaj_material(text, text, text, text, text, text, numeric, text)
from public, anon, authenticated;

grant execute on function public.przyjmij(bigint, numeric, text, text) to authenticated;
grant execute on function public.dodaj_material(text, text, text, text, text, text, numeric, text)
  to authenticated;

-- Data API od razu widzi nowe funkcje i kolumnę
notify pgrst, 'reload schema';
