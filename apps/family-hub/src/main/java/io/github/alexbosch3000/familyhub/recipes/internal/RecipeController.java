// Recipes API: CRUD, scaling to a number of servings and suggestions from a set of items.
package io.github.alexbosch3000.familyhub.recipes.internal;

import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.catalog.Unit;
import io.github.alexbosch3000.familyhub.recipes.Recipe;
import io.github.alexbosch3000.familyhub.recipes.RecipeService;
import io.github.alexbosch3000.familyhub.recipes.RecipeService.IngredientData;
import io.github.alexbosch3000.familyhub.recipes.RecipeService.RecipeData;
import io.github.alexbosch3000.familyhub.recipes.RecipeSuggestion;
import io.github.alexbosch3000.familyhub.recipes.RecipeSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
class RecipeController {

    private final RecipeService recipes;

    RecipeController(RecipeService recipes) {
        this.recipes = recipes;
    }

    record IngredientRequest(@NotNull Long itemId, @Positive double quantity, @NotNull Unit unit,
            @Nullable @Size(max = 120) String note) {
    }

    record RecipeRequest(
            @NotBlank @Size(max = 120) String name,
            @Min(1) @Max(50) int servings,
            @Nullable @PositiveOrZero Integer prepMinutes,
            @Nullable @PositiveOrZero Integer cookMinutes,
            @Nullable @Size(max = 20_000) String instructions,
            @Nullable @Size(max = 20) List<@Size(max = 30) String> tags,
            @Nullable @Size(max = 500) String sourceUrl,
            @NotNull @Size(max = 100) List<@Valid IngredientRequest> ingredients) {

        RecipeData toData() {
            return new RecipeData(name, servings, prepMinutes, cookMinutes, instructions, tags, sourceUrl,
                    ingredients.stream()
                            .map(i -> new IngredientData(i.itemId(), new Quantity(i.quantity(), i.unit()), i.note()))
                            .toList());
        }
    }

    @GetMapping
    List<RecipeSummary> list(@RequestParam(required = false) @Nullable String q) {
        return recipes.list(q);
    }

    /** With {@code servings}, quantities are scaled to that number of people. */
    @GetMapping("/{id}")
    Recipe get(@PathVariable long id, @RequestParam(required = false) @Nullable @Min(1) @Max(50) Integer servings) {
        Recipe recipe = recipes.get(id);
        return servings == null ? recipe : recipe.scaledTo(servings);
    }

    @GetMapping("/suggestions")
    List<RecipeSuggestion> suggestions(@RequestParam List<Long> itemIds) {
        return recipes.suggestionsFor(new HashSet<>(itemIds));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Recipe create(@Valid @RequestBody RecipeRequest request) {
        return recipes.create(request.toData());
    }

    @PutMapping("/{id}")
    Recipe update(@PathVariable long id, @Valid @RequestBody RecipeRequest request) {
        return recipes.update(id, request.toData());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id) {
        recipes.delete(id);
    }
}
