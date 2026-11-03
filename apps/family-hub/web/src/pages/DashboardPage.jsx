// Home page: the coming week, who carries what, open lists, overdue to-dos and the budget of the month.
import { useResource } from '../lib/useResource.js'
import { useSession } from '../lib/session.js'
import { addDays, datePart } from '../lib/dates.js'
import { formatDate, formatDateTime, formatLongDate, formatMoney, formatMonth } from '../lib/format.js'
import { BarList, Meter } from '../components/charts.jsx'
import OccurrenceLabel from '../components/OccurrenceLabel.jsx'
import { Badge, Card, Empty, ErrorMessage, Loading, MemberDot, PageHeader } from '../components/ui.jsx'

function Agenda({ today, occurrences }) {
  const days = Array.from({ length: 7 }, (_, i) => addDays(today, i))
  if (!occurrences.length) return <Empty>Rien de prévu cette semaine.</Empty>
  return (
    <div className="divide-y divide-slate-100">
      {days.map((day) => {
        const items = occurrences.filter((o) => datePart(o.start) === day
          || (o.entry.allDay && datePart(o.start) < day && datePart(o.end) >= day))
        if (!items.length) return null
        return (
          <div key={day} className="py-2">
            <p className="text-xs font-semibold uppercase text-slate-500">
              {day === today ? "Aujourd'hui" : formatDate(day, { weekday: 'long', day: 'numeric', month: 'long' })}
            </p>
            {items.map((o) => <OccurrenceLabel key={`${o.entry.id}-${o.start}`} occurrence={o} />)}
          </div>
        )
      })}
    </div>
  )
}

function TodoList({ title, items, tone }) {
  const { members } = useSession()
  if (!items.length) return null
  return (
    <div>
      <p className={`mb-1 text-xs font-semibold uppercase ${tone}`}>{title}</p>
      <ul className="space-y-1">
        {items.map((t) => (
          <li key={t.id} className="flex items-center gap-2 text-sm">
            <span className="flex-1 truncate text-slate-800">{t.title}</span>
            <span className="text-xs text-slate-500">{formatDateTime(t.dueAt)}</span>
            <MemberDot member={members.find((m) => m.id === t.assigneeId)} size="h-5 w-5 text-[10px]" />
          </li>
        ))}
      </ul>
    </div>
  )
}

function Budget({ summary }) {
  return (
    <Card title={`Budget · ${formatMonth(summary.month)}`} actions={<a href="#/budget" className="text-sm text-indigo-600">Détails</a>}>
      <Meter label="Dépenses du mois" actual={summary.expenseActualCents} forecast={summary.expenseForecastCents}
        limit={summary.budgetTotalCents || null} />
      <div className="mt-4 flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2 text-sm">
        <span className="text-slate-700">Opérations à approuver</span>
        <a href="#/budget/approbations" className="font-semibold text-indigo-700">{summary.pendingApprovals}</a>
      </div>
      {summary.upcoming.length > 0 && (
        <div className="mt-4">
          <p className="mb-1 text-xs font-semibold uppercase text-slate-500">Prochaines échéances</p>
          <ul className="space-y-1 text-sm">
            {summary.upcoming.map((t) => (
              <li key={t.id} className="flex justify-between gap-2">
                <span className="truncate text-slate-700">{formatDate(t.day)} · {t.label}</span>
                <span className="tabular-nums text-slate-600">{formatMoney(t.amountCents)}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
      <div className="mt-4 space-y-1 border-t border-slate-100 pt-3 text-sm">
        {summary.accounts.map((a) => (
          <div key={a.id} className="flex justify-between">
            <span className="text-slate-600">{a.name}</span>
            <span className={`tabular-nums font-medium ${a.balanceCents < 0 ? 'text-red-700' : 'text-slate-800'}`}>
              {formatMoney(a.balanceCents)}
            </span>
          </div>
        ))}
      </div>
    </Card>
  )
}

export default function DashboardPage() {
  const { member } = useSession()
  const { data, error, loading } = useResource('/api/dashboard')
  if (loading) return <Loading />
  if (error) return <ErrorMessage error={error} />

  const loadRows = data.load.map((m) => ({
    key: m.memberId, label: m.name, value: m.entries + m.todos,
    icon: <MemberDot member={{ name: m.name, color: m.color }} size="h-4 w-4 text-[9px]" decorative />,
  }))
  return (
    <>
      <PageHeader title={`Bonjour ${member.name}`} subtitle={formatLongDate(data.today)} />
      <div className="grid gap-4 lg:grid-cols-3">
        <Card title="Les 7 prochains jours" className="lg:col-span-2"
          actions={<a href="#/planning" className="text-sm text-indigo-600">Planning</a>}>
          <Agenda today={data.today} occurrences={data.agenda} />
        </Card>

        <div className="space-y-4">
          <Card title="Qui porte quoi cette semaine">
            <BarList rows={loadRows} format={(v) => `${v} tâche${v > 1 ? 's' : ''}`} />
            <p className="mt-3 text-xs text-slate-500">Entrées du planning et tâches ouvertes assignées à chacun.</p>
          </Card>

          <Card title="À faire" actions={<a href="#/todo" className="text-sm text-indigo-600">Listes</a>}>
            {!data.overdueTodos.length && !data.dueSoonTodos.length
              ? <Empty>Aucune échéance proche.</Empty>
              : (
                <div className="space-y-3">
                  <TodoList title="En retard" items={data.overdueTodos} tone="text-red-700" />
                  <TodoList title="Dans les 48 heures" items={data.dueSoonTodos} tone="text-slate-500" />
                </div>
              )}
          </Card>
        </div>

        <Card title="Courses" actions={<a href="#/courses" className="text-sm text-indigo-600">Listes</a>}>
          {data.shoppingLists.length === 0 ? <Empty>Aucune liste en cours.</Empty> : (
            <ul className="space-y-3">
              {data.shoppingLists.map((l) => (
                <li key={l.id}>
                  <a href={`#/courses/${l.id}`} className="flex items-center justify-between gap-2 text-sm">
                    <span className="truncate font-medium text-slate-800">{l.name}</span>
                    <span className="text-slate-500">{l.checkedCount}/{l.itemCount}</span>
                  </a>
                  <p className="text-xs text-slate-500">
                    {l.plannedFor ? `${formatDate(l.plannedFor)} · ` : ''}environ {formatMoney(l.estimatedTotalCents)}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card title="Repas de la semaine" actions={<a href="#/planning" className="text-sm text-indigo-600">Planifier</a>}>
          <p className="text-3xl font-bold text-slate-800">{data.plannedMeals}</p>
          <p className="text-sm text-slate-500">repas planifiés sur 7 jours</p>
          {data.plannedMeals === 0 && <Badge className="mt-2 bg-amber-50 text-amber-800">Planifier les repas évite les achats impulsifs</Badge>}
        </Card>

        {data.accounting && <Budget summary={data.accounting} />}
      </div>
    </>
  )
}
