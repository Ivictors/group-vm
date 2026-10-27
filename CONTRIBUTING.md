# Contributing, Git, and quality

## Required workflow
- Every change references a GitHub Issue with an objective, scope, out-of-scope items, dependencies, and verifiable criteria.
- `docs/PRODUCT.md` and `docs/ARCHITECTURE.md` define behavior and responsibilities; consult the approved GitHub Issue for the objective, dependencies, and acceptance criteria.
- Use one branch per issue in the project directory and a PR to `main`. Do not develop on `main`.
- Branches: `feat/NN-description`, `fix/NN-description`, `chore/NN-description`.
- Commits: Conventional Commits, without secrets or generated artifacts.

```bash
git fetch origin
# With a clean directory and an up-to-date main branch, create the issue branch:
git switch main
git pull --ff-only origin main
git switch -c chore/NN-description
# Implement and run applicable local tests/checks before pushing;
# use the service's Maven Wrapper when available. Replace NN and description.
git status --short
git diff --check
# Commit and PR require human approval.
git push -u origin chore/NN-description
gh pr create --web --base main --head chore/NN-description
# After merge and issue closure, once you confirm no work remains unmerged:
git switch main
git pull --ff-only origin main
git branch -d chore/NN-description
# Check the GitHub setting before relying on automatic remote-branch deletion.
```

## GitHub branch controls
The public repository has an active [Protect main ruleset](https://github.com/Ivictors/group-vm/rules/25986333) for `main`. It requires a PR, the `foundation` check on an up-to-date branch, and resolved review conversations. It blocks force pushes and deletion and has no bypass actors. The required approval count is zero while there is one maintainer; the author reviews the PR diff and evidence and records that review. This review is not independent, and authors cannot approve their own PRs. Verify the effective GitHub rules before relying on this configuration, since repository settings can change.

Use `area:<service-name>` labels for the eleven services in `docs/ARCHITECTURE.md`, plus `area:frontend`, `area:infrastructure`, and `area:governance`. The undated delivery milestones are Foundation, Commerce MVP, Intelligence MVP, and Launch readiness. Labels identify ownership; milestones group delivery goals, not implementation authorization.

## Verification before push and merge

- Before every push, run existing tests, builds, and checks applicable to the change and available locally; fix failures before publishing.
- Record commands, results, and reasons for checks not run in the PR. CI-only tests must run and pass before merge; missing or mocked tests do not prove real integration.
- For API, framework, or tool decisions, consult current official documentation and record the link, relevant version, and consultation date in the PR.
- Review the README, contracts, and operational documentation when corresponding behavior changes.

## Definition of Ready
Approved issue, clear dependencies, defined contract, testable acceptance criteria, and identified risks.

## Definition of Done
- Acceptance criteria demonstrated; unit, integration, contract, and E2E tests as appropriate for the change.
- Build and required checks pass; logs contain no sensitive data.
- Authorization, validation, idempotency, failures, and observability reviewed where applicable.
- Versioned OpenAPI/event schemas and README/operational documentation updated when the corresponding behavior changes.
- PR includes risks, evidence, tests actually run, and a rollback plan when needed.
- Author review recorded, conversations resolved, and merge authorized. Record completion in the issue.

## PR review
Check service boundaries, data ownership, contract compatibility, migrations, replay behavior, retries, and partial failures.

## CI policy
CI must check changed services individually, using Testcontainers when needed, and run distributed integrations that are not feasible locally. JaCoCo, Spotless, Gitleaks, static analysis, tests, and dependency/image scans are controls to implement through issues; the current workflow checks only the foundation. Deploy only immutable images. No automatic production rollout without approval.

## Independence and daily development
- Each service has its own build, database, code, and tests. HTTP or event mocks are allowed; tests do not import another service's entities, repositories, or migrations.
- Run only the services and infrastructure required by the task. Kafka, Kong, Keycloak, and Kubernetes need not stay running on the notebook.
- Do not confuse mocked tests with real integration tests. State which ran and which remain for CI or a remote environment.
