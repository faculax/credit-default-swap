Here’s a fully revised PRD that bakes in:

* **Front-end React tests** as first-class citizens (alongside backend tests).
* **ReportPortal as the evidence backend** (plus extending it with custom attributes/widgets).
* **Multi-service topology** per story (**/frontend, /backend, /risk-engine, /gateway**).
* **Stories located in `/user-stories`**.

I’ll write it as a fresh, self-contained document.

---

# Product Requirements Document (PRD)

## AI-Assisted, Story-Driven Test & Evidence Framework

*(Frontend + Backend, Multi-Service, ReportPortal-Backed)*

---

## 0. Document Control

| Version | Date       | Author       | Notes                                                        |
| ------- | ---------- | ------------ | ------------------------------------------------------------ |
| 2.0     | 2025-11-17 | System Owner | Revised PRD with React, multi-service topology, ReportPortal |

---

## 1. Overview

### 1.1 Purpose

Define a **Test & Evidence Framework** that:

1. Reads **markdown user stories** from `/user-stories`, using your existing story format.
2. Uses an **LLM agent** to generate tests for:

   * **Backend services** (`/backend`, `/risk-engine`, `/gateway`) – primarily Java/JUnit 5.
   * **Frontend React application** (`/frontend`) – primarily Jest + React Testing Library (and optionally component/e2e tests).
3. Ensures tests are **idempotent and deterministic**, with a shared Test Data / Mock Registry.
4. Captures **rich, queryable evidence** for each test execution:

   * Story, acceptance criteria, test scenarios
   * Services involved (frontend/backend/risk-engine/gateway)
   * Data, environment, component/service versions
   * Assertions and results
5. Stores evidence in **ReportPortal**, extended with:

   * Custom attributes (story, services, layer)
   * Custom dashboards/widgets for story and service coverage.
6. Publishes a **static Evidence Dashboard** (e.g. via GitHub Pages) summarising coverage and results for non-technical stakeholders.

This PRD is intended for both **humans and AI agents** to decompose into epics and stories.

---

### 1.2 Business Problem

Currently:

* Tests are fragmented between front-end and back-end, with no single, story-centric view.

* It’s hard to answer:

  > “For this story in `/user-stories`, which React components, backend services, and flows have automated test evidence?”

* Multi-service flows (frontend → gateway → backend → risk-engine) are not clearly represented in tests or evidence.

* AI-generated tests risk drift and non-determinism over time.

* Non-engineers (PM, audit, leadership) have poor visibility into what’s truly covered.

We need a **story-first, full-stack**, evidence framework.

---

### 1.3 Solution Overview

We will build:

1. **Story Parser & Topology Model**

   * Reads stories from `/user-stories`.
   * Extracts:

     * Story metadata.
     * Acceptance Criteria & Test Scenarios.
     * **Services involved** (e.g. frontend, backend, risk-engine, gateway).

2. **Dual-Stack AI Test Generators**

   * **Backend generator**: JUnit 5 tests in `/backend`, `/risk-engine`, `/gateway`.
   * **Frontend generator**: Jest + React Testing Library tests in `/frontend` (unit + component, optionally integration/e2e).
   * Respect story’s declared service combination.

3. **Test Validation & Crystallization**

   * Validate code (Java + JS/TS).
   * Write tests to appropriate service directories.
   * Open PRs with clear coverage summaries.

4. **Test Data & Mock Registry**

   * Shared registry for:

     * Backend datasets (DB / domain objects).
     * Frontend **API mocks** and fixtures (matching backend datasets).
   * Guarantees consistent test data across layers.

5. **Evidence Collection Layer**

   * Java: JUnit 5 + ReportPortal agent + custom extension.
   * JS/React: Jest reporter integration to ReportPortal (or custom Node client).
   * Captures story, criteria/scenarios, services, data, environment, assertions.

6. **ReportPortal Integration & Extensions**

   * ReportPortal as central evidence store.
   * Custom attributes: `storyId`, `services`, `layer=frontend/backend/gateway/risk-engine`, dataset, environment, etc.
   * Custom dashboards/widgets for:

     * Story coverage.
     * Service coverage.
     * Frontend vs backend coverage.

7. **Static Evidence Dashboard**

   * Export evidence from ReportPortal (via API).
   * Generate static HTML (hosted via GitHub Pages) for story-by-story evidence view.

---

### 1.4 Objectives & Success Metrics

* **Full-stack coverage**

  * For in-scope stories, tests exist covering **all declared services** (frontend + relevant backend services).
