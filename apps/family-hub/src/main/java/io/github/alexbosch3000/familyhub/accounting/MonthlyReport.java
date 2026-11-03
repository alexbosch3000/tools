// Budget report of a month: actual (approved) and forecast (planned or pending) amounts per category.
package io.github.alexbosch3000.familyhub.accounting;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.YearMonth;
import java.util.List;

public record MonthlyReport(YearMonth month, long incomeActualCents, long incomeForecastCents,
        long expenseActualCents, long expenseForecastCents, long budgetTotalCents, List<Line> categories,
        List<GroupLine> groups) {

    /** Net of approved amounts. */
    @JsonProperty
    public long netActualCents() {
        return incomeActualCents - expenseActualCents;
    }

    /** @param forecastCents planned and pending amounts, not yet approved */
    public record Line(String code, String name, Direction direction, BudgetGroup group, Long budgetCents,
            long actualCents, long forecastCents) {

        /** Budget left once forecasts are spent, null without a budget. */
        @JsonProperty
        public Long remainingCents() {
            return budgetCents == null ? null : budgetCents - actualCents - forecastCents;
        }
    }

    /** @param sharePercent share of the month's approved income, null without income */
    public record GroupLine(BudgetGroup group, long actualCents, int targetPercent, Integer sharePercent) {
    }
}
