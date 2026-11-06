// To-do lists: quick add, assignee, start and due date-times, priority and repeating chores.
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useFeedback } from '../../app/useFeedback.ts'
import { useSession } from '../../app/useSession.ts'
import { useAction } from '../../app/useAction.ts'
import { nowLocal } from '../../lib/dates.ts'
import { formatDateTime } from '../../lib/format.ts'
import { REPEATS } from '../../lib/labels.ts'
import { ItemDialog } from './ItemDialog.tsx'
import { Badge, Button, Card, Checkbox, Empty, ErrorMessage, Loading, MemberDot, PageHeader } from '../../components/ui.tsx'

const AREAS = ['todo', 'dashboard'] as const

export function TodoPage() {
  const { members } = useSession()
  const { confirm } = useFeedback()
  const lists = useQuery({ queryKey: ['todo', 'lists'], queryFn: () => unwrap(api().GET('/api/todo/lists')) })
  const [selected, setSelected] = useState<number | null>(null)
  const [showDone, setShowDone] = useState(false)
  const [modal, setModal] = useState<{ item?: Schemas['TodoItem'] } | null>(null)
  const [quick, setQuick] = useState('')
  const [newList, setNewList] = useState('')
  const listId = selected ?? lists.data?.[0]?.id ?? null
  const items = useQuery({
    queryKey: ['todo', 'items', listId, showDone], enabled: listId !== null,
    queryFn: () => unwrap(api().GET('/api/todo/items', { params: { query: { listId: listId!, includeDone: showDone } } })),
  })
  const add = useAction(() => unwrap(api().POST('/api/todo/items', { body: { listId: listId!, title: quick } })),
    { refresh: [...AREAS], onSuccess: () => setQuick('') })
  const toggle = useAction((item: Schemas['TodoItem']) => unwrap(api().PUT('/api/todo/items/{id}/done',
    { params: { path: { id: item.id } }, body: { done: !item.done } })), { refresh: [...AREAS] })
  const createList = useAction(() => unwrap(api().POST('/api/todo/lists', { body: { name: newList } })),
    { refresh: ['todo'], onSuccess: (l) => { setNewList(''); setSelected(l.id) } })
  const deleteList = useAction((id: number) => unwrap(api().DELETE('/api/todo/lists/{id}', { params: { path: { id } } })),
    { refresh: [...AREAS], success: 'Liste supprimée', onSuccess: () => setSelected(null) })

  if (lists.isPending) return <Loading />
  if (lists.isError) return <ErrorMessage error={lists.error} />
  const current = lists.data.find((l) => l.id === listId)
  const now = nowLocal()

  return (
    <>
      <PageHeader title="À faire" subtitle="Tâches ménagères, démarches, école : qui fait quoi, et pour quand." />
      <div className="grid gap-4 md:grid-cols-[16rem_1fr]">
        <Card title="Listes">
          <ul className="space-y-1">
            {lists.data.map((l) => (
              <li key={l.id}>
                <button type="button" onClick={() => setSelected(l.id)} aria-current={l.id === listId || undefined}
                  className={`flex w-full items-center justify-between rounded-lg px-3 py-2 text-sm ${l.id === listId
                    ? 'bg-accent-soft font-medium text-accent' : 'hover:bg-surface-2'}`}>
                  {l.name} <span className="text-xs text-ink-3">{l.openCount}</span>
                </button>
              </li>
            ))}
          </ul>
          <form onSubmit={(e) => { e.preventDefault(); createList.mutate(undefined) }} className="mt-3 flex gap-2">
            <input className="input" placeholder="Nouvelle liste" aria-label="Nouvelle liste" required maxLength={60} value={newList}
              onChange={(e) => setNewList(e.target.value)} />
            <Button type="submit" aria-label="Créer la liste">+</Button>
          </form>
        </Card>
        <Card title={current?.name ?? 'Liste'} actions={<Checkbox label="Tâches faites" checked={showDone} onChange={setShowDone} />}>
          {listId !== null && (
            <form onSubmit={(e) => { e.preventDefault(); add.mutate(undefined) }} className="mb-4 flex gap-2">
              <input className="input" placeholder="Ajouter une tâche…" aria-label="Nouvelle tâche" required maxLength={200} value={quick}
                onChange={(e) => setQuick(e.target.value)} />
              <Button type="submit" variant="primary" disabled={add.isPending}>Ajouter</Button>
            </form>
          )}
          {items.isPending && listId !== null && <Loading />}
          {items.data?.length === 0 && <Empty>Rien à faire ici.</Empty>}
          <ul className="divide-y divide-line">
            {items.data?.map((item) => {
              const overdue = !item.done && item.dueAt !== null && item.dueAt !== undefined && item.dueAt.slice(0, 16) < now
              return (
                <li key={item.id} className="flex items-center gap-3 py-2">
                  <input type="checkbox" checked={item.done} onChange={() => toggle.mutate(item)} aria-label={`Fait : ${item.title}`}
                    className="h-5 w-5 accent-accent" />
                  <button type="button" onClick={() => setModal({ item })} className="flex flex-1 flex-col text-left">
                    <span className={item.done ? 'text-ink-3 line-through' : ''}>{item.title}</span>
                    <span className="flex flex-wrap gap-2 text-xs">
                      {item.dueAt && <span className={overdue ? 'font-semibold text-danger' : 'text-ink-3'}>
                        {overdue ? 'En retard · ' : ''}
                        {item.startAt ? `du ${formatDateTime(item.startAt)} au ${formatDateTime(item.dueAt)}` : formatDateTime(item.dueAt)}</span>}
                      {item.repeat && <span className="text-ink-3">{REPEATS[item.repeat]}</span>}
                    </span>
                  </button>
                  {item.priority === 'HIGH' && <Badge className="bg-danger-soft text-danger">Prioritaire</Badge>}
                  <MemberDot member={members.find((m) => m.id === item.assigneeId)} />
                </li>
              )
            })}
          </ul>
          {current && listId !== null && (
            <div className="mt-4 flex justify-between">
              <Button onClick={() => setModal({})}>Tâche détaillée</Button>
              <Button variant="danger" onClick={async () => {
                if (await confirm({ title: 'Supprimer la liste ?', message: `« ${current.name} » et ses tâches seront supprimées.` })) {
                  deleteList.mutate(listId)
                }
              }}>Supprimer la liste</Button>
            </div>
          )}
        </Card>
      </div>
      {modal && listId !== null && <ItemDialog item={modal.item} listId={listId} lists={lists.data} onClose={() => setModal(null)} />}
    </>
  )
}
