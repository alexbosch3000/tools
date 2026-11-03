// Calendar arithmetic on local ISO strings: dates "YYYY-MM-DD" and minute date-times "YYYY-MM-DDTHH:MM".

const pad = (n) => String(n).padStart(2, '0')

export function toIsoDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** Parses "YYYY-MM-DD" as local midnight; `new Date(iso)` would read it as UTC. */
export function parseIsoDate(iso) {
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function today() {
  return toIsoDate(new Date())
}

export function nowLocal() {
  const d = new Date()
  return `${toIsoDate(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function addDays(iso, days) {
  const date = parseIsoDate(iso)
  date.setDate(date.getDate() + days)
  return toIsoDate(date)
}

export function addMonths(iso, months) {
  const date = parseIsoDate(iso)
  return toIsoDate(new Date(date.getFullYear(), date.getMonth() + months, 1))
}

/** Monday of the week containing the date. */
export function startOfWeek(iso) {
  const date = parseIsoDate(iso)
  const offset = (date.getDay() + 6) % 7
  return addDays(iso, -offset)
}

export function weekDays(iso) {
  const monday = startOfWeek(iso)
  return Array.from({ length: 7 }, (_, i) => addDays(monday, i))
}

/** Weeks (arrays of 7 dates, Monday first) covering the month of the date. */
export function monthGrid(iso) {
  const first = `${iso.slice(0, 7)}-01`
  const lastMonth = first.slice(0, 7)
  const weeks = []
  let day = startOfWeek(first)
  do {
    weeks.push(Array.from({ length: 7 }, (_, i) => addDays(day, i)))
    day = addDays(day, 7)
  } while (day.slice(0, 7) === lastMonth)
  return weeks
}

export const datePart = (dateTime) => dateTime.slice(0, 10)
export const timePart = (dateTime) => dateTime.slice(11, 16)

/** Minutes since midnight of a date-time. */
export function minutesOfDay(dateTime) {
  const [h, m] = timePart(dateTime).split(':').map(Number)
  return h * 60 + m
}

export function combine(date, time) {
  return `${date}T${time || '00:00'}`
}

/** Adds minutes to a minute date-time, across days. */
export function addMinutes(dateTime, minutes) {
  const date = parseIsoDate(datePart(dateTime))
  date.setMinutes(minutesOfDay(dateTime) + minutes)
  return `${toIsoDate(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function monthOf(iso) {
  return iso.slice(0, 7)
}
