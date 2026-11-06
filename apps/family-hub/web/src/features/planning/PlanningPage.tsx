// Planning page: week or month view of the family's entries, member filter, meals turned into shopping.
import { useState } from 'react'
import { useQueries, useQuery } from '@tanstack/react-query'
import { useNavigate } from '@tanstack/react-router'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useSession } from '../../app/useSession.ts'
import { useAction } from '../../app/useAction.ts'
import { addDays, addMonths, monthGrid, nowLocal, today, weekDays } from '../../lib/dates.ts'
import { entryItem, taskItem, type CalendarItem } from '../../lib/calendar.ts'
import { formatDate, formatMonth } from '../../lib/format.ts'
import { Button, ErrorMessage, MemberDot, PageHeader } from '../../components/ui.tsx'
import { ItemDialog } from '../todo/ItemDialog.tsx'
import { EntryDialog } from './EntryDialog.tsx'
import { MonthView } from './MonthView.tsx'
import { WeekView } from './WeekView.tsx'

function useHolidays(years: number[]): Record<string, string> {
  return useQueries({
    queries: [...new Set(years)].map((year) => ({
      queryKey: ['planning', 'holidays', year], staleTime: Infinity,
      queryFn: () => unwrap(api().GET('/api/planning/holidays', { params: { query: { year } } })),
    })),
    combine: (results) => Object.fromEntries(results.flatMap((r) => r.data ?? []).map((h) => [h.day, h.name])),
  })
}

type Modal = { occurrence: Schemas['Occurrence'] } | { task: Schemas['TodoItem'] } | { start: string } | null

export function PlanningPage() {
  const { members } = useSession()
  const navigate = useNavigate()
  const [view, setView] = useState<'week' | 'month'>('week')
  const [anchor, setAnchor] = useState(today())
  const [filter, setFilter] = useState<number[]>([])
  const [modal, setModal] = useState<Modal>(null)

  const days = weekDays(anchor)
  const weeks = monthGrid(anchor)
  const from = (view === 'week' ? days[0] : weeks[0]?.[0]) ?? anchor
  const to = (view === 'week' ? days[6] : weeks.at(-1)?.[6]) ?? anchor
  const occurrences = useQuery({
    queryKey: ['planning', 'occurrences', from, to], placeholderData: (previous) => previous,
    queryFn: () => unwrap(api().GET('/api/planning/occurrences', { params: { query: { from, to } } })),
  })
  const tasks = useQuery({
    queryKey: ['todo', 'scheduled', from, to], placeholderData: (previous) => previous,
    queryFn: () => unwrap(api().GET('/api/todo/scheduled', { params: { query: { from, to } } })),
  })
  const lists = useQuery({ queryKey: ['todo', 'lists'], queryFn: () => unwrap(api().GET('/api/todo/lists')) })
  const holidays = useHolidays([Number(from.slice(0, 4)), Number(to.slice(0, 4))])
  const entries = (occurrences.data ?? [])
    .filter((o) => !filter.length || o.entry.memberIds.some((id) => filter.includes(id))).map(entryItem)
  const dueTasks = (tasks.data ?? [])
    .filter((t) => !filter.length || (t.assigneeId !== null && t.assigneeId !== undefined && filter.includes(t.assigneeId)))
    .map(taskItem).filter((item): item is CalendarItem => item !== null)
  const visible = [...entries, ...dueTasks]
  const open = (item: CalendarItem) => setModal(item.kind === 'entry' ? { occurrence: item.occurrence } : { task: item.task })
  const move = (step: number) => setAnchor(view === 'week' ? addDays(anchor, 7 * step) : addMonths(anchor, step))

  const mealShopping = useAction(() => unwrap(api().POST('/api/planning/meals/shopping', { body: { from: days[0] ?? from, to: days[6] ?? to } })), {
    refresh: ['shopping', 'dashboard'],
    success: (r) => `Liste créée avec ${r.meals} repas`,
    onSuccess: (r) => navigate({ to: '/courses/$listId', params: { listId: String(r.shoppingListId) } }),
  })

  return (
    <>
      <PageHeader title="Planning"
        subtitle={view === 'week' ? `Semaine du ${formatDate(days[0], { day: 'numeric', month: 'long' })}` : formatMonth(anchor.slice(0, 7))}
        actions={(
          <>
            {view === 'week' && (
              <Button onClick={() => mealShopping.mutate(undefined)} disabled={mealShopping.isPending}
                title="Ajoute les recettes des repas de la semaine à une nouvelle liste de courses">Courses des repas</Button>
            )}
            <Button variant="primary" onClick={() => setModal({ start: `${nowLocal().slice(0, 14)}00` })}>Nouvelle entrée</Button>
          </>
        )} />
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <div role="radiogroup" aria-label="Vue" className="flex rounded-lg bg-surface-2 p-1">
          {([['week', 'Semaine'], ['month', 'Mois']] as const).map(([code, label]) => (
            <button key={code} type="button" role="radio" aria-checked={view === code} onClick={() => setView(code)}
              className={`rounded-md px-3 py-1 text-sm ${view === code ? 'bg-surface font-medium shadow' : 'text-ink-2'}`}>{label}</button>
          ))}
        </div>
        <Button variant="ghost" onClick={() => move(-1)} aria-label={view === 'week' ? 'Semaine précédente' : 'Mois précédent'}>‹</Button>
        <Button onClick={() => setAnchor(today())}>Aujourd'hui</Button>
        <Button variant="ghost" onClick={() => move(1)} aria-label={view === 'week' ? 'Semaine suivante' : 'Mois suivant'}>›</Button>
        <div className="ml-auto flex flex-wrap gap-1" role="group" aria-label="Filtrer par membre">
          {members.map((m) => {
            const active = filter.includes(m.id)
            return (
              <button key={m.id} type="button" aria-pressed={active} aria-label={`Afficher seulement ${m.name}`}
                onClick={() => setFilter(active ? filter.filter((id) => id !== m.id) : [...filter, m.id])}
                className={`rounded-full p-0.5 ${active ? 'ring-2 ring-accent' : 'opacity-70'}`}>
                <MemberDot member={m} decorative />
              </button>
            )
          })}
        </div>
      </div>
      <ErrorMessage error={occurrences.error ?? tasks.error} />
      {view === 'week'
        ? <WeekView days={days} items={visible} holidays={holidays} onCreate={(start) => setModal({ start })} onOpen={open} />
        : <MonthView weeks={weeks} month={anchor.slice(0, 7)} items={visible} holidays={holidays}
            onCreate={(start) => setModal({ start })} onOpen={open} />}
      {modal && 'task' in modal && lists.data && (
        <ItemDialog item={modal.task} listId={modal.task.listId} lists={lists.data} onClose={() => setModal(null)} />
      )}
      {modal && !('task' in modal) && <EntryDialog {...modal} onClose={() => setModal(null)} />}
    </>
  )
}
