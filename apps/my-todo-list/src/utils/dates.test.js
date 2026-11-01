import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { formatDate, isPast, isToday, parseIsoDate, toIsoDate, today } from './dates.js'

describe('dates', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 10, 1, 0, 30))
  })
  afterEach(() => vi.useRealTimers())

  it('formats local dates without shifting to UTC', () => {
    expect(toIsoDate(new Date(2026, 10, 1))).toBe('2026-11-01')
    expect(today()).toBe('2026-11-01')
  })

  it('parses ISO dates as local midnight', () => {
    const d = parseIsoDate('2026-11-01')
    expect([d.getFullYear(), d.getMonth(), d.getDate(), d.getHours()]).toEqual([2026, 10, 1, 0])
  })

  it('formats ISO dates as DD/MM/YYYY', () => {
    expect(formatDate('2026-03-07')).toBe('07/03/2026')
    expect(formatDate(null)).toBeNull()
  })

  it('compares against the current day', () => {
    expect(isPast('2026-10-31')).toBe(true)
    expect(isPast('2026-11-01')).toBe(false)
    expect(isToday('2026-11-01')).toBe(true)
  })
})
