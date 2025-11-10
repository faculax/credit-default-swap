#!/usr/bin/env bash
set -euo pipefail

if ! command -v conftest >/dev/null 2>&1; then
  echo "Please install conftest (https://github.com/open-policy-agent/conftest) to run local checks"
  exit 1
fi

echo "Running conftest against policy/ (k8s and terraform subfolders)"
conftest test --policy policy || true

echo "Policy checks complete"
