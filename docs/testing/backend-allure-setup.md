# Backend Allure Integration Guide

**Epic 03: Backend Allure Integration**  
**Story 3.1: Allure Dependencies & Plugins for Spring Boot Services**  
**Version:** 1.0  
**Last Updated:** November 14, 2025

This document describes the Allure reporting integration for the CDS Platform backend services.

---

## Overview

All backend Spring Boot services are configured with Allure JUnit 5 adapters to automatically generate rich test result artifacts during test execution. These results can be aggregated and rendered into comprehensive HTML reports showing test status, trends, and metadata.

---

## Allure Dependencies

### Current Versions

| Dependency | Version | Scope |
|------------|---------|-------|
| `io.qameta.allure:allure-junit5` | 2.25.0 | test |
| `io.qameta.allure:allure-maven` | 2.12.0 | plugin |

### Dependencies in pom.xml

```xml
<dependencies>
    <!-- Allure JUnit 5 Integration -->
    <dependency>
        <groupId>io.qameta.allure</groupId>
        <artifactId>allure-junit5</artifactId>
        <version>2.25.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Maven Plugin Configuration

### Allure Maven Plugin

The `allure-maven` plugin is configured in the `build/plugins` section:

```xml
<plugin>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-maven</artifactId>
    <version>2.12.0</version>
    <configuration>
        <reportVersion>2.25.0</reportVersion>
        <resultsDirectory>${project.build.directory}/allure-results</resultsDirectory>
        <reportDirectory>${project.build.directory}/allure-report</reportDirectory>
    </configuration>
</plugin>
```

### Maven Surefire Configuration

The Maven Surefire plugin is configured to automatically use the Allure JUnit 5 listener:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <!-- Test pattern configuration omitted for brevity -->
        
        <!-- Allure properties for test results -->
        <properties>
            <property>
                <name>listener</name>
                <value>io.qameta.allure.junit5.AllureJunit5</value>
            </property>
        </properties>
        <systemPropertyVariables>
            <allure.results.directory>${project.build.directory}/allure-results</allure.results.directory>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

---

## Directory Structure

### Allure Results Location

When tests run, Allure result files are generated in:

```
backend/
├── target/
│   ├── allure-results/          # Raw JSON result files
│   │   ├── *-result.json        # Test case results
│   │   ├── *-container.json     # Test suite containers
│   │   └── environment.properties (optional)
│   └── allure-report/           # Generated HTML report (after mvn allure:report)
│       ├── index.html
│       ├── data/
│       ├── history/
│       └── ...
```

### Result File Format

Allure generates JSON files for each test execution:

**Result JSON Structure:**
```json
{
  "uuid": "...",
  "historyId": "...",
  "testCaseId": "[engine:junit-jupiter]/[class:...]/[method:...]",
  "testCaseName": "testMethod()",
  "fullName": "com.creditdefaultswap.unit.platform.service.TestClass.testMethod",
  "labels": [
    {"name": "framework", "value": "junit-platform"},
    {"name": "language", "value": "java"},
    {"name": "package", "value": "..."},
    {"name": "testClass", "value": "..."},
    {"name": "testMethod", "value": "..."}
  ],
  "status": "passed|failed|broken|skipped",
  "stage": "finished",
  "start": 1763146562941,
  "stop": 1763146562945
}
```

---

## Usage

### Running Tests with Allure

All standard Maven test commands automatically generate Allure results:

```bash
# Unit tests only (generates allure-results/)
mvn test -Punit-tests

# Integration tests (generates allure-results/)
mvn test -Pintegration-tests

# All tests
mvn test -Pall-tests

# Single test class
mvn test -Dtest=CDSTradeServiceTest
```

After running tests, check the results directory:

```bash
ls target/allure-results/
# Should show:
# - Multiple *-result.json files (one per test method)
# - Multiple *-container.json files (one per test class)
```

### Generating HTML Reports Locally

To generate and view the Allure HTML report locally:

```bash
# Generate the HTML report from results
mvn allure:report

# Serve the report on http://localhost:randomPort
mvn allure:serve
```

The `allure:serve` command will:
1. Generate the HTML report from `target/allure-results/`
2. Start a local web server
3. Open your browser to view the report

### Report Features

The Allure HTML report provides:

- **Overview Dashboard**: Total tests, pass rate, duration trends
- **Test Suites**: Organized by package and class
- **Test Details**: Individual test results with timing and metadata
- **Categories**: Test failures grouped by type
- **Timeline**: Test execution timing visualization
- **History**: Trend analysis across multiple runs (when history is preserved)
- **Graphs**: Pass/fail pie charts, duration histograms

---

## Test Annotations & Metadata

### Basic @StoryId Usage

All tests should use the `@StoryId` annotation for traceability:

```java
import com.creditdefaultswap.unit.platform.testing.story.StoryId;

@Test
@StoryId(value = "UTS-3.1", testType = StoryId.TestType.UNIT, 
         microservice = "cds-platform")
void shouldCreateTrade() {
    // Test implementation
}
```

The @StoryId annotation automatically adds Allure labels for:
- Story ID filtering
- Test type categorization
- Microservice attribution

### Additional Allure Annotations (Optional)

You can add more metadata using Allure annotations:

```java
import io.qameta.allure.*;

