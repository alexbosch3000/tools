// Shopping lists: those in progress, a form to start one, and the last completed trips.
import { useState } from 'react'
import { post } from '../../lib/api.js'
import { formatDate, formatMoney } from '../../lib/format.js'
import { navigate } from '../../lib/router.js'
import { useResource } from '../../lib/useResource.js'
import { useSubmit } from '../../lib/useSubmit.js'
import { Button, Card, Empty, ErrorMessage, Field, Loading, PageHeader } from '../../components/ui.jsx'

export default function ShoppingListsPage() {
  const open = useResource('/api/shopping/lists?status=OPEN')
  const done = useResource('/api/shopping/lists?status=DONE')
  const [form, setForm] = useState({ name: '', plannedFor: '' })
  const { busy, error, run } = useSubmit()

  const create = (e) => {
    e.preventDefault()
    run(async () => {
      const list = await post('/api/shopping/lists', { name: form.name, plannedFor: form.plannedFor || null })
      navigate(`/courses/${list.id}`)
    })
  }

  return (
    <>
      <PageHeader title="Courses" subtitle="Les listes se remplissent à la main, depuis les recettes ou depuis les repas planifiés." />
      <div className="grid gap-4 lg:grid-cols-3">
        <div className="space-y-3 lg:col-span-2">
          {open.loading && <Loading />}
          <ErrorMessage error={open.error} />
          {open.data?.length === 0 && <Card><Empty>Aucune liste en cours.</Empty></Card>}
          {open.data?.map((l) => (
            <a key={l.id} href={`#/courses/${l.id}`} className="card block hover:border-indigo-300">
              <div className="flex items-center justify-between gap-2">
                <h2 className="font-semibold text-slate-800">{l.name}</h2>
                <span className="text-sm text-slate-500">{l.checkedCount}/{l.itemCount} articles</span>
              </div>
              <div className="mt-2 h-1.5 rounded-full bg-slate-100">
                <div className="h-full rounded-full bg-emerald-500"
                  style={{ width: `${l.itemCount ? (l.checkedCount / l.itemCount) * 100 : 0}%` }} />
              </div>
              <p className="mt-2 text-sm text-slate-500">
                {l.plannedFor ? `Prévue ${formatDate(l.plannedFor)} · ` : ''}Estimation {formatMoney(l.estimatedTotalCents)}
              </p>
            </a>
          ))}
        </div>
        <div className="space-y-4">
          <Card title="Nouvelle liste">
            <form onSubmit={create} className="space-y-3">
              <Field label="Nom"><input className="input" required maxLength={80} value={form.name}
                placeholder="Courses du samedi" onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
              <Field label="Prévue le"><input className="input" type="date" value={form.plannedFor}
                onChange={(e) => setForm({ ...form, plannedFor: e.target.value })} /></Field>
              <ErrorMessage error={error} />
              <Button type="submit" variant="primary" disabled={busy}>Créer</Button>
            </form>
          </Card>
          <Card title="Terminées récemment">
            {done.data?.length === 0 && <Empty>Pas encore de courses terminées.</Empty>}
            <ul className="space-y-2 text-sm">
              {done.data?.map((l) => (
                <li key={l.id}>
                  <a href={`#/courses/${l.id}`} className="flex justify-between gap-2 text-slate-700 hover:text-indigo-700">
                    <span className="truncate">{formatDate(l.completedOn)} · {l.name}</span>
                    <span className="tabular-nums">{formatMoney(l.amountPaidCents)}</span>
                  </a>
                </li>
              ))}
            </ul>
          </Card>
        </div>
      </div>
    </>
  )
}
