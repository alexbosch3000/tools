// Dialog to create or edit a to-do item: list, assignee, start and due date-times, priority, repetition.
import { useState, type FormEvent } from 'react'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useSession } from '../../app/useSession.ts'
import { useAction } from '../../app/useAction.ts'
import { combine, datePart, timePart } from '../../lib/dates.ts'
import { PRIORITIES, REPEATS } from '../../lib/labels.ts'
import { Dialog } from '../../components/Dialog.tsx'
import { Button, ErrorMessage, Field, Select } from '../../components/ui.tsx'

const AREAS = ['todo', 'dashboard'] as const

export function ItemDialog({ item, listId, lists, onClose }:
  { item?: Schemas['TodoItem'], listId: number, lists: Schemas['TodoList'][], onClose: () => void }) {
  const { members } = useSession()
  const [form, setForm] = useState({
    listId: String(item?.listId ?? listId), title: item?.title ?? '', notes: item?.notes ?? '',
    assigneeId: item?.assigneeId ? String(item.assigneeId) : '',
    startDate: item?.startAt ? datePart(item.startAt) : '', startTime: item?.startAt ? timePart(item.startAt) : '',
    dueDate: item?.dueAt ? datePart(item.dueAt) : '', dueTime: item?.dueAt ? timePart(item.dueAt) : '',
    priority: item?.priority ?? 'NORMAL' as Schemas['Priority'], repeat: (item?.repeat ?? '') as Schemas['Repeat'] | '',
  })
  const set = (patch: Partial<typeof form>) => setForm((f) => ({ ...f, ...patch }))
  const save = useAction(() => {
    const body: Schemas['TodoItemRequest'] = {
      listId: Number(form.listId), title: form.title, notes: form.notes,
      assigneeId: form.assigneeId ? Number(form.assigneeId) : null,
      startAt: form.startDate ? combine(form.startDate, form.startTime || '09:00') : null,
      dueAt: form.dueDate ? combine(form.dueDate, form.dueTime || '18:00') : null,
      priority: form.priority, repeat: form.repeat || null,
    }
    return item
      ? unwrap(api().PUT('/api/todo/items/{id}', { params: { path: { id: item.id } }, body }))
      : unwrap(api().POST('/api/todo/items', { body }))
  }, { refresh: [...AREAS], inline: true, onSuccess: onClose, success: item ? 'Tâche modifiée' : 'Tâche ajoutée' })
  const toggleDone = useAction(() => unwrap(api().PUT('/api/todo/items/{id}/done',
    { params: { path: { id: item!.id } }, body: { done: !item!.done } })),
  { refresh: [...AREAS], onSuccess: onClose, success: item?.done ? 'Tâche rouverte' : 'Tâche faite' })
  const remove = useAction(() => unwrap(api().DELETE('/api/todo/items/{id}', { params: { path: { id: item!.id } } })),
    { refresh: [...AREAS], onSuccess: onClose, success: 'Tâche supprimée' })

  return (
    <Dialog title={item ? 'Modifier la tâche' : 'Nouvelle tâche'} onClose={onClose} footer={(
      <>
        {item && <Button variant="danger" className="mr-auto" onClick={() => remove.mutate(undefined)}>Supprimer</Button>}
        {item && <Button onClick={() => toggleDone.mutate(undefined)} disabled={toggleDone.isPending}>{item.done ? 'Rouvrir' : 'Marquer faite'}</Button>}
        <Button variant="primary" type="submit" form="todo-form" disabled={save.isPending}>Enregistrer</Button>
      </>
    )}>
      <form id="todo-form" onSubmit={(e: FormEvent) => { e.preventDefault(); save.mutate(undefined) }} className="space-y-4">
        <Field label="Tâche"><input className="input" required maxLength={200} value={form.title} onChange={(e) => set({ title: e.target.value })} /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Liste">
            <Select options={Object.fromEntries(lists.map((l) => [String(l.id), l.name]))} value={form.listId} onChange={(v) => set({ listId: v })} />
          </Field>
          <Field label="Pour">
            <Select options={Object.fromEntries(members.map((m) => [String(m.id), m.name]))} value={form.assigneeId} placeholder="Personne"
              onChange={(v) => set({ assigneeId: v })} />
          </Field>
          <Field label="Début">
            <div className="flex gap-2">
              <input className="input" type="date" value={form.startDate} onChange={(e) => set({ startDate: e.target.value })} />
              <input className="input w-28" type="time" value={form.startTime} disabled={!form.startDate} aria-label="Heure de début"
                onChange={(e) => set({ startTime: e.target.value })} />
            </div>
          </Field>
          <Field label="Échéance">
            <div className="flex gap-2">
              <input className="input" type="date" value={form.dueDate} onChange={(e) => set({ dueDate: e.target.value })} />
              <input className="input w-28" type="time" value={form.dueTime} disabled={!form.dueDate} aria-label="Heure d'échéance"
                onChange={(e) => set({ dueTime: e.target.value })} />
            </div>
          </Field>
          <Field label="Priorité"><Select options={PRIORITIES} value={form.priority} onChange={(v) => set({ priority: v || 'NORMAL' })} /></Field>
        </div>
        <Field label="Répéter" hint="À chaque fois que la tâche est faite, la suivante est créée.">
          <Select options={REPEATS} value={form.repeat} placeholder="Non" onChange={(v) => set({ repeat: v })} />
        </Field>
        <Field label="Notes"><textarea className="input" rows={2} value={form.notes} onChange={(e) => set({ notes: e.target.value })} /></Field>
        <ErrorMessage error={save.error} />
      </form>
    </Dialog>
  )
}
