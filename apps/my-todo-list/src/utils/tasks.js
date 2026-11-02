// Filtering and ordering rules for the task list.
import { today } from './dates.js'

export const FILTERS = {
  all:      () => true,
  active:   t => !t.done && (!t.end || t.end >= today()),
  upcoming: t => !t.done && t.start && t.start > today(),
  overdue:  t => !t.done && t.end && t.end < today(),
  done:     t => t.done,
}

// Open tasks first, then by nearest due date, then most recently created.
function compareTasks(a, b) {
  if (a.done !== b.done) return a.done ? 1 : -1
  if (a.end && b.end) return a.end.localeCompare(b.end)
  if (a.end) return -1
  if (b.end) return 1
  return b.createdAt.localeCompare(a.createdAt)
}

export function selectTasks(tasks, filter, search) {
  const q = search.toLowerCase().trim()
  return tasks
    .filter(FILTERS[filter] ?? FILTERS.all)
    .filter(t => !q || t.title.toLowerCase().includes(q) || (t.desc || '').toLowerCase().includes(q))
    .sort(compareTasks)
}
