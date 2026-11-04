// Application frame: navigation (sidebar on large screens, bottom bar on phones) and the page area.
import type { ReactNode } from 'react'
import { Link } from '@tanstack/react-router'
import { useSession } from '../app/useSession.ts'
import { MemberDot } from './ui.tsx'

type Path = '/' | '/planning' | '/todo' | '/courses' | '/recettes' | '/catalogue' | '/budget' | '/famille'

const NAV: { to: Path, label: string, short: string, icon: string, parentOnly?: boolean, exact?: boolean }[] = [
  { to: '/', label: 'Tableau de bord', short: 'Accueil', icon: 'M3 11l9-8 9 8M5 10v10h14V10', exact: true },
  { to: '/planning', label: 'Planning', short: 'Planning', icon: 'M4 6h16v14H4zM4 10h16M9 3v4M15 3v4' },
  { to: '/todo', label: 'À faire', short: 'À faire', icon: 'M5 12l4 4 10-10' },
  { to: '/courses', label: 'Courses', short: 'Courses', icon: 'M3 4h2l2 12h11l2-8H7M9 20h.01M17 20h.01' },
  { to: '/recettes', label: 'Recettes', short: 'Recettes', icon: 'M6 3v8a3 3 0 006 0V3M9 3v18M17 3c-2 2-2 6 0 8v10' },
  { to: '/catalogue', label: 'Catalogue', short: 'Catalogue', icon: 'M4 5h16M4 12h16M4 19h16' },
  { to: '/budget', label: 'Budget', short: 'Budget', icon: 'M4 7h16v12H4zM4 11h16M15 15h2', parentOnly: true },
  { to: '/famille', label: 'Famille', short: 'Famille', icon: 'M9 11a3 3 0 100-6 3 3 0 000 6zM3 20a6 6 0 0112 0M17 11a3 3 0 100-6M21 20a6 6 0 00-4-5.7' },
]

function Icon({ path }: { path: string }) {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d={path} />
    </svg>
  )
}

export function AppShell({ children }: { children: ReactNode }) {
  const { member, isParent, logout } = useSession()
  const links = NAV.filter((n) => !n.parentOnly || isParent)
  return (
    <div className="min-h-dvh md:flex">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:left-2 focus:top-2 focus:z-50 focus:rounded focus:bg-surface focus:px-3 focus:py-2">
        Aller au contenu
      </a>
      <aside className="sticky top-0 hidden h-dvh w-60 shrink-0 flex-col border-r border-line bg-surface md:flex">
        <div className="flex items-center gap-2 px-5 py-5 text-lg font-bold text-accent">
          <img src={`${import.meta.env.BASE_URL}favicon.svg`} alt="" className="h-7 w-7" /> Famille
        </div>
        <nav aria-label="Navigation principale" className="flex-1 space-y-1 px-3">
          {links.map((n) => (
            <Link key={n.to} to={n.to} activeOptions={{ exact: n.exact ?? false }}
              className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-ink-2 hover:bg-surface-2"
              activeProps={{ className: 'bg-accent-soft text-accent', 'aria-current': 'page' }}>
              <Icon path={n.icon} /> {n.label}
            </Link>
          ))}
        </nav>
        <div className="flex items-center gap-2 border-t border-line px-4 py-3 text-sm">
          <MemberDot member={member} decorative />
          <span className="flex-1 truncate">{member.name}</span>
          <button type="button" onClick={logout} className="text-xs text-ink-3 hover:text-ink">Déconnexion</button>
        </div>
      </aside>

      <header className="sticky top-0 z-30 flex items-center justify-between border-b border-line bg-surface/90 px-4 py-3 backdrop-blur md:hidden">
        <span className="flex items-center gap-2 font-bold text-accent">
          <img src={`${import.meta.env.BASE_URL}favicon.svg`} alt="" className="h-6 w-6" /> Famille
        </span>
        <button type="button" onClick={logout} className="text-xs text-ink-3">Déconnexion</button>
      </header>

      <main id="main" className="mx-auto w-full max-w-6xl flex-1 px-4 pt-6 pb-28 md:px-8 md:py-8">{children}</main>

      <nav aria-label="Navigation principale" className="fixed inset-x-0 bottom-0 z-30 flex overflow-x-auto border-t border-line bg-surface/95 pb-[env(safe-area-inset-bottom)] backdrop-blur md:hidden">
        {links.map((n) => (
          <Link key={n.to} to={n.to} activeOptions={{ exact: n.exact ?? false }}
            className="flex min-w-[4.5rem] flex-1 flex-col items-center gap-0.5 px-2 py-2 text-[11px] text-ink-3"
            activeProps={{ className: 'text-accent', 'aria-current': 'page' }}>
            <Icon path={n.icon} /> {n.short}
          </Link>
        ))}
      </nav>
    </div>
  )
}
