# AGENTS.md

Instructions for AI coding agents (and humans) working in this repository.

## Repository

A monorepo of independent applications. Each app lives in `apps/<app>/`, has its own toolchain, tests, container image and CI/CD workflow, and never depends on another app.

```
.
├── apps/<app>/              # one independent application per directory
├── docs/
│   ├── app-contract.md      # what every app must provide (read before creating or changing an app)
│   └── ci-cd.md             # pipelines, images, deployment, repository settings
├── .github/
│   ├── workflows/
│   │   ├── app-<app>.yml    # one entry-point workflow per app
│   │   └── reusable-*.yml   # shared build, scan and publish steps
│   └── dependabot.yml
└── .claude/                 # Claude Code settings and skills
```

Current apps are listed in [README.md](README.md).

## Working on an app

Run every command from the app directory. The `Makefile` is the interface; do not call CI-specific scripts.

```bash
cd apps/<app>
make install   # once
make ci        # lint + test + audit + build: must pass before every commit
make run       # local development
```

When a change touches several apps, run `make ci` in each of them.

## Rules

### Scope and structure

- Change only the app the task is about. Never import, symlink or copy files across `apps/*`.
- A new app follows [docs/app-contract.md](docs/app-contract.md) and gets its own `app-<app>.yml` workflow. Use the `new-app` skill in Claude Code.
- Prefer the simplest solution that meets the requirement. No speculative abstraction, no unused configuration, no new dependency when the standard library or an existing dependency is enough.
- Keep files small and single-purpose. Put pure logic in testable modules, separate from UI or I/O.

### Naming

| Item | Convention | Example |
|---|---|---|
| App directory, image, workflow | kebab-case, no type suffix | `apps/invoice-parser`, `app-invoice-parser.yml` |
| Files | the stack's convention | `TaskCard.jsx`, `dates.js`, `InvoiceService.java` |
| Branches | `<type>/<short-description>` | `feat/my-todo-list-recurring`, `fix/my-todo-list-timezone` |
| Commits | [Conventional Commits](https://www.conventionalcommits.org) with the app as scope | `feat(my-todo-list): add recurring tasks` |

### Language

- Code, identifiers, comments, commit messages and documentation: English.
- User-facing text inside an app: the language of that app's audience (French for `my-todo-list`).

### Comments

- Write comments for a reader who discovers the code: what a non-obvious block does, why a choice was made, how to use a module.
- Each new file starts with a one-line comment stating its purpose (except JSON and generated files).
- Never write comments describing the change being made ("added", "fixed", "now uses", "refactored", "new"). That belongs in the commit message.
- Do not comment obvious code.

### Tests

- Every change in behaviour comes with a unit test. Bugs are fixed with a test that fails before the fix.
- Tests are deterministic: fake timers and time zones explicitly, no network.

### Security

- Never commit secrets, tokens, credentials or personal data. Configuration comes from environment variables documented in the app README; provide `.env.example` when needed.
- Validate all external input. Never build shell commands, SQL or HTML by string concatenation with untrusted data.
- Keep dependencies locked (`package-lock.json`, Maven versions); run `make audit`.
- Containers run as non-root with a read-only root filesystem. Base images are pinned by digest.
- GitHub Actions are pinned by full commit SHA with the version as a trailing comment. Workflows declare the minimum `permissions`.

### Operability and observability

Apply the runtime contract of [docs/app-contract.md](docs/app-contract.md), which depends on the app's kind. A service listens on port `8080`, answers `/healthz`, exposes Prometheus metrics and OpenTelemetry traces and shuts down gracefully; a user interface and a command-line tool are exempt from part of that, and a command-line tool documents its exit codes instead. JSON logs on stdout, configuration through environment variables, a non-root user and a read-only root filesystem apply to every app.

### Documentation

Update the app `README.md` whenever usage, configuration, endpoints or data change. Update `README.md` at the root when an app is added or removed, and the docs under `docs/` when a convention changes.

An app that introduces a language, a technology or a kind the repository has not used yet also updates the shared setup, in the same pull request: its baseline and, if needed, its kind in [docs/app-contract.md](docs/app-contract.md), its `reusable-<stack>.yml` in the workflow table of [docs/ci-cd.md](docs/ci-cd.md), the CodeQL matrix and the Dependabot ecosystems. See [Adding a stack](docs/app-contract.md#adding-a-stack).

## Definition of done

- [ ] `make ci` passes in every affected app
- [ ] Tests cover the new or changed behaviour
- [ ] READMEs and docs reflect the change
- [ ] No secrets, no debug leftovers, no change-narrating comments
- [ ] Commit messages follow Conventional Commits

## Git

- Work on a feature branch; `main` is deployed automatically.
- Do not force-push shared branches, rewrite published history or bypass hooks (`--no-verify`).
- Pull requests use [.github/pull_request_template.md](.github/pull_request_template.md).
