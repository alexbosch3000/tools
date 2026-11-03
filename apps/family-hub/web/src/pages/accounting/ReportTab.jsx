// Month report: headline figures, 50/30/20 split, envelopes, and the year month by month.
import { formatMoney } from '../../lib/format.js'
import { BUDGET_GROUPS } from '../../lib/labels.js'
import { useResource } from '../../lib/useResource.js'
import { Meter, MonthlyColumns, TargetBars } from '../../components/charts.jsx'
import { Card, ErrorMessage, Loading } from '../../components/ui.jsx'
import MonthPicker from './MonthPicker.jsx'

function Stat({ label, value, hint, tone = 'text-slate-800' }) {
  return (
    <div className="card">
      <p className="text-xs font-semibold uppercase text-slate-500">{label}</p>
      <p className={`mt-1 text-2xl font-bold tabular-nums ${tone}`}>{value}</p>
      {hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
    </div>
  )
}

export default function ReportTab({ month, setMonth }) {
  const report = useResource(`/api/accounting/reports/monthly?month=${month}`)
  const yearly = useResource(`/api/accounting/reports/yearly?year=${month.slice(0, 4)}`)
  if (report.loading) return <Loading />
  if (report.error) return <ErrorMessage error={report.error} />
  const r = report.data
  const expenses = r.categories.filter((c) => c.direction === 'EXPENSE' && (c.budgetCents || c.actualCents || c.forecastCents))
  const income = r.categories.filter((c) => c.direction === 'INCOME' && (c.actualCents || c.forecastCents))
  const net = r.incomeActualCents - r.expenseActualCents

  return (
    <div className="space-y-4">
      <MonthPicker month={month} onChange={setMonth} />
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Revenus" value={formatMoney(r.incomeActualCents)}
          hint={r.incomeForecastCents ? `+ ${formatMoney(r.incomeForecastCents)} attendus` : null} />
        <Stat label="Dépenses" value={formatMoney(r.expenseActualCents)}
          hint={r.expenseForecastCents ? `+ ${formatMoney(r.expenseForecastCents)} prévues ou à approuver` : null} />
        <Stat label="Solde du mois" value={formatMoney(net)} tone={net < 0 ? 'text-red-700' : 'text-slate-800'} />
        <Stat label="Budget restant" value={r.budgetTotalCents ? formatMoney(r.budgetTotalCents - r.expenseActualCents - r.expenseForecastCents) : '—'}
          hint={r.budgetTotalCents ? `sur ${formatMoney(r.budgetTotalCents)} d'enveloppes` : 'Fixez des enveloppes pour suivre le reste à dépenser'} />
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card title="Enveloppes" className="lg:col-span-2">
          {expenses.length === 0 ? <p className="text-sm text-slate-500">Aucune dépense ce mois-ci.</p> : (
            <div className="space-y-3">
              {expenses.map((c) => (c.budgetCents
                ? <Meter key={c.code} label={c.name} actual={c.actualCents} forecast={c.forecastCents} limit={c.budgetCents} />
                : (
                  <div key={c.code} className="flex justify-between gap-2 text-sm">
                    <span className="text-slate-700">{c.name} <span className="text-xs text-slate-400">sans enveloppe</span></span>
                    <span className="tabular-nums text-slate-500">{formatMoney(c.actualCents)}
                      {c.forecastCents > 0 && <> + {formatMoney(c.forecastCents)} prévu</>}</span>
                  </div>
                )))}
            </div>
          )}
        </Card>
        <div className="space-y-4">
          <Card title="Règle 50/30/20">
            <TargetBars rows={r.groups.map((g) => ({ key: g.group, label: BUDGET_GROUPS[g.group], share: g.sharePercent, target: g.targetPercent }))} />
            <p className="mt-3 text-xs text-slate-500">Part des revenus approuvés du mois consacrée aux besoins, aux envies et à l'épargne.</p>
          </Card>
          <Card title="Revenus">
            {income.length === 0 ? <p className="text-sm text-slate-500">Aucun revenu enregistré.</p> : (
              <ul className="space-y-1 text-sm">
                {income.map((c) => (
                  <li key={c.code} className="flex justify-between">
                    <span className="text-slate-700">{c.name}</span>
                    <span className="tabular-nums text-slate-800">{formatMoney(c.actualCents)}
                      {c.forecastCents > 0 && <span className="text-slate-500"> + {formatMoney(c.forecastCents)}</span>}</span>
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
            <summary className="cursor-pointer text-slate-500">Voir le tableau</summary>
            <table className="mt-2 w-full text-left">
              <thead className="text-xs uppercase text-slate-500"><tr><th className="py-1">Mois</th>
                <th className="py-1 text-right">Revenus</th><th className="py-1 text-right">Dépenses</th></tr></thead>
              <tbody className="divide-y divide-slate-100">
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
