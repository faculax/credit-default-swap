# Story 20.3 – Backend Test Generation (Java/JUnit 5)

**As the AI test generator for backend services**,  
I want to generate JUnit 5 test classes in `/backend`, `/gateway`, and `/risk-engine` based on the story test plan  
So that each story has explicit backend coverage tied to its acceptance criteria and scenarios.

## ✅ Acceptance Criteria
- For every `TestPlan` entry that includes `backend`, `gateway`, or `risk-engine`, the generator can produce candidate Java test classes using JUnit 5.
- Generated tests are written into the appropriate module and package structure, for example:
  - `/backend/src/test/java/...` for core backend tests.
  - `/gateway/src/test/java/...` for gateway API/contract tests.
  - `/risk-engine/src/test/java/...` for risk engine tests.
- Each generated test class:
  - Includes annotations or metadata linking it back to the source story (e.g., comments or custom annotations with `storyId`, `title`).
  - Contains at least one test method per relevant acceptance criterion or scenario for that service.
  - Uses existing test harness patterns in the repo (e.g. Spring Boot test annotations, MockMvc, Testcontainers) when applicable.
- Generated tests compile successfully and pass a basic formatting/checkstyle step.
- The generator respects idempotency:
  - Re-running the generator for the same story and service does not create duplicate classes or conflicting methods.
  - If a test file already exists and has been manually edited, the generator either:
    - Appends new tests in a safe region, or
    - Produces a clear report indicating manual intervention is required.
- There is a dry-run mode that:
  - Shows which files and tests would be generated without actually writing to disk.

## 🧪 Test Scenarios
1. **Generate backend tests for a new story**  
   Given a `TestPlan` with `backend` integration tests for a story  
   When the generator runs in normal mode  
   Then it creates a new JUnit 5 test class under `/backend/src/test/java/...` with methods mapped to each acceptance criterion.

2. **Generate gateway API tests**  
   Given a `TestPlan` that includes `gateway` API/contract tests  
   When the generator runs  
   Then it creates a gateway test class that follows existing API test conventions and links to the story metadata.

3. **Generate risk-engine tests**  
   Given a `TestPlan` that includes `risk-engine` unit/integration tests  
   When the generator runs  
   Then it produces JUnit tests in `/risk-engine` with appropriate use of risk engine helpers/mocks.

4. **Dry-run mode**  
   Given a test plan for multiple services  
   When the generator runs in dry-run mode  
   Then it lists the test files and methods that would be created without writing them.

5. **Idempotent regeneration**  
   Given a story for which tests have already been generated  
   When the generator runs again  
   Then it does not create duplicate files or override existing tests silently, and reports any planned changes.

## 🛠 Implementation Guidance
- Implement the backend generator as a service that consumes `TestPlan` objects and repository conventions (root package names, base test classes).
- Use simple string templates or a templating engine for Java class generation.
- Favor small, composable test methods that can be refined manually by developers.
- Keep generator configuration (e.g., package prefixes, base classes) in a central config file under `/test-evidence-framework`.

## 📦 Deliverables
- Backend test generator implementation.
- Configuration for mapping services to module paths and base packages.
- Unit tests for the generator logic (e.g., mapping acceptance criteria to method names, idempotency behaviour).
- Short usage guide in the epic README describing how to invoke the backend generator.

## ⏭ Dependencies / Links
- Depends on Story 20.2 `TestPlan` outputs.
- Will feed into Story 20.6 (Code Validation & Test Crystallization) and Story 20.8 (ReportPortal integration) once tests begin to run.
