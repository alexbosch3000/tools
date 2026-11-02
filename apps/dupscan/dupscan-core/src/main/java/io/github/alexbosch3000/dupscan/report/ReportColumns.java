package io.github.alexbosch3000.dupscan.report;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Shared column definitions and value formatting for the tidy (one-row-per-file) reports. */
final class ReportColumns {

    private ReportColumns() {
    }

    static final List<String> HEADERS = List.of(
            "group_id", "role", "match_type", "distance",
            "path", "file_name", "size_bytes", "width", "height", "format",
            "exif_date", "modified_date", "content_sha256", "perceptual_hash");

    static final String ROLE_ORIGINAL = "original";
    static final String ROLE_DUPLICATE = "duplicate";

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    static String formatDate(Long millis) {
        return millis == null ? "" : TS.format(Instant.ofEpochMilli(millis));
    }

    static String formatPerceptualHash(Long hash) {
        return hash == null ? "" : String.format("%016x", hash);
    }
}
