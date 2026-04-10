// Helpers pour la manipulation et l'affichage des dates ISO (YYYY-MM-DD)

export function today() {
  return new Date().toISOString().slice(0, 10)
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
