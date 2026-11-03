# Standard targets shared by every app in this repository (see docs/app-contract.md).
# Two toolchains: npm for the user interface in web/, Maven for the service that serves it.
# Run `make help` to list them.

APP   := family-hub
IMAGE ?= $(APP):local
PORT  ?= 8080
MVN   := ./mvnw -B -ntp
NPM   := npm --prefix web
JAR   := target/$(APP).jar

# Port the smoke test publishes on the loopback interface.
SMOKE_PORT ?= 18080

# Container hardening: read-only root filesystem, no capabilities, no privilege escalation. The
# SQLite driver extracts its native library to /tmp and loads it from there, so /tmp allows exec;
# /data holds the database (a volume in production).
HARDENING := --read-only --tmpfs /tmp:exec --tmpfs /data --cap-drop ALL --security-opt no-new-privileges

.DEFAULT_GOAL := help
.PHONY: help install lint test audit build run dev-web image image-run smoke ci clean

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

run: build ## Start the service on http://localhost:8080 with its database in ./data
	java -jar $(JAR)

dev-web: ## Start the UI with hot reload on http://localhost:5173, proxying /api to `make run`
	$(NPM) run dev

image: ## Build the container image
	docker build -t $(IMAGE) .

image-run: ## Run the container image with production-like hardening
	docker run --rm -p $(PORT):8080 $(HARDENING) $(IMAGE)

smoke: ## Check the hardened image starts, migrates its database and answers on /readyz and /healthz
	@cid=$$(docker run -d --rm -p 127.0.0.1:$(SMOKE_PORT):8080 $(HARDENING) $(IMAGE)) && \
	trap 'docker stop $$cid >/dev/null' EXIT && \
	for i in $$(seq 1 30); do \
	  curl -fsS http://127.0.0.1:$(SMOKE_PORT)/readyz >/dev/null 2>&1 && \
	  curl -fsS http://127.0.0.1:$(SMOKE_PORT)/healthz && echo && exit 0; sleep 1; \
	done; \
	echo "readyz did not answer" >&2; docker logs $$cid >&2; exit 1

ci: install lint test audit build ## Run the same checks as CI

clean: ## Remove build output and dependencies (the local database in ./data is kept)
	$(MVN) clean
	rm -rf web/dist web/node_modules
