# family-hub

Family organizer shared by every member of the household: planning at minute precision, to-do lists, shopping lists by shop and aisle, recipes, family budget with approvals, and a dashboard. One service (Spring Boot, SQLite) serves both the API and the React user interface. Design and rationale: [docs/architecture.md](docs/architecture.md).

| | |
|---|---|
| Kind | [Service](../../docs/app-contract.md#app-kinds): listens on port `8080`, health, metrics and traces |
| Stack | Java 25, Spring Boot 4.1 (Web MVC, Security, JDBC, Actuator), Flyway, SQLite; React 19, Vite, Tailwind CSS 3 |
| Runtime | Temurin JRE, non-root UID `10001`, database in `/data` |
| Image | `ghcr.io/alexbosch3000/tools/family-hub` |
| CI/CD | [`.github/workflows/app-family-hub.yml`](../../.github/workflows/app-family-hub.yml) |

## Features

| Module | What the family can do |
|---|---|
| Planning | Week and month views, entries at the minute, all-day and multi-day entries, recurrence (daily, weekly on chosen days, monthly, yearly, every *n*), skipped occurrences, attendees, French public holidays, iCalendar subscription for phones |
| Entry types | `EVENT` (holidays, day camp, taxes, sport, health…, with an estimated cost), `SHOPPING` (linked to a list), `MEAL` (recipe and servings), `ACCOUNTING` (month report or approval session) |
| To-do | Lists, assignee, due date-time, priority, repeating chores that recreate themselves when done |
| Shopping | Lists grouped by store type and aisle, manual items and recipe ingredients added up per item, estimated cost, recipes cookable with the list, completion with the amount paid |
| Recipes | Ingredients from the catalog, scaling to any number of servings, add to a list, plan as a meal |
| Catalog | Goods and ingredients with store type, aisle, unit and estimated price; a starter catalog is provided |
| Budget (parents) | Accounts and balances, envelopes per category grouped by the 50/30/20 rule, transactions planned, pending and approved, recurring bills and income, month and year reports |
| Dashboard | Next seven days, load per member, overdue to-dos, open lists, planned meals, budget of the month |

Links between modules:

| From | To | Effect |
|---|---|---|
| Planned meals | Shopping | "Courses des repas" fills a list with the week's recipes, scaled |
| Event with a cost | Budget | Planned expense in the matching category, kept in sync with the entry |
| Completed shopping list | Budget, planning | Expense to approve; the linked shopping entry is marked done |
| Shopping list | Recipes | Recipes ranked by the share of their ingredients already on the list |
| Recurring bills | Budget | Planned transactions for the current and the next month |

## Usage

Prerequisites: JDK 25 and Node.js 26 (`.java-version`, `web/.nvmrc`), `make`, Docker for the image.

```bash
make install     # npm ci in web/, Maven dependencies
make run         # build everything and start on http://localhost:8080 (database in ./data)
make dev-web     # with `make run` started: UI with hot reload on http://localhost:5173
make ci          # lint, test, audit, build: same checks as CI
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
| `APP_COOKIE_SECURE` | `false` | Set to `true` when the service is reached over HTTPS, so the session cookie is never sent in clear. |
| `JAVA_TOOL_OPTIONS` | unset | Set to `-javaagent:/otel/opentelemetry-javaagent.jar` to enable tracing. |
| `OTEL_SERVICE_NAME`, `OTEL_EXPORTER_OTLP_ENDPOINT`, … | unset | Standard OpenTelemetry settings, read by the agent. |

`docker-compose.yml` also reads `IMAGE_TAG`, `HOST_PORT` (default `8081`), `PUID`, `PGID`, `DATA_DIR` and `MEM_LIMIT`; see [`.env.example`](.env.example).

## Deploy on a Synology NAS

[`docker-compose.yml`](docker-compose.yml) runs the published image with a read-only filesystem, no capabilities and log rotation, and keeps the database in `DATA_DIR`. Follow [docs/synology.md](../../docs/synology.md#family-hub).

## Operations

| Endpoint | Purpose |
|---|---|
| `GET /healthz` | Liveness probe. |
| `GET /readyz` | Readiness probe, includes the database. |
| `GET /actuator/prometheus` | Prometheus metrics. |
| `GET /api/admin/backup` | Parents only: consistent copy of the database (`VACUUM INTO`), taken while the service runs. |
| `GET /api/planning/feed/{token}.ics` | iCalendar feed; the token is created in *Famille* and can be revoked. |
| `/api/**` | JSON API used by the UI, session cookie and CSRF header (`X-XSRF-TOKEN`) required. |

- Logs: ECS JSON on stdout. Errors are answered as RFC 9457 problem details with a French message.
- The container runs as UID `10001` with a read-only root filesystem. `/tmp` must allow `exec`: the SQLite driver loads its native library from there.
- Shutdown is graceful (30 seconds). The database is migrated by Flyway at startup.
- Backup: download `/api/admin/backup` from the UI (*Famille*), or copy `DATA_DIR` while the container is stopped. Do not copy `family-hub.db` alone while it runs: recent changes may still be in `family-hub.db-wal`.

## Data

| Table | Content |
|---|---|
| `member` | Members, roles, BCrypt password hashes, hashed calendar tokens |
| `item` | Catalog |
| `recipe`, `recipe_ingredient` | Recipes |
| `shopping_list`, `shopping_list_recipe`, `shopping_line` | Shopping lists |
| `planning_entry`, `planning_entry_member`, `planning_exception` | Planning |
| `todo_list`, `todo_item` | To-do lists |
| `account`, `budget_category`, `ledger_transaction`, `recurring_transaction` | Budget |

Schema changes are new Flyway migrations under `src/main/resources/db/migration`; never edit an applied one.

## Layout

```
src/main/java/.../familyhub/
├── <module>/            # family, catalog, recipes, shopping, planning, todo, accounting, dashboard, admin
│   ├── *.java           # public API of the module: service, records, events
│   └── internal/        # controllers, repositories, pure logic (not visible to other modules)
└── shared/              # errors, SQL conversions, clock, web configuration
src/main/resources/db/migration/   # Flyway SQL migrations, one per module
src/test/java/                     # unit tests per module, HTTP flows (CrossModuleFlowsTest), ModularityTest
web/src/
├── components/          # UI building blocks and charts
├── lib/                 # API client, routing, dates, formatting, calendar layout (covered by *.test.js)
└── pages/               # one page or tab per file
```
