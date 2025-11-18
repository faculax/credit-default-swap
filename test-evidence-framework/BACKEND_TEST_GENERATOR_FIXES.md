# Backend Test Generator - Error Fixes

## TypeScript Compilation Errors Summary

**Total Errors**: 51  
**Primary Issues**: Interface property mismatches, type errors, import preferences

---

## Fix Priority 1: Property Access Errors (CRITICAL)

### Issue 1: story.id doesn't exist
**Locations**: Lines 286, 396, 398, 434

**Current Code**:
```typescript
story.id
```

**Fix**:
```typescript
story.storyId
```

**Explanation**: `StoryModel` interface uses `storyId` property, not `id`.

---

### Issue 2: story.epic doesn't exist
**Locations**: Lines 394, 433

**Current Code**:
```typescript
epic: story.epic
```

**Fix**:
```typescript
epic: story.epicTitle || 'Unknown Epic'
```

**Explanation**: `StoryModel` has `epicTitle` (optional), not `epic`.

---

### Issue 3: story.priority doesn't exist
**Location**: Line 406

**Current Code**:
```typescript
private determineSeverity(story: StoryModel): 'BLOCKER' | 'CRITICAL' | 'NORMAL' | 'MINOR' | 'TRIVIAL' {
  const priority = story.priority?.toLowerCase() || 'medium';
  // ...
}
```

**Fix Option 1** (Infer from story ID):
```typescript
private determineSeverity(story: StoryModel): 'BLOCKER' | 'CRITICAL' | 'NORMAL' | 'MINOR' | 'TRIVIAL' {
  // Infer priority from story characteristics
  // Epic 3-4 = CRITICAL (core trading), Epic 5-6 = NORMAL, Epic 7+ = MINOR
  const epicMatch = story.epicPath?.match(/epic_(\d+)/);
  const epicNum = epicMatch ? parseInt(epicMatch[1]) : 5;
  
  if (epicNum <= 4) return 'CRITICAL';
  if (epicNum <= 6) return 'NORMAL';
  return 'MINOR';
}
```

**Fix Option 2** (Add priority to StoryModel):
```typescript
// In story-model.ts
export interface StoryModel {
  // ... existing properties
  priority?: 'Critical' | 'High' | 'Medium' | 'Low';
}
```

---

### Issue 4: testPlan.testTypes doesn't exist
**Location**: Line 113

**Current Code**:
```typescript
private selectTemplates(testPlan: TestPlan): BackendTestTemplate[] {
  const testTypes = testPlan.testTypes || [];
  // ...
}
```

**Fix**:
```typescript
private selectTemplates(testPlan: TestPlan): BackendTestTemplate[] {
  // Extract test types from plannedTests array
  const testTypes = testPlan.plannedTests
    .flatMap(pt => pt.testTypes)
    .filter((type, index, self) => self.indexOf(type) === index); // unique
  // ...
}
```

**Explanation**: `TestPlan` has `plannedTests: PlannedTest[]`, each with `testTypes: TestType[]`.

---

## Fix Priority 2: Type Mismatches

### Issue 5: acceptanceCriteria type mismatch
**Location**: Line 192

**Current Code**:
```typescript
const methods = this.parseAcceptanceCriteria(story.acceptanceCriteria);
// story.acceptanceCriteria is string[], but function expects string
```

**Fix**:
```typescript
const methods = this.parseAcceptanceCriteria(story.acceptanceCriteria.join('\n'));
```

**Update function signature** (optional, better approach):
```typescript
private parseAcceptanceCriteria(criteria: string | string[]): Array<{
  description: string;
  expected: string;
}> {
  // Handle both string and string[]
  const content = Array.isArray(criteria) ? criteria.join('\n') : criteria;
  
  const results: Array<{ description: string; expected: string }> = [];
  // ... rest of parsing logic
}
```

---

## Fix Priority 3: Import Preferences

### Issue 6: Use node:fs and node:path
**Location**: Top of file

**Current Code**:
```typescript
import * as fs from 'fs';
import * as path from 'path';
```

**Fix**:
```typescript
import * as fs from 'node:fs';
import * as path from 'node:path';
```

