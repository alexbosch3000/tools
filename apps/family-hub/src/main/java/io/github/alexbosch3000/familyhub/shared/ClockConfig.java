// Provides the clock every module reads "now" and "today" from, so tests can fix time.
package io.github.alexbosch3000.familyhub.shared;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ClockConfig {

    @Bean
    Clock clock(AppProperties properties) {
        return Clock.system(properties.timeZone());
    }
}
