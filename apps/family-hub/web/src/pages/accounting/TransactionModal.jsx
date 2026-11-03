// Create or edit a transaction, or approve a planned or pending one with its actual values.
import { useState } from 'react'
import { del, post, put } from '../../lib/api.js'
import { today } from '../../lib/dates.js'
import { centsToInput, parseMoney } from '../../lib/format.js'
import { TRANSACTION_STATUSES } from '../../lib/labels.js'
import { useSubmit } from '../../lib/useSubmit.js'
import { Button, ErrorMessage, Field, Modal, Select } from '../../components/ui.jsx'

function categoryOptions(categories) {
  return Object.fromEntries(categories.map((c) => [c.code, `${c.direction === 'INCOME' ? 'Revenu · ' : ''}${c.name}`]))
}

export default function TransactionModal({ transaction, categories, accounts, onClose, onSaved }) {
  const [form, setForm] = useState({
    day: transaction?.day ?? today(), label: transaction?.label ?? '', amount: centsToInput(transaction?.amountCents),
    categoryCode: transaction?.categoryCode ?? 'courses', accountId: transaction?.accountId ? String(transaction.accountId) : '',
    supplier: transaction?.supplier ?? '', status: transaction?.status ?? 'PENDING',
  })
  const { busy, error, run } = useSubmit()
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const approvable = transaction && transaction.status !== 'APPROVED'

  const body = () => {
    const amountCents = parseMoney(form.amount)
    if (amountCents === null || Number.isNaN(amountCents)) throw new Error('Montant invalide, par exemple 45,90.')
    return { day: form.day, label: form.label, amountCents, categoryCode: form.categoryCode,
      accountId: form.accountId ? Number(form.accountId) : null, supplier: form.supplier, status: form.status }
  }
  const save = (e) => {
    e.preventDefault()
    run(async () => {
      if (transaction) await put(`/api/accounting/transactions/${transaction.id}`, body())
      else await post('/api/accounting/transactions', body())
      onSaved()
    })
  }
  const approve = () => run(async () => {
    const b = body()
    await put(`/api/accounting/transactions/${transaction.id}`, b)
    await post(`/api/accounting/transactions/${transaction.id}/approve`, {})
    onSaved()
  })

  return (
    <Modal title={transaction ? 'Opération' : 'Nouvelle opération'} onClose={onClose} footer={(
      <>
        {transaction && <Button variant="danger" className="mr-auto" disabled={busy}
          onClick={() => run(async () => { await del(`/api/accounting/transactions/${transaction.id}`); onSaved() })}>Supprimer</Button>}
        {approvable && <Button disabled={busy} onClick={approve}>Enregistrer et approuver</Button>}
        <Button variant="primary" type="submit" form="transaction-form" disabled={busy}>Enregistrer</Button>
      </>
    )}>
      <form id="transaction-form" onSubmit={save} className="grid grid-cols-2 gap-3">
        <Field label="Libellé" className="col-span-2"><input className="input" required maxLength={120} value={form.label}
          onChange={(e) => set({ label: e.target.value })} /></Field>
        <Field label="Montant (€)"><input className="input" inputMode="decimal" required value={form.amount}
          onChange={(e) => set({ amount: e.target.value })} /></Field>
        <Field label="Date"><input className="input" type="date" required value={form.day} onChange={(e) => set({ day: e.target.value })} /></Field>
        <Field label="Catégorie"><Select options={categoryOptions(categories)} value={form.categoryCode}
          onChange={(v) => set({ categoryCode: v })} /></Field>
        <Field label="Compte"><Select options={Object.fromEntries(accounts.map((a) => [String(a.id), a.name]))} value={form.accountId}
          placeholder="Non précisé" onChange={(v) => set({ accountId: v ?? '' })} /></Field>
        <Field label="Fournisseur"><input className="input" maxLength={80} value={form.supplier} placeholder="EDF, Orange…"
          onChange={(e) => set({ supplier: e.target.value })} /></Field>
        {!transaction && (
          <Field label="Statut"><Select options={TRANSACTION_STATUSES} value={form.status} onChange={(v) => set({ status: v })} /></Field>
        )}
        <div className="col-span-2"><ErrorMessage error={error} /></div>
      </form>
    </Modal>
  )
}
