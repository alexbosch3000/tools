# Standard targets shared by every app in this repository (see docs/app-contract.md).
# Run `make help` to list them.

APP   := my-todo-list
IMAGE ?= $(APP):local
PORT  ?= 8080
BASE_PATH ?= /

# Port the smoke test publishes on the loopback interface.
SMOKE_PORT ?= 18080

# Container hardening: read-only root filesystem, no capabilities, no privilege escalation.
HARDENING := --read-only --tmpfs /tmp --cap-drop ALL --security-opt no-new-privileges

.DEFAULT_GOAL := help
.PHONY: help install lint test audit build run image image-run smoke ci clean

help: ## List available targets
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  %-10s %s\n", $$1, $$2}'

install: ## Install dependencies exactly as locked
	npm ci --no-fund

lint: ## Run static analysis
	npm run lint

test: ## Run unit tests
	npm test

audit: ## Fail on high or critical known vulnerabilities
	npm audit --audit-level=high

build: ## Build the static bundle into dist/, served under BASE_PATH
	BASE_PATH=$(BASE_PATH) npm run build

run: ## Start the development server with hot reload
	npm run dev

image: ## Build the container image
	docker build -t $(IMAGE) .

image-run: ## Run the container image with production-like hardening
	docker run --rm -p $(PORT):8080 $(HARDENING) $(IMAGE)

smoke: ## Check the hardened image starts and answers on /healthz
	@cid=$$(docker run -d --rm -p 127.0.0.1:$(SMOKE_PORT):8080 $(HARDENING) $(IMAGE)) && \
	trap 'docker stop $$cid >/dev/null' EXIT && \
	for i in 1 2 3 4 5 6 7 8 9 10; do \
	  curl -fsS http://127.0.0.1:$(SMOKE_PORT)/healthz && echo && exit 0; sleep 1; \
	done; \
	echo "healthz did not answer" >&2; docker logs $$cid >&2; exit 1

ci: install lint test audit build ## Run the same checks as CI

clean: ## Remove build output and dependencies
	rm -rf dist node_modules
