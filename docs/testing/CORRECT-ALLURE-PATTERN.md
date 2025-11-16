# Correct Allure Annotation Pattern

## ✅ CORRECT Pattern

### Class Level
```java
import io.qameta.allure.Epic;

@Epic("Unit Tests")  // or "Integration Tests" or "E2E Tests"
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
```

### Method Level
```java
@Test
@Feature("Backend Service")  // or "Frontend Service", "Gateway Service", "Risk Engine Service"
@Story("Specific scenario description")
void testMethod() {
    // test code
}
```

## ❌ WRONG Pattern (Old)
```java
@Epic("Unit Tests")
@Feature("Backend Service")
@Story("Specific scenario")  // <- WRONG: wastes Story at class level
class MyServiceTest {
    
    @Test  // <- WRONG: no Feature/Story at method level
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

## 📝 Enums (Optional but Recommended)

```java
// Use EpicType enum for consistency
import com.creditdefaultswap.unit.platform.testing.allure.EpicType;
import com.creditdefaultswap.unit.platform.testing.allure.FeatureType;

@Epic(EpicType.UNIT_TESTS.getValue())
class MyTest {
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE.getValue())
    @Story("Specific scenario")
    void test() {}
}
```

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
