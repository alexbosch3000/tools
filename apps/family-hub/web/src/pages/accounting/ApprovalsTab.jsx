// Approval queue: pending transactions and planned ones whose date has come.
import { useState } from 'react'
import { post } from '../../lib/api.js'
import { useResource } from '../../lib/useResource.js'
import { useSubmit } from '../../lib/useSubmit.js'
import { Button, Card, Empty, ErrorMessage, Loading } from '../../components/ui.jsx'
import TransactionModal from './TransactionModal.jsx'
import { TransactionRow } from './TransactionsTab.jsx'

export default function ApprovalsTab({ categories, accounts, reloadAccounts }) {
  const queue = useResource('/api/accounting/approvals')
  const [modal, setModal] = useState(null)
  const { busy, error, run } = useSubmit()
  const refresh = () => { queue.reload(); reloadAccounts() }
  const approve = (t) => run(async () => { await post(`/api/accounting/transactions/${t.id}/approve`, {}); refresh() })

  return (
    <Card title="À approuver">
      <p className="mb-3 text-sm text-slate-500">
        Courses terminées, dépenses saisies et échéances arrivées à terme. Ouvrez une ligne pour corriger le montant réel avant d'approuver.
      </p>
      {queue.loading && <Loading />}
      <ErrorMessage error={queue.error ?? error} />
      {queue.data?.length === 0 && <Empty>Tout est à jour.</Empty>}
      <ul className="divide-y divide-slate-100">
        {queue.data?.map((t) => (
          <TransactionRow key={t.id} t={t} onClick={() => setModal(t)}
            action={<Button variant="primary" disabled={busy} onClick={() => approve(t)}>Approuver</Button>} />
        ))}
      </ul>
      {modal && <TransactionModal transaction={modal} categories={categories} accounts={accounts}
        onClose={() => setModal(null)} onSaved={() => { setModal(null); refresh() }} />}
    </Card>
  )
}
