// Money accounts and their balances.
import { useState } from 'react'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useFeedback } from '../../app/useFeedback.ts'
import { useAction } from '../../app/useAction.ts'
import { centsToInput, formatMoney, parseMoney } from '../../lib/format.ts'
import { ACCOUNT_KINDS } from '../../lib/labels.ts'
import { Dialog } from '../../components/Dialog.tsx'
import { Button, Card, ErrorMessage, Field, Select } from '../../components/ui.tsx'
import { ACCOUNTING_AREAS } from './tabs.ts'

function AccountDialog({ account, onClose }: { account?: Schemas['Account'], onClose: () => void }) {
  const { confirm } = useFeedback()
  const [form, setForm] = useState({ name: account?.name ?? '', kind: account?.kind ?? 'CHECKING' as Schemas['AccountKind'],
    opening: centsToInput(account?.openingBalanceCents ?? 0) })
  const save = useAction(() => {
    const openingBalanceCents = parseMoney(form.opening) ?? 0
    if (Number.isNaN(openingBalanceCents)) throw new Error('Montant invalide.')
    const body = { name: form.name, kind: form.kind, openingBalanceCents }
    return account
      ? unwrap(api().PUT('/api/accounting/accounts/{id}', { params: { path: { id: account.id } }, body }))
      : unwrap(api().POST('/api/accounting/accounts', { body }))
  }, { refresh: [...ACCOUNTING_AREAS], inline: true, success: 'Compte enregistré', onSuccess: onClose })
  const remove = useAction(() => unwrap(api().DELETE('/api/accounting/accounts/{id}', { params: { path: { id: account!.id } } })),
    { refresh: [...ACCOUNTING_AREAS], inline: true, success: 'Compte supprimé', onSuccess: onClose })
  return (
    <Dialog title={account ? 'Modifier le compte' : 'Nouveau compte'} onClose={onClose} footer={(
      <>
        {account && <Button variant="danger" className="mr-auto" onClick={async () => {
          if (await confirm({ title: 'Supprimer le compte ?', message: `« ${account.name} » sera supprimé s'il n'a aucune opération.` })) remove.mutate(undefined)
        }}>Supprimer</Button>}
        <Button variant="primary" type="submit" form="account-form" disabled={save.isPending}>Enregistrer</Button>
      </>
    )}>
      <form id="account-form" onSubmit={(e) => { e.preventDefault(); save.mutate(undefined) }} className="space-y-3">
        <Field label="Nom"><input className="input" required maxLength={60} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
        <Field label="Type"><Select options={ACCOUNT_KINDS} value={form.kind} onChange={(v) => setForm({ ...form, kind: v || 'CHECKING' })} /></Field>
        <Field label="Solde initial (€)" hint="Solde au moment où vous commencez le suivi.">
          <input className="input" inputMode="decimal" value={form.opening} onChange={(e) => setForm({ ...form, opening: e.target.value })} />
        </Field>
        <ErrorMessage error={save.error ?? remove.error} />
      </form>
    </Dialog>
  )
}

export function AccountsTab({ accounts }: { accounts: Schemas['Account'][] }) {
  const [modal, setModal] = useState<{ account?: Schemas['Account'] } | null>(null)
  return (
    <Card title="Comptes" actions={<Button variant="primary" onClick={() => setModal({})}>Nouveau compte</Button>}>
      <ul className="divide-y divide-line">
        {accounts.map((a) => (
          <li key={a.id}>
            <button type="button" onClick={() => setModal({ account: a })} className="flex w-full items-center justify-between py-2 text-left">
              <span><span className="block text-sm">{a.name}</span><span className="text-xs text-ink-3">{ACCOUNT_KINDS[a.kind]}</span></span>
              <span className={`text-sm font-semibold tabular-nums ${a.balanceCents < 0 ? 'text-danger' : ''}`}>{formatMoney(a.balanceCents)}</span>
            </button>
          </li>
        ))}
      </ul>
      <p className="mt-3 text-xs text-ink-3">Le solde ajoute au solde initial les opérations approuvées du compte.</p>
      {modal && <AccountDialog account={modal.account} onClose={() => setModal(null)} />}
    </Card>
  )
}
