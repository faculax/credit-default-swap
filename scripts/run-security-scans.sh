#!/usr/bin/env bash
set -euo pipefail

echo "Running local gitleaks..."
if command -v gitleaks >/dev/null 2>&1; then
  gitleaks detect --source . || true
else
  echo "gitleaks not installed; see https://github.com/zricethezav/gitleaks for install"
fi

echo "Running local semgrep..."
if command -v semgrep >/dev/null 2>&1; then
  semgrep --config auto || true
else
  echo "semgrep not installed; pip install semgrep"
fi

echo "Local security scans complete. Use CI for enforced blocking scans."
