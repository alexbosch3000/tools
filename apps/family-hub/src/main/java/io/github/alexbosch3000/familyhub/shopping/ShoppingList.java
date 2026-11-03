// A shopping list with its aggregated rows, the recipes it includes and its estimated cost.
package io.github.alexbosch3000.familyhub.shopping;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

/**
 * @param estimatedTotalCents sum of the rows that could be priced
 * @param unpricedRows        rows left out of the estimate
 */
public record ShoppingList(long id, String name, ShoppingStatus status, LocalDate plannedFor, LocalDate completedOn,
        Long amountPaidCents, List<ShoppingRow> rows, List<ListedRecipe> recipes, long estimatedTotalCents,
        int unpricedRows) {

    @JsonProperty
    public int checkedCount() {
        return (int) rows.stream().filter(ShoppingRow::checked).count();
    }
}
