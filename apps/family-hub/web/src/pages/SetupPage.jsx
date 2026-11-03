// First visit: creates the first parent, who then adds the rest of the family.
import { useState } from 'react'
import { post } from '../lib/api.js'
import { MEMBER_COLORS } from '../lib/labels.js'
import { useSubmit } from '../lib/useSubmit.js'
import { Button, ErrorMessage, Field } from '../components/ui.jsx'

export default function SetupPage({ onDone }) {
  const [form, setForm] = useState({ name: '', username: '', password: '', color: MEMBER_COLORS[0] })
  const { busy, error, run } = useSubmit()
  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  const submit = (e) => {
    e.preventDefault()
    run(async () => {
      await post('/api/setup', form)
      onDone()
    })
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <form onSubmit={submit} className="card w-full max-w-md space-y-4">
        <div>
          <h1 className="text-xl font-bold text-slate-800">Bienvenue</h1>
          <p className="mt-1 text-sm text-slate-500">
            Créez le premier compte parent. Vous ajouterez ensuite les autres membres de la famille.
          </p>
        </div>
        <Field label="Prénom"><input className="input" value={form.name} onChange={set('name')} required /></Field>
        <Field label="Identifiant" hint="3 à 32 caractères : lettres, chiffres, point, tiret.">
          <input className="input" autoComplete="username" value={form.username} onChange={set('username')}
            pattern="[a-zA-Z0-9._\-]{3,32}" required />
        </Field>
        <Field label="Mot de passe" hint="8 caractères au moins.">
          <input className="input" type="password" autoComplete="new-password" minLength={8} value={form.password}
            onChange={set('password')} required />
        </Field>
        <Field label="Couleur"><input type="color" value={form.color} onChange={set('color')} /></Field>
        <ErrorMessage error={error} />
        <Button type="submit" variant="primary" className="w-full" disabled={busy}>Créer la famille</Button>
      </form>
    </div>
  )
}
