# Standard targets shared by every app in this repository (see docs/app-contract.md).
# Two toolchains: npm for the user interface in web/, Maven for the service that serves it.
# Run `make help` to list them.

APP   := family-hub
IMAGE ?= $(APP):local
PORT  ?= 8080
BASE_PATH ?= /
MVN   := ./mvnw -B -ntp
NPM   := npm --prefix web
JAR   := target/$(APP).jar

# Port the smoke test publishes on the loopback interface.
SMOKE_PORT ?= 18080

# Container hardening: read-only root filesystem, no capabilities, no privilege escalation. The
# SQLite driver extracts its native library to /tmp and loads it from there, so /tmp allows exec;
# /data holds the database (a volume in production).
HARDENING := --read-only --tmpfs /tmp:exec --tmpfs /data:uid=10001,gid=10001 --cap-drop ALL --security-opt no-new-privileges

.DEFAULT_GOAL := help
.PHONY: help install lint test audit build pages api run dev-web observe image image-run smoke ci clean

help: ## List available targets
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  %-10s %s\n", $$1, $$2}'

install: ## Install dependencies exactly as locked
	$(NPM) ci --no-fund
	$(MVN) -DskipTests dependency:go-offline

lint: ## Run static analysis
	$(NPM) run lint
	$(MVN) checkstyle:check

test: ## Run unit and integration tests (Vitest, then JUnit on a temporary SQLite file)
	$(NPM) test
	$(MVN) verify

audit: ## Fail on high or critical known vulnerabilities
	$(NPM) audit --audit-level=high
	$(MVN) -DskipTests package ossindex:audit

build: ## Build the user interface, then the runnable jar that serves it
	$(NPM) run build
	$(MVN) -DskipTests package

pages: ## Build the UI alone for GitHub Pages, served under BASE_PATH and asking for the server address
	VITE_REMOTE_API=true BASE_PATH=$(BASE_PATH) $(NPM) run build

api: ## After an API change: rewrite the OpenAPI contract and regenerate the TypeScript client types
	$(MVN) test -Dtest=OpenApiContractTest -DupdateContract=true -Dsurefire.failIfNoSpecifiedTests=false
	$(NPM) run gen:api

run: build ## Start the service on http://localhost:8080 with its database in ./data
	java --enable-native-access=ALL-UNNAMED -jar $(JAR)

dev-web: ## Start the UI with hot reload on http://localhost:5173, proxying /api to `make run`
	$(NPM) run dev

observe: ## Run the image with Grafana, Tempo, Loki and Prometheus (otel-lgtm) on http://localhost:3000
	docker compose -f observability/docker-compose.yml up --build

image: ## Build the container image
	docker build -t $(IMAGE) .

image-run: ## Run the container image with production-like hardening
	docker run --rm -p $(PORT):8080 $(HARDENING) $(IMAGE)

smoke: ## Check the hardened image starts, migrates its database and answers on /readyz and /healthz
	@cid=$$(docker run -d -p 127.0.0.1:$(SMOKE_PORT):8080 $(HARDENING) $(IMAGE)) && \
	trap 'docker rm -f $$cid >/dev/null' EXIT && \
	for i in $$(seq 1 30); do \
	  curl -fsS http://127.0.0.1:$(SMOKE_PORT)/readyz >/dev/null 2>&1 && \
	  curl -fsS http://127.0.0.1:$(SMOKE_PORT)/healthz && echo && exit 0; sleep 1; \
	done; \
	echo "readyz did not answer" >&2; docker logs $$cid >&2; exit 1

ci: install lint test audit build ## Run the same checks as CI

clean: ## Remove build output and dependencies (the local database in ./data is kept)
	$(MVN) clean
	rm -rf web/dist web/node_modules
