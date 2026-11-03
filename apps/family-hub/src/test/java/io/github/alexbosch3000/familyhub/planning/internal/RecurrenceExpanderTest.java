// Expansion of one-off and recurring entries into the occurrences of a date range.
package io.github.alexbosch3000.familyhub.planning.internal;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.alexbosch3000.familyhub.planning.Frequency;
import io.github.alexbosch3000.familyhub.planning.Recurrence;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecurrenceExpanderTest {

    private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate NOV_30 = LocalDate.of(2026, 11, 30);

    private static LocalDateTime at(int month, int day, int hour) {
        return LocalDateTime.of(2026, month, day, hour, 0);
    }

    @Test
    void oneOffEntryIsKeptOnlyWhenItOverlapsTheRange() {
        assertThat(RecurrenceExpander.starts(at(11, 3, 10), at(11, 3, 11), null, List.of(), NOV_1, NOV_30))
                .containsExactly(at(11, 3, 10));
        assertThat(RecurrenceExpander.starts(at(12, 3, 10), at(12, 3, 11), null, List.of(), NOV_1, NOV_30)).isEmpty();
    }

    @Test
    void multiDayEntryStartingBeforeTheRangeOverlapsIt() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 31, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 2, 23, 59);
        assertThat(RecurrenceExpander.starts(start, end, null, List.of(), NOV_1, LocalDate.of(2026, 11, 7)))
                .containsExactly(start);
    }

    @Test
    void weeklyOnSeveralDaysUntilAnEndDateWithoutTheSkippedDays() {
        Recurrence rule = new Recurrence(Frequency.WEEKLY, 1, List.of(MONDAY, WEDNESDAY), LocalDate.of(2026, 11, 18));
        assertThat(RecurrenceExpander.starts(at(11, 4, 18), at(11, 4, 19), rule, List.of(LocalDate.of(2026, 11, 11)),
                NOV_1, NOV_30))
                .containsExactly(at(11, 4, 18), at(11, 9, 18), at(11, 16, 18), at(11, 18, 18));
    }

    @Test
    void everyOtherWeekDefaultsToTheDayOfTheFirstOccurrence() {
        Recurrence rule = new Recurrence(Frequency.WEEKLY, 2, List.of(), null);
        assertThat(RecurrenceExpander.starts(at(11, 4, 18), at(11, 4, 19), rule, List.of(), NOV_1,
                LocalDate.of(2026, 12, 6)))
                .containsExactly(at(11, 4, 18), at(11, 18, 18), at(12, 2, 18));
    }

    @Test
    void monthlyOnThe31stFallsOnTheLastDayOfShorterMonths() {
        Recurrence rule = new Recurrence(Frequency.MONTHLY, 1, List.of(), null);
        assertThat(RecurrenceExpander.starts(at(1, 31, 9), at(1, 31, 10), rule, List.of(), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 4, 30)))
                .containsExactly(at(2, 28, 9), at(3, 31, 9), at(4, 30, 9));
    }

    @Test
    void yearlyOnTheTwentyNinthOfFebruary() {
        Recurrence rule = new Recurrence(Frequency.YEARLY, 1, List.of(), null);
        LocalDateTime birth = LocalDateTime.of(2024, 2, 29, 0, 0);
        assertThat(RecurrenceExpander.starts(birth, birth.plusHours(23), rule, List.of(), LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31)))
                .containsExactly(LocalDateTime.of(2025, 2, 28, 0, 0));
    }

    @Test
    void dailyRuleStartedLongAgoAndOccurrencesCrossingMidnight() {
        Recurrence rule = new Recurrence(Frequency.DAILY, 1, List.of(), null);
        LocalDateTime start = LocalDateTime.of(2020, 1, 1, 23, 0);
        LocalDate day = LocalDate.of(2026, 11, 3);
        assertThat(RecurrenceExpander.starts(start, start.plusHours(2), rule, List.of(), day, day))
                .containsExactly(at(11, 2, 23), at(11, 3, 23));
    }
}
