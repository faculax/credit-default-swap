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

## Service Configuration Status

All three backend services now have standardized Allure JUnit 5 integration:

### Backend Service (CDS Platform)
- ✅ `allure-junit5` dependency (version `2.25.0`)
- ✅ `maven-surefire-plugin` configured with Allure listener
- ✅ `allure-maven` plugin (version `2.12.0`) for report generation
- ✅ `maven-clean-plugin` configured to remove stale results
- ✅ Allure results directory: `target/allure-results`
- ✅ Tests: 53/53 passing with Allure reporting

### Gateway Service
- ✅ `allure-junit5` dependency (version `2.25.0`)
- ✅ `maven-surefire-plugin` configured with Allure listener
- ✅ `allure-maven` plugin (version `2.12.0`) for report generation
- ✅ `maven-clean-plugin` configured to remove stale results
- ✅ Allure results directory: `target/allure-results`
- ✅ Tests: 1/1 passing with Allure reporting

### Risk-Engine Service
- ✅ `allure-junit5` dependency (version `2.25.0`)
- ✅ `maven-surefire-plugin` configured with Allure listener
- ✅ `allure-maven` plugin (version `2.12.0`) for report generation
- ✅ `maven-clean-plugin` configured to remove stale results
- ✅ Allure results directory: `target/allure-results`
- ⚠️ Allure integration working (10 tests passing), but service has pre-existing test failures (7 failures/errors unrelated to Allure)

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
# Unix/macOS/Linux
ls target/allure-results/

# Windows PowerShell
dir target\allure-results\

# Should show:
# - Multiple *-result.json files (one per test method)
# - Multiple *-container.json files (one per test class)
```

### Generating HTML Reports Locally

#### Quick Start - Generate and View

The fastest way to view test results locally:

```bash
# Run tests and immediately view report in browser
mvn clean test allure:serve

# For specific test profiles:
mvn clean test -Punit-tests allure:serve
mvn clean test -Pintegration-tests allure:serve
```

The `allure:serve` command will:
1. Clean previous artifacts (via `mvn clean`)
2. Run tests and generate results in `target/allure-results/`
3. Build HTML report from results
4. Start local web server on random port (e.g., `http://localhost:58234`)
5. Automatically open browser to view report

#### Generate Report Without Server

To generate the HTML report without starting a server:

```bash
# Generate report to target/allure-report/
mvn allure:report

# View the report by opening in browser
# Unix/macOS/Linux:
open target/allure-report/index.html

# Windows:
start target\allure-report\index.html

# Or navigate manually to:
# file:///path/to/project/target/allure-report/index.html
```

#### Selective Test Execution

Run specific test categories while generating Allure results:

```bash
# Unit tests only
mvn clean test -Punit-tests allure:serve

# Integration tests only  
mvn clean test -Pintegration-tests allure:serve

# Single test class
mvn clean test -Dtest=CDSTradeServiceTest allure:serve

# Multiple test classes
mvn clean test -Dtest=CDSTradeServiceTest,TradeValidationServiceTest allure:serve

# Tests matching pattern
mvn clean test -Dtest=*ServiceTest allure:serve
```

#### Cross-Platform Commands

**Windows (PowerShell):**
```powershell
# Clean, test, and serve report
mvn clean test allure:serve

# Or use the wrapper
.\mvnw.cmd clean test allure:serve
```

**Windows (Command Prompt):**
```cmd
mvn clean test allure:serve

rem Or use the wrapper
mvnw.cmd clean test allure:serve
```

**macOS/Linux (bash/zsh):**
```bash
# Clean, test, and serve report
mvn clean test allure:serve

# Or use the wrapper
./mvnw clean test allure:serve
```

#### Cleaning Stale Artifacts

The `maven-clean-plugin` is configured to automatically remove `target/allure-results/` when running `mvn clean`:

```bash
# Remove all build artifacts including allure-results
mvn clean

# Verify cleanup (should return no files)
ls target/allure-results/  # Should show "No such file or directory"
```

