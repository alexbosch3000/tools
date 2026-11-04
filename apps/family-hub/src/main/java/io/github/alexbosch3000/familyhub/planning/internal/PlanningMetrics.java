// Planning metrics: entries created per type.
package io.github.alexbosch3000.familyhub.planning.internal;

import io.github.alexbosch3000.familyhub.planning.EntryType;
import io.github.alexbosch3000.familyhub.shared.Telemetry;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Component;

@Component
public class PlanningMetrics {

    public void created(EntryType type) {
        Counter.builder(Telemetry.PREFIX + "planning.entries.created").description("Planning entries created")
                .tag("entry_type", type.name()).register(Telemetry.registry()).increment();
    }
}
