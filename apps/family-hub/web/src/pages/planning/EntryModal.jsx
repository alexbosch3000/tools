// Create or edit a planning entry, with the fields of its type and its links to the other modules.
import { useState } from 'react'
import { del, post, put } from '../../lib/api.js'
import { datePart, monthOf } from '../../lib/dates.js'
import { dayCode, entryToForm, formToRequest, newEntryForm } from '../../lib/entryForm.js'
import { formatDate } from '../../lib/format.js'
import { ACCOUNTING_ACTIONS, ENTRY_TYPES, EVENT_CATEGORIES, FREQUENCIES, WEEKDAYS } from '../../lib/labels.js'
import { useResource } from '../../lib/useResource.js'
import { useSession } from '../../lib/session.js'
import { useSubmit } from '../../lib/useSubmit.js'
import { Button, Checkbox, ErrorMessage, Field, MemberPicker, Modal, Select } from '../../components/ui.jsx'

function TypePicker({ value, onChange }) {
  return (
    <div className="grid grid-cols-4 gap-1 rounded-lg bg-slate-100 p-1">
      {Object.entries(ENTRY_TYPES).map(([code, label]) => (
        <button key={code} type="button" onClick={() => onChange(code)}
          className={`rounded-md px-2 py-1.5 text-sm font-medium ${value === code ? 'bg-white text-indigo-700 shadow' : 'text-slate-600'}`}>
          {label}
        </button>
      ))}
    </div>
  )
}

