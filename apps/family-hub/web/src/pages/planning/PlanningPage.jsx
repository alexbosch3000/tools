// Planning page: week or month view of the family's entries, member filter, meals turned into shopping.
import { useCallback, useMemo, useState } from 'react'
import { post } from '../../lib/api.js'
import { addDays, addMonths, monthGrid, nowLocal, today, weekDays } from '../../lib/dates.js'
import { formatDate, formatMonth } from '../../lib/format.js'
import { navigate } from '../../lib/router.js'
import { useResource } from '../../lib/useResource.js'
import { useSession } from '../../lib/session.js'
import { useSubmit } from '../../lib/useSubmit.js'
import { Button, ErrorMessage, MemberDot, PageHeader } from '../../components/ui.jsx'
import EntryModal from './EntryModal.jsx'
import MonthView from './MonthView.jsx'
import WeekView from './WeekView.jsx'

function useHolidays(years) {
  const first = useResource(`/api/planning/holidays?year=${years[0]}`)
  const second = useResource(years[1] !== years[0] ? `/api/planning/holidays?year=${years[1]}` : null)
  return useMemo(() => Object.fromEntries([...(first.data ?? []), ...(second.data ?? [])].map((h) => [h.day, h.name])),
    [first.data, second.data])
}

export default function PlanningPage() {
  const { members } = useSession()
  const [view, setView] = useState('week')
  const [anchor, setAnchor] = useState(today())
  const [filter, setFilter] = useState([])
  const [modal, setModal] = useState(null)
  const meals = useSubmit()

  const days = weekDays(anchor)
  const weeks = monthGrid(anchor)
  const from = view === 'week' ? days[0] : weeks[0][0]
  const to = view === 'week' ? days[6] : weeks.at(-1)[6]
  const occurrences = useResource(`/api/planning/occurrences?from=${from}&to=${to}`)
  const holidays = useHolidays([Number(from.slice(0, 4)), Number(to.slice(0, 4))])

  const visible = (occurrences.data ?? []).filter((o) => !filter.length || o.entry.memberIds.some((id) => filter.includes(id)))
  const move = (step) => setAnchor(view === 'week' ? addDays(anchor, 7 * step) : addMonths(anchor, step))
  const close = useCallback(() => setModal(null), [])
  const saved = () => { setModal(null); occurrences.reload() }

  const prepareShopping = () => meals.run(async () => {
    const result = await post('/api/planning/meals/shopping', { from: days[0], to: days[6] })
    navigate(`/courses/${result.shoppingListId}`)
  })

  return (
    <>
      <PageHeader title="Planning"
        subtitle={view === 'week' ? `Semaine du ${formatDate(days[0], { day: 'numeric', month: 'long' })}` : formatMonth(anchor.slice(0, 7))}
        actions={(
          <>
            {view === 'week' && <Button onClick={prepareShopping} disabled={meals.busy}
              title="Ajoute les recettes des repas de la semaine à une nouvelle liste de courses">Courses des repas</Button>}
            <Button variant="primary" onClick={() => setModal({ start: nowLocal().slice(0, 14) + '00' })}>Nouvelle entrée</Button>
          </>
        )} />

      <div className="mb-4 flex flex-wrap items-center gap-2">
        <div className="flex rounded-lg bg-slate-100 p-1">
          {[['week', 'Semaine'], ['month', 'Mois']].map(([code, label]) => (
            <button key={code} type="button" onClick={() => setView(code)}
              className={`rounded-md px-3 py-1 text-sm ${view === code ? 'bg-white font-medium shadow' : 'text-slate-600'}`}>{label}</button>
          ))}
        </div>
        <Button variant="ghost" onClick={() => move(-1)} aria-label="Précédent">‹</Button>
        <Button onClick={() => setAnchor(today())}>Aujourd'hui</Button>
        <Button variant="ghost" onClick={() => move(1)} aria-label="Suivant">›</Button>
        <div className="ml-auto flex flex-wrap gap-1">
          {members.map((m) => {
            const active = filter.includes(m.id)
            return (
              <button key={m.id} type="button" aria-pressed={active} title={`Afficher ${m.name}`}
                onClick={() => setFilter(active ? filter.filter((id) => id !== m.id) : [...filter, m.id])}
                className={`rounded-full p-0.5 ${active ? 'ring-2 ring-indigo-500' : 'opacity-70'}`}>
                <MemberDot member={m} />
              </button>
            )
          })}
        </div>
      </div>
      <ErrorMessage error={meals.error ?? occurrences.error} />

      {view === 'week'
        ? <WeekView days={days} occurrences={visible} holidays={holidays}
            onCreate={(start) => setModal({ start })} onOpen={(occurrence) => setModal({ occurrence })} />
        : <MonthView weeks={weeks} month={anchor.slice(0, 7)} occurrences={visible} holidays={holidays}
            onCreate={(start) => setModal({ start })} onOpen={(occurrence) => setModal({ occurrence })} />}

      {modal && <EntryModal occurrence={modal.occurrence} start={modal.start} onClose={close} onSaved={saved} />}
    </>
  )
}