* **Traceability**

  * For any story in `/user-stories`, we can answer within **5 minutes**:

    > “Which React components and backend endpoints were tested, with what data, and when?”
* **Determinism**

  * Re-running tests (same commit, same dataset/mocks) produces identical results.
* **ReportPortal adoption**

  * 100% of automated tests (backend + frontend) report to ReportPortal with correct attributes.
* **Stakeholder-friendly visibility**

  * Non-engineers can use the static dashboard to understand coverage by story and service.

---

## 2. System & Repository Context

### 2.1 Repository Structure (Monorepo)

Assumed structure:

```text
/user-stories/     # All markdown stories (single source of truth)
/frontend/         # React application
/backend/          # Core backend service (Java/Spring Boot, etc.)
/risk-engine/      # Risk calculation / CDS engine (Java?)
/gateway/          # API gateway / edge service
/tools/            # Test generators, ReportPortal clients, etc. (optional)
/ci/               # CI scripts/config (optional)
```

### 2.2 Services & Layers

* **Frontend**:

  * React, talking to `/gateway` via HTTP APIs.
* **Gateway**:

  * Exposes APIs to frontend, orchestrates backend & risk-engine calls.
* **Backend**:

  * Business logic, persistence, CDS trade storage, etc.
* **Risk Engine**:

  * Pricing, risk, CDS pricing, NPV/spread calc.

Stories may involve **one or several** of these services. Tests must mirror that.

---

## 3. Story Model & Conventions

### 3.1 Story Structure (Existing)

Stories in `/user-stories` follow this pattern:

```markdown
# Story 3.2 – Validation & Business Rules

**As the system**,  
I want to enforce CDS-specific validation and provide clear error messages  
So that only coherent, bookable trades are persisted.

## ✅ Acceptance Criteria
- ...
- ...

## 🧪 Test Scenarios
1. ...
2. ...

## 🧱 Services Involved   (NEW – see below)
- frontend
- gateway
- backend

## 🛠 Implementation Guidance
- ...

## 📦 Deliverables
- ...

## ⏭ Dependencies / Links
- ...
```

> **Note:** `## 🧱 Services Involved` is the recommended section going forward.
> For existing stories, services may be inferred from `Implementation Guidance` or tags until they are updated.

### 3.2 Parsed Story Model

The **Story Parser** must extract:

* `storyId`: `"Story 3.2"` (and normalized ID like `STORY_3_2`).
* `title`: `"Validation & Business Rules"`.
* `actor`, `capability`, `benefit`.
* `acceptanceCriteria[]`:

  * Ordered list of top-level bullets under `## ✅ Acceptance Criteria`.
* `testScenarios[]`:

  * Ordered list of numbered items under `## 🧪 Test Scenarios`.
* `servicesInvolved[]`:

  * List of strings from `## 🧱 Services Involved`.
  * Allowed values: `frontend`, `gateway`, `backend`, `risk-engine`.
  * For stories missing this section, either:

    * Use a fallback mapping (config file), or
    * Mark as incomplete and fail parsing.
* Optional:

  * `implementationGuidance[]`
  * `deliverables[]`
  * `dependencies[]`

### 3.3 Story → Test Planning

Based on `servicesInvolved[]`, the framework must:

* Decide **which kinds of tests to generate**:

  * `frontend` → React tests in `/frontend`.
  * `gateway` → API/contract tests in `/gateway`.
  * `backend` → unit/integration tests in `/backend`.
  * `risk-engine` → unit/integration tests in `/risk-engine`.
* Identify **cross-service integration flows**, e.g.:

  * `frontend + gateway + backend` → full flow tests using:

    * Mocked external systems where necessary.
    * Real inter-service calls in integration environments.

---

## 4. Architecture Overview

### 4.1 High-Level Flow

1. Stories are authored/updated in `/user-stories`.
2. **Story Parser** builds a `StoryModel` with services involved.
3. **AI Test Orchestrator**:

   * For each service in `servicesInvolved[]`,
   * Calls a service-specific **Test Generator** (backend/React/integration).
4. **Test Validators** confirm code quality and constraints.
5. **Test Crystallizer** writes tests into the correct directories and opens PRs.
6. Tests run (locally and in CI) using shared **Test Data & Mock Registry**.
7. **Evidence Collectors** (Java JUnit + Jest reporters) push results into **ReportPortal**.
8. **ReportPortal Extensions**:

   * Provide custom widgets/dashboards for:

     * Coverage by story, by service, by layer.
9. CI pipeline exports aggregated evidence to a **Static Evidence Dashboard**.

---

## 5. Functional Requirements

### EPIC 1 – Story Parsing & Test Planning (FR-1.x)

