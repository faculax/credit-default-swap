# Getting Started with Test Evidence Framework

**Quick start guide to get up and running in 15 minutes**

## 🎯 What is This?

The Test Evidence Framework automates test generation from user story markdown files and provides unified test evidence reporting across all CDS Platform services (Backend, Frontend, Gateway, Risk Engine).

**What you can do:**
- Generate test scaffolding from stories (90% faster than manual)
- Validate test coverage automatically
- **Track test results persistently with ReportPortal** (new!)
- Upload results to ReportPortal with story traceability
- Export static HTML dashboards
- Run CI/CD pipelines with selective execution

---

## 🏃 5-Minute Quick Start with ReportPortal

**The fastest way to see the full workflow:**

```powershell
# 1. Start ReportPortal (Docker-based test tracking)
.\scripts\reportportal-start.ps1

# 2. Configure framework
cd test-evidence-framework
cp reportportal.json.example reportportal.json

# 3. Open ReportPortal UI: http://localhost:8080
#    Login: default / 1q2w3e
#    Create project: "cds-platform"
#    Get API token: Profile → API Keys → Copy token
#    Paste token into reportportal.json

# 4. Run unified tests
.\scripts\test-unified-local.ps1

# 5. Upload results
npm run upload-results -- --all

# 6. View in ReportPortal: http://localhost:8080
#    See: pass/fail trends, historical data, ML-powered analysis
```

**💡 Why ReportPortal?**
- **Persistent tracking**: All test runs stored with timestamps (Allure reports are ephemeral)
- **Historical trends**: See pass/fail rates over time
- **ML auto-analysis**: Automatically groups similar failures
- **Team dashboards**: Share results with widgets and filters

**Learn more:** [../docs/REPORTPORTAL_QUICKSTART.md](../docs/REPORTPORTAL_QUICKSTART.md)

---

## ⚡ Full Installation

### Prerequisites

- **Node.js 20+**, **Java 21+**, **Maven 3.9+**, **Docker** (for TestContainers & ReportPortal)
- **GitHub Copilot** (recommended for AI-assisted workflow)

### Installation (5 minutes)

```bash
# 1. Install framework
cd test-evidence-framework
npm install
npm run build

# 2. Verify
npm run generate-tests -- --help
```

### Two Ways to Generate Tests

#### Option 1: GitHub Copilot Prompt (Recommended - Fully Automated)

**The easiest way!** Use the GitHub Copilot prompt for end-to-end automation:

```bash
# In GitHub Copilot Chat
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1
```

**What this does:**
- ✅ Locates story file automatically
- ✅ Parses acceptance criteria
- ✅ Auto-detects services (frontend/backend/gateway/risk-engine)
- ✅ Generates **complete, working tests** (NO TODOs!)
- ✅ Includes Given/When/Then structure
- ✅ Adds Allure annotations for traceability
- ✅ One test per acceptance criterion
- ✅ Realistic assertions based on AC semantics

**Output Example:**
```json
{
  "status": "GENERATION_COMPLETE",
  "storyId": "story_3_1",
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
  ]
}
```

**See detailed examples:** [.github/prompts/EXAMPLES.md](../../.github/prompts/EXAMPLES.md)

#### Option 2: CLI (Manual Control)

If you prefer direct CLI control or don't have GitHub Copilot:

```bash
# Generate backend tests
npm run generate-tests -- \
  --story ../user-stories/epic_03_cds_trade_capture/story_3_1_create_single_name_cds_trade.md \
  --service backend

# Generate frontend tests
npm run generate-tests -- \
  --story ../user-stories/epic_03_cds_trade_capture/story_3_1_create_single_name_cds_trade.md \
  --service frontend

# Run tests
cd ../backend && mvn clean test
cd ../frontend && npm run test:unit
```

---

## 📚 Core Concepts

### 1. Story-Driven Tests

**Input:** Story markdown with acceptance criteria
```markdown
**AC 1.1:** Should create CDS trade with valid data
- Given authenticated user
- When they submit valid trade data
- Then return 201 Created with trade ID
```

**Output:** Generated test code
```java
@Test
@Description("AC 1.1: Should create trade with valid data")
void testCreateTradeWithValidData() {
    // Generated test stub
}
```

### 2. Services

- **backend**: Spring Boot REST API
- **frontend**: React TypeScript UI
- **gateway**: API Gateway
- **risk-engine**: Risk calculations
- **flow**: End-to-end multi-service tests

### 3. Test Data Registry

Centralized test data in `test-data-registry.json`:

```json
{
  "backend": {
    "trade": [{"id": "TRADE_001", "data": {...}}]
  },
  "frontend": {
    "trade": [{"id": "TRADE_001", "data": {...}}]
  }
}
```

