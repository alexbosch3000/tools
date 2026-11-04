// French formatting of money, quantities, dates and times, and parsing of amounts typed by users.
import type { Schemas } from '../api/client.ts'
import { parseIsoDate } from './dates.ts'
import { UNIT_LABELS } from './labels.ts'

const euros = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' })
const numbers = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 })

export function formatMoney(cents: number | null | undefined): string {
  return cents === null || cents === undefined ? '—' : euros.format(cents / 100)
}

/** "12,50", "12.5" or "12 €" to cents; null when blank, NaN when not a valid amount. */
export function parseMoney(text: string | null | undefined): number | null {
  const value = String(text ?? '').replace(/\s/g, '').replace('€', '').replace(',', '.')
  if (value === '') return null
  if (!/^-?\d+(\.\d{0,2})?$/.test(value)) return Number.NaN
  return Math.round(Number(value) * 100)
}

/** Cents to the editable text of an amount field. */
export function centsToInput(cents: number | null | undefined): string {
  return cents === null || cents === undefined ? '' : (cents / 100).toFixed(2).replace('.', ',')
}

export function formatQuantity({ amount, unit }: Schemas['Quantity']): string {
  const label = UNIT_LABELS[unit]
  const plural = amount > 1 && ['PIECE', 'PACK', 'BUNCH', 'PINCH'].includes(unit)
  return `${numbers.format(amount)} ${plural ? label.plural : label.singular}`
}

export function formatDate(iso: string | null | undefined,
  options: Intl.DateTimeFormatOptions = { weekday: 'short', day: 'numeric', month: 'short' }): string {
  return iso ? parseIsoDate(iso).toLocaleDateString('fr-FR', options) : ''
}

export function formatLongDate(iso: string): string {
  return formatDate(iso, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
}

export function formatMonth(yearMonth: string): string {
  return formatDate(`${yearMonth}-01`, { month: 'long', year: 'numeric' })
}

export function formatTime(dateTime: string | null | undefined): string {
  return dateTime ? dateTime.slice(11, 16) : ''
}

export function formatDateTime(dateTime: string | null | undefined): string {
  return dateTime ? `${formatDate(dateTime.slice(0, 10))} ${formatTime(dateTime)}` : ''
}

/** "1 tâche", "2 tâches": French plural, 0 and 1 being singular. */
export function plural(count: number, singular: string, pluralForm = `${singular}s`): string {
  return `${count} ${count > 1 ? pluralForm : singular}`
}
