# Builds the React user interface and the Spring Boot jar that serves it, then runs the service on a
# JRE-only image as a non-root user. Base images are pinned by digest; Dependabot keeps them up to date.

FROM node:26-alpine@sha256:c6ad9e9c1d80f6eb607c26b4b0a132ff7a2344fad1c78ea3baa2a7d4c8de6d94 AS web
WORKDIR /web
COPY web/package.json web/package-lock.json ./
# --ignore-scripts: the `prepare` hook generates API types from sources not copied yet; `npm run build` does it.
RUN npm ci --no-audit --no-fund --ignore-scripts
COPY web/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-26@sha256:b2c1ad85954592f9928e84327c65201f308ad9b5d8ed7d5b823717c97bf23fbb AS build
WORKDIR /build
# Warm the dependency cache from the manifest alone, so editing sources does not re-download the
# world. Best-effort: the package step below is what must succeed.
COPY pom.xml ./
RUN mvn -B -ntp -DskipTests dependency:go-offline || true
COPY src ./src
COPY --from=web /web/dist ./web/dist
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre-alpine@sha256:56f273059de6b13a6b49fe0ded691ed2d0399bbee76156a8f2d385f7524a3d43
# Apply OS security fixes published after the base image was built, then create the unprivileged
# account the container runs as and the directory of its database.
RUN apk upgrade --no-cache \
    && addgroup -S familyhub \
    && adduser -S -u 10001 -G familyhub familyhub \
    && mkdir /data && chown 10001:familyhub /data
COPY --from=build /build/target/family-hub.jar /app/family-hub.jar
COPY --from=build /build/target/otel/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar
COPY --chmod=755 docker-entrypoint.sh /app/docker-entrypoint.sh
# Version of the image (the CI passes sha-<commit>), reported as service.version in every signal.
ARG VERSION=dev
ENV HOME=/tmp \
    APP_DATA_DIR=/data \
    OTEL_SERVICE_NAME=family-hub \
    OTEL_RESOURCE_ATTRIBUTES=service.namespace=tools,service.version=${VERSION} \
    OTEL_JAVAAGENT_LOGGING=application \
    OTEL_INSTRUMENTATION_MICROMETER_ENABLED=true \
    OTEL_INSTRUMENTATION_LOGBACK_APPENDER_EXPERIMENTAL_CAPTURE_KEY_VALUE_PAIR_ATTRIBUTES=true \
    OTEL_INSTRUMENTATION_COMMON_LOGGING_TRACE_ID=trace.id \
    OTEL_INSTRUMENTATION_COMMON_LOGGING_SPAN_ID=span.id \
    OTEL_INSTRUMENTATION_COMMON_LOGGING_TRACE_FLAGS=trace.flags
WORKDIR /app
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -q --spider http://127.0.0.1:8080/healthz || exit 1
# Telemetry is configured with the standard OTEL_* variables; setting OTEL_EXPORTER_OTLP_ENDPOINT
# attaches the agent, which exports traces, logs and metrics over OTLP.
ENTRYPOINT ["/app/docker-entrypoint.sh"]
