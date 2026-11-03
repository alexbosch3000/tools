// Computes the monthly and yearly budget reports from categories and transactions.
package io.github.alexbosch3000.familyhub.accounting.internal;

import io.github.alexbosch3000.familyhub.accounting.BudgetGroup;
import io.github.alexbosch3000.familyhub.accounting.Category;
import io.github.alexbosch3000.familyhub.accounting.Direction;
import io.github.alexbosch3000.familyhub.accounting.MonthlyReport;
import io.github.alexbosch3000.familyhub.accounting.Transaction;
import io.github.alexbosch3000.familyhub.accounting.TransactionStatus;
import io.github.alexbosch3000.familyhub.accounting.YearlyReport;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public final class ReportCalculator {

    private ReportCalculator() {
    }

    /** Approved amounts are actual; planned and pending ones are forecast. Other months are ignored. */
    public static MonthlyReport monthly(YearMonth month, List<Category> categories, List<Transaction> transactions) {
        List<Transaction> inMonth = transactions.stream().filter(t -> YearMonth.from(t.day()).equals(month)).toList();
        List<MonthlyReport.Line> lines = categories.stream()
                .map(c -> new MonthlyReport.Line(c.code(), c.name(), c.direction(), c.group(), c.monthlyBudgetCents(),
                        sum(inMonth, t -> t.categoryCode().equals(c.code()) && approved(t)),
                        sum(inMonth, t -> t.categoryCode().equals(c.code()) && !approved(t))))
                .toList();
        long incomeActual = sum(inMonth, t -> t.direction() == Direction.INCOME && approved(t));
        List<MonthlyReport.GroupLine> groups = Arrays.stream(BudgetGroup.values())
                .map(g -> {
                    long actual = lines.stream().filter(l -> l.group() == g).mapToLong(MonthlyReport.Line::actualCents)
                            .sum();
                    Integer share = incomeActual == 0 ? null : (int) Math.round(actual * 100.0 / incomeActual);
                    return new MonthlyReport.GroupLine(g, actual, g.targetPercent(), share);
                })
                .toList();
        long budget = categories.stream().map(Category::monthlyBudgetCents).filter(Objects::nonNull)
                .mapToLong(Long::longValue).sum();
        return new MonthlyReport(month, incomeActual,
                sum(inMonth, t -> t.direction() == Direction.INCOME && !approved(t)),
                sum(inMonth, t -> t.direction() == Direction.EXPENSE && approved(t)),
                sum(inMonth, t -> t.direction() == Direction.EXPENSE && !approved(t)),
                budget, lines, groups);
    }

    public static YearlyReport yearly(int year, List<Transaction> transactions) {
        List<YearlyReport.Month> months = IntStream.rangeClosed(1, 12)
                .mapToObj(m -> {
                    YearMonth month = YearMonth.of(year, m);
                    List<Transaction> inMonth = transactions.stream()
                            .filter(t -> YearMonth.from(t.day()).equals(month)).toList();
                    return new YearlyReport.Month(month,
                            sum(inMonth, t -> t.direction() == Direction.INCOME && approved(t)),
                            sum(inMonth, t -> t.direction() == Direction.EXPENSE && approved(t)),
                            sum(inMonth, t -> t.direction() == Direction.INCOME && !approved(t)),
                            sum(inMonth, t -> t.direction() == Direction.EXPENSE && !approved(t)));
                })
                .toList();
        return new YearlyReport(year, months);
    }

    private static boolean approved(Transaction t) {
        return t.status() == TransactionStatus.APPROVED;
    }

    private static long sum(List<Transaction> transactions, Predicate<Transaction> filter) {
        return transactions.stream().filter(filter).mapToLong(Transaction::amountCents).sum();
    }
}
