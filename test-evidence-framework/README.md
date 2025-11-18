# Test Evidence Framework# Test Evidence Framework



**Unified test evidence generation, validation, and reporting for the CDS Platform**AI-assisted, story-driven test and evidence framework for the CDS Platform.



## 🎯 Overview## Overview



The Test Evidence Framework is a comprehensive TypeScript toolkit that bridges user stories, test execution, and evidence reporting. It automates the generation of tests from story requirements, validates implementation completeness, and provides unified evidence dashboards across all services.This framework automates test generation from user stories across all services (frontend, backend, gateway, risk-engine), pushing evidence to ReportPortal and generating unified test coverage reports.



### Key Capabilities## Architecture



- **Story-Driven Testing**: Parses user story markdown to generate test plans and scaffolding```

- **Multi-Service Support**: Backend (Spring Boot/Java), Frontend (React/TypeScript), Gateway, Risk Enginetest-evidence-framework/

- **Flow Testing**: End-to-end integration tests across service boundaries├── src/

- **ReportPortal Integration**: Automated test result uploads with story traceability│   ├── models/              # TypeScript type definitions

- **Evidence Export**: Static HTML dashboards showing coverage and test execution history│   │   ├── story-model.ts   # Story parsing types

- **CI/CD Ready**: GitHub Actions workflows with selective execution and automated reporting│   │   └── test-plan-model.ts # Test planning types

│   ├── parser/              # Story markdown parser

## 🏗 Architecture│   │   └── story-parser.ts

│   ├── catalog/             # In-memory stores

```│   │   ├── story-catalog.ts

test-evidence-framework/│   │   └── test-plan-catalog.ts

├── src/│   ├── planner/             # Test planning engine

│   ├── parser/              # Story markdown parsing (Story 20.1)│   │   └── test-planner.ts

│   │   ├── story-parser.ts│   └── cli/                 # Command-line tools

│   │   └── story-types.ts│       ├── parse-stories.ts

│   ├── planner/             # Test plan generation (Story 20.2)│       └── plan-tests.ts

│   │   ├── test-planner.ts├── dist/                    # Compiled JavaScript

│   │   └── test-plan-types.ts└── package.json

│   ├── generators/          # Test code generators (Stories 20.3-20.5)```

│   │   ├── backend/

│   │   │   ├── backend-test-generator.ts## Installation

│   │   │   └── templates/

│   │   ├── frontend/```bash

│   │   │   ├── frontend-test-generator.tscd test-evidence-framework

│   │   │   └── templates/npm install

│   │   └── flow/npm run build

│   │       ├── flow-test-generator.ts```

│   │       └── templates/

│   ├── validation/          # Code validation (Story 20.6)## Usage

│   │   ├── code-validator.ts

│   │   └── crystallization-engine.ts### 1. Parse User Stories

│   ├── registry/            # Test data management (Story 20.7)

│   │   ├── test-data-registry.tsParse all stories from `/user-stories`:

│   │   └── registry-types.ts

│   ├── reportportal/        # ReportPortal integration (Story 20.8)```bash

│   │   ├── reportportal-client.tsnpm run parse-stories -- --root ../user-stories

│   │   └── allure-reportportal-mapper.ts```

│   ├── evidence/            # Evidence export (Story 20.9)

│   │   ├── reportportal-query-client.ts**With service inference** (automatically detects services from story content):

│   │   ├── evidence-exporter.ts

│   │   └── static-site-generator.ts```bash

│   ├── cli/                 # Command-line toolsnpm run parse-stories -- --root ../user-stories --infer

│   │   ├── generate-tests.ts```

│   │   ├── validate-code.ts

│   │   ├── upload-results.tsWith verbose output to see validation errors:

│   │   └── export-evidence.ts

│   └── utils/               # Shared utilities```bash

│       ├── file-system-utils.tsnpm run parse-stories -- --root ../user-stories --verbose

