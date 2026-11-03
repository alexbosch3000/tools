// Month view: one cell per day with its first occurrences. Clicking a day creates an entry on it.
import { dayAgenda, isBanner } from '../../lib/calendar.js'
import { combine, today } from '../../lib/dates.js'
import { formatTime } from '../../lib/format.js'
import { WEEKDAYS, entryColor } from '../../lib/labels.js'

const SHOWN = 3

export default function MonthView({ weeks, month, occurrences, holidays, onCreate, onOpen }) {
  const current = today()
  return (
    <div className="card overflow-x-auto p-0">
      <div className="min-w-[700px]">
        <div className="grid grid-cols-7 border-b border-slate-200">
          {WEEKDAYS.map(([code, label]) => (
            <div key={code} className="py-2 text-center text-xs font-semibold uppercase text-slate-500">{label}</div>
          ))}
        </div>
        {weeks.map((week) => (
          <div key={week[0]} className="grid grid-cols-7 border-b border-slate-100 last:border-b-0">
            {week.map((day) => {
              const items = dayAgenda(occurrences, day)
              return (
                <div key={day} onClick={(e) => { if (e.target === e.currentTarget) onCreate(combine(day, '09:00')) }}
                  className={`min-h-28 cursor-pointer border-l border-slate-100 p-1 first:border-l-0
                    ${day.slice(0, 7) !== month ? 'bg-slate-50/70' : ''}`}>
                  <div className="pointer-events-none mb-1 flex items-center justify-between gap-1">
                    <span className={`flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold
                      ${day === current ? 'bg-indigo-600 text-white' : day.slice(0, 7) === month ? 'text-slate-700' : 'text-slate-400'}`}>
                      {Number(day.slice(8))}
                    </span>
                    {holidays[day] && <span className="truncate text-[10px] text-rose-700">{holidays[day]}</span>}
                  </div>
                  <div className="space-y-0.5">
                    {items.slice(0, SHOWN).map((o) => (
                      <button key={`${o.entry.id}-${o.start}`} type="button" onClick={() => onOpen(o)}
                        className={`block w-full truncate rounded border-l-4 px-1 text-left text-[11px] ${entryColor(o.entry)}
                          ${o.entry.done ? 'opacity-50 line-through' : ''}`}>
                        {!isBanner(o) && <span className="font-semibold">{formatTime(o.start)} </span>}{o.entry.title}
                      </button>
                    ))}
                    {items.length > SHOWN && <p className="px-1 text-[11px] text-slate-500">+{items.length - SHOWN} autres</p>}
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
