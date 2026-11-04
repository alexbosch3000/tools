// Public API of the recipes module: recipes, scaled ingredients and suggestions from available items.
package io.github.alexbosch3000.familyhub.recipes;

import io.github.alexbosch3000.familyhub.catalog.CatalogService;
import io.github.alexbosch3000.familyhub.catalog.Item;
import io.github.alexbosch3000.familyhub.catalog.ItemKind;
import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.recipes.internal.RecipeMatcher;
import io.github.alexbosch3000.familyhub.recipes.internal.RecipeRepository;
import io.github.alexbosch3000.familyhub.shared.InvalidRequestException;
import io.github.alexbosch3000.familyhub.shared.NotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeService {

    private static final int MAX_SUGGESTIONS = 10;

    private final RecipeRepository repository;
    private final CatalogService catalog;
    private final Clock clock;

    RecipeService(RecipeRepository repository, CatalogService catalog, Clock clock) {
        this.repository = repository;
        this.catalog = catalog;
        this.clock = clock;
    }

    public record IngredientData(long itemId, Quantity quantity, @Nullable String note) {
    }

    public record RecipeData(String name, int servings, @Nullable Integer prepMinutes, @Nullable Integer cookMinutes,
            @Nullable String instructions, @Nullable List<String> tags, @Nullable String sourceUrl,
            List<IngredientData> ingredients) {
    }

    public List<RecipeSummary> list(@Nullable String query) {
        return repository.findSummaries(query);
    }

    public Recipe get(long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Recette introuvable."));
    }

    /** Ingredients of a recipe scaled to a number of servings. */
    public List<RecipeIngredient> ingredientsFor(long recipeId, int servings) {
        return get(recipeId).scaledTo(servings).ingredients();
    }

    /** Recipes using at least one of the items, best covered first. */
    public List<RecipeSuggestion> suggestionsFor(Set<Long> itemIds) {
        return RecipeMatcher.rank(repository.findAll(), itemIds, MAX_SUGGESTIONS);
    }

    @Transactional
    public Recipe create(RecipeData data) {
        validateIngredients(data.ingredients());
        long id = repository.insert(data, LocalDateTime.now(clock));
        repository.replaceIngredients(id, data.ingredients());
        return get(id);
    }

    @Transactional
    public Recipe update(long id, RecipeData data) {
        get(id);
        validateIngredients(data.ingredients());
        repository.update(id, data);
        repository.replaceIngredients(id, data.ingredients());
        return get(id);
    }

    /** Fails with a conflict while a shopping list still includes the recipe. */
    @Transactional
    public void delete(long id) {
        get(id);
        repository.delete(id);
    }

    private void validateIngredients(List<IngredientData> ingredients) {
        Map<Long, Item> items = catalog.getAll(ingredients.stream().map(IngredientData::itemId).toList());
        for (IngredientData ingredient : ingredients) {
            Item item = items.get(ingredient.itemId());
            if (item == null) {
                throw new InvalidRequestException("Ingrédient introuvable : " + ingredient.itemId() + ".");
            }
            if (item.kind() != ItemKind.INGREDIENT) {
                throw new InvalidRequestException("« " + item.name() + " » n'est pas un ingrédient.");
            }
        }
    }
}
