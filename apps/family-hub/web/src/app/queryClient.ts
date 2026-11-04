// Server state cache: what each page reads from the API, refreshed after every change.
import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '../api/client.ts'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      // A client error (400 to 499) will not go away by retrying.
      retry: (failures, error) => !(error instanceof ApiError && error.status >= 400 && error.status < 500)
        && failures < 2,
    },
  },
})

/** Areas of the cache, each reloaded when a change may affect it. */
export type Area = 'session' | 'members' | 'dashboard' | 'planning' | 'todo' | 'shopping' | 'recipes'
  | 'catalog' | 'accounting'

/**
 * Drops every cached answer except the session: the next member must not see the data of the previous
 * one. The session query is kept because the page that observes it stays mounted.
 */
export function forgetMemberData(): void {
  queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== 'session' })
}

export function refresh(...areas: Area[]): Promise<void> {
  return Promise.all(areas.map((area) => queryClient.invalidateQueries({ queryKey: [area] }))).then(() => undefined)
}