#### FR-1.1 Story Parsing (Markdown → StoryModel)

**Goal**
Parse stories from `/user-stories` into `StoryModel` objects.

**Requirements**

* Watch/read `*.md` files from `/user-stories`.
* Extract:

  * `storyId`, `title`, role/capability/benefit.
  * `acceptanceCriteria[]` (bullets).
  * `testScenarios[]` (numbered list).
  * `servicesInvolved[]` from `## 🧱 Services Involved`.
* Validation:

  * At least one acceptance criterion or test scenario.
  * `servicesInvolved` is non-empty and values are from the allowed set.
* Provide clear error messages for malformed stories.

---

#### FR-1.2 Test Planning by Service Combination

**Goal**
Determine required test artifacts for each story based on `servicesInvolved[]`.

**Requirements**

* For each story:

  * Build a `TestPlan` indicating:

    * Which codebases need tests:

      * `/frontend`, `/backend`, `/risk-engine`, `/gateway`.
    * Which types of tests:

      * Unit tests.
      * Component tests.
      * API/contract tests.
      * Cross-service flow tests.
* Examples:

  * `servicesInvolved = [frontend]` → React component/tests only.
  * `servicesInvolved = [frontend, gateway, backend]` → React + gateway API tests + backend integration tests.
  * `servicesInvolved = [risk-engine]` → risk calculation unit/integration tests.

---

### EPIC 2 – AI Test Generation (Backend + Frontend) (FR-2.x)

#### FR-2.1 Backend Test Generation (Java/JUnit 5)

**Goal**
Generate JUnit 5 tests in `/backend`, `/risk-engine`, `/gateway`.

**Requirements**

* Input:

  * `StoryModel`, `TestPlan`, tech-specific config (package names, test base classes).
* Output:

  * Java test classes:

    * In `/backend/src/test/java/...`
    * `/risk-engine/src/test/java/...`
    * `/gateway/src/test/java/...`
* Coverage:

  * One or more tests per acceptance criterion and scenario relevant to that service.
  * Include necessary mocks or test harness code.
* Annotations:

  * `@StoryRef(storyId = "Story 3.2", title = "...")`
  * `@AcceptanceCriterion(index = ..., text = "...")`
  * `@TestScenario(index = ..., text = "...")`
  * `@ServiceUnderTest(value = "backend")` (or gateway/risk-engine)
* Deterministic behaviour:

  * No non-deterministic APIs unless tightly controlled.

---

#### FR-2.2 Frontend React Test Generation (Jest + RTL)

**Goal**
Generate React tests in `/frontend` for stories involving the frontend.

**Requirements**

* Input:

  * `StoryModel`, `TestPlan`, map of story → relevant React components (via config, heuristics, or tags).

* Output:

  * Test files in `/frontend` such as:

    * `/frontend/src/__tests__/{ComponentName}.story-3-2.test.tsx`
  * Using:

    * Jest.
    * React Testing Library (RTL).

* Test types:

  * **Unit/component tests**:

    * Rendering relevant component(s).
    * Asserting UI states for each acceptance criterion and scenario.
  * Optional **integration/e2e**:

    * If configured, use Playwright/Cypress and push results to ReportPortal too.

* Mapping:

  * Annotate tests (via comments or decorators where appropriate) with story metadata, e.g.:

    ```ts
    // @StoryRef: Story 3.2 – Validation & Business Rules
    // @ServicesInvolved: frontend, gateway, backend

    test('shows validation error when maturity equals effective date', async () => {
      ...
    });
    ```

* Determinism:

  * Use stable mock responses (from the shared Test Data & Mock Registry).
  * Avoid real network calls (use mock fetch/Axios, MSW, etc.).

---

#### FR-2.3 Cross-Service Flow Tests

**Goal**
Generate tests that exercise the interaction across multiple services.

**Requirements**

* For stories with `servicesInvolved` containing more than one service:

  * Define **flow tests**:

    * Example: frontend → gateway → backend.
  * These may live in:

    * `/gateway/src/test/...` as integration tests, or
    * A dedicated `/tests/flows/...` module.
* Use:

  * Real services (in a docker-compose test stack), or
  * Test doubles for selected services, depending on environment.
* AI generator must:

  * Build end-to-end test scenarios that align with the story’s `TestScenarios[]`.
  * Use the same datasets/mocks across layers.

---

### EPIC 3 – Test Validation & Crystallization (FR-3.x)

#### FR-3.1 Code Validation (Java + JS/TS)

**Goal**
Ensure generated tests are correct, safe, and follow standards.

