# Epic 06 - CI Orchestration and Artifacts

## Overview
Builds resilient CI workflows that execute all automated test tiers, consolidate artifacts, surface Allure results, and provide feedback loops on every pull request.

## Business Value
- Ensures regressions are caught before merging code to main.
- Provides consistent CI signals and downloadable artifacts for reviewers.
- Establishes the backbone that other governance and reporting features depend on.

## Scope
In Scope:
- Multi-job GitHub Actions workflow spanning frontend and backend test tiers.
- Artifact collection and upload for Allure, coverage, and logs.
- PR feedback mechanisms (status checks, summary comments, links).
Out of Scope:
- Long-running performance or load test pipelines (future enhancement).
- Non-GitHub CI providers.

## Domain Terms
| Term | Definition |
|------|------------|
| Matrix Job | Parallel CI run executing the same steps across services or environments. |
| Artifact Bundle | Collected outputs zipped and uploaded for later retrieval. |
| Status Check | Required GitHub check gating merges on CI success. |

## Core Flow
Trigger workflow on PR/main -> Install dependencies for each stack -> Execute categorized tests with tagging -> Collect Allure, coverage, logs -> Upload artifacts and set PR status.

## Stories
- Story 6.1 - Design CI Workflow Topology Covering All Test Tiers
- Story 6.2 - Implement Backend Test Jobs With Allure Artifact Uploads
- Story 6.3 - Implement Frontend Test Jobs With Allure Artifact Uploads
- Story 6.4 - Add Combined Summary Step Linking Allure Reports in PR Checks
- Story 6.5 - Harden Workflow With Retries, Timeouts, and Caching

## Acceptance Criteria Mapping
| Story | Theme | Key Acceptance |
|-------|-------|----------------|
| 6.1 | Architecture | Workflow YAML supports PR and main triggers, matrix across services, and reusable actions. |
| 6.2 | Backend | Backend job runs unit/integration/contract suites, collects results, fails build on test error. |
| 6.3 | Frontend | Frontend job runs unit/integration/E2E suites headless, attaches screenshots/videos, and uploads Allure results. |
| 6.4 | Feedback | PR check comment links to Allure artifact; status reflects pass/fail with clear messaging. |
| 6.5 | Resilience | Workflow includes dependency caching, retry wrappers for flaky steps, and overall runtime meets target SLA. |

## Quality Approach
- Dry-run workflows in draft PRs before requiring status checks.
- Schedule nightly pipeline verifying long-run stability.
- Monitoring on workflow failure rate with alerting to platform channel.

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Increased CI duration | Use parallelization, caching, and job chunking; review thresholds regularly. |
| Flaky environment setup | Containerize steps or pin images to reduce variance. |
| Artifact storage limits | Prune history, compress artifacts, and enforce retention policies. |
