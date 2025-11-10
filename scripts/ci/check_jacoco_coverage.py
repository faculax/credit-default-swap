#!/usr/bin/env python3
"""
Aggregate JaCoCo xml reports and check LINE coverage against a threshold.
Usage: python scripts/ci/check_jacoco_coverage.py <threshold>
"""
import glob
import xml.etree.ElementTree as ET
import sys


def main():
    if len(sys.argv) < 2:
        print('Usage: check_jacoco_coverage.py <threshold_percent>')
        sys.exit(2)
    try:
        threshold = float(sys.argv[1])
    except ValueError:
        print('Threshold must be a number')
        sys.exit(2)

    covered = 0
    missed = 0
    for path in glob.glob('**/jacoco.xml', recursive=True):
        try:
            tree = ET.parse(path)
        except Exception:
            continue
        root = tree.getroot()
        for c in root.findall('.//counter'):
            if c.attrib.get('type') == 'LINE':
                covered += int(c.attrib.get('covered', 0))
                missed += int(c.attrib.get('missed', 0))

    total = covered + missed
    if total == 0:
        print('No JaCoCo LINE coverage data found; skipping coverage check')
        sys.exit(0)

    pct = (covered / total) * 100.0
    print(f'LINE coverage: {pct:.2f}% ({covered}/{total})')
    if pct < threshold:
        print(f'Coverage {pct:.2f}% below threshold {threshold}%')
        sys.exit(1)

    print('JaCoCo LINE coverage check passed')


if __name__ == '__main__':
    main()
