// One shopping list: rows grouped by shop and aisle, recipes it includes, cookable recipes, completion.
import { useState } from 'react'
import { del, post, put } from '../../lib/api.js'
import { formatDate, formatMoney, formatQuantity, parseMoney } from '../../lib/format.js'
import { AISLES, STORE_TYPES, UNITS } from '../../lib/labels.js'
import { navigate } from '../../lib/router.js'
import { useResource } from '../../lib/useResource.js'
import { useSession } from '../../lib/session.js'
import { useSubmit } from '../../lib/useSubmit.js'
import { today } from '../../lib/dates.js'
import ItemPicker from '../../components/ItemPicker.jsx'
import { findItem } from '../../lib/items.js'
import { Badge, Button, Card, Empty, ErrorMessage, Field, Loading, Modal, PageHeader, Select } from '../../components/ui.jsx'

/** Rows grouped by store type, then by aisle, keeping the server's order. */
function groupRows(rows) {
  const groups = []
  for (const row of rows) {
    let store = groups.find((g) => g.storeType === row.storeType)
    if (!store) groups.push(store = { storeType: row.storeType, aisles: [] })
    let aisle = store.aisles.find((a) => a.aisle === row.aisle)
    if (!aisle) store.aisles.push(aisle = { aisle: row.aisle, rows: [] })
    aisle.rows.push(row)
  }
  return groups
}

function AddItem({ listId, onChange }) {
  const items = useResource('/api/items')
  const [text, setText] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [unit, setUnit] = useState(null)
  const { busy, error, run, setError } = useSubmit()
  const item = findItem(items.data, text)
  const chosenUnit = unit ?? item?.unit ?? 'PIECE'
  const add = (e) => {
    e.preventDefault()
    if (!item) { setError(new Error('Choisissez un article du catalogue.')); return }
    run(async () => {
      onChange(await post(`/api/shopping/lists/${listId}/lines`,
        { itemId: item.id, quantity: Number(quantity.replace(',', '.')), unit: chosenUnit }))
      setText('')
      setQuantity('1')
      setUnit(null)
    })
  }
  return (
    <form onSubmit={add} className="space-y-2">
      <ItemPicker items={items.data} value={text} onChange={(value) => { setText(value); setUnit(null) }} />
      <div className="flex gap-2">
        <input className="input w-24" inputMode="decimal" value={quantity} onChange={(e) => setQuantity(e.target.value)} aria-label="Quantité" />
        <Select options={UNITS} value={chosenUnit} onChange={(v) => setUnit(v)} aria-label="Unité" />
        <Button type="submit" variant="primary" disabled={busy}>Ajouter</Button>
      </div>
      <p className="text-xs text-slate-500">Un article absent ? Ajoutez-le au <a href="#/catalogue" className="text-indigo-600">catalogue</a>.</p>
      <ErrorMessage error={error} />
    </form>
  )
}

function AddRecipe({ listId, onChange }) {
  const recipes = useResource('/api/recipes')
  const [recipeId, setRecipeId] = useState('')
  const [servings, setServings] = useState('4')
  const { busy, error, run } = useSubmit()
  const add = (e) => {
    e.preventDefault()
    run(async () => {
      onChange(await post(`/api/shopping/lists/${listId}/recipes`, { recipeId: Number(recipeId), servings: Number(servings) }))
      setRecipeId('')
    })
  }
  return (
    <form onSubmit={add} className="space-y-2">
      <Select options={Object.fromEntries((recipes.data ?? []).map((r) => [String(r.id), r.name]))} value={recipeId}
        placeholder="Choisir une recette" onChange={(v) => {
          setRecipeId(v ?? '')
          const recipe = (recipes.data ?? []).find((r) => String(r.id) === v)
          if (recipe) setServings(String(recipe.servings))
        }} />
      <div className="flex gap-2">
        <input className="input w-24" type="number" min="1" max="50" value={servings} onChange={(e) => setServings(e.target.value)}
          aria-label="Portions" />
        <span className="self-center text-sm text-slate-500">portions</span>
        <Button type="submit" variant="primary" className="ml-auto" disabled={!recipeId || busy}>Ajouter</Button>
      </div>
      <ErrorMessage error={error} />
    </form>
  )
}

