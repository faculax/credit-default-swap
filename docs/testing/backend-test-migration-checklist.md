# Backend Test Migration Checklist

**Story**: `UTS-2.1` — Backend Test Folder and Package Conventions  
**Purpose**: Step-by-step guide for migrating existing tests to the new folder structure  
**Last Updated**: 2024

---

## 📋 Overview

This checklist helps you migrate existing tests from the flat `src/test/java` structure to the organized **test type folders** (unit/, integration/, contract/).

**Before Migration:**
```
src/test/java/com/creditdefaultswap/platform/
└── service/
    ├── CreditEventServiceTest.java        ← Mixed unit and integration tests
    └── CashSettlementServiceTest.java
```

**After Migration:**
```
src/test/java/com/creditdefaultswap/
├── unit/platform/service/
│   ├── CreditEventServiceTest.java        ← Pure unit tests
│   └── CashSettlementServiceTest.java
└── integration/platform/service/
    └── CreditEventServiceIntegrationTest.java  ← Integration tests
```

---

## 🎯 Step-by-Step Migration Process

### Step 1: Identify Test Type

For each test class, determine if it's a **unit test** or **integration test**:

| Indicator | Unit Test ✅ | Integration Test ✅ |
|-----------|-------------|-------------------|
| Uses `@SpringBootTest` | ❌ No | ✅ Yes |
| Uses `@DataJpaTest`, `@WebMvcTest` | ❌ No | ✅ Yes |
| Uses `@ExtendWith(MockitoExtension.class)` | ✅ Yes | ❌ No (usually) |
| All dependencies are mocked | ✅ Yes | ❌ No |
| Tests interact with database (H2, TestContainers) | ❌ No | ✅ Yes |
| Tests call REST endpoints with MockMvc | ❌ No | ✅ Yes |
| Test execution time | < 100ms | 100ms - 2s |

**Rule of Thumb:** If the test uses Spring's application context, it's an **integration test**. If it only uses mocks, it's a **unit test**.

---

### Step 2: Move Test File to New Location

#### For Unit Tests

**From:**
```
src/test/java/com/creditdefaultswap/platform/service/CreditEventServiceTest.java
```

**To:**
```
src/test/java/com/creditdefaultswap/unit/platform/service/CreditEventServiceTest.java
```

**Action:**
```bash
# Create target directory
mkdir -p backend/src/test/java/com/creditdefaultswap/unit/platform/service

# Move file
mv backend/src/test/java/com/creditdefaultswap/platform/service/CreditEventServiceTest.java \
   backend/src/test/java/com/creditdefaultswap/unit/platform/service/CreditEventServiceTest.java
```

#### For Integration Tests

**From:**
```
src/test/java/com/creditdefaultswap/platform/service/CreditEventServiceTest.java
```

**To:**
```
src/test/java/com/creditdefaultswap/integration/platform/service/CreditEventServiceIntegrationTest.java
```

**Action:**
```bash
# Create target directory
mkdir -p backend/src/test/java/com/creditdefaultswap/integration/platform/service

# Move and rename file
mv backend/src/test/java/com/creditdefaultswap/platform/service/CreditEventServiceTest.java \
   backend/src/test/java/com/creditdefaultswap/integration/platform/service/CreditEventServiceIntegrationTest.java
```

---

### Step 3: Update Package Declaration

**Unit Test — Change package:**

```diff
- package com.creditdefaultswap.platform.service;
+ package com.creditdefaultswap.unit.platform.service;
```

**Integration Test — Change package:**

```diff
- package com.creditdefaultswap.platform.service;
+ package com.creditdefaultswap.integration.platform.service;
```

**⚠️ Important:** Production code imports **do NOT change**. Only the test package changes.

```java
// These imports stay the same:
import com.creditdefaultswap.platform.service.CreditEventService;  // ✅ Correct
import com.creditdefaultswap.platform.repository.CreditEventRepository;  // ✅ Correct

// NOT like this:
import com.creditdefaultswap.unit.platform.service.CreditEventService;  // ❌ Wrong!
```

---

### Step 4: Rename Integration Test Classes

Integration tests should have a clear suffix to distinguish them:

**Before:**
```java
class CreditEventServiceTest { ... }
```

**After:**
```java
class CreditEventServiceIntegrationTest { ... }
```

**Acceptable Suffixes:**
- `IntegrationTest` (preferred)
- `IT` (Maven Failsafe convention)
- `IntTest`

---

### Step 5: Add @Feature and @Story Annotations

Add `@Feature` and `@Story` annotations at the class level for Allure Behaviors grouping:

**Unit Test:**
```java
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Backend Service")
@Story("Credit Event Processing")
class CreditEventServiceTest {
    
    @Test
    void shouldCalculateNotionalCorrectly() { ... }
}
```

**Integration Test:**
```java
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Backend Service")
@Story("Credit Event Integration")
class CreditEventServiceIntegrationTest {
    
    @Test
    void shouldPersistCreditEventToDatabase() { ... }
}
```

**⚠️ Best Practice:** Use consistent Feature names across related tests for proper Behaviors view grouping.

---

### Step 6: Verify Test Class Annotations

**Unit Test — Typical Annotations:**
```java
@ExtendWith(MockitoExtension.class)
class CreditEventServiceTest {
    @Mock
    private CreditEventRepository repository;
    
    @InjectMocks
    private CreditEventService service;
}
```

