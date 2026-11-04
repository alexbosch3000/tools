// Registry of the business metrics, exported both on /actuator/prometheus and over OTLP.
package io.github.alexbosch3000.familyhub.shared;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;

/**
 * Business meters live in Micrometer's global registry: Spring Boot binds its Prometheus registry to
 * it, and the OpenTelemetry agent bridges it to OTLP. A meter registered on the Spring registry alone
 * would only be scraped, never pushed.
 */
public final class Telemetry {

    /** Prefix of every business metric, e.g. familyhub.shopping.trips. */
    public static final String PREFIX = "familyhub.";

    private Telemetry() {
    }

    public static MeterRegistry registry() {
        return Metrics.globalRegistry;
    }
}
