# Story 20.4 – Frontend React Test Generation (Jest + RTL)

**As the AI test generator for the frontend**,  
I want to generate Jest + React Testing Library tests in `/frontend` based on the story test plan  
So that React components have story-aligned, deterministic coverage for acceptance criteria and scenarios.

## ✅ Acceptance Criteria
- For each `TestPlan` that includes the `frontend` service, the generator can produce Jest test files under `/frontend`, for example:
  - `/frontend/src/__tests__/{ComponentName}.story-<storyId>.test.tsx` or an equivalent convention.
- Each generated frontend test file:
  - Uses Jest and React Testing Library (RTL) imports consistent with the existing frontend setup.
  - Contains at least one `test(...)` per relevant acceptance criterion or test scenario for the frontend.
  - Includes comment-based story metadata at the top, e.g.:
    ```ts
    // @StoryRef: Story 3.2 – Validation & Business Rules
    // @ServicesInvolved: frontend, gateway, backend
    ```
- Generated tests do not perform real network calls; they use:
  - Stable API mocks drawn from the shared Mock & Fixture Registry (see Story 20.7), or
  - A local mock layer that can be later wired to the registry.
- Tests are deterministic:
  - Running them multiple times with the same mocks and data yields consistent results.
- The generator can be run in:
  - **Normal mode**: writes or updates test files.
  - **Dry-run mode**: reports the files and tests that would be created.
- Generated tests pass TypeScript and linting checks as per `/frontend` configuration.

## 🧪 Test Scenarios
1. **Generate tests for a single component story**  
   Given a `TestPlan` with `frontend` service and a mapping to component `CDSTradeForm`  
   When the generator runs  
   Then it creates a Jest/RTL test file for `CDSTradeForm` with tests that reflect the story’s acceptance criteria.

2. **Inject story metadata comments**  
   Given a story with ID `Story 3.2` and services `frontend, backend`  
   When the generator creates a test file  
   Then the file contains `@StoryRef` and `@ServicesInvolved` comments with those values.

3. **Use API mocks instead of real HTTP**  
   Given that the story involves calls to the gateway API  
   When the generated tests run  
   Then they stub the network layer (e.g., via MSW or fetch mocks) and do not hit real services.

4. **Dry-run mode**  
   Given multiple planned frontend tests across components  
   When the generator runs in dry-run mode  
   Then it lists the test file paths and test names that would be generated without modifying the filesystem.

5. **Lint and type safety compliance**  
   Given generated test files  
   When the frontend lint and TypeScript checks run  
   Then there are no syntax, type, or lint errors attributable to generated tests.

## 🛠 Implementation Guidance
- Implement the React test generator as a service that consumes `TestPlan` and a configuration mapping stories to React components (config file, tags, or heuristic mapping).
- Use existing frontend tooling (ts-jest / Vite Jest setup) and RTL helpers where available.
- Start with simple, focused tests (render + basic interactions + assertions) so humans can extend them.
- Keep the generator’s behavior configurable (file naming, folder structure, metadata comment format).

## 📦 Deliverables
- Frontend test generator implementation under `/test-evidence-framework` or a tooling subfolder.
- Configuration for mapping stories to components and for controlling test file layout.
- Unit tests for the generator (e.g., verification of file content and metadata comments).
- Short usage guide in the epic README showing how to generate frontend tests for a story.

## ⏭ Dependencies / Links
- Depends on Story 20.2 `TestPlan` outputs.
- Will use datasets and mocks defined in Story 20.7 (Test Data & Mock Registry).
- Frontend tests will feed into Story 20.8 (ReportPortal Evidence Integration) once wired.
