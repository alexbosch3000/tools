// Budget, for parents: month report, transactions, approvals, envelopes, recurring bills and accounts.
import { useQuery } from '@tanstack/react-query'
import { useNavigate, useParams, useSearch } from '@tanstack/react-router'
import { api, unwrap } from '../../api/client.ts'
import { useSession } from '../../app/useSession.ts'
import { monthOf, today } from '../../lib/dates.ts'
import { Empty, ErrorMessage, PageHeader, Tabs } from '../../components/ui.tsx'
import { AccountsTab } from './AccountsTab.tsx'
import { EnvelopesTab } from './EnvelopesTab.tsx'
import { RecurringTab } from './RecurringTab.tsx'
import { ReportTab } from './ReportTab.tsx'
import { ACCOUNTING_TABS, type AccountingTab } from './tabs.ts'
import { ApprovalsTab, TransactionsTab } from './TransactionsTab.tsx'

export function AccountingPage() {
  const { isParent } = useSession()
  const { tab } = useParams({ from: '/budget/$tab' })
  const search = useSearch({ from: '/budget/$tab' })
  const navigate = useNavigate()
  const month = search.month ?? monthOf(today())
  const categories = useQuery({ queryKey: ['accounting', 'categories'], enabled: isParent,
    queryFn: () => unwrap(api().GET('/api/accounting/categories')) })
  const accounts = useQuery({ queryKey: ['accounting', 'accounts'], enabled: isParent,
    queryFn: () => unwrap(api().GET('/api/accounting/accounts')) })
  if (!isParent) return <Empty>Le budget est réservé aux parents.</Empty>

  const go = (to: AccountingTab, toMonth = month) => navigate({ to: '/budget/$tab', params: { tab: to }, search: { month: toMonth } })
  const setMonth = (m: string) => go(tab, m)
  const shared = { categories: categories.data ?? [], accounts: accounts.data ?? [] }
  return (
    <>
      <PageHeader title="Budget" subtitle="Dépenses et revenus de la famille, enveloppes par catégorie, règle 50/30/20." />
      <Tabs tabs={ACCOUNTING_TABS} current={tab} onChange={(t) => go(t)} />
      <ErrorMessage error={categories.error ?? accounts.error} />
      {tab === 'bilan' && <ReportTab month={month} setMonth={setMonth} />}
      {tab === 'operations' && <TransactionsTab month={month} setMonth={setMonth} {...shared} />}
      {tab === 'approbations' && <ApprovalsTab {...shared} />}
      {tab === 'enveloppes' && <EnvelopesTab categories={shared.categories} />}
      {tab === 'echeances' && <RecurringTab {...shared} />}
      {tab === 'comptes' && <AccountsTab accounts={shared.accounts} />}
    </>
  )
}
