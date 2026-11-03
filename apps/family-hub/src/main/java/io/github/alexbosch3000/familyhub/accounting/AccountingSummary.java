// Budget figures shown on the dashboard.
package io.github.alexbosch3000.familyhub.accounting;

import java.time.YearMonth;
import java.util.List;

/** @param upcoming transactions not yet approved in the next 30 days, soonest first */
public record AccountingSummary(YearMonth month, long expenseActualCents, long expenseForecastCents,
        long budgetTotalCents, int pendingApprovals, List<Transaction> upcoming, List<Account> accounts) {
}