│       ├── logger.ts```

│       └── template-engine.ts

├── .github/workflows/       # CI/CD workflows (Story 20.10)Save parsed stories to JSON:

│   ├── test-evidence.yml

│   └── deploy-evidence-dashboard.yml```bash

├── docs/                    # Documentation (Story 20.11)npm run parse-stories -- --root ../user-stories --output parsed-stories.json

│   ├── DEVELOPER_GUIDE.md```

│   ├── QA_GUIDE.md

│   ├── TROUBLESHOOTING.md**Output:**

│   ├── SERVICES_DECISION_MATRIX.md- Lists all valid stories found

│   └── WRITING_CRITERIA.md- Reports validation errors (missing sections, invalid services)

└── package.json- Shows statistics: total stories, by service, by epic

```- **With `--infer`**: Automatically detects services from content when section is missing



## 🚀 Quick Start### 2. Generate Test Plans



### PrerequisitesGenerate test plans for all stories:



- Node.js 20+```bash

- npm or yarnnpm run plan-tests -- --root ../user-stories

- Java 21+ (for backend tests)```

- Maven 3.9+ (for backend tests)

- ReportPortal instance (optional, for evidence reporting)**With service inference** (recommended for existing stories without Services Involved section):



### Installation```bash

npm run plan-tests -- --root ../user-stories --infer

```bash```

cd test-evidence-framework

npm installPlan tests for a specific story:

npm run build

``````bash

npm run plan-tests -- --root ../user-stories --story "Story 20.1"

### Generate Tests from a User Story```



```bashPlan tests for all stories involving a service:

# Parse story and generate test plan

npm run generate-tests -- --story ../user-stories/epic_03_cds_trade_capture/story_3_1_create_single_name_cds_trade.md```bash

npm run plan-tests -- --root ../user-stories --service backend --infer

# Generate backend tests```

npm run generate-tests -- --story ../user-stories/epic_03_cds_trade_capture/story_3_1_create_single_name_cds_trade.md --service backend

With verbose output showing planned test details:

# Generate frontend tests

npm run generate-tests -- --story ../user-stories/epic_03_cds_trade_capture/story_3_1_create_single_name_cds_trade.md --service frontend```bash

npm run plan-tests -- --root ../user-stories --verbose --infer

# Generate flow tests (end-to-end)```

npm run generate-tests -- --story ../user-stories/epic_03_cds_trade_capture/story_3_1_create_single_name_cds_trade.md --service flow

```Save test plans to JSON:



### Run Tests Locally```bash

npm run plan-tests -- --root ../user-stories --output test-plans.json --infer

```bash```

# Backend tests (with TestContainers for PostgreSQL)

cd ../backend**Output:**

mvn clean test- Lists all test plans with service coverage

- Shows if flow tests are required (multi-service stories)

# Frontend tests (with Allure)- Estimates recommended test count and complexity

cd ../frontend- Statistics by service

npm run test:unit- **With `--infer`**: Automatically detects services and generates full test plans



# All services with unified reporting## Story Format

cd ..

./scripts/test-unified-local.ps1  # WindowsStories must follow this markdown structure:

./scripts/test-unified-local.sh   # Linux/Mac

``````markdown

# Story X.Y - Title

### Upload Results to ReportPortal

**As a** [actor],  

```bashI want [capability]  

# Set environment variablesSo that [benefit]

export REPORTPORTAL_ENDPOINT=https://your-reportportal.example.com

export REPORTPORTAL_TOKEN=your-api-token## ✅ Acceptance Criteria

export REPORTPORTAL_PROJECT=cds-platform

- Criterion 1

# Upload backend test results- Criterion 2

npm run upload-results -- --service backend --allure-results ../backend/target/allure-results

## 🧪 Test Scenarios

# Upload frontend test results

npm run upload-results -- --service frontend --allure-results ../frontend/allure-results1. Scenario 1

2. Scenario 2

# Upload all services

npm run upload-results -- --all## 🧱 Services Involved

```

