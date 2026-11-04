// Completes a shopping trip: the amount paid in each kind of shop becomes an expense in its own budget category.
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useSession } from '../../app/useSession.ts'
import { useAction } from '../../app/useAction.ts'
import { today } from '../../lib/dates.ts'
import { formatMoney, parseMoney } from '../../lib/format.ts'
import { STORE_TYPES } from '../../lib/labels.ts'
import { Dialog } from '../../components/Dialog.tsx'
import { Button, ErrorMessage, Field, Select } from '../../components/ui.tsx'

/** Estimated cost of the list per store type, in the order the rows come. */
function estimates(list: Schemas['ShoppingList']): [Schemas['StoreType'], number][] {
  const totals = new Map<Schemas['StoreType'], number>()
  for (const row of list.rows) totals.set(row.storeType, (totals.get(row.storeType) ?? 0) + (row.estimatedCents ?? 0))
  return totals.size ? [...totals.entries()] : [['SUPERMARKET', 0]]
}

export function CompleteDialog({ list, onClose }: { list: Schemas['ShoppingList'], onClose: () => void }) {
  const { isParent } = useSession()
  const stores = estimates(list)
  const [amounts, setAmounts] = useState<Record<string, string>>({})
  const [paidOn, setPaidOn] = useState(today())
  const [accountId, setAccountId] = useState('')
  const accounts = useQuery({ queryKey: ['accounting', 'accounts'], enabled: isParent,
    queryFn: () => unwrap(api().GET('/api/accounting/accounts')) })

  const complete = useAction(() => {
    const payments = stores.map(([storeType]) => ({ storeType, amountCents: parseMoney(amounts[storeType]) }))
    if (payments.some((p) => Number.isNaN(p.amountCents))) throw new Error('Montant invalide, par exemple 87,40.')
    const paid = payments.filter((p): p is { storeType: Schemas['StoreType'], amountCents: number } => (p.amountCents ?? 0) > 0)
    if (!paid.length) throw new Error('Saisissez au moins un montant payé.')
    return unwrap(api().POST('/api/shopping/lists/{id}/complete', { params: { path: { id: list.id } },
      body: { payments: paid, paidOn, accountId: accountId ? Number(accountId) : null } }))
  }, { refresh: ['shopping', 'dashboard', 'accounting', 'planning'], inline: true, onSuccess: onClose,
    success: 'Courses terminées : la dépense attend l’approbation d’un parent' })

  return (
    <Dialog title="Terminer les courses" onClose={onClose}
      footer={<Button variant="primary" type="submit" form="complete-form" disabled={complete.isPending}>Valider</Button>}>
      <form id="complete-form" onSubmit={(e) => { e.preventDefault(); complete.mutate(undefined) }} className="space-y-4">
        <p className="text-sm text-ink-2">
          Indiquez ce que vous avez payé par type de magasin : chaque montant est enregistré dans sa catégorie du budget
          (courses, vêtements, maison…).
        </p>
        <div className="grid gap-3 sm:grid-cols-2">
          {stores.map(([storeType, estimate]) => (
            <Field key={storeType} label={`${STORE_TYPES[storeType]} (€)`} hint={estimate ? `Estimation ${formatMoney(estimate)}` : undefined}>
              <input className="input" inputMode="decimal" value={amounts[storeType] ?? ''} placeholder="0,00"
                onChange={(e) => setAmounts({ ...amounts, [storeType]: e.target.value })} />
            </Field>
          ))}
        </div>
        <Field label="Date"><input className="input" type="date" value={paidOn} onChange={(e) => setPaidOn(e.target.value)} /></Field>
        {isParent && (
          <Field label="Payé depuis">
            <Select options={Object.fromEntries((accounts.data ?? []).map((a) => [String(a.id), a.name]))} value={accountId}
              placeholder="Non précisé" onChange={setAccountId} />
          </Field>
        )}
        <ErrorMessage error={complete.error} />
      </form>
    </Dialog>
  )
}
