// A stored line of a shopping list; several lines of the same item are shown as one row.
package io.github.alexbosch3000.familyhub.shopping.internal;

import io.github.alexbosch3000.familyhub.catalog.Quantity;

/** @param recipeEntryId the recipe entry that induced the line, null for a line added by hand */
public record ShoppingLine(long id, long itemId, Long recipeEntryId, Quantity quantity, boolean checked,
        String note) {
}
