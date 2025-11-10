# NFR Backlog (Release Chain Hardening)

This document captures the initial non-functional requirements backlog for the release-chain-hardening epic. Use this as the canonical list for planning and prioritization. Each item includes category, owner, priority (High/Medium/Low), and notes.

Review cadence: bi-weekly during the release-planning sync.

## Prioritization rubric
- Impact x Effort (High impact, Low effort = quick wins)
- Categories: Security, Performance, Reliability, Compliance, Operability

## Starter NFRs

1. Linting on PRs and Release Branches
   - Category: Code Quality
   - Owner: Platform / Engineering
   - Priority: High
   - Notes: Implemented via `.github/workflows/lint.yml` (super-linter). Local `scripts/run-lint.sh` included.

2. Secrets & SAST Scans
   - Category: Security
   - Owner: Security/Platform
   - Priority: High
   - Notes: `security.yml` added with gitleaks and semgrep; heavy CodeQL gated to release branches.

3. Schema Smoke Check on PRs
   - Category: Reliability
   - Owner: Backend Team
   - Priority: High
   - Notes: `schema-smoke-check.yml` runs Flyway validate/migrate in ephemeral Postgres.

4. Documentation Rendering Policy
   - Category: Operability
   - Owner: Platform
   - Priority: Medium
   - Notes: Rendering workflow conditionally triggered (release branches or manual) — adjust per CI quotas.

5. Policy-as-Code (OPA/Conftest)
   - Category: Policy Enforcement
   - Owner: Platform/Security
   - Priority: Medium
   - Notes: Add Conftest checks for infra manifests and OPA policies for RBAC/CI rules.

6. Data Lineage Instrumentation
   - Category: Compliance / Observability
   - Owner: Data Engineering
   - Priority: Medium
   - Notes: Add tracing points on ETL/migration boundaries; track dataset lineage IDs.

7. Service Health Telemetry & Remediation Signals
   - Category: Reliability
   - Owner: SRE / Platform
   - Priority: High
   - Notes: Standardize Micrometer/OpenTelemetry metrics and define alerting playbooks.

8. Code Quality Gates (Coverage + Static Analysis Thresholds)
   - Category: Code Quality
   - Owner: Engineering
   - Priority: Medium
   - Notes: Define thresholds (e.g., block < 80% test coverage for new code), add to CI.

9. Dependencies CVE Scan & Upgrade Policy
   - Category: Security
   - Owner: Security/Engineering
   - Priority: High
   - Notes: Use `dependabot` or GitHub's dependency scanning and schedule weekly sweep.

10. Secrets Rotation & Credential Management
    - Category: Security
    - Owner: Infrastructure
    - Priority: High
    - Notes: Integrate with Vault or Secrets Manager; remove credentials from code.

## Ownership and Workflow
- Keep this file in `docs/` and reference from `AGENTS.md` and `CONTRIBUTING.md`.
- Owners are responsible for triage, PRs, and pulling items into sprint planning.

## Next Actions
- Assign owners and add GitHub Issues for the top 5 items.
- Implement missing automation (pre-commit, Conftest policies) as follow-ups.