---

## 🔧 Essential Commands

### GitHub Copilot Prompt Commands (Recommended)

The **easiest and most powerful way** to use the framework:

#### Basic Test Generation
```bash
# Auto-detect services and generate all tests
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1
```

#### Dry Run (Planning Only)
```bash
# See what will be generated without creating files
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1 DRY_RUN=true
```

#### Force Regeneration
```bash
# Regenerate tests even if they already exist
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1 FORCE=true
```

#### Generate and Execute
```bash
# Generate tests, run them, and create evidence report
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1 EXECUTE=true
```

#### Override Service Detection
```bash
# Generate only backend tests
@workspace /generate-evidence-tests.prompt STORY_ID=story_9_2 SERVICES=backend

# Generate only frontend tests
@workspace /generate-evidence-tests.prompt STORY_ID=story_12_3 SERVICES=frontend
```

**Key Benefits:**
- ✅ **Idempotent** - Safe to run multiple times
- ✅ **Complete Code** - No TODOs or placeholders
- ✅ **AI-Powered** - Semantic analysis for realistic test logic
- ✅ **Multi-Service** - Frontend AND backend in one command

**Full documentation:** [.github/prompts/README.md](../../.github/prompts/README.md)

### CLI Commands (Alternative)

```bash
# Dry run (see what would be generated)
npm run generate-tests -- --story <path> --dry-run

# Generate for specific service
npm run generate-tests -- --story <path> --service backend
npm run generate-tests -- --story <path> --service frontend
npm run generate-tests -- --story <path> --service flow

# Generate all services
npm run generate-tests -- --story <path> --all
```

### Validate Coverage

```bash
# Check if all acceptance criteria have tests
npm run validate-code -- --story <path>

# Output:
# ✅ AC 1.1: Covered by testCreateTradeWithValidData
# ❌ AC 2.1: NOT COVERED (missing test)
```

### Run Tests

```bash
# Backend (with TestContainers)
cd backend && mvn clean test

# Frontend
cd frontend && npm run test:unit

# Unified (all services)
./scripts/test-unified-local.ps1  # Windows
./scripts/test-unified-local.sh   # Linux/Mac
```

### Upload to ReportPortal (Optional)

```bash
# Set credentials
export REPORTPORTAL_ENDPOINT=https://your-rp.example.com
export REPORTPORTAL_TOKEN=your-token
export REPORTPORTAL_PROJECT=cds-platform

# Upload results
npm run upload-results -- --service backend --allure-results ../backend/target/allure-results
npm run upload-results -- --all
```

---

## 📖 File Structure

```
test-evidence-framework/
├── src/
│   ├── parser/              # Parse story markdown
│   ├── planner/             # Generate test plans
│   ├── generators/          # Generate test code
│   │   ├── backend/
│   │   ├── frontend/
│   │   └── flow/
│   ├── validation/          # Validate coverage
│   ├── registry/            # Test data management
│   ├── reportportal/        # ReportPortal integration
│   ├── evidence/            # Evidence export
│   └── cli/                 # Command-line tools
├── docs/                    # Documentation (you are here)
├── package.json
└── tsconfig.json
```

---

## 🎓 Learning Path

### Day 1: Basic Usage (GitHub Copilot - Fastest!)
1. ✅ Verify GitHub Copilot is enabled in VS Code
2. ✅ Generate tests using prompt: `@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1`
3. ✅ Review generated test files (frontend + backend)
4. ✅ Run tests: `cd backend && mvn test` / `cd frontend && npm test`
5. ✅ Marvel at complete, working tests with no manual work! 🎉

**Time**: 5-10 minutes

### Day 1: Basic Usage (CLI Alternative)
1. ✅ Complete Quick Start (above)
2. ✅ Generate tests for 1-2 stories using CLI
3. ✅ Run tests locally
4. ✅ Review generated code

**Time**: 15-20 minutes

### Day 2: Advanced Features
1. ✅ Add test data to registry
2. ✅ Validate coverage
3. ✅ Crystallize tests
4. ✅ Upload to ReportPortal (if available)

### Day 3: CI/CD Integration
1. ✅ Review GitHub Actions workflows
2. ✅ Test PR workflow
3. ✅ View evidence dashboard

**Next Steps:**
- Read [USER_GUIDE.md](USER_GUIDE.md) for comprehensive usage
- See [INTEGRATION.md](INTEGRATION.md) for CI/CD and ReportPortal
- Check [REFERENCE.md](REFERENCE.md) for troubleshooting

---

## 🚦 CI/CD Pipeline (Preview)

