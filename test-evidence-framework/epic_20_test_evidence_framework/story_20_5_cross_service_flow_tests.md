# Story 20.5 – Cross-Service Flow Tests

**As the test-evidence orchestrator**,  
I want to define and execute cross-service flow tests when a story involves multiple services  
So that end-to-end behavior across frontend, gateway, backend, and risk engine is validated against the story’s test scenarios.

## ✅ Acceptance Criteria
- For any story whose `servicesInvolved[]` contains more than one service, the `TestPlan` can include one or more `flow` test definitions.
- The flow test definition includes:
  - The participating services (e.g. `frontend → gateway → backend`).
  - The key steps of the flow (high-level sequence aligned with the story’s test scenarios).
  - The datasets/mocks to use at each layer.
- There is a concrete location in the repo for implementing flow tests (e.g. under `/gateway/src/test/java/...` or `/tests/flows/...`), and this location is documented.
- Flow tests can be executed in at least one of the following modes:
  - **Integrated services**: running real services locally (e.g. via `docker-compose`) and exercising them end-to-end.
  - **Hybrid**: some services real, others mocked.
- Flow tests re-use the shared Test Data & Mock Registry so that datasets are consistent across layers.
- Example flows are implemented for at least one story involving `frontend + gateway + backend` and one involving `backend + risk-engine`.
- Flow tests surface evidence compatible with the same story-centric model (story ID, services, dataset, assertions) for later ReportPortal integration.

## 🧪 Test Scenarios
1. **Frontend → Gateway → Backend trade capture flow**  
   Given a story with `servicesInvolved = [frontend, gateway, backend]` for CDS trade capture  
   When a flow test is executed  
   Then it simulates a user submitting a trade via the frontend, going through the gateway to the backend, and verifies that the persisted trade and responses match the story’s acceptance criteria.

2. **Backend → Risk Engine pricing flow**  
   Given a story with `servicesInvolved = [backend, risk-engine]` for pricing  
   When a flow test is executed  
   Then it triggers a backend operation that calls the risk engine and verifies the pricing outputs corresponding to the shared dataset.

3. **Hybrid mode with mocked frontend**  
   Given an environment where the frontend is not running  
   When a flow test runs in hybrid mode  
   Then it mocks the frontend calls at the gateway layer but still drives gateway → backend → risk-engine interactions end-to-end.

4. **Dataset consistency across layers**  
   Given a flow that uses dataset `cds-validation-standard-v1`  
   When the flow test runs  
   Then the same logical dataset is used for backend DB seed, gateway contracts, and frontend API mocks.

5. **Error handling in flows**  
   Given a story with error scenarios (e.g. validation failures)  
   When a flow test exercises those scenarios  
   Then it verifies that error responses and UI states match the story’s acceptance criteria across all participating services.

## 🛠 Implementation Guidance
- Start with a small number of representative stories that span multiple services.
- Decide on a default home for flow tests (gateway module vs dedicated `flows` module) and codify it in configuration.
- Reuse existing local orchestration (e.g. `docker-compose.local.yml`) where possible to run multi-service stacks.
- Design the flow abstraction so that additional flows can be added declaratively (e.g. YAML or JSON definitions) in future iterations.

## 📦 Deliverables
- Flow test definition model (e.g. `FlowTestPlan`).
- Implementation of at least one end-to-end flow test for `frontend + gateway + backend`.
- Implementation of at least one end-to-end or hybrid flow test for `backend + risk-engine`.
- Documentation of how to run flow tests locally and in CI.

## ⏭ Dependencies / Links
- Depends on Story 20.2 `TestPlan` (to identify multi-service stories).
- Uses datasets and mocks from Story 20.7 (Test Data & Mock Registry).
- Produces evidence that will be consumed by Story 20.8 (ReportPortal Evidence Integration) and Story 20.9 (Evidence Export & Static Dashboard).
