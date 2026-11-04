// Catalog of purchasable items: food, household, clothing, DIY and garden, with shop, aisle, unit and estimated price.
import { useDeferredValue, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useFeedback } from '../../app/useFeedback.ts'
import { useAction } from '../../app/useAction.ts'
import { centsToInput, formatMoney, parseMoney } from '../../lib/format.ts'
import { AISLES, ITEM_KINDS, STORE_TYPES, UNITS } from '../../lib/labels.ts'
import { Dialog } from '../../components/Dialog.tsx'
import { Badge, Button, Card, Empty, ErrorMessage, Field, Loading, PageHeader, Select } from '../../components/ui.tsx'

function ItemDialog({ item, onClose }: { item?: Schemas['Item'], onClose: () => void }) {
  const { confirm } = useFeedback()
  const [form, setForm] = useState({
    name: item?.name ?? '', kind: item?.kind ?? 'INGREDIENT' as Schemas['ItemKind'], storeType: item?.storeType ?? 'SUPERMARKET' as Schemas['StoreType'],
    aisle: item?.aisle ?? 'GROCERY' as Schemas['Aisle'], unit: item?.unit ?? 'PIECE' as Schemas['Unit'], price: centsToInput(item?.unitPriceCents),
  })
  const set = (patch: Partial<typeof form>) => setForm((f) => ({ ...f, ...patch }))
  const save = useAction(() => {
    const unitPriceCents = parseMoney(form.price)
    if (Number.isNaN(unitPriceCents)) throw new Error('Prix invalide, par exemple 2,50.')
    const body = { name: form.name, kind: form.kind, storeType: form.storeType, aisle: form.aisle, unit: form.unit, unitPriceCents }
    return item ? unwrap(api().PUT('/api/items/{id}', { params: { path: { id: item.id } }, body })) : unwrap(api().POST('/api/items', { body }))
  }, { refresh: ['catalog', 'shopping', 'recipes'], inline: true, success: 'Article enregistré', onSuccess: onClose })
  const remove = useAction(() => unwrap(api().DELETE('/api/items/{id}', { params: { path: { id: item!.id } } })),
    { refresh: ['catalog'], success: 'Article supprimé', onSuccess: onClose })
  return (
    <Dialog title={item ? "Modifier l'article" : 'Nouvel article'} onClose={onClose} footer={(
      <>
        {item && <Button variant="danger" className="mr-auto" onClick={async () => {
          if (await confirm({ title: "Supprimer l'article ?", message: `« ${item.name} » sera retiré du catalogue.` })) remove.mutate(undefined)
        }}>Supprimer</Button>}
        <Button variant="primary" type="submit" form="item-form" disabled={save.isPending}>Enregistrer</Button>
      </>
    )}>
      <form id="item-form" onSubmit={(e) => { e.preventDefault(); save.mutate(undefined) }} className="grid grid-cols-2 gap-3">
        <Field label="Nom" className="col-span-2"><input className="input" required maxLength={80} value={form.name} onChange={(e) => set({ name: e.target.value })} /></Field>
        <Field label="Type" hint="Un ingrédient peut entrer dans une recette."><Select options={ITEM_KINDS} value={form.kind} onChange={(v) => set({ kind: v || 'GOODS' })} /></Field>
        <Field label="Magasin"><Select options={STORE_TYPES} value={form.storeType} onChange={(v) => set({ storeType: v || 'OTHER' })} /></Field>
        <Field label="Rayon"><Select options={AISLES} value={form.aisle} onChange={(v) => set({ aisle: v || 'OTHER' })} /></Field>
        <Field label="Unité"><Select options={UNITS} value={form.unit} onChange={(v) => set({ unit: v || 'PIECE' })} /></Field>
        <Field label="Prix estimé par unité (€)" className="col-span-2"><input className="input" inputMode="decimal" value={form.price} onChange={(e) => set({ price: e.target.value })} /></Field>
        <div className="col-span-2"><ErrorMessage error={save.error ?? remove.error} /></div>
      </form>
    </Dialog>
  )
}

export function CatalogPage() {
  const [kind, setKind] = useState<Schemas['ItemKind'] | ''>('')
  const [store, setStore] = useState<Schemas['StoreType'] | ''>('')
  const [query, setQuery] = useState('')
  const q = useDeferredValue(query.trim())
  const [modal, setModal] = useState<{ item?: Schemas['Item'] } | null>(null)
  const items = useQuery({ queryKey: ['catalog', 'items', kind, q], placeholderData: (previous) => previous,
    queryFn: () => unwrap(api().GET('/api/items', { params: { query: { ...(kind ? { kind } : {}), ...(q ? { q } : {}) } } })) })
  const shown = (items.data ?? []).filter((i) => !store || i.storeType === store)
  return (
    <>
      <PageHeader title="Catalogue" subtitle="Tout ce qui s'achète : alimentation, entretien, vêtements, bricolage, jardin."
        actions={<Button variant="primary" onClick={() => setModal({})}>Nouvel article</Button>} />
      <div className="mb-4 flex flex-wrap gap-2">
        <input type="search" className="input max-w-xs" placeholder="Rechercher…" aria-label="Rechercher un article" value={query} onChange={(e) => setQuery(e.target.value)} />
        <div className="w-40"><Select options={ITEM_KINDS} value={kind} placeholder="Tous les types" aria-label="Type" onChange={setKind} /></div>
        <div className="w-44"><Select options={STORE_TYPES} value={store} placeholder="Tous les magasins" aria-label="Magasin" onChange={setStore} /></div>
      </div>
      <Card className="overflow-x-auto p-0">
        {items.isPending && <Loading />}
        <ErrorMessage error={items.error} />
        {items.isSuccess && shown.length === 0 && <Empty>Aucun article.</Empty>}
        {shown.length > 0 && (
          <table className="w-full text-left text-sm">
            <thead className="border-b border-line text-xs uppercase text-ink-3">
              <tr><th className="px-4 py-2 font-medium">Article</th><th className="px-4 py-2 font-medium">Magasin</th>
                <th className="px-4 py-2 font-medium">Rayon</th><th className="px-4 py-2 text-right font-medium">Prix estimé</th></tr>
            </thead>
            <tbody className="divide-y divide-line">
              {shown.map((i) => (
                <tr key={i.id} className="hover:bg-surface-2">
                  <td className="px-4 py-2">
                    <button type="button" className="text-left hover:text-accent" onClick={() => setModal({ item: i })}>{i.name}</button>
                    {i.kind === 'INGREDIENT' && <Badge className="ml-2 bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-200">ingrédient</Badge>}
                  </td>
                  <td className="px-4 py-2 text-ink-2">{STORE_TYPES[i.storeType]}</td>
                  <td className="px-4 py-2 text-ink-2">{AISLES[i.aisle]}</td>
                  <td className="px-4 py-2 text-right tabular-nums text-ink-2">
                    {i.unitPriceCents === null || i.unitPriceCents === undefined ? '—' : `${formatMoney(i.unitPriceCents)} / ${UNITS[i.unit]}`}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
      {modal && <ItemDialog item={modal.item} onClose={() => setModal(null)} />}
    </>
  )
}
