// Builds the rows of a shopping list from its stored lines: one row per item, sorted along the shops.
package io.github.alexbosch3000.familyhub.shopping.internal;

import io.github.alexbosch3000.familyhub.catalog.Item;
import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.catalog.StoreType;
import io.github.alexbosch3000.familyhub.shopping.ShoppingRow;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ShoppingRows {

    private ShoppingRows() {
    }

    /**
     * Groups lines by item. Quantities are added per dimension, a row is checked when all its lines
     * are, and rows are ordered by store type, aisle, then name.
     *
     * @param items       catalog items of the lines, by id
     * @param recipeNames recipe names by recipe entry id
     */
    public static List<ShoppingRow> build(List<ShoppingLine> lines, Map<Long, Item> items,
            Map<Long, String> recipeNames) {
        Map<Long, List<ShoppingLine>> byItem = lines.stream()
                .collect(Collectors.groupingBy(ShoppingLine::itemId, LinkedHashMap::new, Collectors.toList()));
        return byItem.entrySet().stream()
                .map(e -> row(items.get(e.getKey()), e.getValue(), recipeNames))
                .sorted(Comparator.comparing(ShoppingRow::storeType).thenComparing(ShoppingRow::aisle)
                        .thenComparing(ShoppingRow::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Store type of most rows; ties go to the first store type in declaration order. Null for no rows. */
    public static StoreType mainStoreType(List<ShoppingRow> rows) {
        Map<StoreType, Long> counts = new EnumMap<>(StoreType.class);
        rows.forEach(r -> counts.merge(r.storeType(), 1L, Long::sum));
        return counts.entrySet().stream()
                .max(Comparator.<Map.Entry<StoreType, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static ShoppingRow row(Item item, List<ShoppingLine> lines, Map<Long, String> recipeNames) {
        List<Quantity> quantities = Quantity.aggregate(lines.stream().map(ShoppingLine::quantity).toList());
        List<String> recipes = lines.stream().map(ShoppingLine::recipeEntryId).filter(Objects::nonNull)
                .map(recipeNames::get).filter(Objects::nonNull).distinct().toList();
        List<String> notes = lines.stream().map(ShoppingLine::note).filter(n -> n != null && !n.isBlank())
                .distinct().toList();
        boolean manual = lines.stream().anyMatch(l -> l.recipeEntryId() == null);
        boolean checked = lines.stream().allMatch(ShoppingLine::checked);
        return new ShoppingRow(item.id(), item.name(), item.kind(), item.storeType(), item.aisle(), quantities,
                manual, recipes, notes, checked, estimate(item, quantities));
    }

    private static Long estimate(Item item, List<Quantity> quantities) {
        long total = 0;
        for (Quantity quantity : quantities) {
            Long cents = item.estimateCents(quantity);
            if (cents == null) {
                return null;
            }
            total += cents;
        }
        return total;
    }
}
