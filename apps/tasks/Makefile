# Standard targets shared by every app in this repository (see docs/app-contract.md).
# Run `make help` to list them.

APP   := tasks
IMAGE ?= $(APP):local
PORT  ?= 8080

.DEFAULT_GOAL := help
.PHONY: help install lint test audit build run image image-run ci clean

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

build: ## Build the static bundle into dist/
	npm run build

run: ## Start the development server with hot reload
	npm run dev

image: ## Build the container image
	docker build -t $(IMAGE) .

image-run: ## Run the container image with production-like hardening
	docker run --rm -p $(PORT):8080 --read-only --tmpfs /tmp --cap-drop ALL --security-opt no-new-privileges $(IMAGE)

ci: install lint test audit build ## Run the same checks as CI

clean: ## Remove build output and dependencies
	rm -rf dist node_modules
