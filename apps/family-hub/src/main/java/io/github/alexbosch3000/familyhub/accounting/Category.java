// A budget category (envelope); income categories have no group and no budget.
package io.github.alexbosch3000.familyhub.accounting;
import org.jspecify.annotations.Nullable;

/** @param monthlyBudgetCents envelope for one month, null when the family has not set one */
public record Category(String code, String name, Direction direction, @Nullable BudgetGroup group,
        @Nullable Long monthlyBudgetCents, int position) {
}
