# Allure Annotations Guide

This guide documents how to use Allure annotations across backend and frontend tests to enable unified reporting with proper Behaviors view grouping.

## Overview

The CDS Platform uses standard Allure annotations (`@Epic`, `@Feature`, `@Story`) for backend tests and tag-based labeling for frontend tests. These approaches create a unified Behaviors view in Allure reports with a 3-level hierarchy:

1. **Epic** - Test type categorization (Unit Tests, Integration Tests, E2E Tests)
2. **Feature** - Service or major feature area (Backend Service, Gateway Service, etc.)
3. **Story** - Specific scenario or story being tested

This hierarchy enables filtering and grouping by test type in the Behaviors view, making it easy to see test distribution across unit, integration, and E2E tests.

## Backend Tests (Java)

### CRITICAL: Correct Annotation Placement

**@Epic goes at CLASS level, @Feature and @Story go at METHOD level.**

This is the ONLY correct pattern. All tests MUST follow this approach:

```java
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

@Epic("Unit Tests")  // ✅ CLASS level ONLY
class CreditEventServiceTest {
    
    @Test
    @Feature("Backend Service")  // ✅ METHOD level
    @Story("Credit Event Processing - Record New Event")  // ✅ METHOD level
    void testRecordCreditEvent_Success() {
        // Test implementation
    }
    
    @Test
    @Feature("Backend Service")  // ✅ Each test gets its own labels
    @Story("Credit Event Processing - Idempotent Existing Event")
    void testRecordCreditEvent_Idempotent() {
        // Test implementation
    }
}
```

### ❌ WRONG Pattern (DO NOT USE)

```java
@Epic("Unit Tests")
@Feature("Backend Service")  // ❌ Wrong - wastes Feature at class level
@Story("Credit Event Processing")  // ❌ Wrong - all methods share same story
class CreditEventServiceTest {
    
    @Test
    void testMethod() {  // ❌ No Feature/Story - test invisible in Behaviors
        // Test implementation
    }
}
```

**Why this matters:**
- Putting @Feature/@Story at class level means ALL tests share the same labels
- In Behaviors view, you can't distinguish individual test scenarios
- Each test method MUST have its own @Feature and @Story for proper reporting

### Annotation Guidelines

**@Epic Annotation (CLASS LEVEL ONLY):**
- Represents the test type categorization
- Standard values:
  - `"Unit Tests"` - Pure unit tests with mocked dependencies
  - `"Integration Tests"` - Tests using Spring context or database
  - `"E2E Tests"` - End-to-end tests across multiple services
- Creates top-level grouping in Allure Behaviors view
- **Place ONLY at class level**, never at method level

**@Feature Annotation (METHOD LEVEL - EACH @Test):**
- Represents the service or major feature area
- Standard values:
  - `"Backend Service"` - Core platform services (cds-platform)
  - `"Gateway Service"` - API gateway services
  - `"Risk Engine Service"` - Risk calculation services
  - `"Frontend Service"` - UI components and pages
- Creates second-level grouping under Epic in Behaviors view
- **MUST be added to EACH @Test method**

**@Story Annotation (METHOD LEVEL - EACH @Test):**
- Describes the specific test scenario in format: `"<Component> - <Specific Action>"`
- Should be unique per test method for proper identification
- Examples:
  - `"Credit Event Processing - Record New Event"`
  - `"Credit Event Processing - Idempotent Existing Event"`
  - `"Cash Settlement - Custom Recovery Rate"`
  - `"CDS Trade Management - Save Trade"`
  - `"Risk Calculation - ORE Success"`
- Creates third-level leaf nodes under Feature in Behaviors view
- **MUST be added to EACH @Test method** with unique description

### Examples by Service and Test Type

