// File picker that uploads a database backup to a restore endpoint, then sends the user back to the sign-in.
import { useRef } from 'react'
import { api, unwrap } from '../api/client.ts'
import { refresh } from '../app/queryClient.ts'
import { useAction } from '../app/useAction.ts'
import { useFeedback } from '../app/useFeedback.ts'
import { Button, ErrorMessage } from './ui.tsx'

interface Props {
  path: '/api/admin/restore' | '/api/setup/restore'
  label: string
  /** Question asked before replacing the data, none when there is nothing to lose. */
  confirmation?: string
}

export function RestoreUpload({ path, label, confirmation }: Props) {
  const { confirm } = useFeedback()
  const input = useRef<HTMLInputElement>(null)
  const restore = useAction((file: File) => unwrap(api().POST(path, { body: file as never,
    bodySerializer: (body) => body as BodyInit, headers: { 'Content-Type': 'application/octet-stream' } })), { refresh: [], inline: true, success: 'Sauvegarde restaurée : reconnectez-vous.', onSuccess: () => refresh('session') })

  return (
    <div className="space-y-2">
      <input ref={input} type="file" accept=".db" hidden aria-label={label}
        onChange={async (e) => {
          const file = e.target.files?.[0]
          e.target.value = ''
          if (!file) return
          if (confirmation && !await confirm({ title: 'Restaurer la sauvegarde ?', message: confirmation,
            confirmLabel: 'Restaurer' })) return
          restore.mutate(file)
        }} />
      <Button disabled={restore.isPending} onClick={() => input.current?.click()}>{label}</Button>
      <ErrorMessage error={restore.error} />
    </div>
  )
}
