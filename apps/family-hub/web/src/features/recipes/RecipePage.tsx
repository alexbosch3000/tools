// One recipe: scaled ingredients, steps, and the links to shopping lists and to the meal planning.
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate, useParams, useSearch } from '@tanstack/react-router'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useFeedback } from '../../app/useFeedback.ts'
import { useAction } from '../../app/useAction.ts'
import { addMinutes, combine, today } from '../../lib/dates.ts'
import { formatQuantity } from '../../lib/format.ts'
import { Dialog } from '../../components/Dialog.tsx'
import { Badge, Button, Card, ErrorMessage, Field, Loading, PageHeader, Select } from '../../components/ui.tsx'
import { RecipeEditor } from './RecipeEditor.tsx'

function ToShoppingDialog({ recipe, servings, onClose }: { recipe: Schemas['Recipe'], servings: number, onClose: () => void }) {
  const navigate = useNavigate()
  const lists = useQuery({ queryKey: ['shopping', 'lists', 'OPEN'],
    queryFn: () => unwrap(api().GET('/api/shopping/lists', { params: { query: { status: 'OPEN' } } })) })
  const [listId, setListId] = useState('new')
  const add = useAction(async () => {
    const id = listId === 'new'
      ? (await unwrap(api().POST('/api/shopping/lists', { body: { name: `Courses ${recipe.name}` } }))).id : Number(listId)
    await unwrap(api().POST('/api/shopping/lists/{id}/recipes', { params: { path: { id } }, body: { recipeId: recipe.id, servings } }))
    return id
  }, { refresh: ['shopping', 'dashboard'], inline: true, success: 'Ingrédients ajoutés à la liste',
    onSuccess: (id) => navigate({ to: '/courses/$listId', params: { listId: String(id) } }) })
  return (
    <Dialog title="Ajouter aux courses" onClose={onClose}
      footer={<Button variant="primary" type="submit" form="to-shopping" disabled={add.isPending}>Ajouter</Button>}>
      <form id="to-shopping" onSubmit={(e) => { e.preventDefault(); add.mutate(undefined) }} className="space-y-4">
        <p className="text-sm text-ink-2">Les ingrédients pour {servings} portions sont ajoutés à la liste.</p>
        <Field label="Liste">
          <Select options={{ new: '+ Nouvelle liste', ...Object.fromEntries((lists.data ?? []).map((l) => [String(l.id), l.name])) }}
            value={listId} onChange={(v) => setListId(v || 'new')} />
        </Field>
        <ErrorMessage error={add.error} />
      </form>
    </Dialog>
  )
}

function PlanMealDialog({ recipe, servings, onClose }: { recipe: Schemas['Recipe'], servings: number, onClose: () => void }) {
  const navigate = useNavigate()
  const [day, setDay] = useState(today())
  const [time, setTime] = useState('19:30')
  const plan = useAction(() => {
    const start = combine(day, time)
    return unwrap(api().POST('/api/planning/entries', { body: { title: recipe.name, type: 'MEAL', start, end: addMinutes(start, 60),
      allDay: false, createShoppingList: false, recipeId: recipe.id, servings } }))
  }, { refresh: ['planning', 'dashboard'], inline: true, success: 'Repas planifié', onSuccess: () => navigate({ to: '/planning' }) })
  return (
    <Dialog title="Planifier ce repas" onClose={onClose}
      footer={<Button variant="primary" type="submit" form="plan-meal" disabled={plan.isPending}>Planifier</Button>}>
      <form id="plan-meal" onSubmit={(e) => { e.preventDefault(); plan.mutate(undefined) }} className="grid grid-cols-2 gap-3">
        <Field label="Jour"><input className="input" type="date" required value={day} onChange={(e) => setDay(e.target.value)} /></Field>
        <Field label="Heure"><input className="input" type="time" required value={time} onChange={(e) => setTime(e.target.value)} /></Field>
        <div className="col-span-2"><ErrorMessage error={plan.error} /></div>
      </form>
    </Dialog>
  )
}

