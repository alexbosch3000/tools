// To-do lists: quick add, assignee, due date-time, priority and repeating chores.
import { useState } from 'react'
import { del, post, put } from '../lib/api.js'
import { combine, datePart, nowLocal, timePart } from '../lib/dates.js'
import { formatDateTime } from '../lib/format.js'
import { PRIORITIES, REPEATS } from '../lib/labels.js'
import { useResource } from '../lib/useResource.js'
import { useSession } from '../lib/session.js'
import { useSubmit } from '../lib/useSubmit.js'
import { Badge, Button, Card, Checkbox, Empty, ErrorMessage, Field, Loading, MemberDot, Modal, PageHeader,
  Select } from '../components/ui.jsx'

function ItemModal({ item, lists, onClose, onSaved }) {
  const { members } = useSession()
  const [form, setForm] = useState({
    listId: String(item.listId), title: item.title ?? '', notes: item.notes ?? '',
    assigneeId: item.assigneeId ? String(item.assigneeId) : '',
    dueDate: item.dueAt ? datePart(item.dueAt) : '', dueTime: item.dueAt ? timePart(item.dueAt) : '',
    priority: item.priority ?? 'NORMAL', repeat: item.repeat ?? '',
  })
  const { busy, error, run } = useSubmit()
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))

  const save = (e) => {
    e.preventDefault()
    run(async () => {
      const body = {
        listId: Number(form.listId), title: form.title, notes: form.notes,
        assigneeId: form.assigneeId ? Number(form.assigneeId) : null,
        dueAt: form.dueDate ? combine(form.dueDate, form.dueTime || '18:00') : null,
        priority: form.priority, repeat: form.repeat || null,
      }
      if (item.id) await put(`/api/todo/items/${item.id}`, body)
      else await post('/api/todo/items', body)
      onSaved()
    })
  }

  return (
    <Modal title={item.id ? 'Modifier la tâche' : 'Nouvelle tâche'} onClose={onClose} footer={(
      <>
        {item.id && <Button variant="danger" className="mr-auto" disabled={busy}
          onClick={() => run(async () => { await del(`/api/todo/items/${item.id}`); onSaved() })}>Supprimer</Button>}
        <Button variant="primary" type="submit" form="todo-form" disabled={busy}>Enregistrer</Button>
      </>
    )}>
      <form id="todo-form" onSubmit={save} className="space-y-4">
        <Field label="Tâche"><input className="input" value={form.title} required maxLength={200}
          onChange={(e) => set({ title: e.target.value })} /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Liste">
            <Select options={Object.fromEntries(lists.map((l) => [String(l.id), l.name]))} value={form.listId}
              onChange={(v) => set({ listId: v })} />
          </Field>
          <Field label="Pour">
            <Select options={Object.fromEntries(members.map((m) => [String(m.id), m.name]))} value={form.assigneeId}
              placeholder="Personne" onChange={(v) => set({ assigneeId: v ?? '' })} />
          </Field>
          <Field label="Échéance">
            <div className="flex gap-2">
              <input className="input" type="date" value={form.dueDate} onChange={(e) => set({ dueDate: e.target.value })} />
              <input className="input w-28" type="time" value={form.dueTime} disabled={!form.dueDate}
                onChange={(e) => set({ dueTime: e.target.value })} />
            </div>
          </Field>
          <Field label="Priorité">
            <Select options={PRIORITIES} value={form.priority} onChange={(v) => set({ priority: v ?? 'NORMAL' })} />
          </Field>
        </div>
        <Field label="Répéter" hint="À chaque fois que la tâche est faite, la suivante est créée.">
          <Select options={REPEATS} value={form.repeat} placeholder="Non" onChange={(v) => set({ repeat: v ?? '' })} />
        </Field>
        <Field label="Notes"><textarea className="input" rows="2" value={form.notes}
          onChange={(e) => set({ notes: e.target.value })} /></Field>
        <ErrorMessage error={error} />
      </form>
    </Modal>
  )
}

