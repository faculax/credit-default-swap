# Test Type Label Schema Guide

## Overview

The **Test Type Label Schema** is a centralized specification that defines valid test types, services, severity levels, and metadata for all automated tests across the Credit Default Swap (CDS) Platform. This ensures consistent test labeling, accurate reporting in Allure, and effective test governance.

**Schema Location:** `schema/test-type-schema.json`

---

## Purpose

### Problems Solved
- ❌ **Inconsistent test labeling** across frontend and backend
- ❌ **Invalid test type values** causing reporting issues
- ❌ **Unclear test categorization** making analysis difficult
- ❌ **No governance** over test metadata changes

### Benefits
- ✅ **Unified taxonomy** of test types across all services
- ✅ **Type-safe interfaces** (TypeScript) and constants (Java)
- ✅ **Automated validation** in CI pipeline
- ✅ **Better Allure reports** with consistent labels
- ✅ **Controlled evolution** through change management

---

## Schema Structure

### Test Types

The schema defines six standard test types:

| Test Type | Description | Severity | Coverage Target |
|-----------|-------------|----------|-----------------|
| `unit` | Single component/function in isolation | normal | 80% |
| `integration` | Multiple components with real dependencies | critical | 60% |
| `contract` | Consumer-provider API contract validation | critical | 100% |
| `e2e` | Complete user workflows across full system | blocker | 30% |
| `performance` | Load, stress, and resource utilization testing | normal | - |
| `security` | Vulnerability, auth, and data protection testing | blocker | - |

Each test type includes:
- **id**: Unique identifier (lowercase)
- **displayName**: Human-readable name
- **description**: Detailed explanation
- **defaultSeverity**: Recommended severity level
- **allowedSeverities**: Valid severity values
- **allureLabels**: Epic and feature tags for Allure
- **characteristics**: Key properties (execution time, dependencies, etc.)
- **examples**: Real-world use cases
- **applicableServices**: Which services can use this type
- **recommendedCoverageTarget**: Ideal coverage percentage (null if not applicable)

### Microservices

Four services are defined:

| Service ID | Display Name | Supported Test Types |
|------------|--------------|----------------------|
| `frontend` | Frontend (React) | unit, integration, contract, e2e |
| `backend` | Backend (CDS Platform) | unit, integration, contract, performance, security |
| `gateway` | API Gateway | unit, integration, contract, security |
| `risk-engine` | Risk Engine | unit, integration, performance |

### Validation Rules

```json
{
  "requiredLabels": ["storyId", "testType", "service"],
  "optionalLabels": ["microservice", "severity", "epic", "feature", "owner"],
  "storyIdPattern": "^(UTS|PROJ|EPIC)-\\d+(\\.\\d+)?$",
  "testTypeEnum": ["unit", "integration", "contract", "e2e", "performance", "security"],
  "severityEnum": ["trivial", "minor", "normal", "critical", "blocker"]
}
```

---

## Using the Schema

### Frontend (TypeScript/React)

The schema is integrated into `frontend/src/utils/testHelpers.ts`:

```typescript
import { describeStory, withStoryId } from '@utils/testHelpers';

describeStory(
  { 
    storyId: 'UTS-2.3', 
    testType: 'unit',        // ✅ From schema testTypes[].id
    service: 'frontend',     // ✅ From schema microservices[].id
    severity: 'normal',      // ✅ From schema validationRules.severityEnum
    epic: 'test-architecture',
    feature: 'schema-integration'
  },
  'Schema Integration Tests',
  () => {
    withStoryId({ 
      storyId: 'UTS-2.3', 
      testType: 'unit', 
      service: 'frontend' 
    })('should validate test type', () => {
      // Test implementation
    });
  }
);
```

**TypeScript Types:**
- `TestType`: `'unit' | 'integration' | 'contract' | 'e2e' | 'performance' | 'security'`
- `Service`: `'frontend' | 'backend' | 'gateway' | 'risk-engine'`
- `Severity`: `'trivial' | 'minor' | 'normal' | 'critical' | 'blocker'`

**Validation Functions:**
- `isValidTestType(type: string): boolean`
- `isValidService(service: string): boolean`
- `isValidSeverity(severity: string): boolean`
- `isValidStoryId(storyId: string): boolean`

### Backend (Java/Spring Boot)

The schema is integrated into `backend/src/main/java/com/creditdefaultswap/platform/testing/TestLabels.java`:

```java
import com.creditdefaultswap.platform.testing.TestLabels;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Schema Integration Tests [story:UTS-2.3] [testType:unit] [service:backend]")
class SchemaIntegrationTest {
    
    @Test
    @DisplayName("should validate test type [story:UTS-2.3] [testType:unit] [service:backend]")
    void shouldValidateTestType() {
        // Arrange
        String testType = TestLabels.TestType.UNIT;
        
        // Act
        boolean isValid = TestLabels.TestType.isValid(testType);
        
        // Assert
        assertTrue(isValid);
    }
}
```

