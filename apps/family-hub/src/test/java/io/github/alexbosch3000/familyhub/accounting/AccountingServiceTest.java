// Category code generation from a name.
package io.github.alexbosch3000.familyhub.accounting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccountingServiceTest {

    @Test
    void derivesAnAsciiCodeFromTheName() {
        assertThat(AccountingService.slug("Cantine & goûters")).isEqualTo("cantine-gouters");
        assertThat(AccountingService.slug("  Énergie (gaz)  ")).isEqualTo("energie-gaz");
        assertThat(AccountingService.slug("!!!")).isEqualTo("categorie");
    }
}
