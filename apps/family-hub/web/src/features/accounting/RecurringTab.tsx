// Recurring transactions: utilities, insurance, subscriptions, monthly contributions.
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useFeedback } from '../../app/useFeedback.ts'
import { useAction } from '../../app/useAction.ts'
import { today } from '../../lib/dates.ts'
import { centsToInput, formatMoney, parseMoney } from '../../lib/format.ts'
import { PERIODS } from '../../lib/labels.ts'
import { Dialog } from '../../components/Dialog.tsx'
import { Badge, Button, Card, Checkbox, Empty, ErrorMessage, Field, Loading, Select } from '../../components/ui.tsx'
import { ACCOUNTING_AREAS } from './tabs.ts'

/** Typical household commitments, to fill the form in one click. */
const PRESETS: [string, string][] = [
  ['energie', 'Électricité / gaz'], ['eau', 'Eau'], ['telecom', 'Internet / mobile'], ['assurances', 'Assurance'],
  ['logement', 'Loyer / crédit'], ['apport-mensuel', 'Apport mensuel'],
]

function RecurringDialog({ recurring, categories, accounts, onClose }:
  { recurring?: Schemas['RecurringTransaction'], categories: Schemas['Category'][], accounts: Schemas['Account'][], onClose: () => void }) {
  const { confirm } = useFeedback()
  const [form, setForm] = useState({
    label: recurring?.label ?? '', categoryCode: recurring?.categoryCode ?? 'energie', supplier: recurring?.supplier ?? '',
    amount: centsToInput(recurring?.amountCents), period: recurring?.period ?? 'MONTHLY' as Schemas['Period'],
    dayOfMonth: String(recurring?.dayOfMonth ?? 5), startDate: recurring?.startDate ?? today(), endDate: recurring?.endDate ?? '',
    accountId: recurring?.accountId ? String(recurring.accountId) : '', active: recurring?.active ?? true,
  })
  const set = (patch: Partial<typeof form>) => setForm((f) => ({ ...f, ...patch }))
  const save = useAction(() => {
    const amountCents = parseMoney(form.amount)
    if (amountCents === null || Number.isNaN(amountCents)) throw new Error('Montant invalide.')
    const body: Schemas['RecurringRequest'] = { label: form.label, categoryCode: form.categoryCode, supplier: form.supplier, amountCents,
      period: form.period, dayOfMonth: Number(form.dayOfMonth), startDate: form.startDate, endDate: form.endDate || null,
      accountId: form.accountId ? Number(form.accountId) : null, active: form.active }
    return recurring
      ? unwrap(api().PUT('/api/accounting/recurring/{id}', { params: { path: { id: recurring.id } }, body }))
      : unwrap(api().POST('/api/accounting/recurring', { body }))
  }, { refresh: [...ACCOUNTING_AREAS], inline: true, success: 'Échéance enregistrée', onSuccess: onClose })
  const remove = useAction(() => unwrap(api().DELETE('/api/accounting/recurring/{id}', { params: { path: { id: recurring!.id } } })),
    { refresh: [...ACCOUNTING_AREAS], success: 'Échéance supprimée', onSuccess: onClose })
  return (
    <Dialog title={recurring ? "Modifier l'échéance" : 'Nouvelle échéance'} onClose={onClose} footer={(
      <>
        {recurring && <Button variant="danger" className="mr-auto" onClick={async () => {
          if (await confirm({ title: "Supprimer l'échéance ?", message: 'Les opérations prévues non approuvées seront retirées.' })) remove.mutate(undefined)
        }}>Supprimer</Button>}
        <Button variant="primary" type="submit" form="recurring-form" disabled={save.isPending}>Enregistrer</Button>
      </>
    )}>
      <form id="recurring-form" onSubmit={(e) => { e.preventDefault(); save.mutate(undefined) }} className="grid grid-cols-2 gap-3">
        {!recurring && (
          <div className="col-span-2 flex flex-wrap gap-1">
            {PRESETS.map(([code, label]) => (
              <button key={code} type="button" onClick={() => set({ categoryCode: code, label })}
                className="min-h-8 rounded-full bg-surface-2 px-3 text-xs hover:bg-accent-soft">{label}</button>
            ))}
          </div>
        )}
        <Field label="Libellé" className="col-span-2"><input className="input" required maxLength={120} value={form.label} onChange={(e) => set({ label: e.target.value })} /></Field>
        <Field label="Fournisseur"><input className="input" maxLength={80} value={form.supplier} onChange={(e) => set({ supplier: e.target.value })} /></Field>
        <Field label="Catégorie"><Select options={Object.fromEntries(categories.map((c) => [c.code, c.name]))} value={form.categoryCode}
          onChange={(v) => set({ categoryCode: v })} /></Field>
        <Field label="Montant (€)"><input className="input" inputMode="decimal" required value={form.amount} onChange={(e) => set({ amount: e.target.value })} /></Field>
        <Field label="Fréquence"><Select options={PERIODS} value={form.period} onChange={(v) => set({ period: v || 'MONTHLY' })} /></Field>
        <Field label="Jour du mois"><input className="input" type="number" min="1" max="31" required value={form.dayOfMonth} onChange={(e) => set({ dayOfMonth: e.target.value })} /></Field>
        <Field label="Compte"><Select options={Object.fromEntries(accounts.map((a) => [String(a.id), a.name]))} value={form.accountId}
          placeholder="Non précisé" onChange={(v) => set({ accountId: v })} /></Field>
        <Field label="Début"><input className="input" type="date" required value={form.startDate} onChange={(e) => set({ startDate: e.target.value })} /></Field>
        <Field label="Fin (renouvellement)"><input className="input" type="date" value={form.endDate} onChange={(e) => set({ endDate: e.target.value })} /></Field>
        <div className="col-span-2"><Checkbox label="Active" checked={form.active} onChange={(active) => set({ active })} /></div>
        <div className="col-span-2"><ErrorMessage error={save.error ?? remove.error} /></div>
      </form>
    </Dialog>
  )
}

