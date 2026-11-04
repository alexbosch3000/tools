// Shopping lists: those in progress, a form to start one, and the last completed trips.
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from '@tanstack/react-router'
import { api, unwrap } from '../../api/client.ts'
import { useAction } from '../../app/useAction.ts'
import { formatDate, formatMoney } from '../../lib/format.ts'
import { Button, Card, Empty, ErrorMessage, Field, Loading, PageHeader } from '../../components/ui.tsx'

export function ShoppingListsPage() {
  const navigate = useNavigate()
  const open = useQuery({ queryKey: ['shopping', 'lists', 'OPEN'],
    queryFn: () => unwrap(api().GET('/api/shopping/lists', { params: { query: { status: 'OPEN' } } })) })
  const done = useQuery({ queryKey: ['shopping', 'lists', 'DONE'],
    queryFn: () => unwrap(api().GET('/api/shopping/lists', { params: { query: { status: 'DONE' } } })) })
  const [form, setForm] = useState({ name: '', plannedFor: '' })
  const create = useAction(() => unwrap(api().POST('/api/shopping/lists', { body: { name: form.name, plannedFor: form.plannedFor || null } })), {
    refresh: ['shopping', 'dashboard'], inline: true,
    onSuccess: (list) => navigate({ to: '/courses/$listId', params: { listId: String(list.id) } }),
  })

  return (
    <>
      <PageHeader title="Courses" subtitle="Alimentation, vêtements, bricolage, jardin : à la main, depuis les recettes ou depuis les repas planifiés." />
      <div className="grid gap-4 lg:grid-cols-3">
        <div className="space-y-3 lg:col-span-2">
          {open.isPending && <Loading />}
          <ErrorMessage error={open.error} />
          {open.data?.length === 0 && <Card><Empty>Aucune liste en cours.</Empty></Card>}
          {open.data?.map((l) => (
            <Link key={l.id} to="/courses/$listId" params={{ listId: String(l.id) }} className="card block hover:border-accent">
              <div className="flex items-center justify-between gap-2">
                <h2 className="font-semibold">{l.name}</h2>
                <span className="text-sm text-ink-3">{l.checkedCount}/{l.itemCount} articles</span>
              </div>
              <div className="mt-2 h-1.5 rounded-full bg-surface-2" role="progressbar" aria-valuemin={0} aria-valuemax={l.itemCount}
                aria-valuenow={l.checkedCount} aria-label="Articles dans le panier">
                <div className="h-full rounded-full bg-success" style={{ width: `${l.itemCount ? (l.checkedCount / l.itemCount) * 100 : 0}%` }} />
              </div>
              <p className="mt-2 text-sm text-ink-3">{l.plannedFor ? `Prévue ${formatDate(l.plannedFor)} · ` : ''}Estimation {formatMoney(l.estimatedTotalCents)}</p>
            </Link>
          ))}
        </div>
        <div className="space-y-4">
          <Card title="Nouvelle liste">
            <form onSubmit={(e) => { e.preventDefault(); create.mutate(undefined) }} className="space-y-3">
              <Field label="Nom"><input className="input" required maxLength={80} value={form.name} placeholder="Courses du samedi"
                onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
              <Field label="Prévue le"><input className="input" type="date" value={form.plannedFor}
                onChange={(e) => setForm({ ...form, plannedFor: e.target.value })} /></Field>
              <ErrorMessage error={create.error} />
              <Button type="submit" variant="primary" disabled={create.isPending}>Créer</Button>
            </form>
          </Card>
          <Card title="Terminées récemment">
            {done.data?.length === 0 && <Empty>Pas encore de courses terminées.</Empty>}
            <ul className="space-y-2 text-sm">
              {done.data?.map((l) => (
                <li key={l.id}>
                  <Link to="/courses/$listId" params={{ listId: String(l.id) }} className="flex justify-between gap-2 hover:text-accent">
                    <span className="truncate">{formatDate(l.completedOn)} · {l.name}</span>
                    <span className="tabular-nums">{formatMoney(l.amountPaidCents)}</span>
                  </Link>
                </li>
              ))}
            </ul>
          </Card>
        </div>
      </div>
    </>
  )
}