**Java Constants:**
- `TestLabels.TestType.UNIT`, `INTEGRATION`, `CONTRACT`, `E2E`, `PERFORMANCE`, `SECURITY`
- `TestLabels.Service.FRONTEND`, `BACKEND`, `GATEWAY`, `RISK_ENGINE`
- `TestLabels.Severity.TRIVIAL`, `MINOR`, `NORMAL`, `CRITICAL`, `BLOCKER`
- `TestLabels.STORY_ID_PATTERN`

**Validation Methods:**
- `TestLabels.TestType.isValid(String testType): boolean`
- `TestLabels.Service.isValid(String service): boolean`
- `TestLabels.Severity.isValid(String severity): boolean`
- `TestLabels.isValidStoryId(String storyId): boolean`

---

## Automated Validation

### Validation Script

**Location:** `scripts/validate-test-schema.js`

**Usage:**
```powershell
# Run validation
node scripts/validate-test-schema.js

# Verbose output
node scripts/validate-test-schema.js --verbose

# Future: Auto-fix issues
node scripts/validate-test-schema.js --fix
```

**What It Checks:**
- ✅ Story ID format matches pattern `^(UTS|PROJ|EPIC)-\d+(\.\d+)?$`
- ✅ Test types are in schema `testTypes[].id`
- ✅ Services are in schema `microservices[].id`
- ✅ Severities (if present) are in schema `validationRules.severityEnum`
- ✅ Tests use `withStoryId()` or `describeStory()` helpers (frontend)
- ✅ Tests use `@DisplayName` with metadata (backend)

**Output Example:**
```
🔍 Validating test files against schema...

Schema version: 1.0.0
Last updated: 2025-01-14

📱 Validating frontend tests...
  Found 2 frontend test files

⚙️  Validating backend tests...
  Found 3 backend test files

================================================================================
📊 Validation Results

Total files scanned: 5
Valid files: 5
Files with issues: 0

✅ All tests validated successfully!
================================================================================
```

### CI Integration

Add to GitHub Actions workflow:

```yaml
- name: Validate Test Schema
  run: node scripts/validate-test-schema.js
```

This step runs after tests and fails the build if:
- Invalid story IDs are used
- Invalid test types/services/severities are specified
- Tests lack required metadata

---

## Change Management

### Governance Process

**Schema changes require:**
1. ✅ **Approval** from Tech Lead and QA Lead
2. ✅ **Documentation** updates in this guide
3. ✅ **Backward compatibility** for at least 2 minor versions
4. ✅ **Team notification** via Slack and email
5. ✅ **Migration period** (minimum 2 sprints for deprecations)

### Adding a New Test Type

**Steps:**

1. **Propose Change**
   - Create GitHub issue with rationale
   - Include use cases and examples
   - Tag `@tech-lead` and `@qa-lead`

2. **Update Schema**
   ```json
   {
     "id": "smoke",
     "displayName": "Smoke Test",
     "description": "Quick sanity check after deployment",
     "defaultSeverity": "critical",
     "allowedSeverities": ["normal", "critical", "blocker"],
     "allureLabels": {
       "epic": "smoke",
       "feature": "deployment-validation"
     },
     "characteristics": ["Very fast execution", "Post-deployment checks"],
     "examples": ["Testing health endpoints", "Basic UI load check"],
     "applicableServices": ["frontend", "backend", "gateway"],
     "recommendedCoverageTarget": null
   }
   ```

3. **Update TypeScript Types**
   - Modify `frontend/src/utils/testHelpers.ts`
   - Add `'smoke'` to `TestType` union
   - Update `isValidTestType()` function

4. **Update Java Constants**
   - Modify `backend/src/main/java/.../testing/TestLabels.java`
   - Add `public static final String SMOKE = "smoke";`
   - Update `TestType.isValid()` method

5. **Update Validation Script**
   - No changes needed (reads from schema automatically)

6. **Document and Communicate**
   - Update this guide with new test type details
   - Announce in `#engineering` and `#testing` Slack channels
   - Send email to team distribution list

7. **Validate**
   - Run `node scripts/validate-test-schema.js --verbose`
   - Ensure no existing tests break
   - Create example test using new type

### Deprecating a Test Type

**Example: Deprecate `smoke` test type**

1. **Add Deprecation Flag to Schema**
   ```json
   {
     "id": "smoke",
     "displayName": "Smoke Test",
     "deprecated": true,
     "deprecationReason": "Merged into e2e test type",
     "deprecationDate": "2025-03-01",
     "migrationGuide": "Use testType: 'e2e' with severity: 'blocker' instead"
   }
   ```

2. **Announce Deprecation** (2 sprint notice minimum)
   - Post in Slack with migration instructions
   - Email team with timeline
   - Update documentation with alternatives

3. **Migration Period**
   - Update existing tests to use new type
   - Validation script warns about deprecated type
   - CI still passes (warnings only)

4. **Remove from Schema** (after 2 sprints)
   - Delete test type from `testTypes[]` array
   - Update TypeScript types and Java constants
   - Validation script now errors on usage
   - CI fails if deprecated type found

