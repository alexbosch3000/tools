# Standard targets shared by every app in this repository (see docs/app-contract.md).
# Run `make help` to list them.

APP   := dupscan
IMAGE ?= $(APP):local
MVN   := ./mvnw -B -ntp
JAR   := dupscan-cli/target/dupscan.jar

# Arguments forwarded to the CLI by `make run` and `make image-run`.
ARGS ?= --help

# Container hardening. The SQLite driver extracts its native library to /tmp and loads it
# from there, so /tmp must allow exec: Docker mounts tmpfs noexec by default.
HARDENING := --read-only --tmpfs /tmp:exec --cap-drop ALL --security-opt no-new-privileges

.DEFAULT_GOAL := help
.PHONY: help install lint test audit build run image image-run smoke ci clean

help: ## List available targets
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  %-10s %s\n", $$1, $$2}'

install: ## Resolve dependencies exactly as locked
	$(MVN) -DskipTests dependency:go-offline

lint: ## Run static analysis
	$(MVN) checkstyle:check

test: ## Run unit tests
	$(MVN) verify

audit: ## Fail on high or critical known vulnerabilities
	$(MVN) -DskipTests package ossindex:audit

build: ## Build the runnable jar
	$(MVN) -DskipTests package

run: build ## Run the CLI locally, e.g. make run ARGS="scan --path ~/Pictures"
	java -jar $(JAR) $(ARGS)

image: ## Build the container image
	docker build -t $(IMAGE) .

image-run: ## Run the image with production-like hardening
	docker run --rm $(HARDENING) $(IMAGE) $(ARGS)

smoke: ## Check the hardened image starts and can open its SQLite index
	docker run --rm $(HARDENING) $(IMAGE) scan --path=/tmp --index=/tmp/smoke.db --no-table

ci: install lint test audit build ## Run the same checks as CI

clean: ## Remove build output
	$(MVN) clean
