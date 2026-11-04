// A stored line of a shopping list; lines of the same item for the same member are shown as one row.
package io.github.alexbosch3000.familyhub.shopping.internal;

import io.github.alexbosch3000.familyhub.catalog.Quantity;
import org.jspecify.annotations.Nullable;

/**
 * @param recipeEntryId the recipe entry that induced the line, null for a line added by hand
 * @param memberId      the member the item is for (clothes, shoes), null for the household
 */
public record ShoppingLine(long id, long itemId, @Nullable Long recipeEntryId, @Nullable Long memberId,
        Quantity quantity, boolean checked, @Nullable String note) {
}
