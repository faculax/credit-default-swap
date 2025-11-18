# GitHub Copilot Prompts

This directory contains reusable prompt files for automating common workflows in the CDS Platform project.

## Available Prompts

### 📝 Epic Planning
**File**: `plan-epic.prompt.md`

**Purpose**: Plan an epic, create story markdown files, and GitHub issues

**Usage**:
```
@workspace /plan-epic.prompt EPIC_NUMBER=5
```

**Flags**:
- `FORCE=true` - Create stories even if some already exist
- `NO_ISSUES=true` - Skip GitHub issue creation

**Output**: JSON summary of created stories and issues

---

### 🚀 Epic Implementation
**File**: `implement-epic.prompt.md`

**Purpose**: Implement all stories in an epic (database, backend, frontend, tests)

**Usage**:
```
@workspace /implement-epic.prompt EPIC_NUMBER=5
```

**Flags**:
- `DRY_RUN=true` - Generate plan without implementing
- `SKIP_ISSUES=true` - Skip GitHub issue management

**Output**: Complete implementation with test coverage and GitHub updates

---

### 🧪 Evidence-Based Test Generation
**File**: `generate-evidence-tests.prompt.md`

**Purpose**: Generate complete, production-ready tests from user story acceptance criteria using the Test Evidence Framework

**Usage**:
```
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1
```

**Flags**:
- `DRY_RUN=true` - Show generation plan without creating files
- `FORCE=true` - Regenerate tests even if they already exist
- `EXECUTE=true` - Run tests after generation
- `SERVICES=frontend,backend` - Override auto-detected services

**What It Does**:
1. ✅ Locates story file (`user-stories/epic_0X_*/story_X_Y_*.md`)
2. ✅ Parses acceptance criteria and detects involved services
3. ✅ Generates **complete, working tests** (NO TODOs or placeholders)
4. ✅ Frontend: React Testing Library + Jest + Allure annotations
5. ✅ Backend: Spring Boot + JUnit 5 + Allure annotations
6. ✅ One test per acceptance criterion (1:1 traceability)
7. ✅ Given/When/Then structure with realistic assertions
8. ✅ Optional: Runs tests and generates unified Allure report

**Example**:
```bash
# Generate tests for Story 3.1 (auto-detect services)
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1

# Dry run to see plan first
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1 DRY_RUN=true

# Force regeneration of existing tests
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1 FORCE=true

# Generate and immediately execute tests
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1 EXECUTE=true

# Generate only backend tests
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1 SERVICES=backend
```

**Output**:
```json
{
  "status": "GENERATION_COMPLETE",
  "storyId": "story_3_1",
  "title": "CDS Trade Capture UI & Reference Data",
  "services": ["frontend", "backend"],
  "generatedFiles": [
    {
      "path": "frontend/src/__tests__/components/CDSTradeForm.Story3_1.test.tsx",
      "testCases": 10,
      "linesOfCode": 467
    },
    {
      "path": "backend/src/test/java/com/cds/platform/TradeStory3_1IntegrationTest.java",
      "testCases": 6,
      "linesOfCode": 312
    }
  ],
  "traceability": {
    "acceptanceCriteria": 6,
    "totalTestCases": 16,
    "coverageComplete": true
  },
  "nextSteps": [
    "Run tests: npm run test (frontend) | mvn test (backend)",
    "Generate evidence: allure generate allure-results-unified -o allure-report"
  ]
}
```

**Key Features**:
- ✅ **Idempotent**: Safe to run multiple times (files overwritten completely)
- ✅ **Complete Code**: No TODOs, placeholders, or manual work needed
- ✅ **AI-Powered**: Semantic analysis of acceptance criteria for realistic test logic
- ✅ **Allure Integration**: Full traceability (Epic → Feature → Story → AC → Test)
- ✅ **Multi-Service**: Generates frontend AND backend tests in one command

---

### 🔧 Hotfix Implementation
**File**: `hotfix.prompt.md`

**Purpose**: Implement urgent fixes with minimal process overhead

**Usage**:
```
@workspace /hotfix.prompt ISSUE_NUMBER=42
```