**Backend Platform - Unit Tests:**
```java
@Epic("Unit Tests")  // Class level ONLY
class CDSTradeServiceTest {
    
    @Test
    @Feature("Backend Service")  // Method level
    @Story("CDS Trade Management - Save Trade")
    void testSaveTrade() {
        // Pure unit test with mocked dependencies
    }
    
    @Test
    @Feature("Backend Service")  // Each method gets labels
    @Story("CDS Trade Management - Get Trade By ID")
    void testGetTradeById() {
        // Another test scenario
    }
}

@Epic("Unit Tests")
class CashSettlementServiceTest {
    
    @Test
    @Feature("Backend Service")
    @Story("Cash Settlement - New Calculation")
    void testCalculateCashSettlement_NewEvent() {
        // Unit test for settlement processing
    }
    
    @Test
    @Feature("Backend Service")
    @Story("Cash Settlement - Custom Recovery Rate")
    void testCalculateCashSettlement_CustomRecovery() {
        // Test with custom recovery
    }
}
```

**Backend Platform - Integration Tests:**
```java
@Epic("Integration Tests")  // Class level ONLY
class CDSTradeRepositoryExampleIntegrationTest {
    
    @Test
    @Feature("Backend Service")  // Method level
    @Story("CDS Trade Repository - Save And Retrieve")
    void shouldSaveAndRetrieveCDSTrade() {
        // Integration test with Spring context and database
    }
    
    @Test
    @Feature("Backend Service")
    @Story("CDS Trade Repository - Find All Trades")
    void shouldFindAllTrades() {
        // Another integration scenario
    }
}

@Epic("Integration Tests")
class CouponScheduleServiceTest {
    
    @Test
    @Feature("Backend Service")
    @Story("Coupon Schedule Service - Generate IMM Schedule")
    void testGenerateImmSchedule() {
        // Integration test for schedule generation
    }
}
```

**Gateway Service - Unit Tests:**
```java
@Epic("Unit Tests")
class VersionControllerTest {
    
    @Test
    @Feature("Gateway Service")
    @Story("Version API - Get Version Info")
    public void testGetVersionInfo() {
        // Test for version endpoint
    }
}

@Epic("Unit Tests")
class AuthFilterTest {
    
    @Test
    @Feature("Gateway Service")
    @Story("Authentication - Valid Token")
    void testAuthWithValidToken() {
        // Test for auth handling
    }
    
    @Test
    @Feature("Gateway Service")
    @Story("Authentication - Invalid Token")
    void testAuthWithInvalidToken() {
        // Test rejection scenario
    }
}
```

**Risk Engine Service - Unit Tests:**
```java
@Epic("Unit Tests")
class RiskCalculationServiceTest {
    
    @Test
    @Feature("Risk Engine Service")
    @Story("Risk Calculation - ORE Success")
    void testCalculateRiskMeasures_OreSuccess() {
        // Test for successful risk calculation
    }
    
    @Test
    @Feature("Risk Engine Service")
    @Story("Risk Calculation - ORE Invalid Output")
    void testCalculateRiskMeasures_OreInvalidOutput() {
        // Test error handling
    }
}

@Epic("Unit Tests")
class OreProcessManagerTest {
    
    @Test
    @Feature("Risk Engine Service")
    @Story("ORE Process Management - Execute With Valid Input")
    void testExecuteCalculation_WithValidInput() {
        // Test for ORE process handling
    }
}
```

### Import Statements

Always import from Allure's standard annotations package:

```java
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
```

**Do NOT use:**
- Custom `@StoryId` annotations
- Spring's `@Tag` for Allure labeling
- Any other custom annotation approaches

**Annotation Order:**
Place annotations in this order for clarity:

**At Class Level:**
1. `@Epic("<Test Type>")` - Test type categorization
2. Other class annotations (`@ExtendWith`, `@SpringBootTest`, etc.)
3. Class declaration

**At Method Level:**
1. `@Test` - JUnit test marker
2. `@Feature("<Service Name>")` - Service identifier
3. `@Story("<Component> - <Specific Scenario>")` - Unique test scenario
4. Other method annotations (if any)
5. Method declaration

**Complete Example:**
```java
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Epic("Unit Tests")  // 1. Epic at class level
@ExtendWith(MockitoExtension.class)  // 2. Other class annotations
class MyServiceTest {  // 3. Class declaration
    
    @Test  // 1. Test marker
    @Feature("Backend Service")  // 2. Feature at method level
    @Story("My Service - Specific Scenario")  // 3. Story at method level
    void testSomething() {  // 4. Method declaration
        // Test implementation
    }
}
```

