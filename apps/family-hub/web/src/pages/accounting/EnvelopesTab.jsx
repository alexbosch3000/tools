// Envelopes: monthly budget of each expense category, its 50/30/20 group, and new categories.
import { useState } from 'react'
import { post, put } from '../../lib/api.js'
import { centsToInput, formatMoney, parseMoney } from '../../lib/format.js'
import { BUDGET_GROUPS } from '../../lib/labels.js'
import { useSubmit } from '../../lib/useSubmit.js'
import { Button, Card, ErrorMessage, Field, Select } from '../../components/ui.jsx'

function EnvelopeRow({ category, onSaved }) {
  const [budget, setBudget] = useState(centsToInput(category.monthlyBudgetCents))
  const [group, setGroup] = useState(category.group)
  const { busy, error, run, setError } = useSubmit()
  const changed = budget !== centsToInput(category.monthlyBudgetCents) || group !== category.group
  const save = () => {
    const cents = parseMoney(budget)
    if (Number.isNaN(cents)) { setError(new Error('Montant invalide.')); return }
    run(async () => { await put(`/api/accounting/categories/${category.code}`,
      { name: category.name, group, monthlyBudgetCents: cents }); onSaved() })
  }
  return (
    <li className="py-2">
      <div className="flex flex-wrap items-center gap-2">
        <span className="flex-1 text-sm text-slate-800">{category.name}</span>
        <div className="w-32"><Select options={BUDGET_GROUPS} value={group} onChange={(v) => setGroup(v)} aria-label="Groupe" /></div>
        <input className="input w-28 text-right" inputMode="decimal" placeholder="—" value={budget}
          onChange={(e) => setBudget(e.target.value)} aria-label={`Budget mensuel ${category.name}`} />
        <Button variant={changed ? 'primary' : 'ghost'} disabled={!changed || busy} onClick={save}>OK</Button>
      </div>
      <ErrorMessage error={error} />
    </li>
  )
}

function NewCategory({ onSaved }) {
  const [form, setForm] = useState({ name: '', direction: 'EXPENSE', group: 'NEEDS', budget: '' })
  const { busy, error, run, setError } = useSubmit()
  const submit = (e) => {
    e.preventDefault()
    const cents = parseMoney(form.budget)
    if (Number.isNaN(cents)) { setError(new Error('Montant invalide.')); return }
    run(async () => {
      await post('/api/accounting/categories', { name: form.name, direction: form.direction,
        group: form.direction === 'EXPENSE' ? form.group : null, monthlyBudgetCents: cents })
      setForm({ ...form, name: '', budget: '' })
      onSaved()
    })
  }
  return (
    <form onSubmit={submit} className="space-y-3">
      <Field label="Nom"><input className="input" required maxLength={60} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
      <Field label="Sens"><Select options={{ EXPENSE: 'Dépense', INCOME: 'Revenu' }} value={form.direction}
        onChange={(v) => setForm({ ...form, direction: v })} /></Field>
      {form.direction === 'EXPENSE' && (
        <>
          <Field label="Groupe"><Select options={BUDGET_GROUPS} value={form.group} onChange={(v) => setForm({ ...form, group: v })} /></Field>
          <Field label="Budget mensuel (€)"><input className="input" inputMode="decimal" value={form.budget}
            onChange={(e) => setForm({ ...form, budget: e.target.value })} /></Field>
        </>
      )}
      <ErrorMessage error={error} />
      <Button type="submit" variant="primary" disabled={busy}>Ajouter</Button>
    </form>
  )
}

export default function EnvelopesTab({ categories, reloadCategories }) {
  const expenses = categories.filter((c) => c.direction === 'EXPENSE')
  const total = expenses.reduce((sum, c) => sum + (c.monthlyBudgetCents ?? 0), 0)
  return (
    <div className="grid gap-4 lg:grid-cols-3">
      <Card title={`Enveloppes mensuelles · ${formatMoney(total)}`} className="lg:col-span-2">
        <p className="mb-2 text-sm text-slate-500">
          Une dizaine d'enveloppes suffit. Les besoins visent 50 % des revenus, les envies 30 %, l'épargne 20 %.
        </p>
        <ul className="divide-y divide-slate-100">
          {expenses.map((c) => <EnvelopeRow key={`${c.code}-${c.monthlyBudgetCents}-${c.group}`} category={c} onSaved={reloadCategories} />)}
        </ul>
      </Card>
      <Card title="Nouvelle catégorie"><NewCategory onSaved={reloadCategories} /></Card>
    </div>
  )
}