**Requirements**

* Java:

  * AST parsing / compilation checks.
  * Enforce import & annotation rules.
  * Check banned APIs.
* JS/TS:

  * Run TypeScript/ESLint checks.
  * Ensure tests import RTL, not custom or disallowed helpers.
  * Validate comment-based annotations (StoryRef, ServicesInvolved) or equivalent metadata.

---

#### FR-3.2 Crystallization & PR Creation

**Goal**
Commit validated tests into correct service directories and open PRs.

**Requirements**

* Map from story → service → target file paths:

  * `/backend/src/test/java/...`
  * `/risk-engine/src/test/java/...`
  * `/gateway/src/test/java/...`
  * `/frontend/src/__tests__/...`
* Git/PR behaviour:

  * Branch naming: `feature/tests/story-3-2` or similar.
  * Commit messages summarising tests per service.
  * Single PR per story, with sections:

    * Backend changes.
    * Frontend changes.
    * Flows/integration tests (if any).

---

### EPIC 4 – Test Data & Mock Registry (FR-4.x)

#### FR-4.1 Backend Dataset Registry

**Goal**
Stable datasets for backend services.

**Requirements**

* In `/backend` & `/risk-engine` modules:

  * `TestDataRegistry` and dataset definitions.
* Data elements:

  * Domain objects (e.g. CDS trades).
  * JSON payloads or DB seed scripts.
  * Checksums and metadata.

---

#### FR-4.2 Frontend Mock & Fixture Registry

**Goal**
Stable API mocks and UI fixtures aligned with backend datasets.

**Requirements**

* In `/frontend`, maintain:

  * `MockApiRegistry` with named mocks aligned to backend datasets.

    * E.g. `cds-validation-standard-v1`:

      * a set of mock HTTP responses that match backend dataset `cds-validation-standard-v1`.
* Tests must:

  * Use mocks by **name**, not ad-hoc inline responses.
* Registry metadata:

  * Name, version, checksum.
  * Sample endpoints and responses.

---

#### FR-4.3 Cross-Layer Consistency

**Goal**
Ensure frontend and backend tests refer to the **same logical dataset**.

**Requirements**

* Each logical dataset has:

  * Backend representation (data objects / DB).
  * Frontend representation (API mocks).
* ID/Name must be consistent across layers.
  E.g. `"cds-validation-standard-v1"`.

---

### EPIC 5 – Evidence Collection & ReportPortal Integration (FR-5.x)

#### FR-5.1 Evidence Model

**Goal**
Common evidence model across Java and JS test stacks.

**Requirements**

Each evidence record must include:

* Story metadata:

  * `storyId`, `title`.
  * Acceptance criteria covered (indices + text).
  * Test scenarios covered (indices + text).
* Service metadata:

  * `servicesInvolved[]` from story.
  * `serviceUnderTest` (frontend/backend/gateway/risk-engine/flow).
  * `layer` (e.g. `frontend`, `backend`, `integration`).
* Data metadata:

  * Dataset(s) used:

    * Name, version, checksum, record count.
* Environment metadata:

  * Environment (local, ci, dev, staging).
  * Service version (per service).
  * Build/commit IDs.
* Assertion metadata:

  * Individual assertion details (description, expected/actual, result).
* Execution metadata:

  * Test name (class/file + method/test name).
  * Start/end timestamps.
  * Result (PASS/FAIL/SKIP).
  * Error/stack trace (if failed).

---

#### FR-5.2 Java → ReportPortal Integration

**Goal**
Use ReportPortal’s JUnit 5 agent with custom attributes.

**Requirements**

* Configure ReportPortal for:

  * `/backend`, `/risk-engine`, `/gateway` tests.
* For each test:

  * Attach attributes:

    * `storyId`
    * `storyTitle`
    * `servicesInvolved` (comma-separated or multiple attributes)
    * `serviceUnderTest`
    * `layer`
    * `datasetName`, `datasetVersion`
    * `environment`
    * `commit`
* Implement a `ReportPortalEvidenceExtension`:

  * Reads annotations and dataset metadata.
  * Logs assertion details as RP logs or attachments.
  * Ensures evidence is pushed even on failures.

---

#### FR-5.3 JS/React → ReportPortal Integration

**Goal**
Send Jest/React test results into the same ReportPortal project.

**Requirements**

* Use:

  * A dedicated Jest ReportPortal reporter, or
  * A custom Node client calling ReportPortal’s API.
* Map Jest’s `test`/`describe` structure to:

  * Launch/suite/test in ReportPortal.
