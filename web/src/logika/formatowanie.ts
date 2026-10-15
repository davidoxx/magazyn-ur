// Polska odmiana po liczbie: 1 wydanie, 2 wydania, 5 wydań, 22 wydania
export function odmiana(liczba: number, jeden: string, kilka: string, wiele: string): string {
  if (liczba === 1) return jeden;
  const reszta10 = liczba % 10;
  const reszta100 = liczba % 100;
  return reszta10 >= 2 && reszta10 <= 4 && !(reszta100 >= 12 && reszta100 <= 14) ? kilka : wiele;
}

// Data z bazy (timestamptz, np. "2026-10-08T12:32:10.123456+00:00") w czasie urządzenia:
// "08.10.2026 14:32". Nieznany format pokazujemy tak, jak przyszedł.
export function formatujDate(zBazy: string, strefa?: string): string {
  // Przeglądarki nie muszą rozumieć mikrosekund, więc zostawiamy milisekundy
  const data = new Date(zBazy.replace(/(\.\d{3})\d+/, "$1"));
  if (Number.isNaN(data.getTime())) return zBazy;
  const czesci = Object.fromEntries(
    new Intl.DateTimeFormat("pl-PL", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
      timeZone: strefa,
    })
      .formatToParts(data)
      .map((c) => [c.type, c.value]),
  );
  return `${czesci.day}.${czesci.month}.${czesci.year} ${czesci.hour}:${czesci.minute}`;
}
