// Vue calendrier mensuel — tâches planifiées avec barres multidays
// Sélection par glisser pour créer une tâche sur une plage de dates
import { useState, useEffect, useRef } from 'react'
import { today } from '../utils/dates.js'

const WEEKDAYS = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim']
const MONTHS   = ['Janvier','Février','Mars','Avril','Mai','Juin','Juillet','Août','Septembre','Octobre','Novembre','Décembre']

const PRIORITY = {
  low:    { bg: '#e2e8f0', bar: '#94a3b8', text: '#475569' },
  medium: { bg: '#fef3c7', bar: '#f59e0b', text: '#b45309' },
  high:   { bg: '#fee2e2', bar: '#ef4444', text: '#b91c1c' },
}

const HEADER_H  = 30  // hauteur zone numéro de jour (px)
const LANE_H    = 22  // hauteur d'une ligne de tâche (px)
const MAX_LANES = 3   // lignes visibles avant indicateur débordement

// Grille de 6 semaines, lundi en premier
function buildWeeks(year, month) {
  const first = new Date(year, month, 1)
  const start = new Date(first)
  const dow   = first.getDay()
  start.setDate(first.getDate() - (dow === 0 ? 6 : dow - 1))

  return Array.from({ length: 6 }, (_, w) =>
    Array.from({ length: 7 }, (_, d) => {
      const date = new Date(start)
      date.setDate(start.getDate() + w * 7 + d)
      return date.toISOString().slice(0, 10)
    })
  )
}

// Calcule le layout (colonne + lane) des tâches d'une semaine
function computeWeekLayout(tasks, weekDays) {
  const [wStart, wEnd] = [weekDays[0], weekDays[6]]

  const placed = tasks
    .filter(t => {
      const s = t.start || t.end
      const e = t.end   || t.start
      return s && s <= wEnd && e >= wStart
    })
    .map(t => {
      const s = t.start || t.end
      const e = t.end   || t.start
      const startCol = s < wStart ? 0 : weekDays.indexOf(s)
      const endCol   = e > wEnd   ? 6 : weekDays.indexOf(e)
      return { ...t, startCol, endCol, startsHere: s >= wStart, endsHere: e <= wEnd }
    })
    .filter(t => t.startCol >= 0 && t.endCol >= 0)
    .sort((a, b) =>
      a.startCol !== b.startCol
        ? a.startCol - b.startCol
        : (b.endCol - b.startCol) - (a.endCol - a.startCol)
    )

  // Affectation greedy des lanes (évite les chevauchements visuels)
  const occupied = []
  return placed.map(task => {
    let lane = 0
    while ((occupied[lane] || []).some(r => r.startCol <= task.endCol && r.endCol >= task.startCol)) {
      lane++
    }
    ;(occupied[lane] = occupied[lane] || []).push({ startCol: task.startCol, endCol: task.endCol })
    return { ...task, lane }
  })
}

