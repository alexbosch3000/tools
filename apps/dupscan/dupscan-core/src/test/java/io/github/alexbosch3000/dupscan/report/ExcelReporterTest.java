package io.github.alexbosch3000.dupscan.report;

import io.github.alexbosch3000.dupscan.index.ImageRecord;
import io.github.alexbosch3000.dupscan.match.DuplicateGroup;
import io.github.alexbosch3000.dupscan.match.MatchType;
import io.github.alexbosch3000.dupscan.service.ScanResult;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelReporterTest {

    @Test
    void writesReadableWorkbookWithSummaryAndDuplicates(@TempDir Path tmp) throws IOException {
        ImageRecord original = new ImageRecord("/photos/a.jpg", 2000, 1000, "sha-a",
                1L, 100, 100, "jpeg", null);
        ImageRecord dup = new ImageRecord("/photos/=evil.jpg", 1000, 2000, "sha-b",
                3L, 80, 80, "png", null);
        DuplicateGroup group = new DuplicateGroup(1, MatchType.NEAR, original,
                List.of(dup), Map.of(original.canonicalPath(), 0, dup.canonicalPath(), 2));
        ScanResult result = new ScanResult(List.of(group), 2, 2, 0, 0, 0, 123);

        Path xlsx = tmp.resolve("out.xlsx");
        new ExcelReporter(xlsx).write(result);

        assertThat(Files.exists(xlsx)).isTrue();
        try (InputStream in = Files.newInputStream(xlsx);
             Workbook wb = new XSSFWorkbook(in)) {
            assertThat(wb.getSheet("Summary")).isNotNull();
            Sheet dups = wb.getSheet("Duplicates");
            assertThat(dups).isNotNull();
            assertThat(dups.getRow(0).getCell(0).getStringCellValue()).isEqualTo("group_id");
            // Header + original + duplicate
            assertThat(dups.getLastRowNum()).isEqualTo(2);
            // Formula-injection guard: the duplicate's file_name cell must be neutralised.
            String fileNameCell = dups.getRow(2).getCell(5).getStringCellValue();
            assertThat(fileNameCell).startsWith("'=");
        }
    }

    @Test
    void humanBytesFormatsReadably() {
        assertThat(ExcelReporter.humanBytes(512)).isEqualTo("512 B");
        assertThat(ExcelReporter.humanBytes(1536)).isEqualTo("1.50 KiB");
    }
}
