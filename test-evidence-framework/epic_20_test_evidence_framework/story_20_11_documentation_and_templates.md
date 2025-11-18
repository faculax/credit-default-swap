# Story 20.11 – Documentation & Authoring Templates

**As a developer or QA engineer**,  
I want clear documentation and templates for writing stories, authoring tests, and using the test evidence framework  
So that I can contribute to the framework and understand how to interpret evidence.

## ✅ Acceptance Criteria
- **Developer & QA Documentation** is created that covers:
  - Repository structure and how stories in `/user-stories` map to tests in services (`/frontend`, `/backend`, `/gateway`, `/risk-engine`).
  - How to run tests locally with ReportPortal integration (environment setup, config files).
  - How to add or modify entries in the Test Data & Mock Registry (Story 20.7).
  - How to interpret ReportPortal dashboards and the static Evidence Dashboard (Story 20.9).
  - Troubleshooting common issues (RP connection failures, missing attributes, etc.).
- **Story Authoring Template** is provided that includes:
  - Standard markdown structure with sections:
    - User story (role, capability, benefit).
    - ✅ Acceptance Criteria.
    - 🧪 Test Scenarios.
    - 🧱 Services Involved (with allowed values: `frontend`, `backend`, `gateway`, `risk-engine`).
    - 🛠 Implementation Guidance.
    - 📦 Deliverables.
    - ⏭ Dependencies / Links.
  - Examples and inline guidance for each section.
- **Guidelines for Deciding Services Involved**:
  - Decision matrix or flowchart helping authors determine which services a story touches.
  - Examples of typical service combinations (frontend-only, backend-only, full-stack, etc.).
- **Guidelines for Writing Acceptance Criteria & Test Scenarios**:
  - Best practices for making criteria testable and unambiguous.
  - Examples of good vs poor acceptance criteria.
  - How to align test scenarios with acceptance criteria.
- Documentation is located in:
  - `/test-evidence-framework/README.md` (framework overview).
  - `/test-evidence-framework/docs/` (detailed guides).
  - Story template file: `/user-stories/STORY_TEMPLATE.md`.

## 🧪 Test Scenarios
1. **New developer follows documentation to set up RP locally**  
   Given a new developer joining the team  
   When they read the developer documentation  
   Then they can configure ReportPortal integration locally, run backend and frontend tests, and see results in RP.

2. **QA engineer adds a dataset to the registry**  
   Given a QA engineer needs to create a new test dataset  
   When they follow the registry documentation  
   Then they can add the dataset to the Backend Test Data Registry and a corresponding frontend mock without guidance.

3. **Story author uses the template**  
   Given a PM or developer writing a new story  
   When they copy the story template  
   Then they can fill in all required sections (including `Services Involved`) and produce a valid story that the parser accepts.

4. **User interprets static dashboard**  
   Given a non-technical stakeholder viewing the static Evidence Dashboard  
   When they read the dashboard documentation  
   Then they understand what coverage badges mean, how to navigate per-story pages, and where to find detailed test results.

5. **Troubleshooting guide resolves common issues**  
   Given a developer encountering a "ReportPortal launch not found" error  
   When they consult the troubleshooting section  
   Then they find the cause (e.g., missing API token) and steps to resolve it.

## 🛠 Implementation Guidance
- Write documentation in Markdown for easy versioning and integration with GitHub Pages or Docusaurus.
- Include screenshots or diagrams where helpful (e.g., RP dashboard views, service topology).
- Provide runnable examples (sample commands, config snippets) in code blocks.
- Keep the story template minimal but annotated with inline tips.

## 📦 Deliverables
- `/test-evidence-framework/README.md` (framework overview).
- `/test-evidence-framework/docs/DEVELOPER_GUIDE.md` (setup, running tests, RP integration).
- `/test-evidence-framework/docs/QA_GUIDE.md` (adding datasets, interpreting evidence).
- `/test-evidence-framework/docs/TROUBLESHOOTING.md` (common issues and solutions).
- `/user-stories/STORY_TEMPLATE.md` (story authoring template).
- `/test-evidence-framework/docs/SERVICES_DECISION_MATRIX.md` (how to decide `servicesInvolved`).
- `/test-evidence-framework/docs/WRITING_CRITERIA.md` (best practices for acceptance criteria and test scenarios).

## ⏭ Dependencies / Links
- Draws from all prior stories (20.1–20.10) for content and examples.
- Finalizes the framework by ensuring it is usable and understandable by the full team.
