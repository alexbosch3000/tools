// Authentication metrics: login attempts by outcome, to spot password guessing.
package io.github.alexbosch3000.familyhub.family.internal;

import io.github.alexbosch3000.familyhub.shared.Telemetry;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Component;

@Component
class AuthMetrics {

    enum Outcome { SUCCESS, FAILURE, THROTTLED }

    void login(Outcome outcome) {
        Counter.builder(Telemetry.PREFIX + "auth.logins").description("Login attempts")
                .tag("outcome", outcome.name().toLowerCase()).register(Telemetry.registry()).increment();
    }
}
