// One item of a shopping list, with the quantities requested manually and by recipes added up.
package io.github.alexbosch3000.familyhub.shopping;

import io.github.alexbosch3000.familyhub.catalog.Aisle;
import io.github.alexbosch3000.familyhub.catalog.ItemKind;
import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.catalog.StoreType;
import java.util.List;

/**
 * @param quantities     one quantity per dimension (e.g. "2 pieces" and "300 g" of onions)
 * @param manual         whether the item was also added by hand
 * @param recipes        names of the recipes requiring the item
 * @param estimatedCents estimated cost, null when a quantity cannot be priced
 */
public record ShoppingRow(long itemId, String name, ItemKind kind, StoreType storeType, Aisle aisle,
        List<Quantity> quantities, boolean manual, List<String> recipes, List<String> notes, boolean checked,
        Long estimatedCents) {
}