### Modifying Existing Test Type

**Guidelines:**
- ✅ **Can change**: `description`, `characteristics`, `examples`, `recommendedCoverageTarget`
- ⚠️ **Requires review**: `defaultSeverity`, `allowedSeverities`, `applicableServices`
- ❌ **Cannot change**: `id` (breaking change - use deprecation process)

**Process:**
1. Update schema JSON
2. Increment schema `version` (semantic versioning)
3. Update `lastUpdated` timestamp
4. Document change in schema `changeLog` section
5. No code changes needed (TypeScript/Java read from schema)
6. Announce change to team

---

## Schema Version History

### Version 1.0.0 (2025-01-14)
- Initial schema release
- Defined 6 test types: unit, integration, contract, e2e, performance, security
- Defined 4 services: frontend, backend, gateway, risk-engine
- Established validation rules and required labels
- Created TypeScript and Java integrations
- Built validation script

---

## Frequently Asked Questions

### Q: Can I add custom test types for my service?

**A:** No. Test types must be defined in the shared schema to ensure consistency across all services. If you need a new test type, follow the "Adding a New Test Type" process above.

### Q: What if my test doesn't fit any category?

**A:** All tests should fit one of the six types. If you're unsure:
- **Single component in isolation?** → `unit`
- **Multiple components or APIs?** → `integration`
- **API contract validation?** → `contract`
- **Full user workflow?** → `e2e`
- **Load/stress testing?** → `performance`
- **Security/auth testing?** → `security`

Contact the Tech Lead if genuinely unclear.

### Q: Can I use multiple test types on one test?

**A:** No. Each test must have exactly one `testType`. If a test serves multiple purposes, classify it by its primary purpose. For example, a test that validates API contract AND checks performance should be `contract` if that's the main goal, or `performance` if that's the focus.

### Q: Do I need to label every single test?

**A:** Yes. All tests must have:
- `storyId`: Story/feature identifier
- `testType`: One of the schema-defined types
- `service`: Which service the test belongs to

Optional but recommended:
- `severity`: Priority level
- `epic`: Epic grouping for reports
- `feature`: Feature categorization

### Q: What happens if validation fails in CI?

**A:** The build fails, and you'll see error messages like:
```
❌ Errors (2):
  frontend/src/__tests__/unit/MyTest.test.tsx:15 - Invalid test type: "smoketest". Valid types: unit, integration, contract, e2e, performance, security
  backend/src/test/java/MyTest.java:23 - Invalid story ID format: "STORY-ABC". Expected pattern: ^(UTS|PROJ|EPIC)-\d+(\.\d+)?$
```

Fix the errors and re-run tests.

### Q: Can I run validation locally before pushing?

**A:** Yes! Run:
```powershell
node scripts/validate-test-schema.js
```

Add this to your pre-commit hook:
```bash
#!/bin/sh
node scripts/validate-test-schema.js
if [ $? -ne 0 ]; then
  echo "❌ Test schema validation failed. Fix errors before committing."
  exit 1
fi
```

### Q: How do I update the schema?

**A:** Follow the Change Management process above. Schema changes require approval from Tech Lead and QA Lead. Never edit the schema JSON directly without following the governance process.

### Q: Where can I see how schema values appear in Allure?

**A:** After running tests:
1. Generate Allure report: `npm run test:allure` (frontend) or `./mvnw allure:serve` (backend)
2. Open the report
3. Check "Behaviors" tab → tests grouped by Epic and Feature
4. Check "Suites" tab → tests grouped by Service and Test Type
5. Click a test → see all labels (story, severity, etc.)

---

## Schema File Reference

**Full Schema:** [`schema/test-type-schema.json`](../../schema/test-type-schema.json)

**Key Sections:**
- `testTypes[]`: All valid test type definitions
- `microservices[]`: All valid service definitions
- `validationRules`: Patterns and enums for validation
- `allureConfiguration`: Allure-specific settings
- `changeManagement`: Governance rules
- `metadata`: Schema ownership and documentation links

---

## Related Documentation

- **Frontend Test Conventions:** [`docs/testing/frontend-folder-conventions.md`](./frontend-folder-conventions.md)
- **Backend Test Conventions:** [`docs/testing/backend-folder-conventions.md`](./backend-folder-conventions.md)
- **Frontend Migration Checklist:** [`docs/testing/frontend-test-migration-checklist.md`](./frontend-test-migration-checklist.md)
- **Backend Migration Checklist:** [`docs/testing/backend-test-migration-checklist.md`](./backend-test-migration-checklist.md)

---

## Support & Feedback

- **Questions:** Post in `#testing` or `#engineering` Slack channels
- **Issues:** Open GitHub issue with label `test-schema`
- **Schema Changes:** Follow Change Management process above
- **Maintainers:** Tech Lead (`tech-lead@example.com`), QA Lead (`qa-lead@example.com`)

---

**Last Updated:** 2025-01-14  
**Schema Version:** 1.0.0  
**Document Owner:** Platform Engineering
