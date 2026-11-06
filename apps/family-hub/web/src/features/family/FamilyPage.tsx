// Family members with their sizes, the personal calendar subscription and, for parents, the database backup.
import { useState } from 'react'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { publicApiOrigin } from '../../app/connection.ts'
import { useFeedback } from '../../app/useFeedback.ts'
import { useSession } from '../../app/useSession.ts'
import { useAction } from '../../app/useAction.ts'
import { MEMBER_COLORS, ROLES } from '../../lib/labels.ts'
import { Dialog } from '../../components/Dialog.tsx'
import { RestoreUpload } from '../../components/RestoreUpload.tsx'
import { Badge, Button, Card, ErrorMessage, Field, MemberDot, PageHeader, Select } from '../../components/ui.tsx'

function MemberDialog({ member, onClose }: { member?: Schemas['Member'], onClose: () => void }) {
  const { members } = useSession()
  const { confirm } = useFeedback()
  const [form, setForm] = useState({
    name: member?.name ?? '', color: member?.color ?? MEMBER_COLORS[members.length % MEMBER_COLORS.length] ?? '#2563eb',
    role: member?.role ?? 'CHILD' as Schemas['Role'], canLogin: Boolean(member?.username), username: member?.username ?? '', password: '',
    clothingSize: member?.clothingSize ?? '', shoeSize: member?.shoeSize ?? '',
  })
  const set = (patch: Partial<typeof form>) => setForm((f) => ({ ...f, ...patch }))
  const save = useAction(() => {
    const body: Schemas['MemberRequest'] = { name: form.name, color: form.color, role: form.role,
      username: form.canLogin ? form.username : null, password: form.canLogin && form.password ? form.password : null,
      clothingSize: form.clothingSize || null, shoeSize: form.shoeSize || null }
    return member
      ? unwrap(api().PUT('/api/members/{id}', { params: { path: { id: member.id } }, body }))
      : unwrap(api().POST('/api/members', { body }))
  }, { refresh: ['members', 'session'], inline: true, success: 'Membre enregistré', onSuccess: onClose })
  const remove = useAction(() => unwrap(api().DELETE('/api/members/{id}', { params: { path: { id: member!.id } } })),
    { refresh: ['members', 'planning', 'todo', 'dashboard'], inline: true, success: 'Membre supprimé', onSuccess: onClose })
  return (
    <Dialog title={member ? 'Modifier le membre' : 'Nouveau membre'} onClose={onClose} footer={(
      <>
        {member && <Button variant="danger" className="mr-auto" onClick={async () => {
          if (await confirm({ title: `Supprimer ${member.name} ?`, message: 'Ses tâches restent, sans personne assignée.' })) remove.mutate(undefined)
        }}>Supprimer</Button>}
        <Button variant="primary" type="submit" form="member-form" disabled={save.isPending}>Enregistrer</Button>
      </>
    )}>
      <form id="member-form" onSubmit={(e) => { e.preventDefault(); save.mutate(undefined) }} className="space-y-3">
        <div className="grid grid-cols-[1fr_auto] gap-3">
          <Field label="Prénom"><input className="input" required maxLength={60} value={form.name} onChange={(e) => set({ name: e.target.value })} /></Field>
          <Field label="Couleur"><input type="color" className="h-10 w-14 rounded" value={form.color} onChange={(e) => set({ color: e.target.value })} /></Field>
        </div>
        <Field label="Rôle" hint="Les enfants n'ont pas accès au budget."><Select options={ROLES} value={form.role} onChange={(v) => set({ role: v || 'CHILD' })} /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Taille de vêtements" hint="Ex. 8 ans, M, 38"><input className="input" maxLength={20} value={form.clothingSize} onChange={(e) => set({ clothingSize: e.target.value })} /></Field>
          <Field label="Pointure"><input className="input" maxLength={20} value={form.shoeSize} onChange={(e) => set({ shoeSize: e.target.value })} /></Field>
        </div>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" className="h-4 w-4 accent-accent" checked={form.canLogin} onChange={(e) => set({ canLogin: e.target.checked })} /> Peut se connecter
        </label>
        {form.canLogin && (
          <div className="grid grid-cols-2 gap-3">
            <Field label="Identifiant"><input className="input" required pattern="[a-zA-Z0-9._\-]{3,32}" autoComplete="off" value={form.username}
              onChange={(e) => set({ username: e.target.value })} /></Field>
            <Field label="Mot de passe" hint={member?.username ? 'Vide : inchangé. Le changer déconnecte ses appareils.' : '8 caractères au moins'}>
              <input className="input" type="password" minLength={8} required={!member?.username} autoComplete="new-password" value={form.password}
                onChange={(e) => set({ password: e.target.value })} />
            </Field>
          </div>
        )}
        <ErrorMessage error={save.error ?? remove.error} />
      </form>
    </Dialog>
  )
}

