// Create or edit a planning entry, with the fields of its type and its links to the other modules.
import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useFeedback } from '../../app/useFeedback.ts'
import { useSession } from '../../app/useSession.ts'
import { useAction } from '../../app/useAction.ts'
import { datePart, dayCode, monthOf } from '../../lib/dates.ts'
import { entryToForm, formToRequest, newEntryForm, type EntryForm } from '../../lib/entryForm.ts'
import { formatDate } from '../../lib/format.ts'
import { ACCOUNTING_ACTIONS, ENTRY_TYPES, EVENT_CATEGORIES, FREQUENCIES, WEEKDAYS } from '../../lib/labels.ts'
import { Dialog } from '../../components/Dialog.tsx'
import { Button, Checkbox, ErrorMessage, Field, MemberPicker, Select } from '../../components/ui.tsx'

type Set = (patch: Partial<EntryForm>) => void

function TypePicker({ value, onChange }: { value: Schemas['EntryType'], onChange: (t: Schemas['EntryType']) => void }) {
  return (
    <div role="radiogroup" aria-label="Type" className="grid grid-cols-4 gap-1 rounded-lg bg-surface-2 p-1">
      {(Object.entries(ENTRY_TYPES) as [Schemas['EntryType'], string][]).map(([code, label]) => (
        <button key={code} type="button" role="radio" aria-checked={value === code} onClick={() => onChange(code)}
          className={`rounded-md px-2 py-1.5 text-sm font-medium ${value === code ? 'bg-surface text-accent shadow' : 'text-ink-2'}`}>
          {label}
        </button>
      ))}
    </div>
  )
}

function Recurrence({ form, set }: { form: EntryForm, set: Set }) {
  return (
    <fieldset className="space-y-3 rounded-lg border border-line p-3">
      <Field label="Répétition">
        <Select options={FREQUENCIES} value={form.repeat} placeholder="Ne se répète pas"
          onChange={(v) => set({ repeat: v, days: v === 'WEEKLY' ? [dayCode(form.startDate)] : [] })} />
      </Field>
      {form.repeat && (
        <div className="grid grid-cols-2 gap-3">
          <Field label="Intervalle" hint="2 = une fois sur deux">
            <input className="input" type="number" min="1" max="99" value={form.interval} onChange={(e) => set({ interval: e.target.value })} />
          </Field>
          <Field label="Jusqu'au (inclus)">
            <input className="input" type="date" value={form.until} onChange={(e) => set({ until: e.target.value })} />
          </Field>
        </div>
      )}
      {form.repeat === 'WEEKLY' && (
        <div className="flex flex-wrap gap-1" role="group" aria-label="Jours">
          {WEEKDAYS.map(([code, label]) => {
            const active = form.days.includes(code)
            return (
              <button key={code} type="button" aria-pressed={active}
                onClick={() => set({ days: active ? form.days.filter((d) => d !== code) : [...form.days, code] })}
                className={`min-h-9 rounded-full px-3 text-sm ${active ? 'bg-accent text-accent-ink' : 'bg-surface-2 text-ink-2'}`}>
                {label}
              </button>
            )
          })}
        </div>
      )}
    </fieldset>
  )
}