function ListsPanel({ lists, selected, onSelect, onChanged }) {
  const [name, setName] = useState('')
  const { error, run } = useSubmit()
  const add = (e) => {
    e.preventDefault()
    run(async () => { const list = await post('/api/todo/lists', { name }); setName(''); onChanged(); onSelect(list.id) })
  }
  return (
    <Card title="Listes">
      <ul className="space-y-1">
        {lists.map((l) => (
          <li key={l.id}>
            <button type="button" onClick={() => onSelect(l.id)}
              className={`flex w-full items-center justify-between rounded-lg px-3 py-2 text-sm ${selected === l.id
                ? 'bg-indigo-50 font-medium text-indigo-700' : 'text-slate-700 hover:bg-slate-50'}`}>
              {l.name} <span className="text-xs text-slate-500">{l.openCount}</span>
            </button>
          </li>
        ))}
      </ul>
      <form onSubmit={add} className="mt-3 flex gap-2">
        <input className="input" placeholder="Nouvelle liste" value={name} maxLength={60} onChange={(e) => setName(e.target.value)} required />
        <Button type="submit">+</Button>
      </form>
      <ErrorMessage error={error} />
    </Card>
  )
}

export default function TodoPage() {
  const { members } = useSession()
  const lists = useResource('/api/todo/lists')
  const [selected, setSelected] = useState(null)
  const [showDone, setShowDone] = useState(false)
  const [modal, setModal] = useState(null)
  const [quick, setQuick] = useState('')
  const action = useSubmit()

  const listId = selected ?? lists.data?.[0]?.id ?? null
  const items = useResource(listId ? `/api/todo/items?listId=${listId}&includeDone=${showDone}` : null)
  const refresh = () => { items.reload(); lists.reload() }
  const now = nowLocal()

  if (lists.loading) return <Loading />
  if (lists.error) return <ErrorMessage error={lists.error} />

  const addQuick = (e) => {
    e.preventDefault()
    action.run(async () => { await post('/api/todo/items', { listId, title: quick }); setQuick(''); refresh() })
  }
  const toggle = (item) => action.run(async () => { await put(`/api/todo/items/${item.id}/done`, { done: !item.done }); refresh() })
  const current = lists.data.find((l) => l.id === listId)

  return (
    <>
      <PageHeader title="À faire" subtitle="Tâches ménagères, démarches, école : qui fait quoi, et pour quand." />
      <div className="grid gap-4 md:grid-cols-[16rem_1fr]">
        <ListsPanel lists={lists.data} selected={listId} onSelect={setSelected} onChanged={lists.reload} />
        <Card title={current?.name ?? 'Liste'} actions={<Checkbox label="Afficher les tâches faites" checked={showDone} onChange={setShowDone} />}>
          {listId && (
            <form onSubmit={addQuick} className="mb-4 flex gap-2">
              <input className="input" placeholder="Ajouter une tâche…" value={quick} maxLength={200}
                onChange={(e) => setQuick(e.target.value)} required />
              <Button type="submit" variant="primary">Ajouter</Button>
            </form>
          )}
          <ErrorMessage error={action.error} />
          {items.data && items.data.length === 0 && <Empty>Rien à faire ici.</Empty>}
          <ul className="divide-y divide-slate-100">
            {(items.data ?? []).map((item) => {
              const overdue = !item.done && item.dueAt && item.dueAt.slice(0, 16) < now
              return (
                <li key={item.id} className="flex items-center gap-3 py-2">
                  <input type="checkbox" checked={item.done} onChange={() => toggle(item)} aria-label={`Fait : ${item.title}`}
                    className="h-5 w-5 rounded border-slate-300 text-indigo-600" />
                  <button type="button" onClick={() => setModal(item)} className="flex flex-1 flex-col text-left">
                    <span className={item.done ? 'text-slate-400 line-through' : 'text-slate-800'}>{item.title}</span>
                    <span className="flex flex-wrap gap-2 text-xs">
                      {item.dueAt && <span className={overdue ? 'font-semibold text-red-700' : 'text-slate-500'}>
                        {overdue ? 'En retard · ' : ''}{formatDateTime(item.dueAt)}</span>}
                      {item.repeat && <span className="text-slate-500">{REPEATS[item.repeat]}</span>}
                    </span>
                  </button>
                  {item.priority === 'HIGH' && <Badge className="bg-red-50 text-red-700">Prioritaire</Badge>}
                  <MemberDot member={members.find((m) => m.id === item.assigneeId)} />
                </li>
              )
            })}
          </ul>
          {current && (
            <div className="mt-4 flex justify-between">
              <Button onClick={() => setModal({ listId })}>Tâche détaillée</Button>
              <Button variant="danger" onClick={() => action.run(async () => {
                await del(`/api/todo/lists/${listId}`); setSelected(null); lists.reload()
              })}>Supprimer la liste</Button>
            </div>
          )}
        </Card>
      </div>
      {modal && <ItemModal item={modal} lists={lists.data} onClose={() => setModal(null)}
        onSaved={() => { setModal(null); refresh() }} />}
    </>
  )
}
