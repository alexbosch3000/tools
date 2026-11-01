import { describe, expect, it } from 'vitest'
import { buildWeeks, computeWeekLayout } from './calendar.js'

describe('buildWeeks', () => {
  it('returns 6 weeks starting on the Monday before the 1st', () => {
    const weeks = buildWeeks(2026, 10)
    expect(weeks).toHaveLength(6)
    expect(weeks[0][0]).toBe('2026-10-26')
    expect(weeks[0][6]).toBe('2026-11-01')
    expect(weeks[5][6]).toBe('2026-12-06')
  })

  it('starts on the 1st when the month begins on a Monday', () => {
    expect(buildWeeks(2026, 5)[0][0]).toBe('2026-06-01')
  })
})

describe('computeWeekLayout', () => {
  const week = buildWeeks(2026, 10)[1]

  it('clips tasks to the week and flags continuation', () => {
    const [t] = computeWeekLayout([{ id: 'a', start: '2026-10-30', end: '2026-11-04' }], week)
    expect(t).toMatchObject({ startCol: 0, endCol: 2, startsHere: false, endsHere: true, lane: 0 })
  })

  it('stacks overlapping tasks on separate lanes', () => {
    const layout = computeWeekLayout([
      { id: 'a', start: '2026-11-02', end: '2026-11-04' },
      { id: 'b', start: '2026-11-03', end: '2026-11-03' },
      { id: 'c', start: '2026-11-05', end: '2026-11-05' },
    ], week)
    const lanes = Object.fromEntries(layout.map(t => [t.id, t.lane]))
    expect(lanes).toEqual({ a: 0, b: 1, c: 0 })
  })

  it('treats a task with a single date as a one-day task', () => {
    const [t] = computeWeekLayout([{ id: 'a', end: '2026-11-06' }], week)
    expect(t).toMatchObject({ startCol: 4, endCol: 4 })
  })

  it('ignores tasks outside the week', () => {
    expect(computeWeekLayout([{ id: 'a', start: '2026-12-01', end: '2026-12-02' }], week)).toEqual([])
  })
})
