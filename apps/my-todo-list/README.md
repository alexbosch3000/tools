# my-todo-list

Personal task manager with list and calendar views. Static single-page application (React, Vite, Tailwind CSS); tasks are stored in the browser's `localStorage`, no backend.

| | |
|---|---|
| Stack | Node.js 24, React 18, Vite, Tailwind CSS 3, Vitest, ESLint |
| Runtime | NGINX (unprivileged) serving static files on port `8080` |
| Image | `ghcr.io/alexbosch3000/tools/my-todo-list` |
| Live | https://alexbosch3000.github.io/tools/ |
| CI/CD | [`.github/workflows/app-my-todo-list.yml`](../../.github/workflows/app-my-todo-list.yml) |

## Usage

```bash
make install     # npm ci
make run         # dev server on http://localhost:5173
make ci          # lint, test, audit, build: same checks as CI
make image       # build the container image my-todo-list:local
make image-run   # serve the image on http://localhost:8080
```

## Configuration

| Variable | Scope | Default | Description |
|---|---|---|---|
| `BASE_PATH` | build time (`make build BASE_PATH=/tools/`) | `/` | Public path the app is served from. Set by CI for GitHub Pages. |

## Operations

| Endpoint | Purpose |
|---|---|
| `GET /healthz` | Liveness and readiness probe, returns `{"status":"UP"}`. Not logged. |
| `GET /*` | Application; unknown paths fall back to `index.html`. |

- Access logs: one JSON object per request on stdout; errors on stderr.
- The container runs as UID `101` and works with a read-only root filesystem when `/tmp` is writable (`emptyDir` on Kubernetes).
- Security headers (CSP, `nosniff`, referrer and permissions policies) are defined in [`nginx/security-headers.conf`](nginx/security-headers.conf).
- Fingerprinted files under `/assets/` are cached for one year; `index.html` is always revalidated, so a new deployment is picked up on the next page load.

## Data

Tasks live in the browser under the `localStorage` key `tasks_v1`. Clearing site data deletes them; they are not shared across browsers or devices.

## Layout

```
src/
├── components/   # React components (one per file)
├── hooks/        # state and persistence
└── utils/        # pure functions, covered by *.test.js
nginx/            # runtime web server configuration
```
