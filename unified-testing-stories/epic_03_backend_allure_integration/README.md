# Epic 03 - Backend Allure Integration

## Overview
Implements Allure reporting end-to-end for all Spring Boot services so backend test executions emit rich metadata and can be aggregated with other stacks.

## Business Value
- Provides actionable failure diagnostics for backend services without digging through raw logs.
- Enables service owners to monitor quality trends across unit, integration, and contract suites.
- Supports unified reporting by emitting consistent Allure result artifacts.

## Scope
In Scope:
- Add Allure JUnit 5 adapters and configuration to each backend service.
- Normalize output directories and Gradle/Maven tasks for generating reports.
- Merge backend Allure results into a single artifact per pipeline.
Out of Scope:
- Creation of new backend test suites (covered in functional epics).
- Custom Allure plugin development beyond configuration.

## Domain Terms
| Term | Definition |
|------|------------|
| Allure Adapter | Library that hooks into JUnit to produce Allure results. |
| Results Directory | File system path storing JSON/XML output consumed by Allure. |
| Composite Report | Aggregated Allure output combining multiple services. |

## Core Flow
Configure adapter dependencies -> Standardize build tasks -> Run backend test suites -> Export Allure results -> Aggregate outputs for downstream publishing.

## Stories
- Story 3.1 - Introduce Allure Dependencies & Plugins for Spring Boot Services
- Story 3.2 - Standardize Backend Allure Results Directory Structure
- Story 3.3 - Provide Gradle/Maven Tasks for Local Allure Generation
- Story 3.4 - Merge Multiple Backend Service Results During CI
- Story 3.5 - Document Backend Failure Triage Using Allure Artifacts

## Acceptance Criteria Mapping
| Story | Theme | Key Acceptance |
|-------|-------|----------------|
| 3.1 | Setup | All backend services compile with Allure adapters and produce results when tests run. |
| 3.2 | Consistency | All services write to `target/allure-results` (or agreed path) and expose location via build configuration. |
| 3.3 | Developer Experience | Single command generates HTML report locally; README updated with usage. |
| 3.4 | Aggregation | CI bundles results from each service into shared artifact; duplicates or collisions handled. |
| 3.5 | Enablement | Playbook created showing how to diagnose flaky tests or failures via Allure UI. |

## Quality Approach
- Pipeline run on sample service verifying Allure output passes JSON schema validation.
- Automated check ensuring results directory exists post-test run.
- Manual spot check of HTML report for tagging fidelity before broad rollout.

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Build time increase due to report generation | Run Allure rendering only when requested; default pipeline stores raw results. |
| Version drift of Allure plugins | Pin versions in shared parent POM/Gradle convention plugin. |
| Flaky metadata when tests spawn threads | Provide guidance on Allure lifecycle usage and wrap async operations with listeners. |