**Output**: Quick fix implementation with tests

---

## Workflow Examples

### 1. Complete Epic Workflow
```bash
# Step 1: Plan the epic
@workspace /plan-epic.prompt EPIC_NUMBER=5

# Step 2: Review generated story files in user-stories/epic_05_*/

# Step 3: Implement the epic
@workspace /implement-epic.prompt EPIC_NUMBER=5

# Step 4: Generate evidence-based tests for each story
@workspace /generate-evidence-tests.prompt STORY_ID=story_5_1
@workspace /generate-evidence-tests.prompt STORY_ID=story_5_2
# ... etc
```

### 2. Test-Driven Story Implementation
```bash
# Step 1: Generate tests first (TDD approach)
@workspace /generate-evidence-tests.prompt STORY_ID=story_6_2 DRY_RUN=true

# Step 2: Review test plan

# Step 3: Generate actual tests
@workspace /generate-evidence-tests.prompt STORY_ID=story_6_2

# Step 4: Implement the story logic to make tests pass
@workspace /implement-epic.prompt EPIC_NUMBER=6

# Step 5: Run tests and generate evidence report
@workspace /generate-evidence-tests.prompt STORY_ID=story_6_2 EXECUTE=true
```

### 3. Regenerate Tests After Story Changes
```bash
# Story acceptance criteria were updated
@workspace /generate-evidence-tests.prompt STORY_ID=story_4_3 FORCE=true
```

---

## Prompt File Format

All prompts follow this structure:

```markdown
````prompt
---
model: Claude-4-Sonnet-202501
description: Brief description of prompt purpose
mode: agent
---

# Prompt Title

[Prompt content with input contract, behavior, output format]
````
```

## Best Practices

1. **Always run DRY_RUN first** for complex operations
2. **Review generated plans** before executing
3. **Use FORCE flag carefully** - it overwrites existing code
4. **Check GitHub issues** before and after automation
5. **Run tests after generation** to verify correctness
6. **Generate evidence reports** for traceability

## Test Evidence Framework

The `generate-evidence-tests.prompt.md` uses the **Test Evidence Framework** built in Epic 20:

### Framework Components
- **Story Parser** (20.1): Markdown parsing
- **Service Inference** (20.1+): Auto-detect involved services
- **Test Planner** (20.2): Plan test cases from ACs
- **Intelligent Test Implementor** (20.3 & 20.4): AI code generation
- **Test Data Registry** (20.7): Centralized test data

### Documentation
- Architecture: `docs/test-evidence-framework-architecture.md`
- Getting Started: `test-evidence-framework/docs/GETTING_STARTED.md`
- User Guide: `test-evidence-framework/docs/USER_GUIDE.md`
- Integration: `test-evidence-framework/docs/INTEGRATION.md`
- Reference: `test-evidence-framework/docs/REFERENCE.md`

### Key Innovation
Unlike template-based scaffolding, the framework uses **semantic analysis** to generate **complete, working tests**:

❌ **OLD (Template Scaffolding)**:
```java
@Test
public void testTradeSubmission() {
    // TODO: Add test implementation
}
```

✅ **NEW (Intelligent Generation)**:
```java
@Test
@DisplayName("AC1: Valid trade submission creates trade record")
void testValidTradeSubmission() {
    // GIVEN: Valid CDS trade data
    String requestBody = """
        {"tradeDate": "2025-01-15", "notional": 10000000}
        """;
    
    // WHEN: Trade is submitted
    ResponseEntity<String> response = restTemplate.postForEntity(...);
    
    // THEN: Trade is created successfully
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody().contains("\"tradeId\""));
}
```

---

## Support

For issues or questions:
1. Check `AGENTS.md` for project conventions
2. Review framework docs in `test-evidence-framework/docs/`
3. Examine existing prompt outputs in git history
4. Consult architecture documentation in `docs/`

---

## Version History

- **v1.0** (Nov 18, 2025): Initial prompt collection
  - `plan-epic.prompt.md`
  - `implement-epic.prompt.md`
  - `generate-evidence-tests.prompt.md` ⭐ **NEW**
  - `hotfix.prompt.md`
