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

## Install & Run
```bash
cd playwright
npm install
npm test
```

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