function TypeFields({ form, set }: { form: EntryForm, set: Set }) {
  const lists = useQuery({ queryKey: ['shopping', 'lists', 'OPEN'], enabled: form.type === 'SHOPPING',
    queryFn: () => unwrap(api().GET('/api/shopping/lists', { params: { query: { status: 'OPEN' } } })) })
  const recipes = useQuery({ queryKey: ['recipes', ''], enabled: form.type === 'MEAL',
    queryFn: () => unwrap(api().GET('/api/recipes')) })
  switch (form.type) {
    case 'EVENT':
      return (
        <div className="grid grid-cols-2 gap-3">
          <Field label="Catégorie">
            <Select options={EVENT_CATEGORIES} value={form.category} onChange={(v) => set({ category: v || 'OTHER' })} />
          </Field>
          <Field label="Coût estimé (€)" hint="Prévu au budget à la date de début.">
            <input className="input" inputMode="decimal" value={form.cost} placeholder="0,00" onChange={(e) => set({ cost: e.target.value })} />
          </Field>
        </div>
      )
    case 'SHOPPING':
      return (
        <Field label="Liste de courses" hint="La liste regroupe les recettes et les articles à acheter.">
          <Select options={{ new: '+ Créer une nouvelle liste', ...Object.fromEntries((lists.data ?? []).map((l) => [String(l.id), l.name])) }}
            value={form.shoppingListId} placeholder="Choisir une liste" onChange={(v) => set({ shoppingListId: v })} />
        </Field>
      )
    case 'MEAL':
      return (
        <div className="grid grid-cols-3 gap-3">
          <Field label="Recette" className="col-span-2">
            <Select options={Object.fromEntries((recipes.data ?? []).map((r) => [String(r.id), r.name]))} value={form.recipeId}
              placeholder="Choisir une recette" onChange={(v) => set({ recipeId: v,
                title: form.title || recipes.data?.find((r) => String(r.id) === v)?.name || '' })} />
          </Field>
          <Field label="Portions">
            <input className="input" type="number" min="1" max="50" value={form.servings} placeholder="recette"
              onChange={(e) => set({ servings: e.target.value })} />
          </Field>
        </div>
      )
    case 'ACCOUNTING':
      return (
        <Field label="Action">
          <Select options={ACCOUNTING_ACTIONS} value={form.accountingAction} onChange={(v) => set({ accountingAction: v || 'REPORT' })} />
        </Field>
      )
  }
}

function ModuleLink({ entry, onNavigate }: { entry: Schemas['PlanningEntry'], onNavigate: () => void }) {
  const className = 'inline-block text-sm font-medium text-accent'
  if (entry.type === 'SHOPPING' && entry.shoppingListId) {
    return <Link to="/courses/$listId" params={{ listId: String(entry.shoppingListId) }} onClick={onNavigate} className={className}>Ouvrir la liste →</Link>
  }
  if (entry.type === 'MEAL' && entry.recipeId) {
    return <Link to="/recettes/$recipeId" params={{ recipeId: String(entry.recipeId) }} search={{ servings: entry.servings ?? undefined }}
      onClick={onNavigate} className={className}>Voir la recette →</Link>
  }
  if (entry.type === 'ACCOUNTING') {
    return entry.accountingAction === 'APPROVAL'
      ? <Link to="/budget/$tab" params={{ tab: 'approbations' }} search={{ month: undefined }} onClick={onNavigate} className={className}>Approuver les opérations →</Link>
      : <Link to="/budget/$tab" params={{ tab: 'bilan' }} search={{ month: monthOf(datePart(entry.start)) }} onClick={onNavigate} className={className}>Ouvrir le bilan →</Link>
  }
  return null
}

interface Props {
  /** The occurrence clicked, to skip it when the entry repeats; absent for a new entry. */
  occurrence?: Schemas['Occurrence']
  /** Start "YYYY-MM-DDTHH:MM" of a new entry. */
  start?: string
  onClose: () => void
}