* Attach attributes mirroring Java side:

  * `storyId`, `servicesInvolved[]`, `serviceUnderTest="frontend"`, `layer="frontend"`, dataset, environment, commit.
* Expose a simple API/helper so tests can log:

  * Assertion details (or rely on auto mapping from expect failures).

---

#### FR-5.4 ReportPortal Extensions & Dashboards

**Goal**
Extend ReportPortal to support story- and service-centric views.

**Requirements**

* Configure custom:

  * Launch naming: e.g. `"{branch}-{service}-{YYYYMMDDhhmm}"`.
  * Filter saved searches for:

    * Story-based view.
    * Service-based view.
    * Layer-based (frontend/backend/integration).
* Optionally implement:

  * Custom widgets showing:

    * Coverage by `storyId`.
    * Coverage by `servicesInvolved`.
    * Frontend/Backend parity (how many stories have both FE & BE tests).

---

### EPIC 6 – Evidence Export & Static Dashboard (FR-6.x)

#### FR-6.1 ReportPortal Query Client

**Goal**
Programmatic access to story/service evidence stored in ReportPortal.

**Requirements**

* Implement `ReportPortalQueryClient` that:

  * Queries launches and tests by:

    * `storyId`
    * `servicesInvolved`
    * `serviceUnderTest`
    * Time ranges
  * Aggregates evidence into a unified structure for:

    * `stories.json`
    * `story-{id}.json`
* Must support both Java and JS test events.

---

#### FR-6.2 Static Evidence Site Generation

**Goal**
Generate static HTML from exported JSON.

**Requirements**

* Use data from `ReportPortalQueryClient` to build:

  * `index.html`:

    * List of stories with:

      * `storyId`, `title`
      * Services involved
      * Last execution date
      * Status per service (frontend/backend/gateway/risk-engine)
  * `story-{id}.html`:

    * Story details (“As the … I want …”).
    * Acceptance criteria list with coverage badges:

      * `frontend` / `backend` / `flow` coverage.
    * Test scenarios list with coverage badges.
    * Per-service test history (tables for frontend/backend/gateway/risk-engine).
    * Dataset and environment info.
* Styling: simple, responsive CSS.

---

#### FR-6.3 GitHub Pages Deployment

**Goal**
Publish dashboard automatically from CI.

**Requirements**

* GitHub Actions workflow:

  * Run tests across services.
  * Push evidence to ReportPortal.
  * Export story/service evidence via `ReportPortalQueryClient`.
  * Generate static site (`/site` or `/dist`).
  * Publish to `gh-pages` branch (or equivalent).
* Dashboard URL pattern:

  * Base index.
  * Per-story links, e.g. `/story-3-2.html`.

---

### EPIC 7 – CI/CD Integration (FR-7.x)

**Goal**
Make the entire pipeline automatic and repeatable.

**Requirements**

* For each service (`/frontend`, `/backend`, `/risk-engine`, `/gateway`):

  * CI job to run tests with ReportPortal integration.
* On PR:

  * Run relevant tests (based on changed paths).
  * Optionally push evidence to a **PR-specific launch** in ReportPortal.
  * Comment PR with:

    * Links to ReportPortal.
    * (Optional) preview static dashboard for the story.
* On `main`:

  * Full test suite.
  * ReportPortal evidence update.
  * Static dashboard update + deploy.

---

### EPIC 8 – Documentation & Templates (FR-8.x)

#### FR-8.1 Developer & QA Documentation

**Requirements**

* Describe:

  * Repo structure and how stories map to services.
  * How to run tests (backend + frontend) with ReportPortal locally.
  * How to add/modify Test Data & Mock Registry entries.
  * How to interpret ReportPortal dashboards & static Evidence Dashboard.

---

#### FR-8.2 Story & Test Authoring Templates

**Requirements**

* Provide a standard story template (with `Services Involved`).
* Provide guidelines:

  * How to decide `servicesInvolved`.
  * How to write Acceptance Criteria and Test Scenarios that map well to tests.

---

## 6. Non-Functional Requirements

* **Performance**

  * Test runs must be CI-friendly; cross-service flows can be slower but bounded.
* **Reliability**

  * Evidence path failures (e.g. ReportPortal down) must surface clearly.
* **Security**

  * No secrets or personal data in evidence or static dashboards.
  * ReportPortal behind proper auth & TLS.
* **Maintainability**

  * Clear separation between:

    * Story parsing.
    * Test generation.
    * Data/Mock registry.
    * Evidence integration.
* **Extensibility**

  * Ability to add new services (e.g. `/analytics`) with minimal change.
  * Ability to plug in additional test frameworks.

---

 