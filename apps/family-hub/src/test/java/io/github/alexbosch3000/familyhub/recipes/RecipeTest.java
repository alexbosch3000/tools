// Scaling of a recipe to another number of servings.
package io.github.alexbosch3000.familyhub.recipes;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.catalog.Unit;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecipeTest {

    @Test
    void scalesEveryQuantity() {
        Recipe recipe = new Recipe(1, "Crêpes", 4, 10, 20, "", List.of(), null, List.of(
                new RecipeIngredient(1, "Farine", new Quantity(250, Unit.G), null),
                new RecipeIngredient(2, "Œufs", new Quantity(4, Unit.PIECE), null)));

        Recipe scaled = recipe.scaledTo(6);

        assertThat(scaled.servings()).isEqualTo(6);
        assertThat(scaled.ingredients()).extracting(RecipeIngredient::quantity)
                .containsExactly(new Quantity(375, Unit.G), new Quantity(6, Unit.PIECE));
    }
}
