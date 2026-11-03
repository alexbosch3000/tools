// Create or edit a recipe and its ingredients, picked among the catalog's ingredients.
import { useState } from 'react'
import { post, put } from '../../lib/api.js'
import { findItem } from '../../lib/items.js'
import { UNITS } from '../../lib/labels.js'
import { useResource } from '../../lib/useResource.js'
import { useSubmit } from '../../lib/useSubmit.js'
import ItemPicker from '../../components/ItemPicker.jsx'
import { Button, ErrorMessage, Field, Modal, Select } from '../../components/ui.jsx'

const toForm = (recipe) => ({
  name: recipe?.name ?? '', servings: String(recipe?.servings ?? 4),
  prepMinutes: recipe?.prepMinutes ?? '', cookMinutes: recipe?.cookMinutes ?? '',
  tags: (recipe?.tags ?? []).join(', '), sourceUrl: recipe?.sourceUrl ?? '', instructions: recipe?.instructions ?? '',
  ingredients: (recipe?.ingredients ?? []).map((i) => ({ itemId: i.itemId, itemName: i.itemName,
    quantity: String(i.quantity.amount), unit: i.quantity.unit, note: i.note ?? '' })),
})

const optionalNumber = (value) => (value === '' || value === null ? null : Number(value))

export default function RecipeEditor({ recipe, onClose, onSaved }) {
  const [form, setForm] = useState(() => toForm(recipe))
  const ingredients = useResource('/api/items?kind=INGREDIENT')
  const [search, setSearch] = useState('')
  const picked = findItem(ingredients.data, search)
  const { busy, error, run } = useSubmit()
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const setIngredient = (index, patch) => set({ ingredients: form.ingredients.map((i, n) => (n === index ? { ...i, ...patch } : i)) })

  const save = (e) => {
    e.preventDefault()
    run(async () => {
      const body = {
        name: form.name, servings: Number(form.servings), prepMinutes: optionalNumber(form.prepMinutes),
        cookMinutes: optionalNumber(form.cookMinutes), instructions: form.instructions, sourceUrl: form.sourceUrl || null,
        tags: form.tags.split(',').map((t) => t.trim()).filter(Boolean),
        ingredients: form.ingredients.map((i) => ({ itemId: i.itemId, quantity: Number(String(i.quantity).replace(',', '.')),
          unit: i.unit, note: i.note || null })),
      }
      onSaved(recipe ? await put(`/api/recipes/${recipe.id}`, body) : await post('/api/recipes', body))
    })
  }

  return (
    <Modal wide title={recipe ? 'Modifier la recette' : 'Nouvelle recette'} onClose={onClose}
      footer={<Button variant="primary" type="submit" form="recipe-form" disabled={busy}>Enregistrer</Button>}>
      <form id="recipe-form" onSubmit={save} className="space-y-4">
        <Field label="Nom"><input className="input" required maxLength={120} value={form.name} onChange={(e) => set({ name: e.target.value })} /></Field>
        <div className="grid grid-cols-3 gap-3">
          <Field label="Portions"><input className="input" type="number" min="1" max="50" required value={form.servings}
            onChange={(e) => set({ servings: e.target.value })} /></Field>
          <Field label="Préparation (min)"><input className="input" type="number" min="0" value={form.prepMinutes}
            onChange={(e) => set({ prepMinutes: e.target.value })} /></Field>
          <Field label="Cuisson (min)"><input className="input" type="number" min="0" value={form.cookMinutes}
            onChange={(e) => set({ cookMinutes: e.target.value })} /></Field>
        </div>
        <Field label="Ingrédients">
          <div className="space-y-2">
            {form.ingredients.map((ingredient, index) => (
              <div key={`${ingredient.itemId}-${index}`} className="flex items-center gap-2">
                <span className="w-36 truncate text-sm text-slate-800">{ingredient.itemName}</span>
                <input className="input w-20" inputMode="decimal" value={ingredient.quantity} aria-label="Quantité"
                  onChange={(e) => setIngredient(index, { quantity: e.target.value })} />
                <Select options={UNITS} value={ingredient.unit} aria-label="Unité" onChange={(v) => setIngredient(index, { unit: v ?? 'G' })} />
                <input className="input" placeholder="précision" value={ingredient.note}
                  onChange={(e) => setIngredient(index, { note: e.target.value })} />
                <button type="button" className="text-slate-400 hover:text-red-600" aria-label={`Retirer ${ingredient.itemName}`}
                  onClick={() => set({ ingredients: form.ingredients.filter((_, n) => n !== index) })}>✕</button>
              </div>
            ))}
            <div className="flex gap-2">
              <ItemPicker items={ingredients.data} value={search} onChange={setSearch} placeholder="Ajouter un ingrédient…" />
              <Button disabled={!picked} onClick={() => {
                set({ ingredients: [...form.ingredients, { itemId: picked.id, itemName: picked.name, quantity: '1', unit: picked.unit, note: '' }] })
                setSearch('')
              }}>Ajouter</Button>
            </div>
          </div>
        </Field>
        <Field label="Étapes"><textarea className="input" rows="6" value={form.instructions}
          onChange={(e) => set({ instructions: e.target.value })} /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Étiquettes" hint="Séparées par des virgules"><input className="input" value={form.tags}
            onChange={(e) => set({ tags: e.target.value })} /></Field>
          <Field label="Source (lien)"><input className="input" type="url" value={form.sourceUrl}
            onChange={(e) => set({ sourceUrl: e.target.value })} /></Field>
        </div>
        <ErrorMessage error={error} />
      </form>
    </Modal>
  )
}
