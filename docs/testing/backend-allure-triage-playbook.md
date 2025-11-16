# Backend Test Failure Triage Playbook

**Epic 03: Backend Allure Integration**  
**Story 3.5: Backend Failure Triage Using Allure Artifacts**  
**Version:** 1.0  
**Last Updated:** November 14, 2025

This playbook guides engineers through debugging backend test failures using Allure reports, providing systematic approaches to identify root causes and resolve issues efficiently.

---

## Table of Contents

1. [Quick Reference](#quick-reference)
2. [Locating Allure Artifacts](#locating-allure-artifacts)
3. [Understanding Allure Report Structure](#understanding-allure-report-structure)
4. [Common Failure Signatures](#common-failure-signatures)
5. [Triage Decision Tree](#triage-decision-tree)
6. [Investigation Workflows](#investigation-workflows)
7. [Retrieving Additional Context](#retrieving-additional-context)
8. [Escalation Paths](#escalation-paths)
9. [Preventive Measures](#preventive-measures)

---

## Quick Reference

### Emergency Response

**Test failed in CI? Follow these steps:**

1. **Locate artifacts** → CI logs show artifact URL or download link
2. **Open Allure report** → Extract and open `index.html` or use `mvn allure:serve`
3. **Check failure category** → Navigate to failed test in report
4. **Assess flakiness** → Review History tab for intermittent failures
5. **Gather context** → Check test body, stack trace, attachments
6. **Reproduce locally** → Run same test with `mvn test -Dtest=ClassName`
7. **Fix or escalate** → Apply fix or escalate per decision tree below

### Key Commands

```bash
# Generate local report from results
mvn allure:serve

# Run specific failing test
mvn clean test -Dtest=CDSTradeServiceTest#shouldCreateTrade

# Run all tests in failing class
mvn clean test -Dtest=CDSTradeServiceTest

# Run tests with debug logging
mvn clean test -Dtest=CDSTradeServiceTest -Dlogging.level.root=DEBUG
```

---

## Locating Allure Artifacts

### Local Development

After running tests locally, Allure results are in:
```
<service-root>/target/allure-results/
```

Generate and view report:
```bash
cd backend/  # or gateway/ or risk-engine/
mvn allure:serve
```

### CI Pipeline (Future - Story 3.4)

When CI pipeline is implemented, artifacts will be:
- **Pull Requests:** Attached to PR check with link in GitHub checks
- **Main Branch:** Published to artifact store (e.g., S3, Azure Blob)
- **Access:** Download zip from CI logs artifact section

Expected structure:
```
allure-results-backend-<build-number>.zip
├── backend/
│   └── target/allure-results/*.json
├── gateway/
│   └── target/allure-results/*.json
└── risk-engine/
    └── target/allure-results/*.json
```

---

## Understanding Allure Report Structure

### Main Views

#### 1. **Overview Dashboard**
- **Pass Rate:** Percentage of passed tests
- **Duration:** Total execution time
- **Environment:** Test environment details
- **Trends:** Historical pass/fail chart

**What to look for:**
- Sudden drop in pass rate → investigate recent changes
- Increasing duration → performance regression
- Consistent failures → systematic issue vs flaky test

#### 2. **Suites View**
- Tests organized by package/class hierarchy
- Shows: `com.creditdefaultswap.backend.service.CDSTradeServiceTest`
- Expandable tree structure

**What to look for:**
- All tests in one suite failing → shared setup/teardown issue
- Single test failing → specific test logic problem
- Pattern across packages → dependency issue (e.g., database, messaging)

#### 3. **Timeline View**
- Horizontal timeline showing test execution order
- Visual duration bars
- Parallel vs sequential execution

**What to look for:**
- Long duration tests blocking others → optimize or parallelize
- Tests failing after specific test runs → test isolation issue
- Time gaps → setup/teardown overhead

#### 4. **Behaviors View** (when using @Epic, @Feature, @Story annotations)
- Tests grouped by business functionality
- Story-level organization

**What to look for:**
- All tests for one story failing → feature broke
- Cross-story failures → infrastructure issue

#### 5. **Categories View**
- Failures grouped by exception type
- Custom categorization

**Common Categories:**
- Product defects (assertion failures)
- Test defects (setup issues, bad test data)
- System issues (database unavailable, timeout)

---

## Common Failure Signatures

### 1. **AssertionError: Expected vs Actual Mismatch**

**Example:**
```
org.opentest4j.AssertionFailedError: expected: <1000.0> but was: <0.0>
  at CDSTradeServiceTest.shouldCalculateNotional(CDSTradeServiceTest.java:89)
```

**Likely Causes:**
- **Business logic bug:** Recent code change broke calculation
- **Test data issue:** Expected value changed but test not updated
- **Timing issue:** Value not yet calculated when assertion runs

**Investigation Steps:**
1. Check recent commits affecting the tested method
2. Review test data setup in `@BeforeEach`
3. Add logging to trace actual value computation
4. Check if test passes when run in isolation

**Allure Indicators:**
- Status: `failed`
- Category: `Product defect` (if production code broke) or `Test defect` (if test wrong)
- History: Consistent failure = likely code issue; intermittent = timing/flakiness

---

### 2. **NullPointerException in Test Code**

**Example:**
```
java.lang.NullPointerException: Cannot invoke "getTrade()" because "result" is null
  at CDSTradeServiceTest.shouldRetrieveTrade(CDSTradeServiceTest.java:72)
```

**Likely Causes:**
- **Mock not configured:** Required dependency returning null
- **Setup incomplete:** Missing `@BeforeEach` initialization
- **Service layer bug:** Method actually returning null incorrectly

**Investigation Steps:**
1. Check all mocks are stubbed: `when(service.method()).thenReturn(value)`
2. Verify `@Mock` and `@InjectMocks` annotations present
3. Check if production code changed to return Optional instead of object
4. Run test with debugger to see which method returns null

**Allure Indicators:**
- Status: `failed`
- Stack trace shows test class line number → test bug
- Stack trace shows production class → service bug
- Timeline: Fails immediately → setup issue; fails mid-test → logic issue

---

### 3. **Spring Context Initialization Failure**

**Example:**
```
java.lang.IllegalStateException: Failed to load ApplicationContext
Caused by: org.springframework.beans.factory.BeanCreationException: 
  Error creating bean with name 'dataSource': Could not resolve placeholder 'spring.datasource.url'
```

**Likely Causes:**
- **Missing test properties:** `application-test.yml` incomplete
- **Profile not activated:** Test missing `@ActiveProfiles("test")`
- **Dependency injection issue:** Circular dependency or missing bean
- **Database/external service unavailable:** Testcontainers not starting

**Investigation Steps:**
1. Check `src/test/resources/application-test.yml` exists and is complete
2. Verify test class has `@SpringBootTest` and `@ActiveProfiles("test")`
3. Look for recent changes to application configuration
4. Check if Testcontainers Docker image is pulling correctly
5. Review logs for port conflicts or resource locks

**Allure Indicators:**
- Status: `broken` (not `failed`)
- All integration tests in service fail → context issue
- Timeline shows no test execution → failed before tests ran
- Attachments may include full Spring logs

---

### 4. **Timeout Exceeded**

**Example:**
```
org.junit.jupiter.api.extension.TestExecutionTimeoutException: 
  Test method timed out after 5000 milliseconds
```

**Likely Causes:**
- **Infinite loop or deadlock:** Code logic bug
- **External service not responding:** Database query hanging, HTTP call timing out
- **Resource contention:** CI environment under load
- **Missing mock:** Test calling real service instead of mock

**Investigation Steps:**
1. Check if timeout is realistic for test operation
2. Review recent changes for blocking operations
3. Verify mocks configured for all external calls
4. Check database connection pool settings
5. Look for `await()` or `sleep()` statements that may be too long
6. Run test locally to see if it's environment-specific

**Allure Indicators:**
- Status: `broken`
- Duration: Exactly matches timeout value
- History: Random timeouts = flakiness; consistent = deterministic issue
- Timeline: Shows where test was stuck

---

### 5. **Database Constraint Violation**

**Example:**
```
org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint "uk_trade_reference"
  Detail: Key (trade_reference)=(TRD-001) already exists.
```

**Likely Causes:**
- **Test data collision:** Multiple tests using same ID
- **Cleanup incomplete:** `@AfterEach` not clearing data
- **Test order dependency:** Test expects clean database but previous test left data
- **Flyway/Liquibase migration issue:** Schema not matching expectations

**Investigation Steps:**
1. Check if test uses unique test data (e.g., `UUID.randomUUID()` or test method name)
2. Verify `@Transactional` on test class to auto-rollback
3. Review `@AfterEach` methods for proper cleanup
4. Check if tests pass when run individually but fail in suite
5. Inspect database schema for unexpected constraints

**Allure Indicators:**
- Status: `failed`
- Multiple tests failing with same constraint → shared data issue
- Fails in CI but not locally → order dependency
- History: Intermittent → race condition or parallel test execution issue

---

### 6. **Mockito UnnecessaryStubbingException**

**Example:**
```
org.mockito.exceptions.misusing.UnnecessaryStubbingException:
  Unnecessary stubbings detected.
  1. -> at CDSTradeServiceTest.shouldCalculate(CDSTradeServiceTest.java:45)
  Please remove unnecessary stubbings or use 'lenient()'.
```

**Likely Causes:**
- **Refactored code:** Method no longer calls the stubbed dependency
- **Dead test code:** Test setup includes unused stubs
- **Conditional execution:** Stub only used in certain test branches

**Investigation Steps:**
1. Review which stubs are reported as unnecessary
2. Check if production code changed to not call that method
3. Consider if stub needed for different test execution path
4. Use `lenient()` if stub intentionally optional
5. Remove stub if truly not needed

**Allure Indicators:**
- Status: `failed`
- Recent code change likely removed method call
- All tests in class may fail if in `@BeforeEach`

---

## Triage Decision Tree

```
┌─────────────────────────────────┐
│   Test Failed in Build/PR       │
└────────────┬────────────────────┘
             │
             ▼
    ┌────────────────────┐
    │ Check Allure       │
    │ Report Status      │
    └────┬───────────────┘
         │
         ├─► "failed" (red X) ─────► Assertion/Logic Failure
         │                            ├─► Expected vs Actual mismatch?
         │                            │   ├─► YES → Business logic bug
         │                            │   └─► NO  → Test data issue
         │                            └─► NullPointer/Exception?
         │                                ├─► In test → Test bug
         │                                └─► In prod → Service bug
         │
         ├─► "broken" (yellow !) ──► Infrastructure Failure
         │                            ├─► Context load failed?
         │                            │   ├─► Config issue
         │                            │   └─► Dependency unavailable
         │                            ├─► Timeout?
         │                            │   ├─► Deadlock/infinite loop
         │                            │   └─► Resource contention
         │                            └─► Setup/teardown error?
         │                                ├─► BeforeEach/AfterEach bug
         │                                └─► Test isolation issue
         │
         └─► "passed" but marked ──► False Positive/Flaky
             for investigation       ├─► Check History tab
                                    │   ├─► 90%+ pass → Flaky test
                                    │   └─► Recent change → Monitor
                                    └─► Timeline shows duration spike
                                        └─► Performance regression
```

### Decision Points

**Is this failure consistent or intermittent?**
- **Consistent (fails every run):**
  - High priority
  - Likely deterministic bug
  - Should block PR merge
  - Fix immediately
  
- **Intermittent (passes sometimes):**
  - Medium priority
  - Flaky test or race condition
  - Tag with `@Flaky` or similar
  - Create issue to investigate later
  - Consider adding retry logic or fixing test isolation

**Does it fail locally when you run it?**
- **YES → Reproducible**
  - Debug locally with IDE
  - Add breakpoints in test and production code
  - Fix and verify
  
- **NO → Environment-specific**
  - Check CI environment differences (Java version, OS, resources)
  - Look for timing issues, parallelism problems
  - May need to add debug logging to CI
  - Consider if test depends on external state

**How many tests are failing?**
- **Single test:**
  - Specific test or method bug
  - Fix that test or code
  
- **All tests in a class:**
  - Shared setup/teardown issue
  - Check `@BeforeEach`, `@AfterEach`, `@BeforeAll`
  
- **All tests in service:**
  - Spring context issue
  - Database/infrastructure problem
  - Configuration error
  
- **Tests across multiple services:**
  - Shared dependency broke (database, message queue)
  - Infrastructure failure
  - **ESCALATE** to platform team

---

## Investigation Workflows

### Workflow A: Debugging Assertion Failures

**Scenario:** Test expects value `1000.0` but got `0.0`

```bash
# Step 1: Run test locally with verbose logging
mvn clean test -Dtest=CDSTradeServiceTest#shouldCalculateNotional \
  -Dlogging.level.com.creditdefaultswap=DEBUG

# Step 2: Add println debugging to test
@Test
void shouldCalculateNotional() {
    var result = service.calculate(trade);
    System.out.println("Result notional: " + result.getNotional());  // ← Add this
    assertEquals(1000.0, result.getNotional());
}

# Step 3: Check recent git changes affecting calculation
git log --oneline --since="3 days ago" -- backend/src/main/java/**/*Calculation*.java

# Step 4: Review related commits
git show <commit-hash>

# Step 5: Reproduce in debugger
# Set breakpoint at assertion, step through calculation logic

# Step 6: Fix production code or update test expectation
# If calculation correct and test wrong: update expected value
# If calculation wrong: fix production code

# Step 7: Verify fix
mvn clean test -Dtest=CDSTradeServiceTest#shouldCalculateNotional

# Step 8: Run full test suite to check for side effects
mvn clean test
```

---

### Workflow B: Resolving Spring Context Failures

**Scenario:** `Failed to load ApplicationContext` error

```bash
# Step 1: Check test configuration
cat src/test/resources/application-test.yml

# Step 2: Verify test has proper annotations
grep -A 5 "class.*Test" src/test/java/**/*Test.java | grep -E "@SpringBootTest|@ActiveProfiles"

# Step 3: Check if Testcontainers starting
docker ps  # Should show postgres, redis, etc.

# Step 4: Review Spring logs in Allure report attachments
# Download artifact → Open report → Failed test → Attachments → spring.log

# Step 5: Check for missing bean definitions
grep -r "@Bean" src/main/java/com/creditdefaultswap/backend/config/

# Step 6: Validate property placeholders resolved
# Look for: ${spring.datasource.url} in error
# Add to application-test.yml:
spring:
  datasource:
    url: jdbc:tc:postgresql:15:///testdb

# Step 7: Restart test
mvn clean test

# Step 8: If still failing, check Docker
docker logs <container-id>
```

---

### Workflow C: Investigating Flaky Tests

**Scenario:** Test passes 80% of time, fails intermittently

```bash
# Step 1: Run test 10 times to measure flakiness
for i in {1..10}; do 
  mvn clean test -Dtest=CDSTradeServiceTest#shouldProcessAsync 
  echo "Run $i: $?"
done

# Step 2: Check for timing dependencies
grep -E "sleep|wait|Thread|CompletableFuture|@Async" \
  src/test/java/**/*Test.java \
  src/main/java/**/*.java

# Step 3: Look for shared mutable state
grep -E "static.*=|@BeforeAll" src/test/java/**/*Test.java

# Step 4: Check test execution order dependency
# Run tests in different orders
mvn test -Dtest=CDSTradeServiceTest#testA,testB
mvn test -Dtest=CDSTradeServiceTest#testB,testA

# Step 5: Add Awaitility for async operations
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>

// Replace Thread.sleep(1000)
await().atMost(Duration.ofSeconds(5))
    .until(() -> service.isComplete(), equalTo(true));

# Step 6: Use @RepeatedTest to verify stability
@RepeatedTest(10)
void shouldProcessAsync() {
    // test body
}

# Step 7: Consider tagging as flaky
@Tag("flaky")
@Test
void shouldProcessAsync() {
    // test body
}
```

---

## Retrieving Additional Context

### Logs

**From Allure Report:**
1. Open failed test in Allure
2. Navigate to "Attachments" section
3. Look for:
   - `test-output.log` - Test execution logs
   - `spring-context.log` - Spring startup/shutdown logs
   - `application.log` - Application logs during test

**From CI Artifacts (Future):**
```bash
# Download full logs archive
curl -O https://ci.example.com/builds/12345/artifacts/logs.tar.gz
tar -xzf logs.tar.gz

# Search for specific error
grep -r "ERROR" logs/backend/
grep -r "Exception" logs/backend/ | head -20
```

### Database State

**For Testcontainers:**
```bash
# Find container
docker ps | grep postgres

# Connect to test database
docker exec -it <container-id> psql -U test -d testdb

# Query relevant tables
SELECT * FROM cds_trades WHERE trade_reference = 'TRD-001';
SELECT * FROM lifecycle_events WHERE trade_id = 123 ORDER BY event_date DESC;

# Check constraints
\d+ cds_trades
```

**For shared test database:**
```bash
# Connect using credentials from application-test.yml
psql -h localhost -U testuser -d testdb

# Same queries as above
```

### Stack Traces

**Reading Stack Traces in Allure:**

Example trace:
```
org.opentest4j.AssertionFailedError: expected: <ACTIVE> but was: <TERMINATED>
  at CDSTradeServiceTest.shouldActivateTrade(CDSTradeServiceTest.java:89)  ← Test line
  at java.base/java.lang.reflect.Method.invoke(Method.java:580)
  [... JUnit internals ...]
```

**Key information:**
- **Exception type:** `AssertionFailedError` (assertion), `NullPointerException` (null), etc.
- **Message:** What failed and why
- **First line with project package:** Where in YOUR code the error occurred
- **Test line number:** Exact assertion that failed

**Action:** Click line number in Allure to see code context (if code attached).

---

## Escalation Paths

### Level 1: Self-Service (< 1 hour)
- **Scope:** Single test failure, clear error message
- **Actions:**
  1. Review Allure report and attachments
  2. Reproduce locally
  3. Debug and fix
  4. Verify fix
  5. Push changes
- **Next Step:** If not resolved, escalate to Level 2

### Level 2: Team Help (1-4 hours)
- **Scope:** Multiple test failures, unclear cause, or flaky tests
- **Actions:**
  1. Post in team chat with:
     - Link to Allure report
     - Error message summary
     - Steps taken so far
     - Ask: "Has anyone seen this before?"
  2. Pair with teammate to investigate
  3. Check team playbooks for similar issues
- **Next Step:** If not resolved, escalate to Level 3

### Level 3: Cross-Team (4-24 hours)
- **Scope:** All tests failing, infrastructure issues, or data corruption
- **Actions:**
  1. Create incident ticket with:
     - Service affected
     - Error signatures
     - Impact (how many tests/builds failing)
     - Timeline (when it started)
     - Attempted fixes
  2. Tag platform/infra team
  3. Check status pages for known issues
  4. Join incident channel
- **Next Step:** Follow incident response process

### Level 4: Urgent Escalation (< 1 hour to respond)
- **Scope:** Production-blocking, security issues, or data loss risk
- **Actions:**
  1. Notify on-call engineer immediately
  2. Create P0/P1 incident
  3. Join war room
  4. Follow runbooks for critical failures

### Escalation Templates

**Slack Message Template:**
```
🔴 Test Failure Need Help

**Service:** backend / gateway / risk-engine
**Test:** CDSTradeServiceTest#shouldCalculateLoss
**Error:** AssertionFailedError: expected 1000.0 but was 0.0
**Allure Report:** [link]
**Tried:**
  - Ran locally (passes)
  - Checked recent commits
  - Added debug logging
**Question:** Has the calculation logic changed recently? Or is this a test data issue?
```

**Issue Ticket Template:**
```markdown
## Test Failure: [Test Name]

**Priority:** Medium / High / Critical
**Service:** backend / gateway / risk-engine
**Test Class:** CDSTradeServiceTest
**Test Method:** shouldCalculateLoss
**First Failure:** 2025-11-14 15:30 UTC
**Frequency:** Every run / 50% / Rare

### Error Summary
\`\`\`
AssertionFailedError: expected: <1000.0> but was: <0.0>
  at CDSTradeServiceTest.shouldCalculateLoss(CDSTradeServiceTest.java:89)
\`\`\`

### Allure Report
[Link to report]

### Recent Changes
- Commit abc123: "Refactor loss calculation" by @username

### Investigation Steps Taken
1. Ran test locally - PASSED
2. Reviewed calculation logic - no obvious bugs
3. Checked test data - looks correct

### Additional Context
- Only fails in CI
- Started after merge of PR #456
- Related to Epic 7: Pricing & Risk

### Next Steps
- [ ] Reproduce in CI environment
- [ ] Add more debug logging
- [ ] Compare local vs CI test data

### Assignee
@team-backend
```

---

## Preventive Measures

### Code Review Checklist

**Before Approving PR:**
- [ ] All tests pass in CI ✓
- [ ] Allure report shows no new failures ✓
- [ ] New tests added for new features ✓
- [ ] Tests have clear names (`shouldDoSomethingWhenCondition`) ✓
- [ ] No `Thread.sleep()` used; use Awaitility instead ✓
- [ ] Mocks properly configured with realistic data ✓
- [ ] Test data uses unique identifiers (UUIDs, method names) ✓
- [ ] No hardcoded dates/times; use `Clock` abstraction ✓
- [ ] Database changes have corresponding Flyway migrations ✓
- [ ] Integration tests use Testcontainers, not shared database ✓

### Test Quality Guidelines

**Writing Robust Tests:**

```java
// ❌ BAD: Flaky, timing-dependent
@Test
void shouldProcessAsync() {
    service.processAsync(trade);
    Thread.sleep(1000);  // ← Bad: may not be enough time
    assertTrue(service.isComplete());
}

// ✅ GOOD: Uses Awaitility
@Test
void shouldProcessAsync() {
    service.processAsync(trade);
    await().atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> assertTrue(service.isComplete()));
}

// ❌ BAD: Shared mutable state
static int counter = 0;  // ← Bad: tests affect each other

@Test
void testA() { counter++; }

@Test
void testB() { assertEquals(0, counter); }  // ← Fails if testA runs first

// ✅ GOOD: Isolated state
private int counter;  // ← Instance variable

@BeforeEach
void setup() { counter = 0; }

// ❌ BAD: Hardcoded IDs
@Test
void shouldCreateTrade() {
    var trade = new Trade("TRD-001", ...);  // ← Bad: collision risk
    repository.save(trade);
}

// ✅ GOOD: Unique IDs
@Test
void shouldCreateTrade() {
    var tradeRef = "TRD-" + UUID.randomUUID();
    var trade = new Trade(tradeRef, ...);
    repository.save(trade);
}

// ❌ BAD: Unclear test name
@Test
void test1() { ... }

// ✅ GOOD: Descriptive name
@Test
void shouldCalculateNotionalWhenCurrencyIsUSD() { ... }
```

### Monitoring and Alerts

**Test Health Metrics to Track:**
- **Pass Rate:** Should be > 95%
- **Flaky Test Count:** Should decrease over time
- **Test Duration:** Should not increase significantly
- **Coverage:** Should not decrease with new code

**Setting Up Alerts (Future):**
```yaml
# Example GitHub Actions workflow
- name: Check Test Health
  if: always()
  run: |
    PASS_RATE=$(calculate_pass_rate)
    if [ $PASS_RATE -lt 95 ]; then
      echo "::warning::Pass rate dropped below 95%: $PASS_RATE"
    fi
```

---

## Appendices

### A. Useful Maven Commands

```bash
# Run specific test
mvn test -Dtest=ClassName#methodName

# Run all tests in a class
mvn test -Dtest=ClassName

# Run tests matching pattern
mvn test -Dtest=*ServiceTest

# Run with debug logging
mvn test -Dlogging.level.root=DEBUG -Dlogging.level.com.creditdefaultswap=TRACE

# Run tests and generate Allure report
mvn clean test allure:serve

# Skip tests (emergency only)
mvn clean install -DskipTests

# Run tests in parallel (if configured)
mvn test -T 4  # Use 4 threads
```

### B. Useful Git Commands for Investigation

```bash
# Find when test started failing
git bisect start
git bisect bad  # Current commit (test fails)
git bisect good <commit-hash>  # Last known good commit

# Show changes to specific file
git log -p -- backend/src/main/java/**/Calculator.java

# Find who changed a line
git blame backend/src/test/java/**/*Test.java -L 89,89

# Show all changes in last 3 days
git log --oneline --since="3 days ago" -- backend/

# Compare two branches
git diff main feature-branch -- backend/src/main/java/
```

### C. Docker/Testcontainers Commands

```bash
# List running containers
docker ps

# View container logs
docker logs <container-id>
docker logs -f <container-id>  # Follow logs

# Connect to Postgres container
docker exec -it <container-id> psql -U test -d testdb

# Stop all Testcontainers
docker stop $(docker ps -q --filter "label=org.testcontainers=true")

# Clean up orphaned containers
docker container prune -f

# Check Docker resource usage
docker stats
```

### D. Allure Annotations Reference

```java
// Allure Feature/Story annotations (at class level)
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Backend Service")
@Story("CDS Trade Creation")
class CDSTradeServiceTest {
    // Tests
}

// Additional Allure annotations (optional)
@Epic("Trade Management")
@Severity(SeverityLevel.CRITICAL)
@Description("Verifies that a new CDS trade can be created with valid data")
@Link(name = "Jira Ticket", url = "https://jira.example.com/TICKET-123")

// Step-by-step test flow
@Step("Create trade with reference {0}")
public Trade createTrade(String reference) { ... }

@Step("Verify trade is in ACTIVE status")
public void verifyActive(Trade trade) { ... }

// Attach files/data
@Attachment(value = "Trade JSON", type = "application/json")
public String attachTradeData(Trade trade) {
    return objectMapper.writeValueAsString(trade);
}
```

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-11-14 | Engineering Team | Initial playbook for Story 3.5 |

---

## Feedback

Have suggestions to improve this playbook? Submit a PR or create an issue in the repository.

**Related Documentation:**
- [Backend Allure Setup Guide](backend-allure-setup.md)
- [Test Architecture](test-architecture.md)
- [Golden Path Testing Documentation](golden-path-testing.md)
