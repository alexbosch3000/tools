// Ranking of recipes by the share of their ingredients available.
package io.github.alexbosch3000.familyhub.recipes.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.catalog.Unit;
import io.github.alexbosch3000.familyhub.recipes.Recipe;
import io.github.alexbosch3000.familyhub.recipes.RecipeIngredient;
import io.github.alexbosch3000.familyhub.recipes.RecipeSuggestion;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecipeMatcherTest {

    private static Recipe recipe(long id, String name, long... itemIds) {
        List<RecipeIngredient> ingredients = Arrays.stream(itemIds)
                .mapToObj(i -> new RecipeIngredient(i, "item" + i, new Quantity(1, Unit.PIECE), null)).toList();
        return new Recipe(id, name, 4, null, null, "", List.of(), null, ingredients);
    }

    @Test
    void ranksByCoverageThenByMissingCountAndListsWhatIsMissing() {
        List<Recipe> recipes = List.of(
                recipe(1, "Half", 1, 2),
                recipe(2, "Full", 1, 3),
                recipe(3, "None", 9),
                recipe(4, "Third", 1, 7, 8),
                recipe(5, "Empty"));

        List<RecipeSuggestion> ranked = RecipeMatcher.rank(recipes, Set.of(1L, 3L), 10);

        assertThat(ranked).extracting(RecipeSuggestion::name).containsExactly("Full", "Half", "Third");
        assertThat(ranked.get(1).coverage()).isEqualTo(0.5);
        assertThat(ranked.get(2).missing()).containsExactly("item7", "item8");
    }

    @Test
    void countsAnIngredientListedTwiceOnce() {
        RecipeSuggestion suggestion = RecipeMatcher.rank(List.of(recipe(1, "Twice", 1, 1, 2)), Set.of(1L), 10)
                .getFirst();
        assertThat(suggestion.coverage()).isEqualTo(0.5);
    }

    @Test
    void appliesTheLimit() {
        assertThat(RecipeMatcher.rank(List.of(recipe(1, "A", 1), recipe(2, "B", 1)), Set.of(1L), 1)).hasSize(1);
    }
}
