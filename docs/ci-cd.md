# CI/CD

Each app has its own workflow, triggered only by changes in its directory. Shared steps live in reusable workflows so that every app is verified, packaged and published the same way.

## Workflows

| File | Trigger | Purpose |
|---|---|---|
| `app-<app>.yml` | push to `main`, pull request, manual; filtered on `apps/<app>/**` | Entry point for one app: chains the reusable workflows and its deployment. |
| `reusable-node.yml` | called | Sets up Node.js from `.nvmrc` and runs `make ci`. With `pages: true`, also builds and uploads the GitHub Pages site on `main`. |
| `reusable-java.yml` | called | Sets up the JDK from `.java-version` and runs `make ci`. With `node-dir`, also sets up Node.js from that directory's `.nvmrc` for an embedded front end. |
| `reusable-container.yml` | called | Builds the image, smoke-tests it (`make smoke`), scans it, and when `push: true` publishes it to GHCR with a build provenance attestation. |
| `reusable-pages.yml` | called | Deploys the Pages site uploaded earlier in the same run. |
| `codeql.yml` | push to `main`, pull request, weekly | SAST for all code and for the workflows themselves. |
| `dependency-review.yml` | pull request | Blocks new dependencies with high or critical vulnerabilities. |

Dependabot (`.github/dependabot.yml`) opens weekly update pull requests for GitHub Actions, npm, Maven and Docker base images in every `apps/*` directory, and for npm in `apps/*/web` (front ends embedded in a service). Two rules keep those pull requests mergeable:

- `react` and `react-dom` are grouped, because their versions must move together.
- Major versions of the Node.js, Temurin and Maven base images are not proposed. They follow the LTS releases and are bumped by hand, together with `.nvmrc` or `.java-version`, so that CI keeps testing what the image ships. Tailwind CSS major versions are skipped until the app is migrated on purpose.

## Pipeline of an app

```mermaid
flowchart LR
    A[push / pull request<br>on apps/app/**] --> B[verify<br>make ci]
    B --> C[container<br>build + scan]
    C -->|main only| D[push to GHCR<br>+ provenance attestation]
    D -->|main only| E[deploy]
```

| Stage | Pull request | `main` |
|---|---|---|
| verify | yes | yes |
| container build, smoke test and scan | yes | yes |
| image push and attestation | no | yes |
| deploy | no | yes |

## Images

- Registry: `ghcr.io/<owner>/<repo>/<app>`.
- Tags: `sha-<full commit sha>` on every push to `main`, plus `latest`. Deploy by digest or by `sha-` tag. The Synology `docker-compose.yml` files default to `latest` for convenience on a home NAS: set `IMAGE_TAG` to a `sha-` tag to freeze a version.
- Labels: OCI `org.opencontainers.image.*` labels, including the source commit.
- Scan: Grype fails the build on high or critical vulnerabilities that have a fix available.
- Provenance: verify an image with `gh attestation verify oci://ghcr.io/<owner>/<repo>/<app>:sha-<sha> --owner <owner>`.

## Deployment targets

| Target | When to use | Notes |
|---|---|---|
| GitHub Pages | One static site per repository | Pages hosts a single site: only one app owns it at a time, currently `my-todo-list`. See [Hand over GitHub Pages](#hand-over-github-pages). |
| Synology NAS (Container Manager) | Private use at home | Each app's `docker-compose.yml` runs the GHCR image; see [synology.md](synology.md). The NAS pulls, nothing is pushed to it. |
| Container platform (Kubernetes, Azure Container Apps, ...) | Every other app | Pull the image from GHCR by digest. Declarative deployment (for example an ArgoCD-managed manifest repository) is kept outside this repository. |

## Hand over GitHub Pages

Pages is served at `https://<owner>.github.io/<repo>/`. The app that owns it builds its site with `make build BASE_PATH=/<repo>/` (see the static web app baseline in [app-contract.md](app-contract.md)).

To give Pages to another app, in a single pull request:

1. In the current owner's workflow (`app-my-todo-list.yml`), remove `pages: true` from the `verify` job and delete the `pages` job.
2. In the new owner's workflow, add `pages: true` to its `verify` job and copy the `pages` job.
3. Update the Deployment column of the root `README.md` and the owner named in [Deployment targets](#deployment-targets).

The previous app keeps being published as a container image. Two workflows must never deploy Pages at the same time: each deployment replaces the whole site.

## Supply chain rules

- Third-party actions are pinned to a full commit SHA with the version in a trailing comment; Dependabot updates both.
- Base images are pinned by digest in each `Dockerfile`.
- `GITHUB_TOKEN` is read-only by default; write scopes are granted per job only where needed.
- `actions/checkout` runs with `persist-credentials: false`.

## Repository settings

These settings are not stored in the repository and must be configured once in GitHub:

| Setting | Value |
|---|---|
| Settings → Pages → Source | GitHub Actions |
| Settings → Code security | Dependency graph, Dependabot alerts and security updates, secret scanning, push protection and private vulnerability reporting enabled |
| Settings → Rules → main | Require a pull request, block force pushes, require the `dependency-review` and `CodeQL` checks (exact check names, case-sensitive) |
| Packages → `<app>` → Visibility | Public, or grant access to the consumers of the image |

Per-app checks are path-filtered and are skipped when an app is untouched, so they cannot be marked as required directly: a required check that never runs blocks the pull request.

## Adding a workflow for a new app

Copy `app-my-todo-list.yml` to `app-<app>.yml`, replace `my-todo-list` with the app name, pick the reusable verify workflow matching the stack, and remove `pages: true` and the `pages` job unless the app takes over GitHub Pages. Only a user interface deploys; a service deploys from its image and a command-line tool has no deployment job at all, the published image being the deliverable.

For a stack without a reusable workflow yet, add `reusable-<stack>.yml` following `reusable-node.yml`: set up the toolchain from the app's pin file, then run `make ci`. Add it to the table above in the same pull request, and follow [Adding a stack](app-contract.md#adding-a-stack) for the rest of the one-off setup.
