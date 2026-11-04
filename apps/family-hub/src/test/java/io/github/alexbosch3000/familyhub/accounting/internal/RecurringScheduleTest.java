// Due dates of recurring transactions.
package io.github.alexbosch3000.familyhub.accounting.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alexbosch3000.familyhub.accounting.Period;
import io.github.alexbosch3000.familyhub.accounting.RecurringTransaction;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class RecurringScheduleTest {

    private static RecurringTransaction recurring(Period period, int day, LocalDate start, LocalDate end,
            boolean active) {
        return new RecurringTransaction(1, "Électricité", "energie", "EDF", 9000, period, day, start, end, null,
                active);
    }

    @Test
    void capsTheDayToTheLengthOfTheMonth() {
        RecurringTransaction rent = recurring(Period.MONTHLY, 31, LocalDate.of(2026, 1, 1), null, true);
        assertThat(RecurringSchedule.dueDate(rent, YearMonth.of(2026, 2))).contains(LocalDate.of(2026, 2, 28));
        assertThat(RecurringSchedule.dueDate(rent, YearMonth.of(2026, 3))).contains(LocalDate.of(2026, 3, 31));
    }

    @Test
    void stepsByThePeriodFromTheStartMonth() {
        RecurringTransaction water = recurring(Period.QUARTERLY, 15, LocalDate.of(2026, 1, 15), null, true);
        assertThat(RecurringSchedule.dueDate(water, YearMonth.of(2026, 4))).contains(LocalDate.of(2026, 4, 15));
        assertThat(RecurringSchedule.dueDate(water, YearMonth.of(2026, 5))).isEmpty();
        assertThat(RecurringSchedule.dueDate(water, YearMonth.of(2025, 10))).isEmpty();
    }

    @Test
    void skipsDatesOutsideStartAndEndAndInactiveItems() {
        RecurringTransaction late = recurring(Period.MONTHLY, 5, LocalDate.of(2026, 11, 20), LocalDate.of(2027, 1, 4),
                true);
        assertThat(RecurringSchedule.dueDate(late, YearMonth.of(2026, 11))).isEmpty();
        assertThat(RecurringSchedule.dueDate(late, YearMonth.of(2026, 12))).contains(LocalDate.of(2026, 12, 5));
        assertThat(RecurringSchedule.dueDate(late, YearMonth.of(2027, 1))).isEmpty();
        assertThat(RecurringSchedule.dueDate(recurring(Period.MONTHLY, 5, LocalDate.of(2026, 1, 1), null, false),
                YearMonth.of(2026, 11))).isEmpty();
    }
}
