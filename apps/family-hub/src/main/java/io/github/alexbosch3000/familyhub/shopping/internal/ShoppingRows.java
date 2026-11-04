// Builds the rows of a shopping list from its stored lines: one row per item and member, sorted along the shops.
package io.github.alexbosch3000.familyhub.shopping.internal;

import io.github.alexbosch3000.familyhub.catalog.Item;
import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.shopping.ShoppingRow;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public final class ShoppingRows {

    private ShoppingRows() {
    }

    /** Lines merged into a row: the same item for the same member, or for the household. */
    private record RowKey(long itemId, @Nullable Long memberId) {
    }

    /**
     * Groups lines by item and member. Quantities are added per dimension, a row is checked when all
     * its lines are, and rows are ordered by store type, aisle, name, then member.
     *
     * @param items       catalog items of the lines, by id
     * @param recipeNames recipe names by recipe entry id
     */
    public static List<ShoppingRow> build(List<ShoppingLine> lines, Map<Long, Item> items,
            Map<Long, String> recipeNames) {
        Map<RowKey, List<ShoppingLine>> byRow = lines.stream().collect(Collectors.groupingBy(
                l -> new RowKey(l.itemId(), l.memberId()), LinkedHashMap::new, Collectors.toList()));
        return byRow.entrySet().stream()
                .map(e -> row(Objects.requireNonNull(items.get(e.getKey().itemId())), e.getKey().memberId(),
                        e.getValue(), recipeNames))
                .sorted(Comparator.comparing(ShoppingRow::storeType).thenComparing(ShoppingRow::aisle)
                        .thenComparing(ShoppingRow::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(r -> r.memberId() == null ? 0L : r.memberId()))
                .toList();
    }

    private static ShoppingRow row(Item item, @Nullable Long memberId, List<ShoppingLine> lines,
            Map<Long, String> recipeNames) {
        List<Quantity> quantities = Quantity.aggregate(lines.stream().map(ShoppingLine::quantity).toList());
        List<String> recipes = lines.stream().map(ShoppingLine::recipeEntryId).filter(Objects::nonNull)
                .map(recipeNames::get).filter(Objects::nonNull).distinct().toList();
        List<String> notes = lines.stream().map(ShoppingLine::note).filter(n -> n != null && !n.isBlank())
                .map(Objects::requireNonNull).distinct().toList();
        boolean manual = lines.stream().anyMatch(l -> l.recipeEntryId() == null);
        boolean checked = lines.stream().allMatch(ShoppingLine::checked);
        return new ShoppingRow(item.id(), memberId, item.name(), item.kind(), item.storeType(), item.aisle(),
                quantities, manual, recipes, notes, checked, estimate(item, quantities));
    }

    private static @Nullable Long estimate(Item item, List<Quantity> quantities) {
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
