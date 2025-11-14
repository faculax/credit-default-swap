# API Gateway Service

Spring Cloud Gateway providing routing, load balancing, and cross-cutting concerns for the CDS platform microservices.

## Quick Start

### Build and Run Tests

```bash
# Run all tests with Allure reporting
mvn clean test

# Run and view HTML report in browser
mvn clean test allure:serve
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

- **Port:** 8081
- **Context Path:** `/`
- **Routes:** Configured in `application.yml`
- **Health Check:** `http://localhost:8081/actuator/health`

## Gateway Routes

The gateway routes requests to backend services:
- `/api/v1/**` → Backend Service (port 8080)
- `/risk/**` → Risk Engine Service (port 8082)
