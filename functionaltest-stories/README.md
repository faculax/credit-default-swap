# Functional Test Epics & Stories

This folder mirrors `user-stories` providing exhaustive automated functional test definitions spanning the full stack:

Layers:
- FE E2E: Playwright user journeys (accessibility + performance smoke)
- API Integration: REST-assured / Spring Boot Test (gateway + backend services)
- Contract: Schema & backward compatibility (JSON models, headers)
- Engine: Risk & analytics numerical correctness (golden baseline drift tests)
- Data: Deterministic seed & synthetic dataset provisioning
- Performance Smoke: Quick latency & throughput guard-rails (separate from load tests)

Tagging Convention (prefix with @ in code specs):
- @EPIC_XX (e.g. @EPIC_03)
- @STORY_X_Y (e.g. @STORY_3_1)
- Domain: @TRADE @CREDIT_EVENT @RISK @MARGIN @REFDATA @REPORTING @BONDS @BASKET
- Quality: @NEGATIVE @SECURITY @ACCESSIBILITY @PERFORMANCE @REGRESSION @DRIFT @RESILIENCE
- Priority: @CRITPATH @HIGH @MEDIUM @LOW

Scenario ID Format: `FT-<epic>-<story>-NNN` (e.g. FT-3-1-007) ensuring uniqueness. Non-functional variants use suffixes: `-PERF`, `-A11Y`, `-DRIFT`.

Automation Mapping:
- Playwright specs: `frontend/tests/e2e/<epic>/story_<story>.spec.ts`
- Java integration: `backend/src/test/java/.../it/<Epic>/<Story>IT.java`
- Risk engine regression: `risk-engine/src/test/resources/baselines` & `risk-engine/src/test/java/.../Regression*Test.java`

Execution Profiles:
- `smoke`: critical path subset (tags: @CRITPATH)
- `full-ci`: all except long-running simulations & Monte Carlo high path counts
- `nightly`: includes Monte Carlo full path counts, drift checks, extended performance

See `traceability-matrix.md` for mapping.
