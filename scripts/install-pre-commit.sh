#!/usr/bin/env bash
set -euo pipefail

if ! command -v pre-commit >/dev/null 2>&1; then
  echo "Installing pre-commit (pip)..."
  python -m pip install --user pre-commit
fi

echo "Installing git hooks..."
python -m pre_commit install || pre-commit install
echo "Pre-commit installed. Run 'pre-commit run --all-files' to evaluate current repo."
