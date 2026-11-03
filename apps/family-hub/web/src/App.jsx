// Root component: bootstraps the session, then shows setup, login or the application.
import { useCallback, useEffect, useMemo, useState } from 'react'
import { get, onUnauthorized, post } from './lib/api.js'
import { SessionContext } from './lib/session.js'
import { matchRoute, useRoute } from './lib/router.js'
import Layout from './components/Layout.jsx'
import LoginPage from './pages/LoginPage.jsx'
import SetupPage from './pages/SetupPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import PlanningPage from './pages/planning/PlanningPage.jsx'
import TodoPage from './pages/TodoPage.jsx'
import ShoppingListsPage from './pages/shopping/ShoppingListsPage.jsx'
import ShoppingListPage from './pages/shopping/ShoppingListPage.jsx'
import RecipesPage from './pages/recipes/RecipesPage.jsx'
import RecipePage from './pages/recipes/RecipePage.jsx'
import CatalogPage from './pages/CatalogPage.jsx'
import AccountingPage from './pages/accounting/AccountingPage.jsx'
import FamilyPage from './pages/FamilyPage.jsx'

/** Routes in matching order; `parent` routes are hidden from children. */
const ROUTES = [
  ['/', DashboardPage],
  ['/planning', PlanningPage],
  ['/todo', TodoPage],
  ['/courses', ShoppingListsPage],
  ['/courses/:id', ShoppingListPage],
  ['/recettes', RecipesPage],
  ['/recettes/:id', RecipePage],
  ['/catalogue', CatalogPage],
  ['/budget', AccountingPage, true],
  ['/budget/:tab', AccountingPage, true],
  ['/famille', FamilyPage],
]

function resolve(route, isParent) {
  for (const [pattern, Page, parentOnly] of ROUTES) {
    const params = matchRoute(route, pattern)
    if (params && (!parentOnly || isParent)) return { Page, params }
  }
  return { Page: DashboardPage, params: matchRoute('/', '/') }
}

export default function App() {
  const [state, setState] = useState({ loading: true, setupRequired: false, member: null })
  const [members, setMembers] = useState([])
  const route = useRoute()

  const load = useCallback(() => {
    get('/api/session')
      .then((s) => setState({ loading: false, setupRequired: s.setupRequired, member: s.member }))
      .catch(() => setState({ loading: false, setupRequired: false, member: null }))
  }, [])

  const reloadMembers = useCallback(() => {
    get('/api/members').then(setMembers).catch(() => setMembers([]))
  }, [])

  useEffect(() => {
    load()
    onUnauthorized(() => setState((s) => ({ ...s, member: null })))
  }, [load])

  useEffect(() => {
    if (state.member) reloadMembers()
  }, [state.member, reloadMembers])

  const session = useMemo(() => ({
    member: state.member,
    members,
    isParent: state.member?.role === 'PARENT',
    reloadMembers,
    logout: () => post('/api/auth/logout').finally(load),
  }), [state.member, members, reloadMembers, load])

  if (state.loading) return <p className="p-10 text-center text-slate-400">Chargement…</p>
  if (state.setupRequired) return <SetupPage onDone={load} />
  if (!state.member) return <LoginPage onDone={load} />

  const { Page, params } = resolve(route, session.isParent)
  return (
    <SessionContext.Provider value={session}>
      <Layout route={route}>
        <Page params={params} />
      </Layout>
    </SessionContext.Provider>
  )
}
