// Placement of occurrences in the week and month views.
import { describe, expect, it } from 'vitest'
import { dayAgenda, isBanner, layoutDay, touchesDay } from './calendar.js'

const occ = (id, start, end, allDay = false) => ({ start, end, entry: { id, allDay, title: id } })

describe('calendar layout', () => {
  it('puts overlapping occurrences side by side and others at full width', () => {
    const layout = layoutDay([
      occ('a', '2026-11-03T09:00:00', '2026-11-03T10:00:00'),
      occ('b', '2026-11-03T09:30:00', '2026-11-03T11:00:00'),
      occ('c', '2026-11-03T10:00:00', '2026-11-03T10:30:00'),
      occ('d', '2026-11-03T14:00:00', '2026-11-03T15:00:00'),
    ], '2026-11-03')

    const byId = Object.fromEntries(layout.map((l) => [l.occurrence.entry.id, l]))
    expect(byId.a).toMatchObject({ top: 540, bottom: 600, column: 0, columns: 2 })
    expect(byId.b).toMatchObject({ column: 1, columns: 2 })
    expect(byId.c).toMatchObject({ column: 0, columns: 2 })
    expect(byId.d).toMatchObject({ column: 0, columns: 1 })
  })

  it('gives short occurrences a readable height and keeps only the requested day', () => {
    const layout = layoutDay([
      occ('short', '2026-11-03T08:00:00', '2026-11-03T08:05:00'),
      occ('other', '2026-11-04T08:00:00', '2026-11-04T09:00:00'),
      occ('midnight', '2026-11-03T23:00:00', '2026-11-04T00:00:00'),
    ], '2026-11-03')
    expect(layout.map((l) => [l.occurrence.entry.id, l.top, l.bottom])).toEqual([
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
      .map((o) => o.entry.id)).toEqual(['trip', 'x'])
  })
})
