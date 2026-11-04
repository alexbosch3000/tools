// Lookup of catalog items by typed name.
import { describe, expect, it } from 'vitest'
import type { Schemas } from '../api/client.ts'
import { findItem } from './items.ts'

const item = (id: number, name: string) => ({ id, name }) as Schemas['Item']
const items = [item(1, 'Œufs'), item(2, 'Vis à bois')]

describe('findItem', () => {
  it('matches a name ignoring case and spaces', () => {
    expect(findItem(items, '  vis À BOIS ')?.id).toBe(2)
    expect(findItem(items, 'œufs')?.id).toBe(1)
  })

  it('returns null for a partial name, no text or no catalog yet', () => {
    expect(findItem(items, 'Vis')).toBeNull()
    expect(findItem(items, '')).toBeNull()
    expect(findItem(undefined, 'Œufs')).toBeNull()
  })
})
