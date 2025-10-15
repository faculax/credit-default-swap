#!/bin/bash
# filepath: /Users/facundolaxalde/Development/credit-default-swap/integration-tests/test-recovery-rate-impact.sh
# Integration test: Verify that recovery rate impacts NPV calculation
# Creates two identical CDS trades with different recovery rates and verifies NPVs are different

set -eo pipefail

# Color codes for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;36m'
NC='\033[0m' # No Color

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# URLs
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
RISK_ENGINE_URL="${RISK_ENGINE_URL:-http://localhost:8082}"
TRADE_API="${BACKEND_URL}/api/cds-trades"
RISK_API="${RISK_ENGINE_URL}/api/risk"

# Trade IDs
TRADE_ID_LOW_RECOVERY=""
TRADE_ID_HIGH_RECOVERY=""

# Helper functions
print_header() {
    printf "\n${BLUE}================================${NC}\n"
    printf "${BLUE}%s${NC}\n" "$1"
    printf "${BLUE}================================${NC}\n\n"
}

print_success() {
    printf "${GREEN}✓ %s${NC}\n" "$1"
    ((TESTS_PASSED+=1))
}

print_error() {
    printf "${RED}✗ %s${NC}\n" "$1"
    ((TESTS_FAILED+=1))
}

print_info() {
    printf "${YELLOW}ℹ %s${NC}\n" "$1"
}

# Verify backend is running
check_backend() {
    print_header "Checking Backend Availability"
    
    if curl -s -f "${BACKEND_URL}/actuator/health" > /dev/null 2>&1; then
        print_success "Backend is running at ${BACKEND_URL}"
    else
        print_error "Backend is not responding at ${BACKEND_URL}"
        echo "Please ensure the backend is running before running this test."
        exit 1
    fi
}

check_risk_engine() {
    print_header "Checking Risk Engine Availability"
    
    if curl -s -f "${RISK_API}/health" > /dev/null 2>&1; then
        print_success "Risk engine is running at ${RISK_ENGINE_URL}"
    else
        print_error "Risk engine is not responding at ${RISK_ENGINE_URL}"
        echo "Please ensure the risk engine is running before running this test."
        exit 1
    fi
}

# Create a CDS trade with specified recovery rate
create_trade_with_recovery() {
    local recovery_rate=$1
    local label=$2
    
    print_header "Creating CDS Trade with ${recovery_rate}% Recovery Rate (${label})"
    
    # Use fixed trade parameters (same for both trades except recovery rate)
    local trade_date="2023-06-20"
    local effective_date="2023-06-21"
    local maturity_date="2028-06-20"
    
    local trade_payload=$(cat <<EOF
{
  "referenceEntity": "AAPL",
  "counterparty": "JPMORGAN",
  "notionalAmount": 10000000,
  "currency": "USD",
  "spread": 100,
  "recoveryRate": ${recovery_rate},
  "tradeDate": "${trade_date}",
  "effectiveDate": "${effective_date}",
  "accrualStartDate": "${effective_date}",
  "maturityDate": "${maturity_date}",
  "premiumFrequency": "QUARTERLY",
  "dayCountConvention": "ACT_360",
  "buySellProtection": "BUY",
  "tradeStatus": "ACTIVE",
  "paymentCalendar": "NYC",
  "restructuringClause": ""
}
EOF
)

    print_info "Trade payload (recovery rate: ${recovery_rate}%):"
    echo "$trade_payload" | jq . || echo "$trade_payload"

    local response=$(curl -s -w "\n%{http_code}" -X POST "${TRADE_API}" \
        -H "Content-Type: application/json" \
        -d "$trade_payload")

    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    local trade_id=$(echo "$body" | jq -r '.id // empty' 2>/dev/null || echo "")

    if [[ -n "$trade_id" ]] && [[ "$http_code" == "201" ]]; then
        print_success "Trade created with ID: ${trade_id} (HTTP ${http_code})"
        print_info "Reference: AAPL, Notional: \$10,000,000, Spread: 100 bps, Recovery: ${recovery_rate}%"
        echo "$body" | jq . 2>/dev/null || echo "$body"
        
        # Store trade ID for later use
        if [[ "$label" == "low recovery" ]]; then
            TRADE_ID_LOW_RECOVERY=$trade_id
        else
            TRADE_ID_HIGH_RECOVERY=$trade_id
        fi
        
        return 0
    else
        print_error "Failed to create trade (HTTP ${http_code})"
        echo "Response: $body"
        exit 1
    fi
}

