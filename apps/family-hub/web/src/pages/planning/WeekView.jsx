// Week view: all-day band on top, minute-precision time grid below. Clicking a free slot creates an entry.
import { useEffect, useRef } from 'react'
import { isBanner, layoutDay, touchesDay } from '../../lib/calendar.js'
import { combine, today } from '../../lib/dates.js'
import { formatDate, formatTime } from '../../lib/format.js'
import { entryColor } from '../../lib/labels.js'

const HOUR_PX = 44
const pad = (n) => String(n).padStart(2, '0')

/** Time of a click in the grid, rounded down to the quarter hour. */
function slotTime(event) {
  const rect = event.currentTarget.getBoundingClientRect()
  const minutes = Math.max(0, Math.min(23 * 60 + 45, Math.floor(((event.clientY - rect.top) / HOUR_PX) * 4) * 15))
  return `${pad(Math.floor(minutes / 60))}:${pad(minutes % 60)}`
}

export default function WeekView({ days, occurrences, holidays, onCreate, onOpen }) {
  const scroller = useRef(null)
  useEffect(() => { if (scroller.current) scroller.current.scrollTop = 7 * HOUR_PX }, [])
  const current = today()

  return (
    <div className="card overflow-x-auto p-0">
      <div className="min-w-[760px]">
        <div className="grid grid-cols-[3rem_repeat(7,1fr)] border-b border-slate-200">
          <div />
          {days.map((day) => (
            <div key={day} className={`border-l border-slate-100 px-2 py-2 text-center ${day === current ? 'bg-indigo-50' : ''}`}>
              <p className="text-xs uppercase text-slate-500">{formatDate(day, { weekday: 'short' })}</p>
              <p className={`text-lg font-semibold ${day === current ? 'text-indigo-700' : 'text-slate-800'}`}>{Number(day.slice(8))}</p>
              {holidays[day] && <p className="truncate text-[11px] text-rose-700">{holidays[day]}</p>}
            </div>
          ))}
        </div>

        <div className="grid grid-cols-[3rem_repeat(7,1fr)] border-b border-slate-200">
          <div className="py-1 pr-1 text-right text-[10px] text-slate-400">jour</div>
          {days.map((day) => (
            <div key={day} className="min-h-8 space-y-0.5 border-l border-slate-100 p-0.5">
              {occurrences.filter((o) => isBanner(o) && touchesDay(o, day)).map((o) => (
                <button key={`${o.entry.id}-${o.start}`} type="button" onClick={() => onOpen(o)}
                  className={`block w-full truncate rounded border-l-4 px-1 text-left text-xs ${entryColor(o.entry)}`}>
                  {o.entry.title}
                </button>
              ))}
            </div>
          ))}
        </div>

        <div ref={scroller} className="h-[560px] overflow-y-auto">
          <div className="relative grid grid-cols-[3rem_repeat(7,1fr)]" style={{ height: 24 * HOUR_PX }}>
            <div className="relative">
              {Array.from({ length: 24 }, (_, h) => (
                <span key={h} className="absolute right-1 -translate-y-1/2 text-[10px] text-slate-400" style={{ top: h * HOUR_PX }}>
                  {h > 0 && `${pad(h)}:00`}
                </span>
              ))}
            </div>
            {days.map((day) => (
              <div key={day} className="relative cursor-pointer border-l border-slate-100"
                onClick={(e) => { if (e.target === e.currentTarget) onCreate(combine(day, slotTime(e))) }}
                style={{ backgroundImage: `repeating-linear-gradient(to bottom, #f1f5f9 0, #f1f5f9 1px, transparent 1px, transparent ${HOUR_PX}px)` }}>
                {layoutDay(occurrences, day).map(({ occurrence: o, top, bottom, column, columns }) => (
                  <button key={`${o.entry.id}-${o.start}`} type="button" onClick={() => onOpen(o)}
                    title={`${formatTime(o.start)}–${formatTime(o.end)} ${o.entry.title}`}
                    className={`absolute overflow-hidden rounded-md border-l-4 px-1 text-left text-xs leading-tight shadow-sm
                      ${entryColor(o.entry)} ${o.entry.done ? 'opacity-50 line-through' : ''}`}
                    style={{ top: (top / 60) * HOUR_PX, height: Math.max(18, ((bottom - top) / 60) * HOUR_PX - 2),
                      left: `calc(${(column / columns) * 100}% + 1px)`, width: `calc(${100 / columns}% - 2px)` }}>
                    <span className="font-semibold">{formatTime(o.start)}</span> {o.entry.title}
                  </button>
                ))}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
