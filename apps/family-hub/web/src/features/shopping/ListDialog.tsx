// Edits the name and the planned date of a shopping list.
import { useState } from 'react'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useAction } from '../../app/useAction.ts'
import { Dialog } from '../../components/Dialog.tsx'
import { Button, ErrorMessage, Field } from '../../components/ui.tsx'

export function ListDialog({ list, onClose }: { list: Schemas['ShoppingList'], onClose: () => void }) {
  const [name, setName] = useState(list.name)
  const [plannedFor, setPlannedFor] = useState(list.plannedFor ?? '')
  const save = useAction(() => unwrap(api().PUT('/api/shopping/lists/{id}', { params: { path: { id: list.id } },
    body: { name, plannedFor: plannedFor || null } })),
  { refresh: ['shopping', 'dashboard', 'planning'], inline: true, success: 'Liste modifiée', onSuccess: onClose })

  return (
    <Dialog title="Modifier la liste" onClose={onClose}
      footer={<Button variant="primary" type="submit" form="list-form" disabled={save.isPending}>Enregistrer</Button>}>
      <form id="list-form" onSubmit={(e) => { e.preventDefault(); save.mutate(undefined) }} className="space-y-4">
        <Field label="Nom">
          <input className="input" value={name} maxLength={80} required onChange={(e) => setName(e.target.value)} />
        </Field>
        <Field label="Prévue le">
          <input className="input" type="date" value={plannedFor} onChange={(e) => setPlannedFor(e.target.value)} />
        </Field>
        <ErrorMessage error={save.error} />
      </form>
    </Dialog>
  )
}