---

## Fix Priority 4: Unused Imports

### Issue 7: Remove unused imports
**Location**: Top of file

**Current Code**:
```typescript
import {
  BackendTestTemplate,
  BackendTestGenerationConfig,
  TestMethod,
  GeneratedTestClass,
  TestField,
  TestSetupMethod,
  GeneratedTestMethod,
  AllureAnnotations,
  GenerationResult,
  ServiceName,        // ❌ UNUSED
  TemplateContext     // ❌ UNUSED
} from '../models/backend-test-model';
```

**Fix**:
```typescript
import {
  BackendTestTemplate,
  BackendTestGenerationConfig,
  TestMethod,
  GeneratedTestClass,
  TestField,
  TestSetupMethod,
  GeneratedTestMethod,
  AllureAnnotations,
  GenerationResult
} from '../models/backend-test-model';
```

---

## Fix Priority 5: Code Style

### Issue 8: forEach → for...of
**Location**: Multiple locations

**Current Code**:
```typescript
methods.forEach(method => {
  // ...
});
```

**Fix**:
```typescript
for (const method of methods) {
  // ...
}
```

---

### Issue 9: Extract nested ternary
**Example Location**: buildClassName method

**Current Code**:
```typescript
const suffix = template === 'service' 
  ? 'ServiceTest' 
  : template === 'repository' 
    ? 'RepositoryTest' 
    : template === 'controller' 
      ? 'ControllerTest' 
      : 'Test';
```

**Fix**:
```typescript
const suffixMap: Record<BackendTestTemplate, string> = {
  service: 'ServiceTest',
  repository: 'RepositoryTest',
  controller: 'ControllerTest',
  integration: 'IntegrationTest',
  unit: 'Test'
};
const suffix = suffixMap[template];
```

---

## Complete Fixed selectTemplates Method

```typescript
private selectTemplates(testPlan: TestPlan): BackendTestTemplate[] {
  // Extract unique test types from plannedTests
  const testTypes = testPlan.plannedTests
    .flatMap(pt => pt.testTypes)
    .filter((type, index, self) => self.indexOf(type) === index);
  
  const templates: BackendTestTemplate[] = [];
  
  for (const type of testTypes) {
    if (type === 'unit') {
      templates.push('service', 'unit');
    } else if (type === 'integration') {
      templates.push('integration');
    } else if (type === 'api') {
      templates.push('controller');
    }
  }
  
  // Default to service test if no types specified
  if (templates.length === 0) {
    templates.push('service');
  }
  
  // Remove duplicates
  return [...new Set(templates)];
}
```

---

## Complete Fixed determineSeverity Method

```typescript
private determineSeverity(story: StoryModel): 'BLOCKER' | 'CRITICAL' | 'NORMAL' | 'MINOR' | 'TRIVIAL' {
  // Infer severity from epic number (lower epic = more critical)
  const epicMatch = story.epicPath?.match(/epic_(\d+)/);
  const epicNum = epicMatch ? parseInt(epicMatch[1]) : 5;
  
  // Epic 1-4: Core functionality (CRITICAL)
  if (epicNum <= 4) return 'CRITICAL';
  
  // Epic 5-6: Important features (NORMAL)
  if (epicNum <= 6) return 'NORMAL';
  
  // Epic 7+: Nice-to-have (MINOR)
  return 'MINOR';
}
```

---

## Complete Fixed buildAllureAnnotations Method

```typescript
private buildAllureAnnotations(story: StoryModel): AllureAnnotations {
  return {
    epic: story.epicTitle || 'Unknown Epic',
    feature: story.title,
    story: `${story.storyId}: ${story.title}`,
    severity: this.determineSeverity(story),
    tmsLink: story.normalizedId,
    issue: story.normalizedId
  };
}
```

---

## Complete Fixed findDatasetForTest Method

