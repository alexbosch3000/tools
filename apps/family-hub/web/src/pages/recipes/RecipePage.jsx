// One recipe: scaled ingredients, steps, and the links to shopping lists and to the meal planning.
import { useState } from 'react'
import { del, post } from '../../lib/api.js'
import { combine, today } from '../../lib/dates.js'
import { formatQuantity } from '../../lib/format.js'
import { navigate } from '../../lib/router.js'
import { useResource } from '../../lib/useResource.js'
import { useSubmit } from '../../lib/useSubmit.js'
import { Badge, Button, Card, ErrorMessage, Field, Loading, Modal, PageHeader, Select } from '../../components/ui.jsx'
import RecipeEditor from './RecipeEditor.jsx'

function ToShoppingModal({ recipe, servings, onClose }) {
  const lists = useResource('/api/shopping/lists')
  const [listId, setListId] = useState('new')
  const { busy, error, run } = useSubmit()
  const submit = (e) => {
    e.preventDefault()
    run(async () => {
      const id = listId === 'new' ? (await post('/api/shopping/lists', { name: `Courses ${recipe.name}` })).id : Number(listId)
      await post(`/api/shopping/lists/${id}/recipes`, { recipeId: recipe.id, servings })
      navigate(`/courses/${id}`)
    })
  }
  return (
    <Modal title="Ajouter aux courses" onClose={onClose}
      footer={<Button variant="primary" type="submit" form="to-shopping" disabled={busy}>Ajouter</Button>}>
      <form id="to-shopping" onSubmit={submit} className="space-y-4">
        <p className="text-sm text-slate-600">Les ingrédients pour {servings} portions sont ajoutés à la liste.</p>
        <Field label="Liste">
          <Select options={{ new: '+ Nouvelle liste', ...Object.fromEntries((lists.data ?? []).map((l) => [String(l.id), l.name])) }}
            value={listId} onChange={(v) => setListId(v ?? 'new')} />
        </Field>
        <ErrorMessage error={error} />
      </form>
    </Modal>
  )
}

function PlanMealModal({ recipe, servings, onClose }) {
  const [day, setDay] = useState(today())
  const [time, setTime] = useState('19:30')
  const { busy, error, run } = useSubmit()
  const submit = (e) => {
    e.preventDefault()
    run(async () => {
      const start = combine(day, time)
      const [h, m] = time.split(':').map(Number)
      const endTime = `${String(Math.min(23, h + 1)).padStart(2, '0')}:${String(m).padStart(2, '0')}`
      await post('/api/planning/entries', { title: recipe.name, type: 'MEAL', start, end: combine(day, endTime),
        recipeId: recipe.id, servings })
      navigate('/planning')
    })
  }
  return (
    <Modal title="Planifier ce repas" onClose={onClose}
      footer={<Button variant="primary" type="submit" form="plan-meal" disabled={busy}>Planifier</Button>}>
      <form id="plan-meal" onSubmit={submit} className="grid grid-cols-2 gap-3">
        <Field label="Jour"><input className="input" type="date" value={day} required onChange={(e) => setDay(e.target.value)} /></Field>
        <Field label="Heure"><input className="input" type="time" value={time} required onChange={(e) => setTime(e.target.value)} /></Field>
        <div className="col-span-2"><ErrorMessage error={error} /></div>
      </form>
    </Modal>
  )
}

export default function RecipePage({ params }) {
  const base = useResource(`/api/recipes/${params.id}`)
  const requested = Number(params.query.get('servings'))
  const [servings, setServings] = useState(requested > 0 ? requested : null)
  const people = servings ?? base.data?.servings ?? null
  const scaled = useResource(people ? `/api/recipes/${params.id}?servings=${people}` : null)
  const [modal, setModal] = useState(null)
  const action = useSubmit()

  if (base.loading) return <Loading />
  if (base.error) return <ErrorMessage error={base.error} />
  const recipe = scaled.data ?? base.data

  return (
    <>
      <PageHeader title={recipe.name}
        subtitle={[recipe.prepMinutes && `Préparation ${recipe.prepMinutes} min`, recipe.cookMinutes && `cuisson ${recipe.cookMinutes} min`]
          .filter(Boolean).join(' · ')}
        actions={(
          <>
            <Button onClick={() => setModal('edit')}>Modifier</Button>
            <Button onClick={() => setModal('plan')}>Planifier le repas</Button>
            <Button variant="primary" onClick={() => setModal('shopping')}>Ajouter aux courses</Button>
          </>
        )} />
      <ErrorMessage error={action.error} />
      <div className="grid gap-4 lg:grid-cols-3">
        <Card title="Ingrédients">
          <div className="mb-3 flex items-center gap-2 text-sm">
            <Button variant="ghost" onClick={() => setServings(Math.max(1, people - 1))} aria-label="Moins de portions">−</Button>
            <span className="font-medium">{people} portions</span>
            <Button variant="ghost" onClick={() => setServings(Math.min(50, people + 1))} aria-label="Plus de portions">+</Button>
          </div>
          <ul className="space-y-1 text-sm">
            {recipe.ingredients.map((i, n) => (
              <li key={`${i.itemId}-${n}`} className="flex justify-between gap-2">
                <span className="text-slate-800">{i.itemName}{i.note ? <span className="text-slate-500"> ({i.note})</span> : null}</span>
                <span className="tabular-nums text-slate-600">{formatQuantity(i.quantity)}</span>
              </li>
            ))}
          </ul>
        </Card>
        <Card title="Étapes" className="lg:col-span-2">
          <p className="whitespace-pre-line text-sm leading-relaxed text-slate-700">{recipe.instructions || 'Pas encore d’étapes.'}</p>
          <div className="mt-4 flex flex-wrap items-center gap-2">
            {recipe.tags.map((t) => <Badge key={t}>{t}</Badge>)}
            {recipe.sourceUrl && <a href={recipe.sourceUrl} target="_blank" rel="noreferrer noopener" className="text-sm text-indigo-600">Source</a>}
            <Button variant="danger" className="ml-auto" onClick={() => action.run(async () => {
              await del(`/api/recipes/${recipe.id}`); navigate('/recettes')
            })}>Supprimer</Button>
          </div>
        </Card>
      </div>
      {modal === 'edit' && <RecipeEditor recipe={base.data} onClose={() => setModal(null)}
        onSaved={() => { setModal(null); base.reload(); scaled.reload() }} />}
      {modal === 'shopping' && <ToShoppingModal recipe={recipe} servings={people} onClose={() => setModal(null)} />}
      {modal === 'plan' && <PlanMealModal recipe={recipe} servings={people} onClose={() => setModal(null)} />}
    </>
  )
}
