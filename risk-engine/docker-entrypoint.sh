#!/usr/bin/env bash
set -euo pipefail

echo "[entrypoint] Starting risk-engine with ORE auto-detect"

# If user wants to run ORE directly (bypassing Spring), they can specify: docker run <img> ore <config>
if [ "${1:-}" = "ore" ]; then
  shift || true
  if [ $# -eq 0 ]; then
    echo "[entrypoint] ERROR: 'ore' mode requires a config file path argument" >&2
    exit 1
  fi
  CONFIG_FILE="$1"; shift || true
  # Allow optional additional args after config file
  ORE_BIN="${ORE_BINARY_PATH:-/usr/local/bin/ore}"
  if [ ! -x "$ORE_BIN" ]; then
    echo "[entrypoint] ERROR: ORE binary not found at $ORE_BIN" >&2
    exit 1
  fi
  echo "[entrypoint] Direct ORE invocation: $ORE_BIN $CONFIG_FILE $*"
  exec "$ORE_BIN" "$CONFIG_FILE" "$@"
fi

# Candidate locations (include config default /app/ore/bin/ore and expected /usr/local/bin/ore)
CANDIDATES=( \
  "${ORE_BINARY_PATH:-}" \
  /usr/local/bin/ore \
  /usr/bin/ore \
  /opt/ore/bin/ore \
  /app/ore/bin/ore \
)

SELECTED=""
for c in "${CANDIDATES[@]}"; do
  if [ -n "$c" ] && [ -x "$c" ]; then
    SELECTED="$c"
    break
  fi
done

if [ -z "$SELECTED" ]; then
  echo "[entrypoint] WARNING: No ORE binary found, falling back to STUB implementation" >&2
  export RISK_IMPL=STUB
else
  echo "[entrypoint] Using ORE binary at: $SELECTED"
  export RISK_IMPL=ORE
  export ORE_BINARY_PATH="$SELECTED"
fi

# Allow alternate config via ORE_CONFIG_FILE (maps to spring property risk.ore.config-path)
if [ -n "${ORE_CONFIG_FILE:-}" ]; then
  export ORE_CONFIG_PATH="$ORE_CONFIG_FILE"
fi

echo "[entrypoint] RISK_IMPL=$RISK_IMPL"
echo "[entrypoint] ORE_CONFIG_PATH=${ORE_CONFIG_PATH:-/app/ore/config/ore.xml}"

# Ensure work dir exists
mkdir -p "${ORE_WORK_DIR:-/tmp/ore-work}" || true

# Pass through any additional args to the JVM (so user can run health checks or debugging)
exec java ${JAVA_OPTS:-} -jar /app/risk-engine.jar "$@"
