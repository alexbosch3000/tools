# Security policy

## Reporting a vulnerability

Do not open a public issue. Report it privately through **Security → Report a vulnerability** on the GitHub repository page. Include the affected app, the steps to reproduce and the impact.

You will get an acknowledgement within 7 days.

## Supported versions

Only the latest commit on `main`, and the images built from it, receive fixes.

## Automated controls

| Control | Tool |
|---|---|
| Static analysis (code and workflows) | CodeQL |
| Vulnerable dependencies in pull requests | Dependency review |
| Dependency and base image updates | Dependabot |
| Known vulnerabilities in dependencies | `make audit` in each app |
| Container image vulnerabilities | Grype, in the image build |
| Build provenance | GitHub artifact attestations |
