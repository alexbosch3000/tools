// A recipe added to a shopping list for a number of servings; its ingredients are lines of the list.
package io.github.alexbosch3000.familyhub.shopping;

public record ListedRecipe(long entryId, long recipeId, String name, int servings) {
}
