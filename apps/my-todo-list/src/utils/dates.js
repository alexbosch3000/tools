// Helpers for ISO calendar dates (YYYY-MM-DD) expressed in the user's local time zone.

const pad = (n) => String(n).padStart(2, '0')

export function toIsoDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

// Parses YYYY-MM-DD as local midnight; `new Date(iso)` would parse it as UTC.
export function parseIsoDate(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function today() {
  return toIsoDate(new Date())
}

export function formatDate(iso) {
  if (!iso) return null
  const [y, m, d] = iso.split('-')
  return `${d}/${m}/${y}`
}

export function isPast(iso) {
  return Boolean(iso && iso < today())
}

export function isToday(iso) {
  return iso === today()
}
