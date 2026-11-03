// A budget category (envelope); income categories have no group and no budget.
package io.github.alexbosch3000.familyhub.accounting;

/** @param monthlyBudgetCents envelope for one month, null when the family has not set one */
public record Category(String code, String name, Direction direction, BudgetGroup group, Long monthlyBudgetCents,
        int position) {
}