export function RecurringTab({ categories, accounts }: { categories: Schemas['Category'][], accounts: Schemas['Account'][] }) {
  const recurring = useQuery({ queryKey: ['accounting', 'recurring'], queryFn: () => unwrap(api().GET('/api/accounting/recurring')) })
  const [modal, setModal] = useState<{ recurring?: Schemas['RecurringTransaction'] } | null>(null)
  const byCode = new Map(categories.map((c) => [c.code, c]))
  return (
    <Card title="Échéances récurrentes" actions={<Button variant="primary" onClick={() => setModal({})}>Nouvelle échéance</Button>}>
      <p className="mb-3 text-sm text-ink-3">Chaque échéance crée une opération prévue pour le mois en cours et le suivant, à approuver une fois prélevée.</p>
      {recurring.isPending && <Loading />}
      <ErrorMessage error={recurring.error} />
      {recurring.data?.length === 0 && <Empty>Aucune échéance : ajoutez l'électricité, l'eau, internet, les assurances…</Empty>}
      <ul className="divide-y divide-line">
        {recurring.data?.map((r) => (
          <li key={r.id}>
            <button type="button" onClick={() => setModal({ recurring: r })} className="flex w-full items-center gap-3 py-2 text-left">
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm">{r.label}{r.supplier ? ` · ${r.supplier}` : ''}</span>
                <span className="text-xs text-ink-3">{byCode.get(r.categoryCode)?.name} · {PERIODS[r.period]} le {r.dayOfMonth}</span>
              </span>
              {!r.active && <Badge>inactive</Badge>}
              <span className={`text-sm font-medium tabular-nums ${byCode.get(r.categoryCode)?.direction === 'INCOME' ? 'text-success' : ''}`}>{formatMoney(r.amountCents)}</span>
            </button>
          </li>
        ))}
      </ul>
      {modal && <RecurringDialog recurring={modal.recurring} categories={categories} accounts={accounts} onClose={() => setModal(null)} />}
    </Card>
  )
}
