// Small presentational building blocks shared by every page.
import { useEffect, useId, useRef } from 'react'

const BUTTON_STYLES = {
  primary: 'bg-indigo-600 text-white hover:bg-indigo-700 disabled:bg-indigo-300',
  secondary: 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 disabled:text-slate-400',
  danger: 'border border-red-200 bg-white text-red-600 hover:bg-red-50 disabled:text-red-300',
  ghost: 'text-slate-600 hover:bg-slate-100',
}

export function Button({ variant = 'secondary', className = '', type = 'button', ...props }) {
  return (
    <button
      type={type}
      className={`inline-flex items-center justify-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium
        transition-colors disabled:cursor-not-allowed ${BUTTON_STYLES[variant]} ${className}`}
      {...props}
    />
  )
}

export function Card({ title, actions, children, className = '' }) {
  return (
    <section className={`card ${className}`}>
      {(title || actions) && (
        <header className="mb-3 flex items-center justify-between gap-2">
          {title && <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">{title}</h2>}
          {actions}
        </header>
      )}
      {children}
    </section>
  )
}

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl font-bold text-slate-800">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

/** Dialog closed by Escape or by clicking the backdrop; the first field gets the focus. */
export function Modal({ title, onClose, children, footer, wide = false }) {
  const titleId = useId()
  const panel = useRef(null)
  const close = useRef(onClose)
  useEffect(() => { close.current = onClose })
  // Focus and the Escape handler are set once: re-rendering the form must not steal the focus.
  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape') close.current() }
    window.addEventListener('keydown', onKey)
    panel.current?.querySelector('input, select, textarea')?.focus()
    return () => window.removeEventListener('keydown', onKey)
  }, [])
  return (
    <div className="fixed inset-0 z-40 flex items-start justify-center overflow-y-auto bg-slate-900/40 p-4 sm:items-center"
      onMouseDown={(e) => { if (e.target === e.currentTarget) onClose() }}>
      <div ref={panel} role="dialog" aria-modal="true" aria-labelledby={titleId}
        className={`w-full ${wide ? 'max-w-2xl' : 'max-w-lg'} rounded-2xl bg-white shadow-xl`}>
        <header className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
          <h2 id={titleId} className="text-lg font-semibold text-slate-800">{title}</h2>
          <button type="button" onClick={onClose} aria-label="Fermer"
            className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600">✕</button>
        </header>
        <div className="max-h-[70vh] space-y-4 overflow-y-auto px-5 py-4">{children}</div>
        {footer && <footer className="flex flex-wrap justify-end gap-2 border-t border-slate-100 px-5 py-3">{footer}</footer>}
      </div>
    </div>
  )
}

export function Field({ label, hint, children, className = '' }) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1 block text-sm font-medium text-slate-700">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-xs text-slate-500">{hint}</span>}
    </label>
  )
}

export function Select({ options, value, onChange, placeholder, ...props }) {
  return (
    <select className="input" value={value ?? ''} onChange={(e) => onChange(e.target.value || null)} {...props}>
      {placeholder !== undefined && <option value="">{placeholder}</option>}
      {Object.entries(options).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
    </select>
  )
}

export function Checkbox({ label, checked, onChange }) {
  return (
    <label className="inline-flex items-center gap-2 text-sm text-slate-700">
      <input type="checkbox" className="h-4 w-4 rounded border-slate-300 text-indigo-600"
        checked={checked} onChange={(e) => onChange(e.target.checked)} />
      {label}
    </label>
  )
}

export function Badge({ children, className = 'bg-slate-100 text-slate-700' }) {
  return <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${className}`}>{children}</span>
}

export function ErrorMessage({ error }) {
  if (!error) return null
  return <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error.message ?? String(error)}</p>
}

export function Empty({ children }) {
  return <p className="py-6 text-center text-sm text-slate-400">{children}</p>
}

export function Loading() {
  return <p className="py-6 text-center text-sm text-slate-400">Chargement…</p>
}

export function Tabs({ tabs, current, onChange }) {
  return (
    <nav className="mb-6 flex gap-1 overflow-x-auto border-b border-slate-200">
      {Object.entries(tabs).map(([code, label]) => (
        <button key={code} type="button" onClick={() => onChange(code)}
          className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${current === code
            ? 'border-indigo-600 text-indigo-700' : 'border-transparent text-slate-500 hover:text-slate-700'}`}>
          {label}
        </button>
      ))}
    </nav>
  )
}

/** Coloured initial of a member; `decorative` hides it from screen readers when the name is written next to it. */
export function MemberDot({ member, size = 'h-6 w-6 text-xs', decorative = false }) {
  if (!member) return null
  return (
    <span title={decorative ? undefined : member.name} aria-hidden={decorative || undefined} style={{ backgroundColor: member.color }}
      className={`inline-flex shrink-0 items-center justify-center rounded-full font-semibold text-white ${size}`}>
      {member.name.slice(0, 1).toUpperCase()}
    </span>
  )
}

/** Toggle chips to pick several members. */
export function MemberPicker({ members, value, onChange }) {
  const toggle = (id) => onChange(value.includes(id) ? value.filter((v) => v !== id) : [...value, id])
  return (
    <div className="flex flex-wrap gap-2">
      {members.map((m) => {
        const active = value.includes(m.id)
        return (
          <button key={m.id} type="button" onClick={() => toggle(m.id)} aria-pressed={active}
            className={`inline-flex items-center gap-1.5 rounded-full border px-2 py-1 text-sm ${active
              ? 'border-indigo-500 bg-indigo-50 text-indigo-800' : 'border-slate-200 text-slate-600'}`}>
            <MemberDot member={m} size="h-5 w-5 text-[10px]" decorative /> {m.name}
          </button>
        )
      })}
    </div>
  )
}
