# Test Evidence Framework

AI-assisted, story-driven test and evidence framework for the CDS Platform.

## Overview

This framework automates test generation from user stories across all services (frontend, backend, gateway, risk-engine), pushing evidence to ReportPortal and generating unified test coverage reports.

## Architecture

```
test-evidence-framework/
├── src/
│   ├── models/              # TypeScript type definitions
│   │   ├── story-model.ts   # Story parsing types
│   │   └── test-plan-model.ts # Test planning types
│   ├── parser/              # Story markdown parser
│   │   └── story-parser.ts
│   ├── catalog/             # In-memory stores
│   │   ├── story-catalog.ts
│   │   └── test-plan-catalog.ts
│   ├── planner/             # Test planning engine
│   │   └── test-planner.ts
│   └── cli/                 # Command-line tools
│       ├── parse-stories.ts
│       └── plan-tests.ts
├── dist/                    # Compiled JavaScript
└── package.json
```

## Installation

```bash
cd test-evidence-framework
npm install
npm run build
```

## Usage

### 1. Parse User Stories

Parse all stories from `/user-stories`:

```bash
npm run parse-stories -- --root ../user-stories
```

**With service inference** (automatically detects services from story content):

```bash
npm run parse-stories -- --root ../user-stories --infer
```

With verbose output to see validation errors:

```bash
npm run parse-stories -- --root ../user-stories --verbose
```

Save parsed stories to JSON:

```bash
npm run parse-stories -- --root ../user-stories --output parsed-stories.json
```

**Output:**
- Lists all valid stories found
- Reports validation errors (missing sections, invalid services)
- Shows statistics: total stories, by service, by epic
- **With `--infer`**: Automatically detects services from content when section is missing

### 2. Generate Test Plans

Generate test plans for all stories:

```bash
npm run plan-tests -- --root ../user-stories
```

**With service inference** (recommended for existing stories without Services Involved section):

```bash
npm run plan-tests -- --root ../user-stories --infer
```

Plan tests for a specific story:

```bash
npm run plan-tests -- --root ../user-stories --story "Story 20.1"
```

Plan tests for all stories involving a service:

```bash
npm run plan-tests -- --root ../user-stories --service backend --infer
```

With verbose output showing planned test details:

```bash
npm run plan-tests -- --root ../user-stories --verbose --infer
```

Save test plans to JSON:

```bash
npm run plan-tests -- --root ../user-stories --output test-plans.json --infer
```

**Output:**
- Lists all test plans with service coverage
- Shows if flow tests are required (multi-service stories)
- Estimates recommended test count and complexity
- Statistics by service
- **With `--infer`**: Automatically detects services and generates full test plans

## Story Format

Stories must follow this markdown structure:

```markdown
# Story X.Y - Title

**As a** [actor],  
I want [capability]  
So that [benefit]

## ✅ Acceptance Criteria

- Criterion 1
- Criterion 2

## 🧪 Test Scenarios

1. Scenario 1
2. Scenario 2

## 🧱 Services Involved

- frontend
- backend
- gateway
- risk-engine
```

**Required sections:**
- `## ✅ Acceptance Criteria` or `## 🧪 Test Scenarios` (at least one)
- `## 🧱 Services Involved` (with valid service names)

**Valid service names:**
- `frontend` → React (Jest + RTL tests)
- `backend` → Java Spring Boot (JUnit 5 tests)
- `gateway` → API Gateway (JUnit 5 tests)
- `risk-engine` → Risk calculations (JUnit 5 tests)

## Test Type Mapping

The framework automatically maps services to appropriate test types:

| Service | Test Types |
|---------|-----------|
| `frontend` | `component`, `unit` |
| `backend` | `unit`, `integration`, `api` |
| `gateway` | `unit`, `api` |
| `risk-engine` | `unit`, `integration` |

**Flow tests:** Stories involving multiple services automatically get `flow` tests added to verify cross-service integration.

## Service Inference

For existing stories **without the `## 🧱 Services Involved` section**, the framework can automatically infer which services are involved by analyzing story content (title, acceptance criteria, implementation guidance).

**Enable inference with `--infer` flag:**

```bash
npm run parse-stories -- --root ../user-stories --infer
npm run plan-tests -- --root ../user-stories --infer
```

