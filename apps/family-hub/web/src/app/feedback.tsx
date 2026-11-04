// Toast notifications and confirmation dialogs, available to every page through hooks.
import { useCallback, useMemo, useRef, useState, type ReactNode } from 'react'
import { Button } from '../components/ui.tsx'
import { Dialog } from '../components/Dialog.tsx'
import { FeedbackContext, type Confirmation } from './useFeedback.ts'

interface Toast {
  id: number
  message: string
  tone: 'success' | 'error'
}


export function FeedbackProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const [pending, setPending] = useState<(Confirmation & { resolve: (ok: boolean) => void }) | null>(null)
  const nextId = useRef(1)

  const notify = useCallback((message: string, tone: Toast['tone'] = 'success') => {
    const id = nextId.current++
    setToasts((current) => [...current, { id, message, tone }])
    setTimeout(() => setToasts((current) => current.filter((t) => t.id !== id)), tone === 'error' ? 7000 : 3500)
  }, [])

  const confirm = useCallback((confirmation: Confirmation) =>
    new Promise<boolean>((resolve) => setPending({ ...confirmation, resolve })), [])

  const close = (ok: boolean) => {
    pending?.resolve(ok)
    setPending(null)
  }

  const value = useMemo(() => ({ notify, confirm }), [notify, confirm])
  return (
    <FeedbackContext.Provider value={value}>
      {children}
      <div aria-live="polite" className="pointer-events-none fixed inset-x-0 bottom-4 z-50 flex flex-col items-center gap-2 px-4">
        {toasts.map((toast) => (
          <p key={toast.id} role={toast.tone === 'error' ? 'alert' : 'status'}
            className={`pointer-events-auto rounded-lg px-4 py-2 text-sm font-medium shadow-lg ${toast.tone === 'error'
              ? 'bg-danger text-white dark:text-red-950' : 'bg-ink text-canvas'}`}>
            {toast.message}
          </p>
        ))}
      </div>
      {pending && (
        <Dialog title={pending.title} onClose={() => close(false)} footer={(
          <>
            <Button onClick={() => close(false)}>Annuler</Button>
            <Button variant="danger-solid" onClick={() => close(true)}>{pending.confirmLabel ?? 'Supprimer'}</Button>
          </>
        )}>
          <p className="text-sm text-ink-2">{pending.message}</p>
        </Dialog>
      )}
    </FeedbackContext.Provider>
  )
}
