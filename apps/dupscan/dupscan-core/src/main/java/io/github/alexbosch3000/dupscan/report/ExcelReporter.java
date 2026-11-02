package io.github.alexbosch3000.dupscan.report;

import io.github.alexbosch3000.dupscan.index.ImageRecord;
import io.github.alexbosch3000.dupscan.match.DuplicateGroup;
import io.github.alexbosch3000.dupscan.service.ScanResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Writes an .xlsx report using POI's streaming {@link SXSSFWorkbook}, so memory stays bounded
 * regardless of how many duplicates are found. A Summary sheet carries totals; the Duplicates
 * sheet holds the tidy one-row-per-file data (filter/pivot friendly). If the row count would
 * exceed Excel's per-sheet limit, additional sheets are created automatically.
 */
public final class ExcelReporter implements Reporter {

    private static final int EXCEL_MAX_ROWS = 1_048_576;
    private static final int ROW_WINDOW = 200;

    private final Path output;

    public ExcelReporter(Path output) {
        this.output = output;
    }

    @Override
    public void write(ScanResult result) throws IOException {
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        try (SXSSFWorkbook wb = new SXSSFWorkbook(ROW_WINDOW)) {
            CellStyle header = headerStyle(wb);
            writeSummary(wb, header, result);
            writeDuplicates(wb, header, result);
            try (OutputStream os = Files.newOutputStream(output)) {
                wb.write(os);
            } finally {
                wb.dispose(); // remove temp files backing the streamed rows
            }
        }
    }

    private void writeSummary(SXSSFWorkbook wb, CellStyle header, ScanResult r) {
        Sheet s = wb.createSheet("Summary");
        int row = 0;
        row = kv(s, header, row, "Generated", Instant.now().toString(), true);
        row = kv(s, header, row, "Duplicate groups", Long.toString(r.totalGroups()), false);
        row = kv(s, header, row, "Duplicate files", Long.toString(r.totalDuplicateFiles()), false);
        row = kv(s, header, row, "Reclaimable bytes", Long.toString(r.reclaimableBytes()), false);
        row = kv(s, header, row, "Reclaimable (human)", humanBytes(r.reclaimableBytes()), false);
        row = kv(s, header, row, "Files discovered", Long.toString(r.discovered()), false);
        row = kv(s, header, row, "Files hashed", Long.toString(r.created()), false);
        row = kv(s, header, row, "Files reused (cache)", Long.toString(r.reused()), false);
        row = kv(s, header, row, "Files skipped", Long.toString(r.skipped()), false);
        row = kv(s, header, row, "Index rows pruned", Long.toString(r.pruned()), false);
        kv(s, header, row, "Elapsed (ms)", Long.toString(r.elapsedMillis()), false);
        s.setColumnWidth(0, 30 * 256);
        s.setColumnWidth(1, 40 * 256);
    }

    private int kv(Sheet s, CellStyle header, int rowIdx, String key, String value, boolean headerRow) {
        Row r = s.createRow(rowIdx);
        Cell k = r.createCell(0);
        k.setCellValue(key);
        if (headerRow) {
            k.setCellStyle(header);
        }
        r.createCell(1).setCellValue(value);
        return rowIdx + 1;
    }

    private void writeDuplicates(SXSSFWorkbook wb, CellStyle header, ScanResult result) {
        SheetCursor cursor = new SheetCursor(wb, header);
        for (DuplicateGroup g : result.groups()) {
            cursor.addMember(g, g.original(), ReportColumns.ROLE_ORIGINAL);
            for (ImageRecord d : g.duplicates()) {
                cursor.addMember(g, d, ReportColumns.ROLE_DUPLICATE);
            }
        }
    }

    /** Appends rows, rolling over to a new sheet before hitting Excel's per-sheet limit. */
    private static final class SheetCursor {
        private final SXSSFWorkbook wb;
        private final CellStyle header;
        private Sheet sheet;
        private int rowIdx;
        private int sheetSeq;

        SheetCursor(SXSSFWorkbook wb, CellStyle header) {
            this.wb = wb;
            this.header = header;
            newSheet();
        }

        private void newSheet() {
            sheetSeq++;
            sheet = wb.createSheet(sheetSeq == 1 ? "Duplicates" : "Duplicates_" + sheetSeq);
            Row h = sheet.createRow(0);
            for (int c = 0; c < ReportColumns.HEADERS.size(); c++) {
                Cell cell = h.createCell(c);
                cell.setCellValue(ReportColumns.HEADERS.get(c));
                cell.setCellStyle(header);
            }
            rowIdx = 1;
        }

        void addMember(DuplicateGroup g, ImageRecord r, String role) {
            if (rowIdx >= EXCEL_MAX_ROWS) {
                newSheet();
            }
            Row row = sheet.createRow(rowIdx++);
            int c = 0;
            num(row, c++, g.id());
            text(row, c++, role);
            text(row, c++, g.matchType().name());
            num(row, c++, g.hammingOf(r));
            text(row, c++, CellSanitizer.sanitize(r.canonicalPath()));
            text(row, c++, CellSanitizer.sanitize(r.fileName()));
            num(row, c++, r.sizeBytes());
            num(row, c++, r.width());
            num(row, c++, r.height());
            text(row, c++, CellSanitizer.sanitize(r.format()));
            text(row, c++, ReportColumns.formatDate(r.exifMillis()));
            text(row, c++, ReportColumns.formatDate(r.modifiedMillis()));
            text(row, c++, r.contentSha256());
            text(row, c, ReportColumns.formatPerceptualHash(r.perceptualHash()));
        }

        private void num(Row row, int col, long v) {
            row.createCell(col).setCellValue((double) v);
        }

        private void text(Row row, int col, String v) {
            row.createCell(col).setCellValue(v == null ? "" : v);
        }
    }

    private static CellStyle headerStyle(SXSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    public static String humanBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        String[] units = {"KiB", "MiB", "GiB", "TiB", "PiB"};
        double value = bytes;
        int i = -1;
        do {
            value /= 1024.0;
            i++;
        } while (value >= 1024 && i < units.length - 1);
        return String.format("%.2f %s", value, units[i]);
    }
}
