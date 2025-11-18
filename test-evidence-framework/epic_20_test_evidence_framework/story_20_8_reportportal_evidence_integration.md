# Story 20.8 – ReportPortal Evidence Integration (Java & JS)

**As the evidence collector**,  
I want all backend and frontend tests to push structured evidence to ReportPortal with consistent story, service, and dataset metadata  
So that we have a unified, queryable evidence store for cross-service test results.

## ✅ Acceptance Criteria
- A common **Evidence Model** is defined that applies to both Java and JS test stacks, capturing:
  - **Story metadata**: `storyId`, `title`, acceptance criteria (index + text), test scenarios (index + text).
  - **Service metadata**: `servicesInvolved[]` (from story), `serviceUnderTest` (e.g. `backend`, `frontend`, `gateway`, `risk-engine`), `layer` (e.g. `unit`, `integration`, `component`, `flow`).
  - **Data metadata**: Dataset name, version, checksum, record count.
  - **Environment metadata**: Environment name (local, ci, dev, staging), service versions, build/commit IDs.
  - **Assertion metadata**: Individual assertion details (description, expected, actual, result).
  - **Execution metadata**: Test name (class + method or file + test name), start/end timestamps, result (PASS/FAIL/SKIP), error/stack trace if failed.
- For **Java (JUnit 5) tests** in `/backend`, `/gateway`, `/risk-engine`:
  - ReportPortal's JUnit 5 agent is configured and active.
  - A custom `ReportPortalEvidenceExtension` is implemented that:
    - Reads story annotations (e.g. `@StoryRef`, `@AcceptanceCriterion`, `@ServiceUnderTest`) and dataset metadata.
    - Attaches ReportPortal attributes for all evidence fields (e.g., `storyId`, `servicesInvolved`, `serviceUnderTest`, `layer`, `datasetName`, `datasetVersion`, `environment`, `commit`).
    - Logs assertion details as RP logs or attachments.
    - Ensures evidence is pushed even on test failures.
- For **JS/React tests** in `/frontend`:
  - A Jest ReportPortal reporter is configured (or a custom Node client calling ReportPortal's API).
  - The reporter maps Jest's `test`/`describe` structure to ReportPortal's launch/suite/test hierarchy.
  - Story metadata from comment-based annotations (`@StoryRef`, `@ServicesInvolved`) is extracted and attached as RP attributes.
  - Frontend tests push the same attribute set as backend tests: `storyId`, `servicesInvolved`, `serviceUnderTest="frontend"`, `layer="component"`, dataset, environment, commit.
  - Tests can optionally log assertion details (or rely on auto-mapping from Jest's `expect` failures).
- ReportPortal **extensions and dashboards**:
  - Launch naming convention: `"{branch}-{service}-{YYYYMMDDhhmm}"` or similar.
  - Saved filter searches for:
    - Story-based view (filter by `storyId`).
    - Service-based view (filter by `serviceUnderTest`).
    - Layer-based view (filter by `layer`).
  - Optional custom widgets showing:
    - Coverage by `storyId`.
    - Coverage by `servicesInvolved`.
    - Frontend/Backend parity (stories with both FE & BE tests).

## 🧪 Test Scenarios
1. **Java test pushes story evidence to ReportPortal**  
   Given a JUnit 5 test for Story 3.2 in `/backend` with `@StoryRef` and `@ServiceUnderTest` annotations  
   When the test runs with the ReportPortal agent active  
   Then the evidence is pushed to RP with attributes: `storyId=Story 3.2`, `serviceUnderTest=backend`, `layer=integration`, etc.

2. **React test pushes story evidence to ReportPortal**  
   Given a Jest test for Story 3.2 in `/frontend` with `@StoryRef` comment metadata  
   When the test runs with the RP reporter active  
   Then the evidence is pushed to RP with attributes: `storyId=Story 3.2`, `serviceUnderTest=frontend`, `layer=component`, etc.

3. **Story-based filter in ReportPortal**  
   Given multiple tests across services for Story 3.2  
   When a user filters ReportPortal by `storyId=Story 3.2`  
   Then results include both backend and frontend tests for that story.

4. **Service-based dashboard view**  
   Given tests from multiple stories across all four services  
   When a user views the service-based dashboard widget  
   Then it shows test counts and pass/fail status per service (`frontend`, `backend`, `gateway`, `risk-engine`).

5. **Evidence includes dataset metadata**  
   Given a test using dataset `cds-validation-standard-v1`  
   When the test completes  
   Then the evidence record in RP includes `datasetName=cds-validation-standard-v1`, `datasetVersion=v1`, and checksum.

## 🛠 Implementation Guidance
- Start with ReportPortal's official JUnit 5 agent and extend it with a custom extension for story-specific metadata.
- For Jest, evaluate existing community reporters (e.g., `jest-reportportal`) or write a thin wrapper calling RP's REST API.
- Store configuration (RP URL, project name, API key) in environment variables or a central config file.
- Document how to run tests with RP integration locally and in CI.
- Consider a dry-run mode that simulates RP pushes without actually sending data, for testing the integration.

## 📦 Deliverables
- `ReportPortalEvidenceExtension` for Java (JUnit 5).
- Jest ReportPortal reporter integration for React tests.
- Configuration files for RP (connection, project, launch naming).
- Sample custom RP widgets or dashboard configurations (JSON/YAML).
- Documentation on running tests with RP locally and interpreting results.

## ⏭ Dependencies / Links
- Depends on Stories 20.3, 20.4, 20.5, 20.6 (generated and validated tests).
- Uses datasets from Story 20.7 (Test Data & Mock Registry).
- Enables Story 20.9 (Evidence Export & Static Dashboard) by providing queryable evidence via RP's API.
