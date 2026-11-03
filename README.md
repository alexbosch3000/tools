# tools

A monorepo of small, independent applications. Each app is built, tested, packaged and deployed on its own, with the same conventions and the same CI/CD.

## Apps

| App | Kind | Description | Stack | Deployment |
|---|---|---|---|---|
| [dupscan](apps/dupscan) | Command-line tool | Finds duplicate pictures, byte-identical and visually near-identical, and reports them to Excel and CSV | Java 25, Maven, picocli | `ghcr.io/alexbosch3000/tools/dupscan` |
| [my-todo-list](apps/my-todo-list) | User interface | Task manager with list and calendar views | React, Vite, NGINX | [GitHub Pages](https://alexbosch3000.github.io/tools/), `ghcr.io/alexbosch3000/tools/my-todo-list` |

Apps are independent and deliberately heterogeneous: see [app kinds](docs/app-contract.md#app-kinds) for what each kind owes at runtime.

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
| [docs/synology.md](docs/synology.md) | Run the apps on a Synology NAS with Container Manager |
| [AGENTS.md](AGENTS.md) | Rules for contributors and AI coding agents |
| [SECURITY.md](SECURITY.md) | How to report a vulnerability |

## Adding an app

1. Create `apps/<app>/` following [docs/app-contract.md](docs/app-contract.md).
2. Add `.github/workflows/app-<app>.yml` as described in [docs/ci-cd.md](docs/ci-cd.md).
3. Add the app, with its kind, to the table above.
4. If its language or technology is new to the repository, follow [Adding a stack](docs/app-contract.md#adding-a-stack).

With Claude Code, `/new-app <name> <stack>` does these steps.
