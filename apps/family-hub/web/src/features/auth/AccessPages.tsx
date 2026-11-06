// Screens shown before the application: server address (Pages), first-run setup, login, server unreachable.
import { useState, type FormEvent, type ReactNode } from 'react'
import { useMutation } from '@tanstack/react-query'
import { api, unwrap } from '../../api/client.ts'
import { normalizeServerUrl, remoteMode } from '../../app/connection.ts'
import { signIn } from '../../app/useSession.ts'
import { MEMBER_COLORS } from '../../lib/labels.ts'
import { RestoreUpload } from '../../components/RestoreUpload.tsx'
import { Button, ErrorMessage, Field } from '../../components/ui.tsx'

function Frame({ title, intro, children }: { title: string, intro?: ReactNode, children: ReactNode }) {
  return (
    <main className="flex min-h-dvh items-center justify-center p-4">
      <div className="card w-full max-w-md space-y-4 p-6">
        <div className="flex items-center gap-3">
          <img src={`${import.meta.env.BASE_URL}favicon.svg`} alt="" className="h-9 w-9" />
          <h1 className="text-xl font-bold">{title}</h1>
        </div>
        {intro && <p className="text-sm text-ink-2">{intro}</p>}
        {children}
      </div>
    </main>
  )
}

export function ServerPage({ onConnect }: { onConnect: (url: string) => void }) {
  const [address, setAddress] = useState('')
  const check = useMutation({
    mutationFn: async (url: string) => {
      const response = await fetch(`${url}/api/session`).catch(() => null)
      if (!response?.ok) throw new Error("Aucun serveur family-hub ne répond à cette adresse, ou il n'autorise pas cette page.")
      return url
    },
    onSuccess: onConnect,
  })
  const submit = (event: FormEvent) => {
    event.preventDefault()
    check.mutate(normalizeServerUrl(address))
  }
  return (
    <Frame title="Connexion au serveur familial"
      intro="Cette page est l'interface de family-hub. Les données de la famille restent sur votre serveur (par exemple le NAS) : indiquez son adresse.">
      <form onSubmit={submit} className="space-y-4">
        <Field label="Adresse du serveur" hint="Par exemple https://famille.mondomaine.fr — le serveur doit autoriser cette page (APP_ALLOWED_ORIGINS).">
          <input className="input" inputMode="url" autoComplete="url" required value={address}
            onChange={(e) => setAddress(e.target.value)} placeholder="https://famille.mondomaine.fr" />
        </Field>
        <ErrorMessage error={check.error} />
        <Button type="submit" variant="primary" className="w-full" disabled={check.isPending}>Se connecter au serveur</Button>
      </form>
    </Frame>
  )
}

export function SetupPage() {
  const [form, setForm] = useState({ name: '', username: '', password: '', color: MEMBER_COLORS[0] ?? '#2563eb' })
  const setup = useMutation({ mutationFn: () => unwrap(api().POST('/api/setup', { body: form })), onSuccess: signIn })
  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [key]: e.target.value })
  return (
    <Frame title="Bienvenue" intro="Créez le premier compte parent. Vous ajouterez ensuite les autres membres de la famille.">
      <form onSubmit={(e) => { e.preventDefault(); setup.mutate() }} className="space-y-4">
        <Field label="Prénom"><input className="input" autoComplete="given-name" required maxLength={60} value={form.name} onChange={set('name')} /></Field>
        <Field label="Identifiant" hint="3 à 32 caractères : lettres, chiffres, point, tiret.">
          <input className="input" autoComplete="username" required pattern="[a-zA-Z0-9._\-]{3,32}" value={form.username} onChange={set('username')} />
        </Field>
        <Field label="Mot de passe" hint="8 caractères au moins.">
          <input className="input" type="password" autoComplete="new-password" required minLength={8} value={form.password} onChange={set('password')} />
        </Field>
        <Field label="Couleur"><input type="color" className="h-10 w-16 rounded" value={form.color} onChange={set('color')} /></Field>
        <ErrorMessage error={setup.error} />
        <Button type="submit" variant="primary" className="w-full" disabled={setup.isPending}>Créer la famille</Button>
      </form>
      <div className="space-y-2 border-t border-line pt-4">
        <p className="text-sm text-ink-2">Ou restaurer une sauvegarde de family-hub (fichier .db) :</p>
        <RestoreUpload path="/api/setup/restore" label="Restaurer une sauvegarde" />
      </div>
    </Frame>
  )
}

export function LoginPage({ changeServer }: { changeServer?: () => void }) {
  const [form, setForm] = useState({ username: '', password: '' })
  const login = useMutation({ mutationFn: () => unwrap(api().POST('/api/auth/login', { body: form })), onSuccess: signIn })
  return (
    <Frame title="Connexion">
      <form onSubmit={(e) => { e.preventDefault(); login.mutate() }} className="space-y-4">
        <Field label="Identifiant">
          <input className="input" autoComplete="username" required value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })} />
        </Field>
        <Field label="Mot de passe">
          <input className="input" type="password" autoComplete="current-password" required value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </Field>
        <ErrorMessage error={login.error} />
        <Button type="submit" variant="primary" className="w-full" disabled={login.isPending}>Se connecter</Button>
        {changeServer && <Button variant="ghost" className="w-full" onClick={changeServer}>Changer de serveur</Button>}
      </form>
    </Frame>
  )
}

export function Unreachable({ error, retry, changeServer }: { error: Error, retry: () => void, changeServer: () => void }) {
  return (
    <Frame title="Serveur injoignable">
      <ErrorMessage error={error} />
      <div className="flex gap-2">
        <Button variant="primary" onClick={retry}>Réessayer</Button>
        {remoteMode && <Button onClick={changeServer}>Changer de serveur</Button>}
      </div>
    </Frame>
  )
}
