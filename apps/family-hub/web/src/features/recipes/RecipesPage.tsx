// Recipe book: search by name or tag, and create recipes.
import { useDeferredValue, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from '@tanstack/react-router'
import { api, unwrap } from '../../api/client.ts'
import { Badge, Button, Empty, ErrorMessage, Loading, PageHeader } from '../../components/ui.tsx'
import { RecipeEditor } from './RecipeEditor.tsx'

export function RecipesPage() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const q = useDeferredValue(query.trim())
  const [creating, setCreating] = useState(false)
  const recipes = useQuery({ queryKey: ['recipes', q], placeholderData: (previous) => previous,
    queryFn: () => unwrap(api().GET('/api/recipes', { params: { query: q ? { q } : {} } })) })
  return (
    <>
      <PageHeader title="Recettes" subtitle="Les ingrédients viennent du catalogue : une recette s'ajoute en un clic à une liste de courses."
        actions={<Button variant="primary" onClick={() => setCreating(true)}>Nouvelle recette</Button>} />
      <input type="search" className="input mb-4 max-w-sm" placeholder="Rechercher (nom, étiquette)…" aria-label="Rechercher une recette"
        value={query} onChange={(e) => setQuery(e.target.value)} />
      {recipes.isPending && <Loading />}
      <ErrorMessage error={recipes.error} />
      {recipes.data?.length === 0 && <Empty>Aucune recette.</Empty>}
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {recipes.data?.map((r) => (
          <Link key={r.id} to="/recettes/$recipeId" params={{ recipeId: String(r.id) }} search={{ servings: undefined }}
            className="card block hover:border-accent">
            <h2 className="font-semibold">{r.name}</h2>
            <p className="mt-1 text-sm text-ink-3">
              {r.servings} portions · {r.ingredientCount} ingrédients
              {(r.prepMinutes || r.cookMinutes) ? ` · ${(r.prepMinutes ?? 0) + (r.cookMinutes ?? 0)} min` : ''}
            </p>
            <div className="mt-2 flex flex-wrap gap-1">{r.tags.map((t) => <Badge key={t}>{t}</Badge>)}</div>
          </Link>
        ))}
      </div>
      {creating && <RecipeEditor onClose={() => setCreating(false)}
        onSaved={(r) => navigate({ to: '/recettes/$recipeId', params: { recipeId: String(r.id) }, search: { servings: undefined } })} />}
    </>
  )
}
