# Story 3.4 - Merge Multiple Backend Service Results During CI

**As a** CI engineer  
**I want** backend Allure result directories merged into a single artifact during pipeline runs  
**So that** stakeholders see a consolidated backend quality view for every build.

## Acceptance Criteria
- CI workflow step collects Allure results from each backend service job and merges them into one archive.
- Merge process preserves service labels and avoids overwriting attachments with identical names.
- Failure handling implemented when a service produces no results or outputs malformed data.
- Consolidated artifact uploaded for PR and main branch builds with descriptive naming.
- Pipeline documentation updated showing artifact structure and retrieval instructions.

## Implementation Guidance
- Use Allure CLI `allure generate` or `allure merge` commands, or custom script to combine directories.
- Include metadata file indicating services included and timestamp of merge.
- Consider parallelism vs serialization trade-offs when merging to limit pipeline time.

## Testing Strategy
- Pipeline dry run verifying merge step succeeds with two or more backend services.
- Automated unit tests on merge script handling collision and missing directory scenarios.
- Manual Allure report review ensuring service filters work post-merge.

## Dependencies
- Depends on standardized result directories from Story 3.2.
