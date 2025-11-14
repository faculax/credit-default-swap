# Backend Test Folder Conventions

**Story**: `UTS-2.1` — Backend Test Folder and Package Conventions  
**Status**: ✅ Implemented  
**Last Updated**: 2024

---

## 📋 Overview

This document defines the **standard test folder structure** for all Spring Boot backend services in the CDS Platform. Organizing tests by type (unit, integration, contract, e2e) ensures:

- Clear separation of fast unit tests vs. slower integration tests
- Ability to run different test suites independently via Maven profiles
- Consistent structure across all backend microservices
- Better IDE navigation and test discovery

---

## 🏗️ Folder Structure

All backend tests live under `src/test/java` following this hierarchy:

```
backend/
└── src/
    └── test/
        └── java/
            └── com/
                └── creditdefaultswap/
                    ├── unit/           ← Pure unit tests (no Spring context)
                    ├── integration/    ← Integration tests (@SpringBootTest)
                    ├── contract/       ← Contract tests (Spring Cloud Contract, Pact)
                    └── e2e/            ← End-to-end tests (optional, full system)
```

### Test Type Definitions

| Test Type | Directory | Purpose | Characteristics |
|-----------|-----------|---------|-----------------|
| **Unit** | `src/test/java/.../unit/` | Test individual classes/methods in isolation | • No Spring context<br>• Mocks for dependencies<br>• Fast (<100ms per test)<br>• Use `@ExtendWith(MockitoExtension.class)` |
| **Integration** | `src/test/java/.../integration/` | Test component interactions with real Spring beans | • Uses `@SpringBootTest`<br>• Real or embedded DB (H2)<br>• Slower (100ms-2s per test)<br>• Tests service layers, repositories |
| **Contract** | `src/test/java/.../contract/` | Consumer-driven contract tests between services | • Spring Cloud Contract or Pact<br>• Tests API contracts<br>• Provider-side or consumer-side |
| **E2E** | `src/test/java/.../e2e/` | Full system tests with all services running | • Optional (prefer integration)<br>• Requires Docker Compose<br>• Slowest (seconds per test) |

---

## 📦 Package Naming Conventions

Tests should mirror the **production package structure** within their test type folder:

```
src/test/java/com/creditdefaultswap/
├── unit/
│   ├── platform/
│   │   ├── service/
│   │   │   └── CreditEventServiceTest.java
│   │   ├── util/
│   │   │   └── DateUtilsTest.java
│   │   └── validator/
│   │       └── NotionalValidatorTest.java
│   └── testing/
│       └── validation/
│           └── LabelValidatorTest.java
│
├── integration/
│   └── platform/
│       ├── controller/
│       │   └── CreditEventControllerIntegrationTest.java
│       ├── repository/
│       │   └── CreditEventRepositoryIntegrationTest.java
│       └── service/
│           └── CashSettlementServiceIntegrationTest.java
│
└── contract/
    └── platform/
        └── api/
            └── CreditEventApiContractTest.java
```

**Naming Rules:**
- Unit tests: `{ClassName}Test.java`
- Integration tests: `{ClassName}IntegrationTest.java` or `{Feature}IntegrationTest.java`
- Contract tests: `{ClassName}ContractTest.java` or `{Api}ContractTest.java`
- E2E tests: `{Feature}E2ETest.java`

---

## 🚀 Maven Configuration

Tests are run via **Maven Surefire** with profiles for different test types:

### Run All Unit Tests (Default)
```bash
mvn test
```
Runs only `src/test/java/.../unit/**/*Test.java`

### Run Integration Tests
```bash
mvn test -P integration-tests
```
Runs `src/test/java/.../integration/**/*IntegrationTest.java`

### Run All Tests (Unit + Integration)
```bash
mvn test -P all-tests
```

### Run Contract Tests
```bash
mvn test -P contract-tests
```

---

## ✅ Test Annotation Requirements