- frontend

### Export Evidence Dashboard- backend

- gateway

```bash- risk-engine

# Export all stories```

npm run export-evidence -- --output-dir ./evidence-export

**Required sections:**

# Export specific story- `## ✅ Acceptance Criteria` or `## 🧪 Test Scenarios` (at least one)

npm run export-evidence -- --story-id story_3_1 --output-dir ./evidence-export- `## 🧱 Services Involved` (with valid service names)



# Export with filters**Valid service names:**

npm run export-evidence -- --services backend,frontend --limit 50 --output-dir ./evidence-export- `frontend` → React (Jest + RTL tests)

```- `backend` → Java Spring Boot (JUnit 5 tests)

- `gateway` → API Gateway (JUnit 5 tests)

## 📚 Documentation- `risk-engine` → Risk calculations (JUnit 5 tests)



### For Developers## Test Type Mapping



- **[Developer Guide](docs/DEVELOPER_GUIDE.md)**: Setup, running tests, ReportPortal integrationThe framework automatically maps services to appropriate test types:

- **[Troubleshooting](docs/TROUBLESHOOTING.md)**: Common issues and solutions

- **[CI/CD Integration](CI-INTEGRATION.md)**: GitHub Actions workflows| Service | Test Types |

- **[Evidence Export](EVIDENCE-EXPORT.md)**: Static dashboard generation|---------|-----------|

| `frontend` | `component`, `unit` |

### For QA Engineers| `backend` | `unit`, `integration`, `api` |

| `gateway` | `unit`, `api` |

- **[QA Guide](docs/QA_GUIDE.md)**: Adding test data, interpreting evidence, managing test suites| `risk-engine` | `unit`, `integration` |

- **[Services Decision Matrix](docs/SERVICES_DECISION_MATRIX.md)**: Determining which services a story affects

**Flow tests:** Stories involving multiple services automatically get `flow` tests added to verify cross-service integration.

### For Story Authors

## Service Inference

- **[Story Template](../user-stories/STORY_TEMPLATE.md)**: Template for writing testable user stories

- **[Writing Criteria Guide](docs/WRITING_CRITERIA.md)**: Best practices for acceptance criteria and test scenariosFor existing stories **without the `## 🧱 Services Involved` section**, the framework can automatically infer which services are involved by analyzing story content (title, acceptance criteria, implementation guidance).



## 🔧 Configuration**Enable inference with `--infer` flag:**



### ReportPortal Configuration```bash

npm run parse-stories -- --root ../user-stories --infer

Create `reportportal.json` in the framework root:npm run plan-tests -- --root ../user-stories --infer

```

```json

{**How it works:**

  "endpoint": "https://your-reportportal.example.com",- Analyzes keywords: "ui", "form", "component" → `frontend`

  "token": "your-api-token",- Detects: "api", "endpoint", "controller" → `gateway`

  "project": "cds-platform",- Finds: "service", "repository", "entity" → `backend`

  "launchName": "CDS Platform Tests - Local",- Recognizes: "pricing", "valuation", "risk" → `risk-engine`

  "launchAttributes": [- Applies heuristics: frontend stories usually need gateway + backend

    { "key": "environment", "value": "local" },

    { "key": "framework", "value": "unified-test-evidence" }**See:** [Service Inference Guide](./docs/SERVICE_INFERENCE.md) for details.

  ]

}**Recommendation:** Use `--infer` to bootstrap test planning on existing stories, then add explicit `## 🧱 Services Involved` sections for accuracy.

```

## Output Structure

Or use environment variables:

### Parsed Stories JSON

```bash

export REPORTPORTAL_ENDPOINT=https://your-reportportal.example.com```json

