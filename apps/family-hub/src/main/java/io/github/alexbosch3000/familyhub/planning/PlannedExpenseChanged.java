// Domain event: an EVENT entry carries an estimated cost, to be planned in the budget on its first day.
package io.github.alexbosch3000.familyhub.planning;

import java.time.LocalDate;

public record PlannedExpenseChanged(long entryId, String title, EventCategory category, LocalDate day,
        long amountCents) {
}
