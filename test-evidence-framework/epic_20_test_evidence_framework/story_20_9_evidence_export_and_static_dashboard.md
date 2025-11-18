# Story 20.9 – Evidence Export & Static Dashboard

**As a stakeholder needing evidence visibility**,  
I want a static HTML dashboard that aggregates story coverage and test results from ReportPortal  
So that non-technical users can understand what's tested without using ReportPortal directly.

## ✅ Acceptance Criteria
- A **ReportPortalQueryClient** is implemented that:
  - Connects to ReportPortal's REST API with authentication.
  - Queries launches and tests by:
    - `storyId`
    - `servicesInvolved`
    - `serviceUnderTest`
    - Time ranges
  - Aggregates evidence into structured JSON for:
    - `stories.json` (summary of all stories with test status).
    - `story-{id}.json` (detailed evidence for one story).
  - Handles both Java and JS test events (unified evidence model).
- A **Static Site Generator** is implemented that:
  - Reads exported JSON from the `ReportPortalQueryClient`.
  - Generates static HTML files:
    - `index.html`:
      - List of stories with: `storyId`, `title`, services involved, last execution date, status per service (frontend/backend/gateway/risk-engine).
    - `story-{id}.html`:
      - Story details ("As the … I want …").
      - Acceptance criteria list with coverage badges per service.
      - Test scenarios list with coverage badges per service.
      - Per-service test history (tables for frontend/backend/gateway/risk-engine with test names, timestamps, pass/fail).
      - Dataset and environment info.
  - Uses simple, responsive CSS for readability.
- A **GitHub Pages deployment workflow** is configured that:
  - Runs on `main` branch or on-demand.
  - Exports evidence from ReportPortal via `ReportPortalQueryClient`.
  - Generates static site files (HTML, CSS, assets) into `/site` or `/dist`.
  - Publishes to `gh-pages` branch (or equivalent GitHub Pages setup).
  - Results in a public dashboard URL (e.g., `https://{org}.github.io/{repo}/`).
- The dashboard URL pattern includes:
  - Base index at `/`.
  - Per-story pages at `/story-{id}.html` (e.g., `/story-3-2.html`).

## 🧪 Test Scenarios
1. **Query ReportPortal for story evidence**  
   Given ReportPortal contains test results for Stories 3.2, 4.1, and 4.3 across multiple services  
   When the `ReportPortalQueryClient` runs with a query for all stories  
   Then it returns `stories.json` with summary data for each story.

2. **Generate index.html**  
   Given `stories.json` with 3 stories  
   When the static site generator runs  
   Then it produces `index.html` listing all 3 stories with their services, last run date, and status badges.

3. **Generate story detail page**  
   Given `story-3-2.json` with detailed evidence for Story 3.2  
   When the static site generator runs  
   Then it produces `story-3-2.html` with story description, acceptance criteria, test scenarios, and per-service test results.

4. **Deploy to GitHub Pages**  
   Given generated static site files in `/site`  
   When the GH Pages workflow runs  
   Then it publishes the site to the `gh-pages` branch and the dashboard is accessible via the public URL.

5. **Dashboard shows multi-service coverage**  
   Given a story with `servicesInvolved = [frontend, gateway, backend]`  
   When viewing that story's detail page  
   Then it displays separate test result tables for frontend, gateway, and backend, each with pass/fail counts and links.

## 🛠 Implementation Guidance
- Implement `ReportPortalQueryClient` as a Node.js or Python script that calls RP's REST API.
- Use a simple templating engine (Handlebars, Mustache, or plain template literals) for HTML generation.
- Keep CSS minimal and inline or in a single stylesheet for portability.
- For GitHub Pages, use GitHub Actions with a standard publish workflow (checkout, build, deploy to gh-pages).
- Document the dashboard structure and how to regenerate it locally.

## 📦 Deliverables
- `ReportPortalQueryClient` implementation (Node.js or Python).
- Static site generator scripts (reads JSON, outputs HTML).
- HTML templates for `index.html` and `story-{id}.html`.
- CSS stylesheet for dashboard styling.
- GitHub Actions workflow for deploying to GitHub Pages.
- Documentation on running the export/generation pipeline locally and accessing the dashboard.

## ⏭ Dependencies / Links
- Depends on Story 20.8 (ReportPortal Evidence Integration) for populated evidence data.
- Feeds into Story 20.10 (CI/CD Integration) which automates the export and publish process.