export function EntryDialog({ occurrence, start, onClose }: Props) {
  const { members, isParent } = useSession()
  const { confirm } = useFeedback()
  const entry = occurrence?.entry
  const [form, setForm] = useState<EntryForm>(() => (entry ? entryToForm(entry) : newEntryForm(start ?? '')))
  const set: Set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const areas = ['planning', 'dashboard', 'accounting', 'shopping'] as const
  const close = { refresh: [...areas], onSuccess: onClose }

  const save = useAction(async () => {
    const body = formToRequest(form)
    return entry
      ? unwrap(api().PUT('/api/planning/entries/{id}', { params: { path: { id: entry.id } }, body }))
      : unwrap(api().POST('/api/planning/entries', { body }))
  }, { ...close, inline: true, success: entry ? 'Entrée modifiée' : 'Entrée ajoutée' })
  const remove = useAction(() => unwrap(api().DELETE('/api/planning/entries/{id}', { params: { path: { id: entry!.id } } })),
    { ...close, success: 'Entrée supprimée' })
  const skip = useAction(() => unwrap(api().POST('/api/planning/entries/{id}/exceptions',
    { params: { path: { id: entry!.id } }, body: { day: datePart(occurrence!.start) } })), { ...close, success: 'Occurrence annulée' })
  const toggleDone = useAction(() => unwrap(api().PUT('/api/planning/entries/{id}/done',
    { params: { path: { id: entry!.id } }, body: { done: !entry!.done } })), close)
  const busy = save.isPending || remove.isPending || skip.isPending || toggleDone.isPending

  const submit = (event: FormEvent) => {
    event.preventDefault()
    save.mutate(undefined)
  }
  const askRemove = async () => {
    if (await confirm({ title: 'Supprimer cette entrée ?', message: entry?.recurrence
      ? 'Toutes ses répétitions seront supprimées. Pour une seule date, utilisez « Annuler ce jour ».' : `« ${entry?.title} » sera supprimée.` })) {
      remove.mutate(undefined)
    }
  }

  return (
    <Dialog wide title={entry ? 'Modifier' : 'Nouvelle entrée'} onClose={onClose} footer={(
      <>
        {entry && <Button variant="danger" className="mr-auto" disabled={busy} onClick={askRemove}>Supprimer</Button>}
        {entry?.recurrence && occurrence && (
          <Button disabled={busy} onClick={() => skip.mutate(undefined)}>Annuler le {formatDate(datePart(occurrence.start))}</Button>
        )}
        {entry && !entry.recurrence && (
          <Button disabled={busy} onClick={() => toggleDone.mutate(undefined)}>{entry.done ? 'Rouvrir' : 'Marquer fait'}</Button>
        )}
        <Button variant="primary" type="submit" form="entry-form" disabled={busy}>Enregistrer</Button>
      </>
    )}>
      <form id="entry-form" onSubmit={submit} className="space-y-4">
        <TypePicker value={form.type} onChange={(type) => set({ type })} />
        <Field label="Titre">
          <input className="input" value={form.title} maxLength={120} required onChange={(e) => set({ title: e.target.value })} />
        </Field>
        <TypeFields form={form} set={set} />
        {form.type === 'ACCOUNTING' && !isParent && <p className="text-xs text-ink-3">Le budget n'est accessible qu'aux parents.</p>}
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="Début">
            <div className="flex gap-2">
              <input className="input" type="date" required value={form.startDate} onChange={(e) => set({ startDate: e.target.value,
                endDate: e.target.value > form.endDate ? e.target.value : form.endDate })} />
              {!form.allDay && <input className="input w-28" type="time" required value={form.startTime} aria-label="Heure de début"
                onChange={(e) => set({ startTime: e.target.value })} />}
            </div>
          </Field>
          <Field label="Fin">
            <div className="flex gap-2">
              <input className="input" type="date" required min={form.startDate} value={form.endDate} onChange={(e) => set({ endDate: e.target.value })} />
              {!form.allDay && <input className="input w-28" type="time" required value={form.endTime} aria-label="Heure de fin"
                onChange={(e) => set({ endTime: e.target.value })} />}
            </div>
          </Field>
        </div>
        <Checkbox label="Journée entière" checked={form.allDay}
          onChange={(allDay) => set(allDay && !entry ? { allDay, endDate: form.startDate } : { allDay })} />
        <Field label="Qui ?"><MemberPicker members={members} value={form.memberIds} onChange={(memberIds) => set({ memberIds })} /></Field>
        <Recurrence form={form} set={set} />
        <Field label="Lieu"><input className="input" value={form.location} maxLength={200} onChange={(e) => set({ location: e.target.value })} /></Field>
        <Field label="Notes"><textarea className="input" rows={2} value={form.description} onChange={(e) => set({ description: e.target.value })} /></Field>
        {entry && <ModuleLink entry={entry} onNavigate={onClose} />}
        <ErrorMessage error={save.error} />
      </form>
    </Dialog>
  )
}
