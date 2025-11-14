# Epic 02 - Test Architecture Standardization

## Overview
Creates consistent directories, naming, and metadata for unit, integration, contract, and E2E tests so every repository looks and behaves the same.

## Business Value
- Reduces onboarding time by aligning project layouts and scripts.
- Improves CI reliability because jobs can discover tests deterministically.
- Enables cross-service reporting slices by test type without bespoke adapters.

## Scope
In Scope:
- Folder and namespace conventions for backend and frontend test suites.
- Shared documentation and code templates enforcing the conventions.
- Allure label schema for test type, service/module, and environment context.
Out of Scope:
- Rewriting existing tests beyond what is required to move them into the standard layout.
- Implementing new business-level test coverage (handled in other epics).

## Domain Terms
| Term | Definition |
|------|------------|
| Test Taxonomy | Categorization of tests by level (unit, integration, contract, E2E). |
| Module Label | Identifier applied to tests indicating owning service or package. |
| Golden Path | The standard scaffolding that all new tests must adopt. |

## Core Flow
Define canonical folder structure -> Provide templates and scripts -> Refactor existing suites into structure -> Enforce through lint rules and CI checks.

## Stories
- Story 2.1 - Define Backend Test Folder & Package Conventions
- Story 2.2 - Define Frontend Test Folder & Naming Conventions
- Story 2.3 - Introduce Shared Test Type Label Schema
- Story 2.4 - Migrate Legacy Tests Into Standard Layout
- Story 2.5 - Document Golden Path With Examples

## Acceptance Criteria Mapping
| Story | Theme | Key Acceptance |
|-------|-------|----------------|
| 2.1 | Backend | Spring Boot repos expose `unit`, `integration`, `contract` directories; build tooling locates suites automatically. |
| 2.2 | Frontend | React repo differentiates unit/integration/E2E directories; npm scripts target each category. |
| 2.3 | Metadata | Allure results show test type labels for every execution, regardless of runner. |
| 2.4 | Migration | Existing suites relocated with passing builds; CI verifies no orphaned tests remain outside structure. |
| 2.5 | Documentation | Contributing guide updated with folder diagrams, naming rules, and quick-start snippets. |

## Quality Approach
- Repository linters verifying directory paths for new tests.
- Sample Allure report screenshot stored to confirm labeling works.
- Smoke builds validating test discovery commands succeed with new layout.

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Hidden legacy tests missed during migration | Use file search scripts and codeowners reviews to confirm coverage. |
| Developer friction adapting to templates | Offer generator CLI and pair sessions during initial rollout. |
| Divergent module naming across services | Maintain single source of truth in shared configuration consumed by all repos. |
