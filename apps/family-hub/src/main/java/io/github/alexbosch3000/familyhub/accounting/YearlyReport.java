// Income and expenses month by month over a year.
package io.github.alexbosch3000.familyhub.accounting;

import java.time.YearMonth;
import java.util.List;

public record YearlyReport(int year, List<Month> months) {

    public record Month(YearMonth month, long incomeActualCents, long expenseActualCents, long incomeForecastCents,
            long expenseForecastCents) {
    }
}