**How it works:**
- Analyzes keywords: "ui", "form", "component" → `frontend`
- Detects: "api", "endpoint", "controller" → `gateway`
- Finds: "service", "repository", "entity" → `backend`
- Recognizes: "pricing", "valuation", "risk" → `risk-engine`
- Applies heuristics: frontend stories usually need gateway + backend

**See:** [Service Inference Guide](./docs/SERVICE_INFERENCE.md) for details.

**Recommendation:** Use `--infer` to bootstrap test planning on existing stories, then add explicit `## 🧱 Services Involved` sections for accuracy.

## Output Structure

### Parsed Stories JSON

```json
{
  "parsedAt": "2025-01-20T10:30:00.000Z",
  "rootPath": "/path/to/user-stories",
  "statistics": {
    "totalStories": 96,
    "byService": {
      "frontend": 25,
      "backend": 45,
      "gateway": 30,
      "risk-engine": 20
    },
    "multiService": 15,
    "withValidServices": 91,
    "withMissingServices": 5
  },
  "stories": [
    {
      "storyId": "Story 3.1",
      "normalizedId": "STORY_3_1",
      "title": "CDS Trade Capture UI",
      "filePath": "/path/to/story_3_1.md",
      "acceptanceCriteria": ["..."],
      "testScenarios": ["..."],
      "servicesInvolved": ["frontend", "gateway", "backend"],
      "servicesInvolvedStatus": "PRESENT"
    }
  ]
}
```

### Test Plans JSON

```json
{
  "generatedAt": "2025-01-20T10:35:00.000Z",
  "statistics": {
    "totalPlans": 96,
    "byService": {
      "frontend": 25,
      "backend": 45,
      "gateway": 30,
      "risk-engine": 20
    },
    "flowTestsRequired": 15,
    "multiServicePlans": 15
  },
  "plans": [
    {
      "storyId": "Story 3.1",
      "normalizedId": "STORY_3_1",
      "title": "CDS Trade Capture UI",
      "plannedServices": ["frontend", "gateway", "backend"],
      "plannedTests": [
        {
          "service": "frontend",
          "testTypes": ["component", "unit", "flow"],
          "targetPath": "frontend/src/__tests__",
          "acceptanceCriteria": [0, 1, 2],
          "testScenarios": [0, 1, 2, 3]
        },
        {
          "service": "gateway",
          "testTypes": ["unit", "api", "flow"],
          "targetPath": "gateway/src/test/java",
          "acceptanceCriteria": [0, 1, 2],
          "testScenarios": [0, 1, 2, 3]
        },
        {
          "service": "backend",
          "testTypes": ["unit", "integration", "api", "flow"],
          "targetPath": "backend/src/test/java",
          "acceptanceCriteria": [0, 1, 2],
          "testScenarios": [0, 1, 2, 3]
        }
      ],
      "requiresFlowTests": true,
      "recommendedTestCount": 8,
      "complexity": "medium"
    }
  ]
}
```

## Development

### Build

```bash
npm run build
```

### Watch mode (auto-rebuild on changes)

```bash
npm run dev
```

### Run tests

```bash
npm test
```

### Lint

```bash
npm run lint
```

## Next Steps

See [epic_20_test_evidence_framework/README.md](../test-evidence-framework/epic_20_test_evidence_framework/README.md) for implementation roadmap.

**Implemented:**
- ✅ Story 20.1: Story Parser & Topology
- ✅ Story 20.2: Test Planning by Service

**Next:**
- ⏳ Story 20.3: Backend Test Generation (JUnit 5)
- ⏳ Story 20.4: Frontend React Test Generation (Jest + RTL)
- ⏳ Story 20.7: Test Data & Mock Registry
- ⏳ Story 20.8: ReportPortal Evidence Integration
- ⏳ Story 20.9: Evidence Export & Static Dashboard
- ⏳ Story 20.10: CI/CD Integration

## Links

- [PRD: Testing Evidence Framework](../unified-testing-stories/TestingPRD.md)
- [Epic 20: Test Evidence Framework](../test-evidence-framework/epic_20_test_evidence_framework/README.md)
- [User Stories](../user-stories/)
