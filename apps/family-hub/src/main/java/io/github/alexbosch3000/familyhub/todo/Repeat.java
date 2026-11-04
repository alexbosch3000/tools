// Repetition of a to-do item: completing it creates the next one, due one period later.
package io.github.alexbosch3000.familyhub.todo;

import java.time.LocalDateTime;

public enum Repeat {
    DAILY,
    WEEKLY,
    MONTHLY;

    /**
     * Next due date-time after {@code due}, skipping periods already past at {@code now}, so that a
     * chore completed late does not leave a backlog of overdue copies.
     */
    public LocalDateTime next(LocalDateTime due, LocalDateTime now) {
        LocalDateTime next = step(due, 1);
        for (int periods = 2; !next.isAfter(now); periods++) {
            next = step(due, periods);
        }
        return next;
    }

    private LocalDateTime step(LocalDateTime from, int periods) {
        return switch (this) {
            case DAILY -> from.plusDays(periods);
            case WEEKLY -> from.plusWeeks(periods);
            case MONTHLY -> from.plusMonths(periods);
        };
    }
}
