package io.github.alexbosch3000.dupscan.report;

import io.github.alexbosch3000.dupscan.index.ImageRecord;
import io.github.alexbosch3000.dupscan.match.DuplicateGroup;
import io.github.alexbosch3000.dupscan.service.ScanResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the tidy one-row-per-file report as RFC 4180 CSV (UTF-8 with BOM so Excel detects
 * the encoding). Cheap and unbounded in row count, so it is the recommended format at
 * extreme scale. Text fields pass through {@link CellSanitizer}.
 */
public final class CsvReporter implements Reporter {

    private final Path output;

    public CsvReporter(Path output) {
        this.output = output;
    }

    @Override
    public void write(ScanResult result) throws IOException {
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        try (Writer w = new BufferedWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            w.write('﻿'); // UTF-8 BOM for Excel
            writeRow(w, ReportColumns.HEADERS.toArray(new String[0]));
            for (DuplicateGroup g : result.groups()) {
                writeMember(w, g, g.original(), ReportColumns.ROLE_ORIGINAL);
                for (ImageRecord d : g.duplicates()) {
                    writeMember(w, g, d, ReportColumns.ROLE_DUPLICATE);
                }
            }
        }
    }

    private void writeMember(Writer w, DuplicateGroup g, ImageRecord r, String role) throws IOException {
        writeRow(w, new String[]{
                Integer.toString(g.id()),
                role,
                g.matchType().name(),
                Integer.toString(g.hammingOf(r)),
                CellSanitizer.sanitize(r.canonicalPath()),
                CellSanitizer.sanitize(r.fileName()),
                Long.toString(r.sizeBytes()),
                Integer.toString(r.width()),
                Integer.toString(r.height()),
                CellSanitizer.sanitize(r.format()),
                ReportColumns.formatDate(r.exifMillis()),
                ReportColumns.formatDate(r.modifiedMillis()),
                r.contentSha256(),
                ReportColumns.formatPerceptualHash(r.perceptualHash())
        });
    }

    private void writeRow(Writer w, String[] cells) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(quote(cells[i]));
        }
        sb.append("\r\n");
        w.write(sb.toString());
    }

    private static String quote(String value) {
        String v = value == null ? "" : value;
        boolean needsQuote = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        if (!needsQuote) {
            return v;
        }
        return '"' + v.replace("\"", "\"\"") + '"';
    }
}