# Calculate NPV for a trade
calculate_npv() {
    local trade_id=$1
    local label=$2
    
    print_header "Calculating NPV for Trade ${trade_id} (${label})"
    
    local valuation_date=$(date -u +"%Y-%m-%d")
    local scenario_id="recovery-test-${trade_id}"
    
    local scenario_request=$(cat <<EOF
{
  "scenarioId": "${scenario_id}",
  "tradeIds": [${trade_id}],
  "valuationDate": "${valuation_date}",
  "scenarios": {}
}
EOF
)
    
    print_info "Calculating risk with valuation date: ${valuation_date}"
    
    local response=$(curl -s -X POST "${RISK_API}/scenario/calculate" \
        -H "Content-Type: application/json" \
        -d "$scenario_request")
    
    local first_result=$(echo "$response" | jq '.[0] // empty')
    
    if [[ -z "$first_result" || "$first_result" == "null" ]]; then
        print_error "Risk calculation failed for ${label}"
        echo "$response"
        exit 1
    fi
    
    local npv=$(echo "$first_result" | jq -r '.npv // empty')
    local fair_spread=$(echo "$first_result" | jq -r '.fairSpreadClean // empty')
    local protection_leg=$(echo "$first_result" | jq -r '.protectionLegNPV // empty')
    local premium_leg=$(echo "$first_result" | jq -r '.premiumLegNPVClean // empty')
    
    print_success "NPV: ${npv}"
    print_info "Fair Spread Clean: ${fair_spread} bps"
    print_info "Protection Leg NPV: ${protection_leg}"
    print_info "Premium Leg NPV: ${premium_leg}"
    
    echo "$first_result" | jq '{npv, fairSpreadClean, protectionLegNPV, premiumLegNPVClean}'
    
    # Store values for comparison (both NPV and protection leg)
    if [[ "$label" == "low recovery" ]]; then
        NPV_LOW_RECOVERY=$npv
        PROTECTION_LEG_LOW_RECOVERY=$protection_leg
    else
        NPV_HIGH_RECOVERY=$npv
        PROTECTION_LEG_HIGH_RECOVERY=$protection_leg
    fi
}

