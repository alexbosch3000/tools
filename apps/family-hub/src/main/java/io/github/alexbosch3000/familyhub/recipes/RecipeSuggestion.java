// A recipe that can be cooked, fully or partly, with a given set of items.
package io.github.alexbosch3000.familyhub.recipes;

import java.util.List;

/**
 * @param coverage share of the recipe's ingredients present, from 0 to 1
 * @param missing  names of the ingredients still to buy
 */
public record RecipeSuggestion(long recipeId, String name, int servings, double coverage, List<String> missing) {
}