@Epic("Trade Management")
@Feature("CDS Trade Creation")
@Story("User creates a new CDS trade")
@Severity(SeverityLevel.CRITICAL)
@Test
void shouldCreateTrade() {
    // Test implementation
}
```

---

## Troubleshooting

### Issue: No allure-results directory created

**Symptoms:** After running tests, `target/allure-results/` does not exist

**Causes & Solutions:**
1. **Surefire configuration missing:**
   - Check that `pom.xml` includes the Allure listener configuration
   - Verify `allure.results.directory` system property is set

2. **Tests didn't run:**
   - Ensure tests actually executed (check Maven output)
   - Verify test file naming matches Surefire patterns

### Issue: Empty allure-results directory

**Symptoms:** Directory exists but contains no JSON files

**Causes & Solutions:**
1. **Allure dependency missing:**
   - Check `pom.xml` has `allure-junit5` dependency
   - Run `mvn dependency:tree | grep allure` to verify

2. **JUnit 5 extension not loaded:**
   - Ensure test classes don't manually exclude Allure extension
   - Check for conflicting test listeners

### Issue: Allure report generation fails

**Symptoms:** `mvn allure:report` fails or produces empty report

**Causes & Solutions:**
1. **No results to process:**
   - Run tests first: `mvn test`
   - Check `target/allure-results/` has JSON files

2. **Plugin version mismatch:**
   - Ensure `allure-maven` plugin version is compatible
   - Current: 2.12.0 (compatible with Allure 2.25.0)

3. **Corrupted results:**
   - Clean and regenerate: `mvn clean test allure:report`

### Issue: Tests pass but report shows "broken"

**Symptoms:** Test passes in Maven output but Allure shows "broken" status

**Causes & Solutions:**
1. **Exception in setup/teardown:**
   - Check `@BeforeEach`/`@AfterEach` methods
   - Allure marks tests as "broken" if infrastructure fails

2. **Spring context issues:**
   - For integration tests, verify `@SpringBootTest` configuration
   - Check application context loads successfully

---

## Version Upgrade Process

### Upgrading Allure JUnit 5

1. **Check compatibility:**
   - Visit [Allure releases](https://github.com/allure-framework/allure-java/releases)
   - Verify compatibility with JUnit 5.10.x

2. **Update pom.xml:**
   ```xml
   <dependency>
       <groupId>io.qameta.allure</groupId>
       <artifactId>allure-junit5</artifactId>
       <version>NEW_VERSION</version>
       <scope>test</scope>
   </dependency>
   ```

3. **Update plugin:**
   ```xml
   <plugin>
       <groupId>io.qameta.allure</groupId>
       <artifactId>allure-maven</artifactId>
       <version>NEW_PLUGIN_VERSION</version>
       <configuration>
           <reportVersion>NEW_VERSION</reportVersion>
           ...
       </configuration>
   </plugin>
   ```

4. **Test:**
   ```bash
   mvn clean test allure:report
   ```

5. **Verify:**
   - Check all tests still pass
   - Confirm result files generate correctly
   - Review HTML report for rendering issues

---

## CI/CD Integration

### GitHub Actions Example

```yaml
- name: Run Backend Tests
  run: |
    cd backend
    mvn test -Punit-tests

- name: Upload Allure Results
  uses: actions/upload-artifact@v3
  if: always()
  with:
    name: allure-results-backend
    path: backend/target/allure-results/
    retention-days: 7
```

### Multi-Service Aggregation

When running tests across multiple services:

1. **Collect results:**
   ```bash
   # Run tests in each service
   cd backend && mvn test
   cd ../risk-engine && mvn test
   cd ../gateway && mvn test
   
   # Merge results into single directory
   mkdir -p allure-results
   cp backend/target/allure-results/* allure-results/
   cp risk-engine/target/allure-results/* allure-results/
   cp gateway/target/allure-results/* allure-results/
   ```

2. **Generate unified report:**
   ```bash
   allure generate allure-results --clean -o allure-report
   ```

---

## Additional Resources

- **Allure Documentation:** https://docs.qameta.io/allure/
- **Allure Java:** https://github.com/allure-framework/allure-java
- **Test Architecture:** [`TEST_ARCHITECTURE.md`](./TEST_ARCHITECTURE.md)
- **Golden Path Guide:** [`GOLDEN_PATH.md`](./GOLDEN_PATH.md)
- **Quick Reference:** [`QUICK_REFERENCE.md`](./QUICK_REFERENCE.md)

---

## Summary

✅ **Allure JUnit 5 dependency** (v2.25.0) configured  
✅ **Allure Maven plugin** (v2.12.0) installed  
✅ **Surefire listener** configured to generate results automatically  
✅ **Results directory** set to `target/allure-results/`  
✅ **Report commands** available: `mvn allure:report` and `mvn allure:serve`  
✅ **JSON metadata** includes test status, timing, labels, and traceability

All backend tests now automatically emit Allure-compatible result artifacts ready for aggregation and reporting.
