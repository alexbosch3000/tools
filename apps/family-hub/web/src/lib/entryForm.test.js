// Conversion between planning entries and the entry form.
import { describe, expect, it } from 'vitest'
import { dayCode, entryToForm, formToRequest, newEntryForm } from './entryForm.js'

describe('entry form', () => {
  it('starts a new entry one hour long, across midnight if needed', () => {
    expect(newEntryForm('2026-11-03T23:30')).toMatchObject({
      startDate: '2026-11-03', startTime: '23:30', endDate: '2026-11-04', endTime: '00:30' })
  })

  it('builds a weekly event request with its cost in cents', () => {
    const form = newEntryForm('2026-11-04T17:00', {
      title: 'Judo', category: 'SPORT', cost: '45,50', repeat: 'WEEKLY', days: ['WEDNESDAY'], memberIds: [2] })
    expect(formToRequest(form)).toMatchObject({
      type: 'EVENT', start: '2026-11-04T17:00', end: '2026-11-04T18:00', estimatedCostCents: 4550,
      recurrence: { frequency: 'WEEKLY', interval: 1, days: ['WEDNESDAY'], until: null },
      category: 'SPORT', shoppingListId: null, recipeId: null, accountingAction: null })
  })

  it('keeps only the fields of the type and covers whole days', () => {
    const form = newEntryForm('2026-11-07T10:00', { type: 'SHOPPING', shoppingListId: 'new', cost: '12', allDay: true })
    expect(formToRequest(form)).toMatchObject({
      start: '2026-11-07T00:00', end: '2026-11-07T23:59', createShoppingList: true, shoppingListId: null,
      category: null, estimatedCostCents: null })
  })

  it('rejects an invalid cost', () => {
    expect(() => formToRequest(newEntryForm('2026-11-07T10:00', { cost: 'douze' }))).toThrow(/montant/)
  })

  it('round-trips an entry of the API', () => {
    const entry = {
      title: 'Carbonara', description: '', location: null, type: 'MEAL', start: '2026-11-04T19:30:00',
      end: '2026-11-04T20:30:00', allDay: false, recurrence: null, memberIds: [], category: null,
      estimatedCostCents: null, shoppingListId: null, recipeId: 3, servings: 6, accountingAction: null,
    }
    expect(formToRequest(entryToForm(entry))).toMatchObject({
      type: 'MEAL', start: '2026-11-04T19:30', recipeId: 3, servings: 6, recurrence: null })
  })

  it('names week days like the API', () => {
    expect(dayCode('2026-11-04')).toBe('WEDNESDAY')
  })
})
