// Session of the logged-in member: server choice (Pages), first-run setup, login, members and role.
import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, onUnauthorized, reconnect, unwrap, type Schemas } from '../api/client.ts'
import { remoteMode, serverUrl, setServerUrl, setToken } from './connection.ts'
import { forgetMemberData, queryClient } from './queryClient.ts'
import { LoginPage, ServerPage, SetupPage, Unreachable } from '../features/auth/AccessPages.tsx'
import { Loading } from '../components/ui.tsx'
import { SessionContext, type Session } from './useSession.ts'

/** Shows what is needed before the application: server address, setup, login. */
export function SessionGate({ children }: { children: ReactNode }) {
  const [server, setServer] = useState(serverUrl())
  if (remoteMode && !server) {
    return <ServerPage onConnect={(url) => { setServerUrl(url); reconnect(); setServer(url) }} />
  }
  const changeServer = () => { setServerUrl(null); setToken(null); queryClient.clear(); setServer('') }
  return <SessionLoader changeServer={changeServer}>{children}</SessionLoader>
}

function SessionLoader({ children, changeServer }: { children: ReactNode, changeServer: () => void }) {
  const state = useQuery({ queryKey: ['session'], queryFn: () => unwrap(api().GET('/api/session')) })
  const member = state.data?.member ?? null
  const members = useQuery({
    queryKey: ['members'], enabled: member !== null, queryFn: () => unwrap(api().GET('/api/members')),
  })

  useEffect(() => onUnauthorized(() => {
    setToken(null)
    queryClient.setQueryData<Schemas['SessionState']>(['session'], (s) => s && { ...s, member: null })
  }), [])

  const session = useMemo<Session | null>(() => member && {
    member,
    members: members.data ?? [member],
    isParent: member.role === 'PARENT',
    logout: async () => {
      await api().POST('/api/auth/logout').catch(() => undefined)
      setToken(null)
      queryClient.setQueryData<Schemas['SessionState']>(['session'], (s) => s && { ...s, member: null })
      forgetMemberData()
    },
    changeServer,
  }, [member, members.data, changeServer])

  if (state.isPending) return <div className="mx-auto max-w-sm p-10"><Loading /></div>
  if (state.isError) return <Unreachable error={state.error} retry={() => state.refetch()} changeServer={changeServer} />
  if (state.data.setupRequired) return <SetupPage />
  if (!session) return <LoginPage changeServer={remoteMode ? changeServer : undefined} />
  return <SessionContext.Provider value={session}>{children}</SessionContext.Provider>
}
