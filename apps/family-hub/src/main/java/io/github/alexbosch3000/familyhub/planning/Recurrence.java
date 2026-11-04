// Repetition rule of a planning entry, a subset of iCalendar RRULE.
package io.github.alexbosch3000.familyhub.planning;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @param interval every {@code interval} periods (2 with WEEKLY: every other week)
 * @param days     week days of a WEEKLY rule; empty means the day of the first occurrence
 * @param until    last possible day, inclusive; null repeats forever
 */
public record Recurrence(Frequency frequency, int interval, List<DayOfWeek> days, @Nullable LocalDate until) {

    public Recurrence {
        days = days == null ? List.of() : days.stream().distinct().sorted().toList();
    }
}
