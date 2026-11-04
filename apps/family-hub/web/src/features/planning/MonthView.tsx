// Month view: one cell per day with its first occurrences. Clicking a day creates an entry on it.
import type { Schemas } from '../../api/client.ts'
import { dayAgenda, isBanner } from '../../lib/calendar.ts'
import { combine, today } from '../../lib/dates.ts'
import { formatTime } from '../../lib/format.ts'
import { WEEKDAYS, entryColor } from '../../lib/labels.ts'

const SHOWN = 3

interface Props {
  weeks: string[][]
  month: string
  occurrences: Schemas['Occurrence'][]
  holidays: Record<string, string>
  onCreate: (start: string) => void
  onOpen: (occurrence: Schemas['Occurrence']) => void
}

export function MonthView({ weeks, month, occurrences, holidays, onCreate, onOpen }: Props) {
  const current = today()
  return (
    <div className="card overflow-x-auto p-0">
      <div className="min-w-[700px]">
        <div className="grid grid-cols-7 border-b border-line">
          {WEEKDAYS.map(([code, label]) => <div key={code} className="py-2 text-center text-xs font-semibold uppercase text-ink-3">{label}</div>)}
        </div>
        {weeks.map((week) => (
          <div key={week[0]} className="grid grid-cols-7 border-b border-line last:border-b-0">
            {week.map((day) => {
              const items = dayAgenda(occurrences, day)
              const inMonth = day.slice(0, 7) === month
              return (
                <div key={day} onClick={(e) => { if (e.target === e.currentTarget) onCreate(combine(day, '09:00')) }}
                  className={`min-h-28 cursor-pointer border-l border-line p-1 first:border-l-0 ${inMonth ? '' : 'bg-surface-2/60'}`}>
                  <div className="pointer-events-none mb-1 flex items-center justify-between gap-1">
                    <span className={`flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold
                      ${day === current ? 'bg-accent text-accent-ink' : inMonth ? '' : 'text-ink-3'}`}>{Number(day.slice(8))}</span>
                    {holidays[day] && <span className="truncate text-[10px] text-rose-600 dark:text-rose-300">{holidays[day]}</span>}
                  </div>
                  <div className="space-y-0.5">
                    {items.slice(0, SHOWN).map((o) => (
                      <button key={`${o.entry.id}-${o.start}`} type="button" onClick={() => onOpen(o)}
                        className={`block w-full truncate rounded border-l-4 px-1 text-left text-[11px] ${entryColor(o.entry)} ${o.entry.done ? 'line-through opacity-50' : ''}`}>
                        {!isBanner(o) && <span className="font-semibold">{formatTime(o.start)} </span>}{o.entry.title}
                      </button>
                    ))}
                    {items.length > SHOWN && <p className="px-1 text-[11px] text-ink-3">+{items.length - SHOWN} autres</p>}
                  </div>
                </div>
              )
            })}
          </div>
        ))}
      </div>
    </div>
  )
}
