#!/bin/bash
set -e

echo "=== ORE Integration Verification ==="

# Check if ORE binary exists and is executable
echo "1. Checking for ORE binary..."
if command -v ore >/dev/null 2>&1; then
    echo "✓ ORE binary found in PATH: $(which ore)"
else
    echo "⚠ ORE not found in PATH, checking common locations..."
    for path in /usr/local/bin/ore /opt/ore/bin/ore /usr/bin/ore; do
        if [ -x "$path" ]; then
            echo "✓ ORE binary found at: $path"
            export PATH="$(dirname $path):$PATH"
            break
        fi
    done
fi

# Test ORE binary
echo "2. Testing ORE binary..."
if command -v ore >/dev/null 2>&1; then
    echo "✓ ORE binary is executable"
    ore --help 2>/dev/null || echo "⚠ ORE --help failed, but binary exists"
else
    echo "✗ ORE binary not found or not executable"
    exit 1
fi

# Check Java installation
echo "3. Checking Java installation..."
if command -v java >/dev/null 2>&1; then
    echo "✓ Java found: $(java -version 2>&1 | head -n1)"
else
    echo "✗ Java not found"
    exit 1
fi

# Test working directory
echo "4. Testing ORE working directory..."
mkdir -p "${ORE_WORK_DIR:-/tmp/ore-work}"
echo "✓ ORE working directory: ${ORE_WORK_DIR:-/tmp/ore-work}"

# Test configuration
echo "5. Testing ORE configuration..."
if [ -f "${ORE_CONFIG_PATH:-/app/ore/config/ore.xml}" ]; then
    echo "✓ ORE config found: ${ORE_CONFIG_PATH:-/app/ore/config/ore.xml}"
else
    echo "⚠ ORE config not found at: ${ORE_CONFIG_PATH:-/app/ore/config/ore.xml}"
fi

echo "=== Verification Complete ==="