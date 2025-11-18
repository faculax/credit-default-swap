# Story 20.7 – Test Data & Mock Registry

**As the test data steward**,  
I want a shared registry of datasets and API mocks that spans backend and frontend test layers  
So that tests are deterministic, repeatable, and aligned across services.

## ✅ Acceptance Criteria
- A **Backend Test Data Registry** exists under `/backend` or a shared test module that:
  - Defines named datasets (e.g. `cds-validation-standard-v1`, `cds-credit-event-flow-v1`).
  - Each dataset includes:
    - A unique name and version.
    - A checksum or hash for integrity.
    - Domain objects or DB seed scripts (SQL, JSON, or Java builders).
    - Metadata describing what the dataset covers (e.g., "10 CDS trades with maturity dates ranging 2025–2030").
- A **Frontend Mock & Fixture Registry** exists under `/frontend/src/__mocks__` or similar that:
  - Defines named API mocks aligned with backend datasets (e.g. `mock-cds-validation-standard-v1`).
  - Each mock includes:
    - Matching unique name and version.
    - Sample HTTP responses (JSON) or MSW handlers.
    - Metadata linking it to the corresponding backend dataset.
- Cross-layer consistency:
  - A dataset name/version used in a backend integration test corresponds to the same logical data in a frontend mock.
  - The registry exposes a simple API or lookup table so generators and tests can reference datasets by name rather than inline literals.
- The registry is versioned:
  - Changes to a dataset result in a new version (e.g. `v1` → `v2`).
  - Old versions remain available for reproducibility.
- Documentation describes:
  - How to add a new dataset.
  - How to use datasets in backend and frontend tests.
  - How to ensure cross-layer consistency.

## 🧪 Test Scenarios
1. **Register a new backend dataset**  
   Given a requirement for a new dataset `cds-physical-settlement-v1`  
   When a developer adds it to the Backend Test Data Registry with metadata and domain objects  
   Then it is accessible by name in backend tests and assigned a unique checksum.

2. **Register a corresponding frontend mock**  
   Given the backend dataset `cds-physical-settlement-v1`  
   When a developer adds a matching frontend mock `mock-cds-physical-settlement-v1`  
   Then the mock includes API responses that align with the backend dataset and is linked by name/version.

3. **Reference dataset in generated tests**  
   Given a generated backend test for Story 4.3  
   When the test runs  
   Then it uses dataset `cds-cash-settlement-v1` from the registry rather than hardcoded inline data.

4. **Reference mock in generated React tests**  
   Given a generated React test for Story 4.3  
   When the test runs  
   Then it loads mock `mock-cds-cash-settlement-v1` from the Frontend Mock Registry and does not perform real HTTP calls.

5. **Version a dataset update**  
   Given an existing dataset `cds-validation-standard-v1`  
   When requirements change and the dataset is modified  
   Then a new version `cds-validation-standard-v2` is created and tests can continue to use `v1` until migrated.

## 🛠 Implementation Guidance
- Start with a simple, file-based registry (JSON or YAML manifests + data files).
- Backend datasets can be Java classes, JSON files, or SQL scripts depending on test harness conventions.
- Frontend mocks can be JSON files or TypeScript/JS modules exporting MSW handlers.
- Keep the registry structure flat and discoverable (e.g., `/backend/src/test/resources/datasets/{name}/`, `/frontend/src/__mocks__/api/{name}/`).
- Consider using a central `registry.json` that indexes all datasets and mocks with metadata.

## 📦 Deliverables
- Backend Test Data Registry structure and at least 2 sample datasets.
- Frontend Mock & Fixture Registry structure and at least 2 sample mocks.
- Central registry index (e.g., `registry.json` or equivalent).
- Helper APIs or utility functions for loading datasets by name (Java + TypeScript).
- Documentation on adding, versioning, and using datasets.

## ⏭ Dependencies / Links
- Used by Stories 20.3, 20.4, and 20.5 (generated tests and flow tests).
- Feeds into Story 20.8 (evidence records include dataset metadata).
