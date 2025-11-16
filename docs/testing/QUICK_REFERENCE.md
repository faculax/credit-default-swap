# Testing Quick Reference Cheatsheet

**Epic 02: Test Architecture Standardization**  
**Version:** 1.1  
**Last Updated:** November 16, 2025

One-page reference for common testing tasks in the CDS Trading Platform.

---

## File Locations

### Backend Tests
```
backend/src/test/java/com/creditdefaultswap/
├── unit/              # Unit tests (< 100ms, mocked dependencies)
│   ├── platform/
│   │   ├── service/   # Service layer tests
│   │   └── util/      # Utility class tests
│   └── testing/       # Test framework utilities
├── integration/       # Integration tests (seconds, real DB/HTTP)
│   └── platform/
│       └── repository/
└── e2e/              # End-to-end tests (minutes, full stack)
```

### Frontend Tests
```
frontend/src/__tests__/
├── unit/             # Component/hook unit tests
│   ├── components/
│   ├── hooks/
│   └── utils/
├── integration/      # Page/workflow integration tests
│   └── pages/
└── e2e/             # Full application E2E tests
```

---

## Quick Commands

### Backend
```bash
# Unit tests only
mvn test -Punit-tests

# Integration tests only
mvn test -Pintegration-tests

# All tests
mvn test

# Single test class
mvn test -Dtest=YourServiceTest

# Single test method
mvn test -Dtest=YourServiceTest#shouldDoSomething
```

### Frontend
```bash
# Unit tests
npm run test:unit

# Integration tests
npm run test:integration

# All tests
npm test

# Watch mode
npm test -- --watch

# Coverage
npm test -- --coverage
```

---

## Test Templates

### Backend Unit Test (Mockito)
```java
package com.creditdefaultswap.unit.platform.service;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Feature("Backend Service")
@Story("Service Operations")
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock private Dependency dependency;
    @InjectMocks private Service service;

    @Test
    void shouldDoSomething() {
        when(dependency.getData()).thenReturn("data");
        assertEquals("expected", service.action());
        verify(dependency).getData();
    }
}
```

### Backend Integration Test (Spring)
```java
package com.creditdefaultswap.integration.platform.repository;

import com.creditdefaultswap.platform.CDSPlatformApplication;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@Feature("Backend Service")
@Story("Repository Integration")
@SpringBootTest(classes = CDSPlatformApplication.class)
@ActiveProfiles("test")
@Transactional
class RepositoryIntegrationTest {
    @Autowired private Repository repository;

    @Test
    void shouldPersist() {
        Entity saved = repository.save(new Entity());
        assertNotNull(saved.getId());
    }
}
```

### Frontend Component Test
```typescript
import { render, screen } from '@testing-library/react';
import { Component } from '../../../components/Component';

describe('Component', () => {
  it('should render', () => {
    render(<Component title="Test" />);
    expect(screen.getByText('Test')).toBeInTheDocument();
  });
});
```

### Frontend Hook Test
```typescript
import { renderHook, act } from '@testing-library/react';
import { useHook } from '../../../hooks/useHook';

describe('useHook', () => {
  it('should update state', () => {
    const { result } = renderHook(() => useHook());
    act(() => result.current.update('value'));
    expect(result.current.value).toBe('value');
  });
});
```

---

## Common Assertions

### Java (JUnit 5)
```java
// Equality
assertEquals(expected, actual);
assertNotEquals(value1, value2);

// Nullability
assertNull(value);
assertNotNull(value);

// Booleans
assertTrue(condition);
assertFalse(condition);

// Exceptions
assertThrows(Exception.class, () -> method());

// Collections
assertIterableEquals(expected, actual);
assertTrue(list.isEmpty());

// Custom message
assertEquals(expected, actual, "Custom failure message");
```

### TypeScript (Jest)
```typescript
// Equality
expect(value).toBe(expected);          // Strict equality (===)
expect(value).toEqual(expected);       // Deep equality

// Truthiness
expect(value).toBeTruthy();
expect(value).toBeFalsy();
expect(value).toBeNull();
expect(value).toBeUndefined();

// Numbers
expect(value).toBeGreaterThan(5);
expect(value).toBeLessThanOrEqual(10);

// Strings
expect(string).toContain('substring');
expect(string).toMatch(/pattern/);

// Arrays/Objects
expect(array).toContain(item);
expect(array).toHaveLength(3);
expect(object).toHaveProperty('key', 'value');

// DOM
expect(element).toBeInTheDocument();
expect(element).toHaveClass('active');
expect(element).toBeVisible();
```

---

## Mockito Patterns

```java
// Return values
when(mock.method()).thenReturn(value);
when(mock.method()).thenReturn(value1, value2); // Multiple calls

// Throw exceptions
when(mock.method()).thenThrow(new Exception());

// Argument matchers
when(mock.method(anyString())).thenReturn(value);
when(mock.method(eq("exact"))).thenReturn(value);
when(mock.method(any(Class.class))).thenReturn(value);

// Verify calls
verify(mock).method();
verify(mock, times(2)).method();
verify(mock, never()).method();
verify(mock, atLeast(1)).method();

// Argument capture
ArgumentCaptor<Type> captor = ArgumentCaptor.forClass(Type.class);
verify(mock).method(captor.capture());
assertEquals(expected, captor.getValue());
```

