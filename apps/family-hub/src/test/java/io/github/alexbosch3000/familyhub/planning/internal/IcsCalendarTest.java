// iCalendar output: local times with TZID, recurrence rules, escaping and line folding.
package io.github.alexbosch3000.familyhub.planning.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alexbosch3000.familyhub.planning.EntryType;
import io.github.alexbosch3000.familyhub.planning.EventCategory;
import io.github.alexbosch3000.familyhub.planning.Frequency;
import io.github.alexbosch3000.familyhub.planning.PlanningEntry;
import io.github.alexbosch3000.familyhub.planning.Recurrence;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class IcsCalendarTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final Instant NOW = Instant.parse("2026-11-03T09:00:00Z");

    private static PlanningEntry entry(long id, String title, LocalDateTime start, LocalDateTime end, boolean allDay,
            Recurrence recurrence, List<LocalDate> exceptions) {
        return new PlanningEntry(id, title, "", null, EntryType.EVENT, start, end, allDay, recurrence, exceptions,
                List.of(), EventCategory.SPORT, null, null, null, null, null, false);
    }

    @Test
    void writesRecurringEntriesInLocalTimeWithUtcUntilAndExceptions() {
        Recurrence rule = new Recurrence(Frequency.WEEKLY, 1, List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 12, 18));
        String ics = IcsCalendar.write(List.of(entry(7, "Judo, karaté; dojo", LocalDateTime.of(2026, 11, 4, 18, 0),
                LocalDateTime.of(2026, 11, 4, 19, 30), false, rule, List.of(LocalDate.of(2026, 11, 11)))),
                PARIS, NOW, "Famille");

        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n")
                .contains("UID:entry-7@family-hub\r\n")
                .contains("DTSTAMP:20261103T090000Z\r\n")
                .contains("DTSTART;TZID=Europe/Paris:20261104T180000\r\n")
                .contains("DTEND;TZID=Europe/Paris:20261104T193000\r\n")
                .contains("SUMMARY:Judo\\, karaté\\; dojo\r\n")
                .contains("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE;UNTIL=20261218T225959Z\r\n")
                .contains("EXDATE;TZID=Europe/Paris:20261111T180000\r\n");
    }

    @Test
    void writesAllDayEntriesWithAnExclusiveEndDate() {
        String ics = IcsCalendar.write(List.of(entry(1, "Vacances", LocalDateTime.of(2026, 12, 19, 0, 0),
                LocalDateTime.of(2027, 1, 3, 23, 59), true, null, List.of())), PARIS, NOW, "Famille");
        assertThat(ics).contains("DTSTART;VALUE=DATE:20261219\r\n").contains("DTEND;VALUE=DATE:20270104\r\n");
    }

    @Test
    void describesTheDaylightSavingRulesOfTheZone() {
        String ics = IcsCalendar.write(List.of(), PARIS, NOW, "Famille");
        assertThat(ics).contains("BEGIN:VTIMEZONE\r\nTZID:Europe/Paris\r\n")
                .contains("BEGIN:DAYLIGHT\r\n")
                .contains("TZOFFSETFROM:+0100\r\nTZOFFSETTO:+0200\r\nRRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\r\n")
                .contains("TZOFFSETFROM:+0200\r\nTZOFFSETTO:+0100\r\nRRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\r\n");
    }

    @Test
    void foldsLongLinesAtSeventyFiveOctetsWithoutSplittingCharacters() {
        StringBuilder out = new StringBuilder();
        IcsCalendar.line(out, "SUMMARY:" + "é".repeat(60));
        String[] lines = out.toString().split("\r\n");
        assertThat(lines).hasSizeGreaterThan(1);
        assertThat(Arrays.stream(lines).mapToInt(l -> l.getBytes(StandardCharsets.UTF_8).length).max().orElseThrow())
                .isLessThanOrEqualTo(75);
        assertThat(String.join("", Arrays.stream(lines).map(l -> l.startsWith(" ") ? l.substring(1) : l).toList()))
                .isEqualTo("SUMMARY:" + "é".repeat(60));
    }
}
