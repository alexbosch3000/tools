// Routes of the application, type-checked, with hash history so that GitHub Pages needs no server-side fallback.
import { createHashHistory, createRootRoute, createRoute, createRouter, Navigate, Outlet } from '@tanstack/react-router'
import { AppShell } from '../components/AppShell.tsx'
import { Empty } from '../components/ui.tsx'
import { DashboardPage } from '../features/dashboard/DashboardPage.tsx'
import { PlanningPage } from '../features/planning/PlanningPage.tsx'
import { TodoPage } from '../features/todo/TodoPage.tsx'
import { ShoppingListsPage } from '../features/shopping/ShoppingListsPage.tsx'
import { ShoppingListPage } from '../features/shopping/ShoppingListPage.tsx'
import { RecipesPage } from '../features/recipes/RecipesPage.tsx'
import { RecipePage } from '../features/recipes/RecipePage.tsx'
import { CatalogPage } from '../features/catalog/CatalogPage.tsx'
import { AccountingPage } from '../features/accounting/AccountingPage.tsx'
import { ACCOUNTING_TABS, type AccountingTab } from '../features/accounting/tabs.ts'
import { FamilyPage } from '../features/family/FamilyPage.tsx'

const root = createRootRoute({
  component: () => <AppShell><Outlet /></AppShell>,
  notFoundComponent: () => <Empty>Cette page n'existe pas.</Empty>,
})

const text = (value: unknown) => (typeof value === 'string' && value ? value : undefined)

interface RecipeSearch { servings?: number }
interface BudgetSearch { month?: string }

const routes = [
  createRoute({ getParentRoute: () => root, path: '/', component: DashboardPage }),
  createRoute({ getParentRoute: () => root, path: '/planning', component: PlanningPage }),
  createRoute({ getParentRoute: () => root, path: '/todo', component: TodoPage }),
  createRoute({ getParentRoute: () => root, path: '/courses', component: ShoppingListsPage }),
  createRoute({ getParentRoute: () => root, path: '/courses/$listId', component: ShoppingListPage }),
  createRoute({ getParentRoute: () => root, path: '/recettes', component: RecipesPage }),
  createRoute({
    getParentRoute: () => root, path: '/recettes/$recipeId', component: RecipePage,
    validateSearch: (search: Record<string, unknown>): RecipeSearch => ({ servings: Number(search.servings) || undefined }),
  }),
  createRoute({ getParentRoute: () => root, path: '/catalogue', component: CatalogPage }),
  createRoute({ getParentRoute: () => root, path: '/budget', component: () => <Navigate to="/budget/$tab" params={{ tab: 'bilan' }} /> }),
  createRoute({
    getParentRoute: () => root, path: '/budget/$tab', component: AccountingPage,
    params: {
      parse: ({ tab }: { tab: string }) => ({ tab: (tab in ACCOUNTING_TABS ? tab : 'bilan') as AccountingTab }),
      stringify: ({ tab }: { tab: AccountingTab }) => ({ tab }),
    },
    validateSearch: (search: Record<string, unknown>): BudgetSearch => ({ month: text(search.month) }),
  }),
  createRoute({ getParentRoute: () => root, path: '/famille', component: FamilyPage }),
] as const

export const router = createRouter({
  routeTree: root.addChildren(routes),
  history: createHashHistory(),
  defaultPreload: 'intent',
  scrollRestoration: true,
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