**Integration Test — Typical Annotations:**
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CreditEventServiceIntegrationTest {
    @Autowired
    private CreditEventService service;
    
    @Autowired
    private CreditEventRepository repository;
}
```

---

### Step 7: Run Tests and Verify

**Run Unit Tests:**
```bash
mvn test
# Or explicitly:
mvn test -P unit-tests
```

**Run Integration Tests:**
```bash
mvn test -P integration-tests
```

**Run All Tests:**
```bash
mvn test -P all-tests
```

**Expected Output:**
```
[INFO] --- maven-surefire-plugin:3.2.5:test (default-test) @ cds-platform ---
[INFO] Running com.creditdefaultswap.unit.platform.service.CreditEventServiceTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

### Step 8: Clean Up Old Test Directories

After migrating all tests, remove empty old directories:

```bash
# Check for empty directories
find backend/src/test/java/com/creditdefaultswap/platform -type d -empty

# Remove empty directories
find backend/src/test/java/com/creditdefaultswap/platform -type d -empty -delete
```

**⚠️ Warning:** Only delete directories that are **completely empty** after migration.

---

## 🔍 Migration Example: Full Walkthrough

### Before: Mixed Test

**File:** `src/test/java/com/creditdefaultswap/platform/service/CreditEventServiceTest.java`

```java
package com.creditdefaultswap.platform.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditEventServiceTest {
    
    @Mock
    private CreditEventRepository repository;
    
    @InjectMocks
    private CreditEventService service;
    
    @Test
    void shouldCalculateNotional() {
        // Unit test logic
    }
}
```

### After: Unit Test

**File:** `src/test/java/com/creditdefaultswap/unit/platform/service/CreditEventServiceTest.java`

```java
package com.creditdefaultswap.unit.platform.service;

import com.creditdefaultswap.platform.service.CreditEventService;  // ✅ Production import
import com.creditdefaultswap.platform.repository.CreditEventRepository;  // ✅ Production import
import com.creditdefaultswap.platform.testing.story.StoryId;  // ✅ Testing utility
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Backend Service")
@Story("Credit Event Processing")
@ExtendWith(MockitoExtension.class)
class CreditEventServiceTest {
    
    @Mock
    private CreditEventRepository repository;
    
    @InjectMocks
    private CreditEventService service;
    
    @Test
    void shouldCalculateNotional() {
        // Unit test logic
    }
}
```

---

## 🚨 Common Pitfalls

| Mistake | Fix |
|---------|-----|
| ❌ Importing production classes with `unit.` prefix | ✅ Import from production packages directly |
| ❌ Mixing unit and integration tests in same file | ✅ Split into separate files (unit vs integration) |
| ❌ Using `@SpringBootTest` in unit tests | ✅ Use `@ExtendWith(MockitoExtension.class)` instead |
| ❌ Missing @Feature/@Story annotations | ✅ Add at class level for Allure Behaviors grouping |
| ❌ Renaming integration test but keeping `Test` suffix | ✅ Use `IntegrationTest` suffix |
| ❌ Running `mvn test` expecting all tests | ✅ Use `mvn test -P all-tests` to run all test types |

---

## 📊 Migration Progress Tracker

Use this checklist to track migration progress:

- [ ] Identify all test files in `src/test/java`
- [ ] Classify each test as unit, integration, or contract
- [ ] Create target directories (unit/, integration/, contract/)
- [ ] Move unit tests to `unit/` folders
- [ ] Move integration tests to `integration/` folders
- [ ] Update package declarations in all moved tests
- [ ] Rename integration test classes with `IntegrationTest` suffix
- [ ] Add `@Feature` and `@Story` annotations at class level
- [ ] Run `mvn test` to verify unit tests pass
- [ ] Run `mvn test -P integration-tests` to verify integration tests pass
- [ ] Run `mvn test -P all-tests` to verify all tests pass
- [ ] Clean up empty old directories
- [ ] Update CI pipeline to run different test profiles
- [ ] Document any custom test utilities or helpers

---

## 🛠️ Automation Scripts

### Find All Test Files

```bash
# List all test files
find backend/src/test/java -name "*Test.java" -type f
```

### Find Tests Using @SpringBootTest (Integration Tests)

```bash
# Identify integration tests by annotation
grep -r "@SpringBootTest" backend/src/test/java --include="*Test.java"
```

### Find Tests Using MockitoExtension (Unit Tests)

```bash
# Identify unit tests by annotation
grep -r "@ExtendWith(MockitoExtension.class)" backend/src/test/java --include="*Test.java"
```

---

## 📚 Related Documentation

- [Backend Folder Conventions](./backend-folder-conventions.md) — Complete folder structure reference
- [Unified Label Conventions](./unified-label-conventions.md) — Label validation rules
- [Story Traceability Matrix](./story-traceability-matrix.md) — Linking tests to stories

---

## ❓ FAQ

**Q: What if a test uses both mocks and Spring context?**  
A: If it uses `@SpringBootTest`, it's an integration test, even if it also uses mocks.

**Q: Can I have both unit and integration tests for the same class?**  
A: Yes! `unit/.../FooServiceTest.java` and `integration/.../FooServiceIntegrationTest.java` are both valid.

**Q: Do I need to update the Maven pom.xml?**  
A: The pom.xml has already been updated with Surefire profiles. You only need to move tests.

**Q: What about test utilities and helper classes?**  
A: Put them in the appropriate test type folder: `unit/testing/util/` or `integration/testing/util/`.

**Q: Can I migrate tests incrementally?**  
A: Yes! You can migrate one test class at a time. The old and new structures can coexist temporarily.

---

**Migration Support:** If you encounter issues, refer to the [Backend Folder Conventions](./backend-folder-conventions.md) or consult the team.