# Verify NPVs are different
verify_npv_difference() {
    print_header "Verifying Recovery Rate Impact on NPV"
    
    if [[ -z "$PROTECTION_LEG_LOW_RECOVERY" || -z "$PROTECTION_LEG_HIGH_RECOVERY" ]]; then
        print_error "Missing Protection Leg NPV values for comparison"
        echo "Protection Leg NPV Low Recovery: ${PROTECTION_LEG_LOW_RECOVERY}"
        echo "Protection Leg NPV High Recovery: ${PROTECTION_LEG_HIGH_RECOVERY}"
        exit 1
    fi
    
    print_info "NPV with 20% Recovery: ${NPV_LOW_RECOVERY}"
    print_info "NPV with 60% Recovery: ${NPV_HIGH_RECOVERY}"
    print_info "Protection Leg NPV with 20% Recovery: ${PROTECTION_LEG_LOW_RECOVERY}"
    print_info "Protection Leg NPV with 60% Recovery: ${PROTECTION_LEG_HIGH_RECOVERY}"
    
    # Calculate Protection Leg difference (absolute value)
    local prot_diff=$(echo "$PROTECTION_LEG_HIGH_RECOVERY - $PROTECTION_LEG_LOW_RECOVERY" | bc -l)
    local prot_abs_diff=$(echo "$prot_diff" | sed 's/^-//')
    
    print_info "Protection Leg NPV Difference: ${prot_diff}"
    
    # Verify Protection Leg NPVs are different (at least 1% relative difference)
    # For buying protection: higher recovery should mean lower protection leg value
    # Protection buyer expects to receive (1 - R) * Notional on default
    # So higher R means lower expected payout, lower protection leg value
    local threshold=0.01
    local prot_relative_diff=$(echo "scale=6; $prot_abs_diff / ($PROTECTION_LEG_LOW_RECOVERY + 0.0001)" | bc -l | sed 's/^-//')
    
    print_info "Protection Leg Relative difference: $(echo "scale=4; $prot_relative_diff * 100" | bc -l)%"
    
    if (( $(echo "$prot_relative_diff > $threshold" | bc -l) )); then
        print_success "Protection Leg NPVs are significantly different (>1% relative difference)"
        print_success "Recovery rate impacts Protection Leg NPV calculation as expected"
    else
        print_error "Protection Leg NPVs are too similar (difference: ${prot_relative_diff})"
        print_error "Recovery rate may not be impacting calculation correctly"
        exit 1
    fi
    
    # Verify direction: Higher recovery should mean lower protection leg value (buying protection)
    # Protection buyer: expects to receive (1 - R) * Notional on default
    # So higher R means lower expected payout, lower protection leg value
    if (( $(echo "$PROTECTION_LEG_HIGH_RECOVERY < $PROTECTION_LEG_LOW_RECOVERY" | bc -l) )); then
        print_success "Direction correct: Higher recovery (60%) → Lower Protection Leg NPV"
        print_success "This makes sense: higher recovery means less expected payout on default"
    else
        print_error "Direction incorrect: Higher recovery should mean LOWER Protection Leg NPV"
        print_error "Expected: Protection Leg (60% recovery) < Protection Leg (20% recovery)"
        exit 1
    fi
}

# Delete a CDS trade
delete_trade() {
    local trade_id=$1
    local label=$2
    
    print_info "Deleting trade ${trade_id} (${label})"
    
    local http_code=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "${TRADE_API}/${trade_id}")
    
    if [[ "$http_code" == "204" ]] || [[ "$http_code" == "200" ]]; then
        print_success "Trade ${trade_id} deleted successfully"
    else
        print_error "Failed to delete trade ${trade_id} (HTTP ${http_code})"
    fi
}

# Cleanup function
cleanup() {
    print_header "Cleanup - Deleting Test Trades"
    
    if [[ -n "$TRADE_ID_LOW_RECOVERY" ]]; then
        delete_trade "$TRADE_ID_LOW_RECOVERY" "low recovery"
    fi
    
    if [[ -n "$TRADE_ID_HIGH_RECOVERY" ]]; then
        delete_trade "$TRADE_ID_HIGH_RECOVERY" "high recovery"
    fi
}

# Main test flow
main() {
    print_header "Recovery Rate Impact Integration Test"
    print_info "Backend URL: ${BACKEND_URL}"
    print_info "Risk Engine URL: ${RISK_ENGINE_URL}"
    
    check_backend
    check_risk_engine
    
    # Create two identical trades with different recovery rates
    print_info "Creating two identical CDS trades with different recovery rates..."
    create_trade_with_recovery 20 "low recovery"
    sleep 1
    create_trade_with_recovery 60 "high recovery"
    sleep 1
    
    # Calculate NPV for both trades
    calculate_npv "$TRADE_ID_LOW_RECOVERY" "low recovery"
    sleep 1
    calculate_npv "$TRADE_ID_HIGH_RECOVERY" "high recovery"
    
    # Verify NPVs are different
    verify_npv_difference
    
    # Cleanup
    cleanup
    
    # Summary
    print_header "Test Summary"
    printf "${GREEN}Tests Passed: ${TESTS_PASSED}${NC}\n"
    printf "${RED}Tests Failed: ${TESTS_FAILED}${NC}\n"
    
    if [[ $TESTS_FAILED -eq 0 ]]; then
        printf "\n${GREEN}🎉 All tests passed! Recovery rate impacts NPV correctly.${NC}\n\n"
        exit 0
    else
        printf "\n${RED}❌ Some tests failed${NC}\n\n"
        exit 1
    fi
}

# Run main function
main "$@"
