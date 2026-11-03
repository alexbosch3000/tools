// Budget, for parents: month report, transactions, approvals, envelopes, recurring bills and accounts.
import { monthOf, today } from '../../lib/dates.js'
import { navigate } from '../../lib/router.js'
import { useResource } from '../../lib/useResource.js'
import { ErrorMessage, PageHeader, Tabs } from '../../components/ui.jsx'
import AccountsTab from './AccountsTab.jsx'
import ApprovalsTab from './ApprovalsTab.jsx'
import EnvelopesTab from './EnvelopesTab.jsx'
import RecurringTab from './RecurringTab.jsx'
import ReportTab from './ReportTab.jsx'
import TransactionsTab from './TransactionsTab.jsx'

const TABS = {
  synthese: 'Bilan',
  operations: 'Opérations',
  approbations: 'À approuver',
  enveloppes: 'Enveloppes',
  echeances: 'Échéances',
  comptes: 'Comptes',
}

export default function AccountingPage({ params }) {
  const tab = TABS[params.tab] ? params.tab : 'synthese'
  const month = params.query.get('month') || monthOf(today())
  const categories = useResource('/api/accounting/categories')
  const accounts = useResource('/api/accounting/accounts')
  const shared = { month, categories: categories.data ?? [], accounts: accounts.data ?? [],
    reloadCategories: categories.reload, reloadAccounts: accounts.reload,
    setMonth: (m) => navigate(`/budget/${tab}?month=${m}`) }

  return (
    <>
      <PageHeader title="Budget" subtitle="Dépenses et revenus de la famille, enveloppes par catégorie, règle 50/30/20." />
      <Tabs tabs={TABS} current={tab} onChange={(t) => navigate(`/budget/${t}?month=${month}`)} />
      <ErrorMessage error={categories.error ?? accounts.error} />
      {tab === 'synthese' && <ReportTab {...shared} />}
      {tab === 'operations' && <TransactionsTab {...shared} />}
      {tab === 'approbations' && <ApprovalsTab {...shared} />}
      {tab === 'enveloppes' && <EnvelopesTab {...shared} />}
      {tab === 'echeances' && <RecurringTab {...shared} />}
      {tab === 'comptes' && <AccountsTab {...shared} />}
    </>
  )
}
