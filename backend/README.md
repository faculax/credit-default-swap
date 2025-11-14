# CDS Platform - Backend Service

Core backend service for the Credit Default Swap platform, providing trade management, lifecycle processing, and business logic.

## Quick Start

### Build and Run Tests

```bash
# Run all tests with Allure reporting
mvn clean test

# Run and view HTML report in browser
mvn clean test allure:serve
```

### Test Categories

```bash
# Unit tests only
mvn clean test -Punit-tests allure:serve

# Integration tests only
mvn clean test -Pintegration-tests allure:serve

# All tests
mvn clean test -Pall-tests allure:serve
```

### View Test Results

After running tests, Allure results are generated in `target/allure-results/`.

**Generate and view HTML report:**
```bash
# Start local server with report
mvn allure:serve

# Or generate report without server
mvn allure:report
# Then open: target/allure-report/index.html
```

## Testing Documentation

For comprehensive testing and Allure configuration documentation, see:
- **[Backend Allure Setup Guide](../docs/testing/backend-allure-setup.md)** - Complete Allure integration guide
- **[Test Architecture](../docs/testing/test-architecture.md)** - Testing standards and patterns

## Development

### Prerequisites
- Java 17+
- Maven 3.8+

### Build
```bash
mvn clean install
```

### Run Locally
```bash
mvn spring-boot:run
```

## Service Information

- **Port:** 8080
- **Context Path:** `/api/v1`
- **Health Check:** `http://localhost:8080/actuator/health`
