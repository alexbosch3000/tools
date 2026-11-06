// One shopping list: rows grouped by shop and aisle, items per member (clothes), recipes, cookable recipes, completion.
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from '@tanstack/react-router'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useFeedback } from '../../app/useFeedback.ts'
import { queryClient } from '../../app/queryClient.ts'
import { useSession } from '../../app/useSession.ts'
import { useAction } from '../../app/useAction.ts'
import { formatDate, formatMoney, formatQuantity } from '../../lib/format.ts'
import { findItem } from '../../lib/items.ts'
import { AISLES, STORE_TYPES, UNITS } from '../../lib/labels.ts'
import { ItemPicker } from '../../components/ItemPicker.tsx'
import { Badge, Button, Card, Empty, ErrorMessage, Field, Loading, MemberDot, PageHeader, Select } from '../../components/ui.tsx'
import { CompleteDialog } from './CompleteDialog.tsx'
import { ListDialog } from './ListDialog.tsx'
import { RowDialog } from './RowDialog.tsx'
import { useOptimisticChecks } from './useOptimisticChecks.ts'

type Row = Schemas['ShoppingRow']
const AREAS = ['shopping', 'dashboard'] as const
const rowKey = (row: Row) => `${row.itemId}-${row.memberId ?? 0}`

/** Rows grouped by store type, then by aisle, keeping the server's order. */
function groupRows(rows: Row[]) {
  const groups: { storeType: Schemas['StoreType'], aisles: { aisle: Schemas['Aisle'], rows: Row[] }[] }[] = []
  for (const row of rows) {
    let store = groups.find((g) => g.storeType === row.storeType)
    if (!store) groups.push(store = { storeType: row.storeType, aisles: [] })
    let aisle = store.aisles.find((a) => a.aisle === row.aisle)
    if (!aisle) store.aisles.push(aisle = { aisle: row.aisle, rows: [] })
    aisle.rows.push(row)
  }
  return groups
}

/** Size to remind when buying clothes or shoes for a member. */
function sizeOf(member: Schemas['Member'] | undefined, row: Row): string | null {
  if (!member || row.storeType !== 'CLOTHING') return null
  const size = row.aisle === 'SHOES' ? member.shoeSize : member.clothingSize
  return size ? `${row.aisle === 'SHOES' ? 'pointure' : 'taille'} ${size}` : null
}

function AddItem({ list }: { list: Schemas['ShoppingList'] }) {
  const { members } = useSession()
  const items = useQuery({ queryKey: ['catalog', 'items'], queryFn: () => unwrap(api().GET('/api/items')) })
  const [text, setText] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [unit, setUnit] = useState<Schemas['Unit'] | ''>('')
  const [memberId, setMemberId] = useState('')
  const item = findItem(items.data, text)
  const chosenUnit = unit || item?.unit || 'PIECE'
  const add = useAction(() => {
    if (!item) throw new Error('Choisissez un article du catalogue.')
    return unwrap(api().POST('/api/shopping/lists/{id}/lines', { params: { path: { id: list.id } },
      body: { itemId: item.id, quantity: Number(quantity.replace(',', '.')), unit: chosenUnit, memberId: memberId ? Number(memberId) : null } }))
  }, { refresh: [...AREAS], inline: true, onSuccess: () => { setText(''); setQuantity('1'); setUnit(''); setMemberId('') } })
  return (
    <form onSubmit={(e) => { e.preventDefault(); add.mutate(undefined) }} className="space-y-2">
      <ItemPicker items={items.data} value={text} onChange={(v) => { setText(v); setUnit('') }} />
      <div className="flex gap-2">
        <input className="input w-20" inputMode="decimal" value={quantity} onChange={(e) => setQuantity(e.target.value)} aria-label="Quantité" />
        <Select options={UNITS} value={chosenUnit} onChange={setUnit} aria-label="Unité" />
      </div>
      {item?.storeType === 'CLOTHING' && (
        <Field label="Pour qui ?">
          <Select options={Object.fromEntries(members.map((m) => [String(m.id), m.name]))} value={memberId} placeholder="La maison"
            onChange={setMemberId} />
        </Field>
      )}
      <Button type="submit" variant="primary" className="w-full" disabled={add.isPending}>Ajouter</Button>
      <p className="text-xs text-ink-3">Un article absent ? Ajoutez-le au <Link to="/catalogue" className="text-accent">catalogue</Link>.</p>
      <ErrorMessage error={add.error} />
    </form>
  )
}

