// Text field over the catalog with suggestions; the parent resolves the typed name with findItem.
import { useId } from 'react'

export default function ItemPicker({ items, value, onChange, placeholder = 'Rechercher un article…' }) {
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