## Frontend Tests (JavaScript/TypeScript)

### Required Helper Functions

Frontend tests MUST use helper functions from `frontend/src/utils/testHelpers.ts` with `testType` parameter to enable Epic-level categorization:

```typescript
import { withStoryId, describeStory } from '../utils/testHelpers';

// Option 1: Unit test with withStoryId
withStoryId({ 
  storyId: 'UTS-2.3', 
  testType: 'unit',  // Creates 'Unit Tests' epic
  service: 'frontend' 
})('should render component correctly', () => {
  // Test implementation
});

// Option 2: Integration test with describeStory
describeStory(
  { 
    storyId: 'UTS-3.1', 
    testType: 'integration',  // Creates 'Integration Tests' epic
    service: 'frontend' 
  },
  'API Integration Tests',
  () => {
    it('should fetch data successfully', () => {
      // Test implementation
    });
  }
);

// Option 3: E2E test
withStoryId({ 
  storyId: 'E2E-1.1', 
  testType: 'e2e',  // Creates 'E2E Tests' epic
  service: 'frontend' 
})('should complete full user flow', () => {
  // Test implementation
});
```

### How It Works

1. **Helper functions automatically generate epic labels** based on `testType`:
   - `testType: 'unit'` → `[epic:Unit Tests]`
   - `testType: 'integration'` → `[epic:Integration Tests]`
   - `testType: 'e2e'` → `[epic:E2E Tests]`
   - Other types → `[epic:${testType} Tests]`

2. **Helper functions add all required tags** to test names:
   - `[story:UTS-2.3]` - Story identifier
   - `[testType:unit]` - Test type classification
   - `[service:frontend]` - Service identifier
   - `[feature:Frontend Service]` - Auto-generated feature label
   - `[epic:Unit Tests]` - Auto-generated epic label based on testType

3. **Post-processing extracts labels:**
   - Script: `scripts/add-frontend-labels.ps1`
   - Runs after test execution
   - Parses test names and injects `epic`, `feature`, and `story` labels into Allure JSON

4. **Result in Allure:**
   - Frontend tests appear in 3-level hierarchy
   - Grouped by test type (Epic) → Service (Feature) → Scenario (Story)
   - Unified with backend test categorization

### Test Helper Reference

**`withStoryId(options)`**
- Wraps a test with automatic tagging
- Parameters:
  - `options.storyId`: Story identifier (required, e.g., 'UTS-2.3')
  - `options.testType`: Test type (optional, default: 'unit')
    - Valid values: 'unit', 'integration', 'e2e', 'contract', 'performance', 'security'
  - `options.service`: Service identifier (optional, default: 'frontend')
  - `options.epic`: Custom epic override (optional, auto-generated from testType)
  - `options.feature`: Custom feature override (optional, auto-generated)
  - `options.severity`: Severity level (optional)
- Returns: Tagged test wrapper function

**`describeStory(options, suiteName, suiteFn)`**
- Wraps a test suite with automatic tagging
- Parameters:
  - `options`: Same as withStoryId
  - `suiteName`: Human-readable test suite name
  - `suiteFn`: Function containing tests
- Auto-adds epic/feature/story tags to suite name

## Behaviors View Structure

The annotations create this 3-level hierarchy in Allure reports:

```
Behaviors
├── Unit Tests (Epic - class level)
│   ├── Backend Service (Feature - method level)
│   │   ├── Credit Event Processing - Record New Event (Story - method level, individual test)
│   │   ├── Credit Event Processing - Idempotent Existing Event (individual test)
│   │   ├── Cash Settlement - New Calculation (individual test)
│   │   ├── Cash Settlement - Custom Recovery Rate (individual test)
│   │   └── CDS Trade Management - Save Trade (individual test)
│   ├── Frontend Service
│   │   └── Component Rendering - Default Props (individual test)
│   ├── Gateway Service
│   │   └── Version API - Get Version Info (individual test)
│   └── Risk Engine Service
│       ├── Risk Calculation - ORE Success (individual test)
│       └── ORE Process Management - Execute With Valid Input (individual test)
├── Integration Tests (Epic - class level)
│   ├── Backend Service (Feature - method level)
│   │   ├── CDS Trade Repository - Save And Retrieve (Story - method level, individual test)
│   │   ├── CDS Trade Repository - Find All Trades (individual test)
│   │   └── Coupon Schedule Service - Generate IMM Schedule (individual test)
│   └── Frontend Service
│       └── API Integration - Fetch Data Success (individual test)
└── E2E Tests (Epic - class level)
    └── Frontend Service
        └── User Flows - Complete Checkout (individual test)
```

