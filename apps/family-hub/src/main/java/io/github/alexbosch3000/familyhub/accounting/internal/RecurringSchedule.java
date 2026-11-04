// Computes when a recurring transaction falls due in a given month.
package io.github.alexbosch3000.familyhub.accounting.internal;

import io.github.alexbosch3000.familyhub.accounting.RecurringTransaction;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public final class RecurringSchedule {

    private RecurringSchedule() {
    }

    /**
     * The due date in {@code month}, if any: months are counted from the start month in steps of the
     * period, the day is capped to the month's length, and dates outside {@code [start, end]} are skipped.
     */
    public static Optional<LocalDate> dueDate(RecurringTransaction recurring, YearMonth month) {
        YearMonth first = YearMonth.from(recurring.startDate());
        if (!recurring.active() || month.isBefore(first)
                || first.until(month, ChronoUnit.MONTHS) % recurring.period().months() != 0) {
            return Optional.empty();
        }
        LocalDate due = month.atDay(Math.min(recurring.dayOfMonth(), month.lengthOfMonth()));
        if (due.isBefore(recurring.startDate()) || (recurring.endDate() != null && due.isAfter(recurring.endDate()))) {
            return Optional.empty();
        }
        return Optional.of(due);
    }

    /** Source reference of the transaction generated for a month, unique per recurring transaction and month. */
    public static String sourceRef(long recurringId, YearMonth month) {
        return "recurring-" + recurringId + "-" + month;
    }

    public static String sourceRefPrefix(long recurringId) {
        return "recurring-" + recurringId + "-";
    }
}
