// Verifies the module boundaries: no dependency cycle, no access to another module's internal package.
package io.github.alexbosch3000.familyhub;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    @Test
    void modulesRespectTheirBoundaries() {
        ApplicationModules.of(FamilyHubApplication.class).verify();
    }
}
