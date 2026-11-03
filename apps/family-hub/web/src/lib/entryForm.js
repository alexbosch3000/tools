// Conversion between a planning entry of the API and the fields of the entry form.
import { addMinutes, combine, datePart, timePart } from './dates.js'
import { centsToInput, parseMoney } from './format.js'

/** Empty form starting at a date-time "YYYY-MM-DDTHH:MM", one hour long. */
export function newEntryForm(start, overrides = {}) {
  const end = addMinutes(start, 60)
  return {
    title: '', description: '', location: '', type: 'EVENT',
    startDate: datePart(start), startTime: timePart(start), endDate: datePart(end), endTime: timePart(end),
    allDay: false, repeat: '', interval: 1, days: [], until: '', memberIds: [],
    category: 'OTHER', cost: '', shoppingListId: '', recipeId: '', servings: '', accountingAction: 'REPORT',
    ...overrides,
  }
}

export function entryToForm(entry) {
  const r = entry.recurrence
  return {
    title: entry.title, description: entry.description ?? '', location: entry.location ?? '', type: entry.type,
    startDate: datePart(entry.start), startTime: timePart(entry.start),
    endDate: datePart(entry.end), endTime: timePart(entry.end),
    allDay: entry.allDay, repeat: r?.frequency ?? '', interval: r?.interval ?? 1, days: r?.days ?? [],
    until: r?.until ?? '', memberIds: entry.memberIds,
    category: entry.category ?? 'OTHER', cost: centsToInput(entry.estimatedCostCents),
    shoppingListId: entry.shoppingListId ? String(entry.shoppingListId) : '',
    recipeId: entry.recipeId ? String(entry.recipeId) : '', servings: entry.servings ? String(entry.servings) : '',
    accountingAction: entry.accountingAction ?? 'REPORT',
  }
}

/** Request body of the form; throws an Error with a French message when a value is invalid. */
export function formToRequest(form) {
  const cost = form.type === 'EVENT' ? parseMoney(form.cost) : null
  if (Number.isNaN(cost)) throw new Error('Le coût estimé doit être un montant, par exemple 45,50.')
  const recurrence = form.repeat ? {
    frequency: form.repeat,
    interval: Number(form.interval) || 1,
    days: form.repeat === 'WEEKLY' ? form.days : [],
    until: form.until || null,
  } : null
  return {
    title: form.title,
    description: form.description,
    location: form.location,
    type: form.type,
    start: combine(form.startDate, form.allDay ? '00:00' : form.startTime),
    end: combine(form.endDate, form.allDay ? '23:59' : form.endTime),
    allDay: form.allDay,
    recurrence,
    memberIds: form.memberIds,
    category: form.type === 'EVENT' ? form.category : null,
    estimatedCostCents: cost,
    shoppingListId: form.type === 'SHOPPING' && form.shoppingListId && form.shoppingListId !== 'new'
      ? Number(form.shoppingListId) : null,
    createShoppingList: form.type === 'SHOPPING' && form.shoppingListId === 'new',
    recipeId: form.type === 'MEAL' && form.recipeId ? Number(form.recipeId) : null,
    servings: form.type === 'MEAL' && form.servings ? Number(form.servings) : null,
    accountingAction: form.type === 'ACCOUNTING' ? form.accountingAction : null,
  }
}

/** Day of week code of a date, as the API names it. */
export function dayCode(isoDate) {
  const codes = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']
  const [y, m, d] = isoDate.split('-').map(Number)
  return codes[new Date(y, m - 1, d).getDay()]
}