export default function CalendarView({ tasks, onNewTask, onEditTask }) {
  const now = new Date()
  const [year, setYear]   = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth())
  const [sel, setSel]     = useState(null)   // { start, end } dates ISO affichées
  const selRef            = useRef(null)      // version ref pour éviter les closures périmées

  const weeks    = buildWeeks(year, month)
  const todayStr = today()
  const scheduled = tasks.filter(t => t.start || t.end)

  function prevMonth() {
    month === 0 ? (setYear(y => y - 1), setMonth(11)) : setMonth(m => m - 1)
  }
  function nextMonth() {
    month === 11 ? (setYear(y => y + 1), setMonth(0)) : setMonth(m => m + 1)
  }

  function startSel(date) {
    const s = { start: date, end: date }
    selRef.current = s
    setSel(s)
  }
  function extendSel(date) {
    if (!selRef.current) return
    const s = { ...selRef.current, end: date }
    selRef.current = s
    setSel(s)
  }

  // Mouseup global pour capter le relâchement hors du calendrier
  useEffect(() => {
    function onUp() {
      if (!selRef.current) return
      const { start, end } = selRef.current
      selRef.current = null
      setSel(null)
      const [s, e] = start <= end ? [start, end] : [end, start]
      onNewTask(s, e)
    }
    window.addEventListener('mouseup', onUp)
    return () => window.removeEventListener('mouseup', onUp)
  }, [onNewTask])

  const selMin = sel ? [sel.start, sel.end].sort()[0] : null
  const selMax = sel ? [sel.start, sel.end].sort()[1] : null

  return (
    <div className="select-none">

      {/* Navigation mois */}
      <div className="flex items-center gap-2 mb-4">
        <button
          onClick={prevMonth}
          className="p-2 rounded-lg hover:bg-slate-100 text-slate-500 hover:text-slate-800 transition-colors"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
            <path d="M15 18l-6-6 6-6"/>
          </svg>
        </button>

        <span className="font-bold text-slate-800 w-52 text-center text-lg">
          {MONTHS[month]} {year}
        </span>

        <button
          onClick={nextMonth}
          className="p-2 rounded-lg hover:bg-slate-100 text-slate-500 hover:text-slate-800 transition-colors"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
            <path d="M9 18l6-6-6-6"/>
          </svg>
        </button>

        <button
          onClick={() => { setYear(now.getFullYear()); setMonth(now.getMonth()) }}
          className="ml-2 px-3 py-1.5 text-xs font-semibold bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-full transition-colors"
        >
          Aujourd'hui
        </button>
      </div>

      {/* Grille calendrier */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">

        {/* En-têtes jours */}
        <div className="grid grid-cols-7 border-b border-slate-100">
          {WEEKDAYS.map((d, i) => (
            <div
              key={d}
              className={`py-2.5 text-center text-xs font-bold uppercase tracking-wide ${
                i >= 5 ? 'text-slate-300' : 'text-slate-400'
              }`}
            >
              {d}
            </div>
          ))}
        </div>

        {/* Semaines */}
        {weeks.map((weekDays, wi) => {
          const layout     = computeWeekLayout(scheduled, weekDays)
          const maxLane    = layout.reduce((m, t) => Math.max(m, t.lane), -1)
          const visLanes   = Math.min(maxLane + 1, MAX_LANES)
          const overflow   = layout.filter(t => t.lane >= MAX_LANES)
          const rowHeight  = HEADER_H + visLanes * LANE_H + (overflow.length ? LANE_H : 0) + 8

          return (
            <div
              key={wi}
              className="relative border-b border-slate-100 last:border-0"
              style={{ height: rowHeight }}
            >
              {/* Cellules jours */}
              <div className="absolute inset-0 grid grid-cols-7">
                {weekDays.map((date, di) => {
                  const inMonth  = new Date(date).getMonth() === month
                  const isToday  = date === todayStr
                  const inSel    = selMin && date >= selMin && date <= selMax
                  const isWkend  = di >= 5
                  const dayNum   = new Date(date).getDate()

                  return (
                    <div
                      key={date}
                      className={`border-r border-slate-100 last:border-0 cursor-pointer transition-colors duration-75 ${
                        inSel   ? 'bg-indigo-50' :
                        isToday ? 'bg-indigo-50/30' :
                        isWkend ? 'bg-slate-50/70' :
                        'hover:bg-slate-50'
                      }`}
                      onMouseDown={() => startSel(date)}
                      onMouseEnter={() => extendSel(date)}
                    >
                      <div
                        className="flex justify-end pr-2 pt-1.5"
                        style={{ height: HEADER_H }}
                      >
                        {isToday ? (
                          <span className="w-6 h-6 flex items-center justify-center rounded-full bg-indigo-600 text-white text-xs font-bold">
                            {dayNum}
                          </span>
                        ) : (
                          <span className={`text-xs font-semibold ${
                            !inMonth ? 'text-slate-200' :
                            isWkend  ? 'text-slate-400' :
                                       'text-slate-600'
                          }`}>
                            {dayNum}
                          </span>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>

              {/* Barres de tâches */}
              {layout
                .filter(t => t.lane < MAX_LANES)
                .map(t => {
                  const p      = PRIORITY[t.priority] || PRIORITY.medium
                  const leftPct  = (t.startCol / 7) * 100
                  const widthPct = ((t.endCol - t.startCol + 1) / 7) * 100
                  const top      = HEADER_H + t.lane * LANE_H + 2
                  const radius   = t.startsHere && t.endsHere ? '4px'
                                 : t.startsHere               ? '4px 0 0 4px'
                                 : t.endsHere                 ? '0 4px 4px 0'
                                 : '0'
                  return (
                    <div
                      key={`${t.id}-w${wi}`}
                      className={`absolute flex items-center px-1.5 text-xs font-semibold cursor-pointer hover:brightness-95 transition-opacity ${t.done ? 'opacity-40' : ''}`}
                      style={{
                        left:         `calc(${leftPct}% + 2px)`,
                        width:        `calc(${widthPct}% - 4px)`,
                        top,
                        height:       LANE_H - 3,
                        background:   p.bg,
                        borderLeft:   t.startsHere ? `3px solid ${p.bar}` : 'none',
                        borderRadius: radius,
                        color:        p.text,
                      }}
                      onClick={e => { e.stopPropagation(); onEditTask(t.id) }}
                      title={t.title}
                    >
                      <span className="truncate leading-none">
                        {!t.startsHere && '↳ '}{t.title}
                      </span>
                    </div>
                  )
                })}

              {/* Indicateurs débordement (+N) par colonne */}
              {overflow.length > 0 && weekDays.map((date, di) => {
                const hidden = overflow.filter(t => t.startCol <= di && t.endCol >= di)
                if (!hidden.length) return null
                return (
                  <div
                    key={`ov-${date}`}
                    className="absolute flex items-center px-2 text-xs font-semibold text-slate-400"
                    style={{
                      left:   `calc(${(di / 7) * 100}% + 2px)`,
                      width:  `calc(${(1 / 7) * 100}% - 4px)`,
                      top:    HEADER_H + MAX_LANES * LANE_H + 2,
                      height: LANE_H - 3,
                    }}
                  >
                    +{hidden.length}
                  </div>
                )
              })}
            </div>
          )
        })}
      </div>

      <p className="text-center text-xs text-slate-300 mt-3">
        Cliquez sur un jour · Glissez sur une plage pour créer une tâche avec dates pré-remplies
      </p>
    </div>
  )
}
