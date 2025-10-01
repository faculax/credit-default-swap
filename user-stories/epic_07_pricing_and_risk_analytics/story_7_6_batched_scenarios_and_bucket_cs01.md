# Story 7.6 – Batched Scenarios & Bucket CS01 (Phase C)

## Narrative
Enable batched scenario evaluation and bucket-level CS01 calculations leveraging the long-lived ORE process with minimal incremental latency.

## Acceptance Criteria
- Placeholder – detailed acceptance to be refined during implementation.

## Implementation Notes
- Introduce scenario batching window (configurable ms) accumulating requests before single ORE submission.
- Add bucket CS01: compute sensitivity per curve bucket (tenor or maturity segment) and aggregate.
- Extend API: optional query/POST parameter `includeBucketCs01=true` returns bucket vector.
- Metrics: record batch size distribution & latency improvements.
- Caching: reuse base curve objects across batch to avoid rebuild.

## Test Scenarios (Provisional)
- Batch of N>1 scenario sets processed in single ORE invocation (assert latency < sum of individual).
- Bucket CS01 vector length matches configured bucket set.
- Parallel vs bucket CS01 sum within tolerance of aggregate CS01.

## UI / UX Acceptance (Provisional)
- ScenarioRunModal optionally displays bucket CS01 breakdown table (hidden until implemented flag active).
- RiskMeasuresPanel may show aggregate CS01 unchanged.

## Traceability
Epic: epic_07_pricing_and_risk_analytics
Story ID: 7.6
