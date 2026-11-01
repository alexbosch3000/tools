# CLAUDE.md

@AGENTS.md

## Claude Code specifics

- Shared settings are in `.claude/settings.json`: common build and test commands are pre-approved, reading `.env` files and private keys is denied. Personal overrides go to `.claude/settings.local.json` (git-ignored).
- To create an application, use the `new-app` skill (`/new-app <name> <stack>`).
- Before reporting a task as done, run `make ci` in each affected app and report the result.
