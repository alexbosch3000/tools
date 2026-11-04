// Month report: headline figures, envelopes, 50/30/20 split, income, and the year month by month.
import { useQuery } from '@tanstack/react-query'
import { api, unwrap } from '../../api/client.ts'
import { formatMoney } from '../../lib/format.ts'
import { BUDGET_GROUPS } from '../../lib/labels.ts'
import { Meter, MonthlyColumns, TargetBars } from '../../components/charts.tsx'
import { Card, ErrorMessage, Loading } from '../../components/ui.tsx'
import { MonthPicker } from './shared.tsx'

function Stat({ label, value, hint, tone = '' }: { label: string, value: string, hint?: string | null, tone?: string }) {
  return (
    <div className="card">
      <p className="text-xs font-semibold uppercase text-ink-3">{label}</p>
      <p className={`mt-1 text-2xl font-bold tabular-nums ${tone}`}>{value}</p>
      {hint && <p className="mt-1 text-xs text-ink-3">{hint}</p>}
    </div>
  )
}

export function ReportTab({ month, setMonth }: { month: string, setMonth: (month: string) => void }) {
  const report = useQuery({ queryKey: ['accounting', 'monthly', month], placeholderData: (previous) => previous,
    queryFn: () => unwrap(api().GET('/api/accounting/reports/monthly', { params: { query: { month } } })) })
  const year = Number(month.slice(0, 4))
  const yearly = useQuery({ queryKey: ['accounting', 'yearly', year],
    queryFn: () => unwrap(api().GET('/api/accounting/reports/yearly', { params: { query: { year } } })) })
  if (report.isPending) return <Loading lines={6} />
  if (report.isError) return <ErrorMessage error={report.error} />
  const r = report.data
  const expenses = r.categories.filter((c) => c.direction === 'EXPENSE' && (c.budgetCents || c.actualCents || c.forecastCents))
  const income = r.categories.filter((c) => c.direction === 'INCOME' && (c.actualCents || c.forecastCents))
  const remaining = r.budgetTotalCents - r.expenseActualCents - r.expenseForecastCents

  return (
    <div className="space-y-4">
      <MonthPicker month={month} onChange={setMonth} />
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Revenus" value={formatMoney(r.incomeActualCents)} hint={r.incomeForecastCents ? `+ ${formatMoney(r.incomeForecastCents)} attendus` : null} />
        <Stat label="Dépenses" value={formatMoney(r.expenseActualCents)}
          hint={r.expenseForecastCents ? `+ ${formatMoney(r.expenseForecastCents)} prévues ou à approuver` : null} />
        <Stat label="Solde du mois" value={formatMoney(r.netActualCents)} tone={r.netActualCents < 0 ? 'text-danger' : ''} />
        <Stat label="Budget restant" value={r.budgetTotalCents ? formatMoney(remaining) : '—'} tone={remaining < 0 ? 'text-danger' : ''}
          hint={r.budgetTotalCents ? `sur ${formatMoney(r.budgetTotalCents)} d'enveloppes` : 'Fixez des enveloppes pour suivre le reste à dépenser'} />
      </div>
      <div className="grid gap-4 lg:grid-cols-3">
        <Card title="Enveloppes" className="lg:col-span-2">
          {expenses.length === 0 ? <p className="text-sm text-ink-3">Aucune dépense ce mois-ci.</p> : (
            <div className="space-y-3">
              {expenses.map((c) => (c.budgetCents
                ? <Meter key={c.code} label={c.name} actual={c.actualCents} forecast={c.forecastCents} limit={c.budgetCents} />
                : (
                  <div key={c.code} className="flex justify-between gap-2 text-sm">
                    <span>{c.name} <span className="text-xs text-ink-3">sans enveloppe</span></span>
                    <span className="tabular-nums text-ink-2">{formatMoney(c.actualCents)}{c.forecastCents > 0 && <> + {formatMoney(c.forecastCents)} prévu</>}</span>
                  </div>
                )))}
            </div>
          )}
        </Card>
        <div className="space-y-4">
          <Card title="Règle 50/30/20">
            <TargetBars rows={r.groups.map((g) => ({ key: g.group, label: BUDGET_GROUPS[g.group], share: g.sharePercent, target: g.targetPercent }))} />
            <p className="mt-3 text-xs text-ink-3">Part des revenus approuvés du mois consacrée aux besoins, aux envies et à l'épargne.</p>
          </Card>
          <Card title="Revenus">
            {income.length === 0 ? <p className="text-sm text-ink-3">Aucun revenu enregistré.</p> : (
              <ul className="space-y-1 text-sm">
                {income.map((c) => (
                  <li key={c.code} className="flex justify-between">
                    <span>{c.name}</span>
                    <span className="tabular-nums">{formatMoney(c.actualCents)}{c.forecastCents > 0 && <span className="text-ink-3"> + {formatMoney(c.forecastCents)}</span>}</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>
      </div>
      {yearly.data && (
        <Card title={`Année ${yearly.data.year}`}>
          <MonthlyColumns months={yearly.data.months} />
          <details className="mt-3 text-sm">
            <summary className="cursor-pointer text-ink-3">Voir le tableau</summary>
            <table className="mt-2 w-full text-left">
              <thead className="text-xs uppercase text-ink-3"><tr><th className="py-1 font-medium">Mois</th>
                <th className="py-1 text-right font-medium">Revenus</th><th className="py-1 text-right font-medium">Dépenses</th></tr></thead>
              <tbody className="divide-y divide-line">
                {yearly.data.months.map((m) => (
                  <tr key={m.month}><td className="py-1">{m.month}</td>
                    <td className="py-1 text-right tabular-nums">{formatMoney(m.incomeActualCents)}</td>
                    <td className="py-1 text-right tabular-nums">{formatMoney(m.expenseActualCents)}</td></tr>
                ))}
              </tbody>
            </table>
          </details>
        </Card>
      )}
    </div>
  )
}
