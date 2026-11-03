# dupscan

Finds duplicate pictures across folders and reports them. Detects both **exact** duplicates
(byte-identical, whatever the name, date, owner or extension) and **near** duplicates
(re-compressed, resized or reformatted copies) through perceptual hashing. Command-line tool,
designed for large photo libraries on a NAS. It only reads and reports: it never moves,
renames or deletes anything.

| | |
|---|---|
| Kind | [Command-line tool](../../docs/app-contract.md#app-kinds): runs to completion, listens on nothing |
| Stack | Java 25, Maven, picocli, TwelveMonkeys ImageIO, metadata-extractor, Apache POI, SQLite |
| Runtime | Command-line tool on a Temurin JRE, non-root UID `10001` |
| Image | `ghcr.io/alexbosch3000/tools/dupscan` |
| CI/CD | [`.github/workflows/app-dupscan.yml`](../../.github/workflows/app-dupscan.yml) |

## Usage

```bash
make install                                   # resolve dependencies
make ci                                        # lint, test, audit, build: same checks as CI
make run ARGS="scan --path ~/Pictures"         # run the CLI locally
make image                                     # build the image dupscan:local
make image-run ARGS="--help"                   # run the image with production hardening
```

Running the built jar directly:

```bash
java -jar dupscan-cli/target/dupscan.jar scan \
  --path /volume1/photos --path /volume1/backup \
  --threshold 0.15 \
  --report dupes.xlsx --csv dupes.csv
```

From the container, mounting the photos read-only and a writable volume for the index and
reports:

```bash
docker run --rm --read-only --tmpfs /tmp --cap-drop ALL --security-opt no-new-privileges \
  -v /volume1/photos:/photos:ro -v /volume1/dupscan:/data \
  ghcr.io/alexbosch3000/tools/dupscan \
  scan --path /photos --index /data/index.db --report /data/dupes.xlsx
```

### Options

| Option | Description |
|---|---|
| `-p, --path DIR` | Folder to scan, repeatable. Nested folders are collapsed so nothing is scanned twice. |
| `--include GLOB` / `--exclude GLOB` | Path filters, repeatable, e.g. `--exclude '**/@eaDir/**'` to skip Synology thumbnails. |
| `--formats EXT,...` | Extensions to scan. Default: common formats plus HEIC/HEIF and camera RAW. |
| `--min-size BYTES` | Ignore files smaller than this. Default `0`. |
| `--follow-symlinks` | Follow symbolic links. Off by default; cycles are detected when on. |
| `-m, --mode MODE` | `exact`, `perceptual` or `exact+perceptual`. Default `exact+perceptual`. |
| `-t, --threshold 0.0-1.0` | Perceptual tolerance as a normalized Hamming distance. Default `0.15`. |
| `--hash ALGO` | `dhash` (default) or `phash`. |
| `--original-rule RULE` | `highest-quality` (default), `oldest` or `shortest-path`. |
| `--index FILE` | SQLite index enabling incremental re-scans. Default `~/.dupscan/index.db`. |
| `--rehash` / `--prune` | Re-hash everything / drop index rows for files that disappeared. |
| `-r, --report FILE.xlsx` / `--csv FILE.csv` | Report outputs. |
| `--top N` / `--no-table` | Limit the console table to N groups / print only the summary. |
| `--threads N` | Worker threads. Default: number of CPU cores. |
| `--max-pixels N` / `--max-bytes N` | Decode-bomb guards. |

## Configuration

The scan is configured by command-line flags, documented above. Only two environment
variables affect the process:

| Variable | Scope | Default | Description |
|---|---|---|---|
| `HOME` | runtime | `/tmp` in the image | Base of the default index path (`$HOME/.dupscan/index.db`). Set to `/tmp` so the image works with a read-only root filesystem. |
| `JAVA_TOOL_OPTIONS` | runtime | unset | Standard JVM options, e.g. `-Xmx2g` to cap heap on a small NAS. |

No secrets are needed: the tool makes no outbound network connection.

## Operations

A command-line tool, not a service: no HTTP port, no health endpoint, no metrics
(see [app kinds](../../docs/app-contract.md#app-kinds)).

| Exit code | Meaning |
|---|---|
| `0` | No duplicates found. |
| `1` | Duplicates found. |
| `2` | Usage error. |
| `3` | Runtime error. |

- Progress and warnings go to stderr, the report table to stdout. Unreadable or oversized
  files are skipped and logged; one bad file never aborts a run.
- The container runs as UID `10001` with a read-only root filesystem, writing only to `/tmp`
  and to the volumes you mount.
- Interrupting a scan is safe: every batch is committed to the index, so the next run resumes
  instead of re-hashing.

## Reports

The Excel and CSV reports share the same tidy layout, one row per file, built for filtering
and pivot tables: `group_id`, `role` (`original` or `duplicate`), `match_type`, `distance`,
`path`, `file_name`, `size_bytes`, `width`, `height`, `format`, `exif_date`, `modified_date`,
`content_sha256`, `perceptual_hash`. The workbook adds a `Summary` sheet and splits across
sheets when it would exceed Excel's row limit. Prefer CSV for very large libraries.

Cell values starting with `=`, `+`, `-` or `@` are prefixed with an apostrophe, so a crafted
filename cannot become a formula when the report is opened.

## Data

The SQLite index stores, per file, its canonical path, size, modification time, SHA-256,
perceptual hash, dimensions, format and EXIF date. It exists to make re-scans incremental:
deleting it only costs a full re-hash. It contains file paths, so treat it as personal data
and keep it next to the library rather than in a shared location.

## Layout

```
dupscan-core/     engine: scan, hash, match, index, report
  src/main/java/.../scan      filesystem walk, root normalization, file filter
  src/main/java/.../pipeline  bounded producer/worker/single-writer pipeline
  src/main/java/.../hash      SHA-256, perceptual hashing, decode guards
  src/main/java/.../index     IndexStore interface and its SQLite implementation
  src/main/java/.../match     BK-tree, union-find, duplicate grouping
  src/main/java/.../report    Excel and CSV reporters
  src/main/java/.../service   DuplicateScanService: the one entry point
dupscan-cli/      picocli front-end and the runnable jar
checkstyle.xml    rules enforced by `make lint`
```

`dupscan-core` holds no CLI or web dependency, so the engine can later be driven by something
other than a command line. `WorkSource` and `IndexStore` are interfaces: the local filesystem
walk and the SQLite index are the current implementations.