function CompleteModal({ list, onClose, onDone }) {
  const { isParent } = useSession()
  const accounts = useResource(isParent ? '/api/accounting/accounts' : null)
  const [amount, setAmount] = useState('')
  const [paidOn, setPaidOn] = useState(today())
  const [accountId, setAccountId] = useState('')
  const { busy, error, run, setError } = useSubmit()
  const submit = (e) => {
    e.preventDefault()
    const cents = parseMoney(amount)
    if (cents === null || Number.isNaN(cents)) { setError(new Error('Saisissez le montant payé, par exemple 87,40.')); return }
    run(async () => {
      onDone(await post(`/api/shopping/lists/${list.id}/complete`, { amountPaidCents: cents, paidOn,
        accountId: accountId ? Number(accountId) : null }))
    })
  }
  return (
    <Modal title="Terminer les courses" onClose={onClose} footer={
      <Button variant="primary" type="submit" form="complete-form" disabled={busy}>Valider</Button>}>
      <form id="complete-form" onSubmit={submit} className="space-y-4">
        <p className="text-sm text-slate-600">
          Le montant est enregistré au budget comme une dépense à approuver par un parent. Estimation : {formatMoney(list.estimatedTotalCents)}.
        </p>
        <Field label="Montant payé (€)"><input className="input" inputMode="decimal" value={amount} required
          onChange={(e) => setAmount(e.target.value)} /></Field>
        <Field label="Date"><input className="input" type="date" value={paidOn} onChange={(e) => setPaidOn(e.target.value)} /></Field>
        {isParent && (
          <Field label="Payé depuis">
            <Select options={Object.fromEntries((accounts.data ?? []).map((a) => [String(a.id), a.name]))} value={accountId}
              placeholder="Non précisé" onChange={(v) => setAccountId(v ?? '')} />
          </Field>
        )}
        <ErrorMessage error={error} />
      </form>
    </Modal>
  )
}

function Suggestions({ rows }) {
  const ids = rows.map((r) => r.itemId).join(',')
  const suggestions = useResource(ids ? `/api/recipes/suggestions?itemIds=${ids}` : null)
  if (!suggestions.data?.length) return <Empty>Ajoutez des ingrédients pour voir les recettes possibles.</Empty>
  return (
    <ul className="space-y-2 text-sm">
      {suggestions.data.slice(0, 5).map((s) => (
        <li key={s.recipeId}>
          <a href={`#/recettes/${s.recipeId}`} className="flex justify-between gap-2 font-medium text-slate-800 hover:text-indigo-700">
            {s.name} <span className="tabular-nums text-slate-500">{Math.round(s.coverage * 100)} %</span>
          </a>
          {s.missing.length > 0 && <p className="text-xs text-slate-500">Manque : {s.missing.join(', ')}</p>}
        </li>
      ))}
    </ul>
  )
}

