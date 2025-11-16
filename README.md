# 💳 CDS Platform

> **Credit Default Swap Trading & Risk Management Platform**

A comprehensive platform for managing credit default swap (CDS) instruments, supporting single-name CDS, index CDS, and complex credit derivatives with integrated risk analytics and lifecycle management.

---

## 🏗️ Architecture

**Microservices Stack:**
- **Backend Service** - Core business logic, trade capture, lifecycle events
- **Gateway Service** - API Gateway, routing, authentication
- **Risk Engine** - Risk calculations, pricing, margin analytics (with ORE integration)
- **Frontend** - React + TypeScript UI for trading and portfolio management

**Technology Stack:**
- **Backend**: Spring Boot 3.x, Java 17, PostgreSQL
- **Frontend**: React 18, TypeScript, TailwindCSS
- **Risk Analytics**: QuantLib ORE (Open Risk Engine)
- **Infrastructure**: Docker Compose, GitHub Actions CI/CD
- **Testing**: JUnit 5, Jest, Cypress, Allure Framework

---

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Node.js 18+
- JDK 17+
- Maven 3.8+

### Run All Services

```bash
# Start all services (backend, gateway, risk-engine, frontend, databases)
docker-compose up --build

# Access applications:
# - Frontend: http://localhost:3000
# - Backend API: http://localhost:8080
# - Gateway: http://localhost:8090
# - Risk Engine: http://localhost:8081
```

### Run Individual Services

**Backend:**
```bash
cd backend
./mvnw spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm install
npm start
```

**Risk Engine:**
```bash
cd risk-engine
./mvnw spring-boot:run
```

---

## 🧪 Testing & Quality

### Test Reports

