# Frontend Testing with Allure

Complete guide to running tests and generating Allure reports for the CDS Platform frontend.

## 📋 Table of Contents

- [Quick Start](#quick-start)
- [Test Types](#test-types)
- [Running Tests](#running-tests)
- [Generating Reports](#generating-reports)
- [npm Scripts Reference](#npm-scripts-reference)
- [Cross-Platform Support](#cross-platform-support)
- [Troubleshooting](#troubleshooting)

---

## 🚀 Quick Start

```bash
# Install dependencies
npm install

# Run all tests
npm test

# Generate comprehensive Allure report (Jest + Cypress)
npm run test:report:merge
```

---

## 🧪 Test Types

### Unit Tests
- **Location**: `src/__tests__/unit/**/*.test.tsx`
- **Framework**: Jest + React Testing Library
- **Purpose**: Test individual components in isolation
- **Speed**: Fast (~10-50ms per test)

### Integration Tests  
- **Location**: `src/__tests__/integration/**/*.test.tsx`
- **Framework**: Jest + React Testing Library + MSW
- **Purpose**: Test component interactions and API integrations
- **Speed**: Medium (~100-1000ms per test)

### E2E Tests
- **Location**: `cypress/e2e/**/*.cy.ts`
- **Framework**: Cypress
- **Purpose**: Test complete user workflows in real browser
- **Speed**: Slow (~1-10s per test)

---

## 🏃 Running Tests

### Jest Tests (Unit + Integration)

```bash
# Run unit tests only
npm run test:unit

# Run integration tests only
npm run test:integration

# Run all Jest tests (unit + integration)
npm run test:all

# Run tests in watch mode (for development)
npm run test:watch

# Run tests with coverage
npm run test:coverage
```

### Cypress Tests (E2E)

```bash
# Run E2E tests headlessly (CI mode)
npm run test:e2e

# Open Cypress Test Runner (interactive mode)
npm run test:e2e:open

# Run E2E tests headlessly with explicit flag
npm run test:e2e:headless
```

---

## 📊 Generating Reports

### Individual Reports

```bash
# Generate report for unit tests only
npm run test:unit:report

# Generate report for integration tests only  
npm run test:integration:report

# Generate report for E2E tests only
npm run test:e2e:report
```

### Merged Report (Recommended)

```bash
# Run all tests and generate unified report
npm run test:report:merge
```

This command:
1. ✅ Runs all Jest tests (unit + integration)
2. ✅ Runs all Cypress tests (E2E)
3. ✅ Merges results into `allure-results-merged/`
4. ✅ Generates HTML report
5. ✅ Opens report in browser

---

## 📝 npm Scripts Reference

### Test Execution

| Script | Description |
|--------|-------------|
| `npm test` | Run unit tests (default) |
| `npm run test:unit` | Run unit tests only |
| `npm run test:integration` | Run integration tests only |
| `npm run test:all` | Run all Jest tests |
| `npm run test:e2e` | Run Cypress E2E tests |
| `npm run test:e2e:open` | Open Cypress Test Runner |
| `npm run test:watch` | Run tests in watch mode |
| `npm run test:coverage` | Run tests with coverage |

### Allure Reporting

| Script | Description |
|--------|-------------|
| `npm run test:unit:report` | Unit tests + Allure report |
| `npm run test:integration:report` | Integration tests + Allure report |
| `npm run test:e2e:report` | E2E tests + Allure report |
| `npm run test:report:merge` | All tests + merged Allure report |
| `npm run allure:clean` | Clean all Allure artifacts |
| `npm run allure:generate` | Generate report from `allure-results/` |
| `npm run allure:generate:merged` | Generate report from merged results |
| `npm run allure:serve` | Generate and open report |
| `npm run allure:open` | Open existing report |

### Cypress Shortcuts

| Script | Description |
|--------|-------------|
| `npm run cypress:open` | Open Cypress Test Runner |
| `npm run cypress:run` | Run Cypress tests headlessly |

---

## 🌍 Cross-Platform Support

All npm scripts work on **Windows**, **macOS**, and **Linux** thanks to:

- **rimraf**: Cross-platform file deletion (replaces `rm -rf`)
- **cross-env**: Cross-platform environment variables
- **Node.js scripts**: Pure JavaScript for merging logic

### Platform-Specific Notes

**Windows PowerShell/CMD**:
```powershell
npm run test:all
npm run test:report:merge
```

**macOS/Linux Bash**:
```bash
npm run test:all
npm run test:report:merge
```

**Git Bash on Windows**:
```bash
npm run test:all
npm run test:report:merge
```

---

## 🔍 Troubleshooting

### Issue: No Allure Results Generated

**Problem**: Tests run but `allure-results/` is empty.

**Solution**:
```bash
# Clean and retry
npm run allure:clean
npm run test:all
ls allure-results  # Should show *.json files
```

### Issue: Allure CLI Not Found

**Problem**: `'allure' is not recognized as a command`

**Solution**:

**macOS**:
```bash
brew install allure
```

**Windows**:
```powershell
scoop install allure
```

**Linux**:
```bash
wget https://github.com/allure-framework/allure2/releases/download/2.25.0/allure-2.25.0.tgz
tar -zxf allure-2.25.0.tgz
sudo mv allure-2.25.0 /opt/
sudo ln -s /opt/allure-2.25.0/bin/allure /usr/local/bin/allure
```

### Issue: Merge Script Fails

**Problem**: `test:report:merge` fails with "No results found"

**Solution**:
```bash
# Run tests separately first
npm run test:all       # Should generate allure-results/
npm run test:e2e       # Should generate cypress/allure-results/

# Verify results exist
ls allure-results
ls cypress/allure-results

# Then merge
node scripts/merge-allure-results.js
```

### Issue: Cypress Binary Not Found

**Problem**: `Cypress binary not found` error

**Solution**:
```bash
# Reinstall Cypress
npm install cypress --force

# Verify installation
npx cypress verify
```

### Issue: Port Already in Use

**Problem**: E2E tests fail with "Port 3000 already in use"

**Solution**:
```bash
# Stop the development server
# Kill process on port 3000 (adjust for your OS)

# Windows
netstat -ano | findstr :3000
taskkill /PID <PID> /F

# macOS/Linux
lsof -ti:3000 | xargs kill -9
```

---

## 📚 Additional Resources

- [Frontend Allure Setup Guide](../../docs/testing/frontend-allure-setup.md)
- [Test Helpers Documentation](./src/utils/testHelpers.ts)
- [Cypress Documentation](https://docs.cypress.io/)
- [Allure Report Documentation](https://docs.qameta.io/allure/)

---

## 🎯 Best Practices

1. **Always clean before critical runs**:
   ```bash
   npm run allure:clean && npm run test:report:merge
   ```

2. **Use merged reports for comprehensive view**:
   - Combines all test types (unit, integration, E2E)
   - Shows complete story-to-test traceability
   - Includes screenshots and videos from E2E tests

3. **Tag tests with story metadata**:
   ```typescript
   withStoryId({ 
     storyId: 'UTS-2.2', 
     testType: 'unit', 
     severity: 'critical' 
   })('should render correctly', () => { /* ... */ });
   ```

4. **Run tests before committing**:
   ```bash
   npm run test:all  # Fast feedback
   npm run test:e2e  # Before PR submission
   ```

---

**Epic 04: Frontend Allure Integration** | **Story 4.4: Harmonized npm Scripts**
