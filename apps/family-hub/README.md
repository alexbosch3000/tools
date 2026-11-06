# family-hub

Family organizer shared by every member of the household: planning at minute precision, to-do lists, shopping lists by shop and aisle (food, DIY, garden, clothing), recipes, family budget with approvals, and a dashboard. One service (Spring Boot, SQLite) serves both the API and the user interface; the same interface is also published on GitHub Pages and connects to that service. Design and rationale: [docs/architecture.md](docs/architecture.md).

| | |
|---|---|
| Kind | [Service](../../docs/app-contract.md#app-kinds): listens on port `8080`, health, metrics and traces |
| Stack | Java 25, Spring Boot 4.1 (Web MVC, Security, JDBC, Actuator), Spring Modulith, Flyway, SQLite, JSpecify, springdoc; TypeScript, React 19, TanStack Query and Router, Vite, Tailwind CSS 4 |
| Runtime | Temurin JRE, non-root UID `10001`, database in `/data` |
| Image | `ghcr.io/alexbosch3000/tools/family-hub` |
| Live UI | https://alexbosch3000.github.io/tools/ (asks for the address of your server) |
| CI/CD | [`.github/workflows/app-family-hub.yml`](../../.github/workflows/app-family-hub.yml) |

## Features

| Module | What the family can do |
|---|---|
| Planning | Week and month views, entries at the minute, all-day and multi-day entries, recurrence (daily, weekly on chosen days, monthly, yearly, every *n*), skipped occurrences, attendees, French public holidays, iCalendar subscription for phones |
| Entry types | `EVENT` (holidays, day camp, taxes, sport, health…, with an estimated cost), `SHOPPING` (linked to a list), `MEAL` (recipe and servings), `ACCOUNTING` (month report or approval session) |
| To-do | Lists, assignee, due date-time, priority, repeating chores that recreate themselves when done |
| Shopping | Lists grouped by store type (supermarket, DIY, garden, clothing…) and aisle, manual items and recipe ingredients added up per item (ingredients already at home can be unchecked when adding a recipe), list renaming, rows editable (quantity, unit and price actually bought, back to the computed value on demand), clothes and shoes bought for a member with their size, estimated cost, recipes cookable with the list, completion with the amount paid in each kind of shop |
| Recipes | Ingredients from the catalog, scaling to any number of servings, add to a list, plan as a meal |
| Catalog | Goods and ingredients with store type, aisle, unit and estimated price; a starter catalog is provided |
| Budget (parents) | Accounts and balances, envelopes per category grouped by the 50/30/20 rule, transactions planned, pending and approved, recurring bills and income, month and year reports |
| Dashboard | Next seven days, load per member, overdue to-dos, open lists, planned meals, budget of the month |

Links between modules:

| From | To | Effect |
|---|---|---|
| Planned meals | Shopping | "Courses des repas" fills a list with the week's recipes, scaled |
| Event with a cost | Budget | Planned expense in the matching category, kept in sync with the entry |
| Completed shopping list | Budget, planning | One expense to approve per kind of shop, in its category (groceries, DIY, garden, clothing…); the linked shopping entry is marked done |
| Shopping list | Recipes | Recipes ranked by the share of their ingredients already on the list |
| Recurring bills | Budget | Planned transactions for the current and the next month |

## Usage

Prerequisites: JDK 25 and Node.js 26 (`.java-version`, `web/.nvmrc`), `make`, Docker for the image.

```bash
make install     # npm ci in web/, Maven dependencies
make run         # build everything and start on http://localhost:8080 (database in ./data)
make dev-web     # with `make run` started: UI with hot reload on http://localhost:5173
make ci          # lint, test, audit, build: same checks as CI
make api         # after an API change: rewrite the OpenAPI contract, regenerate the TypeScript types
make pages       # build the UI alone for GitHub Pages (BASE_PATH=/tools/), asking for the server address
make observe     # run the image with Grafana, Tempo, Loki and Prometheus on http://localhost:3000
make image       # build the container image family-hub:local
make image-run   # run the image on http://localhost:8080 with production hardening
make smoke       # start the hardened image and check /readyz and /healthz
```

The first visit asks for the first parent's name, login and password. Do this before exposing the service: until then, anyone reaching it can create that account. Parents then add the other members in *Famille*; a member without login only appears as an assignee.

## Configuration

| Variable | Default | Description |
|---|---|---|
| `APP_DATA_DIR` | `./data` (`/data` in the image) | Directory of the SQLite database `family-hub.db`. Must be writable. |
| `APP_TIME_ZONE` | `Europe/Paris` | Time zone of the family. Planning times are wall-clock times in this zone. |
| `APP_ALLOWED_ORIGINS` | empty | Origins allowed to call the API from a browser (CORS), comma-separated, for example `https://alexbosch3000.github.io` to use the interface published on GitHub Pages. Empty: only the interface served by the service itself. |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | unset | OTLP/HTTP endpoint of an OpenTelemetry collector, for example `http://collector:4318`. When set, the image attaches the OpenTelemetry Java agent, which exports traces, logs and metrics. |
| `OTEL_JAVAAGENT_ENABLED` | `true` | `false` keeps the agent detached even when an endpoint is set. |
| `OTEL_SERVICE_NAME`, `OTEL_RESOURCE_ATTRIBUTES`, `OTEL_EXPORTER_OTLP_HEADERS`, … | set by the image | Standard OpenTelemetry settings. The image sets `service.name=family-hub`, `service.namespace=tools` and `service.version` to the image tag (`sha-<commit>`). |

`docker-compose.yml` also reads `IMAGE_TAG`, `HOST_PORT` (default `8081`), `PUID`, `PGID`, `DATA_DIR` and `MEM_LIMIT`; see [`.env.example`](.env.example).

### Interface on GitHub Pages

The `app-family-hub` workflow publishes the interface on GitHub Pages (`make pages`). At first use it asks for the address of the family's server and keeps it in the browser, with the access token. The server must:

- be reached over HTTPS (an HTTPS page cannot call an HTTP server), for example through the NAS reverse proxy;
- list `https://alexbosch3000.github.io` in `APP_ALLOWED_ORIGINS`.

The Pages build carries a Content Security Policy that only allows calls to HTTPS servers. Sessions use a Bearer token rather than a cookie: browsers block third-party cookies, and a token in a header is not sent automatically, so no CSRF protection is needed.

## Deploy on a Synology NAS

[`docker-compose.yml`](docker-compose.yml) runs the published image with a read-only filesystem, no capabilities and log rotation, and keeps the database in `DATA_DIR`. Follow [docs/synology.md](../../docs/synology.md#family-hub).

## Operations

| Endpoint | Purpose |
|---|---|
| `GET /healthz` | Liveness probe. |
| `GET /readyz` | Readiness probe: database and free disk space in `APP_DATA_DIR` (50 MB at least). |
| `GET /actuator/prometheus` | Prometheus metrics, including the business metrics below. |
| `GET /actuator/info` | Version, build time and Java runtime. |
| `GET /api/admin/backup` | Parents only: consistent copy of the database (`VACUUM INTO`), taken while the service runs. |
| `GET /api/planning/feed/{token}.ics` | iCalendar feed; the token is created in *Famille* and can be revoked. |
| `POST /api/auth/login` | Returns an access token for `Authorization: Bearer <token>`, valid 30 days after its last use. Five failures for a username within 15 minutes answer `429` with `Retry-After`. |
| `/api/**` | JSON API used by the UI, Bearer token required. Contract: [`web/src/api/openapi.json`](web/src/api/openapi.json) (OpenAPI 3.1), checked by `OpenApiContractTest`. |

- Logs: ECS JSON on stdout, with `trace.id` and `span.id` when the agent is attached; key-value pairs (`member.id`, …) become log attributes in OTLP. No passwords, tokens or amounts are logged. Errors are answered as RFC 9457 problem details with a French message.
- Tokens are stored hashed (SHA-256); changing a password revokes every token of the member, logging out revokes the token of the device.
- The container runs as UID `10001` with a read-only root filesystem. `/tmp` must allow `exec`: the SQLite driver loads its native library from there.
- Shutdown is graceful (30 seconds). The database is migrated by Flyway at startup.
- Backup: download `/api/admin/backup` from the UI (*Famille*), or copy `DATA_DIR` while the container is stopped. Do not copy `family-hub.db` alone while it runs: recent changes may still be in `family-hub.db-wal`.

## Observability

With `OTEL_EXPORTER_OTLP_ENDPOINT` set, the agent exports:

| Signal | Content |
|---|---|
| Traces | HTTP requests, JDBC queries, and business spans (`ShoppingService.complete`, `AccountingService.approve`, …) carrying ids such as `familyhub.shopping.list_id` |
| Logs | Application logs correlated with their trace |
| Metrics | HTTP server, JVM and connection pool metrics, and the business metrics below |

| Metric | Type | Meaning |
|---|---|---|
| `familyhub.auth.logins{outcome}` | counter | Logins: `success`, `failure`, `throttled` |
| `familyhub.planning.entries.created{entry_type}` | counter | Planning entries created, per type |
| `familyhub.shopping.lists.open` | gauge | Shopping lists not completed yet |
| `familyhub.shopping.trips` | counter | Shopping lists completed |
| `familyhub.shopping.spent{store_type}` | summary (euros) | Amounts paid per kind of shop |
| `familyhub.todo.items.completed`, `familyhub.todo.items.overdue` | counter, gauge | To-do throughput and backlog |
| `familyhub.accounting.transactions.approved`, `familyhub.accounting.approvals.pending` | counter, gauge | Budget approvals |

`make observe` starts the image next to `grafana/otel-lgtm` (Grafana on http://localhost:3000 with Tempo, Loki and Prometheus) and provisions the [family-hub dashboard](observability/grafana/family-hub.json). [`observability/prometheus/alerts.yml`](observability/prometheus/alerts.yml) holds alert rules for a Prometheus or Mimir ruler: service down, error rate, latency, password guessing, disk almost full, approvals piling up.

## Data

| Table | Content |
|---|---|
| `member` | Members, roles, clothing and shoe sizes, BCrypt password hashes, hashed calendar tokens |
| `auth_token` | Hashed access tokens, last use |
| `item` | Catalog |
| `recipe`, `recipe_ingredient` | Recipes |
| `shopping_list`, `shopping_list_recipe`, `shopping_line`, `shopping_purchase` | Shopping lists, and what is actually bought for a row when it differs from what is needed |
| `planning_entry`, `planning_entry_member`, `planning_exception` | Planning |
| `todo_list`, `todo_item` | To-do lists |
| `account`, `budget_category`, `ledger_transaction`, `recurring_transaction` | Budget |

Schema changes are new Flyway migrations under `src/main/resources/db/migration`; never edit an applied one.

## Layout

```
src/main/java/.../familyhub/
├── <module>/            # family, catalog, recipes, shopping, planning, todo, accounting, dashboard, admin
│   ├── *.java           # public API of the module: service, records, events
│   └── internal/        # controllers, repositories, metrics, pure logic (not visible to other modules)
└── shared/              # errors, SQL conversions, clock, telemetry, web and OpenAPI configuration
src/main/resources/db/migration/   # Flyway SQL migrations, one per module
src/test/java/                     # unit tests per module, HTTP flows (CrossModuleFlowsTest), ModularityTest, OpenApiContractTest
observability/                     # local Grafana stack, dashboard and alert rules
web/src/
├── api/                 # OpenAPI contract, typed client (schema.d.ts is generated, not committed)
├── app/                 # session, routes, server-state cache, notifications
├── components/          # UI building blocks and charts
├── features/<module>/   # pages and dialogs of each module
└── lib/                 # dates, formatting, calendar layout, labels (pure, covered by *.test.ts)
```
