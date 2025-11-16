# Allure Annotations Guide

This guide documents how to use Allure annotations across backend and frontend tests to enable unified reporting with proper Behaviors view grouping.

## Overview

The CDS Platform uses standard Allure annotations (`@Feature`, `@Story`) for backend tests and tag-based labeling for frontend tests. These approaches create a unified Behaviors view in Allure reports that groups tests by service and story.

## Backend Tests (Java)

### Required Annotations

All backend test classes MUST include both `@Feature` and `@Story` annotations at the class level:

```java
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

@Feature("Backend Service")
@Story("Credit Event Processing")
class CreditEventServiceTest {
    
    @Test
    void shouldProcessCreditEvent() {
        // Test implementation
    }
}
```

### Annotation Guidelines

**@Feature Annotation:**
- Represents the service or major feature area
- Standard values:
  - `"Backend Service"` - Core platform services (cds-platform)
  - `"Gateway Service"` - API gateway services
  - `"Risk Engine Service"` - Risk calculation services
- Creates top-level grouping in Allure Behaviors view

**@Story Annotation:**
- Describes the specific story or scenario being tested
- Should be human-readable and match story titles from `user-stories/`
- Examples:
  - `"Credit Event Processing"`
  - `"Cash Settlement"`
  - `"CDS Trade Management"`
  - `"Risk Calculation"`
- Creates second-level grouping under Features in Behaviors view

### Examples by Service

**Backend Platform (cds-platform):**
```java
@Feature("Backend Service")
@Story("CDS Trade Management")
class CDSTradeServiceTest {
    // Tests for trade CRUD operations
}

@Feature("Backend Service")
@Story("Cash Settlement")
class CashSettlementServiceTest {
    // Tests for settlement processing
}
```

**Gateway Service:**
```java
@Feature("Gateway Service")
@Story("Version API")
class VersionControllerTest {
    // Tests for version endpoint
}

@Feature("Gateway Service")
@Story("Authentication")
class AuthFilterTest {
    // Tests for auth handling
}
```

**Risk Engine:**
```java
@Feature("Risk Engine Service")
@Story("Risk Calculation")
class RiskCalculationServiceTest {
    // Tests for PV/CVA calculations
}
```

### Import Statements

Always import from Allure's standard annotations package:

```java
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
```

**Do NOT use:**
- Custom `@StoryId` annotations
- Spring's `@Tag` for Allure labeling
- Any other custom annotation approaches

## Frontend Tests (JavaScript/TypeScript)

### Required Helper Functions

Frontend tests MUST use helper functions from `frontend/src/utils/testHelpers.ts` to add feature/epic tags:

```typescript
import { withStoryId, describeStory } from '../utils/testHelpers';

// Option 1: Using withStoryId wrapper
describe('API Service Tests', withStoryId(() => {
    it('should fetch data successfully', () => {
        // Test implementation
    });
}, 'API-001'));

// Option 2: Using describeStory
describeStory('API Service Data Fetching', 'API-001', () => {
    it('should handle errors gracefully', () => {
        // Test implementation
    });
});
```

### How It Works

1. **Helper functions automatically add tags** to test names:
   - `[feature:Frontend Service]`
   - `[epic:microservice Tests]`

2. **Post-processing extracts labels:**
   - Script: `scripts/add-frontend-labels.ps1`
   - Runs after test execution
   - Parses test names and injects `feature` and `story` labels into Allure JSON

3. **Result in Allure:**
   - Frontend tests appear under "Frontend Service" in Behaviors view
   - Grouped alongside backend tests
   - Unified reporting across all services

### Test Helper Reference

**`withStoryId(testFn, storyId)`**
- Wraps a test suite with automatic tagging
- Parameters:
  - `testFn`: Function containing test suite
  - `storyId`: Story identifier (e.g., 'API-001')
- Returns: Tagged test suite function

**`describeStory(description, storyId, testFn)`**
- Alternative syntax with description first
- Parameters:
  - `description`: Human-readable test suite name
  - `storyId`: Story identifier
  - `testFn`: Function containing tests
- Auto-adds feature/epic tags to suite name

## Behaviors View Structure

The annotations create this hierarchy in Allure reports:

```
Behaviors
├── Backend Service
│   ├── Credit Event Processing (5 tests)
│   ├── Cash Settlement (3 tests)
│   └── CDS Trade Management (8 tests)
├── Frontend Service
│   ├── API Service Tests (4 tests)
│   └── Component Tests (8 tests)
├── Gateway Service
│   └── Version API (1 test)
└── Risk Engine Service
    └── Risk Calculation (17 tests)
```

## Migration from @StoryId

If you encounter tests using the older `@StoryId` annotation pattern, update them:

**Before:**
```java
@StoryId(value = "UTS-210", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
class OldTest {
    // ...
}
```

**After:**
```java
@Feature("Backend Service")
@Story("Credit Event Processing")
class OldTest {
    // ...
}
```

## Validation

### Backend
Verify annotations are present:
```bash
# Search for test classes missing @Feature
grep -r "class.*Test" backend/src/test/java/ | \
  xargs grep -L "@Feature"

# Search for test classes missing @Story
grep -r "class.*Test" backend/src/test/java/ | \
  xargs grep -L "@Story"
```

### Frontend
Check test helpers are used:
```bash
# Find tests that should use helpers
grep -r "describe(" frontend/src/ --include="*.test.ts*" | \
  grep -v "withStoryId\|describeStory"
```

### Unified Report
After running tests, verify Behaviors view:
```powershell
# Run unified test script
.\scripts\test-unified-local.ps1

# Open report and check Behaviors tab
# All services should appear with proper grouping
```

## Best Practices

1. **Consistency**: Use same Feature names across related tests
2. **Clarity**: Story descriptions should match user story titles
3. **Placement**: Always at class level for backend, suite level for frontend
4. **No Duplication**: Don't add both `@Feature`/`@Story` AND custom annotations
5. **Post-Processing**: Let scripts handle frontend label injection
6. **Validation**: Check Behaviors view after adding new tests

## Troubleshooting

**Tests don't appear in Behaviors view:**
- Backend: Verify `@Feature` and `@Story` are present at class level
- Frontend: Check that `withStoryId()` or `describeStory()` is used
- Both: Ensure post-processing script ran successfully

**Wrong service grouping:**
- Check Feature annotation value matches standard names
- Backend Service / Frontend Service / Gateway Service / Risk Engine Service

**Missing labels in JSON:**
- Frontend: Run `scripts/add-frontend-labels.ps1` manually
- Verify test names contain `[feature:...]` tags

## Related Documentation

- **Test Generation Prompt**: `.github/prompts/implement-tests.prompt.md`
- **Test Helpers**: `frontend/src/utils/testHelpers.ts`
- **Post-Processing**: `scripts/add-frontend-labels.ps1`
- **Unified Testing**: `docs/testing/GOLDEN_PATH.md`
- **CI Workflows**: `.github/workflows/unified-reports.yml`
