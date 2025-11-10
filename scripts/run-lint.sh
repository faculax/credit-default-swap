#!/usr/bin/env bash
set -euo pipefail

# Run language-specific linters in a quota-friendly, local fashion.
# This script is intentionally conservative: it runs a subset of linters that are quick
# and can be cached. CI uses the full super-linter.

echo "Running shellcheck..."
if command -v shellcheck >/dev/null 2>&1; then
  find . -path ./target -prune -o -name '*.sh' -print | xargs shellcheck || true
else
  echo "shellcheck not installed; skipping"
fi

echo "Running markdownlint..."
if command -v markdownlint >/dev/null 2>&1; then
  markdownlint README.md || true
else
  echo "markdownlint not installed; skipping"
fi

echo "Frontend lint (if node_modules exist)..."
if [ -d frontend ] && [ -f frontend/package.json ]; then
  if command -v npm >/dev/null 2>&1; then
    pushd frontend >/dev/null
    if [ -f package.json ]; then
      # If repo has eslint configured, run it; otherwise skip
      if grep -q "eslint" package.json >/dev/null 2>&1; then
        npm run lint || true
      else
        echo "No frontend eslint script; skipping"
      fi
    fi
    popd >/dev/null
  fi
fi

echo "Local lint pass complete. For CI parity use .github/workflows/lint.yml"