```typescript
private findDatasetForTest(story: StoryModel, template: BackendTestTemplate): string | undefined {
  if (!this.datasetRegistry) {
    this.loadDatasetRegistry();
  }
  
  // 1. Try to find by story ID
  const byStoryId = this.datasetRegistry.datasets?.find(
    (ds: any) => ds.storyId === story.storyId
  );
  if (byStoryId) return byStoryId.path;
  
  // 2. Try to find by title keywords
  const titleLower = story.title.toLowerCase();
  const keywords = ['trade', 'pricing', 'portfolio', 'credit', 'event', 'margin'];
  
  for (const keyword of keywords) {
    if (titleLower.includes(keyword)) {
      const byKeyword = this.datasetRegistry.datasets?.find(
        (ds: any) => ds.path.toLowerCase().includes(keyword)
      );
      if (byKeyword) return byKeyword.path;
    }
  }
  
  // 3. Fallback defaults by template type
  const defaults: Record<string, string> = {
    service: 'datasets/cds-trades/single-name-buy-protection.json',
    repository: 'datasets/cds-trades/single-name-buy-protection.json',
    controller: 'datasets/cds-trades/single-name-buy-protection.json',
    integration: 'datasets/cds-trades/single-name-buy-protection.json',
    unit: 'datasets/cds-trades/single-name-buy-protection.json'
  };
  
  return defaults[template];
}
```

---

## CLI Fixes (generate-backend-tests.ts)

### Remove unused import
```typescript
// ❌ Remove
import * as fs from 'node:fs';

// Keep only what's used
import * as path from 'node:path';
```

### Fix type annotations
```typescript
// Add type annotations to lambda parameters
filteredStories = stories.filter((story: StoryModel) =>
  story.servicesInvolved.includes(argv.service as ServiceName)
);

filteredStories = filteredStories.filter((story: StoryModel) =>
  story.storyId?.includes(argv.story!) || story.title.includes(argv.story!)
);

const backendPlans = allPlans.filter((plan: TestPlan) =>
  ['backend', 'gateway', 'risk-engine'].includes(plan.service)
);

const story = filteredStories.find((s: StoryModel) => s.storyId === plan.storyId);
```

### Fix forEach → for...of
```typescript
// ❌ Current
results.forEach(result => {
  // ...
});

// ✅ Fix
for (const result of results) {
  // ...
}
```

### Fix top-level await
```typescript
// ❌ Current
main().catch(error => {
  console.error('\n❌ Fatal error:', error);
  process.exit(1);
});

// ✅ Fix
try {
  await main();
} catch (error) {
  console.error('\n❌ Fatal error:', error);
  process.exit(1);
}
```

### Fix method calls
```typescript
// parseDirectory doesn't exist on StoryParser
// ❌ Current
const stories = parser.parseDirectory(argv.root);

// ✅ Fix - read the actual parser implementation
// Need to check StoryParser API
```

---

## Summary of Changes

| File | Lines Changed | Critical Fixes | Style Fixes |
|------|---------------|----------------|-------------|
| `backend-test-generator.ts` | ~15 locations | 5 | 10 |
| `generate-backend-tests.ts` | ~8 locations | 3 | 5 |

**Total Estimated Time**: 30-45 minutes

---

## Testing Checklist

After fixes:

- [ ] TypeScript compiles without errors (`npm run build`)
- [ ] CLI runs without crashing (`npm run generate-backend-tests -- --help`)
- [ ] Generate test for Story 3.1 (`npm run generate-backend-tests -- --story 3.1 --dry-run`)
- [ ] Verify Java code structure
- [ ] Check generated files are valid Java
- [ ] Validate dataset paths resolve correctly
- [ ] Test with multiple stories
- [ ] Verify Allure annotations

---

## Next Actions

1. **Apply Critical Fixes** (15 min)
   - Fix property access (story.id → story.storyId)
   - Fix selectTemplates method
   - Fix type mismatches

2. **Apply Style Fixes** (15 min)
   - Update imports
   - Remove unused code
   - Refactor nested ternaries
   - Replace forEach with for...of

3. **Test & Validate** (15 min)
   - Build and fix any remaining errors
   - Run CLI with sample story
   - Verify generated Java code
   - Check file structure

4. **Document & Complete** (5 min)
   - Update STORY_20_3_README.md
   - Mark as complete
   - Prepare for Story 20.4
