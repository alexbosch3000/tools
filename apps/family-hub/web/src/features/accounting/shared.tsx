// Pieces shared by the budget tabs: month selector, transaction row and transaction dialog.
import { useState } from 'react'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useFeedback } from '../../app/useFeedback.ts'
import { useAction } from '../../app/useAction.ts'
import { addMonths, today } from '../../lib/dates.ts'
import { centsToInput, formatDate, formatMoney, formatMonth, parseMoney } from '../../lib/format.ts'
import { SOURCE_TYPES, TRANSACTION_STATUSES, TRANSACTION_STATUS_STYLES } from '../../lib/labels.ts'
import { Dialog } from '../../components/Dialog.tsx'
import { Badge, Button, ErrorMessage, Field, Select } from '../../components/ui.tsx'
import { ACCOUNTING_AREAS } from './tabs.ts'

export function MonthPicker({ month, onChange }: { month: string, onChange: (month: string) => void }) {
  const shift = (n: number) => onChange(addMonths(`${month}-01`, n).slice(0, 7))
  return (
    <div className="flex items-center gap-2">
      <Button variant="ghost" onClick={() => shift(-1)} aria-label="Mois précédent">‹</Button>
      <span className="min-w-36 text-center font-medium capitalize" aria-live="polite">{formatMonth(month)}</span>
      <Button variant="ghost" onClick={() => shift(1)} aria-label="Mois suivant">›</Button>
    </div>
  )
}

export function TransactionRow({ t, onOpen, action }: { t: Schemas['Transaction'], onOpen: () => void, action?: React.ReactNode }) {
  return (
    <li className="flex flex-wrap items-center gap-3 py-2">
      <button type="button" onClick={onOpen} className="flex min-w-0 flex-1 items-center gap-3 text-left">
        <span className="w-20 shrink-0 text-xs text-ink-3">{formatDate(t.day)}</span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm">{t.label}</span>
          <span className="block truncate text-xs text-ink-3">{t.categoryName}{t.supplier ? ` · ${t.supplier}` : ''} · {SOURCE_TYPES[t.sourceType]}</span>
        </span>
        <Badge className={TRANSACTION_STATUS_STYLES[t.status]}>{TRANSACTION_STATUSES[t.status]}</Badge>
        <span className={`w-24 text-right text-sm font-medium tabular-nums ${t.direction === 'INCOME' ? 'text-success' : ''}`}>
          {t.direction === 'INCOME' ? '+' : '−'}{formatMoney(t.amountCents)}
        </span>
      </button>
      {action}
    </li>
  )
}

const categoryOptions = (categories: Schemas['Category'][]) =>
  Object.fromEntries(categories.map((c) => [c.code, `${c.direction === 'INCOME' ? 'Revenu · ' : ''}${c.name}`]))

export function TransactionDialog({ transaction, categories, accounts, onClose }:
  { transaction?: Schemas['Transaction'], categories: Schemas['Category'][], accounts: Schemas['Account'][], onClose: () => void }) {
  const { confirm } = useFeedback()
  const [form, setForm] = useState({
    day: transaction?.day ?? today(), label: transaction?.label ?? '', amount: centsToInput(transaction?.amountCents),
    categoryCode: transaction?.categoryCode ?? 'courses', accountId: transaction?.accountId ? String(transaction.accountId) : '',
    supplier: transaction?.supplier ?? '', status: transaction?.status ?? 'PENDING' as Schemas['TransactionStatus'],
  })
  const set = (patch: Partial<typeof form>) => setForm((f) => ({ ...f, ...patch }))
  const body = (): Schemas['TransactionRequest'] => {
    const amountCents = parseMoney(form.amount)
    if (amountCents === null || Number.isNaN(amountCents)) throw new Error('Montant invalide, par exemple 45,90.')
    return { day: form.day, label: form.label, amountCents, categoryCode: form.categoryCode,
      accountId: form.accountId ? Number(form.accountId) : null, supplier: form.supplier, status: form.status }
  }
  const done = { refresh: [...ACCOUNTING_AREAS], inline: true, onSuccess: onClose }
  const save = useAction(() => transaction
    ? unwrap(api().PUT('/api/accounting/transactions/{id}', { params: { path: { id: transaction.id } }, body: body() }))
    : unwrap(api().POST('/api/accounting/transactions', { body: body() })), { ...done, success: 'Opération enregistrée' })
  const approve = useAction(async () => {
    const id = transaction!.id
    await unwrap(api().PUT('/api/accounting/transactions/{id}', { params: { path: { id } }, body: body() }))
    return unwrap(api().POST('/api/accounting/transactions/{id}/approve', { params: { path: { id } }, body: {} }))
  }, { ...done, success: 'Opération approuvée' })
  const remove = useAction(() => unwrap(api().DELETE('/api/accounting/transactions/{id}', { params: { path: { id: transaction!.id } } })),
    { ...done, success: 'Opération supprimée' })
  const busy = save.isPending || approve.isPending || remove.isPending

  return (
    <Dialog title={transaction ? 'Opération' : 'Nouvelle opération'} onClose={onClose} footer={(
      <>
        {transaction && <Button variant="danger" className="mr-auto" disabled={busy} onClick={async () => {
          if (await confirm({ title: "Supprimer l'opération ?", message: `« ${transaction.label} » sera supprimée du budget.` })) remove.mutate(undefined)
        }}>Supprimer</Button>}
        {transaction && transaction.status !== 'APPROVED' && <Button disabled={busy} onClick={() => approve.mutate(undefined)}>Enregistrer et approuver</Button>}
        <Button variant="primary" type="submit" form="transaction-form" disabled={busy}>Enregistrer</Button>
      </>
    )}>
      <form id="transaction-form" onSubmit={(e) => { e.preventDefault(); save.mutate(undefined) }} className="grid grid-cols-2 gap-3">
        <Field label="Libellé" className="col-span-2"><input className="input" required maxLength={120} value={form.label} onChange={(e) => set({ label: e.target.value })} /></Field>
        <Field label="Montant (€)"><input className="input" inputMode="decimal" required value={form.amount} onChange={(e) => set({ amount: e.target.value })} /></Field>
        <Field label="Date"><input className="input" type="date" required value={form.day} onChange={(e) => set({ day: e.target.value })} /></Field>
        <Field label="Catégorie"><Select options={categoryOptions(categories)} value={form.categoryCode} onChange={(v) => set({ categoryCode: v })} /></Field>
        <Field label="Compte"><Select options={Object.fromEntries(accounts.map((a) => [String(a.id), a.name]))} value={form.accountId}
          placeholder="Non précisé" onChange={(v) => set({ accountId: v })} /></Field>
        <Field label="Fournisseur"><input className="input" maxLength={80} value={form.supplier} placeholder="EDF, Orange, Decathlon…"
          onChange={(e) => set({ supplier: e.target.value })} /></Field>
        {!transaction && <Field label="Statut"><Select options={TRANSACTION_STATUSES} value={form.status} onChange={(v) => set({ status: v || 'PENDING' })} /></Field>}
        <div className="col-span-2"><ErrorMessage error={save.error ?? approve.error ?? remove.error} /></div>
      </form>
    </Dialog>
  )
}
