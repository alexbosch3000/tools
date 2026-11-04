// The logged-in member and the family, for every page rendered inside SessionGate.
import { createContext, useContext } from 'react'
import type { Schemas } from '../api/client.ts'
import { setToken } from './connection.ts'
import { forgetMemberData, queryClient } from './queryClient.ts'

export interface Session {
  member: Schemas['Member']
  members: Schemas['Member'][]
  isParent: boolean
  logout: () => Promise<void>
  /** Forgets the server, to connect to another one (UI served by GitHub Pages). */
  changeServer: () => void
}

export const SessionContext = createContext<Session | null>(null)

export function useSession(): Session {
  const session = useContext(SessionContext)
  if (!session) throw new Error('useSession outside SessionGate')
  return session
}

/** Stores a token received from login or setup and makes the app reload its data as that member. */
export function signIn({ token, member }: Schemas['Session']): void {
  setToken(token)
  forgetMemberData()
  queryClient.setQueryData<Schemas['SessionState']>(['session'], { setupRequired: false, member })
}