This structure enables:
- **Filtering by test type** - View only unit tests, integration tests, or E2E tests
- **Service-level grouping** - See all tests for a specific service
- **Individual test identification** - Each test has unique Story describing its specific scenario

## Migration from @StoryId

If you encounter tests using the older `@StoryId` annotation pattern, migrate to the new @Epic at class / @Feature+@Story at method pattern:

**Before (OLD - DO NOT USE):**
```java
import com.creditdefaultswap.unit.platform.testing.story.StoryId;

@StoryId(value = "UTS-2.1", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
class DateUtilsExampleTest {
    
    @Test
    void testCalculateDaysBetween() {
        // Test implementation
    }
}
```

**After (CORRECT):**
```java
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Epic("Unit Tests")  // Class level ONLY
class DateUtilsExampleTest {
    
    @Test
    @Feature("Backend Service")  // Method level
    @Story("Date Utilities - Calculate Days Between")  // Method level
    void testCalculateDaysBetween() {
        // Test implementation
    }
}
```

**Integration Test Migration:**
```java
// Before (OLD - DO NOT USE)
@StoryId(value = "UTS-2.1", testType = StoryId.TestType.INTEGRATION, microservice = "cds-platform")
class CDSTradeRepositoryExampleIntegrationTest {
    
    @Test
    void shouldSaveAndRetrieveCDSTrade() {
        // Test
    }
}

// After (CORRECT)
@Epic("Integration Tests")  // Class level ONLY
class CDSTradeRepositoryExampleIntegrationTest {
    
    @Test
    @Feature("Backend Service")  // Method level
    @Story("CDS Trade Repository - Save And Retrieve")  // Method level
    void shouldSaveAndRetrieveCDSTrade() {
        // Test
    }
}
```

**Old Class-Level Pattern (ALSO WRONG - DO NOT USE):**
```java
// This is WRONG even though it uses @Epic/@Feature/@Story
@Epic("Unit Tests")
@Feature("Backend Service")  // ❌ Wrong - at class level
@Story("Credit Event Processing")  // ❌ Wrong - at class level
class CreditEventServiceTest {
    
    @Test
    void testMethod() {  // ❌ No labels - test invisible
        // ...
    }
}
```

**Correct Pattern:**
```java
@Epic("Unit Tests")  // ✅ Class level ONLY
class CreditEventServiceTest {
    
    @Test
    @Feature("Backend Service")  // ✅ Method level
    @Story("Credit Event Processing - Specific Scenario")  // ✅ Method level
    void testMethod() {
        // ...
    }
}
```

## Validation

### Backend
Verify correct Epic/Feature/Story annotation placement:

```bash
# Check for @Epic at class level (correct)
grep -r "@Epic" backend/src/test/java/ | grep "class.*Test"

# Check for @Feature at method level (correct)
grep -r "@Feature" backend/src/test/java/ -A 1 | grep "@Test"

# Check for @Story at method level (correct)
grep -r "@Story" backend/src/test/java/ -A 1 | grep "@Test"

# Find WRONG pattern: @Feature at class level (should be empty)
grep -r "class.*Test" backend/src/test/java/ -B 5 | grep -B 1 "@Feature"

# Find WRONG pattern: @Story at class level (should be empty)
grep -r "class.*Test" backend/src/test/java/ -B 5 | grep -B 1 "@Story"

# Check for old @StoryId usage (should be empty)
grep -r "@StoryId" backend/src/test/java/
```

