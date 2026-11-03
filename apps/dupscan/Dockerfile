# Builds the runnable jar, then runs the CLI on a JRE-only image as a non-root user.
# Base images are pinned by digest; Dependabot keeps them up to date.

FROM maven:3.9-eclipse-temurin-26@sha256:b2c1ad85954592f9928e84327c65201f308ad9b5d8ed7d5b823717c97bf23fbb AS build
WORKDIR /build
# Warm the dependency cache from the manifests alone, so editing sources does not
# re-download the world. Best-effort: the package step below is what must succeed.
COPY pom.xml ./
COPY dupscan-core/pom.xml dupscan-core/
COPY dupscan-cli/pom.xml dupscan-cli/
RUN mvn -B -ntp -DskipTests dependency:go-offline || true
COPY . .
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre-alpine@sha256:56f273059de6b13a6b49fe0ded691ed2d0399bbee76156a8f2d385f7524a3d43
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
