// Lookup of catalog items by the name typed in a search field.
import type { Schemas } from '../api/client.ts'

/** The item whose name matches the text, ignoring case and surrounding spaces; null when none does. */
export function findItem(items: Schemas['Item'][] | undefined, text: string): Schemas['Item'] | null {
  const name = text.trim().toLocaleLowerCase('fr')
  if (!name || !items) return null
  return items.find((item) => item.name.toLocaleLowerCase('fr') === name) ?? null
}
