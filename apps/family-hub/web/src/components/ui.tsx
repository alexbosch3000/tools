// Presentational building blocks shared by every page.
import type { ButtonHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react'
import type { Schemas } from '../api/client.ts'

const BUTTON_STYLES = {
  primary: 'bg-accent text-accent-ink hover:opacity-90',
  secondary: 'border border-line bg-surface text-ink hover:bg-surface-2',
  danger: 'border border-line bg-surface text-danger hover:bg-danger-soft',
  'danger-solid': 'bg-danger text-white hover:opacity-90 dark:text-red-950',
  ghost: 'text-ink-2 hover:bg-surface-2',
} as const

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: keyof typeof BUTTON_STYLES
}

export function Button({ variant = 'secondary', className = '', type = 'button', ...props }: ButtonProps) {
  return (
    <button type={type}
      className={`inline-flex min-h-10 items-center justify-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium
        transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent
        disabled:cursor-not-allowed disabled:opacity-50 ${BUTTON_STYLES[variant]} ${className}`}
      {...props} />
  )
}

export function Card({ title, actions, children, className = '' }:
  { title?: ReactNode, actions?: ReactNode, children: ReactNode, className?: string }) {
  return (
    <section className={`card ${className}`}>
      {(title || actions) && (
        <header className="mb-3 flex items-center justify-between gap-2">
          {title && <h2 className="text-sm font-semibold uppercase tracking-wide text-ink-3">{title}</h2>}
          {actions}
        </header>
      )}
      {children}
    </section>
  )
}

export function PageHeader({ title, subtitle, actions }: { title: string, subtitle?: ReactNode, actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-ink-2">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

export function Field({ label, hint, children, className = '' }:
  { label: string, hint?: string, children: ReactNode, className?: string }) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1 block text-sm font-medium text-ink-2">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-xs text-ink-3">{hint}</span>}
    </label>
  )
}

interface SelectProps<K extends string> extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'onChange' | 'value'> {
  options: Partial<Record<K, string>>
  value: K | '' | null | undefined
  onChange: (value: K | '') => void
  placeholder?: string
}

export function Select<K extends string>({ options, value, onChange, placeholder, ...props }: SelectProps<K>) {
  return (
    <select className="input" value={value ?? ''} onChange={(e) => onChange(e.target.value as K | '')} {...props}>
      {placeholder !== undefined && <option value="">{placeholder}</option>}
      {Object.entries(options).map(([code, label]) => <option key={code} value={code}>{label as string}</option>)}
    </select>
  )
}

export function Checkbox({ label, checked, onChange }: { label: string, checked: boolean, onChange: (v: boolean) => void }) {
  return (
    <label className="inline-flex items-center gap-2 text-sm text-ink-2">
      <input type="checkbox" className="h-4 w-4 accent-accent" checked={checked} onChange={(e) => onChange(e.target.checked)} />
      {label}
    </label>
  )
}

export function Badge({ children, className = 'bg-surface-2 text-ink-2' }: { children: ReactNode, className?: string }) {
  return <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${className}`}>{children}</span>
}

export function ErrorMessage({ error }: { error: Error | null | undefined }) {
  if (!error) return null
  return <p role="alert" className="rounded-lg bg-danger-soft px-3 py-2 text-sm text-danger">{error.message}</p>
}

export function Empty({ children }: { children: ReactNode }) {
  return <p className="py-6 text-center text-sm text-ink-3">{children}</p>
}

/** Placeholder lines shown while a page loads, so the layout does not jump. */
export function Loading({ lines = 3 }: { lines?: number }) {
  return (
    <div role="status" aria-label="Chargement" className="space-y-3 py-4">
      {Array.from({ length: lines }, (_, i) => (
        <div key={i} className="h-4 animate-pulse rounded bg-surface-2" style={{ width: `${90 - i * 15}%` }} />
      ))}
    </div>
  )
}

export function Tabs<K extends string>({ tabs, current, onChange }:
  { tabs: Record<K, string>, current: K, onChange: (tab: K) => void }) {
  return (
    <div role="tablist" className="mb-6 flex gap-1 overflow-x-auto border-b border-line">
      {(Object.entries(tabs) as [K, string][]).map(([code, label]) => (
        <button key={code} type="button" role="tab" aria-selected={current === code} onClick={() => onChange(code)}
          className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${current === code
            ? 'border-accent text-accent' : 'border-transparent text-ink-3 hover:text-ink'}`}>
          {label}
        </button>
      ))}
    </div>
  )
}

type MemberLike = Pick<Schemas['Member'], 'name' | 'color'>

/** Coloured initial of a member; `decorative` hides it from screen readers when the name is written next to it. */
export function MemberDot({ member, size = 'h-6 w-6 text-xs', decorative = false }:
  { member: MemberLike | null | undefined, size?: string, decorative?: boolean }) {
  if (!member) return null
  return (
    <span title={decorative ? undefined : member.name} aria-hidden={decorative || undefined} role={decorative ? undefined : 'img'}
      aria-label={decorative ? undefined : member.name} style={{ backgroundColor: member.color }}
      className={`inline-flex shrink-0 items-center justify-center rounded-full font-semibold text-white ${size}`}>
      {member.name.slice(0, 1).toUpperCase()}
    </span>
  )
}

/** Toggle chips to pick several members. */
export function MemberPicker({ members, value, onChange }:
  { members: Schemas['Member'][], value: number[], onChange: (ids: number[]) => void }) {
  const toggle = (id: number) => onChange(value.includes(id) ? value.filter((v) => v !== id) : [...value, id])
  return (
    <div className="flex flex-wrap gap-2">
      {members.map((m) => {
        const active = value.includes(m.id)
        return (
          <button key={m.id} type="button" onClick={() => toggle(m.id)} aria-pressed={active}
            className={`inline-flex min-h-9 items-center gap-1.5 rounded-full border px-2.5 py-1 text-sm ${active
              ? 'border-accent bg-accent-soft text-accent' : 'border-line text-ink-2'}`}>
            <MemberDot member={m} size="h-5 w-5 text-[10px]" decorative /> {m.name}
          </button>
        )
      })}
    </div>
  )
}
