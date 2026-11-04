// Text field over the catalog with suggestions; the parent resolves the typed name with findItem.
import { useId } from 'react'
import type { Schemas } from '../api/client.ts'

export function ItemPicker({ items, value, onChange, placeholder = 'Rechercher un article…' }:
  { items: Schemas['Item'][] | undefined, value: string, onChange: (value: string) => void, placeholder?: string }) {
  const listId = useId()
  return (
    <>
      <input className="input" list={listId} value={value} placeholder={placeholder} aria-label={placeholder}
        onChange={(e) => onChange(e.target.value)} />
      <datalist id={listId}>
        {(items ?? []).map((i) => <option key={i.id} value={i.name} />)}
      </datalist>
    </>
  )
}
