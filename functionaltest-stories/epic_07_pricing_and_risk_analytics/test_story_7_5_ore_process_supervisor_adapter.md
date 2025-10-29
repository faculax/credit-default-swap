# Functional Test Story 7.5 – ORE Process Supervisor & Adapter

Trace: story_7_5_ore_process_supervisor_and_adapter
Tags: @EPIC_07 @STORY_7_5 @RISK @PROCESS

## Objective
Validate robustness of external ORE process supervision, adapter communication, restart policies, and error handling.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-7-5-001 | Launch ORE process | Engine | @PROCESS |
| FT-7-5-002 | Heartbeat detection | Engine | @HEARTBEAT |
| FT-7-5-003 | Unexpected exit triggers restart | Engine | @RESILIENCE |
| FT-7-5-004 | Restart limit reached -> circuit open | Engine | @RESILIENCE |
| FT-7-5-005 | Graceful shutdown sequence | Engine | @PROCESS |
| FT-7-5-006 | Zombie process detection | Engine | @RESILIENCE |
| FT-7-5-007 | Adapter request/response schema validation | Engine | @CONTRACT |
| FT-7-5-008 | Timeout on ORE response triggers fallback | Engine | @RESILIENCE |
| FT-7-5-009 | Parallel requests queued respecting limit | Engine | @CONCURRENCY |
| FT-7-5-010 | Metrics (activeRequests) exposed | Engine | @METRICS |
| FT-7-5-011 | Log redaction of command line args | Engine | @SECURITY |
| FT-7-5-012 | Crash during calculation cleans temp files | Engine | @CLEANUP |
| FT-7-5-013 | Health endpoint reflects degraded state | API | @OBSERVABILITY |
| FT-7-5-014 | Manual restart endpoint authorized only | API | @SECURITY |
| FT-7-5-015 | Config reload without downtime | Engine | @CONFIG |
| FT-7-5-016 | CPU spike detection alert | Engine | @PERFORMANCE |
| FT-7-5-017 | Memory leak detection threshold | Engine | @MEMORY |
| FT-7-5-018 | Circuit half-open probe success closes | Engine | @RESILIENCE |
| FT-7-5-019 | Circuit half-open failure reopens | Engine | @RESILIENCE |
| FT-7-5-020 | Telemetry span attributes set | Engine | @TELEMETRY |
| FT-7-5-021 | Observability traces propagate IDs | Engine | @TELEMETRY |
| FT-7-5-022 | Security: unauthorized ORE binary path blocked | Engine | @SECURITY |

## Automation Strategy
Process simulation harness; mock ORE binary or wrapper script injecting failures.
