// Placement of occurrences in the week and month views.
import { describe, expect, it } from 'vitest'
import type { Schemas } from '../api/client.ts'
import { dayAgenda, entryItem, isBanner, layoutDay, taskItem, touchesDay, type CalendarItem } from './calendar.ts'

const occ = (title: string, start: string, end: string, allDay = false) =>
  entryItem({ start, end, entry: { id: title.length, title, allDay } } as unknown as Schemas['Occurrence'])

const title = (item: CalendarItem) => (item.kind === 'entry' ? item.occurrence.entry.title : item.task.title)

const task = (patch: Partial<Schemas['TodoItem']>) =>
  taskItem({ id: 1, listId: 1, title: 'Tâche', notes: '', priority: 'NORMAL', done: false, ...patch })

describe('calendar layout', () => {
  it('puts overlapping occurrences side by side and others at full width', () => {
    const layout = layoutDay([
      occ('a', '2026-11-03T09:00:00', '2026-11-03T10:00:00'),
      occ('bb', '2026-11-03T09:30:00', '2026-11-03T11:00:00'),
      occ('ccc', '2026-11-03T10:00:00', '2026-11-03T10:30:00'),
      occ('dddd', '2026-11-03T14:00:00', '2026-11-03T15:00:00'),
    ], '2026-11-03')
    const by = Object.fromEntries(layout.map((l) => [title(l.item), l]))
    expect(by.a).toMatchObject({ top: 540, bottom: 600, column: 0, columns: 2 })
    expect(by.bb).toMatchObject({ column: 1, columns: 2 })
    expect(by.ccc).toMatchObject({ column: 0, columns: 2 })
    expect(by.dddd).toMatchObject({ column: 0, columns: 1 })
  })

  it('gives short occurrences a readable height and keeps only the requested day', () => {
    const layout = layoutDay([
      occ('short', '2026-11-03T08:00:00', '2026-11-03T08:05:00'),
      occ('other', '2026-11-04T08:00:00', '2026-11-04T09:00:00'),
      occ('midnight', '2026-11-03T23:00:00', '2026-11-04T00:00:00'),
    ], '2026-11-03')
    expect(layout.map((l) => [title(l.item), l.top, l.bottom])).toEqual([
      ['short', 480, 500], ['midnight', 1380, 1440]])
  })

  it('shows all-day and multi-day occurrences as banners on each day they cover', () => {
    const holidays = occ('holidays', '2026-12-19T00:00:00', '2027-01-03T23:59:00', true)
    const trip = occ('trip', '2026-11-03T18:00:00', '2026-11-05T12:00:00')
    expect(isBanner(holidays)).toBe(true)
    expect(isBanner(trip)).toBe(true)
    expect(touchesDay(trip, '2026-11-04')).toBe(true)
    expect(touchesDay(trip, '2026-11-06')).toBe(false)
    expect(dayAgenda([occ('x', '2026-11-04T08:00:00', '2026-11-04T09:00:00'), trip], '2026-11-04')
      .map(title)).toEqual(['trip', 'x'])
  })
})

describe('to-dos in the calendar', () => {
  it('places a due-only task as an instant at its due time', () => {
    const item = task({ dueAt: '2026-11-03T18:00:00' })!
    expect(item).toMatchObject({ start: '2026-11-03T18:00:00', end: '2026-11-03T18:00:00' })
    expect(isBanner(item)).toBe(false)
    expect(layoutDay([item], '2026-11-03').map((l) => [l.top, l.bottom])).toEqual([[1080, 1100]])
  })

  it('keeps a same-day span in the grid and shows a multi-day span as a banner', () => {
    expect(isBanner(task({ startAt: '2026-11-03T09:00:00', dueAt: '2026-11-03T12:00:00' })!)).toBe(false)
    const span = task({ startAt: '2026-11-02T09:00:00', dueAt: '2026-11-04T18:00:00' })!
    expect(isBanner(span)).toBe(true)
    expect(touchesDay(span, '2026-11-03')).toBe(true)
    expect(touchesDay(span, '2026-11-05')).toBe(false)
  })

  it('uses the start when there is no due date and ignores undated tasks', () => {
    expect(task({ startAt: '2026-11-03T09:00:00' })).toMatchObject({ end: '2026-11-03T09:00:00' })
    expect(task({})).toBeNull()
  })
})
