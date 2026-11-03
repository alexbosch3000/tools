// Easter computation and French public holidays.
package io.github.alexbosch3000.familyhub.planning.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class FrenchHolidaysTest {

    @ParameterizedTest
    @CsvSource({"2024, 2024-03-31", "2025, 2025-04-20", "2026, 2026-04-05", "2027, 2027-03-28", "2038, 2038-04-25"})
    void computesEasterSunday(int year, LocalDate easter) {
        assertThat(FrenchHolidays.easterSunday(year)).isEqualTo(easter);
    }

    @Test
    void listsTheElevenHolidaysWithMovableFeasts() {
        Map<LocalDate, String> holidays = FrenchHolidays.of(2026);
        assertThat(holidays).hasSize(11)
                .containsEntry(LocalDate.of(2026, 4, 6), "Lundi de Pâques")
                .containsEntry(LocalDate.of(2026, 5, 14), "Ascension")
                .containsEntry(LocalDate.of(2026, 5, 25), "Lundi de Pentecôte")
                .containsEntry(LocalDate.of(2026, 7, 14), "Fête nationale");
        assertThat(List.copyOf(holidays.keySet())).isSorted();
    }
}
