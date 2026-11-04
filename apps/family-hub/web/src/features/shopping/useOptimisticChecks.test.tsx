// A ticked box shows its new state during the click itself, and the server's value once the save settles.
import { afterEach, describe, expect, it } from 'vitest'
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { useOptimisticChecks } from './useOptimisticChecks.ts'

afterEach(cleanup)

function deferred() {
  let resolve!: () => void
  let reject!: (error: Error) => void
  const promise = new Promise<void>((res, rej) => { resolve = res; reject = rej })
  return { promise, resolve, reject }
}

function Box({ serverValue, save }: { serverValue: boolean, save: (checked: boolean) => Promise<unknown> }) {
  const checks = useOptimisticChecks()
  return (
    <input type="checkbox" aria-label="Œufs" checked={checks.isChecked('eggs', serverValue)}
      onChange={() => checks.toggle('eggs', serverValue, save)} />
  )
}

const box = () => screen.getByRole<HTMLInputElement>('checkbox', { name: 'Œufs' })

describe('useOptimisticChecks', () => {
  it('shows the tick as soon as the box is clicked, before the server answers', () => {
    const pending = deferred()
    render(<Box serverValue={false} save={() => pending.promise} />)
    fireEvent.click(box())
    expect(box().checked).toBe(true)
  })

  it('sends the value shown, and lets a second click untick before the first save ends', () => {
    const sent: boolean[] = []
    render(<Box serverValue={false} save={(checked) => { sent.push(checked); return deferred().promise }} />)
    fireEvent.click(box())
    fireEvent.click(box())
    expect(sent).toEqual([true, false])
    expect(box().checked).toBe(false)
  })

  it('goes back to the server value when the save fails', async () => {
    const pending = deferred()
    render(<Box serverValue={false} save={() => pending.promise} />)
    fireEvent.click(box())
    await act(async () => pending.reject(new Error('offline')))
    expect(box().checked).toBe(false)
  })

  it('shows the server value once the save succeeded', async () => {
    const pending = deferred()
    const { rerender } = render(<Box serverValue={false} save={() => pending.promise} />)
    fireEvent.click(box())
    rerender(<Box serverValue={true} save={() => pending.promise} />)
    await act(async () => pending.resolve())
    expect(box().checked).toBe(true)
  })
})
