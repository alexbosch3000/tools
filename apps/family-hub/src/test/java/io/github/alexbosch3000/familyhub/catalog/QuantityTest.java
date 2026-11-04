// Unit conversion, aggregation and pricing of quantities.
package io.github.alexbosch3000.familyhub.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class QuantityTest {

    @Test
    void keepsTheUnitWhenAllQuantitiesShareIt() {
        assertThat(Quantity.aggregate(List.of(new Quantity(2, Unit.TBSP), new Quantity(1, Unit.TBSP))))
                .containsExactly(new Quantity(3, Unit.TBSP));
    }

    @Test
    void convertsMixedUnitsAndPicksTheLargerUnitFromOneThousand() {
        assertThat(Quantity.aggregate(List.of(new Quantity(800, Unit.G), new Quantity(0.7, Unit.KG))))
                .containsExactly(new Quantity(1.5, Unit.KG));
        assertThat(Quantity.aggregate(List.of(new Quantity(50, Unit.CL), new Quantity(2, Unit.TBSP))))
                .containsExactly(new Quantity(530, Unit.ML));
    }

    @Test
    void keepsDimensionsApartInFirstSeenOrder() {
        assertThat(Quantity.aggregate(List.of(new Quantity(2, Unit.PIECE), new Quantity(150, Unit.G),
                new Quantity(1, Unit.PIECE), new Quantity(1, Unit.PACK))))
                .containsExactly(new Quantity(3, Unit.PIECE), new Quantity(150, Unit.G), new Quantity(1, Unit.PACK));
    }

    @Test
    void convertsOnlyWithinADimension() {
        assertThat(new Quantity(250, Unit.G).in(Unit.KG)).isEqualTo(0.25);
        assertThat(new Quantity(2, Unit.PIECE).in(Unit.KG)).isNull();
    }

    @Test
    void estimatesThePriceInTheItemsUnit() {
        Item tomatoes = new Item(1, "Tomates", ItemKind.INGREDIENT, StoreType.SUPERMARKET, Aisle.FRUITS_VEGETABLES,
                Unit.KG, 300L);
        assertThat(tomatoes.estimateCents(new Quantity(500, Unit.G))).isEqualTo(150);
        assertThat(tomatoes.estimateCents(new Quantity(3, Unit.PIECE))).isNull();
        Item unpriced = new Item(2, "Sel", ItemKind.INGREDIENT, StoreType.SUPERMARKET, Aisle.GROCERY, Unit.KG, null);
        assertThat(unpriced.estimateCents(new Quantity(1, Unit.KG))).isNull();
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThatThrownBy(() -> new Quantity(-1, Unit.G)).isInstanceOf(IllegalArgumentException.class);
    }
}