Always use `mvn clean test` when you want fresh results to avoid stale artifacts mixing with new test runs.

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

### Basic @Feature and @Story Usage

All test classes should use `@Feature` and `@Story` annotations at the class level for Allure Behaviors grouping:

```java
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

@Feature("Backend Service")
@Story("CDS Trade Creation")
class CDSTradeServiceTest {
    
    @Test
    void shouldCreateTrade() {
        // Test implementation
    }
}
```

The `@Feature` and `@Story` annotations create the Behaviors view hierarchy in Allure reports:
- **@Feature**: Service or major feature area (e.g., "Backend Service", "Gateway Service")
- **@Story**: Specific story or scenario being tested (e.g., "CDS Trade Creation", "Credit Event Processing")

### Additional Allure Annotations (Optional)

You can add more metadata using Allure annotations:

```java
import io.qameta.allure.*;

@Epic("Trade Management")
@Feature("Backend Service")
@Story("CDS Trade Creation")
@Severity(SeverityLevel.CRITICAL)
class CDSTradeServiceTest {
    
    @Test
    void shouldCreateTrade() {
        // Test implementation
    }
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

### Issue: `mvn allure:serve` fails with "command not found"

**Symptoms:** Error message: `Allure commandline is not installed` or similar

**Causes & Solutions:**
1. **Allure CLI not installed (automatic download should work):**
   - The `allure-maven` plugin automatically downloads Allure CLI
   - If it fails, check network connectivity
   - Verify Maven can access Maven Central repository

2. **Manual installation (if automatic fails):**
   ```bash
   # macOS (using Homebrew)
   brew install allure
   
   # Windows (using Scoop)
   scoop install allure
   
   # Or download from: https://github.com/allure-framework/allure2/releases
   ```

3. **Verify installation:**
   ```bash
   allure --version
   # Should show: 2.25.0 or compatible version
   ```

### Issue: Browser doesn't open with `mvn allure:serve`

**Symptoms:** Report is served but browser doesn't automatically open

**Causes & Solutions:**
1. **Check terminal output:**
   - Look for URL like `http://localhost:58234`
   - Manually open this URL in browser

2. **Desktop environment issues:**
   - On Linux servers without GUI, you cannot auto-open
   - Copy the URL and paste in browser on your local machine
   - Or use `mvn allure:report` and manually open `target/allure-report/index.html`

3. **Port already in use:**
   - Allure uses random ports to avoid conflicts
   - If it still fails, check firewall settings
   - Try stopping other local servers

### Issue: Tests pass but report shows "broken"

**Symptoms:** Test passes in Maven output but Allure shows "broken" status

**Causes & Solutions:**
1. **Exception in setup/teardown:**
   - Check `@BeforeEach`/`@AfterEach` methods
   - Allure marks tests as "broken" if infrastructure fails

2. **Spring context issues:**
   - For integration tests, verify `@SpringBootTest` configuration
   - Check application context loads successfully

### Issue: Stale test results mixing with new runs

**Symptoms:** Old test results appear in new reports

**Causes & Solutions:**
1. **Always clean before running tests:**
   ```bash
   # Good: Fresh results
   mvn clean test allure:serve
   
   # Bad: May have stale artifacts
   mvn test allure:serve
   ```

2. **Manual cleanup if needed:**
   ```bash
   # Remove allure-results manually
   rm -rf target/allure-results/   # Unix/macOS
   Remove-Item -Recurse target\allure-results\  # Windows PowerShell
   ```

### Issue: Cross-platform path issues

**Symptoms:** Tests fail or report generation fails on different OS

**Causes & Solutions:**
1. **Use Maven properties for paths:**
   - `${project.build.directory}` resolves to `target/` on all platforms
   - Already configured in `pom.xml` - no changes needed

2. **Avoid hardcoded paths in tests:**
   ```java
   // Bad: Windows-specific
   Path path = Paths.get("C:\\data\\file.txt");
   
   // Good: Cross-platform
   Path path = Paths.get("src", "test", "resources", "file.txt");
   ```

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
