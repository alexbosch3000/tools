// Transactions of a month, whatever their status.
import { useState } from 'react'
import { formatDate, formatMoney } from '../../lib/format.js'
import { SOURCE_TYPES, TRANSACTION_STATUSES, TRANSACTION_STATUS_STYLES } from '../../lib/labels.js'
import { useResource } from '../../lib/useResource.js'
import { Badge, Button, Card, Empty, ErrorMessage, Loading } from '../../components/ui.jsx'
import MonthPicker from './MonthPicker.jsx'
import TransactionModal from './TransactionModal.jsx'

export function TransactionRow({ t, onClick, action }) {
  return (
    <li className="flex flex-wrap items-center gap-3 py-2">
      <button type="button" onClick={onClick} className="flex flex-1 items-center gap-3 text-left">
        <span className="w-20 shrink-0 text-xs text-slate-500">{formatDate(t.day)}</span>
        <span className="flex-1">
          <span className="block text-sm text-slate-800">{t.label}</span>
          <span className="text-xs text-slate-500">{t.categoryName}{t.supplier ? ` · ${t.supplier}` : ''} · {SOURCE_TYPES[t.sourceType]}</span>
        </span>
        <Badge className={TRANSACTION_STATUS_STYLES[t.status]}>{TRANSACTION_STATUSES[t.status]}</Badge>
        <span className={`w-24 text-right text-sm font-medium tabular-nums ${t.direction === 'INCOME' ? 'text-emerald-700' : 'text-slate-800'}`}>
          {t.direction === 'INCOME' ? '+' : '−'}{formatMoney(t.amountCents)}
        </span>
      </button>
      {action}
    </li>
  )
}

export default function TransactionsTab({ month, setMonth, categories, accounts, reloadAccounts }) {
  const transactions = useResource(`/api/accounting/transactions?month=${month}`)
  const [modal, setModal] = useState(null)
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <MonthPicker month={month} onChange={setMonth} />
        <Button variant="primary" onClick={() => setModal({})}>Nouvelle opération</Button>
      </div>
      <Card>
        {transactions.loading && <Loading />}
        <ErrorMessage error={transactions.error} />
        {transactions.data?.length === 0 && <Empty>Aucune opération ce mois-ci.</Empty>}
        <ul className="divide-y divide-slate-100">
          {transactions.data?.map((t) => <TransactionRow key={t.id} t={t} onClick={() => setModal({ transaction: t })} />)}
        </ul>
      </Card>
      {modal && <TransactionModal transaction={modal.transaction} categories={categories} accounts={accounts}
        onClose={() => setModal(null)} onSaved={() => { setModal(null); transactions.reload(); reloadAccounts() }} />}
    </div>
  )
}
