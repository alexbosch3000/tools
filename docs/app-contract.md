# App contract

Every directory under `apps/` is an independent application. This document lists what each one must provide so that it can be built, tested, deployed and operated the same way as the others.

## Independence

- An app never imports code, configuration or assets from another app.
- An app has its own dependency manifest and lockfile, its own `Makefile`, its own `Dockerfile` and its own workflow.
- Shared behaviour lives in the reusable workflows under `.github/workflows/reusable-*.yml`, not in shared code. If two apps need the same library, publish it as a package or duplicate it deliberately.

## Directory layout

```
apps/<app>/
├── README.md          # purpose, usage, configuration, endpoints
├── Makefile           # standard targets, see below
├── Dockerfile         # multi-stage, non-root, pinned base images
├── .dockerignore
├── <manifest>         # package.json + package-lock.json, pom.xml, ...
├── <toolchain pin>    # .nvmrc, .java-version, ...
└── src/               # code and tests, following the stack's conventions
```

`<app>` is lowercase kebab-case, short, without a type suffix (`my-todo-list`, not `my-todo-list-app`). The same name is used for the directory, the image (`ghcr.io/<owner>/<repo>/<app>`) and the workflow (`app-<app>.yml`).

## Makefile targets

| Target | Required | Behaviour |
|---|---|---|
| `help` | yes | Default goal; lists targets. |
| `install` | yes | Installs dependencies exactly as locked (`npm ci`, `mvn -B dependency:go-offline`, ...). |
| `lint` | yes | Static analysis and formatting checks. Fails on findings. |
| `test` | yes | Unit tests, no network or external service required. |
| `audit` | yes | Fails on known high or critical vulnerabilities in dependencies. |
| `build` | yes | Produces the deployable artifact. |
| `ci` | yes | `install lint test audit build`: exactly what CI runs. |
| `run` | yes | Starts the app locally for development. |
| `image` | yes | Builds the container image `<app>:local`. |
| `image-run` | yes | Runs the image with a read-only root filesystem and all capabilities dropped. |
| `clean` | yes | Removes build output. |

CI only calls `make ci`, so the pipeline and a developer's machine run the same commands.

## Runtime contract

The table below describes a **service**. Two other kinds of app exist and are held to the same
rules except where noted:

- **Static site**: built assets served by a web server. The metrics, traces and shutdown rows do not apply.
- **Batch tool**: runs to completion and listens on nothing (a command-line tool, a scheduled job). The port, health, metrics and traces rows do not apply; it documents its exit codes in its README instead. Logs, configuration, secrets, user and filesystem rows still apply.

| Concern | Requirement |
|---|---|
| Port | HTTP on `8080`. |
| Health | `GET /healthz` returns `200` when the process can serve traffic. Services with dependencies also expose `GET /readyz`. |
| Logs | Structured JSON, one event per line, on stdout/stderr. No log files. No secrets or personal data in logs. |
| Metrics | Services expose Prometheus metrics on `GET /metrics` (or `/actuator/prometheus` for Spring Boot). Static sites are exempt. |
| Traces | Services use OpenTelemetry and are configured only through the standard `OTEL_*` environment variables (`OTEL_SERVICE_NAME`, `OTEL_EXPORTER_OTLP_ENDPOINT`, ...). Tracing is disabled when no endpoint is set. |
| Configuration | Environment variables only, documented in the app README with their default. The same image runs in every environment. |
| Secrets | Injected at runtime (environment or mounted file). Never committed, never baked into the image. |
| Shutdown | Handles `SIGTERM` and finishes in-flight requests within 30 seconds. |
| User | Runs as a non-root UID declared with `USER` in the Dockerfile. |
| Filesystem | Works with a read-only root filesystem; writes only to `/tmp`. |

## Stack baselines

### Static web app (Node.js build, NGINX runtime)

Reference implementation: [`apps/my-todo-list`](../apps/my-todo-list).

- Node.js version pinned in `.nvmrc`, dependencies locked in `package-lock.json`.
- Vite for the build, Vitest for tests, ESLint flat config for linting.
- Runtime image `nginxinc/nginx-unprivileged`, JSON access logs, `/healthz`, security headers including a Content Security Policy without `unsafe-inline` scripts.
- `make build BASE_PATH=/sub/path/` builds the site for a sub-path (default `/`) into `dist/`. This is what makes the app deployable on GitHub Pages.

### Java service (Spring Boot, Maven)

- Maven Wrapper (`mvnw`) committed, Java version pinned in `.java-version` and in `maven.compiler.release`.
- `make test` runs `./mvnw -B verify`; `make audit` runs the OWASP `dependency-check-maven` plugin or an equivalent scanner.
- Spring Boot Actuator: map `/healthz` and `/readyz` to the liveness and readiness groups, expose `prometheus` and `health` only.
- Logs in JSON with `logging.structured.format.console=ecs` (Spring Boot 3.4+).
- Traces through the OpenTelemetry Java agent, enabled by `JAVA_TOOL_OPTIONS=-javaagent:/otel/opentelemetry-javaagent.jar` and the `OTEL_*` variables.
- Runtime image based on a JRE-only distroless or Temurin JRE image, non-root.
- Add `java-kotlin` to the CodeQL matrix and a `maven` entry to `.github/dependabot.yml`.

### Java command-line tool (Maven)

Reference implementation: [`apps/dupscan`](../apps/dupscan).

- Maven Wrapper (`mvnw`) committed, Java version pinned in `.java-version` and in `maven.compiler.release`.
- `make lint` runs Checkstyle against the app's own `checkstyle.xml`; `make test` runs `./mvnw -B verify`.
- `make audit` runs the OSS Index scanner, failing from CVSS 7.0 upwards. Invoke it as `package ossindex:audit` so that a multi-module build resolves its own modules from the reactor.
- Batch tool, so the port, health, metrics and traces rows of the runtime contract do not apply: the README documents the exit codes instead (`0` success, non-zero for findings and errors).
- Writes only to paths the caller passes and to `$HOME`, which the image sets to `/tmp` so the container still works with a read-only root filesystem.
- Runtime image based on a Temurin JRE, non-root, with `ENTRYPOINT` on the jar and `CMD ["--help"]`.
- Verified in CI by `reusable-java.yml`. Add `java-kotlin` to the CodeQL matrix and a `maven` entry to `.github/dependabot.yml`.
