// Modal dialog on the native <dialog> element: focus trap, Escape, inert page and focus return come from the browser.
import { useEffect, useId, useRef, type ReactNode } from 'react'

interface Props {
  title: string
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
  wide?: boolean
}

export function Dialog({ title, onClose, children, footer, wide = false }: Props) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  const close = useRef(onClose)
  useEffect(() => { close.current = onClose })

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return undefined
    if (typeof dialog.showModal === 'function' && !dialog.open) dialog.showModal()
    const onCancel = (event: Event) => {
      event.preventDefault()
      close.current()
    }
    dialog.addEventListener('cancel', onCancel)
    return () => {
      dialog.removeEventListener('cancel', onCancel)
      if (dialog.open && typeof dialog.close === 'function') dialog.close()
    }
  }, [])

  return (
    <dialog ref={ref} aria-labelledby={titleId}
      onClick={(event) => { if (event.target === ref.current) close.current() }}
      className={`m-auto w-[calc(100%-2rem)] ${wide ? 'max-w-2xl' : 'max-w-lg'} rounded-2xl border border-line bg-surface
        p-0 text-ink shadow-2xl`}>
      <header className="flex items-center justify-between border-b border-line px-5 py-4">
        <h2 id={titleId} className="text-lg font-semibold">{title}</h2>
        <button type="button" onClick={() => close.current()} aria-label="Fermer"
          className="rounded-lg p-1.5 text-ink-3 hover:bg-surface-2 hover:text-ink">
          <svg viewBox="0 0 20 20" className="h-5 w-5" aria-hidden="true">
            <path d="M5 5l10 10M15 5L5 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </svg>
        </button>
      </header>
      <div className="max-h-[70vh] space-y-4 overflow-y-auto px-5 py-4">{children}</div>
      {footer && <footer className="flex flex-wrap justify-end gap-2 border-t border-line px-5 py-3">{footer}</footer>}
    </dialog>
  )
}