function AddRecipe({ list }: { list: Schemas['ShoppingList'] }) {
  const recipes = useQuery({ queryKey: ['recipes', ''], queryFn: () => unwrap(api().GET('/api/recipes')) })
  const [recipeId, setRecipeId] = useState('')
  const [servings, setServings] = useState('4')
  const [skipped, setSkipped] = useState<Set<number>>(new Set())
  const portions = Number(servings)
  const validServings = Number.isInteger(portions) && portions >= 1 && portions <= 50
  const detail = useQuery({ queryKey: ['recipes', 'detail', recipeId, portions], enabled: !!recipeId && validServings,
    queryFn: () => unwrap(api().GET('/api/recipes/{id}', { params: { path: { id: Number(recipeId) }, query: { servings: portions } } })) })
  const add = useAction(() => unwrap(api().POST('/api/shopping/lists/{id}/recipes', { params: { path: { id: list.id } },
    body: { recipeId: Number(recipeId), servings: portions, skippedItemIds: [...skipped] } })),
  { refresh: [...AREAS], inline: true, onSuccess: () => { setRecipeId(''); setSkipped(new Set()) } })
  const toggle = (itemId: number) => setSkipped((previous) => {
    const next = new Set(previous)
    if (!next.delete(itemId)) next.add(itemId)
    return next
  })
  return (
    <form onSubmit={(e) => { e.preventDefault(); add.mutate(undefined) }} className="space-y-2">
      <Select options={Object.fromEntries((recipes.data ?? []).map((r) => [String(r.id), r.name]))} value={recipeId} placeholder="Choisir une recette"
        aria-label="Recette" onChange={(v) => {
          setRecipeId(v)
          setSkipped(new Set())
          const recipe = recipes.data?.find((r) => String(r.id) === v)
          if (recipe) setServings(String(recipe.servings))
        }} />
      <div className="flex items-center gap-2">
        <input className="input w-20" type="number" min="1" max="50" value={servings} onChange={(e) => setServings(e.target.value)} aria-label="Portions" />
        <span className="text-sm text-ink-3">portions</span>
        <Button type="submit" variant="primary" className="ml-auto" disabled={!recipeId || !validServings || add.isPending}>Ajouter</Button>
      </div>
      {detail.data && (
        <fieldset className="space-y-1">
          <legend className="text-xs text-ink-3">Décochez ce que vous avez déjà.</legend>
          {detail.data.ingredients.map((ingredient) => (
            <label key={ingredient.itemId} className="flex items-center gap-2 text-sm">
              <input type="checkbox" className="h-4 w-4 accent-accent" checked={!skipped.has(ingredient.itemId)}
                onChange={() => toggle(ingredient.itemId)} />
              {ingredient.itemName} · {formatQuantity(ingredient.quantity)}
            </label>
          ))}
        </fieldset>
      )}
      <ErrorMessage error={add.error} />
    </form>
  )
}

function Suggestions({ rows }: { rows: Row[] }) {
  const itemIds = [...new Set(rows.map((r) => r.itemId))]
  const suggestions = useQuery({ queryKey: ['recipes', 'suggestions', itemIds], enabled: itemIds.length > 0,
    queryFn: () => unwrap(api().GET('/api/recipes/suggestions', { params: { query: { itemIds } } })) })
  if (!suggestions.data?.length) return <Empty>Ajoutez des ingrédients pour voir les recettes possibles.</Empty>
  return (
    <ul className="space-y-2 text-sm">
      {suggestions.data.slice(0, 5).map((s) => (
        <li key={s.recipeId}>
          <Link to="/recettes/$recipeId" params={{ recipeId: String(s.recipeId) }} search={{ servings: undefined }}
            className="flex justify-between gap-2 font-medium hover:text-accent">
            {s.name} <span className="tabular-nums text-ink-3">{Math.round(s.coverage * 100)} %</span>
          </Link>
          {s.missing.length > 0 && <p className="text-xs text-ink-3">Manque : {s.missing.join(', ')}</p>}
        </li>
      ))}
    </ul>
  )
}

