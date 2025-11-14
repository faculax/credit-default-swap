PRD: Unified Test Coverage & Reporting Platform
*For Existing React + Spring Boot Codebase*

---

## 1. Overview

### 1.1 Problem Statement

The current React frontend and Spring Boot backend services lack:

* A consistent, enforced mapping between user stories and automated tests.
* A unified way to run and categorize tests (unit, integration, contract, E2E).
* A single, easy-to-browse reporting UI for overall quality status.
* A permanent, shareable location for test reports and historical trends.

This makes it harder to:

* Know if a given feature/story is truly covered.
* Catch regressions early.
* Onboard new engineers or external reviewers.
* Demonstrate quality to stakeholders.

### 1.2 Product Vision

Create a **Unified Test Coverage & Reporting Platform** where:

* Every user story is mapped to at least one automated test.
* Tests are consistently categorized (unit, integration, contract, E2E).
* All tests feed into **Allure reports**.
* Allure reports are generated automatically per branch/PR and **published to GitHub Pages** for the main branch.
* Test coverage and story-to-test mapping become first-class citizens of the development process.

### 1.3 Goals

1. **Traceability**: 100% of user stories have associated automated tests traceable in code and reports.
2. **Visibility**: Allure reports available for:

   * Each PR / feature branch (as CI artifacts).
   * Main branch (persisted on GitHub Pages with history).
3. **Consistency**: Standardised test structure and tagging across:

   * React frontend.
   * All Spring Boot services.
4. **Quality Gates**:

   * CI fails if new/changed stories have no test mapping.
   * CI fails if test suites fail.
   * Optionally, coverage thresholds enforced.

### 1.4 Non-Goals

* Rewriting existing business logic from scratch.
* Replacing existing ticketing/issue-tracking tools (Jira, GitHub Issues, etc.).
* Full manual test management (this PRD focuses on automated tests).
* Full-blown analytics/BI implementation (basic Allure-based reporting is in scope).

---

## 2. Stakeholders & Personas

### 2.1 Stakeholders

* **Engineering Lead / Tech Lead** – Owns technical direction and quality standards.
* **Developers (Frontend & Backend)** – Implement stories, write tests, maintain pipelines.
* **QA / SDETs** – Design test strategies, ensure coverage and reliability.
* **Product Owners / BAs** – Need visibility that stories are “truly done”.
* **DevOps / Platform Engineers** – Maintain CI pipelines and GitHub Pages infrastructure.

### 2.2 Personas

1. **Backend Developer (Spring Boot)**
   Wants: easy way to add tests, see failures, and verify story coverage for a service.

2. **Frontend Developer (React)**
   Wants: clear place to write component/E2E tests, see UI regression issues early.

3. **QA Engineer**
   Wants: a single Allure dashboard showing which stories are covered and which are gaps.

4. **Product Owner**
   Wants: confidence that a story is only marked “Done” when associated tests pass.

---

## 3. Current vs Target State

### 3.1 Current State (Assumed)

* React app with some unit/E2E tests (possibly Jest, React Testing Library, Cypress/Playwright).
* Multiple Spring Boot services with some unit and integration tests (JUnit 5, MockMVC, etc.).
* CI already exists (e.g., GitHub Actions) but:

  * Tests may not be standardized.
  * No central Allure reporting.
  * No enforced story-to-test mapping.
* No GitHub Pages-backed test report site.

### 3.2 Target State

* **Standard test folders & naming conventions** across repos.
* **Allure integrated** into:

  * Backend tests (JUnit 5).
  * Frontend tests (Jest/Cypress/Playwright adapters).
  * Contract tests (Pact or Spring Cloud Contract).
