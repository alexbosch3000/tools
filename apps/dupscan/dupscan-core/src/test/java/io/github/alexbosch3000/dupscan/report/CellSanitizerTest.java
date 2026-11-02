package io.github.alexbosch3000.dupscan.report;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CellSanitizerTest {

    @Test
    void neutralisesFormulaLeadingCharacters() {
        assertThat(CellSanitizer.sanitize("=HYPERLINK(\"http://x\")")).startsWith("'=");
        assertThat(CellSanitizer.sanitize("+1")).isEqualTo("'+1");
        assertThat(CellSanitizer.sanitize("-2")).isEqualTo("'-2");
        assertThat(CellSanitizer.sanitize("@cmd")).isEqualTo("'@cmd");
    }

    @Test
    void leavesNormalTextUnchanged() {
        assertThat(CellSanitizer.sanitize("IMG_1234.jpg")).isEqualTo("IMG_1234.jpg");
        assertThat(CellSanitizer.sanitize("")).isEqualTo("");
        assertThat(CellSanitizer.sanitize(null)).isNull();
    }
}