export REPORTPORTAL_TOKEN=your-api-token{

export REPORTPORTAL_PROJECT=cds-platform  "parsedAt": "2025-01-20T10:30:00.000Z",

```  "rootPath": "/path/to/user-stories",

  "statistics": {

### Test Data Registry    "totalStories": 96,

    "byService": {

The framework maintains a centralized test data registry at:      "frontend": 25,

      "backend": 45,

```      "gateway": 30,

test-evidence-framework/test-data-registry.json      "risk-engine": 20

```    },

    "multiService": 15,

This registry provides:    "withValidServices": 91,

- **Backend Test Data**: Entities for integration tests (CDSTrade, ReferenceEntity, etc.)    "withMissingServices": 5

- **Frontend Mocks**: Mock data for component and integration tests  },

- **Flow Test Data**: End-to-end test scenarios with multi-service interactions  "stories": [

    {

Add new test data entries via CLI:      "storyId": "Story 3.1",

      "normalizedId": "STORY_3_1",

```bash      "title": "CDS Trade Capture UI",

npm run registry -- add --type backend --category trade --data '{"tradeId":"T001",...}'      "filePath": "/path/to/story_3_1.md",

npm run registry -- add --type frontend --category trade --data '{"tradeId":"T001",...}'      "acceptanceCriteria": ["..."],

```      "testScenarios": ["..."],

      "servicesInvolved": ["frontend", "gateway", "backend"],

## 🎨 Code Generation Templates      "servicesInvolvedStatus": "PRESENT"

    }

### Backend Tests (Java/Spring Boot)  ]

}

Generated tests include:```

- Unit tests with Mockito

- Integration tests with `@SpringBootTest` and TestContainers### Test Plans JSON

- Repository tests with `@DataJpaTest`

- Controller tests with MockMvc```json

- Service tests with comprehensive mocking{

  "generatedAt": "2025-01-20T10:35:00.000Z",

Example:  "statistics": {

    "totalPlans": 96,

```java    "byService": {

@SpringBootTest      "frontend": 25,

@AutoConfigureMockMvc      "backend": 45,

@Testcontainers      "gateway": 30,

@AllureFeature("CDS Trade Capture")      "risk-engine": 20

@AllureStory("Create Single Name CDS Trade")    },

class CDSTradeControllerIntegrationTest {    "flowTestsRequired": 15,

    // Generated test methods based on acceptance criteria    "multiServicePlans": 15

}  },

```  "plans": [

    {

### Frontend Tests (React/TypeScript)      "storyId": "Story 3.1",

      "normalizedId": "STORY_3_1",

Generated tests include:      "title": "CDS Trade Capture UI",

- Component tests with React Testing Library      "plannedServices": ["frontend", "gateway", "backend"],

- Hook tests with `@testing-library/react-hooks`      "plannedTests": [

- Integration tests with mock API responses        {

- Accessibility tests with jest-axe          "service": "frontend",

          "testTypes": ["component", "unit", "flow"],

Example:          "targetPath": "frontend/src/__tests__",

          "acceptanceCriteria": [0, 1, 2],

```typescript          "testScenarios": [0, 1, 2, 3]

describe('CDSTradeForm', () => {        },

  it('should create trade when form submitted with valid data', async () => {        {

    // Generated test based on acceptance criteria          "service": "gateway",

  });          "testTypes": ["unit", "api", "flow"],

});          "targetPath": "gateway/src/test/java",

```          "acceptanceCriteria": [0, 1, 2],

          "testScenarios": [0, 1, 2, 3]

### Flow Tests (End-to-End)        },

        {

Generated tests include:          "service": "backend",

- Multi-service integration scenarios          "testTypes": ["unit", "integration", "api", "flow"],

- REST API orchestration          "targetPath": "backend/src/test/java",

- Database state verification          "acceptanceCriteria": [0, 1, 2],

- Cross-service data consistency checks          "testScenarios": [0, 1, 2, 3]

        }

Example:      ],

      "requiresFlowTests": true,

```typescript      "recommendedTestCount": 8,

