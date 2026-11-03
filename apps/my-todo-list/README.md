# my-todo-list

Personal task manager with list and calendar views. Static single-page application (React, Vite, Tailwind CSS); tasks are stored in the browser's `localStorage`, no backend.

| | |
|---|---|
| Stack | Node.js 26, React 18, Vite, Tailwind CSS 3, Vitest, ESLint |
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
make smoke       # start the hardened image and check /healthz
```

## Configuration

| Variable | Scope | Default | Description |
|---|---|---|---|
| `BASE_PATH` | build time (`make build BASE_PATH=/tools/`) | `/` | Public path the app is served from. Set by CI for GitHub Pages. |
| `IMAGE_TAG` | `docker-compose.yml` | `latest` | Image tag to run; use a `sha-<commit>` tag to pin a version. |
| `HOST_PORT` | `docker-compose.yml` | `8080` | Port published on the Docker host. |

## Deploy on a Synology NAS

[`docker-compose.yml`](docker-compose.yml) runs the published image with the same hardening as `make image-run` (read-only filesystem, no capabilities, log rotation). Copy it with [`.env.example`](.env.example) renamed to `.env` to the NAS and follow [docs/synology.md](../../docs/synology.md). The app then answers on `http://<nas>:8080`. Tasks stay in each browser's `localStorage`, so tasks created on GitHub Pages do not appear there.

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
