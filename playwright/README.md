# Playwright Functional Test Suite

This folder contains the initial Playwright setup for functional, accessibility, and deterministic risk-oriented UI tests.

## Structure
- `playwright.config.ts` - Global configuration, multi-browser projects.
- `helpers/` - Reusable helpers (`auth.ts`, `seed.ts`).
- `tests/` - Spec files grouped by epic.

## Tags
Use tags in test titles:
- `@smoke` - Fast core path validation
- `@regression` - Broader functional coverage
- `@a11y` - Accessibility scans with axe-core
- `@risk` - Deterministic pricing / analytics checks

Filter examples:
```bash
npx playwright test --grep "@smoke"
```

## Environment Variables
- `BASE_URL` default `http://localhost:3000`
- `AUTH_USER` / `AUTH_PASS` credentials for login
- `SEED` deterministic run seed (defaults to 42)

## Prerequisites

**CRITICAL**: The frontend depends on the full Docker stack (backend, gateway, database, risk-engine). Tests will fail if services aren't running.

### Start the Stack

From the **project root** directory:

```powershell
docker-compose up -d
```

Wait ~30-60 seconds for health checks to pass. Verify with:

```powershell
docker-compose ps
```

All services should show "healthy" or "Up" status.

### Stop the Stack

```powershell
docker-compose down
```

## Install & Run Tests

```bash
cd playwright
npm install
npm test
```

**Global Setup Hook**: Before tests run, Playwright verifies:
- Frontend accessible at `http://localhost:3000`
- Backend health at `http://localhost:8080/actuator/health`
- Gateway health at `http://localhost:8081/actuator/health`

If any service is down, tests fail fast with a clear error message.

## Next Steps
1. Flesh out selectors to match actual application components.
2. Add API contract snapshot assertions (hashing JSON responses).
3. Integrate performance metrics collection (navigation timing + custom logs).
4. Wire into CI pipeline with separate jobs for smoke vs full regression.

## Determinism
The `seed.ts` helper provides a simple LCG for repeatable sequences to verify reproducibility of pricing logic or simulation-driven UI states.

## Accessibility
Axe is only loaded inside accessibility-tagged tests to keep the smoke path fast.

---
Generated scaffold; expand with additional epics after pilot validation.

## Skip Philosophy
Tests skip (not fail) when a required UI element or route is not yet implemented in the MVP. This keeps the pipeline green while surfacing capability gaps in reports. Once a feature lands, the skip automatically disappears because the locator becomes present. Use the helper `skipIfMissing(locator, reason, testInfo)` for consistency.

## Scripts Added
```powershell
npm run test:headed      # Watch tests in a real browser
npm run test:ui          # Interactive test explorer
npm run test:visual      # UI explorer + headed
npm run test:smoke       # Only @smoke tagged tests
npm run test:accessibility # Only @a11y tests
npm run trace:show       # Open first found trace.zip in viewer
```

## Always-On Tracing
`trace: 'on'` is enabled in `playwright.config.ts`. Switch to `'retain-on-failure'` in CI if artifacts grow too large.

## CI/CD Integration

### GitHub Actions Example

```yaml
name: E2E Tests

on: [push, pull_request]

jobs:
  playwright:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Start Docker stack
        run: |
          docker-compose up -d
          # Wait for health checks
          timeout 90 bash -c 'until docker-compose ps | grep -q "healthy"; do sleep 2; done'
      
      - name: Setup Node
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Install Playwright dependencies
        working-directory: ./playwright
        run: |
          npm ci
          npx playwright install --with-deps
      
      - name: Run tests
        working-directory: ./playwright
        run: npm test
      
      - name: Upload test artifacts
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-report
          path: playwright/playwright-report/
      
      - name: Teardown
        if: always()
        run: docker-compose down -v
```

### Azure Pipelines Example

```yaml
trigger:
  - main

pool:
  vmImage: 'ubuntu-latest'

steps:
- task: Docker@2
  inputs:
    command: 'up'
    arguments: '-d'

- script: |
    timeout 90 bash -c 'until docker-compose ps | grep -q "healthy"; do sleep 2; done'
  displayName: 'Wait for Docker stack'

- task: NodeTool@0
  inputs:
    versionSpec: '18.x'

- script: |
    cd playwright
    npm ci
    npx playwright install --with-deps
    npm test
  displayName: 'Run Playwright tests'

- task: PublishTestResults@2
  condition: always()
  inputs:
    testResultsFormat: 'JUnit'
    testResultsFiles: 'playwright/test-results/junit.xml'

- script: docker-compose down -v
  condition: always()
  displayName: 'Teardown stack'
```

