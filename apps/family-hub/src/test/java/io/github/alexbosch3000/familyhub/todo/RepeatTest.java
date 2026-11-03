// Next due date of a repeating to-do item.
package io.github.alexbosch3000.familyhub.todo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RepeatTest {

    @Test
    void movesOnePeriodAfterTheDueDate() {
        LocalDateTime due = LocalDateTime.of(2026, 11, 3, 18, 0);
        assertThat(Repeat.WEEKLY.next(due, due.plusHours(1))).isEqualTo(LocalDateTime.of(2026, 11, 10, 18, 0));
        assertThat(Repeat.DAILY.next(due, due.minusHours(2))).isEqualTo(LocalDateTime.of(2026, 11, 4, 18, 0));
    }

    @Test
    void skipsPeriodsAlreadyPastWhenCompletedLate() {
        LocalDateTime due = LocalDateTime.of(2026, 10, 20, 18, 0);
        assertThat(Repeat.WEEKLY.next(due, LocalDateTime.of(2026, 11, 3, 10, 0)))
                .isEqualTo(LocalDateTime.of(2026, 11, 3, 18, 0));
    }

    @Test
    void monthlyKeepsTheOriginalDayOfMonth() {
        LocalDateTime due = LocalDateTime.of(2026, 1, 31, 9, 0);
        assertThat(Repeat.MONTHLY.next(due, LocalDateTime.of(2026, 3, 1, 0, 0)))
                .isEqualTo(LocalDateTime.of(2026, 3, 31, 9, 0));
    }
}
