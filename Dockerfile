# syntax=docker/dockerfile:1

# ---- Build stage (runs on the native build platform; the jar is arch-independent) ----
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
# Resolve dependencies first for better layer caching.
COPY pom.xml .
COPY dupscan-core/pom.xml dupscan-core/pom.xml
COPY dupscan-cli/pom.xml dupscan-cli/pom.xml
RUN mvn -q -e -DskipTests dependency:go-offline || true
COPY . .
RUN mvn -q -DskipTests clean package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
# Run as a non-root user; photo volumes are mounted read-only by the operator.
RUN useradd --system --uid 1000 --create-home dupscan
WORKDIR /app
COPY --from=build /src/dupscan-cli/target/dupscan.jar /app/dupscan.jar
USER 1000:1000
# /data is the writable volume for the index and reports; /photos is mounted read-only.
VOLUME ["/data"]
ENTRYPOINT ["java", "-jar", "/app/dupscan.jar"]
CMD ["--help"]
