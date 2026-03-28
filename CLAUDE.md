# CLAUDE.md

This file provides guidance for AI assistants (Claude and others) working in this repository.

## Repository Overview

**Repository**: `alexbosch3000/tools`
**Purpose**: A tools repository (currently in early setup; contents TBD by the owner)
**State**: Freshly initialized — only a README exists so far

## Current Structure

```
tools/
├── README.md       # Minimal project header
└── CLAUDE.md       # This file
```

## Development Branch

Active development happens on feature branches. The main branch is `main`. When working on tasks, develop on the branch specified in your session context (e.g., `claude/<task-slug>`).

## Git Workflow

1. **Branch naming**: `claude/<short-description>` for AI-driven tasks, `feature/<name>` for regular features
2. **Commits**: Write clear, imperative commit messages (e.g., "Add CSV parser utility")
3. **Push**: Always push with `-u` to set upstream: `git push -u origin <branch-name>`
4. **PRs**: Do not create pull requests unless explicitly requested by the user

## General Conventions (apply when code is added)

- Prefer small, single-purpose files over large monolithic ones
- Add a brief comment at the top of each new file explaining its purpose
- Keep dependencies minimal; prefer standard library solutions where practical
- Document any non-obvious logic inline

## Working with This Repo as an AI Assistant

- **Read before editing**: Always read a file before modifying it
- **Don't over-engineer**: Match scope to what was asked; avoid speculative abstractions
- **No unsolicited cleanup**: Don't refactor or add comments to code you didn't change
- **Security**: Never introduce command injection, hardcoded secrets, or other OWASP vulnerabilities
- **Reversible actions**: Confirm with the user before destructive git operations (force push, reset --hard, branch deletion)

## Adding New Tools or Scripts

When this repo grows to contain actual tools:

1. Place scripts in a logical subdirectory (e.g., `scripts/`, `bin/`, or per-language folders)
2. Update this CLAUDE.md with the new structure and any language-specific conventions
3. Add a `README.md` or inline documentation explaining usage
4. Include tests if the tool has non-trivial logic

## Future Setup Checklist

When the owner defines the project direction, consider adding:

- [ ] `.gitignore` appropriate for the language/framework
- [ ] Language-specific package file (`package.json`, `pyproject.toml`, etc.)
- [ ] CI/CD workflow (`.github/workflows/`)
- [ ] `CONTRIBUTING.md` for collaborators
- [ ] Linting and formatting configuration