### Unit Tests
```java
package com.creditdefaultswap.unit.platform.service;

import com.creditdefaultswap.platform.testing.story.StoryId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditEventServiceTest {
    
    @Test
    @StoryId(value = "UTS-401", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
    void shouldCalculateNotionalCorrectly() {
        // Pure unit test with mocked dependencies
    }
}
```

### Integration Tests
```java
package com.creditdefaultswap.integration.platform.service;

import com.creditdefaultswap.platform.testing.story.StoryId;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CreditEventServiceIntegrationTest {
    
    @Test
    @StoryId(value = "UTS-402", testType = StoryId.TestType.INTEGRATION, microservice = "cds-platform")
    void shouldPersistCreditEventToDatabase() {
        // Integration test with real Spring beans and embedded DB
    }
}
```

---

## 🔄 Migration Checklist

When migrating existing tests to this structure:

1. **Identify test type** — Is it a unit test (mocks only) or integration test (Spring context)?
2. **Move file** — Relocate from `src/test/java/.../service/` to `src/test/java/.../unit/service/` or `integration/service/`
3. **Update package declaration** — Change `package com.creditdefaultswap.platform.service;` to `package com.creditdefaultswap.unit.platform.service;`
4. **Update test name** — Add `IntegrationTest` suffix if it's an integration test
5. **Update @StoryId** — Set correct `testType = StoryId.TestType.UNIT` or `INTEGRATION`
6. **Update imports** — Production code imports remain the same (no `unit.` or `integration.` prefix)
7. **Run tests** — Verify with `mvn test` (unit) or `mvn test -P integration-tests`

**See Also:** [Backend Test Migration Checklist](./backend-test-migration-checklist.md) for detailed step-by-step instructions.

---

## 🎯 Quick Reference

| Scenario | Test Type | Location | Maven Command |
|----------|-----------|----------|---------------|
| Testing `CreditEventService.calculateNotional()` with mocks | Unit | `unit/platform/service/CreditEventServiceTest.java` | `mvn test` |
| Testing `CreditEventRepository.save()` with H2 | Integration | `integration/platform/repository/CreditEventRepositoryIntegrationTest.java` | `mvn test -P integration-tests` |
| Testing REST API `/api/credit-events` with MockMvc | Integration | `integration/platform/controller/CreditEventControllerIntegrationTest.java` | `mvn test -P integration-tests` |
| Testing contract between Gateway and CDS Platform | Contract | `contract/platform/api/CreditEventApiContractTest.java` | `mvn test -P contract-tests` |

---

## 🛠️ Enforcement

- **CI Pipeline** — Runs unit tests on every commit, integration tests on PR merge
- **Label Validation** — LabelValidator enforces correct `testType` labels at runtime
- **Pre-commit Hooks** — (Future) Check test file location matches declared `testType`

---

## 📚 Related Documentation

- [Unified Label Conventions](./unified-label-conventions.md) — Label schema and validation rules
- [Backend Test Migration Checklist](./backend-test-migration-checklist.md) — Step-by-step migration guide
- [Frontend Folder Conventions](./frontend-folder-conventions.md) — (Epic 02 Story 2.2)
- [Story Traceability Matrix](./story-traceability-matrix.md) — Linking tests to user stories

---

## ❓ FAQ

**Q: Can I have both unit and integration tests for the same class?**  
A: Yes! `unit/service/FooServiceTest.java` (fast mocked tests) and `integration/service/FooServiceIntegrationTest.java` (slower Spring tests) are both valid.

**Q: Where do I put testing utilities and helpers?**  
A: Put them in the appropriate test type folder: `unit/testing/util/` for unit test helpers, `integration/testing/util/` for integration test helpers.

**Q: What if a test uses mocks but also needs a Spring context?**  
A: That's an integration test. The presence of `@SpringBootTest` or `@DataJpaTest` makes it an integration test, regardless of mocking.

**Q: Should contract tests be in a separate microservice?**  
A: No, contract tests live in the producer microservice under `contract/`. Consumer-driven contracts are defined by the consumer and verified by the producer.

---

**Story Completion**: This document fulfills UTS-2.1 requirements for defining backend test folder and package conventions.