describe('CDS Trade Lifecycle', () => {      "complexity": "medium"

  it('should create trade via frontend, persist in backend, and appear in portfolio', async () => {    }

    // Generated flow test spanning frontend → gateway → backend → database  ]

  });}

});```

```

## Development

## 🔍 Validation & Crystallization

### Build

The framework validates generated tests against story requirements:

```bash

```bashnpm run build

npm run validate-code -- --story ../user-stories/epic_03_cds_trade_capture/story_3_1_create_single_name_cds_trade.md```

```

### Watch mode (auto-rebuild on changes)

Validation checks:

- ✅ All acceptance criteria have corresponding test cases```bash

- ✅ Test scenarios are implementednpm run dev

- ✅ Required services are tested```

- ✅ Test data registry entries exist

- ✅ Allure annotations are correct (feature, story, epic)### Run tests



**Crystallization** locks validated tests to prevent drift:```bash

npm test

```bash```

npm run crystallize -- --story story_3_1

```### Lint



Crystallized tests are marked as "frozen" in the registry and trigger warnings if modified without updating the story.```bash

npm run lint

## 📊 Evidence Reporting```



### ReportPortal Dashboard## Next Steps



After uploading test results to ReportPortal, view:See [epic_20_test_evidence_framework/README.md](../test-evidence-framework/epic_20_test_evidence_framework/README.md) for implementation roadmap.



- **Launches**: All test executions grouped by launch name**Implemented:**

- **Filters**: Pre-configured filters for each service (backend, frontend, gateway, risk-engine)- ✅ Story 20.1: Story Parser & Topology

- **Widgets**: Custom dashboards showing coverage per story- ✅ Story 20.2: Test Planning by Service

- **Attributes**: Tests tagged with `story`, `service`, `epic`, `acceptanceCriteria`

**Next:**

### Static HTML Dashboard- ⏳ Story 20.3: Backend Test Generation (JUnit 5)

- ⏳ Story 20.4: Frontend React Test Generation (Jest + RTL)

Export a static dashboard for stakeholders:- ⏳ Story 20.7: Test Data & Mock Registry

- ⏳ Story 20.8: ReportPortal Evidence Integration

```bash- ⏳ Story 20.9: Evidence Export & Static Dashboard

npm run export-evidence -- --output-dir ./evidence-export- ⏳ Story 20.10: CI/CD Integration

```

## Links

The dashboard includes:

- **Story Index**: List of all stories with coverage badges- [PRD: Testing Evidence Framework](../unified-testing-stories/TestingPRD.md)

- **Story Details**: Per-story pages with acceptance criteria, test results, and history- [Epic 20: Test Evidence Framework](../test-evidence-framework/epic_20_test_evidence_framework/README.md)

- **Service Tables**: Test execution status per service- [User Stories](../user-stories/)

- **History**: Chronological test execution timeline

Deploy to GitHub Pages:

```bash
# Automated via GitHub Actions (on push to main)
# Or manually:
cd evidence-export
git init
git add .
git commit -m "Evidence dashboard"
git push -f https://github.com/your-org/your-repo.git main:gh-pages
```

View at: `https://your-org.github.io/your-repo/`

## 🚦 CI/CD Integration

The framework includes GitHub Actions workflows:

### Main CI/CD Workflow (`.github/workflows/test-evidence.yml`)

**Pull Requests:**
- Selective test execution (only run tests for changed services)
- ReportPortal upload with PR-specific launch names
- Automated PR comments with test summary and ReportPortal links

**Main Branch:**
- Full test suite execution
- ReportPortal upload with "Main Branch" launch
- Evidence dashboard regeneration and GitHub Pages deployment

### Dashboard Deployment (`.github/workflows/deploy-evidence-dashboard.yml`)

