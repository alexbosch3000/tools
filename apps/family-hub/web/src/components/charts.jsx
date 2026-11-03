// Charts drawn with plain HTML and SVG: meters against a limit, horizontal bars and monthly columns.
// Colours follow the validated default palette of the dataviz guidelines (one hue for magnitude,
// blue and orange for the two series income and expenses).
import { useState } from 'react'
import { formatMoney } from '../lib/format.js'

const COLORS = {
  value: '#2a78d6',
  forecast: '#86b6ef',
  track: '#cde2fb',
  income: '#2a78d6',
  expense: '#eb6834',
  over: '#dc2626',
}

/**
 * Spent amount against a limit: solid for actual, light for forecast. Going over the limit is
 * said in words, not only by colour.
 */
export function Meter({ actual, forecast = 0, limit, label }) {
  const total = actual + forecast
  const scale = Math.max(limit || 0, total, 1)
  const over = limit !== null && limit !== undefined && total > limit
  return (
    <div>
      {label && (
        <div className="mb-1 flex items-baseline justify-between gap-2 text-sm">
          <span className="text-slate-700">{label}</span>
          <span className="text-slate-500">
            {formatMoney(actual)}
            {forecast > 0 && <> + {formatMoney(forecast)} prévu</>}
            {limit !== null && limit !== undefined && <> / {formatMoney(limit)}</>}
            {over && <span className="ml-1 font-semibold text-red-700">· dépassé</span>}
          </span>
        </div>
      )}
      <div className="flex h-2.5 w-full gap-0.5 overflow-hidden rounded-full" style={{ backgroundColor: COLORS.track }}
        role="img" aria-label={`${formatMoney(actual)} dépensés${limit ? ` sur ${formatMoney(limit)}` : ''}`}>
        <div className="h-full rounded-l-full" style={{ width: `${(actual / scale) * 100}%`,
          backgroundColor: over ? COLORS.over : COLORS.value }} />
        {forecast > 0 && <div className="h-full" style={{ width: `${(forecast / scale) * 100}%`,
          backgroundColor: COLORS.forecast }} />}
      </div>
    </div>
  )
}

/** Ranked magnitudes, one hue, value printed at the tip of each bar. */
export function BarList({ rows, format = String }) {
  const max = Math.max(1, ...rows.map((r) => r.value))
  return (
    <ul className="space-y-2">
      {rows.map((row) => (
        <li key={row.key} className="flex items-center gap-3 text-sm" title={`${row.label} : ${format(row.value)}`}>
          <span className="flex w-28 shrink-0 items-center gap-2 truncate text-slate-700">{row.icon}{row.label}</span>
          <span className="flex flex-1 items-center gap-2">
            <span className="h-3 rounded-r" style={{ width: `${(row.value / max) * 100}%`, minWidth: row.value ? 4 : 0,
              backgroundColor: COLORS.value }} />
            <span className="shrink-0 tabular-nums text-slate-600">{format(row.value)}</span>
          </span>
        </li>
      ))}
    </ul>
  )
}

/** Share of income per 50/30/20 group, with the target marked by a tick. */
export function TargetBars({ rows }) {
  return (
    <ul className="space-y-3">
      {rows.map((row) => (
        <li key={row.key} className="text-sm">
          <div className="mb-1 flex justify-between text-slate-700">
            <span>{row.label}</span>
            <span className="tabular-nums text-slate-500">
              {row.share === null ? '—' : `${row.share} %`} <span className="text-slate-400">(cible {row.target} %)</span>
            </span>
          </div>
          <div className="relative h-2.5 rounded-full" style={{ backgroundColor: COLORS.track }}>
            <div className="h-full rounded-full" style={{ width: `${Math.min(100, row.share ?? 0)}%`,
              backgroundColor: COLORS.value }} />
            <div className="absolute -top-1 w-0.5 bg-slate-700" style={{ left: `${row.target}%`, height: 18 }}
              aria-hidden="true" />
          </div>
        </li>
      ))}
    </ul>
  )
}

const MONTH_INITIALS = ['J', 'F', 'M', 'A', 'M', 'J', 'J', 'A', 'S', 'O', 'N', 'D']

/**
 * Income and expenses per month as paired columns (at most 16px wide, rounded top, square on the
 * axis), with a hover readout. Built with CSS so that labels keep their size on every screen.
 */
export function MonthlyColumns({ months }) {
  const [hover, setHover] = useState(null)
  const max = Math.max(1, ...months.flatMap((m) => [m.incomeActualCents, m.expenseActualCents]))
  const shown = hover === null ? null : months[hover]
  const bar = (cents, color) => (
    <div className="w-full max-w-4 rounded-t" style={{ height: `${(cents / max) * 100}%`, backgroundColor: color,
      minHeight: cents > 0 ? 2 : 0 }} />
  )
  return (
    <div onMouseLeave={() => setHover(null)}>
      <div className="mb-2 flex flex-wrap items-center gap-4 text-xs text-slate-600">
        <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-sm"
          style={{ backgroundColor: COLORS.income }} />Revenus</span>
        <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-sm"
          style={{ backgroundColor: COLORS.expense }} />Dépenses</span>
        <span className="ml-auto min-h-4 tabular-nums text-slate-700">
          {shown && <>{shown.month} · revenus {formatMoney(shown.incomeActualCents)} · dépenses {formatMoney(shown.expenseActualCents)}</>}
        </span>
      </div>
      <div className="flex h-40 border-b border-slate-300" role="img" aria-label="Revenus et dépenses approuvés par mois">
        {months.map((m, i) => (
          <div key={m.month} onMouseEnter={() => setHover(i)}
            className={`flex h-full flex-1 items-end justify-center gap-0.5 px-0.5 ${hover === i ? 'bg-slate-100' : ''}`}>
            {bar(m.incomeActualCents, COLORS.income)}
            {bar(m.expenseActualCents, COLORS.expense)}
          </div>
        ))}
      </div>
      <div className="flex text-xs text-slate-500">
        {months.map((m) => <span key={m.month} className="flex-1 pt-1 text-center">{MONTH_INITIALS[Number(m.month.slice(5, 7)) - 1]}</span>)}
      </div>
    </div>
  )
}
