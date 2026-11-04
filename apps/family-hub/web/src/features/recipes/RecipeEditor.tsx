// Create or edit a recipe and its ingredients, picked among the catalog's ingredients.
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, unwrap, type Schemas } from '../../api/client.ts'
import { useAction } from '../../app/useAction.ts'
import { findItem } from '../../lib/items.ts'
import { UNITS } from '../../lib/labels.ts'
import { Dialog } from '../../components/Dialog.tsx'
import { ItemPicker } from '../../components/ItemPicker.tsx'
import { Button, ErrorMessage, Field, Select } from '../../components/ui.tsx'

interface IngredientRow { itemId: number, itemName: string, quantity: string, unit: Schemas['Unit'], note: string }

const optionalNumber = (value: string) => (value === '' ? null : Number(value))

export function RecipeEditor({ recipe, onClose, onSaved }:
  { recipe?: Schemas['Recipe'], onClose: () => void, onSaved: (recipe: Schemas['Recipe']) => void }) {
  const [form, setForm] = useState({
    name: recipe?.name ?? '', servings: String(recipe?.servings ?? 4),
    prepMinutes: recipe?.prepMinutes?.toString() ?? '', cookMinutes: recipe?.cookMinutes?.toString() ?? '',
    tags: (recipe?.tags ?? []).join(', '), sourceUrl: recipe?.sourceUrl ?? '', instructions: recipe?.instructions ?? '',
  })
  const [ingredients, setIngredients] = useState<IngredientRow[]>(() => (recipe?.ingredients ?? []).map((i) => ({
    itemId: i.itemId, itemName: i.itemName, quantity: String(i.quantity.amount), unit: i.quantity.unit, note: i.note ?? '' })))
  const catalog = useQuery({ queryKey: ['catalog', 'items', 'INGREDIENT'],
    queryFn: () => unwrap(api().GET('/api/items', { params: { query: { kind: 'INGREDIENT' } } })) })
  const [search, setSearch] = useState('')
  const picked = findItem(catalog.data, search)
  const set = (patch: Partial<typeof form>) => setForm((f) => ({ ...f, ...patch }))
  const setIngredient = (index: number, patch: Partial<IngredientRow>) =>
    setIngredients((rows) => rows.map((row, n) => (n === index ? { ...row, ...patch } : row)))

  const save = useAction(() => {
    const body: Schemas['RecipeRequest'] = {
      name: form.name, servings: Number(form.servings), prepMinutes: optionalNumber(form.prepMinutes),
      cookMinutes: optionalNumber(form.cookMinutes), instructions: form.instructions, sourceUrl: form.sourceUrl || null,
      tags: form.tags.split(',').map((t) => t.trim()).filter(Boolean),
      ingredients: ingredients.map((i) => ({ itemId: i.itemId, quantity: Number(i.quantity.replace(',', '.')), unit: i.unit, note: i.note || null })),
    }
    return recipe
      ? unwrap(api().PUT('/api/recipes/{id}', { params: { path: { id: recipe.id } }, body }))
      : unwrap(api().POST('/api/recipes', { body }))
  }, { refresh: ['recipes', 'shopping'], inline: true, success: 'Recette enregistrée', onSuccess: onSaved })

  return (
    <Dialog wide title={recipe ? 'Modifier la recette' : 'Nouvelle recette'} onClose={onClose}
      footer={<Button variant="primary" type="submit" form="recipe-form" disabled={save.isPending}>Enregistrer</Button>}>
      <form id="recipe-form" onSubmit={(e) => { e.preventDefault(); save.mutate(undefined) }} className="space-y-4">
        <Field label="Nom"><input className="input" required maxLength={120} value={form.name} onChange={(e) => set({ name: e.target.value })} /></Field>
        <div className="grid grid-cols-3 gap-3">
          <Field label="Portions"><input className="input" type="number" min="1" max="50" required value={form.servings} onChange={(e) => set({ servings: e.target.value })} /></Field>
          <Field label="Préparation (min)"><input className="input" type="number" min="0" value={form.prepMinutes} onChange={(e) => set({ prepMinutes: e.target.value })} /></Field>
          <Field label="Cuisson (min)"><input className="input" type="number" min="0" value={form.cookMinutes} onChange={(e) => set({ cookMinutes: e.target.value })} /></Field>
        </div>
        <fieldset className="space-y-2">
          <legend className="mb-1 text-sm font-medium text-ink-2">Ingrédients</legend>
          {ingredients.map((ingredient, index) => (
            <div key={`${ingredient.itemId}-${index}`} className="flex flex-wrap items-center gap-2 sm:flex-nowrap">
              <span className="w-full truncate text-sm sm:w-36">{ingredient.itemName}</span>
              <input className="input w-20" inputMode="decimal" value={ingredient.quantity} aria-label={`Quantité de ${ingredient.itemName}`}
                onChange={(e) => setIngredient(index, { quantity: e.target.value })} />
              <div className="w-32"><Select options={UNITS} value={ingredient.unit} aria-label={`Unité de ${ingredient.itemName}`}
                onChange={(v) => setIngredient(index, { unit: v || 'G' })} /></div>
              <input className="input" placeholder="précision" value={ingredient.note} aria-label={`Précision pour ${ingredient.itemName}`}
                onChange={(e) => setIngredient(index, { note: e.target.value })} />
              <button type="button" aria-label={`Retirer ${ingredient.itemName}`} className="rounded p-1 text-ink-3 hover:text-danger"
                onClick={() => setIngredients((rows) => rows.filter((_, n) => n !== index))}>✕</button>
            </div>
          ))}
          <div className="flex gap-2">
            <ItemPicker items={catalog.data} value={search} onChange={setSearch} placeholder="Ajouter un ingrédient…" />
            <Button disabled={!picked} onClick={() => {
              if (!picked) return
              setIngredients((rows) => [...rows, { itemId: picked.id, itemName: picked.name, quantity: '1', unit: picked.unit, note: '' }])
              setSearch('')
            }}>Ajouter</Button>
          </div>
        </fieldset>
        <Field label="Étapes"><textarea className="input" rows={6} value={form.instructions} onChange={(e) => set({ instructions: e.target.value })} /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Étiquettes" hint="Séparées par des virgules"><input className="input" value={form.tags} onChange={(e) => set({ tags: e.target.value })} /></Field>
          <Field label="Source (lien)"><input className="input" type="url" value={form.sourceUrl} onChange={(e) => set({ sourceUrl: e.target.value })} /></Field>
        </div>
        <ErrorMessage error={save.error} />
      </form>
    </Dialog>
  )
}