- Manual trigger with optional filters (story ID, services, limit)
- Scheduled daily deployment (00:00 UTC)
- Exports evidence from ReportPortal
- Generates HTML dashboard
- Deploys to GitHub Pages

See **[CI Integration Guide](CI-INTEGRATION.md)** for setup and configuration.

## 📈 Metrics & Reporting

The framework tracks:

- **Story Coverage**: % of acceptance criteria with passing tests
- **Service Coverage**: % of stories tested per service
- **Test Execution History**: Pass/fail trends over time
- **Crystallization Status**: Locked vs unlocked tests

View metrics in:
- ReportPortal widgets
- Static dashboard summary page
- CI/CD workflow summaries

## 🛠 Development

### Building the Framework

```bash
npm install
npm run build     # Compile TypeScript to dist/
npm run lint      # Run ESLint
npm run test      # Run unit tests
```

### Adding a New Generator

1. Create generator class in `src/generators/<service>/`
2. Implement `ITestGenerator` interface
3. Add templates in `src/generators/<service>/templates/`
4. Update CLI in `src/cli/generate-tests.ts`
5. Add documentation in `docs/`

### Adding a New CLI Command

1. Create command file in `src/cli/`
2. Add script to `package.json`
3. Update README.md with usage examples
4. Add integration tests

## 🤝 Contributing

1. Write user story using `STORY_TEMPLATE.md`
2. Generate tests: `npm run generate-tests -- --story <path>`
3. Implement functionality in services
4. Run tests locally and validate
5. Submit PR (CI will run tests and comment with results)
6. Merge to main (dashboard updates automatically)

## 📦 Project Structure

```
credit-default-swap/
├── backend/                 # Spring Boot backend service
│   ├── src/test/           # Generated backend tests
│   └── target/allure-results/
├── frontend/               # React frontend application
│   ├── src/__tests__/      # Generated frontend tests
│   └── allure-results/
├── gateway/                # API Gateway service
│   └── src/test/
├── risk-engine/            # Risk calculation service
│   └── src/test/
├── test-evidence-framework/  # This framework
│   ├── src/
│   ├── docs/
│   └── .github/workflows/
├── user-stories/           # Story markdown files
│   ├── STORY_TEMPLATE.md
│   ├── epic_03_cds_trade_capture/
│   ├── epic_04_cds_credit_event_processing/
│   └── ...
└── scripts/                # Utility scripts
    ├── test-unified-local.ps1
    └── test-unified-local.sh
```

## 🐛 Troubleshooting

Common issues:

- **ReportPortal connection failed**: Check `REPORTPORTAL_ENDPOINT`, `REPORTPORTAL_TOKEN`, and network access
- **Tests not appearing in RP**: Verify Allure annotations (`@Feature`, `@Story`, `@Epic`) and result upload
- **Dashboard not deploying**: Check GitHub Pages settings and `deploy-evidence-dashboard.yml` workflow
- **Path filters not working**: Review `detect-changes` job output and path patterns in `test-evidence.yml`

See **[Reference Guide](docs/REFERENCE.md)** for comprehensive troubleshooting.

## 📄 License

MIT License - see LICENSE file for details

## � Documentation

- **[Getting Started](docs/GETTING_STARTED.md)** - Quick start guide (5 min install, 10 min first tests)
- **[User Guide](docs/USER_GUIDE.md)** - Comprehensive developer and QA guide
- **[Integration](docs/INTEGRATION.md)** - CI/CD, ReportPortal, and evidence dashboard setup
- **[Reference](docs/REFERENCE.md)** - Troubleshooting, service selection, and writing best practices
- **[Story Template](../user-stories/STORY_TEMPLATE.md)** - Template for writing new stories

**External Resources:**
- [ReportPortal Documentation](https://reportportal.io/docs)
- [Allure Documentation](https://docs.qameta.io/allure/)

---

**Version**: 1.0.0  
**Epic**: 20 - Test Evidence Framework  
**Last Updated**: November 2025
