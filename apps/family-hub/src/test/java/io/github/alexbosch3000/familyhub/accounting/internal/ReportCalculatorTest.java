// Monthly and yearly budget reports.
package io.github.alexbosch3000.familyhub.accounting.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alexbosch3000.familyhub.accounting.BudgetGroup;
import io.github.alexbosch3000.familyhub.accounting.Category;
import io.github.alexbosch3000.familyhub.accounting.Direction;
import io.github.alexbosch3000.familyhub.accounting.MonthlyReport;
import io.github.alexbosch3000.familyhub.accounting.SourceType;
import io.github.alexbosch3000.familyhub.accounting.Transaction;
import io.github.alexbosch3000.familyhub.accounting.TransactionStatus;
import io.github.alexbosch3000.familyhub.accounting.YearlyReport;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportCalculatorTest {

    private static final List<Category> CATEGORIES = List.of(
            new Category("salaires", "Salaires", Direction.INCOME, null, null, 1),
            new Category("courses", "Courses", Direction.EXPENSE, BudgetGroup.NEEDS, 60_000L, 2),
            new Category("loisirs", "Loisirs", Direction.EXPENSE, BudgetGroup.WANTS, 10_000L, 3),
            new Category("epargne", "Épargne", Direction.EXPENSE, BudgetGroup.SAVINGS, null, 4));

    private static Transaction tx(String day, long cents, String category, TransactionStatus status) {
        Direction direction = category.equals("salaires") ? Direction.INCOME : Direction.EXPENSE;
        return new Transaction(0, LocalDate.parse(day), category, cents, direction, category, category, null, status,
                SourceType.MANUAL, null, null, null, null, null);
    }

    private static final List<Transaction> TRANSACTIONS = List.of(
            tx("2026-11-01", 300_000, "salaires", TransactionStatus.APPROVED),
            tx("2026-11-07", 20_000, "courses", TransactionStatus.APPROVED),
            tx("2026-11-14", 8_000, "courses", TransactionStatus.PENDING),
            tx("2026-11-20", 5_000, "loisirs", TransactionStatus.PLANNED),
            tx("2026-11-02", 30_000, "epargne", TransactionStatus.APPROVED),
            tx("2026-10-30", 99_999, "courses", TransactionStatus.APPROVED));

    @Test
    void separatesActualFromForecastAndComputesRemainingEnvelopes() {
        MonthlyReport report = ReportCalculator.monthly(YearMonth.of(2026, 11), CATEGORIES, TRANSACTIONS);

        assertThat(report.incomeActualCents()).isEqualTo(300_000);
        assertThat(report.expenseActualCents()).isEqualTo(50_000);
        assertThat(report.expenseForecastCents()).isEqualTo(13_000);
        assertThat(report.netActualCents()).isEqualTo(250_000);
        assertThat(report.budgetTotalCents()).isEqualTo(70_000);
        MonthlyReport.Line groceries = report.categories().get(1);
        assertThat(groceries.actualCents()).isEqualTo(20_000);
        assertThat(groceries.forecastCents()).isEqualTo(8_000);
        assertThat(groceries.remainingCents()).isEqualTo(32_000);
        assertThat(report.categories().get(3).remainingCents()).isNull();
    }

    @Test
    void comparesGroupsWithThe503020Rule() {
        MonthlyReport report = ReportCalculator.monthly(YearMonth.of(2026, 11), CATEGORIES, TRANSACTIONS);
        assertThat(report.groups()).extracting(MonthlyReport.GroupLine::group, MonthlyReport.GroupLine::sharePercent,
                        MonthlyReport.GroupLine::targetPercent)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(BudgetGroup.NEEDS, 7, 50),
                        org.assertj.core.groups.Tuple.tuple(BudgetGroup.WANTS, 0, 30),
                        org.assertj.core.groups.Tuple.tuple(BudgetGroup.SAVINGS, 10, 20));

        MonthlyReport noIncome = ReportCalculator.monthly(YearMonth.of(2026, 10), CATEGORIES, TRANSACTIONS);
        assertThat(noIncome.groups().getFirst().sharePercent()).isNull();
    }

    @Test
    void totalsEveryMonthOfTheYear() {
        YearlyReport report = ReportCalculator.yearly(2026, TRANSACTIONS);
        assertThat(report.months()).hasSize(12);
        YearlyReport.Month november = report.months().get(10);
        assertThat(november.incomeActualCents()).isEqualTo(300_000);
        assertThat(november.expenseActualCents()).isEqualTo(50_000);
        assertThat(november.expenseForecastCents()).isEqualTo(13_000);
        assertThat(report.months().get(9).expenseActualCents()).isEqualTo(99_999);
    }
}
