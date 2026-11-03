// Matching of hash routes.
import { describe, expect, it } from 'vitest'
import { matchRoute } from './router.js'

describe('matchRoute', () => {
  it('extracts path parameters and the query', () => {
    const match = matchRoute('/courses/12?store=DIY', '/courses/:id')
    expect(match.id).toBe('12')
    expect(match.query.get('store')).toBe('DIY')
  })

  it('rejects other paths', () => {
    expect(matchRoute('/courses', '/courses/:id')).toBeNull()
    expect(matchRoute('/recettes/3', '/courses/:id')).toBeNull()
  })
})
