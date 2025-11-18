# Epic 20: AI-Assisted Story-Driven Test & Evidence Framework

## Overview
Design and implement a full-stack, story-centric test and evidence framework that spans frontend React, backend services, gateway, and risk engine, with ReportPortal as the unified evidence backend and user stories in `/user-stories` as the single source of truth.

## Scope (Phase 1)
- Parse story markdown in `/user-stories` into a structured story model.
- Plan tests per story across services: `/frontend`, `/backend`, `/gateway`, `/risk-engine`.
- Generate AI-assisted tests for Java (JUnit 5) and React (Jest + RTL).
- Validate and crystallize generated tests into the monorepo with PR-ready structure.
- Introduce a shared Test Data & Mock Registry for deterministic, cross-layer datasets.
- Integrate all test stacks with ReportPortal using a consistent evidence model.
- Export evidence from ReportPortal and generate a static Evidence Dashboard.
- Wire the end-to-end flow into CI for repeatable execution and reporting.

## Out of Scope (Phase 1)
- Non-ReportPortal evidence backends (e.g. custom DB).
- Complex e2e browser automation beyond targeted flows.
- Support for non-Java/non-React services.

## Stories
- [Story 20.1 – Story Parsing & Service Topology Modeling](./story_20_1_story_parser_and_topology.md)
- [Story 20.2 – Test Planning by Service Combination](./story_20_2_test_planning_by_service.md)
- [Story 20.3 – Backend Test Generation (Java/JUnit 5)](./story_20_3_backend_test_generation.md)
- [Story 20.4 – Frontend React Test Generation (Jest + RTL)](./story_20_4_frontend_react_test_generation.md)
- [Story 20.5 – Cross-Service Flow Tests](./story_20_5_cross_service_flow_tests.md)
- [Story 20.6 – Code Validation & Test Crystallization](./story_20_6_code_validation_and_crystallization.md)
- [Story 20.7 – Test Data & Mock Registry](./story_20_7_test_data_and_mock_registry.md)
- [Story 20.8 – ReportPortal Evidence Integration (Java & JS)](./story_20_8_reportportal_evidence_integration.md)
- [Story 20.9 – Evidence Export & Static Dashboard](./story_20_9_evidence_export_and_static_dashboard.md)
- [Story 20.10 – CI/CD Integration for Unified Evidence](./story_20_10_ci_cd_integration_unified_evidence.md)
- [Story 20.11 – Documentation & Authoring Templates](./story_20_11_documentation_and_templates.md)

## Acceptance Criteria Mapping
| Acceptance Theme                          | Story    |
|-------------------------------------------|----------|
| Story parsing & validation                | 20.1     |
| Service-aware test planning               | 20.2     |
| Backend test generation                   | 20.3     |
| Frontend React test generation            | 20.4     |
| Cross-service flow tests                  | 20.5     |
| Code validation & crystallization         | 20.6     |
| Test data & mock registry                 | 20.7     |
| Java & JS ReportPortal integration        | 20.8     |
| Evidence export & static dashboard        | 20.9     |
| CI/CD wiring for evidence                 | 20.10    |
| Documentation & templates                 | 20.11    |

## Risks / Notes
- Requires careful alignment of metadata (story IDs, datasets, services) across languages and services.
- Misconfigured ReportPortal attributes could lead to misleading coverage views.
- Cross-service flows can increase test runtime if not scoped appropriately.

## Future Enhancements (Backlog Seeds)
- Support for additional services (e.g. analytics) and languages.
- Pluggable test generators for other frameworks (e.g. Cypress, Playwright).
- Richer static dashboards with per-PR diffs and change impact views.
