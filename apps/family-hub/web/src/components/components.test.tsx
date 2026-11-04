// Behaviour of shared components: budget meter wording, member selection, toggles exposed to assistive technologies.
import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { useState } from 'react'
import type { Schemas } from '../api/client.ts'
import { Meter } from './charts.tsx'
import { MemberPicker } from './ui.tsx'

afterEach(cleanup)

const member = (id: number, name: string) => ({ id, name, color: '#2563eb', role: 'CHILD', hasCalendarFeed: false }) as Schemas['Member']

describe('Meter', () => {
  it('says in words when the envelope is exceeded, not only with a colour', () => {
    render(<Meter label="Courses" actual={50_000} forecast={15_000} limit={60_000} />)
    expect(screen.getByText(/dépassé/)).toBeTruthy()
  })

  it('stays silent while within the envelope', () => {
    render(<Meter label="Courses" actual={20_000} limit={60_000} />)
    expect(screen.queryByText(/dépassé/)).toBeNull()
    expect(screen.getByRole('img').getAttribute('aria-label')).toMatch(/dépensés sur/)
  })
})

describe('MemberPicker', () => {
  function Harness() {
    const [value, setValue] = useState<number[]>([1])
    return <MemberPicker members={[member(1, 'Léa'), member(2, 'Sam')]} value={value} onChange={setValue} />
  }

  it('toggles members and exposes the state with aria-pressed', () => {
    render(<Harness />)
    const lea = screen.getByRole('button', { name: 'Léa' })
    const sam = screen.getByRole('button', { name: 'Sam' })
    expect(lea.getAttribute('aria-pressed')).toBe('true')
    fireEvent.click(sam)
    fireEvent.click(lea)
    expect(sam.getAttribute('aria-pressed')).toBe('true')
    expect(lea.getAttribute('aria-pressed')).toBe('false')
  })
})
