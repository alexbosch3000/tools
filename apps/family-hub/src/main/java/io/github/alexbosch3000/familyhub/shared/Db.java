// Conversions between Java values and their SQLite storage (ISO-8601 text, integer booleans, nullable numbers).
package io.github.alexbosch3000.familyhub.shared;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SQLite has no date type: date-times are stored as {@code yyyy-MM-ddTHH:mm} text, which sorts
 * chronologically, and dates as {@code yyyy-MM-dd}.
 */
public final class Db {

    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private Db() {
    }

    public static String text(LocalDateTime value) {
        return value == null ? null : value.format(MINUTE);
    }

    public static String text(LocalDate value) {
        return value == null ? null : value.toString();
    }

    public static String text(Enum<?> value) {
        return value == null ? null : value.name();
    }

    public static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : LocalDateTime.parse(value);
    }

    public static LocalDate date(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : LocalDate.parse(value);
    }

    public static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    public static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    public static <E extends Enum<E>> E enumValue(ResultSet rs, String column, Class<E> type) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : Enum.valueOf(type, value);
    }

    /** Splits a comma-separated column; an empty or null column gives an empty list. */
    public static List<String> csv(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public static String csv(List<String> values) {
        return values == null ? "" : values.stream().map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }
}