export function ShoppingListPage() {
  const { listId } = useParams({ from: '/courses/$listId' })
  const id = Number(listId)
  const navigate = useNavigate()
  const { members } = useSession()
  const { confirm } = useFeedback()
  const key = ['shopping', 'list', id]
  const list = useQuery({ queryKey: key, queryFn: () => unwrap(api().GET('/api/shopping/lists/{id}', { params: { path: { id } } })) })
  const [store, setStore] = useState<Schemas['StoreType'] | null>(null)
  const [completing, setCompleting] = useState(false)
  const [editing, setEditing] = useState(false)
  const [editingRow, setEditingRow] = useState<Row | null>(null)

  const checks = useOptimisticChecks()
  const isChecked = (row: Row) => checks.isChecked(rowKey(row), row.checked)
  const setChecked = useAction(({ row, checked }: { row: Row, checked: boolean }) => unwrap(api().PUT('/api/shopping/lists/{id}/items/{itemId}/checked', {
    params: { path: { id, itemId: row.itemId }, query: { memberId: row.memberId ?? undefined } }, body: { checked } })),
  { refresh: ['dashboard'], onSuccess: (updated) => queryClient.setQueryData(key, updated) })
  const tick = (row: Row) => checks.toggle(rowKey(row), row.checked, (checked) => setChecked.mutateAsync({ row, checked }))
  const removeRow = useAction((row: Row) => unwrap(api().DELETE('/api/shopping/lists/{id}/items/{itemId}', {
    params: { path: { id, itemId: row.itemId }, query: { memberId: row.memberId ?? undefined } } })), { refresh: [...AREAS] })
  const removeRecipe = useAction((entryId: number) => unwrap(api().DELETE('/api/shopping/lists/{id}/recipes/{entryId}',
    { params: { path: { id, entryId } } })), { refresh: [...AREAS] })
  const removeList = useAction(() => unwrap(api().DELETE('/api/shopping/lists/{id}', { params: { path: { id } } })), {
    refresh: [...AREAS, 'planning'], success: 'Liste supprimée', onSuccess: () => navigate({ to: '/courses' }) })

  if (list.isPending) return <Loading lines={6} />
  if (list.isError) return <ErrorMessage error={list.error} />
  const data = list.data
  const open = data.status === 'OPEN'
  const checked = data.rows.filter(isChecked).length
  const stores = [...new Set(data.rows.map((r) => r.storeType))]
  const rows = data.rows.filter((r) => !store || r.storeType === store)

  return (
    <>
      <PageHeader title={data.name}
        subtitle={open
          ? `${data.plannedFor ? `Prévue ${formatDate(data.plannedFor)} · ` : ''}${checked}/${data.rows.length} dans le panier · estimation ${formatMoney(data.estimatedTotalCents)}${data.unpricedRows ? ` (${data.unpricedRows} sans prix)` : ''}`
          : `Terminée ${formatDate(data.completedOn)} · payé ${formatMoney(data.amountPaidCents)}`}
        actions={(
          <>
            {open && <Button onClick={() => setEditing(true)}>Modifier</Button>}
            <Button variant="danger" onClick={async () => {
              if (await confirm({ title: 'Supprimer la liste ?', message: `« ${data.name} » et ses articles seront supprimés.` })) removeList.mutate(undefined)
            }}>Supprimer</Button>
            {open && <Button variant="primary" onClick={() => setCompleting(true)} disabled={!data.rows.length}>Terminer les courses</Button>}
          </>
        )} />
      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          {stores.length > 1 && (
            <div className="mb-4 flex flex-wrap gap-1" role="group" aria-label="Filtrer par magasin">
              {[null, ...stores].map((s) => (
                <button key={s ?? 'all'} type="button" aria-pressed={store === s} onClick={() => setStore(s)}
                  className={`min-h-9 rounded-full px-3 text-sm ${store === s ? 'bg-accent text-accent-ink' : 'bg-surface-2 text-ink-2'}`}>
                  {s ? STORE_TYPES[s] : 'Tout'}
                </button>
              ))}
            </div>
          )}
          {rows.length === 0 && <Empty>La liste est vide.</Empty>}
          {groupRows(rows).map((group) => (
            <section key={group.storeType} className="mb-5">
              <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-ink-3">{STORE_TYPES[group.storeType]}</h2>
              {group.aisles.map((aisle) => (
                <div key={aisle.aisle} className="mb-3">
                  <h3 className="mb-1 text-xs font-medium text-ink-3">{AISLES[aisle.aisle]}</h3>
                  <ul className="divide-y divide-line rounded-lg border border-line">
                    {aisle.rows.map((row) => {
                      const member = members.find((m) => m.id === row.memberId)
                      const needed = row.quantities.map(formatQuantity).join(' + ')
                      const details = [member && `pour ${member.name}`, sizeOf(member, row), row.purchase && `besoin ${needed}`,
                        row.purchase?.note, ...row.recipes, ...row.notes].filter(Boolean)
                      return (
                        <li key={rowKey(row)} className="flex items-center gap-3 px-3 py-2">
                          <input type="checkbox" checked={isChecked(row)} disabled={!open} onChange={() => tick(row)}
                            aria-label={`Dans le panier : ${row.name}${member ? ` pour ${member.name}` : ''}`} className="h-5 w-5 accent-accent" />
                          <button type="button" disabled={!open} onClick={() => setEditingRow(row)}
                            className="min-w-0 flex-1 text-left enabled:hover:text-accent">
                            <span className={`block ${isChecked(row) ? 'text-ink-3 line-through' : ''}`}>
                              {row.name} <span className="text-sm text-ink-3">{row.purchase ? formatQuantity(row.purchase.quantity) : needed}</span>
                            </span>
                            {details.length > 0 && <span className="block truncate text-xs text-ink-3">{details.join(' · ')}</span>}
                          </button>
                          <MemberDot member={member} size="h-5 w-5 text-[10px]" />
                          <span className="text-xs tabular-nums text-ink-3">{formatMoney(row.estimatedCents)}</span>
                          {open && (
                            <button type="button" onClick={() => removeRow.mutate(row)} aria-label={`Retirer ${row.name}`}
                              className="rounded p-1 text-ink-3 hover:text-danger">✕</button>
                          )}
                        </li>
                      )
                    })}
                  </ul>
                </div>
              ))}
            </section>
          ))}
        </Card>
        <div className="space-y-4">
          {open && <Card title="Ajouter un article"><AddItem list={data} /></Card>}
          <Card title="Recettes de la liste">
            {data.recipes.length === 0 && <p className="mb-3 text-sm text-ink-3">Les ingrédients d'une recette ajoutée sont calculés pour le nombre de portions.</p>}
            <ul className="mb-3 flex flex-wrap gap-2">
              {data.recipes.map((r) => (
                <li key={r.entryId}>
                  <Badge className="bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-200">
                    <Link to="/recettes/$recipeId" params={{ recipeId: String(r.recipeId) }} search={{ servings: r.servings }}>{r.name} · {r.servings} p.</Link>
                    {open && <button type="button" className="ml-1.5" aria-label={`Retirer ${r.name}`} onClick={() => removeRecipe.mutate(r.entryId)}>✕</button>}
                  </Badge>
                </li>
              ))}
            </ul>
            {open && <AddRecipe list={data} />}
          </Card>
          <Card title="Recettes possibles"><Suggestions rows={data.rows} /></Card>
        </div>
      </div>
      {editingRow && <RowDialog key={rowKey(editingRow)} listId={id} row={editingRow} onClose={() => setEditingRow(null)} />}
      {editing && <ListDialog list={data} onClose={() => setEditing(false)} />}
      {completing && <CompleteDialog list={data} onClose={() => setCompleting(false)} />}
    </>
  )
}
