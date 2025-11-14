# Unified Label Conventions

## Overview

The unified label schema ensures consistent test labeling across backend (Java/Allure) and frontend (TypeScript/Jest) test suites. This document describes the label validation system and how to use it.

## Label Schema

The authoritative label schema is defined in `unified-testing-config/label-schema.json`:

```json
{
  "testTypes": [
    { "name": "unit", "description": "..." },
    { "name": "integration", "description": "..." },
    { "name": "contract", "description": "..." },
    { "name": "e2e", "description": "..." }
  ],
  "services": [
    { "id": "backend", "name": "..." },
    { "id": "frontend", "name": "..." }
  ],
  "microservices": [
    { "id": "cds-platform", "name": "..." },
    { "id": "gateway", "name": "..." },
    { "id": "risk-engine", "name": "..." },
    { "id": "risk-ui", "name": "..." },
    { "id": "trade-ui", "name": "..." },
    { "id": "settlement-ui", "name": "..." }
  ]
}
```

## Validation Tools

### Backend (Java)

**Class:** `com.creditdefaultswap.testing.validation.LabelValidator`

The `LabelValidator` class validates labels at test execution time:

```java
import com.creditdefaultswap.testing.validation.LabelValidator;

LabelValidator validator = new LabelValidator();

// Validate individual labels
validator.validateTestType("unit");
validator.validateService("backend");
validator.validateMicroservice("cds-platform");

// Validate all labels together
validator.validateLabels("unit", "backend", "cds-platform");

// Check validity without throwing
boolean isValid = validator.isValidTestType("unit");
```

**Integration:**  
The `StoryLabelApplier` automatically validates labels when applying `@StoryId` annotations. Invalid labels will cause test execution to fail with a clear error message.

**Test:** `LabelValidatorTest.java` - 15 test cases covering all validation scenarios

### Frontend (JavaScript)

**Script:** `scripts/validate-labels.mjs`

Validates labels in Jest test output or Allure results:

```bash
# Validate backend Allure results
node scripts/validate-labels.mjs --allure-results backend/allure-results

# Validate frontend Jest output
npm test -- --json --outputFile=test-output.json
node scripts/validate-labels.mjs --jest-output test-output.json

# Fail build on errors
node scripts/validate-labels.mjs \
  --allure-results backend/allure-results \
  --fail-on-error

# Show detailed validation output
node scripts/validate-labels.mjs \
  --allure-results backend/allure-results \
  --verbose
```

**Test:** `scripts/__tests__/validate-labels.test.mjs` - Comprehensive test coverage for validation logic

## Label Reference

### Test Types

| Value | Description | Use Cases |
|-------|-------------|-----------|
| `unit` | Narrow-scope tests focusing on individual classes or functions | Service method tests, utility function tests |
| `integration` | Tests covering interactions between components or layers | Database integration, service-to-service calls |
| `contract` | Consumer and producer contract tests enforcing service APIs | API contract verification, Pact tests |
| `e2e` | End-to-end scenarios exercising the system from user journey | Full workflow tests, UI automation |

### Services

| ID | Name | Description |
|----|------|-------------|
| `backend` | Spring Boot Backend Services | All Java backend microservices |
| `frontend` | React Frontend Application | TypeScript React UI |

### Microservices

| ID | Name | Type |
|----|------|------|
| `cds-platform` | CDS Platform Service | Backend |
| `gateway` | API Gateway Service | Backend |
| `risk-engine` | Risk Engine Service | Backend |
| `risk-ui` | Risk Analytics Frontend | Frontend |
| `trade-ui` | Trade Capture Frontend | Frontend |
| `settlement-ui` | Settlement Frontend | Frontend |

## Adding New Labels

To add new test types, services, or microservices:

1. **Update Schema**: Edit `unified-testing-config/label-schema.json`
2. **Copy to Backend**: Run `Copy-Item unified-testing-config/label-schema.json backend/src/test/resources/`
3. **Validate Tests**: Run backend tests to ensure `LabelValidator` loads new values
4. **Update Documentation**: Add entries to this table
5. **Update Enums**: Add new values to `TestType` or `Service` enums if applicable

**Change Control:** Label schema changes require:
- Pull request review by platform team
- Validation that all existing tests still pass
- Update to this documentation
- Communication to all teams using the labels

## CI Integration

### Backend Build

The `StoryLabelApplier` validates labels automatically during test execution. No additional CI steps required - invalid labels will fail the build.

### Frontend Build

Add validation step to CI pipeline:

```yaml
# .github/workflows/test.yml
- name: Run Jest tests with JSON output
  run: npm test -- --json --outputFile=test-output.json

- name: Validate test labels
  run: node scripts/validate-labels.mjs --jest-output test-output.json --fail-on-error
```

### Combined Validation

Validate both backend and frontend in single CI job:

```yaml
- name: Validate all test labels
  run: |
    node scripts/validate-labels.mjs \
      --allure-results backend/allure-results \
      --jest-output frontend/test-output.json \
      --fail-on-error \
      --verbose
```

## Validation Output

### Success

```
✓ Loaded label schema from unified-testing-config/label-schema.json
  Valid testTypes: unit, integration, contract, e2e
  Valid services: backend, frontend
  Valid microservices: cds-platform, gateway, risk-engine, risk-ui, trade-ui, settlement-ui

Validating Allure results from: backend/allure-results
Found 35 Allure test results

=== Validation Results ===
Total tests: 35
Valid labels: 35
Errors: 0
Warnings: 0

✓ All labels validated successfully
```

### Errors

```
=== Validation Results ===
Total tests: 35
Valid labels: 33
Errors: 2
Warnings: 0

⚠ Invalid testType labels: 1
⚠ Invalid microservice labels: 1

=== Errors ===
✗ CashSettlementServiceTest > calculateSettlement
  Invalid testType "functional". Valid values: unit, integration, contract, e2e

✗ CreditEventServiceTest > recordEvent
  Invalid microservice "unknown". Valid values: cds-platform, gateway, risk-engine, ...

✗ Label validation failed with errors
```

## Best Practices

1. **Use Canonical Values**: Only use test types and services defined in the schema
2. **Validate Early**: Run validation in local development before pushing
3. **Keep Schema Updated**: Add new microservices to schema as they're created
4. **Document Changes**: Update this file when modifying the schema
5. **Review Schema PRs**: Treat schema changes as breaking changes requiring team review
6. **Test Validators**: Ensure `LabelValidatorTest` and `validate-labels.test.mjs` pass after schema changes

## Troubleshooting

### Invalid Label Errors

**Problem**: Test fails with "Invalid testType 'functional'"

**Solution**: Use one of the approved test types (unit, integration, contract, e2e)

### Schema Not Found

**Problem**: "label-schema.json not found in classpath"

**Solution**: Copy schema to backend test resources:
```powershell
Copy-Item unified-testing-config/label-schema.json backend/src/test/resources/
```

### Validation Script Errors

**Problem**: "Failed to load label schema"

**Solution**: Ensure `unified-testing-config/label-schema.json` exists and is valid JSON

## Related Documentation

- [Backend Story ID Annotation](./story-id-annotation.md)
- [Frontend Story Tagging](./frontend-story-tagging.md)
- [Story Catalog](../../unified-testing-config/story-catalog.json)
- [Traceability Matrix](./traceability-matrix.md)