When you push code, GitHub Actions automatically:
1. ✅ Detects changed services
2. ✅ Runs tests for changed services only (4-7 min)
3. ✅ Uploads results to ReportPortal
4. ✅ Posts PR comment with test summary
5. ✅ Deploys evidence dashboard (main branch)

**PR Comment Example:**
```markdown
## Test Results

| Service | Tests | Passed | Failed | Status |
|---|---:|---:|---:|:---:|
| Backend | 45 | 43 | 2 | ❌ |
| Frontend | 32 | 32 | 0 | ✅ |

[View in ReportPortal](https://rp.example.com/launch/123)
```

---

## 🚀 GitHub Copilot Prompt Quick Reference

### Basic Commands

```bash
# Generate tests (auto-detect services)
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1

# See plan without generating
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1 DRY_RUN=true

# Regenerate existing tests
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1 FORCE=true

# Generate and run tests
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1 EXECUTE=true

# Backend only
@workspace /generate-evidence-tests.prompt STORY_ID=story_9_2 SERVICES=backend
```

### What Gets Generated

#### Frontend Tests
**Location:** `frontend/src/__tests__/components/{Component}.Story{X}_{Y}.test.tsx`

**Features:**
- ✅ React Testing Library + userEvent
- ✅ Allure annotations (epic, feature, story)
- ✅ Given/When/Then structure
- ✅ One test per acceptance criterion
- ✅ Complete assertions (no TODOs!)

**Example:**
```typescript
it('AC1: should display all required fields', () => {
  // GIVEN: Form is rendered
  render(<CDSTradeForm />);
  
  // WHEN: User views form
  const input = screen.getByLabelText(/notional/i);
  
  // THEN: Field is visible
  expect(input).toBeInTheDocument();
});
```

#### Backend Tests
**Location:** `backend/src/test/java/com/cds/platform/{Feature}Story{X}_{Y}IntegrationTest.java`

**Features:**
- ✅ Spring Boot + TestRestTemplate
- ✅ Allure annotations (@Epic, @Story, @Severity)
- ✅ Given/When/Then structure
- ✅ One test per acceptance criterion
- ✅ Complete request/response handling

**Example:**
```java
@Test
@Story("Story 3.1")
void testValidTradeSubmission() {
    // GIVEN: Valid trade data
    String request = """{"notional": 10000000}""";
    
    // WHEN: Trade submitted
    ResponseEntity<String> response = restTemplate.postForEntity(...);
    
    // THEN: Trade created
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
}
```

### Why Use Copilot Prompts?

| Feature | Copilot Prompt | Manual CLI |
|---------|---------------|------------|
| **Time to Complete** | 5-10 min | 30-60 min |
| **Code Quality** | Complete, working | Scaffolds with TODOs |
| **Multi-Service** | ✅ All in one command | ❌ One at a time |
| **AI Analysis** | ✅ Semantic understanding | ❌ Template-based |
| **Idempotent** | ✅ Safe to re-run | ⚠️ Manual tracking |

### Full Documentation

- **Prompt File:** `.github/prompts/generate-evidence-tests.prompt.md`
- **Examples:** `.github/prompts/EXAMPLES.md`
- **Complete Guide:** `.github/prompts/README.md`

---

## 📚 Practical Examples

### Example 1: Simple Backend Story (Story 4.2 - Credit Event Processor)

**Command:**
```bash
@workspace /generate-evidence-tests.prompt STORY_ID=story_4_2
```

**Output:**
- Generated: `backend/src/test/java/com/cds/platform/CreditEventStory4_2IntegrationTest.java`
- Test cases: 4 (matching 4 acceptance criteria)
- Coverage: 100%

**Generated Test Excerpt:**
```java
@Test
@Story("Story 4.2 - Credit Event Processor")
@Severity(SeverityLevel.CRITICAL)
@Description("AC1: Service processes credit event messages from queue")
void testAC1_ProcessCreditEventMessage() {
    // GIVEN: Credit event message
    String eventPayload = """
        {
          "eventType": "DEFAULT",
          "referenceEntity": "TICKER-123",
          "eventDate": "2025-01-15"
        }
        """;
    
    // WHEN: Event is posted to processor
    ResponseEntity<String> response = restTemplate.postForEntity(
        "/api/credit-events/process",
        new HttpEntity<>(eventPayload, headers),
        String.class
    );
    
    // THEN: Event is processed successfully
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
}
```

---

### Example 2: Full-Stack Form Story (Story 3.1 - CDS Trade Capture)

**Command:**
```bash
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1
```

