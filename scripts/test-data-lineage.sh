#!/bin/bash
set -e

echo "=== Testing Data Lineage API ==="
echo ""

# Configuration
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
LINEAGE_ENDPOINT="${BACKEND_URL}/api/lineage"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Backend URL: ${BACKEND_URL}${NC}"
echo ""

# Test 1: Ingest a lineage event
echo -e "${GREEN}Test 1: Ingest lineage event (ETL transform)${NC}"
RESPONSE=$(curl -s -X POST "${LINEAGE_ENDPOINT}" \
  -H "Content-Type: application/json" \
  -d '{
    "dataset": "cds_trades_cleaned",
    "operation": "ETL_TRANSFORM",
    "inputs": {
      "raw_trades": "cds_trades_raw",
      "reference_data": "issuer_master"
    },
    "outputs": {
      "cleaned_trades": "cds_trades_cleaned",
      "rejected_trades": "cds_trades_rejected"
    },
    "userName": "etl-pipeline",
    "runId": "run-2025-11-10-001"
  }')

echo "Response: ${RESPONSE}"
echo ""

# Extract the ID from response (requires jq)
if command -v jq &> /dev/null; then
  EVENT_ID=$(echo "${RESPONSE}" | jq -r '.id')
  echo -e "${BLUE}Created event ID: ${EVENT_ID}${NC}"
  echo ""
fi

# Test 2: Ingest another event in the same run
echo -e "${GREEN}Test 2: Ingest lineage event (aggregation)${NC}"
curl -s -X POST "${LINEAGE_ENDPOINT}" \
  -H "Content-Type: application/json" \
  -d '{
    "dataset": "cds_portfolio_summary",
    "operation": "AGGREGATION",
    "inputs": {
      "cleaned_trades": "cds_trades_cleaned",
      "market_data": "market_quotes"
    },
    "outputs": {
      "summary": "cds_portfolio_summary"
    },
    "userName": "etl-pipeline",
    "runId": "run-2025-11-10-001"
  }' | jq '.'
echo ""

# Test 3: Query lineage by dataset
echo -e "${GREEN}Test 3: Query lineage by dataset (cds_trades_cleaned)${NC}"
curl -s "${LINEAGE_ENDPOINT}?dataset=cds_trades_cleaned" | jq '.'
echo ""

# Test 4: Query lineage by run ID
echo -e "${GREEN}Test 4: Query lineage by run ID (run-2025-11-10-001)${NC}"
curl -s "${LINEAGE_ENDPOINT}/run/run-2025-11-10-001" | jq '.'
echo ""

# Test 5: Ingest a migration lineage event
echo -e "${GREEN}Test 5: Ingest lineage event (Flyway migration)${NC}"
curl -s -X POST "${LINEAGE_ENDPOINT}" \
  -H "Content-Type: application/json" \
  -d '{
    "dataset": "cds_trades",
    "operation": "SCHEMA_MIGRATION",
    "inputs": {
      "migration_script": "V42__add_collateral_fields.sql"
    },
    "outputs": {
      "table": "cds_trades",
      "columns_added": ["collateral_type", "collateral_amount"]
    },
    "userName": "flyway",
    "runId": "migration-v42"
  }' | jq '.'
echo ""

echo -e "${GREEN}=== All tests completed ===${NC}"
