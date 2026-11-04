# App contract

Every directory under `apps/` is an independent application. This document lists what each one must provide so that it can be built, tested, deployed and operated the same way as the others.

## Independence

- An app never imports code, configuration or assets from another app.
- An app has its own dependency manifest and lockfile, its own `Makefile`, its own `Dockerfile` and its own workflow.
- Shared behaviour lives in the reusable workflows under `.github/workflows/reusable-*.yml`, not in shared code. If two apps need the same library, publish it as a package or duplicate it deliberately.

## App kinds

Apps are deliberately heterogeneous: any language or technology is allowed, as long as the app
exposes the same `Makefile` interface and ships its own image and workflow. What an app owes at
runtime depends on its kind, not on its language.

| Kind | What it is | Runtime rows that apply |
|---|---|---|
| Service | Listens for traffic: HTTP or gRPC API, worker consuming a queue. | All of them. |
| User interface | Built assets served by a web server, or a packaged desktop or mobile bundle. | All but metrics, traces and shutdown. |
| Command-line tool | Runs to completion and listens on nothing: CLI, batch, scheduled job. | All but port, health, metrics and traces. Documents its exit codes in its README instead. |

An app's kind is stated in the table of the root [README.md](../README.md).

## Directory layout

```
apps/<app>/
├── README.md          # purpose, usage, configuration, endpoints
├── Makefile           # standard targets, see below
├── Dockerfile         # multi-stage, non-root, pinned base images
├── .dockerignore
├── docker-compose.yml       # optional: run the published image on a Docker host such as a Synology NAS
├── .env.example       # optional: variables read by docker-compose.yml
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
| `smoke` | yes | Starts the hardened image and checks that it works: `/healthz` answers for a user interface or a service, a minimal run succeeds for a command-line tool. CI runs it before scanning the image. |
| `clean` | yes | Removes build output. |

CI only calls `make ci`, so the pipeline and a developer's machine run the same commands.

## Runtime contract

The rows below describe a service; [App kinds](#app-kinds) says which of them a user interface or
a command-line tool is exempt from. Logs, configuration, secrets, user and filesystem apply to
every app whatever its kind.

| Concern | Requirement |
|---|---|
| Port | HTTP on `8080`. |
| Health | `GET /healthz` returns `200` when the process can serve traffic. Services with dependencies also expose `GET /readyz`. |
| Logs | Structured JSON, one event per line, on stdout/stderr. No log files. No secrets or personal data in logs. |
| Metrics | Services expose Prometheus metrics on `GET /metrics` (or `/actuator/prometheus` for Spring Boot). |
| Traces | Services use OpenTelemetry and are configured only through the standard `OTEL_*` environment variables (`OTEL_SERVICE_NAME`, `OTEL_EXPORTER_OTLP_ENDPOINT`, ...). Tracing is disabled when no endpoint is set. |
| Configuration | Environment variables only, documented in the app README with their default. The same image runs in every environment. |
| Secrets | Injected at runtime (environment or mounted file). Never committed, never baked into the image. |
| Shutdown | Handles `SIGTERM` and finishes in-flight requests within 30 seconds. |
| User | Runs as a non-root UID declared with `USER` in the Dockerfile. |
| Filesystem | Works with a read-only root filesystem; writes only to `/tmp`. Docker mounts tmpfs `noexec`: an app that loads a native library from `/tmp` mounts it with `exec`. |

## Stack baselines

### Static web app (Node.js build, NGINX runtime)

Reference implementation: [`apps/my-todo-list`](../apps/my-todo-list).

- Node.js version pinned in `.nvmrc`, dependencies locked in `package-lock.json`.
- Vite for the build, Vitest for tests, ESLint flat config for linting.
- Runtime image `nginxinc/nginx-unprivileged`, JSON access logs, `/healthz`, security headers including a Content Security Policy without `unsafe-inline` scripts.
- `make build BASE_PATH=/sub/path/` builds the site for a sub-path (default `/`) into `dist/`. This is what makes the app deployable on GitHub Pages.

### Java service (Spring Boot, Maven)

Reference implementation: [`apps/family-hub`](../apps/family-hub).

- Maven Wrapper (`mvnw`) committed, Java version pinned in `.java-version` and in `maven.compiler.release`.
- `make test` runs `./mvnw -B verify`; `make audit` runs the OWASP `dependency-check-maven` plugin or an equivalent scanner.
- Spring Boot Actuator: map `/healthz` and `/readyz` to the liveness and readiness groups, expose `health`, `prometheus` and `info` (build information) only.
- Logs in JSON with `logging.structured.format.console=ecs` (Spring Boot 3.4+).
- Traces, logs and metrics through the OpenTelemetry Java agent, attached by the image entrypoint only when `OTEL_EXPORTER_OTLP_ENDPOINT` is set and configured by the `OTEL_*` variables; business metrics registered with Micrometer are bridged to OTLP and also scraped on `/actuator/prometheus`.
- Runtime image based on a JRE-only distroless or Temurin JRE image, non-root. The agent jar is copied next to the application jar (for example by `maven-dependency-plugin`) and excluded from it.
- Add `java-kotlin` to the CodeQL matrix and a `maven` entry to `.github/dependabot.yml`.
- An embedded user interface lives in `web/` with its own `package.json`, `.nvmrc` and the static web app tooling (Vite, Vitest, ESLint). Every `Makefile` target runs the npm step first, then Maven; Maven packages `web/dist` as static content, so one image serves both. The workflow passes `node-dir: web` to `reusable-java.yml`, and Dependabot covers `/apps/*/web`. To be published on GitHub Pages as well, `make pages BASE_PATH=/sub/path/` builds the interface alone into `web/dist`, asking for the server address at first use; the service then lists the Pages origin in its CORS allowlist.
- Persistence that must run without an external service in `make test`: SQLite on a mounted volume, migrated by Flyway, with `/tmp` mounted `exec` for the driver's native library.

### Java command-line tool (Maven)

Reference implementation: [`apps/dupscan`](../apps/dupscan).

- Maven Wrapper (`mvnw`) committed. `.java-version` pins the JDK used by CI and by the image build, and matches the JRE of the runtime image; `maven.compiler.release` sets the bytecode level, the oldest Java the tool supports.
- `make lint` runs Checkstyle against the app's own `checkstyle.xml`; `make test` runs `./mvnw -B verify`.
- `make audit` runs the OSS Index scanner, failing from CVSS 7.0 upwards. Invoke it as `package ossindex:audit` so that a multi-module build resolves its own modules from the reactor.
- Batch tool, so the port, health, metrics and traces rows of the runtime contract do not apply: the README documents the exit codes instead (`0` success, non-zero for findings and errors).
- Writes only to paths the caller passes and to `$HOME`, which the image sets to `/tmp` so the container still works with a read-only root filesystem.
- Runtime image based on a Temurin JRE, non-root, with `ENTRYPOINT` on the jar and `CMD ["--help"]`.
- Verified in CI by `reusable-java.yml`. Add `java-kotlin` to the CodeQL matrix and a `maven` entry to `.github/dependabot.yml`.

### Adding a stack

A language or technology used for the first time is introduced once, then reused. In the same pull
request as the app that needs it:

1. Write its baseline above: toolchain pin, how each `Makefile` target maps to the stack's tools, and the runtime image.
2. Add `.github/workflows/reusable-<stack>.yml` — set up the toolchain from the pin file, then run `make ci` — and list it in the workflow table of [ci-cd.md](ci-cd.md).
3. Add the language to the CodeQL matrix and the package ecosystem to `.github/dependabot.yml`, where each supports it.
4. Add the app, with its kind, to the table of the root [README.md](../README.md).

A stack that CodeQL or Dependabot does not cover is still allowed: record the gap in its baseline so
that it reads as a deliberate choice rather than an oversight.
