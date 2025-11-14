# Story 8.1 - Provide Unified CLI for Local Test Runs With Allure Output

**As a** developer  
**I want** a single command to run backend and frontend tests locally and open the Allure report  
**So that** I can validate my changes quickly before pushing.

## Acceptance Criteria
- CLI script or npm/maven wrapper runs both backend and frontend suites sequentially or in parallel.
- Command cleans previous artifacts, executes tests, merges Allure results, and serves HTML report locally.
- Exit code reflects combined success or failure of all suites.
- Script supports optional flags to run subsets (backend only, frontend only, E2E only).
- README updated with installation instructions and examples for PowerShell, Bash, and zsh.

## Implementation Guidance
- Implement wrapper in Node or Python for cross-platform compatibility.
- Reuse existing scripts from Epics 03, 04, and 06 to avoid duplication.
- Provide configuration file for customizing behavior (paths, concurrency) if needed.

## Testing Strategy
- Run CLI on Windows and macOS verifying it produces merged Allure report and exits correctly.
- Automated smoke test in CI to ensure CLI remains functional over time.
- Collect developer feedback during pilot and iterate before general release.

## Dependencies
- Requires Allure integration across services (Epics 03 and 04) and merge utilities (Epic 05).
