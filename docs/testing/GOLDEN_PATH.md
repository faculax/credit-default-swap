# Testing Golden Path Guide

**Epic 02: Test Architecture Standardization**  
**Last Updated:** November 14, 2025

This guide provides the recommended approach for creating new tests in the CDS Trading Platform. Following these patterns ensures consistency, maintainability, and proper integration with our reporting infrastructure.

---

## Table of Contents

1. [Quick Reference](#quick-reference)
2. [Backend Java Tests](#backend-java-tests)
3. [Frontend TypeScript Tests](#frontend-typescript-tests)
4. [Test Type Selection](#test-type-selection)
5. [Story Traceability](#story-traceability)
6. [Common Patterns](#common-patterns)

---

## Quick Reference

### Backend Test Location Matrix

| Test Type | Location Pattern | Example |
|-----------|------------------|---------|
| **Unit Test** | `backend/src/test/java/com/creditdefaultswap/unit/{domain}/` | `unit/platform/service/CDSTradeServiceTest.java` |
| **Integration Test** | `backend/src/test/java/com/creditdefaultswap/integration/{domain}/` | `integration/platform/repository/CDSTradeRepositoryExampleIntegrationTest.java` |
| **E2E Test** | `backend/src/test/java/com/creditdefaultswap/e2e/` | `e2e/TradeLifecycleE2ETest.java` |

### Frontend Test Location Matrix

| Test Type | Location Pattern | Example |
|-----------|------------------|---------|
| **Unit Test** | `frontend/src/__tests__/unit/{structure}/` | `__tests__/unit/components/RegressionStatusBadge.test.tsx` |
| **Integration Test** | `frontend/src/__tests__/integration/{structure}/` | `__tests__/integration/pages/CreditEventWorkflow.test.tsx` |
| **E2E Test** | `frontend/src/__tests__/e2e/` | `__tests__/e2e/trade-capture-flow.test.ts` |

### Maven Test Profiles

```bash
# Run unit tests only
mvn test -Punit-tests

# Run integration tests only  
mvn test -Pintegration-tests

# Run all tests
mvn test
```

### Frontend Test Commands

```bash
# Run unit tests
npm run test:unit

# Run integration tests
npm run test:integration

# Run all tests
npm test
```

---

## Backend Java Tests

### Creating a Unit Test

**Location:** `backend/src/test/java/com/creditdefaultswap/unit/{domain}/{component}/`

**Template:**

```java
package com.creditdefaultswap.unit.platform.service;

import com.creditdefaultswap.platform.service.YourService;
import com.creditdefaultswap.unit.platform.testing.story.StoryId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YourServiceTest {

    @Mock
    private DependencyService dependencyService;

    @InjectMocks
    private YourService yourService;

    @Test
    @StoryId(value = "UTS-X.Y", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
    void shouldDoSomethingExpected() {
        // Arrange
        when(dependencyService.getData()).thenReturn("test-data");

        // Act
        String result = yourService.performAction();

        // Assert
        assertEquals("expected-result", result);
        verify(dependencyService).getData();
    }
}
```

**Key Points:**
- ✅ Package name matches directory: `com.creditdefaultswap.unit.{domain}`
- ✅ Use `@ExtendWith(MockitoExtension.class)` for Mockito support
- ✅ Mock external dependencies with `@Mock`
- ✅ Inject mocks into the class under test with `@InjectMocks`
- ✅ Every test annotated with `@StoryId`
- ✅ Follow Arrange-Act-Assert pattern

**Real Example:** [`CDSTradeServiceTest.java`](../../backend/src/test/java/com/creditdefaultswap/unit/platform/service/CDSTradeServiceTest.java)

---

### Creating an Integration Test

**Location:** `backend/src/test/java/com/creditdefaultswap/integration/{domain}/{component}/`

**Template:**

```java
package com.creditdefaultswap.integration.platform.repository;

import com.creditdefaultswap.platform.CDSPlatformApplication;
import com.creditdefaultswap.platform.model.YourEntity;
import com.creditdefaultswap.platform.repository.YourRepository;
import com.creditdefaultswap.unit.platform.testing.story.StoryId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CDSPlatformApplication.class)
@ActiveProfiles("test")
@Transactional
class YourRepositoryIntegrationTest {

    @Autowired
    private YourRepository repository;

    @Test
    @StoryId(value = "UTS-X.Y", testType = StoryId.TestType.INTEGRATION, microservice = "cds-platform")
    void shouldPersistAndRetrieveEntity() {
        // Arrange
        YourEntity entity = new YourEntity();
        entity.setField("value");

        // Act
        YourEntity saved = repository.save(entity);
        YourEntity retrieved = repository.findById(saved.getId()).orElseThrow();

        // Assert
        assertNotNull(retrieved);
        assertEquals("value", retrieved.getField());
    }
}
```

**Key Points:**
- ✅ Package name: `com.creditdefaultswap.integration.{domain}`
- ✅ Use `@SpringBootTest(classes = CDSPlatformApplication.class)`
- ✅ Use `@ActiveProfiles("test")` for test database
- ✅ Use `@Transactional` to rollback changes after each test
- ✅ Autowire real Spring beans
- ✅ Test database interactions, HTTP calls, or multi-component workflows

**Real Example:** [`CDSTradeRepositoryExampleIntegrationTest.java`](../../backend/src/test/java/com/creditdefaultswap/integration/platform/repository/CDSTradeRepositoryExampleIntegrationTest.java)

---

### Creating a Service Test with Spring Context

Some service tests require the Spring application context (e.g., for scheduled tasks, complex service interactions).

**Template:**

```java
package com.creditdefaultswap.unit.platform.service;

import com.creditdefaultswap.platform.CDSPlatformApplication;
import com.creditdefaultswap.platform.service.YourService;
import com.creditdefaultswap.unit.platform.testing.story.StoryId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CDSPlatformApplication.class)
@ActiveProfiles("test")
@Transactional
class YourServiceTest {

    @Autowired
    private YourService yourService;

    @Test
    @StoryId(value = "UTS-X.Y", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
    void shouldExecuteBusinessLogic() {
        // Test implementation
    }
}
```

**Real Example:** [`CouponScheduleServiceTest.java`](../../backend/src/test/java/com/creditdefaultswap/unit/platform/service/CouponScheduleServiceTest.java)

---

## Frontend TypeScript Tests

### Creating a Component Unit Test

**Location:** `frontend/src/__tests__/unit/components/{component}.test.tsx`

**Template:**

```typescript
import { render, screen } from '@testing-library/react';
import { YourComponent } from '../../../components/YourComponent';

describe('YourComponent', () => {
  it('should render with expected content', () => {
    // Arrange
    const props = { title: 'Test Title' };

    // Act
    render(<YourComponent {...props} />);

    // Assert
    expect(screen.getByText('Test Title')).toBeInTheDocument();
  });

  it('should handle user interaction', () => {
    // Arrange
    const mockCallback = jest.fn();
    render(<YourComponent onClick={mockCallback} />);

    // Act
    screen.getByRole('button').click();

    // Assert
    expect(mockCallback).toHaveBeenCalledTimes(1);
  });
});
```

**Key Points:**
- ✅ Import components using relative paths: `../../../components/`
- ✅ Use React Testing Library for rendering
- ✅ Use `screen` queries for accessible element selection
- ✅ Test user interactions, not implementation details
- ✅ One `describe` block per component
- ✅ Multiple `it` blocks for different scenarios

**Real Example:** [`RegressionStatusBadge.test.tsx`](../../frontend/src/__tests__/unit/components/RegressionStatusBadge.test.tsx)

---

### Creating a Hook Unit Test

**Location:** `frontend/src/__tests__/unit/hooks/{hookName}.test.ts`

**Template:**

```typescript
import { renderHook, act } from '@testing-library/react';
import { useYourHook } from '../../../hooks/useYourHook';

describe('useYourHook', () => {
  it('should initialize with default values', () => {
    // Act
    const { result } = renderHook(() => useYourHook());

    // Assert
    expect(result.current.value).toBe(null);
    expect(result.current.loading).toBe(false);
  });

  it('should update value when action is triggered', () => {
    // Arrange
    const { result } = renderHook(() => useYourHook());

    // Act
    act(() => {
      result.current.updateValue('new-value');
    });

    // Assert
    expect(result.current.value).toBe('new-value');
  });
});
```

**Key Points:**
- ✅ Use `renderHook` from React Testing Library
- ✅ Wrap state updates in `act()`
- ✅ Test hook logic in isolation
- ✅ Import hooks using relative paths: `../../../hooks/`

---

### Creating an Integration Test

**Location:** `frontend/src/__tests__/integration/pages/{page}.test.tsx`

**Template:**

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import { YourPage } from '../../../pages/YourPage';
import { BrowserRouter } from 'react-router-dom';

// Mock API calls
jest.mock('../../../services/api', () => ({
  fetchData: jest.fn(() => Promise.resolve({ data: 'test' }))
}));

describe('YourPage Integration', () => {
  it('should load and display data from API', async () => {
    // Act
    render(
      <BrowserRouter>
        <YourPage />
      </BrowserRouter>
    );

    // Assert
    await waitFor(() => {
      expect(screen.getByText('test')).toBeInTheDocument();
    });
  });
});
```

**Key Points:**
- ✅ Mock API calls with `jest.mock()`
- ✅ Wrap in Router providers if component uses routing
- ✅ Use `waitFor` for async operations
- ✅ Test complete user workflows

**Real Example:** [`CreditEventWorkflow.test.tsx`](../../frontend/src/__tests__/integration/pages/CreditEventWorkflow.test.tsx)

---

## Test Type Selection

### When to Use Each Test Type

| Test Type | Purpose | Characteristics | Example |
|-----------|---------|-----------------|---------|
| **Unit** | Test single class/function in isolation | • Fast (<100ms)<br>• No external dependencies<br>• Use mocks/stubs | Testing service logic with mocked repositories |
| **Integration** | Test interaction between components | • Slower (seconds)<br>• Real database/HTTP<br>• Multiple components | Testing repository with real database |
| **E2E** | Test complete user workflows | • Slowest (minutes)<br>• Full application stack<br>• Real browser | Testing trade capture to settlement flow |

### Decision Tree

```
┌─────────────────────────────────────┐
│ Does it test a single class/method? │
└──────────┬──────────────────────────┘
           │
    ┌──────┴──────┐
    │     YES     │──────► UNIT TEST
    └─────────────┘
           │
           │ NO
           ▼
┌─────────────────────────────────────┐
│ Does it need real infrastructure?   │
│ (Database, HTTP, Message Queue)     │
└──────────┬──────────────────────────┘
           │
    ┌──────┴──────┐
    │     YES     │──────► INTEGRATION TEST
    └─────────────┘
           │
           │ NO (tests multiple components)
           ▼
┌─────────────────────────────────────┐
│ Does it test end-to-end workflow    │
│ across multiple services/UI?        │
└──────────┬──────────────────────────┘
           │
    ┌──────┴──────┐
    │     YES     │──────► E2E TEST
    └─────────────┘
```

---

## Story Traceability

### Using @StoryId Annotation

Every test must be annotated with `@StoryId` to enable traceability in Allure reports.

**Syntax:**

```java
@StoryId(
    value = "UTS-X.Y",                    // Story ID from user stories
    testType = StoryId.TestType.UNIT,     // UNIT, INTEGRATION, or E2E
    microservice = "cds-platform"         // Service name
)
```

**Example:**

```java
@Test
@StoryId(value = "UTS-5.1", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
void shouldGenerateImmCouponSchedule() {
    // Test implementation
}
```

**Test Type Values:**
- `StoryId.TestType.UNIT` - Unit tests
- `StoryId.TestType.INTEGRATION` - Integration tests
- `StoryId.TestType.E2E` - End-to-end tests

**Microservice Values:**
- `cds-platform` - Main backend service
- `risk-engine` - Risk calculation service
- `gateway` - API gateway

### Finding Story IDs

Story IDs are documented in:
- [`unified-testing-stories/TestingPRD.md`](../../unified-testing-stories/TestingPRD.md) - Test infrastructure stories (UTS-X.Y)
- [`user-stories/`](../../user-stories/) - Feature stories by epic

---

## Common Patterns

### Testing Exceptions

```java
@Test
@StoryId(value = "UTS-X.Y", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
void shouldThrowExceptionWhenInvalidInput() {
    // Arrange
    InvalidInput input = new InvalidInput();

    // Act & Assert
    assertThrows(ValidationException.class, () -> {
        service.process(input);
    });
}
```

### Testing Async Methods

```java
@Test
@StoryId(value = "UTS-X.Y", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
void shouldCompleteAsyncOperation() throws Exception {
    // Arrange
    CompletableFuture<Result> future = service.asyncOperation();

    // Act
    Result result = future.get(5, TimeUnit.SECONDS);

    // Assert
    assertNotNull(result);
}
```

### Testing with Date/Time

```java
@Test
@StoryId(value = "UTS-X.Y", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
void shouldCalculateDateCorrectly() {
    // Arrange
    LocalDate startDate = LocalDate.of(2025, 1, 15);
    
    // Act
    LocalDate result = service.calculateMaturity(startDate, 5);
    
    // Assert
    assertEquals(LocalDate.of(2030, 1, 15), result);
}
```

### Testing Collections

```java
@Test
@StoryId(value = "UTS-X.Y", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
void shouldReturnExpectedList() {
    // Act
    List<Trade> trades = service.getActiveTrades();

    // Assert
    assertNotNull(trades);
    assertFalse(trades.isEmpty());
    assertEquals(3, trades.size());
    assertTrue(trades.stream().allMatch(t -> t.getStatus() == TradeStatus.ACTIVE));
}
```

### Testing Frontend Forms

```typescript
import { fireEvent } from '@testing-library/react';

it('should submit form with valid data', async () => {
  // Arrange
  const mockSubmit = jest.fn();
  render(<TradeForm onSubmit={mockSubmit} />);

  // Act
  fireEvent.change(screen.getByLabelText('Notional'), {
    target: { value: '1000000' }
  });
  fireEvent.change(screen.getByLabelText('Spread'), {
    target: { value: '150' }
  });
  fireEvent.click(screen.getByRole('button', { name: /submit/i }));

  // Assert
  await waitFor(() => {
    expect(mockSubmit).toHaveBeenCalledWith({
      notional: 1000000,
      spread: 150
    });
  });
});
```

---

## Best Practices

### Do ✅

- **Follow AAA Pattern:** Arrange, Act, Assert
- **One Assertion Per Test:** Test one behavior at a time
- **Use Descriptive Names:** `shouldCalculateCorrectSpreadWhenCdsIsActive()`
- **Mock External Dependencies:** Databases, HTTP, file systems
- **Clean Up Resources:** Use `@AfterEach` or `@Transactional`
- **Test Edge Cases:** Null, empty, boundary values
- **Keep Tests Fast:** Unit tests < 100ms, Integration < 5s
- **Make Tests Deterministic:** No random data, fixed dates

### Don't ❌

- **Don't Test Implementation Details:** Test behavior, not private methods
- **Don't Share State Between Tests:** Each test should be independent
- **Don't Use Real External Services:** Mock APIs, databases in unit tests
- **Don't Copy-Paste Tests:** Extract common setup to helper methods
- **Don't Ignore Flaky Tests:** Fix or remove unstable tests
- **Don't Skip Assertions:** Every test must verify something
- **Don't Test Framework Code:** Trust Spring Boot, React Testing Library

---

## Troubleshooting

### Common Issues

**Issue:** Package declaration doesn't match directory structure
```
[ERROR] package com.creditdefaultswap.platform.service does not match expected package com.creditdefaultswap.unit.platform.service
```
**Solution:** Update package declaration to match the file path under `src/test/java/`

**Issue:** Cannot find @StoryId annotation
```
[ERROR] cannot find symbol: @StoryId
```
**Solution:** Add import: `import com.creditdefaultswap.unit.platform.testing.story.StoryId;`

**Issue:** Spring Boot test can't find application class
```
[ERROR] Unable to find a @SpringBootConfiguration
```
**Solution:** Add `@SpringBootTest(classes = CDSPlatformApplication.class)`

**Issue:** Frontend test can't find component
```
Module not found: Can't resolve '../../../components/YourComponent'
```
**Solution:** Check import path - count `../` correctly from test location

---

## Additional Resources

- **Test Architecture:** [`docs/testing/TEST_ARCHITECTURE.md`](./TEST_ARCHITECTURE.md)
- **Folder Conventions:** Backend [`docs/testing/backend-test-conventions.md`](./backend-test-conventions.md), Frontend [`docs/testing/frontend-test-conventions.md`](./frontend-test-conventions.md)
- **Shared Schema:** [`docs/testing/test-type-schema.json`](./test-type-schema.json)
- **Testing PRD:** [`unified-testing-stories/TestingPRD.md`](../../unified-testing-stories/TestingPRD.md)

---

## Questions?

For questions or suggestions about testing patterns, please refer to:
- Story tracking in [`unified-testing-stories/`](../../unified-testing-stories/)
- Team testing conventions in [`docs/testing/`](./README.md)
