// One ingredient line of a recipe: a catalog item and its quantity.
package io.github.alexbosch3000.familyhub.recipes;

import io.github.alexbosch3000.familyhub.catalog.Quantity;

public record RecipeIngredient(long itemId, String itemName, Quantity quantity, String note) {
}
