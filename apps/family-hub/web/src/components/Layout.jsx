// Application frame: navigation (sidebar on large screens, scrolling bar on phones) and page area.
import { useSession } from '../lib/session.js'
import { MemberDot } from './ui.jsx'

const NAV = [
  ['/', 'Tableau de bord'],
  ['/planning', 'Planning'],
  ['/todo', 'À faire'],
  ['/courses', 'Courses'],
  ['/recettes', 'Recettes'],
  ['/catalogue', 'Catalogue'],
  ['/budget', 'Budget', true],
  ['/famille', 'Famille'],
]

function isActive(route, path) {
  const current = route.split('?')[0]
  return path === '/' ? current === '/' : current === path || current.startsWith(`${path}/`)
}

export default function Layout({ route, children }) {
  const { member, isParent, logout } = useSession()
  const links = NAV.filter(([, , parentOnly]) => !parentOnly || isParent)
  return (
    <div className="min-h-screen bg-slate-50 md:flex">
      <aside className="hidden w-56 shrink-0 flex-col border-r border-slate-200 bg-white md:flex">
        <div className="px-5 py-5 text-lg font-bold text-indigo-700">Famille</div>
        <nav className="flex-1 space-y-1 px-3">
          {links.map(([path, label]) => (
            <a key={path} href={`#${path}`}
              className={`block rounded-lg px-3 py-2 text-sm font-medium ${isActive(route, path)
                ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:bg-slate-100'}`}>
              {label}
            </a>
          ))}
        </nav>
        <div className="flex items-center gap-2 border-t border-slate-100 px-4 py-3 text-sm">
          <MemberDot member={member} decorative />
          <span className="flex-1 truncate text-slate-700">{member.name}</span>
          <button type="button" onClick={logout} className="text-xs text-slate-500 hover:text-slate-800">
            Déconnexion
          </button>
        </div>
      </aside>

      <header className="sticky top-0 z-30 border-b border-slate-200 bg-white md:hidden">
        <div className="flex items-center justify-between px-4 py-3">
          <span className="font-bold text-indigo-700">Famille</span>
          <button type="button" onClick={logout} className="text-xs text-slate-500">Déconnexion</button>
        </div>
        <nav className="flex gap-1 overflow-x-auto px-2 pb-2">
          {links.map(([path, label]) => (
            <a key={path} href={`#${path}`}
              className={`whitespace-nowrap rounded-full px-3 py-1 text-sm ${isActive(route, path)
                ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-600'}`}>
              {label}
            </a>
          ))}
        </nav>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 md:px-8 md:py-8">{children}</main>
    </div>
  )
}
