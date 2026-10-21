-- =====================================================================
--  009 – katalog części w Supabase (prywatny bucket „katalog”)
--
--  Plik katalog.json (tworzy go narzedzia/katalog_z_excela.py) wgrywa
--  administrator w panelu: Storage -> katalog -> Upload. Zalogowani
--  (poziom 1+) mogą go tylko pobrać: strona pokazuje z niego katalog,
--  a aplikacja i program na Windows pobierają nowszy niż wbudowany.
--  Nie ma polityk dodawania ani usuwania, więc plik zmienia tylko panel.
--
--  Można go uruchomić wielokrotnie.
-- =====================================================================

insert into storage.buckets (id, name, public)
values ('katalog', 'katalog', false)
on conflict (id) do nothing;

drop policy if exists "katalog: odczyt" on storage.objects;
create policy "katalog: odczyt" on storage.objects
  for select to authenticated
  using (bucket_id = 'katalog' and (select public.moj_poziom()) >= 1);