📊 **[View Latest Test Report](https://[your-org].github.io/[your-repo]/)**

Our unified test reporting system combines results from:
- ✅ Backend Service tests (JUnit 5)
- ✅ Gateway Service tests (JUnit 5)
- ✅ Risk Engine tests (JUnit 5)
- ✅ Frontend Unit tests (Jest)
- ✅ Frontend Integration tests (Jest)
- ✅ Frontend E2E tests (Cypress)

**Key Features:**
- **Story Traceability** - Every test linked to user story ID
- **Historical Trends** - 20-build pass rate tracking
- **Service Filtering** - Filter by backend/frontend/service
- **Comprehensive Metadata** - Build numbers, commit SHAs, timestamps

### Running Tests Locally

**Backend (all services):**
```bash
cd backend  # or gateway, or risk-engine
./mvnw clean test

# Generate Allure report
allure generate target/allure-results -o target/allure-report
allure open target/allure-report
```

**Frontend:**
```bash
cd frontend

# Run all tests with merged report
npm run test:all
npm run allure:open

# Or run individual test types:
npm run test:unit          # Unit tests
npm run test:integration   # Integration tests
npm run test:e2e           # E2E tests (Cypress)

# Generate individual reports:
npm run test:unit:report
npm run test:integration:report
npm run test:e2e:report
```

### Documentation

- 📖 **[Test Reports Guide](./docs/TESTING_REPORTS.md)** - Comprehensive testing documentation
- 📋 **[Testing PRD](./unified-testing-stories/TestingPRD.md)** - Testing strategy and roadmap
- 🎯 **[Frontend Testing Guide](./frontend/TESTING.md)** - Frontend-specific test setup
- 🔧 **[CI Workflows](./.github/workflows/)** - GitHub Actions configuration

---

## 📁 Project Structure

```
credit-default-swap/
├── backend/                 # Core backend service (Spring Boot)
│   ├── src/main/java/      # Application code
│   └── src/test/java/      # JUnit 5 tests
├── gateway/                 # API Gateway service (Spring Boot)
│   └── src/test/java/      # Gateway tests
├── risk-engine/             # Risk calculation engine (Spring Boot + ORE)
│   ├── ore-resources/      # QuantLib ORE configuration
│   └── src/test/java/      # Risk engine tests
├── frontend/                # React + TypeScript UI
│   ├── src/                # React components
│   ├── cypress/            # E2E tests
│   └── src/__tests__/      # Jest unit/integration tests
├── docs/                    # Documentation
│   ├── TESTING_REPORTS.md  # Testing guide
│   ├── schema/             # Database schema docs
│   └── reporting/          # Test report assets
├── .github/workflows/       # CI/CD pipelines
│   ├── backend-tests.yml   # Backend CI
│   ├── frontend-tests.yml  # Frontend CI
│   └── unified-reports.yml # Unified reporting
├── user-stories/            # Feature user stories (Epics 03-15)
├── unified-testing-stories/ # Testing infrastructure stories
├── docker-compose.yml       # Multi-service orchestration
└── README.md               # This file
```

---

## 📚 Documentation

### For Developers
- **[AGENTS.md](./AGENTS.md)** - Coding principles, vibes, and service scaffolding guide
- **[Frontend Testing](./frontend/TESTING.md)** - React/TypeScript test setup
- **[Database Schema](./docs/schema/)** - ER diagrams and migrations

### For QA/Testing
- **[Test Reports Guide](./docs/TESTING_REPORTS.md)** - Accessing and navigating test reports
- **[Testing PRD](./unified-testing-stories/TestingPRD.md)** - Testing strategy (71% complete)
- **[Story Traceability](./unified-testing-stories/epic_01_story_traceability_backbone/)** - Story ID conventions

### For Product/Business
- **[User Stories](./user-stories/)** - Feature epics (Epics 03-15)
  - Epic 03: CDS Trade Capture
  - Epic 04: Credit Event Processing
  - Epic 05: Routine Lifecycle & Position Changes
  - Epic 06: Index & Constituent Management
  - Epic 07: Pricing & Risk Analytics
  - Epic 08: Margin, Clearing & Capital
  - Epic 09: Reference & Market Data Mastering
  - Epic 10: Reporting, Audit & Replay
  - Epic 11-15: Advanced derivatives (baskets, bonds, correlation)

---

## 🔄 CI/CD Pipeline

### Automated Workflows

**On Pull Request:**
1. Backend Tests - 3 jobs (backend, gateway, risk-engine)
2. Frontend Tests - 4 jobs (unit, integration, e2e, summary)
3. Artifacts uploaded (Allure results, screenshots, videos)
4. Test summary generated in PR checks

**On Main Branch Merge:**
1. All PR checks run again
2. **Unified Reports** workflow triggered:
   - Downloads all 6 artifact patterns
   - Merges into single Allure report
   - Restores historical trends
   - Publishes to GitHub Pages

### Viewing CI Results

- **GitHub Actions**: [Actions Tab](../../actions)
- **Test Reports**: https://[your-org].github.io/[your-repo]/
- **Artifacts**: Download from workflow run (30-day retention)

---

## 🛠️ Development Workflow

### Creating a Feature

1. **Create branch from main:**
   ```bash
   git checkout main
   git pull
   git checkout -b feature/epic-XX-story-YY
   ```

2. **Implement feature + tests:**
   - Tag tests with story ID: `[story:epic_XX_story_YY]`
   - Include severity: `[severity:critical]`
   - Add service tag: `[service:backend]` or `[service:frontend]`

3. **Run tests locally:**
   ```bash
   # Backend
   ./mvnw test
   
   # Frontend
   npm run test:all
   ```

4. **Push and create PR:**
   ```bash
   git add .
   git commit -m "[epic_XX_story_YY] Feature description"
   git push origin feature/epic-XX-story-YY
   ```

5. **Review PR checks:**
   - Verify all tests pass
   - Check Allure artifacts for story coverage
   - Ensure no regressions in test trends

6. **Merge to main:**
   - Unified report automatically updates on GitHub Pages

### Adding a New Service

Follow the **[AGENTS.md](./AGENTS.md)** guide for service scaffolding. Key steps:

1. Choose existing service as template
2. Copy structure and configs
3. Update identifiers (service name, packages, ports)
4. Add Postgres schema if needed
5. Register in `docker-compose.yml`
6. Add CI job in `.github/workflows/backend-tests.yml`
7. Update unified merge patterns in `.github/workflows/unified-reports.yml`

---

## 🐛 Troubleshooting

### Tests Pass Locally but Fail in CI
- Check Node/Java versions match CI (Node 18, Java 17)
- Clean install dependencies: `npm ci`, `./mvnw clean install`
- Review [troubleshooting guide](./docs/TESTING_REPORTS.md#-troubleshooting-common-issues)

### Report Shows 404 on GitHub Pages
- Verify GitHub Pages enabled (Settings > Pages)
- Check `unified-reports` workflow succeeded
- Wait 2-3 minutes after deployment
- See [Issue #1 in troubleshooting](./docs/TESTING_REPORTS.md#issue-1-report-shows-404-error)

### Missing Test Data for a Service
- Check individual service CI workflow succeeded
- Verify artifact upload step passed
- Review artifact name patterns in `unified-reports.yml`
- See [Issue #2 in troubleshooting](./docs/TESTING_REPORTS.md#issue-2-missing-test-data-for-a-service)

**Full troubleshooting guide:** [docs/TESTING_REPORTS.md](./docs/TESTING_REPORTS.md)

---

## 🤝 Contributing

### Definition of Done

A story is considered "Done" when:
- ✅ Code implemented and reviewed
- ✅ **Automated tests written and passing**
- ✅ **Tests tagged with story ID** (`[story:epic_XX_story_YY]`)
- ✅ Documentation updated
- ✅ CI checks pass (build + tests)
- ✅ **Test coverage visible in Allure report**
- ✅ Deployed to staging/production

### Code Review Checklist

- [ ] All tests pass locally
- [ ] New tests include story ID tags
- [ ] Coverage for critical paths (severity: critical/major)
- [ ] No flaky tests introduced
- [ ] Documentation updated (if applicable)
- [ ] CI artifacts show expected test results

### Coding Standards

- **Java**: Follow Spring Boot best practices, use Lombok for boilerplate
- **TypeScript**: Strict mode, ESLint rules enforced
- **Testing**: Prefer integration tests for business logic, unit for utilities
- **Style**: Consistent fonts (Arial, Georgia), colors per [AGENTS.md](./AGENTS.md)

---

## 📊 Project Status

### Testing Infrastructure: **71% Complete**

| Phase | Status | Completion |
|-------|--------|------------|
| **Phase 1**: Foundations (Epics 01-04) | ✅ Complete | 100% |
| **Phase 2**: CI Integration (Epic 06 partial) | ✅ Complete | 100% |
| **Phase 3**: GitHub Pages (Epic 05) | ✅ Complete | 100% |
| **Phase 4**: Governance (Epics 06-08) | 🚧 In Progress | 20% |

**Next Milestones:**
- [ ] Story 6.4: PR comment summaries
- [ ] Story 6.5: CI resilience improvements
- [ ] Epic 07: Story-to-test enforcement
- [ ] Epic 08: Developer experience enhancements

See [TestingPRD.md](./unified-testing-stories/TestingPRD.md#10-implementation-progress) for detailed progress.

### Feature Development

Active epics:
- **Epic 03-06**: Foundation CDS functionality (trade capture, lifecycle, indices)
- **Epic 07-08**: Pricing, risk, margin calculations
- **Epic 09-10**: Data mastering, reporting, audit
- **Epic 11-15**: Advanced derivatives (baskets, bonds, correlation)

---

## 📞 Support & Contact

### Slack Channels
- **#cds-platform-dev** - General development discussion
- **#cds-platform-testing** - Testing infrastructure and reports
- **#cds-platform-support** - Production issues and escalations

### GitHub
- **Issues**: [Create Issue](../../issues/new/choose)
- **Discussions**: [Discussions Tab](../../discussions)
- **Wiki**: [Project Wiki](../../wiki)

### Escalation Path
1. Team Slack channels
2. GitHub issue with `testing-infrastructure` or `bug` label
3. Tag `@platform-team` or `@qa-team`

---

## 📜 License

[Your License Here - e.g., MIT, Apache 2.0, Proprietary]

---

## 🙏 Acknowledgements

- **Allure Framework** - Test reporting ([allure-framework/allure2](https://github.com/allure-framework/allure2))
- **Cypress** - E2E testing ([cypress.io](https://www.cypress.io/))
- **QuantLib ORE** - Risk analytics ([OpenSourceRisk/Engine](https://github.com/OpenSourceRisk/Engine))
- **Spring Boot** - Backend framework ([spring.io/projects/spring-boot](https://spring.io/projects/spring-boot))
- **React** - Frontend framework ([react.dev](https://react.dev/))

---

**Built with ❤️ by the CDS Platform Team**

*Last updated: 2025-01-XX*
