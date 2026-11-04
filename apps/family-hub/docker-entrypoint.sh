#!/bin/sh
# Starts the service. The OpenTelemetry agent is attached only when an OTLP endpoint is configured,
# so that a NAS without a telemetry backend runs without it.
set -eu

if [ -n "${OTEL_EXPORTER_OTLP_ENDPOINT:-}${OTEL_EXPORTER_OTLP_TRACES_ENDPOINT:-}" ] \
    && [ "${OTEL_JAVAAGENT_ENABLED:-true}" != "false" ]; then
  set -- -javaagent:/otel/opentelemetry-javaagent.jar "$@"
fi

# The SQLite driver loads a native library, which Java 25 asks to allow explicitly.
exec java -XX:MaxRAMPercentage=75 --enable-native-access=ALL-UNNAMED "$@" -jar /app/family-hub.jar
