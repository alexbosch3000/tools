package io.github.alexbosch3000.dupscan.cli.report;

import io.github.alexbosch3000.dupscan.index.ImageRecord;
import io.github.alexbosch3000.dupscan.match.DuplicateGroup;
import io.github.alexbosch3000.dupscan.report.ExcelReporter;
import io.github.alexbosch3000.dupscan.report.Reporter;
import io.github.alexbosch3000.dupscan.service.ScanResult;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a concise console summary and a table whose rows are matching pictures, with an
 * "ORIGINAL" and a "DUPLICATE" column (plus group, match type, distance and size). At scale
 * only the top groups by reclaimable space are shown; full detail belongs in the file reports.
 */
public final class StdoutTableReporter implements Reporter {

    private static final int MAX_PATH = 48;

    private final PrintStream out;
    private final int topGroups;
    private final boolean showTable;

    public StdoutTableReporter(PrintStream out, int topGroups, boolean showTable) {
        this.out = out;
        this.topGroups = topGroups;
        this.showTable = showTable;
    }

    @Override
    public void write(ScanResult result) {
        out.println();
        out.println("Scan complete in " + (result.elapsedMillis() / 1000.0) + "s");
        out.println("  Files discovered : " + result.discovered());
        out.println("  Files hashed     : " + result.created());
        out.println("  Reused from cache: " + result.reused());
        out.println("  Skipped          : " + result.skipped());
        if (result.pruned() > 0) {
            out.println("  Pruned (vanished): " + result.pruned());
        }
        out.println("  Duplicate groups : " + result.totalGroups());
        out.println("  Duplicate files  : " + result.totalDuplicateFiles());
        out.println("  Reclaimable space: " + ExcelReporter.humanBytes(result.reclaimableBytes()));

        if (!showTable) {
            return;
        }
        if (result.groups().isEmpty()) {
            out.println();
            out.println("No duplicates found.");
            return;
        }

        int shown = Math.min(topGroups, result.groups().size());
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < shown; i++) {
            DuplicateGroup g = result.groups().get(i);
            ImageRecord original = g.original();
            boolean first = true;
            for (ImageRecord d : g.duplicates()) {
                rows.add(new String[]{
                        first ? Integer.toString(g.id()) : "",
                        first ? g.matchType().name() : "",
                        first ? shorten(original.canonicalPath()) : "",
                        shorten(d.canonicalPath()),
                        Integer.toString(g.hammingOf(d)),
                        ExcelReporter.humanBytes(d.sizeBytes())
                });
                first = false;
            }
        }

        out.println();
        printTable(new String[]{"GROUP", "MATCH", "ORIGINAL", "DUPLICATE", "DIST", "SIZE"}, rows);

        if (result.groups().size() > shown) {
            out.println();
            out.println("... and " + (result.groups().size() - shown)
                    + " more group(s). See the report file for the full list.");
        }
    }

    // Keeps the tail of the path, which carries the file name. ASCII only: the console
    // encoding is not UTF-8 everywhere, and a container usually has no locale set.
    private static String shorten(String path) {
        if (path.length() <= MAX_PATH) {
            return path;
        }
        return "..." + path.substring(path.length() - (MAX_PATH - 3));
    }

    private void printTable(String[] headers, List<String[]> rows) {
        int cols = headers.length;
        int[] width = new int[cols];
        for (int c = 0; c < cols; c++) {
            width[c] = headers[c].length();
        }
        for (String[] row : rows) {
            for (int c = 0; c < cols; c++) {
                width[c] = Math.max(width[c], row[c].length());
            }
        }
        out.println(renderRow(headers, width));
        out.println(renderSeparator(width));
        for (String[] row : rows) {
            out.println(renderRow(row, width));
        }
    }

    private static String renderRow(String[] cells, int[] width) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < cells.length; c++) {
            if (c > 0) {
                sb.append("  ");
            }
            sb.append(pad(cells[c], width[c]));
        }
        return sb.toString().stripTrailing();
    }

    private static String renderSeparator(int[] width) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < width.length; c++) {
            if (c > 0) {
                sb.append("  ");
            }
            sb.append("-".repeat(width[c]));
        }
        return sb.toString();
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        return s + " ".repeat(width - s.length());
    }
}
