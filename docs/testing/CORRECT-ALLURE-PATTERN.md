# Correct Allure Annotation Pattern

## ✅ CORRECT Pattern

### Class Level
```java
import com.creditdefaultswap.unit.platform.testing.allure.EpicType;
import io.qameta.allure.Epic;

@Epic(EpicType.UNIT_TESTS)  // or INTEGRATION_TESTS or E2E_TESTS
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
```

### Method Level
```java
import com.creditdefaultswap.unit.platform.testing.allure.FeatureType;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Test
@Feature(FeatureType.BACKEND_SERVICE)  // or FRONTEND_SERVICE, GATEWAY_SERVICE, RISK_ENGINE_SERVICE
@Story("Specific scenario description")
void testMethod() {
    // test code
}
```

## ❌ WRONG Pattern (Old)
```java
@Epic("Unit Tests")  // ❌ Using string literal instead of EpicType constant
@Feature("Backend Service")  // ❌ WRONG: class level Feature wasted
@Story("Specific scenario")  // ❌ WRONG: wastes Story at class level
class MyServiceTest {
    
    @Test  // ❌ WRONG: no Feature/Story at method level
    void testMethod() {
    }
}
```

## 📊 Results in Allure Behaviors View

```
Behaviors
├── Unit Tests (Epic - from class annotation)
│   ├── Backend Service (Feature - from method annotation)
│   │   ├── Credit Event Processing - Record New Event (Story - from method annotation)
│   │   ├── Credit Event Processing - Idempotent Existing Event (Story)
│   │   └── Credit Event Processing - Trade Not Found Error (Story)
│   ├── Frontend Service (Feature)
│   │   ├── Component Rendering - Initial State (Story)
│   │   └── Component Rendering - With Props (Story)
│   └── Gateway Service (Feature)
│       └── Version API - Returns Version Info (Story)
├── Integration Tests (Epic)
│   └── Backend Service (Feature)
│       ├── CDS Trade Repository - Save And Retrieve (Story)
│       └── CDS Trade Repository - Delete By Id (Story)
└── E2E Tests (Epic)
    └── Backend Service (Feature)
        └── Full Trade Lifecycle (Story)
```

## 🎯 Key Points

1. **@Epic** = Test Type (Unit/Integration/E2E) - **CLASS LEVEL ONLY**
2. **@Feature** = Service/Component - **METHOD LEVEL** (can also be class level if all tests are same feature)
3. **@Story** = Specific test scenario - **METHOD LEVEL ONLY**

This creates a 3-level hierarchy: Epic → Feature → Story

## 📝 Constants (REQUIRED)

**Always use `EpicType` and `FeatureType` constants** for consistency and compile-time safety:

```java
// EpicType and FeatureType are constant classes (not enums)
import com.creditdefaultswap.unit.platform.testing.allure.EpicType;
import com.creditdefaultswap.unit.platform.testing.allure.FeatureType;

@Epic(EpicType.UNIT_TESTS)  // EpicType.UNIT_TESTS, INTEGRATION_TESTS, E2E_TESTS
class MyTest {
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)  // FeatureType.BACKEND_SERVICE, FRONTEND_SERVICE, GATEWAY_SERVICE, RISK_ENGINE_SERVICE
    @Story("Specific scenario")
    void test() {}
}
```

**Available Constants:**
- **EpicType:** `UNIT_TESTS`, `INTEGRATION_TESTS`, `E2E_TESTS`
- **FeatureType:** `BACKEND_SERVICE`, `FRONTEND_SERVICE`, `GATEWAY_SERVICE`, `RISK_ENGINE_SERVICE`

## Frontend Pattern

```typescript
// Test file structure
describeStory('ComponentName', { testType: 'unit' }, () => {
  
  it('should do something', withStoryId({ 
    feature: 'Frontend Service',
    story: 'Component Rendering - Should Do Something'
  }, () => {
    // test code
  }));
  
});
```

This generates:
- `[epic:Unit Tests]` from testType
- `[feature:Frontend Service]` from feature param
- `[story:Component Rendering - Should Do Something]` from story param

Post-processing script extracts these tags into Allure labels.
