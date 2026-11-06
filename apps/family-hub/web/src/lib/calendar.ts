// Placement of planning occurrences and scheduled to-dos in the week and month views.
import type { Schemas } from '../api/client.ts'
import { addDays, datePart, minutesOfDay, timePart } from './dates.ts'

type Occurrence = Schemas['Occurrence']
type TodoItem = Schemas['TodoItem']

/** What the planning shows: an occurrence of a planning entry, or a to-do with a start and/or a due date. */
export type CalendarItem =
  | { kind: 'entry', key: string, start: string, end: string, allDay: boolean, occurrence: Occurrence }
  | { kind: 'task', key: string, start: string, end: string, allDay: false, task: TodoItem }

export function entryItem(occurrence: Occurrence): CalendarItem {
  return { kind: 'entry', key: `entry-${occurrence.entry.id}-${occurrence.start}`, start: occurrence.start,
    end: occurrence.end, allDay: occurrence.entry.allDay, occurrence }
}

/** A to-do spans its start to its due date; with only one of them it is an instant; with none it is not shown. */
export function taskItem(task: TodoItem): CalendarItem | null {
  const start = task.startAt ?? task.dueAt
  const end = task.dueAt ?? task.startAt
  if (!start || !end) return null
  return { kind: 'task', key: `task-${task.id}`, start, end, allDay: false, task }
}

const DAY_MINUTES = 24 * 60

/** Whether an item touches a given day; an instant counts on its own day. */
export function touchesDay(item: CalendarItem, day: string): boolean {
  const dayStart = `${day}T00:00`
  const dayEnd = `${addDays(day, 1)}T00:00`
  const start = item.start.slice(0, 16)
  const end = item.end.slice(0, 16)
  return start < dayEnd && (end > dayStart || start >= dayStart)
}

/**
 * All-day items and timed items spanning several days are shown in the band above the grid; an
 * entry ending at midnight the next day still fits in its day.
 */
export function isBanner(item: CalendarItem): boolean {
  if (item.allDay) return true
  const startDay = datePart(item.start)
  const endDay = datePart(item.end)
  if (startDay === endDay) return false
  return !(endDay === addDays(startDay, 1) && timePart(item.end) === '00:00')
}

function endMinutes(item: CalendarItem): number {
  return datePart(item.end) > datePart(item.start) ? DAY_MINUTES : minutesOfDay(item.end)
}

export interface Placement {
  item: CalendarItem
  /** Minutes since midnight. */
  top: number
  bottom: number
  column: number
  columns: number
}

/**
 * Lays out the timed items of a day in columns: overlapping items share the width of
 * their cluster, the others take it all.
 */
export function layoutDay(calendarItems: CalendarItem[], day: string): Placement[] {
  const items = calendarItems
    .filter((o) => !isBanner(o) && touchesDay(o, day))
    .map((o) => {
      const top = minutesOfDay(o.start)
      // Short items keep a readable height of 20 minutes.
      return { item: o, top, bottom: Math.min(DAY_MINUTES, Math.max(endMinutes(o), top + 20)), column: 0 }
    })
    .sort((a, b) => a.top - b.top || b.bottom - a.bottom)

  const result: Placement[] = []
  let cluster: typeof items = []
  let clusterEnd = -1
  const flush = () => {
    const columnEnds: number[] = []
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

/** Items of a day for the month view: banners first, then by start time. */
export function dayAgenda(calendarItems: CalendarItem[], day: string): CalendarItem[] {
  return calendarItems
    .filter((o) => touchesDay(o, day))
    .sort((a, b) => Number(isBanner(b)) - Number(isBanner(a)) || a.start.localeCompare(b.start))
}