export function RecipePage() {
  const { recipeId } = useParams({ from: '/recettes/$recipeId' })
  const search = useSearch({ from: '/recettes/$recipeId' })
  const id = Number(recipeId)
  const navigate = useNavigate()
  const { confirm } = useFeedback()
  const base = useQuery({ queryKey: ['recipes', 'recipe', id], queryFn: () => unwrap(api().GET('/api/recipes/{id}', { params: { path: { id } } })) })
  const [servings, setServings] = useState<number | null>(search.servings ?? null)
  const people = servings ?? base.data?.servings ?? 4
  const scaled = useQuery({ queryKey: ['recipes', 'recipe', id, people], enabled: base.isSuccess, placeholderData: (previous) => previous,
    queryFn: () => unwrap(api().GET('/api/recipes/{id}', { params: { path: { id }, query: { servings: people } } })) })
  const [modal, setModal] = useState<'edit' | 'shopping' | 'plan' | null>(null)
  const remove = useAction(() => unwrap(api().DELETE('/api/recipes/{id}', { params: { path: { id } } })),
    { refresh: ['recipes'], success: 'Recette supprimée', onSuccess: () => navigate({ to: '/recettes' }) })

  if (base.isPending) return <Loading lines={6} />
  if (base.isError) return <ErrorMessage error={base.error} />
  const recipe = scaled.data ?? base.data

  return (
    <>
      <PageHeader title={recipe.name}
        subtitle={[recipe.prepMinutes && `Préparation ${recipe.prepMinutes} min`, recipe.cookMinutes && `cuisson ${recipe.cookMinutes} min`].filter(Boolean).join(' · ')}
        actions={(
          <>
            <Button onClick={() => setModal('edit')}>Modifier</Button>
            <Button onClick={() => setModal('plan')}>Planifier le repas</Button>
            <Button variant="primary" onClick={() => setModal('shopping')}>Ajouter aux courses</Button>
          </>
        )} />
      <div className="grid gap-4 lg:grid-cols-3">
        <Card title="Ingrédients">
          <div className="mb-3 flex items-center gap-2 text-sm">
            <Button variant="ghost" onClick={() => setServings(Math.max(1, people - 1))} aria-label="Moins de portions">−</Button>
            <span className="font-medium" aria-live="polite">{people} portions</span>
            <Button variant="ghost" onClick={() => setServings(Math.min(50, people + 1))} aria-label="Plus de portions">+</Button>
          </div>
          <ul className="space-y-1 text-sm">
            {recipe.ingredients.map((i, n) => (
              <li key={`${i.itemId}-${n}`} className="flex justify-between gap-2">
                <span>{i.itemName}{i.note && <span className="text-ink-3"> ({i.note})</span>}</span>
                <span className="tabular-nums text-ink-2">{formatQuantity(i.quantity)}</span>
              </li>
            ))}
          </ul>
        </Card>
        <Card title="Étapes" className="lg:col-span-2">
          <p className="whitespace-pre-line text-sm leading-relaxed text-ink-2">{recipe.instructions || 'Pas encore d’étapes.'}</p>
          <div className="mt-4 flex flex-wrap items-center gap-2">
            {recipe.tags.map((t) => <Badge key={t}>{t}</Badge>)}
            {recipe.sourceUrl && <a href={recipe.sourceUrl} target="_blank" rel="noreferrer noopener" className="text-sm text-accent">Source</a>}
            <Button variant="danger" className="ml-auto" onClick={async () => {
              if (await confirm({ title: 'Supprimer la recette ?', message: `« ${recipe.name} » sera supprimée.` })) remove.mutate(undefined)
            }}>Supprimer</Button>
          </div>
        </Card>
      </div>
      {modal === 'edit' && <RecipeEditor recipe={base.data} onClose={() => setModal(null)} onSaved={() => setModal(null)} />}
      {modal === 'shopping' && <ToShoppingDialog recipe={recipe} servings={people} onClose={() => setModal(null)} />}
      {modal === 'plan' && <PlanMealDialog recipe={recipe} servings={people} onClose={() => setModal(null)} />}
    </>
  )
}
