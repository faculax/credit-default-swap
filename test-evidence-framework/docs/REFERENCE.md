# Reference Guide

**Troubleshooting, Service Selection, and Writing Best Practices**

---

## 📋 Table of Contents

1. [Troubleshooting](#troubleshooting)
2. [Service Selection Guide](#service-selection-guide)
3. [Writing Good Acceptance Criteria](#writing-good-acceptance-criteria)

---

## 🔧 Troubleshooting

### Top 10 Common Issues

#### 1. Framework Build Fails

**Symptoms:**
```
error TS2307: Cannot find module '@shared/TestRegistry'
```

**Solution:**
```bash
cd test-evidence-framework
npm install
npm run build

# Verify
npm run test
```

**Root cause:** TypeScript compilation error due to missing dependencies or circular imports.

---

#### 2. Test Generation Produces No Output

**Symptoms:**
```bash
npm run generate -- --service backend
# No files generated
```

**Solution:**
1. Check story file syntax:
   ```bash
   npm run validate-story -- --file user-stories/epic_03/story_3_1.md
   ```
2. Verify story has acceptance criteria section
3. Check for parsing errors in console output
4. Run with `--dry-run` to see plan:
   ```bash
   npm run generate -- --service backend --dry-run
   ```

**Root cause:** Story markdown doesn't match expected format (missing sections, invalid headers).

---

#### 3. Maven Tests Fail with PostgreSQL Connection Error

**Symptoms:**
```
org.postgresql.util.PSQLException: Connection refused
```

**Solution:**
1. Verify Docker running: `docker ps`
2. Check TestContainers configuration:
   ```java
   @Container
   static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
       .withDatabaseName("test")
       .withUsername("test")
       .withPassword("test");
   ```
3. Ensure Docker Desktop started (Windows)
4. Check TestContainers logs: `~/.testcontainers/`

**Root cause:** TestContainers can't connect to Docker daemon.

---

#### 4. Frontend Tests Fail with "Cannot find module"

**Symptoms:**
```
Cannot find module '@/components/TradeForm' from 'TradeForm.test.tsx'
```

**Solution:**
1. Check TypeScript paths in `tsconfig.json`:
   ```json
   {
     "compilerOptions": {
       "baseUrl": "src",
       "paths": {
         "@/*": ["*"]
       }
     }
   }
   ```
2. Restart Jest: `npm test -- --clearCache`
3. Verify file exists at `src/components/TradeForm.tsx`

**Root cause:** Path alias misconfiguration or stale Jest cache.

---

#### 5. Coverage Validation Fails Incorrectly

**Symptoms:**
```
❌ AC2: "System validates trade details"
   Reason: Missing test for criterion
```
But test exists.

**Solution:**
1. Check test includes `@Story("3.1")` annotation
2. Verify test method name includes criterion keyword:
   ```java
   @Test
   @Story("3.1")
   public void testSystemValidatesTradeDetails() { ... }
   ```
3. Run validator with `--verbose`:
   ```bash
   npm run validate-coverage -- --story-id 3.1 --verbose
   ```

**Root cause:** Test missing story annotation or doesn't match criterion keywords.

---

#### 6. ReportPortal Upload Fails (401)

**Symptoms:**
```
Error: Failed to upload to ReportPortal: 401 Unauthorized
```

**Solution:**
1. Verify API token:
   ```bash
   # Test authentication
   curl -H "Authorization: Bearer YOUR_TOKEN" \
     https://reportportal.example.com/api/v1/user
   ```
2. Check `reportportal.json` configuration:
   ```json
   {
     "endpoint": "https://reportportal.example.com",
     "token": "valid-api-token",
     "project": "cds-platform"
   }
   ```
3. Regenerate token in ReportPortal UI (Profile → API Token)

**Root cause:** Expired or invalid API token.

---

#### 7. Evidence Dashboard Shows No Data

**Symptoms:**
Dashboard loads but shows "No stories found".

**Solution:**
1. Verify ReportPortal has data:
   - Open ReportPortal UI
   - Check "Launches" tab for recent executions
2. Run export with verbose logging:
   ```bash
   npm run export-evidence -- --output-dir ./test-export --verbose
   ```
3. Check for API errors in console
4. Verify launch attributes include `story:story_X_Y`

**Root cause:** No launches with story attributes in ReportPortal.

---

#### 8. CI/CD Workflow Skips All Tests

**Symptoms:**
GitHub Actions workflow completes in 30s without running tests.

**Solution:**
1. Check path filters in `.github/workflows/test-evidence.yml`:
   ```yaml
   filters:
     backend: 'backend/**'
   ```
2. Verify changed files match filters:
   ```bash
   git diff --name-only origin/main
   ```
3. Manually trigger with "Run all tests" option
4. Push a change to `test-evidence-framework/` to force all tests

**Root cause:** Path filters exclude all changed files.

---

#### 9. Generated Tests Don't Compile

**Symptoms:**
```
error: cannot find symbol
  symbol: class TradeEntity
```

**Solution:**
1. Check imports in generated test file
2. Verify entity classes exist in `src/main/java`
3. Update template if needed: `templates/backend/integration-test.java.hbs`
4. Regenerate tests:
   ```bash
   npm run generate -- --service backend
   ```

**Root cause:** Template references classes that don't exist or wrong package.

---

#### 10. Crystallization Prevents Necessary Changes

**Symptoms:**
```
Warning: Test is crystallized and should not be modified
```
But need to update test.

**Solution:**
1. Unlock crystallization:
   ```bash
   npm run crystallize -- --story-id 3.1 --unlock
   ```
2. Make necessary changes
3. Re-lock after validation:
   ```bash
   npm run crystallize -- --story-id 3.1
   ```

**Root cause:** Tests locked but requirements changed.

---

### Quick Diagnostics

**Framework health check:**
```bash
cd test-evidence-framework
npm run build && npm test
```

**Backend test health check:**
```bash
cd backend
./mvnw clean test -Dtest=HealthCheckTest
```

**Frontend test health check:**
```bash
cd frontend
npm test -- --testNamePattern="smoke"
```

**ReportPortal connectivity:**
```bash
curl -H "Authorization: Bearer $REPORTPORTAL_TOKEN" \
  $REPORTPORTAL_ENDPOINT/api/v1/user
```

---

## 🎯 Service Selection Guide

### When to Use Each Service Type

#### Backend Integration Tests

**Use for:**
- REST API endpoint testing (POST, GET, PUT, DELETE)
- End-to-end database integration
- Multi-layer validation (Controller → Service → Repository)
- Transaction rollback testing
- Authentication/authorization flows

**Example scenarios:**
- "User submits a trade creation request"
- "System validates and persists trade to database"
- "System rejects invalid trade with 400 error"
- "User retrieves trade by ID"

**Test characteristics:**
- `@SpringBootTest` with full context
- `@Transactional` for rollback
- TestContainers for PostgreSQL
- MockMvc or RestAssured for HTTP
- 2-5 seconds execution time

**Generated structure:**
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Transactional
public class TradeCreationIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(...);
    
    @Test
    public void testUserSubmitsTradeCreationRequest() { ... }
}
```

---

#### Backend Repository Tests

**Use for:**
- Database query testing
- Custom repository method validation
- JPA relationship verification
- Data constraint testing

**Example scenarios:**
- "Repository finds trades by status"
- "Repository filters trades by date range"
- "System enforces unique constraint on trade ID"
- "Repository handles null values correctly"

**Test characteristics:**
- `@DataJpaTest` with minimal context
- In-memory H2 database (fast)
- Only JPA/repository beans loaded
- <1 second execution time

**Generated structure:**
```java
@DataJpaTest
public class TradeRepositoryTest {
    @Autowired
    private TradeRepository repository;
    
    @Test
    public void testFindTradesByStatus() { ... }
}
```

---

#### Backend Service Tests

**Use for:**
- Business logic validation (pure functions)
- Service method behavior with mocked dependencies
- Error handling and exception flows
- Edge case scenarios

**Example scenarios:**
- "Service calculates notional amount correctly"
- "Service validates trade date against settlement date"
- "Service throws exception for negative notional"
- "Service calls repository with correct parameters"

**Test characteristics:**
- No Spring context (plain JUnit)
- Mockito for mocking dependencies
- Very fast (<100ms)
- Isolated unit tests

**Generated structure:**
```java
@ExtendWith(MockitoExtension.class)
public class TradeServiceTest {
    @Mock
    private TradeRepository repository;
    
    @InjectMocks
    private TradeService service;
    
    @Test
    public void testCalculateNotionalAmount() { ... }
}
```

---

#### Frontend Component Tests

**Use for:**
- React component rendering
- User interaction (click, type, select)
- Conditional rendering logic
- Prop validation
- Accessibility testing

**Example scenarios:**
- "Trade form renders all input fields"
- "User clicks submit button to create trade"
- "System displays validation error for empty fields"
- "Component shows loading state during API call"

**Test characteristics:**
- React Testing Library
- Mocked API calls (MSW or jest.fn)
- DOM assertions
- Fast (<100ms per test)

**Generated structure:**
```typescript
describe('TradeForm Component', () => {
  it('should render all input fields', () => {
    render(<TradeForm />);
    expect(screen.getByLabelText(/Trade ID/i)).toBeInTheDocument();
  });
  
  it('should submit trade on button click', async () => {
    render(<TradeForm />);
    await userEvent.click(screen.getByRole('button', { name: /Submit/i }));
    // assertions
  });
});
```

---

#### Frontend Hook Tests

**Use for:**
- Custom React hooks behavior
- State management logic
- Side effects (useEffect)
- Hook composition

**Example scenarios:**
- "useTrade hook fetches trade data on mount"
- "useTradeForm hook validates input on change"
- "useAuth hook redirects if not authenticated"

**Test characteristics:**
- `renderHook` from React Testing Library
- Isolated hook testing
- Fast (<50ms per test)

**Generated structure:**
```typescript
describe('useTrade Hook', () => {
  it('should fetch trade data on mount', async () => {
    const { result } = renderHook(() => useTrade('TRADE-001'));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.trade).toBeDefined();
  });
});
```

---

#### Frontend Integration Tests

**Use for:**
- Multi-component flows
- API integration (with MSW)
- Routing and navigation
- Global state management

**Example scenarios:**
- "User navigates from trade list to trade details"
- "User creates trade and sees success message"
- "System handles API error gracefully"

**Test characteristics:**
- Multiple components rendered together
- MSW for API mocking
- React Router integration
- Slower (1-3 seconds per test)

**Generated structure:**
```typescript
describe('Trade Creation Flow', () => {
  beforeEach(() => {
    server.use(
      rest.post('/api/trades', (req, res, ctx) => {
        return res(ctx.json({ id: 'TRADE-001' }));
      })
    );
  });
  
  it('should create trade and show success', async () => {
    render(<App />);
    // multi-step user interaction
  });
});
```

---

#### Flow Tests (End-to-End)

**Use for:**
- Multi-service scenarios
- Complete user journeys
- System-level integration
- Critical path validation

**Example scenarios:**
- "User creates trade, system calculates risk, user views margin"
- "System processes credit event affecting multiple trades"
- "User uploads CSV, system imports trades, user sees summary"

**Test characteristics:**
- Multiple services involved (backend, frontend, gateway, risk-engine)
- Docker Compose for infrastructure
- Playwright or Selenium for browser automation
- Slowest (10-30 seconds per test)

**Generated structure:**
```typescript
describe('Trade Lifecycle Flow', () => {
  it('should create trade, calculate risk, and view margin', async () => {
    // Step 1: Create trade (frontend → backend)
    // Step 2: Trigger risk calculation (backend → risk-engine)
    // Step 3: View margin (frontend)
    // Assertions across all services
  });
});
```

---

### Decision Matrix

| Criterion | Backend Integration | Backend Repository | Backend Service | Frontend Component | Frontend Hook | Frontend Integration | Flow (E2E) |
|-----------|---------------------|-------------------|----------------|-------------------|--------------|---------------------|-----------|
| **API endpoint** | ✅ | ❌ | ❌ | ❌ | ❌ | ⚠️ | ✅ |
| **Database query** | ⚠️ | ✅ | ❌ | ❌ | ❌ | ❌ | ⚠️ |
| **Business logic** | ⚠️ | ❌ | ✅ | ❌ | ⚠️ | ❌ | ❌ |
| **UI rendering** | ❌ | ❌ | ❌ | ✅ | ❌ | ⚠️ | ✅ |
| **User interaction** | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ | ✅ |
| **React hook** | ❌ | ❌ | ❌ | ❌ | ✅ | ⚠️ | ❌ |
| **Multi-component** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| **Multi-service** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| **Speed** | Medium (2-5s) | Fast (<1s) | Very Fast (<100ms) | Fast (<100ms) | Very Fast (<50ms) | Medium (1-3s) | Slow (10-30s) |

**Legend:**
- ✅ Primary use case
- ⚠️ Possible but not ideal
- ❌ Not applicable

---

### Anti-Patterns

#### ❌ Don't Use Backend Integration for Pure Logic

**Bad:**
```java
@SpringBootTest
public class CalculationTest {
    @Autowired
    private CalculationService service;
    
    @Test
    public void testAddition() {
        assertEquals(5, service.add(2, 3));
    }
}
```

**Good:**
```java
@ExtendWith(MockitoExtension.class)
public class CalculationServiceTest {
    @InjectMocks
    private CalculationService service;
    
    @Test
    public void testAddition() {
        assertEquals(5, service.add(2, 3));
    }
}
```

**Why:** No need for full Spring context for pure logic.

---

#### ❌ Don't Use Flow Tests for Single Component

**Bad:**
```typescript
// flow-tests/button-click.test.ts
describe('Button Click Flow', () => {
  it('should trigger onClick', () => { ... });
});
```

**Good:**
```typescript
// components/Button.test.tsx
describe('Button Component', () => {
  it('should trigger onClick', () => { ... });
});
```

**Why:** Overkill for simple component testing.

---

#### ❌ Don't Use Component Tests for Hook Logic

**Bad:**
```typescript
describe('Component with useTrade', () => {
  it('should fetch trade data', () => {
    render(<ComponentUsingUseTrade />);
    // Testing hook behavior through component
  });
});
```

**Good:**
```typescript
describe('useTrade Hook', () => {
  it('should fetch trade data', () => {
    const { result } = renderHook(() => useTrade('TRADE-001'));
    // Direct hook testing
  });
});
```

**Why:** Isolate hook logic for faster, clearer tests.

---

## ✍️ Writing Good Acceptance Criteria

### Anatomy of a Good Criterion

**Template:**
```
[Actor] [Action] [Object] [Context/Constraint]
```

**Examples:**

✅ **Good:**
- "User submits a trade creation request with valid ISDA trade details"
- "System validates trade notional is positive and less than $1 billion"
- "System persists trade to database with PENDING status"
- "System returns 400 error if buyer party is missing"

❌ **Bad:**
- "Trade creation works" (too vague)
- "The system should probably validate some things" (non-specific)
- "User does trade stuff and sees results" (unclear action)

---

### SMART Criteria

**Specific:** Clear subject, action, and expected outcome
**Measurable:** Can verify pass/fail objectively
**Achievable:** Realistic for one test
**Relevant:** Directly supports story goal
**Testable:** Can write automated test

**Example:**

✅ **SMART:**
"System calculates upfront payment as NPV of fixed and floating legs with 2-decimal precision"

❌ **Not SMART:**
"System calculates payment" (not specific, not measurable)

---

### Actor Clarity

**Specify WHO performs the action:**

✅ **Good:**
- "User submits..."
- "System validates..."
- "Admin approves..."
- "Scheduler triggers..."

❌ **Bad:**
- "Trade is submitted..." (passive voice, unclear actor)
- "Validation happens..." (unclear who validates)

---

### Testable Verbs

**Use specific, testable verbs:**

| Use This | Not This |
|----------|----------|
| validates | checks |
| persists | saves |
| returns | gives |
| calculates | figures out |
| rejects | doesn't accept |
| displays | shows |

**Why:** Specific verbs map directly to test assertions.

---

### Input/Output Specification

**Specify WHAT goes in and WHAT comes out:**

✅ **Good:**
- "System accepts trade creation request with fields: tradeId, notional, maturity, buyer, seller"
- "System returns HTTP 201 with created trade JSON including generated ID and timestamp"

❌ **Bad:**
- "System accepts trade" (what fields?)
- "System returns success" (what exactly?)

---

### Boundary Conditions

**Include edge cases:**

✅ **Good:**
- "System validates notional is positive and less than $1 billion"
- "System rejects maturity date before trade date"
- "System accepts spreads from -1000 to +1000 basis points"

❌ **Bad:**
- "System validates notional" (what's valid?)

---

### Error Scenarios

**Don't forget unhappy paths:**

✅ **Good:**
- AC4: "System validates trade details are complete and correct"
- AC5: "System returns 400 error with descriptive message if validation fails"

❌ **Bad:**
- AC4: "System validates trade details" (no error case)

---

### Granularity

**One testable assertion per criterion:**

✅ **Good:**
- AC1: "System validates trade ID is unique"
- AC2: "System validates notional is positive"
- AC3: "System validates maturity date is after trade date"

❌ **Bad:**
- AC1: "System validates trade ID is unique, notional is positive, and maturity date is valid"
  (Too many assertions; should be 3 separate criteria)

---

### Domain-Specific Language

**Use business terms:**

✅ **Good:**
- "System calculates NPV using discount curve"
- "System applies credit spread to fixed leg"
- "System determines protection buyer and seller"

❌ **Bad:**
- "System calculates value using math"
- "System applies number to thing"

---

### Acceptance Criteria Checklist

Before finalizing story:

- [ ] Each AC starts with actor (User/System/Admin)
- [ ] Each AC uses specific, testable verb
- [ ] Each AC specifies inputs and expected outputs
- [ ] Boundary conditions included
- [ ] Error scenarios covered
- [ ] Each AC is independently testable
- [ ] No overlapping criteria
- [ ] Domain language used consistently
- [ ] No implementation details (what, not how)

---

### Good Story Example

**Story 3.1: Single-Name CDS Trade Creation**

**Acceptance Criteria:**

1. User submits trade creation request with ISDA trade details (tradeId, notional, maturity, spread, buyer, seller)
2. System validates trade ID is unique across all trades
3. System validates notional is positive and less than $1 billion
4. System validates maturity date is after trade date and within 30 years
5. System validates buyer and seller parties exist in reference data
6. System calculates upfront payment based on market spread and trade spread
7. System persists trade to database with PENDING status and current timestamp
8. System returns HTTP 201 with created trade JSON including generated UUID and calculated fields
9. System returns HTTP 400 with validation error details if any field fails validation
10. System logs trade creation event to audit log

**Why this works:**
- Each AC is specific and testable
- Covers happy path (AC1-8) and error path (AC9)
- Includes boundary conditions (AC3, AC4)
- Uses domain language (ISDA, notional, spread, upfront payment)
- Clear actors (User in AC1, System in AC2-10)
- Observable outcomes (HTTP status codes, database records, audit logs)

---

### Poor Story Example

**Story: Trade Stuff**

**Acceptance Criteria:**

1. User creates trade
2. System validates
3. Trade is saved
4. Errors are handled

**Why this fails:**
- Vague actors ("Trade is saved" — passive voice)
- No specific inputs/outputs
- No boundary conditions
- No error details
- Not independently testable
- No domain language

---

### Keyword Matching for Coverage

**Validator looks for keyword matches between AC and test names:**

**AC:**
"System validates trade notional is positive"

**Good test names:**
```java
@Test
public void testSystemValidatesTradeNotionalIsPositive() { ... }
```
```typescript
it('should validate trade notional is positive', () => { ... });
```

**Bad test names:**
```java
@Test
public void testTrade() { ... }  // Too vague, won't match
```
```typescript
it('should work', () => { ... });  // No keywords
```

**Matching rules:**
- Minimum 50% keyword overlap
- Case-insensitive
- Ignores stop words (is, the, a, and, or)
- Stemming (validate/validates/validated)

---

### Template for New Stories

Use `test-evidence-framework/STORY_TEMPLATE.md`:

```markdown
# Story X.Y: [Title]

## Description
[User-facing description of the feature]

## Acceptance Criteria

1. [Actor] [action] [object] [context]
2. [Actor] [action] [object] [context]
...

## Services Involved
- [ ] Backend
- [ ] Frontend
- [ ] Gateway
- [ ] Risk Engine

## Test Data Required
- [Entity 1]: [Description]
- [Entity 2]: [Description]

## Dependencies
- [Other stories or external systems]

## Notes
- [Any clarifications or assumptions]
```

---

**Need more help?** Check [USER_GUIDE.md](USER_GUIDE.md) for development workflow and [INTEGRATION.md](INTEGRATION.md) for CI/CD setup.