**What to look for:**
- ✅ `@Epic("...")` appears before `class XTest`
- ✅ `@Feature("...")` appears after `@Test` annotation
- ✅ `@Story("...")` appears after `@Test` annotation
- ❌ `@Feature` should NEVER appear at class level
- ❌ `@Story` should NEVER appear at class level

### Frontend
Check test helpers use testType parameter:
```bash
# Find tests that should specify testType
grep -r "withStoryId\|describeStory" frontend/src/ --include="*.test.ts*" | \
  grep -v "testType:"
```

### Unified Report
After running tests, verify 3-level Behaviors view hierarchy:
```powershell
# Run unified test script
.\scripts\test-unified-local.ps1

# Open report and check Behaviors tab
# Should see: Epic (test type) → Feature (service) → Story (scenario)
```

## Best Practices

1. **CRITICAL - Correct Placement**: 
   - @Epic at CLASS level ONLY
   - @Feature and @Story at METHOD level (each @Test)
   - Never put @Feature or @Story at class level
2. **Test Type Classification**: Use correct Epic based on test type:
   - Unit tests with mocks → `@Epic("Unit Tests")`
   - Tests with Spring context/DB → `@Epic("Integration Tests")`
   - Full system tests → `@Epic("E2E Tests")`
3. **Unique Stories**: Each @Test method needs unique @Story describing its specific scenario
4. **Story Format**: Use `"<Component> - <Specific Action>"` format for clarity
5. **Consistency**: Use same Feature names across related tests
6. **Import Statements**: Always use `io.qameta.allure.*` imports
7. **Annotation Order**: 
   - Class: @Epic → other class annotations → class declaration
   - Method: @Test → @Feature → @Story → method declaration
8. **No Duplication**: Don't mix with custom @StoryId annotations
9. **Post-Processing**: Let scripts handle frontend label injection
10. **Validation**: Check Behaviors view shows 3 levels with individual test nodes after adding new tests

## Troubleshooting

**Tests don't appear in Behaviors view:**
- Backend: Verify `@Epic` is at class level, `@Feature` and `@Story` are at EACH @Test method level
- Frontend: Check that `withStoryId()` or `describeStory()` includes `testType` parameter
- Both: Ensure post-processing script ran successfully

**Wrong Epic/test type grouping:**
- Check Epic annotation value at class level: "Unit Tests", "Integration Tests", or "E2E Tests"
- Verify test type matches actual test implementation (mock vs Spring context)

**Wrong service grouping:**
- Check Feature annotation value at METHOD level matches standard names
- Backend Service / Frontend Service / Gateway Service / Risk Engine Service

**Missing 3-level hierarchy:**
- Verify @Epic at class level AND @Feature/@Story at EACH method
- Check import statements include `io.qameta.allure.Epic`, `Feature`, `Story`

**All tests in a class share same Story (WRONG):**
- This means @Story is at class level instead of method level
- Move @Story annotation from class to EACH @Test method
- Give each method a unique Story describing its specific scenario

**Tests appear under wrong Feature:**
- Check that @Feature is at METHOD level, not class level
- Verify each @Test method has its own @Feature annotation

**Missing labels in JSON:**
- Frontend: Run `scripts/add-frontend-labels.ps1` manually
- Verify test names contain `[epic:...]`, `[feature:...]`, and `[story:...]` tags

**"Cannot find symbol" compile error for @Feature/@Story:**
- These annotations must be at METHOD level, verify placement
- Check imports: `import io.qameta.allure.Feature;` and `import io.qameta.allure.Story;`

## Related Documentation

- **CORRECT Pattern Guide**: `docs/testing/CORRECT-ALLURE-PATTERN.md` ⭐ **Visual reference**
- **Test Generation Prompt**: `.github/prompts/implement-tests.prompt.md`
- **Test Helpers**: `frontend/src/utils/testHelpers.ts`
- **Post-Processing**: `scripts/add-frontend-labels.ps1`
- **Unified Testing**: `docs/testing/GOLDEN_PATH.md`
- **CI Workflows**: `.github/workflows/unified-reports.yml`
- **EpicType Enum**: `backend/src/test/java/.../testing/allure/EpicType.java`
- **FeatureType Enum**: `backend/src/test/java/.../testing/allure/FeatureType.java`
