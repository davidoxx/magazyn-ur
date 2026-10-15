-- =====================================================================
--  002 – blokada zmiany typu materiału SAP z niezaksięgowanymi wydaniami
--
--  Widok wydania_do_sap pokazuje tylko wydania materiałów typu SAP.
--  Gdyby materiał z niezaksięgowanymi wydaniami zmienił typ na KOSZTOWY,
--  te wydania zniknęłyby z listy lidera bez zaksięgowania w SAP,
--  a wyczysc_historie mogłaby je potem usunąć.
-- =====================================================================

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
    raise exception 'Nie można zmienić typu: materiał ma wydania czekające na zaksięgowanie w SAP. Najpierw je zaksięguj albo anuluj.';
  end if;
  return new;
end;
$$;

create trigger przed_zmiana_typu
  before update of typ on public.materialy
  for each row
  when (old.typ = 'SAP' and new.typ <> 'SAP')
  execute function public.blokada_zmiany_typu();

-- Funkcję wywołuje tylko trigger
revoke execute on function public.blokada_zmiany_typu() from public, anon, authenticated;