---

## Testing Library Patterns

```typescript
// Queries (prefer these in order)
screen.getByRole('button', { name: /submit/i })
screen.getByLabelText('Email')
screen.getByPlaceholderText('Enter name')
screen.getByText('Hello')
screen.getByDisplayValue('Current value')
screen.getByAltText('Image description')
screen.getByTestId('custom-element')

// User interactions
import { fireEvent } from '@testing-library/react';
fireEvent.click(element);
fireEvent.change(input, { target: { value: 'text' } });

// Async operations
import { waitFor } from '@testing-library/react';
await waitFor(() => {
  expect(screen.getByText('Loaded')).toBeInTheDocument();
});

// User events (more realistic)
import userEvent from '@testing-library/user-event';
const user = userEvent.setup();
await user.click(button);
await user.type(input, 'text');
```

---

## Allure Annotations

### Backend (@Feature / @Story)
```java
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Backend Service")           // Service or feature area
@Story("Credit Event Processing")     // Specific story/scenario
class YourTest {
    // Tests
}

// Standard Feature values:
// - "Backend Service" (cds-platform)
// - "Gateway Service" (gateway)
// - "Risk Engine Service" (risk-engine)
// - "Frontend Service" (frontend)
```

### Frontend (Test Helpers)
```typescript
import { withStoryId, describeStory } from '../utils/testHelpers';

// Option 1: withStoryId wrapper
describe('Tests', withStoryId(() => {
    it('should work', () => { });
}, 'STORY-001'));

// Option 2: describeStory
describeStory('Feature Name', 'STORY-001', () => {
    it('should work', () => { });
});

// Automatically adds [feature:Frontend Service] and [epic:...] tags
// Post-processed by scripts/add-frontend-labels.ps1
```

---

## Test Lifecycle Hooks

### Java (JUnit 5)
```java
@BeforeAll static void setupClass() { }       // Once before all tests
@BeforeEach void setup() { }                  // Before each test
@AfterEach void tearDown() { }                // After each test
@AfterAll static void tearDownClass() { }     // Once after all tests
```

### TypeScript (Jest)
```typescript
beforeAll(() => { });      // Once before all tests
beforeEach(() => { });     // Before each test
afterEach(() => { });      // After each test
afterAll(() => { });       // Once after all tests
```

---

## Decision: Unit vs Integration vs E2E

| When to Use | Unit | Integration | E2E |
|-------------|------|-------------|-----|
| **Testing** | Single class/function | Multiple components | Complete workflow |
| **Speed** | < 100ms | Seconds | Minutes |
| **Dependencies** | Mocked | Real (DB, HTTP) | Full stack |
| **Isolation** | High | Medium | Low |
| **Coverage** | Logic | Interactions | User flows |
| **Run Frequency** | Every commit | Every PR | Nightly/Release |

**Rule of thumb:** 70% Unit, 20% Integration, 10% E2E

---

## Import Path Reference

### Backend
```java
// Allure annotations
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

// Service classes (production code)
import com.creditdefaultswap.platform.service.YourService;

// Models
import com.creditdefaultswap.platform.model.YourEntity;
```

### Frontend
```typescript
// From unit test
import { Component } from '../../../components/Component';
import { useHook } from '../../../hooks/useHook';
import { util } from '../../../utils/util';

// From integration test  
import { Page } from '../../../pages/Page';

// Test helpers for Allure
import { withStoryId, describeStory } from '../utils/testHelpers';
```

---

## Common Issues & Fixes

| Issue | Solution |
|-------|----------|
| Package doesn't match directory | Update package to `com.creditdefaultswap.{unit\|integration\|e2e}.{domain}` |
| Can't find @Feature/@Story | Import `io.qameta.allure.Feature` and `io.qameta.allure.Story` |
| SpringBootTest fails | Add `@SpringBootTest(classes = CDSPlatformApplication.class)` |
| Frontend import error | Count `../` from test location to `src/` |
| Test timeout | Increase timeout or mock slow operations |
| Flaky test | Remove random data, fix timing issues, check async |

---

## Pro Tips

✅ **Name tests clearly:** `shouldCalculateSpreadWhenCdsIsActive()`  
✅ **One assertion per test:** Test one behavior at a time  
✅ **Arrange-Act-Assert:** Structure tests consistently  
✅ **Test edge cases:** Null, empty, boundary values  
✅ **Make tests fast:** Mock slow dependencies  
✅ **Clean up:** Use `@Transactional` or `@AfterEach`  
✅ **Fail fast:** Use specific assertions  
✅ **Keep DRY:** Extract common setup to helpers

❌ **Don't test private methods:** Test public API  
❌ **Don't share state:** Each test independent  
❌ **Don't use real services:** Mock external APIs  
❌ **Don't ignore flaky tests:** Fix or remove  
❌ **Don't skip assertions:** Every test must verify

---

**Full Guide:** [`GOLDEN_PATH.md`](./GOLDEN_PATH.md)  
**Architecture:** [`TEST_ARCHITECTURE.md`](./TEST_ARCHITECTURE.md)  
**Conventions:** [`backend-test-conventions.md`](./backend-test-conventions.md), [`frontend-test-conventions.md`](./frontend-test-conventions.md)
