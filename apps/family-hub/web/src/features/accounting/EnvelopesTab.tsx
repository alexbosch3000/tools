// Envelopes: monthly budget of each expense category and its 50/30/20 group, and new categories.
import { useState } from 'react'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useAction } from '../../app/useAction.ts'
import { centsToInput, formatMoney, parseMoney } from '../../lib/format.ts'
import { BUDGET_GROUPS } from '../../lib/labels.ts'
import { Button, Card, ErrorMessage, Field, Select } from '../../components/ui.tsx'
import { ACCOUNTING_AREAS } from './tabs.ts'

function EnvelopeRow({ category }: { category: Schemas['Category'] }) {
  const [budget, setBudget] = useState(centsToInput(category.monthlyBudgetCents))
  const [group, setGroup] = useState<Schemas['BudgetGroup'] | ''>(category.group ?? 'NEEDS')
  const changed = budget !== centsToInput(category.monthlyBudgetCents) || group !== category.group
  const save = useAction(() => {
    const cents = parseMoney(budget)
    if (Number.isNaN(cents)) throw new Error('Montant invalide.')
    return unwrap(api().PUT('/api/accounting/categories/{code}', { params: { path: { code: category.code } },
      body: { name: category.name, group: group || null, monthlyBudgetCents: cents } }))
  }, { refresh: [...ACCOUNTING_AREAS], inline: true, success: `Enveloppe « ${category.name} » enregistrée` })
  return (
    <li className="py-2">
      <form onSubmit={(e) => { e.preventDefault(); save.mutate(undefined) }} className="flex flex-wrap items-center gap-2">
        <span className="flex-1 text-sm">{category.name}</span>
        <div className="w-32"><Select options={BUDGET_GROUPS} value={group} onChange={setGroup} aria-label={`Groupe de ${category.name}`} /></div>
        <input className="input w-28 text-right" inputMode="decimal" placeholder="—" value={budget} onChange={(e) => setBudget(e.target.value)}
          aria-label={`Budget mensuel ${category.name}`} />
        <Button type="submit" variant={changed ? 'primary' : 'ghost'} disabled={!changed || save.isPending}>OK</Button>
      </form>
      <ErrorMessage error={save.error} />
    </li>
  )
}

function NewCategory() {
  const [form, setForm] = useState({ name: '', direction: 'EXPENSE' as Schemas['Direction'], group: 'NEEDS' as Schemas['BudgetGroup'], budget: '' })
  const create = useAction(() => {
    const cents = parseMoney(form.budget)
    if (Number.isNaN(cents)) throw new Error('Montant invalide.')
    return unwrap(api().POST('/api/accounting/categories', { body: { name: form.name, direction: form.direction,
      group: form.direction === 'EXPENSE' ? form.group : null, monthlyBudgetCents: cents } }))
  }, { refresh: [...ACCOUNTING_AREAS], inline: true, success: 'Catégorie ajoutée', onSuccess: () => setForm({ ...form, name: '', budget: '' }) })
  return (
    <form onSubmit={(e) => { e.preventDefault(); create.mutate(undefined) }} className="space-y-3">
      <Field label="Nom"><input className="input" required maxLength={60} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
      <Field label="Sens"><Select options={{ EXPENSE: 'Dépense', INCOME: 'Revenu' }} value={form.direction}
        onChange={(v) => setForm({ ...form, direction: v || 'EXPENSE' })} /></Field>
      {form.direction === 'EXPENSE' && (
        <>
          <Field label="Groupe"><Select options={BUDGET_GROUPS} value={form.group} onChange={(v) => setForm({ ...form, group: v || 'NEEDS' })} /></Field>
          <Field label="Budget mensuel (€)"><input className="input" inputMode="decimal" value={form.budget} onChange={(e) => setForm({ ...form, budget: e.target.value })} /></Field>
        </>
      )}
      <ErrorMessage error={create.error} />
      <Button type="submit" variant="primary" disabled={create.isPending}>Ajouter</Button>
    </form>
  )
}

export function EnvelopesTab({ categories }: { categories: Schemas['Category'][] }) {
  const expenses = categories.filter((c) => c.direction === 'EXPENSE')
  const total = expenses.reduce((sum, c) => sum + (c.monthlyBudgetCents ?? 0), 0)
  return (
    <div className="grid gap-4 lg:grid-cols-3">
      <Card title={`Enveloppes mensuelles · ${formatMoney(total)}`} className="lg:col-span-2">
        <p className="mb-2 text-sm text-ink-3">Une dizaine d'enveloppes suffit. Les besoins visent 50 % des revenus, les envies 30 %, l'épargne 20 %.</p>
        <ul className="divide-y divide-line">
          {expenses.map((c) => <EnvelopeRow key={`${c.code}-${c.monthlyBudgetCents}-${c.group}`} category={c} />)}
        </ul>
      </Card>
      <Card title="Nouvelle catégorie"><NewCategory /></Card>
    </div>
  )
}
