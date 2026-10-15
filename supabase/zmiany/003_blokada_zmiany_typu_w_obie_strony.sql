-- =====================================================================
--  003 – blokada zmiany typu materiału w obie strony
--
--  Widok wydania_do_sap pokazuje niezaksięgowane, nieanulowane wydania
--  materiałów typu SAP. Zmiana typu przy takich wydaniach psuje listę lidera:
--   - SAP -> KOSZTOWY: czekające wydania znikają bez zaksięgowania,
--   - KOSZTOWY -> SAP: dawne wydania kosztowe nagle trafiają na listę.
--  Wydania anulowane nie blokują zmiany, bo i tak nie ma ich na liście.
--
--  Zastępuje funkcję i trigger z 002. Działa też, jeśli 002 nie było
--  uruchamiane. Można go uruchomić wielokrotnie.
-- =====================================================================

create or replace function public.blokada_zmiany_typu()
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

drop trigger if exists przed_zmiana_typu on public.materialy;

create trigger przed_zmiana_typu
  before update of typ on public.materialy
  for each row
  when (old.typ <> new.typ)
  execute function public.blokada_zmiany_typu();

-- Funkcję wywołuje tylko trigger
revoke execute on function public.blokada_zmiany_typu() from public, anon, authenticated;
