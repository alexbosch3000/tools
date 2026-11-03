// Login form.
import { useState } from 'react'
import { post } from '../lib/api.js'
import { useSubmit } from '../lib/useSubmit.js'
import { Button, ErrorMessage, Field } from '../components/ui.jsx'

export default function LoginPage({ onDone }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const { busy, error, run } = useSubmit()

  const submit = (e) => {
    e.preventDefault()
    run(async () => {
      await post('/api/auth/login', { username, password })
      onDone()
    })
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <form onSubmit={submit} className="card w-full max-w-sm space-y-4">
        <h1 className="text-xl font-bold text-slate-800">Connexion</h1>
        <Field label="Identifiant">
          <input className="input" autoComplete="username" value={username}
            onChange={(e) => setUsername(e.target.value)} required />
        </Field>
        <Field label="Mot de passe">
          <input className="input" type="password" autoComplete="current-password" value={password}
            onChange={(e) => setPassword(e.target.value)} required />
        </Field>
        <ErrorMessage error={error} />
        <Button type="submit" variant="primary" className="w-full" disabled={busy}>Se connecter</Button>
      </form>
    </div>
  )
}
