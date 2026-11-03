// Recipe book: search by name or tag, and create recipes.
import { useState } from 'react'
import { navigate } from '../../lib/router.js'
import { useResource } from '../../lib/useResource.js'
import { Badge, Button, Empty, ErrorMessage, Loading, PageHeader } from '../../components/ui.jsx'
import RecipeEditor from './RecipeEditor.jsx'

export default function RecipesPage() {
  const [query, setQuery] = useState('')
  const [creating, setCreating] = useState(false)
  const recipes = useResource(`/api/recipes${query.trim() ? `?q=${encodeURIComponent(query.trim())}` : ''}`)

  return (
    <>
      <PageHeader title="Recettes" subtitle="Les ingrédients viennent du catalogue : une recette s'ajoute en un clic à une liste de courses."
        actions={<Button variant="primary" onClick={() => setCreating(true)}>Nouvelle recette</Button>} />
      <input className="input mb-4 max-w-sm" placeholder="Rechercher (nom, étiquette)…" value={query} onChange={(e) => setQuery(e.target.value)} />
      {recipes.loading && !recipes.data && <Loading />}
      <ErrorMessage error={recipes.error} />
      {recipes.data?.length === 0 && <Empty>Aucune recette.</Empty>}
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {recipes.data?.map((r) => (
          <a key={r.id} href={`#/recettes/${r.id}`} className="card block hover:border-indigo-300">
            <h2 className="font-semibold text-slate-800">{r.name}</h2>
            <p className="mt-1 text-sm text-slate-500">
              {r.servings} portions · {r.ingredientCount} ingrédients
              {(r.prepMinutes || r.cookMinutes) ? ` · ${(r.prepMinutes ?? 0) + (r.cookMinutes ?? 0)} min` : ''}
            </p>
            <div className="mt-2 flex flex-wrap gap-1">{r.tags.map((t) => <Badge key={t}>{t}</Badge>)}</div>
          </a>
        ))}
      </div>
      {creating && <RecipeEditor onClose={() => setCreating(false)} onSaved={(r) => navigate(`/recettes/${r.id}`)} />}
    </>
  )
}