**Output:**
- **Frontend:** 10 test cases (467 lines)
- **Backend:** 6 test cases (312 lines)
- Total coverage: All 6 acceptance criteria

**Frontend Test Excerpt:**
```typescript
it('AC4: should validate notional amount is greater than zero', async () => {
  // GIVEN: Trade form with notional input
  const user = userEvent.setup();
  render(<CDSTradeForm />);
  
  // WHEN: User enters zero notional
  const notionalInput = screen.getByLabelText(/notional amount/i);
  await user.clear(notionalInput);
  await user.type(notionalInput, '0');
  await user.tab();
  
  // THEN: Validation error is displayed
  expect(screen.getByText(/notional must be greater than zero/i)).toBeInTheDocument();
});
```

---

### Example 3: Dry Run (Planning Only)

**Command:**
```bash
@workspace /generate-evidence-tests.prompt STORY_ID=story_5_3 DRY_RUN=true
```

**Output:**
```json
{
  "status": "PLAN_READY",
  "storyId": "story_5_3",
  "title": "Payment Settlement Engine",
  "detectedServices": ["backend"],
  "plannedTests": {
    "backend": {
      "testCases": 4,
      "estimatedLines": 280
    }
  }
}
```

---

### Example 4: Generate and Execute

**Command:**
```bash
@workspace /generate-evidence-tests.prompt STORY_ID=story_6_1 EXECUTE=true
```

**Output:**
```json
{
  "status": "GENERATION_COMPLETE",
  "generatedFiles": [...],
  "testExecution": {
    "frontend": { "status": "passed", "tests": 7 },
    "backend": { "status": "passed", "tests": 5 }
  },
  "evidenceReport": {
    "reportGenerated": true,
    "summary": { "total": 12, "passed": 12 }
  }
}
```

---

### Common Workflow Patterns

#### Pattern 1: TDD (Tests First)
```bash
# Step 1: Generate tests
@workspace /generate-evidence-tests.prompt STORY_ID=story_8_3

# Step 2: Run tests (should fail - no implementation yet)
cd backend && mvn test -Dtest="*Story8_3*"

# Step 3: Implement story logic to make tests pass

# Step 4: Run tests again (should pass)
cd backend && mvn test -Dtest="*Story8_3*"
```

#### Pattern 2: Evidence-Driven Epic Development
```bash
# Generate tests for all stories in epic
@workspace /generate-evidence-tests.prompt STORY_ID=story_5_1
@workspace /generate-evidence-tests.prompt STORY_ID=story_5_2
@workspace /generate-evidence-tests.prompt STORY_ID=story_5_3

# Implement stories, then generate unified evidence report
.\scripts\test-unified-local.ps1
allure generate allure-results-unified -o allure-report
allure open allure-report
```

#### Pattern 3: Story Refinement
```bash
# Initial generation
@workspace /generate-evidence-tests.prompt STORY_ID=story_10_1

# [Product owner updates acceptance criteria]

# Regenerate with latest criteria
@workspace /generate-evidence-tests.prompt STORY_ID=story_10_1 FORCE=true
```

---

### Troubleshooting

#### Issue: "TESTS_ALREADY_EXIST"
**Solution:**
```bash
# Use FORCE to regenerate
@workspace /generate-evidence-tests.prompt STORY_ID=story_7_4 FORCE=true
```

#### Issue: "COMPILATION_ERROR: backend"
**Cause:** Generated test references classes that don't exist yet.  
**Solution:** This is expected! Tests are generated before implementation (TDD). Implement the story logic to resolve.

---

## ❓ Common Questions

**Q: Do I need ReportPortal to use the framework?**  
A: No. ReportPortal is optional. You can use the framework locally without it.

**Q: Can I use this for existing tests?**  
A: Yes. The framework can generate new tests or augment existing ones.

**Q: What if my story doesn't follow the template?**  
A: The parser is flexible but works best with Given/When/Then format. See [Story Template](../user-stories/STORY_TEMPLATE.md).

**Q: How do I add custom test data?**  
A: Edit `test-data-registry.json` or use CLI: `npm run registry -- add --type backend --category trade --data '{...}'`

**Q: Can I customize the generated tests?**  
A: Yes. Edit templates in `src/generators/*/templates/` and regenerate.

---

## 🆘 Getting Help

- **Issues?** Check [REFERENCE.md - Troubleshooting](REFERENCE.md#troubleshooting)
- **Questions?** Slack: `#cds-testing`
- **Bugs?** GitHub Issues: [github.com/your-org/credit-default-swap/issues](https://github.com/your-org/credit-default-swap/issues)

---

**Ready to dive deeper?** Continue to [USER_GUIDE.md](USER_GUIDE.md) →
