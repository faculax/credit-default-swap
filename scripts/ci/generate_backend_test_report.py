#!/usr/bin/env python3
"""
Generate a backend test classification markdown report.
Classification heuristics:
- Contract: classname contains 'Contract'
- Integration: classname contains 'Integration' OR endswith 'IT'
- Unit: other test classes ending with 'Test'
Counts are by individual test methods (testcases in surefire XML).
Also computes line coverage percent from JaCoCo reports per module.
Outputs: Backend_TEST_REPORT.md at repository root.
"""
import os
import re
import xml.etree.ElementTree as ET
from pathlib import Path
from datetime import datetime, timezone

MODULES = ["backend", "gateway", "risk-engine"]
REPO_ROOT = Path(__file__).resolve().parents[2]
OUTPUT_PATH = REPO_ROOT / "Backend_TEST_REPORT.md"

# Data structures
unit_tests = 0
contract_tests = 0
integration_tests = 0
passed = 0
failed = 0
skipped = 0
module_breakdown = {m: {"unit":0, "contract":0, "integration":0, "passed":0, "failed":0, "skipped":0} for m in MODULES}
coverage_info = {}

TAG_PATTERN = re.compile(r"@Tag\(\s*\"(unit|contract|integration)\"\s*\)")
tag_map = {}

def scan_tags():
    for module in MODULES:
        test_src = REPO_ROOT / module / "src" / "test" / "java"
        if not test_src.exists():
            continue
        for java_file in test_src.rglob("*.java"):
            try:
                content = java_file.read_text(encoding="utf-8", errors="ignore")
            except Exception:
                continue
            m = TAG_PATTERN.search(content)
            if m:
                # Extract simple class name
                class_match = re.search(r"class\s+(\w+)", content)
                if class_match:
                    tag_map[class_match.group(1)] = m.group(1)

def classify(classname: str) -> str:
    if not classname:
        return "unit"
    simple = classname.split('.')[-1]
    if simple in tag_map:
        return tag_map[simple]
    if "Contract" in simple:
        return "contract"
    if "Integration" in simple or simple.endswith("IT"):
        return "integration"
    if simple.endswith("Test"):
        return "unit"
    return "unit"

scan_tags()

for module in MODULES:
    reports_dir = REPO_ROOT / module / "target" / "surefire-reports"
    if not reports_dir.exists():
        continue
    for xml_file in reports_dir.glob("*.xml"):
        try:
            tree = ET.parse(xml_file)
            root = tree.getroot()
        except ET.ParseError:
            continue
        # Each <testcase>
        for tc in root.findall('.//testcase'):
            classname = tc.get('classname') or ''
            category = classify(classname.split('.')[-1])
            has_failure = tc.find('failure') is not None
            has_error = tc.find('error') is not None
            is_skipped = tc.find('skipped') is not None
            if category == 'contract':
                contract_tests += 1
                module_breakdown[module]['contract'] += 1
            elif category == 'integration':
                integration_tests += 1
                module_breakdown[module]['integration'] += 1
            else:
                unit_tests += 1
                module_breakdown[module]['unit'] += 1
            if is_skipped:
                skipped += 1
                module_breakdown[module]['skipped'] += 1
            elif has_failure or has_error:
                failed += 1
                module_breakdown[module]['failed'] += 1
            else:
                passed += 1
                module_breakdown[module]['passed'] += 1
    # Coverage
    jacoco_xml = REPO_ROOT / module / "target" / "site" / "jacoco" / "jacoco.xml"
    if jacoco_xml.exists():
        try:
            tree = ET.parse(jacoco_xml)
            root = tree.getroot()
            line_counter = root.find(".//counter[@type='LINE']")
            if line_counter is not None:
                missed = int(line_counter.get('missed'))
                covered = int(line_counter.get('covered'))
                total = missed + covered
                pct = (covered * 100.0 / total) if total else 0.0
                coverage_info[module] = {"covered": covered, "missed": missed, "total": total, "pct": pct}
        except ET.ParseError:
            pass

# Use timezone-aware UTC time
# Robust UTC timestamp generation supporting older Python where attribute access may differ
try:
    now = datetime.now(timezone.utc).isoformat()
except Exception:
    # Fallback: naive UTC timestamp using fromtimestamp
    now = datetime.fromtimestamp(datetime.now().timestamp(), timezone.utc).isoformat()

lines = []
lines.append("## Backend Test Report")
lines.append("")
lines.append(f"Generated: {now}")
lines.append("")
# Summary table
lines.append("### Summary")
lines.append("")
lines.append("| Metric | Value |")
lines.append("|--------|-------|")
lines.append(f"| Total Tests | {unit_tests + contract_tests + integration_tests} |")
lines.append(f"| Passed | {passed} |")
lines.append(f"| Failed | {failed} |")
lines.append(f"| Skipped | {skipped} |")
lines.append(f"| Unit Tests | {unit_tests} |")
lines.append(f"| Contract Tests | {contract_tests} |")
lines.append(f"| Integration Tests | {integration_tests} |")
lines.append("")

# Per-module breakdown
lines.append("### Per-Module Breakdown")
lines.append("")
lines.append("| Module | Unit | Contract | Integration | Passed | Failed | Skipped |")
lines.append("|--------|------|----------|-------------|-------:|-------:|--------:|")
for m,b in module_breakdown.items():
    total_module = b['unit'] + b['contract'] + b['integration']
    if total_module == 0:
        continue
    lines.append(f"| {m} | {b['unit']} | {b['contract']} | {b['integration']} | {b['passed']} | {b['failed']} | {b['skipped']} |")
lines.append("")

# Coverage
lines.append("### Coverage (Line)")
lines.append("")
lines.append("| Module | Coverage % | Covered | Missed | Total |")
lines.append("|--------|-----------:|--------:|-------:|------:|")
for m, info in coverage_info.items():
    lines.append(f"| {m} | {info['pct']:.2f}% | {info['covered']} | {info['missed']} | {info['total']} |")
lines.append("")

lines.append("### Classification Rules")
lines.append("- Contract: classname contains 'Contract'")
lines.append("- Integration: classname contains 'Integration' or ends with 'IT'")
lines.append("- Unit: all remaining classes ending with 'Test'")
lines.append("")

lines.append("### Next Improvements")
lines.append("- Add negative path tests for exception handling branches.")
lines.append("- Increase integration coverage for cross-module orchestration.")
lines.append("- Add contract tests for external API adapters if missing.")
lines.append("- Consider tagging tests with @Tag to refine classification later.")
lines.append("")

OUTPUT_PATH.write_text("\n".join(lines), encoding="utf-8")
print(f"Backend test report written to {OUTPUT_PATH}")
