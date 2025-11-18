# Service Inference Feature

## Overview

The test-evidence-framework includes **automatic service inference** to detect which services (frontend, backend, gateway, risk-engine) are involved in a story when the `## 🧱 Services Involved` section is missing.

## How It Works

The inference engine analyzes story content using keyword matching:

### Keywords by Service

**Frontend** (`frontend`):
- UI components: `ui`, `form`, `component`, `react`, `view`, `display`, `dashboard`
- Interactions: `button`, `input`, `modal`, `table`, `chart`
- Styling: `responsive`

**Gateway** (`gateway`):
- API layer: `endpoint`, `api`, `rest`, `controller`, `route`, `http`
- Data flow: `request`, `response`, `validation`
- Security: `authentication`

**Backend** (`backend`):
- Data layer: `service`, `repository`, `entity`, `persistence`, `database`
- Logic: `business logic`, `calculation`, `workflow`
- Model: `domain model`

**Risk Engine** (`risk-engine`):
- Analytics: `pricing`, `valuation`, `risk`, `pv01`, `dv01`, `sensitivity`
- Calculations: `ore`, `curve`, `scenario`, `monte carlo`, `simulation`

### Heuristics

1. **Frontend → Gateway**: If frontend is detected, gateway is usually also involved
2. **Gateway → Backend**: If gateway is detected, backend is usually also involved
3. **Score-based**: Services with keyword matches > 0 are included

## Usage

### Enable Inference

Add the `--infer` (or `-i`) flag to any command:

```bash
# Parse stories with inference
npm run parse-stories -- --root ../user-stories --infer

# Generate test plans with inference
npm run plan-tests -- --root ../user-stories --infer

# Combine with other flags
npm run plan-tests -- --root ../user-stories --infer --service frontend --verbose
```

### Without Inference (Default)

Stories missing `## 🧱 Services Involved` will:
- Get `servicesInvolvedStatus: MISSING`
- Generate empty test plans (no services, no tests)
- Show zero in statistics

### With Inference Enabled

Stories missing the section will:
- Analyze title, acceptance criteria, implementation guidance, deliverables
- Infer likely services based on keywords
- Get `servicesInvolvedStatus: PRESENT` if services were inferred
- Include a warning: `"Missing section - inferred: frontend, backend, gateway"`
- Generate full test plans for inferred services

## Example Results

### Before (without `--infer`):

```
✅ Parsed 5 valid stories
❌ Found 91 stories with errors

📊 Statistics:
   By service:
     frontend:     0
     backend:      0
     gateway:      0
     risk-engine:  0
```

### After (with `--infer`):

```
✅ Parsed 96 valid stories

📊 Statistics:
   By service:
     frontend:     45
     backend:      78
     gateway:      62
     risk-engine:  28
```

## Inference Quality

The inference engine provides reasonable accuracy for:
- ✅ **Frontend stories** - High accuracy (mentions UI, forms, components)
- ✅ **Backend stories** - Good accuracy (mentions services, repositories, entities)
- ✅ **Gateway stories** - Medium accuracy (often inferred via frontend heuristic)
- ✅ **Risk-engine stories** - High accuracy (distinctive keywords: pricing, valuation, ore)

### Recommendations

1. **For existing stories**: Use `--infer` to quickly get started and generate initial test plans
2. **For new stories**: Always add explicit `## 🧱 Services Involved` section
3. **Review inferred services**: Check warnings in verbose mode to verify inference accuracy
4. **Add missing sections**: Use inference output as a guide to add explicit sections

## Example: Story 3.1 (CDS Trade Capture UI)

**Content analyzed:**
- Title: "CDS Trade Capture **UI** & Reference Data"
- Acceptance Criteria: "**Form** displays all fields", "**Dropdowns** populated", "**Layout responsive**"
- Implementation: "Reuse existing `CDSTradeForm` **component**"
- Deliverables: "Updated **React** form **component**"

**Inference result:**
```
Services inferred: frontend, gateway, backend
Confidence: high (multiple frontend keywords + heuristics)
```

**Generated test plan:**
- `frontend`: component, unit, flow tests → `frontend/src/__tests__`
- `gateway`: unit, api, flow tests → `gateway/src/test/java`
- `backend`: unit, integration, api, flow tests → `backend/src/test/java`

## API Usage

```typescript
import { StoryParser } from './parser/story-parser';
import { StoryCatalog } from './catalog/story-catalog';

// Enable inference via constructor
const parser = new StoryParser(true); // enableInference = true

// Parse with inference
const results = parser.parseStoriesInDirectory('../user-stories');

const catalog = new StoryCatalog();
for (const result of results) {
  if (result.validation.valid) {
    catalog.add(result.story);
    
    // Check if services were inferred
    if (result.validation.warnings.some(w => w.message.includes('inferred'))) {
      console.log(`Inferred services for ${result.story.storyId}:`, result.story.servicesInvolved);
    }
  }
}
```

## Future Enhancements

### LLM-Based Inference (Potential Story 20.12)

Replace keyword matching with LLM analysis:

```typescript
// Hypothetical future API
const aiInference = new AIServiceInferenceHelper({
  model: 'gpt-4',
  prompt: `Analyze this user story and determine which services are involved:
    - frontend (React UI)
    - backend (Spring Boot services)
    - gateway (REST API)
    - risk-engine (quantitative calculations)
    
    Story: ${story.title}
    Acceptance Criteria: ${story.acceptanceCriteria.join('\n')}
    
    Return JSON: { "services": [...], "confidence": "high|medium|low", "reasoning": "..." }`
});

const inferred = await aiInference.inferServices(story);
```

Benefits:
- Higher accuracy (contextual understanding)
- Confidence scoring with reasoning
- Can detect subtle service involvement
- Learns from existing labeled stories

## Troubleshooting

**Q: Inference is adding too many services**
- Review the keywords in `service-inference.ts`
- Adjust heuristics (e.g., disable frontend→gateway auto-add)
- Add explicit sections to override inference

**Q: Inference is missing services**
- Add more domain-specific keywords to `SERVICE_KEYWORDS`
- Check if story uses unusual terminology
- Consider using explicit sections for edge cases

**Q: Should I use inference for production?**
- For bootstrapping: Yes (quick start, generate initial plans)
- For long-term: No (explicit sections are more maintainable and accurate)
- Best practice: Use inference, review output, add explicit sections

## Related

- [Story Parser Implementation](../src/parser/story-parser.ts)
- [Service Inference Helper](../src/inference/service-inference.ts)
- [Test Planning](../src/planner/test-planner.ts)
- [Story Format Guide](../README.md#story-format)
