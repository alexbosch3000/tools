// Placement of planning occurrences in the week and month views.
import { addDays, datePart, minutesOfDay, timePart } from './dates.js'

const DAY_MINUTES = 24 * 60

/** Whether an occurrence touches a given day; an instant occurrence counts on its own day. */
export function touchesDay(occurrence, day) {
  const dayStart = `${day}T00:00`
  const dayEnd = `${addDays(day, 1)}T00:00`
  const start = occurrence.start.slice(0, 16)
  const end = occurrence.end.slice(0, 16)
  return start < dayEnd && (end > dayStart || start >= dayStart)
}

/**
 * All-day entries and timed entries spanning several days are shown in the band above the grid; an
 * entry ending at midnight the next day still fits in its day.
 */
export function isBanner(occurrence) {
  if (occurrence.entry.allDay) return true
  const startDay = datePart(occurrence.start)
  const endDay = datePart(occurrence.end)
  if (startDay === endDay) return false
  return !(endDay === addDays(startDay, 1) && timePart(occurrence.end) === '00:00')
}

function endMinutes(occurrence) {
  return datePart(occurrence.end) > datePart(occurrence.start) ? DAY_MINUTES : minutesOfDay(occurrence.end)
}

/**
 * Lays out the timed occurrences of a day in columns: overlapping occurrences share the width of
 * their cluster. Returns { occurrence, top, bottom, column, columns } with top and bottom in minutes.
 */
export function layoutDay(occurrences, day) {
  const items = occurrences
    .filter((o) => !isBanner(o) && touchesDay(o, day))
    .map((o) => {
      const top = minutesOfDay(o.start)
      // Short entries keep a readable height of 20 minutes.
      const bottom = Math.min(DAY_MINUTES, Math.max(endMinutes(o), top + 20))
      return { occurrence: o, top, bottom }
    })
    .sort((a, b) => a.top - b.top || b.bottom - a.bottom)

  const result = []
  let cluster = []
  let clusterEnd = -1
  const flush = () => {
    const columnEnds = []
    for (const item of cluster) {
      let column = columnEnds.findIndex((end) => end <= item.top)
      if (column === -1) column = columnEnds.length
      columnEnds[column] = item.bottom
      item.column = column
    }
    for (const item of cluster) result.push({ ...item, columns: columnEnds.length })
    cluster = []
  }
  for (const item of items) {
    if (cluster.length && item.top >= clusterEnd) flush()
    cluster.push(item)
    clusterEnd = Math.max(clusterEnd, item.bottom)
  }
  if (cluster.length) flush()
  return result
}

/** Occurrences of a day for the month view: banners first, then by start time. */
export function dayAgenda(occurrences, day) {
  return occurrences
    .filter((o) => touchesDay(o, day))
    .sort((a, b) => Number(isBanner(b)) - Number(isBanner(a)) || a.start.localeCompare(b.start))
}
