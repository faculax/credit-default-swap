# Epic 07 - Governance and Quality Gates

## Overview
Enforces policies that prevent untested stories from merging, applies coverage thresholds, and updates Definition of Done so the unified testing platform remains effective.

## Business Value
- Maintains confidence that every user story has automated validation.
- Prevents regression debt by failing fast when coverage erodes.
- Aligns engineering workflow with stakeholder expectations for quality.

## Scope
In Scope:
- CI checks correlating story IDs in branches/commits with tagged tests.
- Minimum coverage thresholds using JaCoCo and Jest coverage reports.
- Updates to Definition of Done, code review checklist, and onboarding materials.
Out of Scope:
- Manual QA workflows or UAT gatekeeping.
- Integration with external compliance tooling (future work).

## Domain Terms
| Term | Definition |
|------|------------|
| Story Enforcement Script | Automation verifying that referenced story IDs have tagged tests.
| Coverage Threshold | Minimum acceptable percentage for lines/branches exercised by automated tests.
| Definition of Done | Team agreement on criteria required before marking work complete.

## Core Flow
CI inspects PR metadata -> Story enforcement script validates coverage mapping -> Coverage reports evaluated against thresholds -> Build fails or passes -> Documentation keeps policies transparent.

## Stories
- Story 7.1 - Detect Story IDs in Branches and Commits
- Story 7.2 - Fail CI When Story IDs Lack Tagged Tests
- Story 7.3 - Enforce Backend Coverage Thresholds With JaCoCo
- Story 7.4 - Enforce Frontend Coverage Thresholds With Jest
- Story 7.5 - Update Definition of Done and Review Checklists

## Acceptance Criteria Mapping
| Story | Theme | Key Acceptance |
|-------|-------|----------------|
| 7.1 | Detection | Script parses PR title, branch name, and commits; exposes detected IDs as build output.
| 7.2 | Enforcement | Build fails with actionable message when no matching tests found; exemptions require override workflow.
| 7.3 | Coverage | JaCoCo report uploaded; build fails under agreed percentages with team-notified summary.
| 7.4 | Coverage | Frontend coverage thresholds enforced via Jest; results attached to PR artifact.
| 7.5 | Governance | Contributing guide includes new DoD; checklist updated and communicated during team sync.

## Quality Approach
- Unit tests on enforcement scripts covering positive and negative cases.
- Integration test running full CI flow on sample repo demonstrating pass/fail behavior.
- Periodic review of coverage thresholds to adjust as codebase evolves.

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| False positives blocking PRs | Provide override flag with approval process; log diagnostics for debugging.
| Coverage metrics gamed with trivial tests | Pair thresholds with review of meaningful assertions and mutation testing backlog.
| Policy fatigue | Automate reminders and share dashboards demonstrating benefits to reinforce adoption.
