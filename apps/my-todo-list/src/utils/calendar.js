// Month grid and task bar layout for the calendar view.
import { toIsoDate } from './dates.js'

// Returns 6 weeks of ISO dates, Monday first, covering the given month.
export function buildWeeks(year, month) {
  const first = new Date(year, month, 1)
  const dow = first.getDay()
  const start = new Date(year, month, 1 - (dow === 0 ? 6 : dow - 1))

  return Array.from({ length: 6 }, (_, w) =>
    Array.from({ length: 7 }, (_, d) =>
      toIsoDate(new Date(start.getFullYear(), start.getMonth(), start.getDate() + w * 7 + d)),
    ),
  )
}

// Places each task overlapping the week on a column range and a lane.
// Lanes are assigned greedily so that bars never overlap visually.
export function computeWeekLayout(tasks, weekDays) {
  const [wStart, wEnd] = [weekDays[0], weekDays[6]]

  const placed = tasks
    .filter(t => {
      const s = t.start || t.end
      const e = t.end || t.start
      return s && s <= wEnd && e >= wStart
    })
    .map(t => {
      const s = t.start || t.end
      const e = t.end || t.start
      const startCol = s < wStart ? 0 : weekDays.indexOf(s)
      const endCol = e > wEnd ? 6 : weekDays.indexOf(e)
      return { ...t, startCol, endCol, startsHere: s >= wStart, endsHere: e <= wEnd }
    })
    .filter(t => t.startCol >= 0 && t.endCol >= 0)
    .sort((a, b) =>
      a.startCol !== b.startCol
        ? a.startCol - b.startCol
        : (b.endCol - b.startCol) - (a.endCol - a.startCol),
    )

  const occupied = []
  return placed.map(task => {
    let lane = 0
    while ((occupied[lane] || []).some(r => r.startCol <= task.endCol && r.endCol >= task.startCol)) {
      lane++
    }
    ;(occupied[lane] = occupied[lane] || []).push({ startCol: task.startCol, endCol: task.endCol })
    return { ...task, lane }
  })
}
