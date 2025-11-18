# Story 20.1 – Story Parsing & Service Topology Modeling

**As the test-evidence framework**,  
I want to parse markdown user stories in `/user-stories` into a structured model and capture which services they involve  
So that all downstream test generation and evidence can be driven from a single, consistent source of truth.

## ✅ Acceptance Criteria
- Story parser scans `/user-stories/**/story_*.md` and builds an in-memory `StoryModel` for each story.
- For each story, the parser extracts at minimum:
  - `storyId` (e.g. `"Story 3.2"`) and a normalized ID (e.g. `STORY_3_2`).
  - `title` and the three-part user story (role, capability, benefit) when present.
  - An ordered list of `acceptanceCriteria` from the `## ✅ Acceptance Criteria` section.
  - An ordered list of `testScenarios` from the `## 🧪 Test Scenarios` section.
  - `servicesInvolved[]` from `## 🧱 Services Involved` (when present).
- For stories without an explicit `## 🧱 Services Involved` section:
  - The parser marks them as `servicesInvolvedStatus = MISSING`, and
  - Surfaces a clear validation error including file path and missing section name.
- The parser validates that:
  - At least one acceptance criterion or test scenario exists for each story.
  - Any parsed `servicesInvolved[]` values are restricted to the set `{ frontend, backend, gateway, risk-engine }`.
- Parsed stories are exposed via a `StoryCatalog` API that supports:
  - Listing all stories and basic metadata.
  - Fetching a story by id or normalized id.
  - Filtering by service (e.g. all stories involving `frontend`).
- Parser behavior is covered by automated tests for:
  - A well-formed story file with all sections present.
  - A story missing `Services Involved`.
  - A story with invalid service names.
  - Empty or malformed acceptance criteria and test scenarios sections.

## 🧪 Test Scenarios
1. **Happy path – full story**  
   Given a story markdown file with `Acceptance Criteria`, `Test Scenarios`, and `Services Involved` sections  
   When the parser processes it  
   Then the resulting `StoryModel` contains correct story ID, title, acceptance criteria, test scenarios, and `servicesInvolved = [frontend, gateway, backend]`.

2. **Missing Services Involved**  
   Given a story markdown file without a `## 🧱 Services Involved` section  
   When the parser processes it  
   Then it marks `servicesInvolvedStatus = MISSING` and records a validation warning identifying the file.

3. **Invalid service name**  
   Given a story with `Services Involved` including `frontend`, `backend`, and an invalid value `mobile-api`  
   When the parser processes it  
   Then the parser flags a validation error indicating the allowed service set and the invalid value.

4. **No acceptance criteria or test scenarios**  
   Given a story markdown file that omits both `Acceptance Criteria` and `Test Scenarios` sections  
   When the parser processes it  
   Then the parser marks the story as invalid, with a clear error that no testable criteria were found.

5. **StoryCatalog filtering by service**  
   Given multiple parsed stories with different `servicesInvolved`  
   When the `StoryCatalog` is queried for `service = frontend`  
   Then only stories with `frontend` in their `servicesInvolved[]` are returned.

## 🛠 Implementation Guidance
- Implement a small, testable parser library under `/test-evidence-framework` (language is flexible, but align with repo conventions).
- Avoid heavy markdown dependencies where possible; focus on simple structural parsing based on headers and lists.
- Define a `StoryModel` and `StoryCatalog` interface that can be reused by backend and frontend test generators.
- Keep validation logic explicit and return both `StoryModel` and `ValidationResult` objects so CI can fail fast on malformed stories.

## 📦 Deliverables
- `StoryModel` and `StoryCatalog` types/interfaces.
- Parser implementation that reads from `/user-stories` and populates the catalog.
- Unit tests for the parser and catalog behaviors.
- Short README note in `/test-evidence-framework` explaining how story parsing works and how to add `Services Involved` to new stories.

## ⏭ Dependencies / Links
- Consumes existing story markdown files in `/user-stories`.
- Feeds into Story 20.2 (Test Planning) and all subsequent test generation stories.
