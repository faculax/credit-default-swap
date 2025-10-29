# Functional Test Story 13.19 – Multi-Asset Support

Trace: story_13_19_multi-asset-support
Tags: @EPIC_13 @SIMULATION @MULTI_ASSET

## Objective
Validate simulation handles multi-asset portfolios (CDS, bonds) with correct aggregation and currency conversion.

## Scenario Index
| ID | Scenario | Layer | Tags |
|----|----------|-------|------|
| FT-13-19-001 | Submit multi-asset run | API | @POSITIVE |
| FT-13-19-002 | Asset type segmentation metrics | Engine | @METRICS |
| FT-13-19-003 | Currency conversion correct | Engine | @NUMERIC |
| FT-13-19-004 | Unauthorized submission -> 403 | API | @SECURITY |
| FT-13-19-005 | Drift multi-asset aggregation baseline | Engine | @DRIFT |
| FT-13-19-006 | Logging redacts instrument IDs | API | @SECURITY |
| FT-13-19-007 | API contract multi-asset schema | Contract | @CONTRACT |
| FT-13-19-008 | Performance multi-asset runtime | Engine | @PERFORMANCE |
| FT-13-19-009 | Accessibility multi-asset UI labels | E2E | @ACCESSIBILITY |
| FT-13-19-010 | Concurrency multi-asset runs stable | Engine | @CONCURRENCY |
| FT-13-19-011 | Edge single asset degenerates gracefully | Engine | @EDGE |
| FT-13-19-012 | Metrics assetTypeBreakdown exported | Engine | @METRICS |

## Automation Strategy
Run with baseline portfolio JSON; check conversions; metrics segmentation.