export default function ShoppingListPage({ params }) {
  const list = useResource(`/api/shopping/lists/${params.id}`)
  const [store, setStore] = useState(null)
  const [completing, setCompleting] = useState(false)
  const action = useSubmit()

  if (list.loading) return <Loading />
  if (list.error) return <ErrorMessage error={list.error} />
  const data = list.data
  const open = data.status === 'OPEN'
  const stores = [...new Set(data.rows.map((r) => r.storeType))]
  const rows = data.rows.filter((r) => !store || r.storeType === store)
  // Shows the server's version of the list after each change, and reloads it if the change failed.
  const update = (request) => action.run(async () => { list.setData(await request()); return true })
    .then((ok) => { if (!ok) list.reload() })
  // Ticking an item is shown at once: in a shop, the network can be slow.
  const toggle = (row) => {
    list.setData({ ...data, rows: data.rows.map((r) => (r.itemId === row.itemId ? { ...r, checked: !row.checked } : r)) })
    update(() => put(`/api/shopping/lists/${data.id}/items/${row.itemId}/checked`, { checked: !row.checked }))
  }

  return (
    <>
      <PageHeader title={data.name}
        subtitle={open
          ? `${data.plannedFor ? `Prévue ${formatDate(data.plannedFor)} · ` : ''}${data.rows.filter((r) => r.checked).length}/${data.rows.length} dans le panier · estimation ${formatMoney(data.estimatedTotalCents)}${data.unpricedRows ? ` (${data.unpricedRows} sans prix)` : ''}`
          : `Terminée ${formatDate(data.completedOn)} · payé ${formatMoney(data.amountPaidCents)}`}
        actions={(
          <>
            <Button variant="danger" onClick={() => action.run(async () => { await del(`/api/shopping/lists/${data.id}`); navigate('/courses') })}>
              Supprimer
            </Button>
            {open && <Button variant="primary" onClick={() => setCompleting(true)}>Terminer les courses</Button>}
          </>
        )} />
      <ErrorMessage error={action.error} />

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          {stores.length > 1 && (
            <div className="mb-4 flex flex-wrap gap-1">
              <button type="button" onClick={() => setStore(null)}
                className={`rounded-full px-3 py-1 text-sm ${!store ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-600'}`}>Tout</button>
              {stores.map((s) => (
                <button key={s} type="button" onClick={() => setStore(s)}
                  className={`rounded-full px-3 py-1 text-sm ${store === s ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-600'}`}>
                  {STORE_TYPES[s]}
                </button>
              ))}
            </div>
          )}
          {rows.length === 0 && <Empty>La liste est vide.</Empty>}
          {groupRows(rows).map((group) => (
            <section key={group.storeType} className="mb-5">
              <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">{STORE_TYPES[group.storeType]}</h2>
              {group.aisles.map((aisle) => (
                <div key={aisle.aisle} className="mb-3">
                  <p className="mb-1 text-xs font-medium text-slate-400">{AISLES[aisle.aisle]}</p>
                  <ul className="divide-y divide-slate-100 rounded-lg border border-slate-100">
                    {aisle.rows.map((row) => (
                      <li key={row.itemId} className="flex items-center gap-3 px-3 py-2">
                        <input type="checkbox" checked={row.checked} disabled={!open} aria-label={`Dans le panier : ${row.name}`}
                          className="h-5 w-5 rounded border-slate-300 text-emerald-600"
                          onChange={() => toggle(row)} />
                        <div className="flex-1">
                          <p className={row.checked ? 'text-slate-400 line-through' : 'text-slate-800'}>
                            {row.name} <span className="text-sm text-slate-500">{row.quantities.map(formatQuantity).join(' + ')}</span>
                          </p>
                          {(row.recipes.length > 0 || row.notes.length > 0) && (
                            <p className="text-xs text-slate-500">{[...row.recipes, ...row.notes].join(' · ')}</p>
                          )}
                        </div>
                        <span className="text-xs tabular-nums text-slate-500">{formatMoney(row.estimatedCents)}</span>
                        {open && <button type="button" aria-label={`Retirer ${row.name}`} className="text-slate-400 hover:text-red-600"
                          onClick={() => update(() => del(`/api/shopping/lists/${data.id}/items/${row.itemId}`))}>✕</button>}
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </section>
          ))}
        </Card>

        <div className="space-y-4">
          {open && <Card title="Ajouter un article"><AddItem listId={data.id} onChange={list.setData} /></Card>}
          <Card title="Recettes de la liste">
            {data.recipes.length === 0 && <p className="mb-3 text-sm text-slate-500">Les ingrédients d'une recette ajoutée sont calculés pour le nombre de portions.</p>}
            <ul className="mb-3 flex flex-wrap gap-2">
              {data.recipes.map((r) => (
                <li key={r.entryId}>
                  <Badge className="bg-amber-50 text-amber-900">
                    <a href={`#/recettes/${r.recipeId}?servings=${r.servings}`}>{r.name} · {r.servings} p.</a>
                    {open && <button type="button" className="ml-1.5 text-amber-700" aria-label={`Retirer ${r.name}`}
                      onClick={() => update(() => del(`/api/shopping/lists/${data.id}/recipes/${r.entryId}`))}>✕</button>}
                  </Badge>
                </li>
              ))}
            </ul>
            {open && <AddRecipe listId={data.id} onChange={list.setData} />}
          </Card>
          <Card title="Recettes possibles"><Suggestions rows={data.rows} /></Card>
        </div>
      </div>

      {completing && <CompleteModal list={data} onClose={() => setCompleting(false)}
        onDone={(updated) => { setCompleting(false); list.setData(updated) }} />}
    </>
  )
}
