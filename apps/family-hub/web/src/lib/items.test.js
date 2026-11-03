// Lookup of catalog items by typed name.
import { describe, expect, it } from 'vitest'
import { findItem } from './items.js'

const items = [{ id: 1, name: 'Œufs' }, { id: 2, name: 'Vis à bois' }]

describe('findItem', () => {
  it('matches a name ignoring case and spaces', () => {
    expect(findItem(items, '  vis À BOIS ')).toEqual(items[1])
    expect(findItem(items, 'œufs')).toEqual(items[0])
  })

  it('returns null for a partial name, no text or no catalog yet', () => {
    expect(findItem(items, 'Vis')).toBeNull()
    expect(findItem(items, '')).toBeNull()
    expect(findItem(null, 'Œufs')).toBeNull()
  })
})
