# dupscan

Find duplicate pictures across folders — both **exact** (byte/content-identical) and
**near-duplicate** (re-compressed, resized or reformatted copies) — and report them to the
console, Excel and CSV. Built for large photo libraries on a NAS.

It is **read-only**: it never moves, renames or deletes your files. It only reports.

## Why

The same photo often exists many times with different names, dates, owners and formats.
`dupscan` detects those copies so you can reorganise with confidence:

- **Exact** duplicates via a streaming **SHA-256** of the file bytes (independent of name,
  date, owner or extension).
- **Near** duplicates via a 64-bit **perceptual hash** (dHash or pHash) compared with a
  configurable Hamming tolerance, so re-encoded copies still match.

## Design at a glance

Multi-module Maven project, layered so the engine is reusable (a backend API can sit on top
later without a rewrite):

- **`dupscan-core`** — the engine: scan, hash, match, persistent index, reporting, exposed
  through one facade `DuplicateScanService`. No CLI/web dependencies.
- **`dupscan-cli`** — the picocli front-end and the runnable fat jar.

Key properties:

- **Scales to millions:** a streaming producer/worker/single-writer pipeline keeps memory
  flat; a persistent **SQLite** index (WAL mode) makes re-scans **incremental** (unchanged
  files are skipped) and the run **resumable** after interruption.
- **Safe concurrency:** one writer with idempotent per-path upserts — retries and re-runs
  never double-count; one bad file is skipped and logged, never aborting the run.
- **Multi-node ready:** `WorkSource` and `IndexStore` are interfaces. v1 uses a local
  filesystem walk + SQLite; a future backend can drop in a distributed queue + shared
  database without touching scanning, hashing or matching.

## Build

```bash
mvn clean verify
# runnable jar: dupscan-cli/target/dupscan.jar
```

Requires Java 21+ and Maven 3.8+.

## Usage

```bash
java -jar dupscan-cli/target/dupscan.jar scan \
  --path /volume1/photos --path /volume1/backup \
  --threshold 0.15 \
  --report dupes.xlsx --csv dupes.csv
```

The console shows a summary and a table whose rows are matching pictures with `ORIGINAL`
and `DUPLICATE` columns. Exit codes: `0` = no duplicates, `1` = duplicates found,
`2` = usage error, `3` = runtime error.

### Options

| Option | Description |
| --- | --- |
| `-p, --path DIR` | Folder to scan (repeatable). Nested folders are de-duplicated so nothing is scanned twice. |
| `--include GLOB` / `--exclude GLOB` | Path filters (repeatable). E.g. `--exclude '**/@eaDir/**'`. |
| `--formats EXT,...` | Restrict extensions (default: common + heic/heif + raw). |
| `--min-size BYTES` | Ignore files smaller than this. |
| `--follow-symlinks` | Follow symlinks (off by default; cycle-detected when on). |
| `-m, --mode` | `exact` \| `perceptual` \| `exact+perceptual` (default). |
| `-t, --threshold 0.0-1.0` | Perceptual tolerance (normalized Hamming distance, default `0.15`). |
| `--hash` | `dhash` (default) \| `phash`. |
| `--original-rule` | `highest-quality` (default) \| `oldest` \| `shortest-path`. |
| `--index FILE` | SQLite index for incremental re-scans (default `~/.dupscan/index.db`). |
| `--rehash` / `--prune` | Re-hash everything / remove rows for vanished files. |
| `-r, --report FILE.xlsx` / `--csv FILE.csv` | Report outputs. |
| `--top N` / `--no-table` | Console table size / suppress it. |
| `--threads N` | Worker threads (default: CPU cores). |
| `--max-pixels N` / `--max-bytes N` | Decode-bomb guards. |

### Report format

Both Excel and CSV use a **tidy, one-row-per-file** layout (filter/pivot friendly):
`group_id, role, match_type, distance, path, file_name, size_bytes, width, height, format,
exif_date, modified_date, content_sha256, perceptual_hash`. The Excel file adds a `Summary`
sheet and auto-splits across sheets if it would exceed Excel's per-sheet row limit. At very
large scale, prefer CSV.

## Docker (Synology DSM 7 / Container Manager)

```bash
docker build -t dupscan .

docker run --rm --network none \
  -u 1000:1000 \
  -v /volume1/photos:/photos:ro \
  -v /volume1/dupscan:/data \
  dupscan scan --path /photos --index /data/index.db \
               --report /data/dupes.xlsx --csv /data/dupes.csv
```

Photos are mounted **read-only**, `/data` is the writable volume for the index and reports,
the container runs **non-root**, and `--network none` is safe because the app makes no
outbound connections.

## CI/CD (GitHub Actions)

The full lifecycle runs on GitHub-native features — no external services or secrets beyond
the built-in `GITHUB_TOKEN`:

- **CI** (`.github/workflows/ci.yml`): on every push to `main` and every pull request,
  builds and runs the test suite with `./mvnw clean verify` (JDK 21), and uploads the jar
  and test reports as artifacts.
- **Release / deploy** (`.github/workflows/release.yml`): on a `vX.Y.Z` tag (or manual
  dispatch), stamps the project version, builds, attaches the jar to a **GitHub Release**,
  and pushes a **multi-arch (amd64/arm64) image to GHCR** at
  `ghcr.io/<owner>/tools` (tags `X.Y.Z`, `X.Y`, `latest`). Pull it on the Synology with
  `docker pull ghcr.io/<owner>/tools:latest`.
- **CodeQL** (`.github/workflows/codeql.yml`): static security analysis on push, PR and a
  weekly schedule, with results in the repository's Security tab.
- **Dependabot** (`.github/dependabot.yml`): weekly update PRs for Maven dependencies,
  GitHub Actions and the Docker base image.

Cut a release:

```bash
git tag v0.1.0 && git push origin v0.1.0
```

The build is reproducible via the committed Maven Wrapper (`./mvnw`, pinned to 3.9.11).

## Security notes

- **Read-only**: never modifies your photos in v1.
- **Spreadsheet formula injection**: filename/path cells beginning with `= + - @` are
  neutralised before being written to Excel/CSV.
- **Decode-bomb guards**: image dimensions are checked from the header before decoding;
  oversized files are skipped.
- **No network**: the app makes no outbound calls.

## Roadmap (out of scope for v1)

- Backend API (`dupscan-api`) exposing `DuplicateScanService` over HTTP + a thin client CLI.
- Multi-node distribution via the existing `WorkSource`/`IndexStore` seams.
- A confirmed destructive-cleanup command (move/delete/hardlink duplicates).
- Full native HEIC/RAW pixel decode (libheif/dcraw) beyond what ImageIO handles.

## Notes on formats

Common formats (JPEG/PNG/GIF/BMP/TIFF/WEBP) decode via the JDK + TwelveMonkeys ImageIO.
HEIC/HEIF and camera RAW are always **content-hashed** (exact matching works); perceptual
hashing applies when a supported reader can decode them, and otherwise those files are
indexed but excluded from near-duplicate matching (see roadmap).
