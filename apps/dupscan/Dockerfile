# Builds the runnable jar, then runs the CLI on a JRE-only image as a non-root user.
# Base images are pinned by digest; Dependabot keeps them up to date.

FROM maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 AS build
WORKDIR /build
# Warm the dependency cache from the manifests alone, so editing sources does not
# re-download the world. Best-effort: the package step below is what must succeed.
COPY pom.xml ./
COPY dupscan-core/pom.xml dupscan-core/
COPY dupscan-cli/pom.xml dupscan-cli/
RUN mvn -B -ntp -DskipTests dependency:go-offline || true
COPY . .
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine@sha256:82cf3d9ccbe1ed46252008d51b2f2329e6b9e8a2f4394c4bebefc3ea7f4b1372
# Apply OS security fixes published after the base image was built, then create the
# unprivileged account the container runs as.
RUN apk upgrade --no-cache \
    && addgroup -S dupscan \
    && adduser -S -u 10001 -G dupscan dupscan
COPY --from=build /build/dupscan-cli/target/dupscan.jar /app/dupscan.jar
# Keep the default index path (~/.dupscan) on tmpfs so the image works with a
# read-only root filesystem; mount a volume and pass --index for a persistent index.
ENV HOME=/tmp
WORKDIR /data
USER 10001
ENTRYPOINT ["java", "-jar", "/app/dupscan.jar"]
CMD ["--help"]
