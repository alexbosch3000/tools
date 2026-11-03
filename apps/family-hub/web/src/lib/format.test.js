// French formatting and parsing of amounts and quantities.
import { describe, expect, it } from 'vitest'
import { centsToInput, formatMoney, formatQuantity, parseMoney } from './format.js'

const spaces = (text) => text.replace(/\s/g, ' ')

describe('format', () => {
  it('formats euros the French way', () => {
    expect(spaces(formatMoney(123456))).toBe('1 234,56 €')
    expect(formatMoney(null)).toBe('—')
  })

  it('parses amounts typed with a comma or a dot', () => {
    expect(parseMoney('12,5')).toBe(1250)
    expect(parseMoney(' 87.40 € ')).toBe(8740)
    expect(parseMoney('')).toBeNull()
    expect(parseMoney('12,345')).toBeNaN()
    expect(parseMoney('abc')).toBeNaN()
    expect(centsToInput(8740)).toBe('87,40')
  })

  it('formats quantities with French units and plurals', () => {
    expect(formatQuantity({ amount: 1.5, unit: 'KG' })).toBe('1,5 kg')
    expect(formatQuantity({ amount: 3, unit: 'PIECE' })).toBe('3 pièces')
    expect(formatQuantity({ amount: 1, unit: 'PIECE' })).toBe('1 pièce')
    expect(formatQuantity({ amount: 2.3333, unit: 'TBSP' })).toBe('2,33 c. à soupe')
  })
})
