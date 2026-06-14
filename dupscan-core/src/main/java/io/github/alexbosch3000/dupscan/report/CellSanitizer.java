package io.github.alexbosch3000.dupscan.report;

/**
 * Neutralises spreadsheet formula injection. Filenames and paths are attacker-influenced
 * data; a cell beginning with {@code = + - @} (or a control char that some apps treat as a
 * formula lead-in) is prefixed with an apostrophe so Excel/Sheets/LibreOffice render it as
 * literal text instead of evaluating it.
 */
public final class CellSanitizer {

    private CellSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        char c = value.charAt(0);
        if (c == '=' || c == '+' || c == '-' || c == '@'
                || c == '\t' || c == '\r' || c == '\n') {
            return "'" + value;
        }
        return value;
    }
}
