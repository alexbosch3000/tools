// Ranks recipes by how much of their ingredients a set of available items covers.
package io.github.alexbosch3000.familyhub.recipes.internal;

import io.github.alexbosch3000.familyhub.recipes.Recipe;
import io.github.alexbosch3000.familyhub.recipes.RecipeIngredient;
import io.github.alexbosch3000.familyhub.recipes.RecipeSuggestion;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RecipeMatcher {

    private RecipeMatcher() {
    }

    /**
     * Keeps recipes sharing at least one item with {@code available}, ordered by coverage, then by
     * fewest missing ingredients, then by name. An item counts once even if listed twice in a recipe.
     */
    public static List<RecipeSuggestion> rank(List<Recipe> recipes, Set<Long> available, int limit) {
        return recipes.stream()
                .filter(r -> !r.ingredients().isEmpty())
                .map(r -> suggestion(r, available))
                .filter(s -> s.coverage() > 0)
                .sorted(Comparator.comparingDouble(RecipeSuggestion::coverage).reversed()
                        .thenComparingInt(s -> s.missing().size())
                        .thenComparing(RecipeSuggestion::name))
                .limit(limit)
                .toList();
    }

    private static RecipeSuggestion suggestion(Recipe recipe, Set<Long> available) {
        Map<Long, String> distinct = new LinkedHashMap<>();
        for (RecipeIngredient ingredient : recipe.ingredients()) {
            distinct.putIfAbsent(ingredient.itemId(), ingredient.itemName());
        }
        long present = distinct.keySet().stream().filter(available::contains).count();
        List<String> missing = distinct.entrySet().stream()
                .filter(e -> !available.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        return new RecipeSuggestion(recipe.id(), recipe.name(), recipe.servings(),
                (double) present / distinct.size(), missing);
    }
}
