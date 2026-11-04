// Transactions of a month whatever their status, and the approval queue.
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useAction } from '../../app/useAction.ts'
import { Button, Card, Empty, ErrorMessage, Loading } from '../../components/ui.tsx'
import { MonthPicker, TransactionDialog, TransactionRow } from './shared.tsx'
import { ACCOUNTING_AREAS } from './tabs.ts'

interface Props {
  month: string
  setMonth: (month: string) => void
  categories: Schemas['Category'][]
  accounts: Schemas['Account'][]
}

export function TransactionsTab({ month, setMonth, categories, accounts }: Props) {
  const transactions = useQuery({ queryKey: ['accounting', 'transactions', month], placeholderData: (previous) => previous,
    queryFn: () => unwrap(api().GET('/api/accounting/transactions', { params: { query: { month } } })) })
  const [modal, setModal] = useState<{ transaction?: Schemas['Transaction'] } | null>(null)
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <MonthPicker month={month} onChange={setMonth} />
        <Button variant="primary" onClick={() => setModal({})}>Nouvelle opération</Button>
      </div>
      <Card>
        {transactions.isPending && <Loading />}
        <ErrorMessage error={transactions.error} />
        {transactions.data?.length === 0 && <Empty>Aucune opération ce mois-ci.</Empty>}
        <ul className="divide-y divide-line">
          {transactions.data?.map((t) => <TransactionRow key={t.id} t={t} onOpen={() => setModal({ transaction: t })} />)}
        </ul>
      </Card>
      {modal && <TransactionDialog transaction={modal.transaction} categories={categories} accounts={accounts} onClose={() => setModal(null)} />}
    </div>
  )
}

export function ApprovalsTab({ categories, accounts }: Omit<Props, 'month' | 'setMonth'>) {
  const queue = useQuery({ queryKey: ['accounting', 'approvals'], queryFn: () => unwrap(api().GET('/api/accounting/approvals')) })
  const [modal, setModal] = useState<Schemas['Transaction'] | null>(null)
  const approve = useAction((t: Schemas['Transaction']) => unwrap(api().POST('/api/accounting/transactions/{id}/approve',
    { params: { path: { id: t.id } }, body: {} })), { refresh: [...ACCOUNTING_AREAS], success: (t) => `« ${t.label} » approuvée` })
  return (
    <Card title="À approuver">
      <p className="mb-3 text-sm text-ink-3">
        Courses terminées, dépenses saisies et échéances arrivées à terme. Ouvrez une ligne pour corriger le montant réel avant d'approuver.
      </p>
      {queue.isPending && <Loading />}
      <ErrorMessage error={queue.error} />
      {queue.data?.length === 0 && <Empty>Tout est à jour.</Empty>}
      <ul className="divide-y divide-line">
        {queue.data?.map((t) => (
          <TransactionRow key={t.id} t={t} onOpen={() => setModal(t)}
            action={<Button variant="primary" disabled={approve.isPending} onClick={() => approve.mutate(t)}>Approuver</Button>} />
        ))}
      </ul>
      {modal && <TransactionDialog transaction={modal} categories={categories} accounts={accounts} onClose={() => setModal(null)} />}
    </Card>
  )
}
