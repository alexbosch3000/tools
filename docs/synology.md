# Run the apps on a Synology NAS

How to run the published images with Container Manager on DSM 7. The images are built for `linux/amd64`, so they need a Synology model with an x86_64 (Intel or AMD) processor. On an ARM model they fail with `exec format error`.

The NAS pulls the images from GHCR, which is public: no registry login, and nothing is pushed to the NAS.

## Prerequisites

- **Container Manager** installed from Package Center.
- **SSH** enabled (Control Panel → Terminal & SNMP). Every command below runs over SSH, and Docker needs `sudo` on DSM. If `docker compose` is not found, use `docker-compose`.

## Install an app

The steps are the same for every app under `apps/`:

1. Create `/volume1/docker/<app>` and copy `docker-compose.yml` and `.env.example` from `apps/<app>/` into it (File Station or `scp`). Rename `.env.example` to `.env` and adjust it. The variables are listed in the app's README.
2. Start it from that folder:

   ```bash
   cd /volume1/docker/<app>
   sudo docker compose up -d
   ```

Container Manager → Project → Create can also use the folder.

## my-todo-list

It answers on `http://<nas>:8080` (change `HOST_PORT` if the port is taken; DSM itself uses 5000 and 5001). The container exposes `GET /healthz`, and Docker reports its health in Container Manager.

To reach it over HTTPS, add a rule in Control Panel → Login Portal → Advanced → Reverse Proxy that forwards `https://<hostname>:443` to `http://localhost:8080`, with a certificate from Control Panel → Security → Certificate. Do not forward port 8080 on the router.

Tasks are stored in each browser's `localStorage`: they are not shared between devices, nor with the GitHub Pages version of the app.

## family-hub

It answers on `http://<nas>:8081` (`HOST_PORT`). The database lives in `DATA_DIR`, which must exist and belong to the account in `PUID:PGID` before the first start:

```bash
id <your-dsm-user>                                  # uid=1026(...) gid=100(users) -> PUID=1026, PGID=100
sudo mkdir -p /volume1/docker/family-hub/data
sudo chown 1026:100 /volume1/docker/family-hub/data   # your PUID:PGID
cd /volume1/docker/family-hub && sudo docker compose up -d
```

Open the app once from the local network and create the first parent account before publishing it: until then, the first visitor would create it. To reach it from outside, add a reverse proxy rule (Control Panel → Login Portal → Advanced → Reverse Proxy) from `https://<hostname>:443` to `http://localhost:8081`, with a certificate for `<hostname>` (Control Panel → Security → Certificate, Let's Encrypt). Never forward port 8081 on the router.

The interface is also published on GitHub Pages (`https://alexbosch3000.github.io/tools/`). It asks once for the server address, `https://<hostname>`, and keeps it in the browser. This requires the HTTPS reverse proxy above, because an HTTPS page cannot call an HTTP server, and `APP_ALLOWED_ORIGINS=https://alexbosch3000.github.io` in `.env` (the default of `.env.example`). Leave `APP_ALLOWED_ORIGINS` empty to use only the interface served by the NAS.

Observability: set `OTEL_EXPORTER_OTLP_ENDPOINT` in `.env` to an OpenTelemetry collector (for example `http://<collector>:4318`) to export traces, logs and metrics; Prometheus can also scrape `/actuator/prometheus`.

Backups: the *Famille* page offers a consistent copy of the database to download. With Hyper Backup, back up `DATA_DIR` while the container is stopped: a copy of a running SQLite database can miss its latest changes, which are still in `family-hub.db-wal`.

## dupscan

A scan runs to completion and exits, so the project is started on demand rather than kept running.

1. Find the account that owns your photos and put its ids in `.env`:

   ```bash
   id <your-dsm-user>        # uid=1026(...) gid=100(users) -> PUID=1026, PGID=100
   ```

2. Create the output folder and give it to that account:

   ```bash
   sudo mkdir -p /volume1/docker/dupscan
   sudo chown 1026:100 /volume1/docker/dupscan     # your PUID:PGID
   ```

3. Run a scan:

   ```bash
   cd /volume1/docker/dupscan
   sudo docker compose run --rm dupscan
   ```

The reports `duplicates.xlsx` and `duplicates.csv` and the index `index.db` are written to `DATA_DIR`. The photos are mounted read-only and are never modified. The index makes later scans incremental. Thumbnails in `@eaDir` are skipped.

To schedule it, create a user-defined script in Control Panel → Task Scheduler running as `root`. The tool exits with `1` when it finds duplicates, which Task Scheduler would report as a failure, so accept that code:

```bash
cd /volume1/docker/dupscan && docker compose run --rm dupscan || [ $? -eq 1 ]
```

## Update and roll back

```bash
cd /volume1/docker/<app>
sudo docker compose pull
sudo docker compose up -d        # not needed for dupscan: the next run uses the new image
```

`IMAGE_TAG=latest` follows the `main` branch. To freeze a version, or to go back to the previous one, set `IMAGE_TAG=sha-<full commit sha>` in `.env` and run the same commands. The tags are listed on the package page of the repository on GitHub.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `exec format error` | The NAS has an ARM processor | These images are amd64 only; an ARM model needs an `arm64` build. |
| `Cannot open index at /data/index.db` | `PUID:PGID` cannot write `DATA_DIR` | `chown` the folder to that account, or fix the ids in `.env`. |
| `port is already allocated` | Another service uses `HOST_PORT` | Set another `HOST_PORT` in `.env`. |
| `dupscan` exits with code `1` | Duplicates were found | Expected: the reports list them. `2` is a usage error, `3` a runtime error. |

Container logs rotate at three files of 10 MB, so they cannot fill the volume.
