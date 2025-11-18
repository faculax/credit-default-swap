# User Guide

**Comprehensive guide for developers and QA engineers**

---

## 📋 Table of Contents

1. [Development Workflow](#development-workflow)
2. [Test Generation](#test-generation)
3. [Test Data Management](#test-data-management)
4. [Coverage Validation](#coverage-validation)
5. [Running Tests](#running-tests)
6. [QA & Evidence](#qa--evidence)
7. [Best Practices](#best-practices)

---

## 🔄 Development Workflow

### GitHub Copilot Workflow (Recommended - Fully Automated)

**The modern, AI-assisted approach using GitHub Copilot prompts:**

```
1. Write/Update Story
   ↓
2. Run Copilot Prompt (generates complete tests automatically)
   @workspace /generate-evidence-tests.prompt STORY_ID=story_3_1
   ↓
3. Review Generated Tests (complete, working code)
   ↓
4. Run Tests & Generate Evidence
   ↓
5. Push to GitHub (CI runs automatically)
```

**Key Advantages:**
- ✅ **5-10 minutes** instead of hours per story
- ✅ **Complete code** (no TODOs or placeholders)
- ✅ **AI-powered** semantic analysis for realistic logic
- ✅ **Idempotent** (safe to re-run after story changes)
- ✅ **Multi-service** (frontend + backend in one command)

**Prompt Location:** `.github/prompts/generate-evidence-tests.prompt.md`  
**Full Documentation:** `.github/prompts/README.md`  
**Examples:** `.github/prompts/EXAMPLES.md`

---

### Standard Workflow (Manual CLI)

```
1. Write/Update Story
   ↓
2. Generate Test Plan (dry-run)
   ↓
3. Generate Test Code
   ↓
4. Implement Test Logic
   ↓
5. Run Tests Locally
   ↓
6. Validate Coverage
   ↓
7. Crystallize (lock tests)
   ↓
8. Push to GitHub (CI runs automatically)
```

### Detailed Steps

**Step 1: Write Story**

Use the story template at `user-stories/STORY_TEMPLATE.md`:

```markdown
**As a** [role]  
**I want** [capability]  
**So that** [benefit]

## ✅ Acceptance Criteria

**AC 1.1:** Should create trade with valid data
- Given authenticated user
- When they submit valid trade
- Then return 201 Created

## 🧱 Services Involved
- [x] backend
- [x] frontend
```

**Step 2: Generate Test Plan**

```bash
npm run generate-tests -- --story ../user-stories/epic_03/story_3_1.md --dry-run

# Review output:
# - Extracted acceptance criteria
# - Identified services
# - Planned test cases
```

**Step 3: Generate Tests**

```bash
# Backend tests
npm run generate-tests -- --story ../user-stories/epic_03/story_3_1.md --service backend --output ../backend/src/test/java

# Frontend tests
npm run generate-tests -- --story ../user-stories/epic_03/story_3_1.md --service frontend --output ../frontend/src/__tests__
```

**Step 4: Implement Tests**

Edit generated files and add test logic:

```java
@Test
@Description("AC 1.1: Should create trade with valid data")
void testCreateTradeWithValidData() {
    // 1. Arrange: Set up test data
    CDSTradeRequest request = CDSTradeRequest.builder()
        .referenceEntity("TESLA INC")
        .notional(new BigDecimal("10000000"))
        .build();
    
    // 2. Act: Call API
    ResponseEntity<CDSTradeResponse> response = restTemplate.postForEntity(
        "/api/trades", request, CDSTradeResponse.class);
    
    // 3. Assert: Verify results
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getTradeId()).isNotNull();
}
```

**Step 5: Run Tests**

```bash
cd backend
mvn clean test

# Or specific test
mvn test -Dtest=CDSTradeControllerIntegrationTest
```

**Step 6: Validate Coverage**

```bash
cd test-evidence-framework
npm run validate-code -- --story ../user-stories/epic_03/story_3_1.md

# Fix missing coverage if needed
```

**Step 7: Crystallize**

```bash
npm run crystallize -- --story story_3_1

# Locks tests to prevent unintended changes
```

---

## 🏭 Test Generation

### Using GitHub Copilot Prompt (Recommended)

**The fastest and most powerful method!**

#### Basic Usage

```bash
# Auto-detect services and generate complete tests
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1
```

**What You Get:**
- ✅ Frontend tests with React Testing Library + Jest + Allure
- ✅ Backend tests with Spring Boot + JUnit 5 + Allure  
- ✅ Complete Given/When/Then structure
- ✅ One test per acceptance criterion (1:1 mapping)
- ✅ Realistic assertions based on AC semantics
- ✅ No TODOs or placeholders!

**Example Output:**

**Frontend** (`frontend/src/__tests__/components/CDSTradeForm.Story3_1.test.tsx`):
```typescript
describe('Story 3.1 - CDS Trade Capture UI', () => {
  beforeEach(() => {
    allure.epic('Epic 03 - CDS Trade Capture');
    allure.feature('Trade Entry Form');
    allure.story('Story 3.1 - CDS Trade Capture UI');
    allure.severity('critical');
  });

  it('AC1: should display all required CDS trade fields', () => {
    // GIVEN: Trade form is rendered
    render(<CDSTradeForm />);
    
    // WHEN: User views the form
    const notionalInput = screen.getByLabelText(/notional amount/i);
    const spreadInput = screen.getByLabelText(/spread/i);
    
    // THEN: All required fields are visible
    expect(notionalInput).toBeInTheDocument();
    expect(spreadInput).toBeInTheDocument();
  });

  it('AC4: should validate notional amount is greater than zero', async () => {
    // GIVEN: Trade form with notional input
    const user = userEvent.setup();
    render(<CDSTradeForm />);
    
    // WHEN: User enters zero notional
    await user.type(screen.getByLabelText(/notional/i), '0');
    await user.tab();
    
    // THEN: Validation error is displayed
    expect(screen.getByText(/must be greater than zero/i)).toBeInTheDocument();
  });
});
```

**Backend** (`backend/src/test/java/com/cds/platform/TradeStory3_1IntegrationTest.java`):
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Epic("Epic 03 - CDS Trade Capture")
@Feature("Trade Entry")
@DisplayName("Story 3.1 - CDS Trade Capture UI")
public class TradeStory3_1IntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    private HttpHeaders headers;
    
    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    @Story("Story 3.1 - CDS Trade Capture UI")
    @Severity(SeverityLevel.CRITICAL)
    @Description("AC1: Form displays all required CDS trade fields")
    void testAC1_ValidTradeSubmission() {
        // GIVEN: Valid CDS trade data
        String requestBody = """
            {
              "tradeDate": "2025-01-15",
              "notionalAmount": 10000000.00,
              "spread": 250,
              "tenor": "5Y"
            }
            """;
        
        // WHEN: Trade is submitted to API
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/trades",
            new HttpEntity<>(requestBody, headers),
            String.class
        );
        
        // THEN: Trade is created successfully
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().contains("\"tradeId\""));
        assertTrue(response.getBody().contains("\"status\":\"PENDING\""));
    }
}
```

#### Advanced Usage Flags

**Dry Run (Planning Only)**:
```bash
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1 DRY_RUN=true
```
Shows generation plan without creating files.

**Force Regeneration**:
```bash
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1 FORCE=true
```
Regenerates tests even if they already exist (useful after story AC updates).

**Generate and Execute**:
```bash
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1 EXECUTE=true
```
Generates tests, runs them, and creates unified Allure evidence report.

**Override Service Detection**:
```bash
# Backend only
@workspace /generate-evidence-tests.prompt STORY_ID=story_9_2 SERVICES=backend

# Frontend only  
@workspace /generate-evidence-tests.prompt STORY_ID=story_12_3 SERVICES=frontend

# Multiple services
@workspace /generate-evidence-tests.prompt STORY_ID=story_6_1 SERVICES=frontend,backend
```

#### Common Workflows

**Workflow 1: TDD (Test-Driven Development)**
```bash
# Step 1: Generate tests first
@workspace /generate-evidence-tests.prompt STORY_ID=story_5_2

# Step 2: Run tests (they will fail - no implementation yet)
cd backend && mvn test -Dtest="*Story5_2*"

# Step 3: Implement story logic to make tests pass

# Step 4: Run tests again (should pass now)
cd backend && mvn test -Dtest="*Story5_2*"
```

**Workflow 2: Evidence-Driven Epic**
```bash
# Generate tests for all stories in epic
@workspace /generate-evidence-tests.prompt STORY_ID=story_6_1
@workspace /generate-evidence-tests.prompt STORY_ID=story_6_2  
@workspace /generate-evidence-tests.prompt STORY_ID=story_6_3

# Implement stories

# Generate unified evidence report
.\scripts\test-unified-local.ps1
allure open allure-report
```

**Workflow 3: Story Refinement Loop**
```bash
# Initial generation
@workspace /generate-evidence-tests.prompt STORY_ID=story_8_4

# Product owner updates acceptance criteria in story file

# Regenerate tests with new criteria
@workspace /generate-evidence-tests.prompt STORY_ID=story_8_4 FORCE=true
```

**Full Documentation:**
- **Prompt File:** `.github/prompts/generate-evidence-tests.prompt.md`
- **Usage Guide:** `.github/prompts/README.md`
- **Practical Examples:** `.github/prompts/EXAMPLES.md`

---

### Using CLI (Alternative Method)

### Backend Tests (Java/Spring Boot)

**Generated test types:**

1. **Integration Tests** (`@SpringBootTest`)
   ```java
   @SpringBootTest
   @AutoConfigureMockMvc
   @Testcontainers
   class CDSTradeControllerIntegrationTest {
       @Autowired private MockMvc mockMvc;
       // Tests HTTP endpoints with real database
   }
   ```

2. **Repository Tests** (`@DataJpaTest`)
   ```java
   @DataJpaTest
   @Testcontainers
   class CDSTradeRepositoryTest {
       @Autowired private CDSTradeRepository repository;
       // Tests database operations
   }
   ```

3. **Service Tests** (Mockito)
   ```java
   @ExtendWith(MockitoExtension.class)
   class CDSTradeServiceTest {
       @Mock private CDSTradeRepository repository;
       @InjectMocks private CDSTradeService service;
       // Tests business logic with mocks
   }
   ```

**Customization:**

Edit templates in `src/generators/backend/templates/`:
- `integration-test.template.java`
- `repository-test.template.java`
- `service-test.template.java`

### Frontend Tests (React/TypeScript)

**Generated test types:**

1. **Component Tests**
   ```typescript
   import { render, screen, fireEvent } from '@testing-library/react';
   
   describe('CDSTradeForm', () => {
     it('should create trade when form submitted', async () => {
       render(<CDSTradeForm />);
       // Test component behavior
     });
   });
   ```

2. **Hook Tests**
   ```typescript
   import { renderHook, act } from '@testing-library/react-hooks';
   
   describe('useCDSTrade', () => {
     it('should create trade', async () => {
       const { result } = renderHook(() => useCDSTrade());
       // Test custom hook
     });
   });
   ```

3. **Integration Tests** (Mock Service Worker)
   ```typescript
   import { rest } from 'msw';
   import { setupServer } from 'msw/node';
   
   const server = setupServer(
     rest.post('/api/trades', (req, res, ctx) => {
       return res(ctx.json({ tradeId: 'T001' }));
     })
   );
   // Test with mocked API
   ```

**Customization:**

Edit templates in `src/generators/frontend/templates/`

### Flow Tests (End-to-End)

**Generated test types:**

```typescript
describe('CDS Trade Lifecycle', () => {
  it('should create via frontend and persist in backend', async () => {
    // Step 1: Create via frontend API
    const createResponse = await axios.post('http://localhost:3000/api/trades', {...});
    
    // Step 2: Verify in backend API
    const getResponse = await axios.get(`http://localhost:8080/api/trades/${createResponse.data.tradeId}`);
    
    // Step 3: Verify database state
    // Step 4: Verify in frontend UI
  });
});
```

---

## 📊 Test Data Management

### Test Data Registry

**Location:** `test-evidence-framework/test-data-registry.json`

**Structure:**
```json
{
  "backend": {
    "trade": [
      {
        "id": "TRADE_VALID_001",
        "type": "single-name-cds",
        "description": "Valid trade for AC 1.1",
        "data": {
          "tradeId": "T001",
          "referenceEntity": "TESLA INC",
          "notional": 10000000,
          "spread": 150,
          "maturity": "2028-12-20"
        }
      }
    ]
  },
  "frontend": {
    "trade": [
      {
        "id": "TRADE_VALID_001",
        "description": "Frontend mock for valid trade",
        "data": {
          "tradeId": "T001",
          "referenceEntity": "Tesla Inc",
          "notional": "10,000,000",
          "spread": "1.50%"
        }
      }
    ]
  }
}
```

### Adding Test Data

**Via CLI:**
```bash
npm run registry -- add \
  --type backend \
  --category trade \
  --id TRADE_VALID_002 \
  --data '{"tradeId":"T002","referenceEntity":"FORD","notional":5000000}'
```

**Manually:**
Edit `test-data-registry.json` directly

### Using Test Data

**Backend:**
```java
@Test
void testCreateTrade() {
    // Data from registry: TRADE_VALID_001
    CDSTradeRequest request = CDSTradeRequest.builder()
        .tradeId("T001")
        .referenceEntity("TESLA INC")
        .notional(new BigDecimal("10000000"))
        .build();
}
```

**Frontend:**
```typescript
import { MOCK_TRADES } from '@/test-data/registry';

const mockTrade = MOCK_TRADES.find(t => t.id === 'TRADE_VALID_001');
render(<CDSTradeCard trade={mockTrade} />);
```

### Best Practices

1. **Naming:** Use `<ENTITY>_<SCENARIO>_<NUMBER>` (e.g., `TRADE_VALID_001`, `TRADE_INVALID_MISSING_ENTITY`)
2. **Consistency:** Keep backend and frontend data synchronized
3. **Documentation:** Add `description` field explaining purpose
4. **Reusability:** Create generic data for common scenarios
5. **Versioning:** Include `version` field if format changes

---

## ✅ Coverage Validation

### Validate Coverage

```bash
npm run validate-code -- --story ../user-stories/epic_03/story_3_1.md
```

**Output:**
```
Validating story: story_3_1_create_single_name_cds_trade

Acceptance Criteria Coverage:
✅ AC 1.1: Should create trade with valid data
   - Covered by: testCreateTradeWithValidData (backend/CDSTradeControllerIntegrationTest.java)

✅ AC 1.2: Should reject trade with invalid data
   - Covered by: testCreateTradeWithInvalidData (backend/CDSTradeControllerIntegrationTest.java)

❌ AC 2.1: Should display trade in portfolio
   - NOT COVERED: Missing test in frontend

Services Tested:
✅ backend - 2/2 criteria (100%)
❌ frontend - 0/1 criteria (0%)

Overall Coverage: 66.67% (2/3 criteria)
```

### Fix Missing Coverage

```bash
# Generate missing tests
npm run generate-tests -- \
  --story ../user-stories/epic_03/story_3_1.md \
  --service frontend \
  --criteria "AC 2.1"
```

### Crystallization

**Lock validated tests:**
```bash
npm run crystallize -- --story story_3_1

# Output:
# ✅ Crystallized 5 tests for story_3_1
```

**Behavior:**
- Tests marked with `@Crystallized` annotation
- If modified without story update, validation fails
- Prevents coverage regression

**Unlock tests:**
```bash
npm run decrystallize -- --story story_3_1
```

---

## 🧪 Running Tests

### Daily Workflow with ReportPortal

**Recommended workflow for persistent test tracking:**

```powershell
# 1. Ensure ReportPortal is running
.\scripts\reportportal-start.ps1 -Status

# 2. Run unified tests
cd test-evidence-framework
.\scripts\test-unified-local.ps1

# 3. Upload results to ReportPortal
npm run upload-results -- --all

# 4. View results
# Open: http://localhost:8080
# Navigate: Launches → Latest launch → View details
```

**💡 Why this workflow?**
- **Historical tracking**: All runs saved with timestamps
- **Trend analysis**: See pass/fail rates over time
- **Team visibility**: Share dashboards with stakeholders
- **ML insights**: Auto-detection of failure patterns

**Quick commands:**
```powershell
# Start ReportPortal
.\scripts\reportportal-start.ps1

# Check status
.\scripts\reportportal-start.ps1 -Status

# View logs
.\scripts\reportportal-start.ps1 -Logs

# Stop ReportPortal
.\scripts\reportportal-start.ps1 -Stop
```

**Learn more:** [INTEGRATION.md](INTEGRATION.md#reportportal-integration)

---

### Backend Tests

**All tests:**
```bash
cd backend
mvn clean test
```

**Specific test class:**
```bash
mvn test -Dtest=CDSTradeControllerIntegrationTest
```

**Specific test method:**
```bash
mvn test -Dtest=CDSTradeControllerIntegrationTest#testCreateTradeWithValidData
```

**With coverage:**
```bash
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html
```

**Generate Allure report:**
```bash
mvn test
mvn allure:report
mvn allure:serve  # Opens in browser
```

### Frontend Tests

**All tests:**
```bash
cd frontend
npm run test:unit
```

**Watch mode:**
```bash
npm test
```

**Specific file:**
```bash
npm test -- CDSTradeForm.test.tsx
```

**With coverage:**
```bash
npm run test:coverage
```

**Generate Allure report:**
```bash
npm run test:unit
npx allure generate allure-results -o allure-report
npx allure open allure-report
```

### Unified Testing (All Services)

**Windows:**
```powershell
cd test-evidence-framework
.\scripts\test-unified-local.ps1
```

**Linux/Mac:**
```bash
cd test-evidence-framework
./scripts/test-unified-local.sh
```

**What it does:**
1. Runs backend tests → `backend/target/allure-results/`
2. Runs frontend tests → `frontend/allure-results/`
3. Runs gateway tests → `gateway/target/allure-results/`
4. Runs risk-engine tests → `risk-engine/target/allure-results/`
5. Merges all results → `allure-results-unified/`
6. Generates report → `allure-report/`
7. Opens in browser

**Upload to ReportPortal:**
```powershell
# After running tests
npm run upload-results -- --all
```

---

## 📈 QA & Evidence

### Test Evidence Sources

1. **ReportPortal** (Docker) - **Primary**: Persistent test tracking, historical trends, ML auto-analysis
2. **Static Dashboard** (GitHub Pages) - Historical coverage and trends  
3. **Allure Reports** (Local) - Immediate feedback during development

### ReportPortal Dashboard (Recommended)

**Access:** http://localhost:8080 (after running `.\scripts\reportportal-start.ps1`)

**Launch Overview:**
- **Status:** ✅ Passed, ❌ Failed, ⚠️ Skipped
- **Statistics:** Total, passed, failed, skipped
- **Attributes:** Filter by `story:story_3_1`, `service:backend`, `environment:local`

**Test Details:**
- Description (linked to acceptance criteria)
- Logs and stack traces
- Attachments (screenshots, API responses)
- History (previous executions with trend graph)

**Key Metrics:**
- **Pass Rate:** Target ≥ 95% (main), ≥ 90% (feature branches)
- **Flakiness:** Tests with >10% failure rate (auto-detected by ML)
- **Duration:** Flag tests >30s (integration), >5s (unit)

**Creating Filters:**

1. Go to Filters → Add Filter
2. Create filters for:
   - **Your Stories**: `story:story_3_*` (replace 3 with your epic)
   - **Your Service**: `service:backend` or `service:frontend`
   - **Recent Failures**: `status:FAILED AND startTime:7d`
3. Save and pin to dashboard

**Widgets:**

Add these to your personal dashboard:
- **Launch Statistics Timeline**: See pass/fail trends over last 30 launches
- **Failed Test Cases Top-20**: Identify flaky tests
- **Overall Statistics**: Current sprint health

**Learn more:** [INTEGRATION.md](INTEGRATION.md#reportportal-integration)

---

### Interpreting Allure Reports (Local Development)

**Access:** `https://your-org.github.io/your-repo/`

**Features:**
- **Story Index:** List with coverage badges (✅ 100%, ⚠️ 50-99%, ❌ <50%)
- **Story Details:** Acceptance criteria, test results per service, execution history
- **Service Tables:** Test status breakdown
- **History:** Chronological timeline with ReportPortal links

### Quality Gates

**Recommended gates:**
1. Test pass rate ≥ 95%
2. Story coverage ≥ 80%
3. No critical bugs (P0/P1)
4. Performance: API <500ms (p95), UI <2s load

**Enforcement (CI/CD):**
```yaml
- name: Check Quality Gates
  run: npm run quality-gates -- --check
  # Fails PR if gates not met
```

---

## 🎯 Best Practices

### Test Naming

- **Backend:** `test<Feature><Scenario>` (e.g., `testCreateTradeWithValidData`)
- **Frontend:** `should <behavior>` (e.g., `should create trade when form submitted`)
- **Flow:** `should <end-to-end scenario>`

### Test Organization

- **Unit Tests:** Test methods/functions in isolation
- **Integration Tests:** Test with dependencies (DB, APIs)
- **Flow Tests:** Test complete user journeys

### Allure Annotations

Always include:
```java
@AllureEpic("Epic 03 - CDS Trade Capture")
@AllureFeature("CDS Trade Capture")
@AllureStory("Create Single Name CDS Trade")
@Description("AC 1.1: Should create trade with valid data")
```

```typescript
allure.epic('Epic 03 - CDS Trade Capture');
allure.feature('CDS Trade Capture');
allure.story('Create Single Name CDS Trade');
allure.description('AC 1.1: Should create trade with valid data');
```

### Assertions

Use descriptive messages:
```java
// Good
assertThat(response.getStatusCode())
    .as("Trade creation should return 201 Created")
    .isEqualTo(HttpStatus.CREATED);

// Bad
assertThat(response.getStatusCode()).isEqualTo(201);
```

### Error Handling

Test both happy path and errors:
```java
@Test void testCreateTradeWithValidData() { /* happy path */ }
@Test void testCreateTradeWithMissingEntity() { /* error case */ }
@Test void testCreateTradeWithNegativeNotional() { /* error case */ }
```

### Test Data

- Use Test Data Registry for shared data
- Create test-specific data for edge cases
- Clean up after tests (TestContainers handles this automatically)

---

## 🤖 GitHub Copilot Integration

### Overview

The Test Evidence Framework includes AI-powered automation via GitHub Copilot prompts. This provides the **fastest and most comprehensive way** to generate tests.

### Available Prompts

**Location:** `.github/prompts/`

#### 1. Evidence-Based Test Generation
**File:** `generate-evidence-tests.prompt.md`

**Purpose:** Generate complete, production-ready tests from user story acceptance criteria

**Usage:**
```bash
@workspace /generate-evidence-tests.prompt STORY_ID=story_3_1
```

**What It Does:**
1. ✅ Locates story file automatically
2. ✅ Parses acceptance criteria
3. ✅ Auto-detects services (frontend/backend/gateway/risk-engine)
4. ✅ Generates complete tests with Given/When/Then
5. ✅ Adds Allure annotations for traceability
6. ✅ Creates realistic assertions based on AC semantics
7. ✅ Optionally runs tests and generates evidence report

**Flags:**
- `DRY_RUN=true` - Show plan without creating files
- `FORCE=true` - Regenerate existing tests
- `EXECUTE=true` - Generate, run tests, create report
- `SERVICES=backend` - Override auto-detection

**Output:**
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
  ],
  "traceability": {
    "acceptanceCriteria": 6,
    "totalTestCases": 16,
    "coverageComplete": true
  }
}
```

#### 2. Epic Planning
**File:** `plan-epic.prompt.md`

**Purpose:** Plan epic, create story files, and GitHub issues

**Usage:**
```bash
@workspace /plan-epic.prompt EPIC_NUMBER=5
```

#### 3. Epic Implementation
**File:** `implement-epic.prompt.md`

**Purpose:** Implement all stories in an epic (database, backend, frontend, tests)

**Usage:**
```bash
@workspace /implement-epic.prompt EPIC_NUMBER=5
```

### Key Advantages of Copilot Prompts

#### vs. Manual CLI
| Aspect | Copilot Prompt | Manual CLI |
|--------|---------------|------------|
| **Speed** | 5-10 min | 30-60 min |
| **Completeness** | Complete code | Scaffolds only |
| **Multi-Service** | All in one command | One at a time |
| **Service Detection** | Auto | Manual |
| **Semantic Analysis** | AI-powered | Template-based |
| **Idempotency** | Built-in | Manual tracking |

#### Complete vs. Scaffold Generation

**Copilot Prompt** (Complete):
```java
@Test
@Story("Story 3.1")
void testValidTradeSubmission() {
    // GIVEN: Valid CDS trade data
    String request = """
        {
          "tradeDate": "2025-01-15",
          "notional": 10000000
        }
        """;
    
    // WHEN: Trade is submitted
    ResponseEntity<String> response = restTemplate.postForEntity(
        "/api/trades",
        new HttpEntity<>(request, headers),
        String.class
    );
    
    // THEN: Trade is created successfully
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody().contains("\"tradeId\""));
}
```

**Manual CLI** (Scaffold):
```java
@Test
@Story("Story 3.1")
void testValidTradeSubmission() {
    // TODO: Add test implementation
    // TODO: Add assertions
}
```

### Best Practices

1. **Use Copilot Prompts First**
   - Start with `@workspace /generate-evidence-tests.prompt`
   - Fall back to CLI only if customization needed

2. **Run Dry Run for New Stories**
   ```bash
   @workspace /generate-evidence-tests.prompt STORY_ID=story_X_Y DRY_RUN=true
   ```
   Review plan before generation.

3. **Leverage Idempotency**
   - Safe to re-run after story updates
   - Use `FORCE=true` to regenerate existing tests

4. **Combine with TDD**
   ```bash
   # Generate tests first
   @workspace /generate-evidence-tests.prompt STORY_ID=story_5_2
   
   # Implement to make tests pass
   # ...
   
   # Verify
   cd backend && mvn test -Dtest="*Story5_2*"
   ```

5. **Generate Evidence in One Command**
   ```bash
   @workspace /generate-evidence-tests.prompt STORY_ID=story_3_1 EXECUTE=true
   ```
   Generates tests, runs them, creates Allure report.

### Documentation

- **Prompt Specifications:** `.github/prompts/*.prompt.md`
- **Usage Guide:** `.github/prompts/README.md`
- **Practical Examples:** `.github/prompts/EXAMPLES.md`
- **Workflow Details:** `GENERATE_TESTS_WORKFLOW.md`

---

## 📚 Additional Resources

- **[GETTING_STARTED.md](GETTING_STARTED.md)** - Quick start
- **[INTEGRATION.md](INTEGRATION.md)** - CI/CD, ReportPortal, evidence export
- **[REFERENCE.md](REFERENCE.md)** - Troubleshooting, services matrix, writing criteria
- **[Story Template](../user-stories/STORY_TEMPLATE.md)** - Story authoring
- **[Framework README](../README.md)** - Architecture overview

---

**Questions?** Check [REFERENCE.md](REFERENCE.md) or ask in Slack: `#cds-testing`
