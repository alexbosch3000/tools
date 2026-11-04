// Charts in plain HTML and CSS: meters against a limit, ranked bars, targets and monthly columns.
// Colours come from the chart tokens of index.css (validated palette, light and dark steps).
import { useState, type ReactNode } from 'react'
import { formatMoney } from '../lib/format.ts'

/** Spent amount against a limit: solid for actual, light for forecast; going over is said in words. */
export function Meter({ label, actual, forecast = 0, limit }:
  { label?: string, actual: number, forecast?: number, limit?: number | null }) {
  const total = actual + forecast
  const scale = Math.max(limit ?? 0, total, 1)
  const over = limit !== null && limit !== undefined && total > limit
  return (
    <div>
      {label && (
        <div className="mb-1 flex items-baseline justify-between gap-2 text-sm">
          <span>{label}</span>
          <span className="text-right tabular-nums text-ink-2">
            {formatMoney(actual)}
            {forecast > 0 && <> + {formatMoney(forecast)} prévu</>}
            {limit !== null && limit !== undefined && <> / {formatMoney(limit)}</>}
            {over && <span className="ml-1 font-semibold text-danger">· dépassé</span>}
          </span>
        </div>
      )}
      <div role="img" aria-label={`${formatMoney(actual)} dépensés${limit ? ` sur ${formatMoney(limit)}` : ''}`}
        className="flex h-2.5 w-full gap-0.5 overflow-hidden rounded-full bg-chart-track">
        <div className={`h-full rounded-l-full ${over ? 'bg-danger' : 'bg-chart-1'}`} style={{ width: `${(actual / scale) * 100}%` }} />
        {forecast > 0 && <div className="h-full bg-chart-soft" style={{ width: `${(forecast / scale) * 100}%` }} />}
      </div>
    </div>
  )
}

export interface BarRow {
  key: string | number
  label: string
  value: number
  icon?: ReactNode
}

/** Ranked magnitudes, one hue, value printed at the tip of each bar. */
export function BarList({ rows, format = String }: { rows: BarRow[], format?: (value: number) => string }) {
  const max = Math.max(1, ...rows.map((r) => r.value))
  return (
    <ul className="space-y-2">
      {rows.map((row) => (
        <li key={row.key} className="flex items-center gap-3 text-sm">
          <span className="flex w-28 shrink-0 items-center gap-2 truncate">{row.icon}{row.label}</span>
          <span className="flex flex-1 items-center gap-2">
            <span className="h-3 rounded-r bg-chart-1" style={{ width: `${(row.value / max) * 100}%`, minWidth: row.value ? 4 : 0 }} />
            <span className="shrink-0 tabular-nums text-ink-2">{format(row.value)}</span>
          </span>
        </li>
      ))}
    </ul>
  )
}

/** Share of income per 50/30/20 group, with the target marked by a tick. */
export function TargetBars({ rows }: { rows: { key: string, label: string, share: number | null | undefined, target: number }[] }) {
  return (
    <ul className="space-y-3">
      {rows.map((row) => (
        <li key={row.key} className="text-sm">
          <div className="mb-1 flex justify-between">
            <span>{row.label}</span>
            <span className="tabular-nums text-ink-2">
              {row.share === null || row.share === undefined ? '—' : `${row.share} %`}
              <span className="text-ink-3"> (cible {row.target} %)</span>
            </span>
          </div>
          <div className="relative h-2.5 rounded-full bg-chart-track">
            <div className="h-full rounded-full bg-chart-1" style={{ width: `${Math.min(100, row.share ?? 0)}%` }} />
            <div className="absolute -top-1 h-[18px] w-0.5 bg-ink" style={{ left: `${row.target}%` }} aria-hidden="true" />
          </div>
        </li>
      ))}
    </ul>
  )
}

const MONTH_INITIALS = ['J', 'F', 'M', 'A', 'M', 'J', 'J', 'A', 'S', 'O', 'N', 'D']

interface MonthTotals {
  month: string
  incomeActualCents: number
  expenseActualCents: number
}

/** Income and expenses per month as paired columns (rounded top, square on the axis), with a hover readout. */
export function MonthlyColumns({ months }: { months: MonthTotals[] }) {
  const [hover, setHover] = useState<number | null>(null)
  const max = Math.max(1, ...months.flatMap((m) => [m.incomeActualCents, m.expenseActualCents]))
  const shown = hover === null ? null : months[hover]
  const column = (cents: number, color: string) => (
    <div className={`w-full max-w-4 rounded-t ${color}`}
      style={{ height: `${(cents / max) * 100}%`, minHeight: cents > 0 ? 2 : 0 }} />
  )
  return (
    <div onMouseLeave={() => setHover(null)}>
      <div className="mb-2 flex flex-wrap items-center gap-4 text-xs text-ink-2">
        <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-sm bg-chart-1" />Revenus</span>
        <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-sm bg-chart-2" />Dépenses</span>
        <span className="ml-auto min-h-4 tabular-nums text-ink">
          {shown && <>{shown.month} · revenus {formatMoney(shown.incomeActualCents)} · dépenses {formatMoney(shown.expenseActualCents)}</>}
        </span>
      </div>
      <div role="img" aria-label="Revenus et dépenses approuvés par mois" className="flex h-40 border-b border-line">
        {months.map((m, i) => (
          <div key={m.month} onMouseEnter={() => setHover(i)}
            className={`flex h-full flex-1 items-end justify-center gap-0.5 px-0.5 ${hover === i ? 'bg-surface-2' : ''}`}>
            {column(m.incomeActualCents, 'bg-chart-1')}
            {column(m.expenseActualCents, 'bg-chart-2')}
          </div>
        ))}
      </div>
      <div className="flex text-xs text-ink-3">
        {months.map((m) => <span key={m.month} className="flex-1 pt-1 text-center">{MONTH_INITIALS[Number(m.month.slice(5, 7)) - 1]}</span>)}
      </div>
    </div>
  )
}
