// Calendar arithmetic on local ISO strings: dates "YYYY-MM-DD" and minute date-times "YYYY-MM-DDTHH:MM".

const pad = (n: number) => String(n).padStart(2, '0')

export function toIsoDate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** Parses "YYYY-MM-DD" as local midnight; `new Date(iso)` would read it as UTC. */
export function parseIsoDate(iso: string): Date {
  const [y = 1970, m = 1, d = 1] = iso.slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function today(): string {
  return toIsoDate(new Date())
}

export function nowLocal(): string {
  const d = new Date()
  return `${toIsoDate(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function addDays(iso: string, days: number): string {
  const date = parseIsoDate(iso)
  date.setDate(date.getDate() + days)
  return toIsoDate(date)
}

/** First day of the month `months` away from the month of the date. */
export function addMonths(iso: string, months: number): string {
  const date = parseIsoDate(iso)
  return toIsoDate(new Date(date.getFullYear(), date.getMonth() + months, 1))
}

/** Monday of the week containing the date. */
export function startOfWeek(iso: string): string {
  return addDays(iso, -((parseIsoDate(iso).getDay() + 6) % 7))
}

export function weekDays(iso: string): string[] {
  const monday = startOfWeek(iso)
  return Array.from({ length: 7 }, (_, i) => addDays(monday, i))
}

/** Weeks (arrays of 7 dates, Monday first) covering the month of the date. */
export function monthGrid(iso: string): string[][] {
  const month = iso.slice(0, 7)
  const weeks: string[][] = []
  let day = startOfWeek(`${month}-01`)
  do {
    const monday = day
    weeks.push(Array.from({ length: 7 }, (_, i) => addDays(monday, i)))
    day = addDays(day, 7)
  } while (day.slice(0, 7) === month)
  return weeks
}

export const datePart = (dateTime: string) => dateTime.slice(0, 10)
export const timePart = (dateTime: string) => dateTime.slice(11, 16)
export const monthOf = (iso: string) => iso.slice(0, 7)

/** Minutes since midnight of a date-time. */
export function minutesOfDay(dateTime: string): number {
  const [h = 0, m = 0] = timePart(dateTime).split(':').map(Number)
  return h * 60 + m
}

export function combine(date: string, time: string): string {
  return `${date}T${time || '00:00'}`
}

/** Adds minutes to a minute date-time, across days. */
export function addMinutes(dateTime: string, minutes: number): string {
  const date = parseIsoDate(datePart(dateTime))
  date.setMinutes(minutesOfDay(dateTime) + minutes)
  return `${toIsoDate(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** Day of week code of a date, as the API names it. */
export function dayCode(iso: string) {
  const codes = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'] as const
  return codes[parseIsoDate(iso).getDay()] ?? 'MONDAY'
}