function Recurrence({ form, set }) {
  return (
    <div className="space-y-3 rounded-lg border border-slate-200 p-3">
      <Field label="Répétition">
        <Select options={FREQUENCIES} value={form.repeat} placeholder="Ne se répète pas"
          onChange={(v) => set({ repeat: v ?? '', days: v === 'WEEKLY' ? [dayCode(form.startDate)] : [] })} />
      </Field>
      {form.repeat && (
        <div className="grid grid-cols-2 gap-3">
          <Field label="Intervalle" hint="2 = une fois sur deux">
            <input className="input" type="number" min="1" max="99" value={form.interval}
              onChange={(e) => set({ interval: e.target.value })} />
          </Field>
          <Field label="Jusqu'au (inclus)">
            <input className="input" type="date" value={form.until} onChange={(e) => set({ until: e.target.value })} />
          </Field>
        </div>
      )}
      {form.repeat === 'WEEKLY' && (
        <div className="flex flex-wrap gap-1">
          {WEEKDAYS.map(([code, label]) => {
            const active = form.days.includes(code)
            return (
              <button key={code} type="button" aria-pressed={active}
                onClick={() => set({ days: active ? form.days.filter((d) => d !== code) : [...form.days, code] })}
                className={`rounded-full px-3 py-1 text-sm ${active ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-600'}`}>
                {label}
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}

function TypeFields({ form, set }) {
  const lists = useResource(form.type === 'SHOPPING' ? '/api/shopping/lists' : null)
  const recipes = useResource(form.type === 'MEAL' ? '/api/recipes' : null)
  switch (form.type) {
    case 'EVENT':
      return (
        <div className="grid grid-cols-2 gap-3">
          <Field label="Catégorie">
            <Select options={EVENT_CATEGORIES} value={form.category} onChange={(v) => set({ category: v ?? 'OTHER' })} />
          </Field>
          <Field label="Coût estimé (€)" hint="Prévu au budget à la date de début.">
            <input className="input" inputMode="decimal" value={form.cost} placeholder="0,00"
              onChange={(e) => set({ cost: e.target.value })} />
          </Field>
        </div>
      )
    case 'SHOPPING': {
      const options = { new: '+ Créer une nouvelle liste',
        ...Object.fromEntries((lists.data ?? []).map((l) => [String(l.id), l.name])) }
      return (
        <Field label="Liste de courses" hint="La liste regroupe les recettes et les ingrédients à acheter.">
          <Select options={options} value={form.shoppingListId} placeholder="Choisir une liste"
            onChange={(v) => set({ shoppingListId: v ?? '' })} />
        </Field>
      )
    }
    case 'MEAL':
      return (
        <div className="grid grid-cols-3 gap-3">
          <Field label="Recette" className="col-span-2">
            <Select options={Object.fromEntries((recipes.data ?? []).map((r) => [String(r.id), r.name]))}
              value={form.recipeId} placeholder="Choisir une recette"
              onChange={(v) => set({ recipeId: v ?? '', title: form.title || (recipes.data ?? [])
                .find((r) => String(r.id) === v)?.name || '' })} />
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
          <Select options={ACCOUNTING_ACTIONS} value={form.accountingAction}
            onChange={(v) => set({ accountingAction: v ?? 'REPORT' })} />
        </Field>
      )
    default:
      return null
  }
}

/** Link from an entry to the page of the module it references. */
function linkOf(entry) {
  if (!entry) return null
  if (entry.type === 'SHOPPING' && entry.shoppingListId) return ['Ouvrir la liste', `#/courses/${entry.shoppingListId}`]
  if (entry.type === 'MEAL' && entry.recipeId) return ['Voir la recette', `#/recettes/${entry.recipeId}?servings=${entry.servings}`]
  if (entry.type === 'ACCOUNTING') {
    return entry.accountingAction === 'APPROVAL'
      ? ['Approuver les opérations', '#/budget/approbations']
      : ['Ouvrir le bilan', `#/budget/synthese?month=${monthOf(datePart(entry.start))}`]
  }
  return null
}

/**
 * @param occurrence the occurrence clicked, to skip it when the entry repeats; null for a new entry
 * @param start      start of a new entry
 */
export default function EntryModal({ occurrence, start, defaults, onClose, onSaved }) {
  const { members, isParent } = useSession()
  const entry = occurrence?.entry ?? null
  const [form, setForm] = useState(() => (entry ? entryToForm(entry) : newEntryForm(start, defaults)))
  const { busy, error, run } = useSubmit()
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const link = linkOf(entry)

  const save = (e) => {
    e.preventDefault()
    run(async () => {
      const body = formToRequest(form)
      if (entry) await put(`/api/planning/entries/${entry.id}`, body)
      else await post('/api/planning/entries', body)
      onSaved()
    })
  }
  const act = (action) => run(async () => { await action(); onSaved() })

  return (
    <Modal wide title={entry ? 'Modifier' : 'Nouvelle entrée'} onClose={onClose} footer={(
      <>
        {entry && <Button variant="danger" disabled={busy} className="mr-auto"
          onClick={() => act(() => del(`/api/planning/entries/${entry.id}`))}>Supprimer</Button>}
        {entry?.recurrence && (
          <Button disabled={busy} onClick={() => act(() => post(`/api/planning/entries/${entry.id}/exceptions`,
            { day: datePart(occurrence.start) }))}>
            Annuler le {formatDate(datePart(occurrence.start))}
          </Button>
        )}
        {entry && !entry.recurrence && (
          <Button disabled={busy} onClick={() => act(() => put(`/api/planning/entries/${entry.id}/done`, { done: !entry.done }))}>
            {entry.done ? 'Rouvrir' : 'Marquer fait'}
          </Button>
        )}
        <Button variant="primary" type="submit" form="entry-form" disabled={busy}>Enregistrer</Button>
      </>
    )}>
      <form id="entry-form" onSubmit={save} className="space-y-4">
        <TypePicker value={form.type} onChange={(type) => set({ type })} />
        <Field label="Titre">
          <input className="input" value={form.title} maxLength={120} required onChange={(e) => set({ title: e.target.value })} />
        </Field>
        <TypeFields form={form} set={set} />
        {form.type === 'ACCOUNTING' && !isParent && (
          <p className="text-xs text-slate-500">Le budget n'est accessible qu'aux parents.</p>
        )}
        <div className="grid grid-cols-2 gap-3">
          <Field label="Début">
            <div className="flex gap-2">
              <input className="input" type="date" required value={form.startDate} onChange={(e) => set({ startDate: e.target.value,
                endDate: e.target.value > form.endDate ? e.target.value : form.endDate })} />
              {!form.allDay && <input className="input w-28" type="time" required value={form.startTime}
                onChange={(e) => set({ startTime: e.target.value })} />}
            </div>
          </Field>
          <Field label="Fin">
            <div className="flex gap-2">
              <input className="input" type="date" required value={form.endDate} min={form.startDate}
                onChange={(e) => set({ endDate: e.target.value })} />
              {!form.allDay && <input className="input w-28" type="time" required value={form.endTime}
                onChange={(e) => set({ endTime: e.target.value })} />}
            </div>
          </Field>
        </div>
        <Checkbox label="Journée entière" checked={form.allDay}
          onChange={(allDay) => set(allDay && !entry ? { allDay, endDate: form.startDate } : { allDay })} />
        <Field label="Qui ?">
          <MemberPicker members={members} value={form.memberIds} onChange={(memberIds) => set({ memberIds })} />
        </Field>
        <Recurrence form={form} set={set} />
        <Field label="Lieu">
          <input className="input" value={form.location} maxLength={200} onChange={(e) => set({ location: e.target.value })} />
        </Field>
        <Field label="Notes">
          <textarea className="input" rows="2" value={form.description} onChange={(e) => set({ description: e.target.value })} />
        </Field>
        {link && <a href={link[1]} onClick={onClose} className="inline-block text-sm font-medium text-indigo-600">{link[0]} →</a>}
        <ErrorMessage error={error} />
      </form>
    </Modal>
  )
}
