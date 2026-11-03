// Local calendar arithmetic on ISO dates and minute date-times.
import { describe, expect, it } from 'vitest'
import { addDays, addMinutes, addMonths, minutesOfDay, monthGrid, startOfWeek, weekDays } from './dates.js'

describe('dates', () => {
  it('finds the Monday of a week, across months', () => {
    expect(startOfWeek('2026-11-03')).toBe('2026-11-02')
    expect(startOfWeek('2026-11-01')).toBe('2026-10-26')
    expect(weekDays('2026-11-08')).toEqual(['2026-11-02', '2026-11-03', '2026-11-04', '2026-11-05',
      '2026-11-06', '2026-11-07', '2026-11-08'])
  })

  it('adds days and months', () => {
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01')
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28')
    expect(addMonths('2026-01-31', 1)).toBe('2026-02-01')
  })

  it('builds the weeks of a month view', () => {
    const weeks = monthGrid('2026-11-15')
    expect(weeks[0][0]).toBe('2026-10-26')
    expect(weeks.at(-1)[6]).toBe('2026-12-06')
    expect(weeks).toHaveLength(6)
    expect(monthGrid('2027-02-10')).toHaveLength(4)
  })

  it('works with minute date-times', () => {
    expect(minutesOfDay('2026-11-03T18:30:00')).toBe(1110)
    expect(addMinutes('2026-11-03T23:30', 45)).toBe('2026-11-04T00:15')
  })
})
