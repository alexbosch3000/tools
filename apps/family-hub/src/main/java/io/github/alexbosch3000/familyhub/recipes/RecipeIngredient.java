// One ingredient line of a recipe: a catalog item and its quantity.
package io.github.alexbosch3000.familyhub.recipes;

import io.github.alexbosch3000.familyhub.catalog.Quantity;
import org.jspecify.annotations.Nullable;

public record RecipeIngredient(long itemId, String itemName, Quantity quantity, @Nullable String note) {
}
