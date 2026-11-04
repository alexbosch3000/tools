// A bill, subscription or regular income that generates a PLANNED transaction each time it falls due.
package io.github.alexbosch3000.familyhub.accounting;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/**
 * @param dayOfMonth due day; on shorter months the last day of the month
 * @param endDate    last possible due date, null for no end
 */
public record RecurringTransaction(long id, String label, String categoryCode, @Nullable String supplier,
        long amountCents, Period period, int dayOfMonth, LocalDate startDate, @Nullable LocalDate endDate,
        @Nullable Long accountId, boolean active) {
}
