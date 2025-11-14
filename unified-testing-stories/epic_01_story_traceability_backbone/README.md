# Epic 01 - Story-to-Test Traceability Backbone

## Overview
Establishes a canonical, enforceable mapping between product backlog items and automated tests across all services so anyone can prove story coverage in seconds.

## Business Value
- Makes Definition of Done auditable and repeatable for every story line item.
- Accelerates reviews by letting engineers, QA, and product verify coverage at a glance.
- Enables downstream analytics on coverage gaps and test health.

## Scope
In Scope:
- Canonical story identifier rules and validation tooling.
- Backend and frontend tagging schemes consumable by Allure.
- Generation of an aggregated story-to-test traceability artifact per pipeline run.
Out of Scope:
- Two-way sync with external ALM systems (tracked separately).
- Manual test case capture or UAT tracking.

## Domain Terms
| Term | Definition |
|------|------------|
| Story ID | Unique identifier for backlog work items (e.g., PROJ-123). |
| Traceability Matrix | Machine-generated mapping of story IDs to automated tests and last-known result. |
| Coverage Label | Test metadata linking executions to story IDs and test types. |

## Core Flow
Story is planned with agreed ID -> Engineers tag automated tests with matching label -> CI validates tagging rules -> Allure surfaces story-centric views -> Traceability matrix artifact is published.

## Stories
- Story 1.1 - Adopt Canonical Story ID Syntax & Validation
- Story 1.2 - Backend Test Tagging & Allure Story Labels
- Story 1.3 - Frontend Test Tagging & Decorators
- Story 1.4 - Unified Story Label Conventions Across Test Types
- Story 1.5 - Automated Traceability Matrix Export in CI

## Acceptance Criteria Mapping
| Story | Theme | Key Acceptance |
|-------|-------|----------------|
| 1.1 | Governance | Repository linting rejects non-conforming story IDs; documented format consumed by teams. |
| 1.2 | Backend | JUnit tests include story tags; Allure report shows linked stories for backend suites. |
| 1.3 | Frontend | Jest/Cypress specs emit story labels; Allure output retains association. |
| 1.4 | Consistency | Shared helper/util ensures type + story labels exist for unit, integration, contract, E2E suites. |
| 1.5 | Reporting | CI job produces JSON/CSV matrix listing story IDs, associated tests, last status, artifact persisted. |

## Quality Approach
- Static analysis rule or Git hook validating story ID usage in commits and branch names.
- Snapshot tests on generated traceability matrix schema.
- Cross-service smoke run confirming story filter works end-to-end in Allure.

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Teams forget to tag tests | Provide code templates, IDE snippets, and CI failure guidance. |
| Conflicting story formats across repos | Central configuration shared via package/module enforced in pipelines. |
| Traceability artifact drift | Version artifact schema and add regression tests to guard against breaking changes. |
