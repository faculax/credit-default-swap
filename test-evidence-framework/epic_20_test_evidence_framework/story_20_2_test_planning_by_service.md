# Story 20.2 – Test Planning by Service Combination

**As the test-evidence orchestrator**,  
I want to derive a concrete test plan from each parsed story and its `servicesInvolved`  
So that I know exactly which services and test types must be generated and executed for that story.

## ✅ Acceptance Criteria
- For each `StoryModel`, a `TestPlan` object is created that includes:
  - The story identifier and title.
  - A list of `plannedServices[]` drawn from `{ frontend, backend, gateway, risk-engine }`.
  - For each service, a set of planned test types (e.g. `unit`, `component`, `api`, `integration`, `flow`).
- The mapping from `servicesInvolved[]` to `TestPlan` follows these rules:
  - `[frontend]` → frontend `component` and/or `unit` tests only.
  - `[backend]` → backend `unit` and `integration` tests.
  - `[gateway]` → gateway `api/contract` tests.
  - `[risk-engine]` → risk-engine `unit` and `integration` tests.
  - `[frontend, gateway, backend]` → frontend component tests, gateway API tests, backend integration tests, plus at least one cross-service `flow` test definition.
  - Any combination including `risk-engine` adds risk-engine `unit` / `integration` tests to the plan.
- For each acceptance criterion and test scenario, the `TestPlan` records which services are expected to cover it.
- The planner exposes a query API (e.g. `TestPlanCatalog`) that supports:
  - Listing all plans for a given story.
  - Listing all stories that require tests in a given service (e.g., all stories requiring `risk-engine`).
- Planner behavior is covered by automated tests for:
  - Single-service stories (frontend-only, backend-only, risk-engine-only).
  - Multi-service stories (frontend + gateway + backend, backend + risk-engine).
  - Stories with missing or invalid `servicesInvolved` (planner rejects or flags them clearly).

## 🧪 Test Scenarios
1. **Frontend-only story**  
   Given a story with `servicesInvolved = [frontend]`  
   When the planner builds a `TestPlan`  
   Then it includes only `frontend` with test types `component` and/or `unit` and excludes all other services.

2. **Backend-only story**  
   Given a story with `servicesInvolved = [backend]`  
   When the planner builds a `TestPlan`  
   Then it includes `backend` with `unit` and `integration` test types, and no frontend/gateway/risk-engine entries.

3. **Full-stack flow story**  
   Given a story with `servicesInvolved = [frontend, gateway, backend]`  
   When the planner builds a `TestPlan`  
   Then it includes:
   - `frontend` with `component` tests,
   - `gateway` with `api/contract` tests,
   - `backend` with `integration` tests,
   - At least one explicit `flow` test definition linking these services.

4. **Risk-engine-centric story**  
   Given a story with `servicesInvolved = [risk-engine]`  
   When the planner builds a `TestPlan`  
   Then it schedules `unit` and `integration` tests for `/risk-engine` and does not plan frontend or gateway tests.

5. **Invalid servicesInvolved**  
   Given a story where parsing produced an invalid or empty `servicesInvolved`  
   When the planner processes it  
   Then it fails fast with a descriptive error and does not produce a `TestPlan`.

## 🛠 Implementation Guidance
- Implement planning as a pure function from `StoryModel` → `TestPlan`, plus a `TestPlanCatalog` for aggregation and querying.
- Keep mapping rules in a configuration structure so they can be evolved without changing code.
- Consider representing test types as an enum or string union (`unit`, `component`, `api`, `integration`, `flow`).
- Do not tie the planner directly to any specific language implementations; this plan is consumed by Java and JS generators separately.

## 📦 Deliverables
- `TestPlan` and `TestPlanCatalog` types/interfaces.
- Implementation of the planner that maps `StoryModel` to `TestPlan`.
- Unit tests covering the mapping rules and error cases.
- Brief documentation in the epic README linking Story 20.1 parsing outputs to the planner.

## ⏭ Dependencies / Links
- Depends on Story 20.1’s `StoryModel` and `StoryCatalog`.
- Feeds into Stories 20.3, 20.4, and 20.5 for actual test generation.
