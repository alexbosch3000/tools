// Signing in shows the member at once on the page that observes the session, without the data of a previous member.
import { afterEach, describe, expect, it } from 'vitest'
import { act, cleanup, render, screen } from '@testing-library/react'
import { QueryClientProvider, useQuery } from '@tanstack/react-query'
import type { Schemas } from '../api/client.ts'
import { queryClient } from './queryClient.ts'
import { token } from './connection.ts'
import { signIn } from './useSession.ts'

afterEach(() => {
  cleanup()
  queryClient.clear()
  localStorage.clear()
})

const alex = { id: 1, name: 'Alex', color: '#2563eb', role: 'PARENT', hasCalendarFeed: false } as Schemas['Member']

function WhoIsThere() {
  const session = useQuery({
    queryKey: ['session'],
    queryFn: (): Promise<Schemas['SessionState']> => Promise.resolve({ setupRequired: true, member: null }),
  })
  if (session.isPending) return null
  return <p>{session.data?.member?.name ?? 'Personne'}</p>
}

describe('signIn', () => {
  it('updates the session already on screen and stores the token', async () => {
    render(<QueryClientProvider client={queryClient}><WhoIsThere /></QueryClientProvider>)
    await screen.findByText('Personne')

    act(() => signIn({ token: 'fht_secret', member: alex }))

    expect(await screen.findByText('Alex')).toBeTruthy()
    expect(token()).toBe('fht_secret')
  })

  it('forgets what was cached for the previous member', () => {
    queryClient.setQueryData(['accounting', 'transactions'], [{ id: 1 }])
    signIn({ token: 'fht_secret', member: alex })
    expect(queryClient.getQueryData(['accounting', 'transactions'])).toBeUndefined()
  })
})
