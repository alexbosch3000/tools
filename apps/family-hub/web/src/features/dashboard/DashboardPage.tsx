// Home page: the coming week, who carries what, open lists, overdue to-dos and the budget of the month.
import { Link } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useSession } from '../../app/useSession.ts'
import { addDays, datePart } from '../../lib/dates.ts'
import { formatDate, formatDateTime, formatLongDate, formatMoney, formatMonth, plural } from '../../lib/format.ts'
import { BarList, Meter } from '../../components/charts.tsx'
import { OccurrenceLabel } from '../../components/OccurrenceLabel.tsx'
import { Badge, Card, Empty, ErrorMessage, Loading, MemberDot, PageHeader } from '../../components/ui.tsx'

function Agenda({ today, occurrences }: { today: string, occurrences: Schemas['Occurrence'][] }) {
  if (!occurrences.length) return <Empty>Rien de prévu cette semaine.</Empty>
  return (
    <div className="divide-y divide-line">
      {Array.from({ length: 7 }, (_, i) => addDays(today, i)).map((day) => {
        const items = occurrences.filter((o) => datePart(o.start) === day
          || (o.entry.allDay && datePart(o.start) < day && datePart(o.end) >= day))
        if (!items.length) return null
        return (
          <div key={day} className="py-2">
            <p className="text-xs font-semibold uppercase text-ink-3">
              {day === today ? "Aujourd'hui" : formatDate(day, { weekday: 'long', day: 'numeric', month: 'long' })}
            </p>
            {items.map((o) => <OccurrenceLabel key={`${o.entry.id}-${o.start}`} occurrence={o} />)}
          </div>
        )
      })}
    </div>
  )
}

function Todos({ title, items, tone }: { title: string, items: Schemas['TodoItem'][], tone: string }) {
  const { members } = useSession()
  if (!items.length) return null
  return (
    <div>
      <p className={`mb-1 text-xs font-semibold uppercase ${tone}`}>{title}</p>
      <ul className="space-y-1">
        {items.map((t) => (
          <li key={t.id} className="flex items-center gap-2 text-sm">
            <span className="flex-1 truncate">{t.title}</span>
            <span className="text-xs text-ink-3">{formatDateTime(t.dueAt)}</span>
            <MemberDot member={members.find((m) => m.id === t.assigneeId)} size="h-5 w-5 text-[10px]" />
          </li>
        ))}
      </ul>
    </div>
  )
}

function Budget({ summary }: { summary: Schemas['AccountingSummary'] }) {
  return (
    <Card title={`Budget · ${formatMonth(summary.month)}`}
      actions={<Link to="/budget/$tab" params={{ tab: 'bilan' }} className="text-sm text-accent">Détails</Link>}>
      <Meter label="Dépenses du mois" actual={summary.expenseActualCents} forecast={summary.expenseForecastCents}
        limit={summary.budgetTotalCents || null} />
      <Link to="/budget/$tab" params={{ tab: 'approbations' }}
        className="mt-4 flex items-center justify-between rounded-lg bg-surface-2 px-3 py-2 text-sm">
        <span>Opérations à approuver</span>
        <span className="font-semibold text-accent">{summary.pendingApprovals}</span>
      </Link>
      {summary.upcoming.length > 0 && (
        <div className="mt-4">
          <p className="mb-1 text-xs font-semibold uppercase text-ink-3">Prochaines échéances</p>
          <ul className="space-y-1 text-sm">
            {summary.upcoming.map((t) => (
              <li key={t.id} className="flex justify-between gap-2">
                <span className="truncate">{formatDate(t.day)} · {t.label}</span>
                <span className="tabular-nums text-ink-2">{formatMoney(t.amountCents)}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
      <div className="mt-4 space-y-1 border-t border-line pt-3 text-sm">
        {summary.accounts.map((a) => (
          <div key={a.id} className="flex justify-between">
            <span className="text-ink-2">{a.name}</span>
            <span className={`font-medium tabular-nums ${a.balanceCents < 0 ? 'text-danger' : ''}`}>{formatMoney(a.balanceCents)}</span>
          </div>
        ))}
      </div>
    </Card>
  )
}

export function DashboardPage() {
  const { member } = useSession()
  const dashboard = useQuery({ queryKey: ['dashboard'], queryFn: () => unwrap(api().GET('/api/dashboard')) })
  if (dashboard.isPending) return <Loading lines={6} />
  if (dashboard.isError) return <ErrorMessage error={dashboard.error} />
  const data = dashboard.data
  const load = data.load.map((m) => ({
    key: m.memberId, label: m.name, value: m.entries + m.todos,
    icon: <MemberDot member={m} size="h-4 w-4 text-[9px]" decorative />,
  }))

  return (
    <>
      <PageHeader title={`Bonjour ${member.name}`} subtitle={formatLongDate(data.today)} />
      <div className="grid gap-4 lg:grid-cols-3">
        <Card title="Les 7 prochains jours" className="lg:col-span-2"
          actions={<Link to="/planning" className="text-sm text-accent">Planning</Link>}>
          <Agenda today={data.today} occurrences={data.agenda} />
        </Card>
        <div className="space-y-4">
          <Card title="Qui porte quoi cette semaine">
            <BarList rows={load} format={(v) => plural(v, 'tâche')} />
            <p className="mt-3 text-xs text-ink-3">Entrées du planning et tâches ouvertes assignées à chacun.</p>
          </Card>
          <Card title="À faire" actions={<Link to="/todo" className="text-sm text-accent">Listes</Link>}>
            {!data.overdueTodos.length && !data.dueSoonTodos.length ? <Empty>Aucune échéance proche.</Empty> : (
              <div className="space-y-3">
                <Todos title="En retard" items={data.overdueTodos} tone="text-danger" />
                <Todos title="Dans les 48 heures" items={data.dueSoonTodos} tone="text-ink-3" />
              </div>
            )}
          </Card>
        </div>
        <Card title="Courses" actions={<Link to="/courses" className="text-sm text-accent">Listes</Link>}>
          {data.shoppingLists.length === 0 ? <Empty>Aucune liste en cours.</Empty> : (
            <ul className="space-y-3">
              {data.shoppingLists.map((l) => (
                <li key={l.id}>
                  <Link to="/courses/$listId" params={{ listId: String(l.id) }} className="flex items-center justify-between gap-2 text-sm">
                    <span className="truncate font-medium">{l.name}</span>
                    <span className="text-ink-3">{l.checkedCount}/{l.itemCount}</span>
                  </Link>
                  <p className="text-xs text-ink-3">{l.plannedFor ? `${formatDate(l.plannedFor)} · ` : ''}environ {formatMoney(l.estimatedTotalCents)}</p>
                </li>
              ))}
            </ul>
          )}
        </Card>
        <Card title="Repas de la semaine" actions={<Link to="/planning" className="text-sm text-accent">Planifier</Link>}>
          <p className="text-3xl font-bold">{data.plannedMeals}</p>
          <p className="text-sm text-ink-2">repas planifiés sur 7 jours</p>
          {data.plannedMeals === 0 && (
            <Badge className="mt-2 bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-200">
              Planifier les repas évite les achats impulsifs
            </Badge>
          )}
        </Card>
        {data.accounting && <Budget summary={data.accounting} />}
      </div>
    </>
  )
}
