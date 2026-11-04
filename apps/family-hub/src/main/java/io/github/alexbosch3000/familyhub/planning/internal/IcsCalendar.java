// Writes planning entries as an iCalendar (RFC 5545) feed that phone and desktop calendars can subscribe to.
package io.github.alexbosch3000.familyhub.planning.internal;

import io.github.alexbosch3000.familyhub.planning.PlanningEntry;
import io.github.alexbosch3000.familyhub.planning.Recurrence;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneOffsetTransitionRule;
import java.util.List;
import java.util.stream.Collectors;

public final class IcsCalendar {

    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MAX_LINE_OCTETS = 75;

    private IcsCalendar() {
    }

    /**
     * Entries are written with local times and a {@code TZID}, so that a weekly 18:00 activity stays
     * at 18:00 after a daylight-saving change, and the time zone is described in a VTIMEZONE block.
     */
    public static String write(List<PlanningEntry> entries, ZoneId zone, Instant now, String name) {
        StringBuilder out = new StringBuilder();
        line(out, "BEGIN:VCALENDAR");
        line(out, "VERSION:2.0");
        line(out, "PRODID:-//family-hub//planning//FR");
        line(out, "CALSCALE:GREGORIAN");
        line(out, "METHOD:PUBLISH");
        line(out, "X-WR-CALNAME:" + escape(name));
        line(out, "X-WR-TIMEZONE:" + zone.getId());
        timeZone(out, zone);
        String stamp = LOCAL.format(now.atOffset(ZoneOffset.UTC)) + "Z";
        for (PlanningEntry entry : entries) {
            event(out, entry, zone, stamp);
        }
        line(out, "END:VCALENDAR");
        return out.toString();
    }

    private static void event(StringBuilder out, PlanningEntry entry, ZoneId zone, String stamp) {
        line(out, "BEGIN:VEVENT");
        line(out, "UID:entry-" + entry.id() + "@family-hub");
        line(out, "DTSTAMP:" + stamp);
        if (entry.allDay()) {
            line(out, "DTSTART;VALUE=DATE:" + DATE.format(entry.start()));
            line(out, "DTEND;VALUE=DATE:" + DATE.format(entry.end().toLocalDate().plusDays(1)));
        } else {
            line(out, "DTSTART;TZID=" + zone.getId() + ":" + LOCAL.format(entry.start()));
            line(out, "DTEND;TZID=" + zone.getId() + ":" + LOCAL.format(entry.end()));
        }
        line(out, "SUMMARY:" + escape(entry.title()));
        if (entry.description() != null && !entry.description().isBlank()) {
            line(out, "DESCRIPTION:" + escape(entry.description()));
        }
        if (entry.location() != null && !entry.location().isBlank()) {
            line(out, "LOCATION:" + escape(entry.location()));
        }
        line(out, "CATEGORIES:" + (entry.category() == null ? entry.type().name() : entry.category().name()));
        if (entry.recurrence() != null) {
            line(out, "RRULE:" + rule(entry.recurrence(), entry.allDay(), zone));
            for (LocalDate day : entry.exceptions()) {
                line(out, entry.allDay()
                        ? "EXDATE;VALUE=DATE:" + DATE.format(day)
                        : "EXDATE;TZID=" + zone.getId() + ":" + LOCAL.format(day.atTime(entry.start().toLocalTime())));
            }
        }
        line(out, "END:VEVENT");
    }

    /** UNTIL is a date for all-day entries and, as RFC 5545 requires with TZID, a UTC time otherwise. */
    static String rule(Recurrence recurrence, boolean allDay, ZoneId zone) {
        StringBuilder rule = new StringBuilder("FREQ=").append(recurrence.frequency().name())
                .append(";INTERVAL=").append(recurrence.interval());
        if (!recurrence.days().isEmpty()) {
            rule.append(";BYDAY=").append(recurrence.days().stream().map(IcsCalendar::day)
                    .collect(Collectors.joining(",")));
        }
        if (recurrence.until() != null) {
            rule.append(";UNTIL=").append(allDay
                    ? DATE.format(recurrence.until())
                    : LOCAL.format(recurrence.until().atTime(23, 59, 59).atZone(zone)
                            .withZoneSameInstant(ZoneOffset.UTC)) + "Z");
        }
        return rule.toString();
    }

    private static String day(DayOfWeek day) {
        return day.name().substring(0, 2);
    }

    /** Describes the zone's current daylight-saving rules, or its fixed offset when it has none. */
    private static void timeZone(StringBuilder out, ZoneId zone) {
        line(out, "BEGIN:VTIMEZONE");
        line(out, "TZID:" + zone.getId());
        List<ZoneOffsetTransitionRule> rules = zone.getRules().getTransitionRules();
        if (rules.isEmpty()) {
            ZoneOffset offset = zone.getRules().getOffset(Instant.EPOCH);
            line(out, "BEGIN:STANDARD");
            line(out, "DTSTART:19700101T000000");
            line(out, "TZOFFSETFROM:" + offset(offset));
            line(out, "TZOFFSETTO:" + offset(offset));
            line(out, "END:STANDARD");
        }
        for (ZoneOffsetTransitionRule transition : rules) {
            boolean daylight = transition.getOffsetAfter().getTotalSeconds()
                    > transition.getStandardOffset().getTotalSeconds();
            String kind = daylight ? "DAYLIGHT" : "STANDARD";
            LocalDateTime first = transition.createTransition(1970).getDateTimeBefore();
            line(out, "BEGIN:" + kind);
            line(out, "DTSTART:" + LOCAL.format(first));
            line(out, "TZOFFSETFROM:" + offset(transition.getOffsetBefore()));
            line(out, "TZOFFSETTO:" + offset(transition.getOffsetAfter()));
            line(out, "RRULE:FREQ=YEARLY;BYMONTH=" + transition.getMonth().getValue() + ";BYDAY="
                    + byDay(transition));
            line(out, "END:" + kind);
        }
        line(out, "END:VTIMEZONE");
    }

    /**
     * Transition rules are "last Sunday of the month" (negative or late day indicator) or "first
     * Sunday on or after a day".
     */
    private static String byDay(ZoneOffsetTransitionRule transition) {
        String day = day(transition.getDayOfWeek());
        int indicator = transition.getDayOfMonthIndicator();
        if (indicator < 0 || indicator > 24) {
            return "-1" + day;
        }
        return ((indicator - 1) / 7 + 1) + day;
    }

    private static String offset(ZoneOffset offset) {
        int seconds = offset.getTotalSeconds();
        int minutes = Math.abs(seconds) / 60;
        return String.format("%s%02d%02d", seconds < 0 ? "-" : "+", minutes / 60, minutes % 60);
    }

    static String escape(String text) {
        return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
                .replace("\r\n", "\\n").replace("\n", "\\n");
    }

    /** Appends a content line folded at 75 octets, without splitting a UTF-8 character, ended by CRLF. */
    static void line(StringBuilder out, String content) {
        int octets = 0;
        int limit = MAX_LINE_OCTETS;
        for (int i = 0; i < content.length(); ) {
            int codePoint = content.codePointAt(i);
            String character = new String(Character.toChars(codePoint));
            int size = character.getBytes(StandardCharsets.UTF_8).length;
            if (octets + size > limit) {
                out.append("\r\n ");
                octets = 0;
                limit = MAX_LINE_OCTETS - 1;
            }
            out.append(character);
            octets += size;
            i += Character.charCount(codePoint);
        }
        out.append("\r\n");
    }
}
