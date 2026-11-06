// One item of a shopping list for one member (or the household), with its quantities added up.
package io.github.alexbosch3000.familyhub.shopping;

import io.github.alexbosch3000.familyhub.catalog.Aisle;
import io.github.alexbosch3000.familyhub.catalog.ItemKind;
import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.catalog.StoreType;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @param memberId       the member the item is for, null for the household
 * @param quantities     what is needed: one quantity per dimension (e.g. "2 pieces" and "300 g" of onions)
 * @param purchase       what will be bought when it differs from what is needed, null otherwise
 * @param manual         whether the item was also added by hand
 * @param recipes        names of the recipes requiring the item
 * @param estimatedCents estimated cost, from the purchase when there is one; null when it cannot be priced
 */
public record ShoppingRow(long itemId, @Nullable Long memberId, String name, ItemKind kind, StoreType storeType,
        Aisle aisle, List<Quantity> quantities, @Nullable Purchase purchase,
        boolean manual, List<String> recipes, List<String> notes,
        boolean checked, @Nullable Long estimatedCents) {
}
