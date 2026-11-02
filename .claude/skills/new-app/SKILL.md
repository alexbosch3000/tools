---
name: new-app
description: Scaffold a new independent application under apps/ with its Makefile, Dockerfile, tests, README and CI/CD workflow, following docs/app-contract.md. Use when the user asks to create, add or bootstrap an app, service, tool or website in this repository.
argument-hint: <name> <stack> [description]
---

# Create a new app

Arguments: `$ARGUMENTS` (name, stack, optional description). Ask for any missing one. The name must be lowercase kebab-case without a type suffix, and `apps/<name>` must not exist yet.

## Steps

1. Read `docs/app-contract.md` and `docs/ci-cd.md`.
2. Settle the kind (service, user interface, command-line tool) and the stack, then pick the reference:
   - Static web app (React, Vite): copy the structure of `apps/my-todo-list` (Makefile, Dockerfile, `nginx/`, `.nvmrc`, ESLint and Vite configs), without its source code.
   - Java command-line tool: copy the structure of `apps/dupscan` (Makefile, Dockerfile, `checkstyle.xml`, `.java-version`, Maven Wrapper), without its source code.
   - Other stacks: follow the matching baseline in `docs/app-contract.md`. If none exists, propose one to the user before writing code.

   The kind decides which runtime requirements apply, so confirm it before writing code: only a service owes a port, `/healthz`, metrics and traces.
3. Create `apps/<name>/` with:
   - `README.md` using the same sections as `apps/my-todo-list/README.md` (summary table, usage, configuration, operations, layout).
   - `Makefile` exposing every target of the contract, with `APP := <name>`.
   - `Dockerfile` (multi-stage, non-root, base images pinned by digest) and `.dockerignore`.
   - A minimal working feature with at least one unit test.
   - For services: `/healthz`, `/readyz`, `/metrics`, JSON logs, `OTEL_*` configuration, graceful shutdown.
4. Create `.github/workflows/app-<name>.yml` from `app-my-todo-list.yml`: replace the app name and the path filters, use the reusable verify workflow of the stack (create `reusable-<stack>.yml` if missing), remove `pages: true` and the `pages` job unless the user explicitly wants this app to take over Pages (then follow "Hand over GitHub Pages" in `docs/ci-cd.md`).
5. If the stack or the kind is new to the repository, update the shared setup in the same pull request, following "Adding a stack" in `docs/app-contract.md`: write the baseline there, list the new `reusable-<stack>.yml` in the workflow table of `docs/ci-cd.md`, add the language to the CodeQL matrix in `.github/workflows/codeql.yml` and the ecosystem to `.github/dependabot.yml`.
6. Add the app, with its kind, to the table in the root `README.md`.
7. Run `make ci` (and `make image` if Docker is available) in `apps/<name>` and fix every failure.
8. Commit with `feat(<name>): add <name> app`.
