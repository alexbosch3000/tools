// Family members, the personal calendar subscription and, for parents, the database backup.
import { useState } from 'react'
import { del, post, put } from '../lib/api.js'
import { MEMBER_COLORS, ROLES } from '../lib/labels.js'
import { useSession } from '../lib/session.js'
import { useSubmit } from '../lib/useSubmit.js'
import { Badge, Button, Card, ErrorMessage, Field, MemberDot, Modal, PageHeader, Select } from '../components/ui.jsx'

function MemberModal({ member, onClose, onSaved }) {
  const { members } = useSession()
  const [form, setForm] = useState({
    name: member?.name ?? '', color: member?.color ?? MEMBER_COLORS[members.length % MEMBER_COLORS.length],
    role: member?.role ?? 'CHILD', canLogin: Boolean(member?.username), username: member?.username ?? '', password: '',
  })
  const { busy, error, run } = useSubmit()
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const save = (e) => {
    e.preventDefault()
    run(async () => {
      const body = { name: form.name, color: form.color, role: form.role,
        username: form.canLogin ? form.username : null, password: form.canLogin && form.password ? form.password : null }
      if (member) await put(`/api/members/${member.id}`, body)
      else await post('/api/members', body)
      onSaved()
    })
  }
  return (
    <Modal title={member ? 'Modifier le membre' : 'Nouveau membre'} onClose={onClose} footer={(
      <>
        {member && <Button variant="danger" className="mr-auto" disabled={busy}
          onClick={() => run(async () => { await del(`/api/members/${member.id}`); onSaved() })}>Supprimer</Button>}
        <Button variant="primary" type="submit" form="member-form" disabled={busy}>Enregistrer</Button>
      </>
    )}>
      <form id="member-form" onSubmit={save} className="space-y-3">
        <div className="grid grid-cols-[1fr_auto] gap-3">
          <Field label="Prénom"><input className="input" required maxLength={60} value={form.name} onChange={(e) => set({ name: e.target.value })} /></Field>
          <Field label="Couleur"><input type="color" className="h-10 w-14" value={form.color} onChange={(e) => set({ color: e.target.value })} /></Field>
        </div>
        <Field label="Rôle" hint="Les enfants n'ont pas accès au budget.">
          <Select options={ROLES} value={form.role} onChange={(v) => set({ role: v })} />
        </Field>
        <label className="flex items-center gap-2 text-sm text-slate-700">
          <input type="checkbox" checked={form.canLogin} onChange={(e) => set({ canLogin: e.target.checked })} />
          Peut se connecter
        </label>
        {form.canLogin && (
          <div className="grid grid-cols-2 gap-3">
            <Field label="Identifiant"><input className="input" required pattern="[a-zA-Z0-9._\-]{3,32}" value={form.username}
              autoComplete="off" onChange={(e) => set({ username: e.target.value })} /></Field>
            <Field label="Mot de passe" hint={member?.username ? 'Vide : inchangé' : '8 caractères au moins'}>
              <input className="input" type="password" minLength={8} required={!member?.username} value={form.password}
                autoComplete="new-password" onChange={(e) => set({ password: e.target.value })} />
            </Field>
          </div>
        )}
        <ErrorMessage error={error} />
      </form>
    </Modal>
  )
}

function CalendarFeed() {
  const { member } = useSession()
  const [url, setUrl] = useState(null)
  const [active, setActive] = useState(member.hasCalendarFeed)
  const { busy, error, run } = useSubmit()
  const generate = () => run(async () => {
    const { token } = await post('/api/members/me/calendar-token')
    setUrl(`${window.location.origin}/api/planning/feed/${token}.ics`)
    setActive(true)
  })
  const revoke = () => run(async () => { await del('/api/members/me/calendar-token'); setUrl(null); setActive(false) })
  return (
    <Card title="Le planning dans mon téléphone">
      <p className="mb-3 text-sm text-slate-600">
        Abonnez votre agenda (Google Agenda, Calendrier Apple, Outlook) au planning familial : les entrées s'y affichent et
        se mettent à jour. L'adresse est personnelle et secrète.
      </p>
      {url && (
        <div className="mb-3 space-y-2">
          <input className="input font-mono text-xs" readOnly value={url} onFocus={(e) => e.target.select()} aria-label="Adresse d'abonnement" />
          <Button onClick={() => navigator.clipboard?.writeText(url)}>Copier l'adresse</Button>
        </div>
      )}
      <div className="flex flex-wrap gap-2">
        <Button variant="primary" disabled={busy} onClick={generate}>{active ? 'Générer une nouvelle adresse' : "Créer l'adresse d'abonnement"}</Button>
        {active && <Button variant="danger" disabled={busy} onClick={revoke}>Désactiver</Button>}
      </div>
      {active && !url && <p className="mt-2 text-xs text-slate-500">Une adresse est active. En générer une nouvelle désactive l'ancienne.</p>}
      <ErrorMessage error={error} />
    </Card>
  )
}

export default function FamilyPage() {
  const { members, isParent, reloadMembers, member: me } = useSession()
  const [modal, setModal] = useState(null)
  return (
    <>
      <PageHeader title="Famille" actions={isParent && <Button variant="primary" onClick={() => setModal({})}>Ajouter un membre</Button>} />
      <div className="grid gap-4 lg:grid-cols-2">
        <Card title="Membres">
          <ul className="divide-y divide-slate-100">
            {members.map((m) => (
              <li key={m.id} className="flex items-center gap-3 py-2">
                <MemberDot member={m} size="h-8 w-8 text-sm" decorative />
                <span className="flex-1">
                  <span className="block text-sm font-medium text-slate-800">{m.name}{m.id === me.id ? ' (vous)' : ''}</span>
                  <span className="text-xs text-slate-500">{m.username ? `Identifiant ${m.username}` : 'Sans connexion'}</span>
                </span>
                <Badge>{ROLES[m.role]}</Badge>
                {isParent && <Button variant="ghost" onClick={() => setModal({ member: m })}>Modifier</Button>}
              </li>
            ))}
          </ul>
        </Card>
        <div className="space-y-4">
          <CalendarFeed />
          {isParent && (
            <Card title="Sauvegarde">
              <p className="mb-3 text-sm text-slate-600">Téléchargez une copie complète et cohérente des données de la famille.</p>
              <a href="/api/admin/backup" className="inline-flex rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
                Télécharger la sauvegarde
              </a>
            </Card>
          )}
        </div>
      </div>
      {modal && <MemberModal member={modal.member} onClose={() => setModal(null)}
        onSaved={() => { setModal(null); reloadMembers() }} />}
    </>
  )
}
