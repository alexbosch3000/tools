// Look of a calendar item (planning entry or to-do) as a chip: colours, prefix and accessible description.
import type { CalendarItem } from '../../lib/calendar.ts'
import { PRIORITIES, entryColor, taskColor } from '../../lib/labels.ts'

export function itemTitle(item: CalendarItem): string {
  return item.kind === 'entry' ? item.occurrence.entry.title : item.task.title
}

export function itemDone(item: CalendarItem): boolean {
  return item.kind === 'entry' ? item.occurrence.entry.done : item.task.done
}

/** Colour classes, struck through and faded once done. */
export function itemClass(item: CalendarItem): string {
  const colors = item.kind === 'entry' ? entryColor(item.occurrence.entry) : taskColor(item.task.priority)
  return `${colors} ${itemDone(item) ? 'line-through opacity-50' : ''}`
}

/** Marks a to-do among planning entries: a box that is ticked once done, and "!" for a high priority. */
export function itemPrefix(item: CalendarItem): string {
  if (item.kind === 'entry') return ''
  return `${item.task.done ? '☑' : '☐'} ${item.task.priority === 'HIGH' ? '! ' : ''}`
}

/** Description of a to-do for assistive technologies and tooltips; null for planning entries. */
export function taskLabel(item: CalendarItem): string | null {
  return item.kind === 'task' ? `Tâche · priorité ${PRIORITIES[item.task.priority]}` : null
}
