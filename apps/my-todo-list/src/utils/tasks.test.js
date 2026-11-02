import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { selectTasks } from './tasks.js'

const task = (id, fields = {}) => ({ id, title: id, done: false, createdAt: '2026-01-01T00:00:00Z', ...fields })

describe('selectTasks', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 10, 1, 12))
  })
  afterEach(() => vi.useRealTimers())

  const tasks = [
    task('overdue', { end: '2026-10-30' }),
    task('upcoming', { start: '2026-11-10', end: '2026-11-12' }),
    task('open'),
    task('done', { done: true }),
  ]

  it.each([
    ['all', ['overdue', 'upcoming', 'open', 'done']],
    ['active', ['upcoming', 'open']],
    ['upcoming', ['upcoming']],
    ['overdue', ['overdue']],
    ['done', ['done']],
  ])('applies the "%s" filter', (filter, expected) => {
    expect(selectTasks(tasks, filter, '').map(t => t.id)).toEqual(expected)
  })

  it('searches title and description case-insensitively', () => {
    const list = [task('Alpha'), task('beta', { desc: 'contains ALPHA' }), task('gamma')]
    expect(selectTasks(list, 'all', ' alpha ').map(t => t.id)).toEqual(['Alpha', 'beta'])
  })

  it('falls back to all tasks for an unknown filter', () => {
    expect(selectTasks(tasks, 'unknown', '')).toHaveLength(4)
  })
})