* **Traceability model**:

  * Each user story has an ID (e.g., `PROJ-123`, `US-456`, or GitHub issue #).
  * Test methods/scenarios tagged with the story ID (e.g., JUnit tags, Cypress tags).
  * Allure displays story IDs and allows filtering.
* **CI Pipelines**:

  * On PR: run tests, generate Allure report as artifact, link in PR checks.
  * On main: run tests, generate & publish Allure report to GitHub Pages with history.
* **Governance**:

  * “No story without tests” enforced via CI, code review, or both.

---

## 4. Functional Requirements

### 4.1 User Story → Test Mapping

**FR-1**: Define a **canonical story identifier format**

* E.g., `PROJ-###` (Jira), `US-###`, or `ISSUE-###` for GitHub.
* Must be unique and stable.

**FR-2**: Introduce a **mapping convention**:

* Backend tests: use JUnit tags or custom annotations, e.g.:

  * `@Tag("story:PROJ-123")`
  * or `@Story("PROJ-123")` (Allure annotation)

* Frontend tests:

  * Jest/Cypress: `describe('[story:PROJ-123] User logs in', () => { ... })`
  * or use Allure decorators/annotations where supported.

**FR-3**: Story-to-test traceability:

* Allure report must:

  * Display story IDs as labels.
  * Allow filtering/grouping test cases by story ID.
* A story is considered **covered** if:

  * At least one automated test is tagged with that story ID.
  * That test passes in the main-branch pipeline.

**FR-4**: Traceability matrix (simplified):

* Generate, in CI, an exported artifact (JSON/CSV) listing:

  * Story ID.
  * Tests referencing it.
  * Last run status.
* Optional: integrate with an external tracker (Jira/GitHub Issues) later.

---

### 4.2 Test Types & Structure

**FR-5**: Test types supported:

1. **Unit Tests**

   * Backend: JUnit 5, focusing on single classes.
   * Frontend: Jest unit tests for components/utilities.

2. **Integration Tests**

   * Backend: Spring Boot test slices or full-context tests hitting DBs/mocks.
   * Frontend: Component-level integration tests (e.g., React Testing Library).

3. **Contract Tests**

   * Backend: Consumer–producer contract tests.
   * Tooling: Pact or Spring Cloud Contract (assume one and standardize).

4. **E2E/UI Tests**

   * Frontend: Cypress or Playwright hitting deployed app (or dev server).

**FR-6**: Folder conventions (example):

* Backend service:

  * `src/test/java/.../unit/...`
  * `src/test/java/.../integration/...`
  * `src/test/java/.../contract/...`
* Frontend:

  * `src/__tests__/unit/...`
  * `src/__tests__/integration/...`
  * `e2e/...`

**FR-7**: Each test suite must **clearly declare its type** via:

* Allure labels (e.g., `@Epic("unit")`, `@Feature("integration")`).
* Or a naming/tagging convention recognized by Allure.

---

### 4.3 Allure Integration

**FR-8**: Backend Allure integration:

* Use Allure JUnit 5 adapter.
* Test runs produce Allure results files (e.g., in `build/allure-results` or `target/allure-results`).
* Maven/Gradle tasks for:

  * `test` (run tests).
  * `allureReport` (generate HTML).
  * `allureServe` (local dev).

**FR-9**: Frontend Allure integration:

* Install Allure adapter for chosen framework (Jest/Cypress/Playwright).
* Allure results generated into a known directory (e.g., `allure-results` at project root).
* Ensure multiple test runners can be merged into a single Allure report.

**FR-10**: Merging results:

* CI step aggregates backend + frontend `allure-results` folders into a single directory before generating the HTML report.

---

### 4.4 CI Pipeline (GitHub Actions or equivalent)

**FR-11**: CI Workflow on Pull Requests

For every PR:

1. Checkout code.
2. Install dependencies (backend & frontend).
3. Run:

   * Backend unit/integration/contract tests.
   * Frontend unit/integration/E2E tests (where feasible in CI).
4. Collect Allure result files.
5. Generate Allure HTML report (or store results for later rendering).
6. Upload:

   * Allure results and/or HTML as build artifacts.
7. Add PR status checks:

   * Tests must pass.
   * (Optionally) Allure summary comment with link to artifact.

**FR-12**: CI Workflow on Main Branch

On pushes to `main`:

1. Execute same test workflow as PR.
2. Generate final Allure HTML report (merged across all modules).
3. Publish the report to **GitHub Pages**:

   * Use a dedicated branch (e.g., `gh-pages`).
   * Maintain Allure `history` folder for trends.
4. Optionally, update a simple `index.html` at root with:

   * Links to “Latest Report”.
   * Links to “Service-specific Reports” if needed.

---

### 4.5 GitHub Pages Public Site

**FR-13**: Public-facing reports (internal or external):

* `https://<org>.github.io/<repo>/` shows:

  * A landing page:

    * “Latest Allure Test Report”.
    * Short explanation of structure.
  * Link to Allure main index.

**FR-14**: Report navigation:

* Allure UI must allow:

  * Filter by:

    * Story ID.
    * Test type (unit, integration, contract, E2E).
    * Module/service.
  * View historical trend graphs (via Allure’s history feature).

---

### 4.6 Governance & Quality Gates

**FR-15**: “No Story Without Tests” Enforcement

* For each commit/PR that references story IDs (e.g., in branch name or commit message):

  * CI script checks if there is at least one test with matching tag/story ID.
  * If none found, **fail build** or mark with warning status.

**FR-16**: Coverage thresholds (optional but recommended)

* Introduce coverage tools:

  * Backend: JaCoCo.
  * Frontend: Jest coverage.
* CI enforces minimum coverage per module (e.g., 70–80% to start).
* Failing coverage fails the PR pipeline.

**FR-17**: Definition of Done (DoD) update

For any story to be marked as “Done”:

* Automated tests exist and are tagged with the story ID.
* All tests pass in the main pipeline.
* Allure report for that build is green for that story.

---

## 5. Non-Functional Requirements

**NFR-1: Performance**

* CI pipelines (per PR) should typically complete within X minutes (define threshold, e.g., 15–20).
* Allure report generation must add minimal overhead (target < 2 minutes).

**NFR-2: Reliability**

* CI workflows must be stable and deterministic.
* Allure publishing must handle:

  * Concurrent builds on main.
  * Network failures (retry steps).

**NFR-3: Security**

* If GitHub Pages is public:

  * Ensure no secrets or sensitive data appear in test logs.
* Mask credentials in test outputs.
* Consider using GitHub environments or internal-only Pages if needed.

**NFR-4: Developer Experience**

* Local commands must be simple:

  * `./gradlew test allureReport` (backend).
  * `npm test` / `npm run test:e2e` followed by `allure generate` (frontend).
* Documentation for:

  * How to tag tests with story IDs.
  * How to view local Allure reports.
  * How to interpret CI failures.

---

## 6. Example User Stories (for this Platform)

1. **US-TEST-001 – View test results per story**

> As a **developer**, I want to view all tests linked to a given story ID in the Allure report so that I can easily verify coverage before marking a story as done.

* Acceptance Criteria:

  * Given a story ID (`PROJ-123`), I can filter in Allure and see all related tests.
  * Each test clearly shows its type (unit/integration/contract/E2E).
  * The overall status for that story is visible.

2. **US-TEST-002 – Publish test reports to GitHub Pages**

> As a **product owner**, I want a stable URL for the latest test report so that I can quickly check the health of the system before releases.

* Acceptance Criteria:

  * A URL (GitHub Pages) shows the latest Allure report for the main branch.
  * Historical trends are available for at least the last N builds.
  * The URL is updated automatically on every successful main-branch build.

3. **US-TEST-003 – Enforce tests for new stories**

> As a **tech lead**, I want CI to fail when a PR implements a story without any automated tests so that we maintain a high quality bar.

* Acceptance Criteria:

  * If PR title or branch name contains `PROJ-123`, CI checks that there is at least one test tagged `PROJ-123`.
  * If none found, CI fails with a clear message.
  * If found, CI continues to normal test execution.

4. **US-TEST-004 – Unified Allure report across services**

> As a **QA engineer**, I want a single report that aggregates frontend and all backend services so that I can see overall system quality in one place.

* Acceptance Criteria:

  * Allure report shows modules/services as labels.
  * Tests from React and each Spring Boot service appear together.
  * Filtering by module is possible.

---

## 7. Implementation Phases & Milestones

### Phase 1 – Foundations

* Decide on story ID format and tagging convention.
* Integrate Allure into:

  * At least one backend service (JUnit 5).
  * React test stack (Jest/Cypress/Playwright).
* Create local scripts to run tests and generate Allure reports.

**Deliverable**: Local Allure reports for one service + frontend.

---

### Phase 2 – CI Integration

* Add CI workflows for:

  * Running tests on PR and main.
  * Generating and archiving Allure results.
* Validate that Allure artifacts are available from PR builds.

**Deliverable**: PR pipelines with Allure artifacts accessible.

---

### Phase 3 – GitHub Pages Publishing

* Configure GitHub Pages (or equivalent hosting).
* Add pipeline step to:

  * Merge Allure results from all modules.
  * Publish HTML to Pages.
  * Maintain history.

**Deliverable**: Public (or internal) URL with the latest Allure report for main.

---

### Phase 4 – Governance & Quality Gates

* Implement “no story without tests” checker (simple script or small service).
* Integrate coverage thresholds.
* Update Definition of Done and team agreements.
* Add documentation and onboarding guides.

**Deliverable**: Enforced policy via CI; documented process.

---

## 8. Risks & Mitigations

* **Risk**: Initial setup complexity across multiple services.
  **Mitigation**: Pilot in one service + frontend, then roll out with a template.

* **Risk**: CI times increase significantly.
  **Mitigation**: Split pipelines, use parallel jobs, nightly full test runs if needed.

* **Risk**: Teams ignore story tagging.
  **Mitigation**: CI enforcement + code review checklist + training.

* **Risk**: Exposure of sensitive logs on public GitHub Pages.
  **Mitigation**: Scrub logs, make Pages private/internally accessible, or host behind VPN if needed.

---

## 9. Assumptions & Open Questions

### Assumptions

* Repositories are on GitHub and GitHub Actions is available.
* Allure is acceptable as the standard reporting tool.
* Story IDs are already used in an issue tracker (Jira, GitHub Issues, etc.).

---