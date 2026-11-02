# tools

A monorepo of small, independent applications. Each app is built, tested, packaged and deployed on its own, with the same conventions and the same CI/CD.

## Apps

| App | Description | Stack | Deployment |
|---|---|---|---|
| [dupscan](apps/dupscan) | Duplicate picture finder, exact and near-duplicate, with Excel and CSV reports | Java 21, Maven, picocli | `ghcr.io/alexbosch3000/tools/dupscan` |
| [my-todo-list](apps/my-todo-list) | Task manager with list and calendar views | React, Vite, NGINX | [GitHub Pages](https://alexbosch3000.github.io/tools/), `ghcr.io/alexbosch3000/tools/my-todo-list` |

## Quick start

Prerequisites: `make`, Docker, and the toolchain of the app (see its README).

```bash
cd apps/my-todo-list
make help      # list targets
make install
make run       # development server
make ci        # same checks as CI
```

## Documentation

| Document | Content |
|---|---|
| [docs/app-contract.md](docs/app-contract.md) | Layout, Makefile targets and runtime requirements every app follows |
| [docs/ci-cd.md](docs/ci-cd.md) | Workflows, images, deployment targets, repository settings |
| [AGENTS.md](AGENTS.md) | Rules for contributors and AI coding agents |
| [SECURITY.md](SECURITY.md) | How to report a vulnerability |

## Adding an app

1. Create `apps/<app>/` following [docs/app-contract.md](docs/app-contract.md).
2. Add `.github/workflows/app-<app>.yml` as described in [docs/ci-cd.md](docs/ci-cd.md).
3. Add the app to the table above.

With Claude Code, `/new-app <name> <stack>` does these steps.
