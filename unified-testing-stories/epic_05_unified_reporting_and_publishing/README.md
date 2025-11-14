# Epic 05 - Unified Reporting and Publishing

## Overview
Delivers a single Allure-powered reporting portal that aggregates results from all modules and publishes continuously to GitHub Pages for long-term visibility.

## Business Value
- Gives stakeholders a stable URL to monitor quality before releases.
- Preserves historical trends, enabling regression detection over time.
- Reduces context switching by centralizing frontend and backend reporting outputs.

## Scope
In Scope:
- Merge frontend and backend Allure result sets into a unified report.
- Automate HTML report generation and publishing to GitHub Pages with history retention.
- Provide a landing page explaining navigation and taxonomy.
Out of Scope:
- Building custom dashboards beyond Allure capabilities.
- Authentication or access controls beyond defaults (handled separately if required).

## Domain Terms
| Term | Definition |
|------|------------|
| History Bundle | Allure history directory enabling trend charts. |
| Publishing Workflow | Automated process pushing artifacts to GitHub Pages branch. |
| Landing Page | Entry document linking to latest report and module subsections. |

## Core Flow
Collect Allure results from all modules -> Merge results and copy history -> Generate HTML -> Publish to GitHub Pages -> Update landing page links.

## Stories
- Story 5.1 - Define Allure Report Merge Strategy Across Modules
- Story 5.2 - Automate Allure HTML Generation With History Retention
- Story 5.3 - Publish Reports to GitHub Pages Main Branch Pipeline
- Story 5.4 - Create Reporting Landing Page With Navigation Aids
- Story 5.5 - Document Report Access and Troubleshooting Steps

## Acceptance Criteria Mapping
| Story | Theme | Key Acceptance |
|-------|-------|----------------|
| 5.1 | Aggregation | Merge script handles duplicate test IDs, attaches module labels, and exits non-zero on failure. |
| 5.2 | Automation | Build produces HTML plus history artifacts and stores them for reuse; supports incremental updates. |
| 5.3 | Publishing | Main branch workflow pushes to `gh-pages` (or target branch) and invalidates caches; retries on transient failures. |
| 5.4 | UX | Landing page highlights latest report, module filters, and instructions for stakeholders. |
| 5.5 | Enablement | README documents how to rebuild locally, recover history folder, and debug publishing issues. |

## Quality Approach
- Integration tests covering merge script scenarios (multiple services, missing results, conflicts).
- Dry-run pipeline verifying GitHub Pages deploy without manual steps.
- Monitoring of Pages availability with automated alerts on failures.

## Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Publishing clashes from concurrent builds | Use atomic deploy workflow with lockfiles or queuing strategy. |
| History corruption leading to lost trends | Backup history folder as part of artifact retention policy. |
| Public exposure of sensitive data | Sanitize logs/attachments and consider private Pages or alternative hosting if needed. |
