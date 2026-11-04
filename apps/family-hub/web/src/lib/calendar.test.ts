// Placement of occurrences in the week and month views.
import { describe, expect, it } from 'vitest'
import type { Schemas } from '../api/client.ts'
import { dayAgenda, isBanner, layoutDay, touchesDay } from './calendar.ts'

const occ = (title: string, start: string, end: string, allDay = false) =>
  ({ start, end, entry: { id: title.length, title, allDay } }) as unknown as Schemas['Occurrence']

describe('calendar layout', () => {
  it('puts overlapping occurrences side by side and others at full width', () => {
    const layout = layoutDay([
      occ('a', '2026-11-03T09:00:00', '2026-11-03T10:00:00'),
      occ('bb', '2026-11-03T09:30:00', '2026-11-03T11:00:00'),
      occ('ccc', '2026-11-03T10:00:00', '2026-11-03T10:30:00'),
      occ('dddd', '2026-11-03T14:00:00', '2026-11-03T15:00:00'),
    ], '2026-11-03')
    const by = Object.fromEntries(layout.map((l) => [l.occurrence.entry.title, l]))
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
    expect(layout.map((l) => [l.occurrence.entry.title, l.top, l.bottom])).toEqual([
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
      .map((o) => o.entry.title)).toEqual(['trip', 'x'])
  })
})
