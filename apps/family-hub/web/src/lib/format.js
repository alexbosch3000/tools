// French formatting of money, quantities, dates and times, and parsing of amounts typed by users.
import { parseIsoDate } from './dates.js'
import { UNIT_LABELS } from './labels.js'

const euros = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' })
const numbers = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 })

export function formatMoney(cents) {
  if (cents === null || cents === undefined) return '—'
  return euros.format(cents / 100)
}

/** "12,50", "12.5" or "12" in euros to cents; null when blank, NaN when not a valid amount. */
export function parseMoney(text) {
  const value = String(text ?? '').replace(/\s/g, '').replace('€', '').replace(',', '.')
  if (value === '') return null
  if (!/^-?\d+(\.\d{0,2})?$/.test(value)) return NaN
  return Math.round(Number(value) * 100)
}

/** Cents to the editable text of an amount input. */
export function centsToInput(cents) {
  if (cents === null || cents === undefined) return ''
  return (cents / 100).toFixed(2).replace('.', ',')
}

export function formatNumber(value) {
  return numbers.format(value)
}

export function formatQuantity({ amount, unit }) {
  const label = UNIT_LABELS[unit] ?? unit
  const plural = amount > 1 && ['PIECE', 'PACK', 'BUNCH', 'PINCH'].includes(unit)
  return `${numbers.format(amount)} ${plural ? label.plural : label.singular}`
}

export function formatDate(iso, options = { weekday: 'short', day: 'numeric', month: 'short' }) {
  if (!iso) return ''
  return parseIsoDate(iso).toLocaleDateString('fr-FR', options)
}

export function formatLongDate(iso) {
  return formatDate(iso, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
}

export function formatMonth(yearMonth) {
  return formatDate(`${yearMonth}-01`, { month: 'long', year: 'numeric' })
}

export function formatTime(dateTime) {
  return dateTime ? dateTime.slice(11, 16) : ''
}

export function formatDateTime(dateTime) {
  if (!dateTime) return ''
  return `${formatDate(dateTime.slice(0, 10))} ${formatTime(dateTime)}`
}
