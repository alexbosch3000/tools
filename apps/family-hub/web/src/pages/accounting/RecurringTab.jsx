// Recurring transactions: utilities, insurance, subscriptions, monthly contributions.
import { useState } from 'react'
import { del, post, put } from '../../lib/api.js'
import { today } from '../../lib/dates.js'
import { centsToInput, formatMoney, parseMoney } from '../../lib/format.js'
import { PERIODS } from '../../lib/labels.js'
import { useResource } from '../../lib/useResource.js'
import { useSubmit } from '../../lib/useSubmit.js'
import { Badge, Button, Card, Checkbox, Empty, ErrorMessage, Field, Loading, Modal, Select } from '../../components/ui.jsx'

/** Typical French household commitments, to fill the form in one click. */
const PRESETS = {
  energie: 'Électricité / gaz',
  eau: 'Eau',
  telecom: 'Internet / mobile',
  assurances: 'Assurance',
  logement: 'Loyer / crédit',
  'apport-mensuel': 'Apport mensuel',
}

function RecurringModal({ recurring, categories, accounts, onClose, onSaved }) {
  const [form, setForm] = useState({
    label: recurring?.label ?? '', categoryCode: recurring?.categoryCode ?? 'energie', supplier: recurring?.supplier ?? '',
    amount: centsToInput(recurring?.amountCents), period: recurring?.period ?? 'MONTHLY',
    dayOfMonth: String(recurring?.dayOfMonth ?? 5), startDate: recurring?.startDate ?? today(), endDate: recurring?.endDate ?? '',
    accountId: recurring?.accountId ? String(recurring.accountId) : '', active: recurring?.active ?? true,
  })
  const { busy, error, run, setError } = useSubmit()
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const save = (e) => {
    e.preventDefault()
    const amountCents = parseMoney(form.amount)
    if (amountCents === null || Number.isNaN(amountCents)) { setError(new Error('Montant invalide.')); return }
    run(async () => {
      const body = { ...form, amountCents, dayOfMonth: Number(form.dayOfMonth), endDate: form.endDate || null,
        accountId: form.accountId ? Number(form.accountId) : null }
      if (recurring) await put(`/api/accounting/recurring/${recurring.id}`, body)
      else await post('/api/accounting/recurring', body)
      onSaved()
    })
  }
  return (
    <Modal title={recurring ? "Modifier l'échéance" : 'Nouvelle échéance'} onClose={onClose} footer={(
      <>
        {recurring && <Button variant="danger" className="mr-auto" disabled={busy}
          onClick={() => run(async () => { await del(`/api/accounting/recurring/${recurring.id}`); onSaved() })}>Supprimer</Button>}
        <Button variant="primary" type="submit" form="recurring-form" disabled={busy}>Enregistrer</Button>
      </>
    )}>
      <form id="recurring-form" onSubmit={save} className="grid grid-cols-2 gap-3">
        {!recurring && (
          <div className="col-span-2 flex flex-wrap gap-1">
            {Object.entries(PRESETS).map(([code, label]) => (
              <button key={code} type="button" onClick={() => set({ categoryCode: code, label })}
                className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-700 hover:bg-indigo-50">{label}</button>
            ))}
          </div>
        )}
        <Field label="Libellé" className="col-span-2"><input className="input" required maxLength={120} value={form.label}
          onChange={(e) => set({ label: e.target.value })} /></Field>
        <Field label="Fournisseur"><input className="input" maxLength={80} value={form.supplier} onChange={(e) => set({ supplier: e.target.value })} /></Field>
        <Field label="Catégorie"><Select options={Object.fromEntries(categories.map((c) => [c.code, c.name]))} value={form.categoryCode}
          onChange={(v) => set({ categoryCode: v })} /></Field>
        <Field label="Montant (€)"><input className="input" inputMode="decimal" required value={form.amount}
          onChange={(e) => set({ amount: e.target.value })} /></Field>
        <Field label="Fréquence"><Select options={PERIODS} value={form.period} onChange={(v) => set({ period: v })} /></Field>
        <Field label="Jour du mois"><input className="input" type="number" min="1" max="31" required value={form.dayOfMonth}
          onChange={(e) => set({ dayOfMonth: e.target.value })} /></Field>
        <Field label="Compte"><Select options={Object.fromEntries(accounts.map((a) => [String(a.id), a.name]))} value={form.accountId}
          placeholder="Non précisé" onChange={(v) => set({ accountId: v ?? '' })} /></Field>
        <Field label="Début"><input className="input" type="date" required value={form.startDate} onChange={(e) => set({ startDate: e.target.value })} /></Field>
        <Field label="Fin (renouvellement)"><input className="input" type="date" value={form.endDate} onChange={(e) => set({ endDate: e.target.value })} /></Field>
        <div className="col-span-2"><Checkbox label="Active" checked={form.active} onChange={(active) => set({ active })} /></div>
        <div className="col-span-2"><ErrorMessage error={error} /></div>
      </form>
    </Modal>
  )
}

export default function RecurringTab({ categories, accounts }) {
  const recurring = useResource('/api/accounting/recurring')
  const [modal, setModal] = useState(null)
  const byCode = Object.fromEntries(categories.map((c) => [c.code, c]))
  return (
    <Card title="Échéances récurrentes" actions={<Button variant="primary" onClick={() => setModal({})}>Nouvelle échéance</Button>}>
      <p className="mb-3 text-sm text-slate-500">
        Chaque échéance crée une opération prévue pour le mois en cours et le suivant, à approuver une fois prélevée.
      </p>
      {recurring.loading && <Loading />}
      <ErrorMessage error={recurring.error} />
      {recurring.data?.length === 0 && <Empty>Aucune échéance : ajoutez l'électricité, l'eau, internet, les assurances…</Empty>}
      <ul className="divide-y divide-slate-100">
        {recurring.data?.map((r) => (
          <li key={r.id}>
            <button type="button" onClick={() => setModal({ recurring: r })} className="flex w-full items-center gap-3 py-2 text-left">
              <span className="flex-1">
                <span className="block text-sm text-slate-800">{r.label}{r.supplier ? ` · ${r.supplier}` : ''}</span>
                <span className="text-xs text-slate-500">{byCode[r.categoryCode]?.name} · {PERIODS[r.period]} le {r.dayOfMonth}</span>
              </span>
              {!r.active && <Badge>inactive</Badge>}
              <span className={`text-sm font-medium tabular-nums ${byCode[r.categoryCode]?.direction === 'INCOME' ? 'text-emerald-700' : 'text-slate-800'}`}>
                {formatMoney(r.amountCents)}
              </span>
            </button>
          </li>
        ))}
      </ul>
      {modal && <RecurringModal recurring={modal.recurring} categories={categories} accounts={accounts}
        onClose={() => setModal(null)} onSaved={() => { setModal(null); recurring.reload() }} />}
    </Card>
  )
}
