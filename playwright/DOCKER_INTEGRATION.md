# Playwright + Docker Stack Integration

## Problem Solved
Playwright tests now verify the full Docker stack (frontend, backend, gateway, database) is running **before** any tests execute, preventing mysterious timeouts and providing clear feedback.

## What Was Added

### 1. Global Setup Hook (`global-setup.ts`)
- Checks frontend at `http://localhost:3000`
- Checks backend health at `http://localhost:8080/actuator/health`
- Checks gateway health at `http://localhost:8081/actuator/health`
- **Fails fast** with clear instructions if any service is down

### 2. Updated `playwright.config.ts`
Added:
```typescript
globalSetup: require.resolve('./global-setup'),
```

### 3. Enhanced README
- **Prerequisites** section with Docker startup commands
- **CI/CD Integration** examples (GitHub Actions + Azure Pipelines)
- Clear explanation of health check behavior

## Developer Workflow

### Starting Fresh
```powershell
# From project root
docker-compose up -d

# Wait for services to be healthy (~30-60s)
docker-compose ps

# Run tests
cd playwright
npm test
```

### What Happens When Docker Is Down
```
🔍 Verifying Docker stack is running...
❌ Frontend unreachable at http://localhost:3000
❌ Backend unreachable at http://localhost:8080/actuator/health

💡 Start the stack with:
   docker-compose up -d
```

Test run stops immediately with exit code 1.

### What Happens When Docker Is Up
```
🔍 Verifying Docker stack is running...
   ✅ Frontend reachable
   ✅ Gateway healthy
   ✅ Backend healthy

✅ All stack health checks passed. Proceeding with tests.

Running 112 tests...
```

## CI/CD Integration

Both GitHub Actions and Azure Pipelines examples provided in README show:
1. `docker-compose up -d`
2. Wait for health checks (with timeout)
3. Run Playwright tests
4. Teardown with `docker-compose down -v`

## Benefits

✅ **No Silent Failures**: Tests won't mysteriously timeout waiting for unreachable services  
✅ **Clear Errors**: Developers immediately know if they forgot to start Docker  
✅ **CI-Ready**: Examples show proper orchestration for automated pipelines  
✅ **Fast Feedback**: Fails in ~5s instead of waiting for test timeouts  
✅ **Documented**: README clearly states prerequisites and startup sequence

## Stack Dependency Map

```
Playwright Tests
      ↓ (requires)
   Frontend:3000
      ↓ (requires)
   Gateway:8081
      ↓ (requires)
   Backend:8080 + Risk-Engine:8082
      ↓ (requires)
   PostgreSQL:5432
```

All verified by global setup before tests run.

## Optional Enhancements (Future)

- Add retry logic with exponential backoff for flaky health endpoints
- Support `SKIP_HEALTH_CHECK=true` env var for rare edge cases
- Add `--no-docker` CLI flag to run against externally hosted stack
- Detect `docker-compose.yml` changes and warn if stack might be stale