function CalendarFeed() {
  const { member } = useSession()
  const { notify } = useFeedback()
  const [url, setUrl] = useState<string | null>(null)
  const [active, setActive] = useState(member.hasCalendarFeed)
  const generate = useAction(() => unwrap(api().POST('/api/members/me/calendar-token')), {
    refresh: ['members'], onSuccess: ({ token }) => { setUrl(`${publicApiOrigin()}/api/planning/feed/${token}.ics`); setActive(true) } })
  const revoke = useAction(() => unwrap(api().DELETE('/api/members/me/calendar-token')), {
    refresh: ['members'], success: 'Abonnement désactivé', onSuccess: () => { setUrl(null); setActive(false) } })
  return (
    <Card title="Le planning dans mon téléphone">
      <p className="mb-3 text-sm text-ink-2">
        Abonnez votre agenda (Google Agenda, Calendrier Apple, Outlook) au planning familial : les entrées s'y affichent et se mettent à jour.
        L'adresse est personnelle et secrète.
      </p>
      {url && (
        <div className="mb-3 space-y-2">
          <input className="input font-mono text-xs" readOnly value={url} onFocus={(e) => e.target.select()} aria-label="Adresse d'abonnement" />
          <Button onClick={() => navigator.clipboard?.writeText(url).then(() => notify('Adresse copiée'))}>Copier l'adresse</Button>
        </div>
      )}
      <div className="flex flex-wrap gap-2">
        <Button variant="primary" disabled={generate.isPending} onClick={() => generate.mutate(undefined)}>
          {active ? 'Générer une nouvelle adresse' : "Créer l'adresse d'abonnement"}
        </Button>
        {active && <Button variant="danger" disabled={revoke.isPending} onClick={() => revoke.mutate(undefined)}>Désactiver</Button>}
      </div>
      {active && !url && <p className="mt-2 text-xs text-ink-3">Une adresse est active. En générer une nouvelle désactive l'ancienne.</p>}
    </Card>
  )
}

function Backup() {
  const download = useAction(async () => {
    const blob = await unwrap(api().GET('/api/admin/backup', { parseAs: 'blob' }))
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob as Blob)
    link.download = `family-hub-${new Date().toISOString().slice(0, 10)}.db`
    link.click()
    URL.revokeObjectURL(link.href)
  }, { success: 'Sauvegarde téléchargée' })
  return (
    <Card title="Sauvegarde">
      <p className="mb-3 text-sm text-ink-2">Téléchargez une copie complète et cohérente des données de la famille.</p>
      <Button disabled={download.isPending} onClick={() => download.mutate(undefined)}>Télécharger la sauvegarde</Button>
      <ErrorMessage error={download.error} />
      <div className="mt-4 border-t border-line pt-4">
        <RestoreUpload path="/api/admin/restore" label="Restaurer une sauvegarde"
          confirmation="Toutes les données seront remplacées par celles de la sauvegarde, y compris les comptes et mots de passe." />
      </div>
    </Card>
  )
}

export function FamilyPage() {
  const { members, isParent, member: me, changeServer } = useSession()
  const [modal, setModal] = useState<{ member?: Schemas['Member'] } | null>(null)
  return (
    <>
      <PageHeader title="Famille" actions={isParent && <Button variant="primary" onClick={() => setModal({})}>Ajouter un membre</Button>} />
      <div className="grid gap-4 lg:grid-cols-2">
        <Card title="Membres">
          <ul className="divide-y divide-line">
            {members.map((m) => (
              <li key={m.id} className="flex items-center gap-3 py-2">
                <MemberDot member={m} size="h-8 w-8 text-sm" decorative />
                <span className="min-w-0 flex-1">
                  <span className="block text-sm font-medium">{m.name}{m.id === me.id ? ' (vous)' : ''}</span>
                  <span className="block truncate text-xs text-ink-3">
                    {[m.username ? `Identifiant ${m.username}` : 'Sans connexion', m.clothingSize && `taille ${m.clothingSize}`,
                      m.shoeSize && `pointure ${m.shoeSize}`].filter(Boolean).join(' · ')}
                  </span>
                </span>
                <Badge>{ROLES[m.role]}</Badge>
                {isParent && <Button variant="ghost" onClick={() => setModal({ member: m })} aria-label={`Modifier ${m.name}`}>Modifier</Button>}
              </li>
            ))}
          </ul>
        </Card>
        <div className="space-y-4">
          <CalendarFeed />
          {isParent && <Backup />}
          <Card title="Serveur">
            <p className="mb-3 break-all text-sm text-ink-2">{publicApiOrigin()}</p>
            <Button onClick={changeServer}>Changer de serveur</Button>
          </Card>
        </div>
      </div>
      {modal && <MemberDialog member={modal.member} onClose={() => setModal(null)} />}
    </>
  )
}
