// Aggregation of shopping lines into rows and choice of the main store type.
package io.github.alexbosch3000.familyhub.shopping.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alexbosch3000.familyhub.catalog.Aisle;
import io.github.alexbosch3000.familyhub.catalog.Item;
import io.github.alexbosch3000.familyhub.catalog.ItemKind;
import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.catalog.StoreType;
import io.github.alexbosch3000.familyhub.catalog.Unit;
import io.github.alexbosch3000.familyhub.shopping.Purchase;
import io.github.alexbosch3000.familyhub.shopping.ShoppingRow;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShoppingRowsTest {

    private static final Item ONIONS = new Item(1, "Oignons", ItemKind.INGREDIENT, StoreType.SUPERMARKET,
            Aisle.FRUITS_VEGETABLES, Unit.KG, 200L);
    private static final Item MILK = new Item(2, "Lait", ItemKind.INGREDIENT, StoreType.SUPERMARKET, Aisle.DAIRY,
            Unit.L, 110L);
    private static final Item SCREWS = new Item(3, "Vis", ItemKind.GOODS, StoreType.DIY, Aisle.HARDWARE, Unit.PACK,
            null);
    private static final Map<Long, Item> ITEMS = Map.of(1L, ONIONS, 2L, MILK, 3L, SCREWS);

    @Test
    void mergesLinesOfAnItemAndKeepsTheirOrigin() {
        List<ShoppingLine> lines = List.of(
                new ShoppingLine(1, 1, 10L, null, new Quantity(300, Unit.G), true, null),
                new ShoppingLine(2, 1, 11L, null, new Quantity(200, Unit.G), false, null),
                new ShoppingLine(3, 1, null, null, new Quantity(2, Unit.PIECE), false, "rouges"));

        ShoppingRow onions = ShoppingRows.build(lines, ITEMS, Map.of(10L, "Ratatouille", 11L, "Soupe"),
                Map.of()).getFirst();

        assertThat(onions.quantities()).containsExactly(new Quantity(500, Unit.G), new Quantity(2, Unit.PIECE));
        assertThat(onions.recipes()).containsExactly("Ratatouille", "Soupe");
        assertThat(onions.notes()).containsExactly("rouges");
        assertThat(onions.manual()).isTrue();
        assertThat(onions.checked()).isFalse();
        assertThat(onions.estimatedCents()).as("pieces cannot be priced per kilogram").isNull();
    }

    @Test
    void sortsByStoreTypeThenAisleAndPricesWhatItCan() {
        List<ShoppingLine> lines = List.of(
                new ShoppingLine(1, 3, null, null, new Quantity(1, Unit.PACK), true, null),
                new ShoppingLine(2, 2, null, null, new Quantity(2, Unit.L), true, null),
                new ShoppingLine(3, 1, null, null, new Quantity(1, Unit.KG), false, null));

        List<ShoppingRow> rows = ShoppingRows.build(lines, ITEMS, Map.of(), Map.of());

        assertThat(rows).extracting(ShoppingRow::name).containsExactly("Oignons", "Lait", "Vis");
        assertThat(rows).extracting(ShoppingRow::estimatedCents).containsExactly(200L, 220L, null);
        assertThat(rows.get(1).checked()).isTrue();
    }

    @Test
    void keepsTheSameItemForDifferentMembersApart() {
        Item shoes = new Item(4, "Chaussures", ItemKind.GOODS, StoreType.CLOTHING, Aisle.SHOES, Unit.PIECE, 4500L);
        List<ShoppingRow> rows = ShoppingRows.build(List.of(
                new ShoppingLine(1, 4, null, 2L, new Quantity(1, Unit.PIECE), false, "pointure 32"),
                new ShoppingLine(2, 4, null, 1L, new Quantity(1, Unit.PIECE), true, null),
                new ShoppingLine(3, 4, null, 2L, new Quantity(1, Unit.PIECE), false, null)),
                Map.of(4L, shoes), Map.of(), Map.of());

        assertThat(rows).extracting(ShoppingRow::memberId).containsExactly(1L, 2L);
        assertThat(rows.get(1).quantities()).containsExactly(new Quantity(2, Unit.PIECE));
        assertThat(rows.get(1).estimatedCents()).isEqualTo(9000);
        assertThat(rows.get(0).checked()).isTrue();
    }

    private static ShoppingRow rowWith(Purchase purchase) {
        List<ShoppingLine> lines = List.of(
                new ShoppingLine(1, 1, null, null, new Quantity(4, Unit.PIECE), false, null));
        Map<ShoppingRows.RowKey, Purchase> purchases = Map.of(new ShoppingRows.RowKey(1, null), purchase);
        return ShoppingRows.build(lines, ITEMS, Map.of(), purchases).getFirst();
    }

    @Test
    void aPurchaseWithAPriceReplacesTheEstimate() {
        ShoppingRow row = rowWith(new Purchase(new Quantity(1, Unit.PACK), 120L, null));

        assertThat(row.quantities()).containsExactly(new Quantity(4, Unit.PIECE));
        assertThat(row.purchase()).isNotNull();
        assertThat(row.estimatedCents()).isEqualTo(120);
    }

    @Test
    void aPurchaseWithoutAPriceIsConvertedThroughTheItemUnit() {
        assertThat(rowWith(new Purchase(new Quantity(500, Unit.G), null, null)).estimatedCents()).isEqualTo(100);
    }

    @Test
    void aPurchaseInAnotherDimensionWithoutAPriceCannotBePriced() {
        assertThat(rowWith(new Purchase(new Quantity(1, Unit.PACK), null, null)).estimatedCents()).isNull();
    }
}
