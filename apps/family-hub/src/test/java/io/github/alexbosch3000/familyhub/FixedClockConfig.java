// Fixes the application clock for HTTP-level tests: Tuesday 3 November 2026, 10:00 in Paris.
package io.github.alexbosch3000.familyhub;

import java.time.Clock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class FixedClockConfig {

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(IntegrationTest.NOW.atZone(IntegrationTest.ZONE).toInstant(), IntegrationTest.ZONE);
    }
}
