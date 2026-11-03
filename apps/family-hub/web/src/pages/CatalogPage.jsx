// Catalog of purchasable items: goods and ingredients, with shop, aisle, unit and estimated price.
import { useState } from 'react'
import { del, post, put } from '../lib/api.js'
import { centsToInput, formatMoney, parseMoney } from '../lib/format.js'
import { AISLES, ITEM_KINDS, STORE_TYPES, UNITS } from '../lib/labels.js'
import { useResource } from '../lib/useResource.js'
import { useSubmit } from '../lib/useSubmit.js'
import { Badge, Button, Card, Empty, ErrorMessage, Field, Loading, Modal, PageHeader, Select } from '../components/ui.jsx'

function ItemModal({ item, onClose, onSaved }) {
  const [form, setForm] = useState({
    name: item?.name ?? '', kind: item?.kind ?? 'INGREDIENT', storeType: item?.storeType ?? 'SUPERMARKET',
    aisle: item?.aisle ?? 'GROCERY', unit: item?.unit ?? 'PIECE', price: centsToInput(item?.unitPriceCents),
  })
  const { busy, error, run, setError } = useSubmit()
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const save = (e) => {
    e.preventDefault()
    const unitPriceCents = parseMoney(form.price)
    if (Number.isNaN(unitPriceCents)) { setError(new Error('Prix invalide, par exemple 2,50.')); return }
    run(async () => {
      const body = { name: form.name, kind: form.kind, storeType: form.storeType, aisle: form.aisle, unit: form.unit, unitPriceCents }
      if (item) await put(`/api/items/${item.id}`, body)
      else await post('/api/items', body)
      onSaved()
    })
  }
  return (
    <Modal title={item ? "Modifier l'article" : 'Nouvel article'} onClose={onClose} footer={(
      <>
        {item && <Button variant="danger" className="mr-auto" disabled={busy}
          onClick={() => run(async () => { await del(`/api/items/${item.id}`); onSaved() })}>Supprimer</Button>}
        <Button variant="primary" type="submit" form="item-form" disabled={busy}>Enregistrer</Button>
      </>
    )}>
      <form id="item-form" onSubmit={save} className="grid grid-cols-2 gap-3">
        <Field label="Nom" className="col-span-2"><input className="input" required maxLength={80} value={form.name}
          onChange={(e) => set({ name: e.target.value })} /></Field>
        <Field label="Type" hint="Un ingrédient peut entrer dans une recette.">
          <Select options={ITEM_KINDS} value={form.kind} onChange={(v) => set({ kind: v })} /></Field>
        <Field label="Magasin"><Select options={STORE_TYPES} value={form.storeType} onChange={(v) => set({ storeType: v })} /></Field>
        <Field label="Rayon"><Select options={AISLES} value={form.aisle} onChange={(v) => set({ aisle: v })} /></Field>
        <Field label="Unité"><Select options={UNITS} value={form.unit} onChange={(v) => set({ unit: v })} /></Field>
        <Field label="Prix estimé par unité (€)" className="col-span-2"><input className="input" inputMode="decimal"
          value={form.price} onChange={(e) => set({ price: e.target.value })} /></Field>
        <div className="col-span-2"><ErrorMessage error={error} /></div>
      </form>
    </Modal>
  )
}

export default function CatalogPage() {
  const [kind, setKind] = useState(null)
  const [query, setQuery] = useState('')
  const [modal, setModal] = useState(null)
  const params = new URLSearchParams()
  if (kind) params.set('kind', kind)
  if (query.trim()) params.set('q', query.trim())
  const items = useResource(`/api/items?${params}`)

  return (
    <>
      <PageHeader title="Catalogue" subtitle="Tout ce qui s'achète : alimentation, entretien, bricolage, jardin."
        actions={<Button variant="primary" onClick={() => setModal({})}>Nouvel article</Button>} />
      <div className="mb-4 flex flex-wrap gap-2">
        <input className="input max-w-xs" placeholder="Rechercher…" value={query} onChange={(e) => setQuery(e.target.value)} />
        <div className="w-44"><Select options={ITEM_KINDS} value={kind} placeholder="Tous les types" onChange={setKind} /></div>
      </div>
      <Card className="overflow-x-auto p-0">
        {items.loading && !items.data && <Loading />}
        <ErrorMessage error={items.error} />
        {items.data?.length === 0 && <Empty>Aucun article.</Empty>}
        {items.data?.length > 0 && (
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 text-xs uppercase text-slate-500">
              <tr><th className="px-4 py-2">Article</th><th className="px-4 py-2">Magasin</th><th className="px-4 py-2">Rayon</th>
                <th className="px-4 py-2 text-right">Prix estimé</th></tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {items.data.map((i) => (
                <tr key={i.id} className="cursor-pointer hover:bg-slate-50" onClick={() => setModal({ item: i })}>
                  <td className="px-4 py-2 text-slate-800">{i.name} {i.kind === 'INGREDIENT' && <Badge className="ml-1 bg-amber-50 text-amber-800">ingrédient</Badge>}</td>
                  <td className="px-4 py-2 text-slate-600">{STORE_TYPES[i.storeType]}</td>
                  <td className="px-4 py-2 text-slate-600">{AISLES[i.aisle]}</td>
                  <td className="px-4 py-2 text-right tabular-nums text-slate-600">
                    {i.unitPriceCents === null ? '—' : `${formatMoney(i.unitPriceCents)} / ${UNITS[i.unit]}`}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
      {modal && <ItemModal item={modal.item} onClose={() => setModal(null)} onSaved={() => { setModal(null); items.reload() }} />}
    </>
  )
}
