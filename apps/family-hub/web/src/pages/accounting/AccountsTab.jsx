// Money accounts and their balances.
import { useState } from 'react'
import { del, post, put } from '../../lib/api.js'
import { centsToInput, formatMoney, parseMoney } from '../../lib/format.js'
import { ACCOUNT_KINDS } from '../../lib/labels.js'
import { useSubmit } from '../../lib/useSubmit.js'
import { Button, Card, ErrorMessage, Field, Modal, Select } from '../../components/ui.jsx'

function AccountModal({ account, onClose, onSaved }) {
  const [form, setForm] = useState({ name: account?.name ?? '', kind: account?.kind ?? 'CHECKING',
    opening: centsToInput(account?.openingBalanceCents ?? 0) })
  const { busy, error, run, setError } = useSubmit()
  const save = (e) => {
    e.preventDefault()
    const openingBalanceCents = parseMoney(form.opening) ?? 0
    if (Number.isNaN(openingBalanceCents)) { setError(new Error('Montant invalide.')); return }
    run(async () => {
      const body = { name: form.name, kind: form.kind, openingBalanceCents }
      if (account) await put(`/api/accounting/accounts/${account.id}`, body)
      else await post('/api/accounting/accounts', body)
      onSaved()
    })
  }
  return (
    <Modal title={account ? 'Modifier le compte' : 'Nouveau compte'} onClose={onClose} footer={(
      <>
        {account && <Button variant="danger" className="mr-auto" disabled={busy}
          onClick={() => run(async () => { await del(`/api/accounting/accounts/${account.id}`); onSaved() })}>Supprimer</Button>}
        <Button variant="primary" type="submit" form="account-form" disabled={busy}>Enregistrer</Button>
      </>
    )}>
      <form id="account-form" onSubmit={save} className="space-y-3">
        <Field label="Nom"><input className="input" required maxLength={60} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
        <Field label="Type"><Select options={ACCOUNT_KINDS} value={form.kind} onChange={(v) => setForm({ ...form, kind: v })} /></Field>
        <Field label="Solde initial (€)" hint="Solde au moment où vous commencez le suivi.">
          <input className="input" inputMode="decimal" value={form.opening} onChange={(e) => setForm({ ...form, opening: e.target.value })} />
        </Field>
        <ErrorMessage error={error} />
      </form>
    </Modal>
  )
}

export default function AccountsTab({ accounts, reloadAccounts }) {
  const [modal, setModal] = useState(null)
  return (
    <Card title="Comptes" actions={<Button variant="primary" onClick={() => setModal({})}>Nouveau compte</Button>}>
      <ul className="divide-y divide-slate-100">
        {accounts.map((a) => (
          <li key={a.id}>
            <button type="button" onClick={() => setModal({ account: a })} className="flex w-full items-center justify-between py-2 text-left">
              <span><span className="block text-sm text-slate-800">{a.name}</span>
                <span className="text-xs text-slate-500">{ACCOUNT_KINDS[a.kind]}</span></span>
              <span className={`text-sm font-semibold tabular-nums ${a.balanceCents < 0 ? 'text-red-700' : 'text-slate-800'}`}>{formatMoney(a.balanceCents)}</span>
            </button>
          </li>
        ))}
      </ul>
      <p className="mt-3 text-xs text-slate-500">Le solde ajoute au solde initial les opérations approuvées du compte.</p>
      {modal && <AccountModal account={modal.account} onClose={() => setModal(null)}
        onSaved={() => { setModal(null); reloadAccounts() }} />}
    </Card>
  )
}
