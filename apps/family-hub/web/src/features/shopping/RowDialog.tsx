// Edits what will be bought for a shopping row (quantity, unit, estimated price) when it differs from what is needed.
import { useState } from 'react'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useAction } from '../../app/useAction.ts'
import { centsToInput, formatQuantity, parseMoney } from '../../lib/format.ts'
import { UNITS } from '../../lib/labels.ts'
import { Dialog } from '../../components/Dialog.tsx'
import { Button, ErrorMessage, Field, Select } from '../../components/ui.tsx'

type Row = Schemas['ShoppingRow']

interface Props {
  listId: number
  row: Row
  onClose: () => void
}

export function RowDialog({ listId, row, onClose }: Props) {
  const initial = row.purchase?.quantity ?? row.quantities[0]
  const [quantity, setQuantity] = useState(initial ? String(initial.amount).replace('.', ',') : '1')
  const [unit, setUnit] = useState<Schemas['Unit']>(initial?.unit ?? 'PIECE')
  const [price, setPrice] = useState(centsToInput(row.purchase?.priceCents))
  const [note, setNote] = useState(row.purchase?.note ?? '')
  const path = { params: { path: { id: listId, itemId: row.itemId }, query: { memberId: row.memberId ?? undefined } } }
  const options = { refresh: ['shopping', 'dashboard'] as const, inline: true, onSuccess: onClose }

  const save = useAction(() => {
    const amount = Number(quantity.replace(',', '.'))
    if (!(amount > 0)) throw new Error('Quantité invalide.')
    const priceCents = parseMoney(price)
    if (Number.isNaN(priceCents)) throw new Error('Prix invalide, par exemple 1,20.')
    return unwrap(api().PUT('/api/shopping/lists/{id}/items/{itemId}/purchase', { ...path,
      body: { quantity: amount, unit, priceCents, note: note.trim() || null } }))
  }, { ...options, refresh: [...options.refresh], success: 'Article modifié' })
  const reset = useAction(() => unwrap(api().DELETE('/api/shopping/lists/{id}/items/{itemId}/purchase', path)),
    { ...options, refresh: [...options.refresh] })

  return (
    <Dialog title="Modifier l'article" onClose={onClose}
      footer={(
        <>
          {row.purchase && <Button onClick={() => reset.mutate(undefined)} disabled={reset.isPending}>Revenir au calcul</Button>}
          <Button variant="primary" type="submit" form="row-form" disabled={save.isPending}>Enregistrer</Button>
        </>
      )}>
      <form id="row-form" onSubmit={(e) => { e.preventDefault(); save.mutate(undefined) }} className="space-y-4">
        <p className="text-sm text-ink-2">
          <span className="font-medium">{row.name}</span> · Besoin : {row.quantities.map(formatQuantity).join(' + ')}
          {row.recipes.length > 0 && ` (${row.recipes.join(', ')})`}
        </p>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Quantité à acheter">
            <input className="input" inputMode="decimal" value={quantity} onChange={(e) => setQuantity(e.target.value)} />
          </Field>
          <Field label="Unité">
            <Select options={UNITS} value={unit} onChange={(v) => v && setUnit(v)} />
          </Field>
        </div>
        <Field label="Prix estimé (€)" hint="Laissez vide pour utiliser le prix du catalogue.">
          <input className="input" inputMode="decimal" value={price} placeholder="0,00" onChange={(e) => setPrice(e.target.value)} />
        </Field>
        <Field label="Note">
          <input className="input" maxLength={120} value={note} onChange={(e) => setNote(e.target.value)} />
        </Field>
        <ErrorMessage error={save.error ?? reset.error} />
      </form>
    </Dialog>
  )
}
