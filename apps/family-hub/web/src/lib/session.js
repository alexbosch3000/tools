// Session context: the logged-in member and the family, available to every page.
import { createContext, useContext } from 'react'

export const SessionContext = createContext(null)

/** { member, members, isParent, reloadMembers, logout } */
export function useSession() {
  return useContext(SessionContext)
}
