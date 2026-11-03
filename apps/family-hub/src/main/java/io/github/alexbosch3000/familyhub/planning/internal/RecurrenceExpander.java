// Expands a planning entry, recurring or not, into its occurrences within a date range.
package io.github.alexbosch3000.familyhub.planning.internal;

import io.github.alexbosch3000.familyhub.planning.Recurrence;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RecurrenceExpander {

    /** Guards against unbounded loops on rules starting far in the past. */
    private static final int MAX_STEPS = 50_000;

    private RecurrenceExpander() {
    }

    /**
     * Start date-times of the occurrences overlapping {@code [from, to]} (whole days). Monthly and
     * yearly repetitions are computed from the first start, so a rule on the 31st falls on the last
     * day of shorter months instead of drifting.
     */
    public static List<LocalDateTime> starts(LocalDateTime start, LocalDateTime end, Recurrence rule,
            Collection<LocalDate> exceptions, LocalDate from, LocalDate to) {
        Duration duration = Duration.between(start, end);
        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEnd = to.plusDays(1).atStartOfDay();
        List<LocalDateTime> result = new ArrayList<>();
        if (rule == null) {
            if (overlaps(start, start.plus(duration), rangeStart, rangeEnd)) {
                result.add(start);
            }
            return result;
        }
        for (int step = 0; step < MAX_STEPS; step++) {
            for (LocalDateTime candidate : candidates(start, rule, step)) {
                if (candidate.isBefore(start)) {
                    continue;
                }
                if ((rule.until() != null && candidate.toLocalDate().isAfter(rule.until()))
                        || !candidate.isBefore(rangeEnd)) {
                    return result;
                }
                if (!exceptions.contains(candidate.toLocalDate())
                        && overlaps(candidate, candidate.plus(duration), rangeStart, rangeEnd)) {
                    result.add(candidate);
                }
            }
        }
        return result;
    }

    /** Occurrences of the {@code step}-th period, in chronological order. */
    private static List<LocalDateTime> candidates(LocalDateTime start, Recurrence rule, int step) {
        long n = (long) step * rule.interval();
        return switch (rule.frequency()) {
            case DAILY -> List.of(start.plusDays(n));
            case MONTHLY -> List.of(start.plusMonths(n));
            case YEARLY -> List.of(start.plusYears(n));
            case WEEKLY -> {
                LocalDate week = start.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .plusWeeks(n);
                List<DayOfWeek> days = rule.days().isEmpty() ? List.of(start.getDayOfWeek()) : rule.days();
                yield days.stream()
                        .map(day -> week.plusDays(day.ordinal()).atTime(start.toLocalTime()))
                        .toList();
            }
        };
    }

    /** Whether an occurrence touches the range; an instant occurrence counts on the day it happens. */
    private static boolean overlaps(LocalDateTime start, LocalDateTime end, LocalDateTime rangeStart,
            LocalDateTime rangeEnd) {
        return start.isBefore(rangeEnd) && (end.isAfter(rangeStart) || !start.isBefore(rangeStart));
    }
}
