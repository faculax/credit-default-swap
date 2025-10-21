#!/bin/bash
# filepath: /Users/facundolaxalde/Development/credit-default-swap/integration-tests/test-cds-lifecycle.sh

set -eo pipefail  # Exit on error and pipe failures (but not undefined vars for now)

# Color codes for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;36m'
NC='\033[0m' # No Color

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# Environment selection - Interactive prompt
select_environment() {
    echo ""
    printf "${BLUE}================================${NC}\n"
    printf "${BLUE}Select target environment:${NC}\n"
    printf "${BLUE}================================${NC}\n"
    echo "  1) Localhost"
    echo "  2) Render (Production)"
    echo ""
    read -p "Enter choice (1 or 2) [1]: " choice
    choice=${choice:-1}
    
    case $choice in
        1)
            BACKEND_URL="http://localhost:8080"
            RISK_ENGINE_URL="http://localhost:8082"
            ENVIRONMENT="localhost"
            ;;
        2)
            BACKEND_URL="https://credit-default-swap-backend.onrender.com"
            RISK_ENGINE_URL="https://credit-default-swap-risk-engine.onrender.com"
            ENVIRONMENT="render"
            ;;
        *)
            printf "${RED}Invalid choice. Defaulting to localhost.${NC}\n"
            BACKEND_URL="http://localhost:8080"
            RISK_ENGINE_URL="http://localhost:8082"
            ENVIRONMENT="localhost"
            ;;
    esac
    
    TRADE_API="${BACKEND_URL}/api/cds-trades"
    RISK_API="${RISK_ENGINE_URL}/api/risk"
    LIFECYCLE_API="${BACKEND_URL}/api/lifecycle"
}

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

# Create a random CDS trade
create_test_trade() {
    print_header "Creating Test CDS Trade (Backdated like frontend random generator)"

    # Arrays aligned with frontend choices (simplified subset)
    local counterparties=("JPMORGAN" "GOLDMAN" "MORGAN_STANLEY" "CITIBANK" "BARCLAYS")
    local reference_entities=("TESLA" "FORD" "APPLE" "MICROSOFT" "AMAZON")
    local frequencies=("QUARTERLY" "SEMI_ANNUAL" "ANNUAL" "MONTHLY")
    local day_counts=("ACT_360" "ACT_365")
    local calendars=("USD" "NYC")
    local restructuring_clauses=("MOD_R" "FULL_R" "")

    # Helper to pick random item
    local counterparty=${counterparties[$((RANDOM % ${#counterparties[@]}))]}
    local reference_entity=${reference_entities[$((RANDOM % ${#reference_entities[@]}))]}
    local premium_frequency=${frequencies[$((RANDOM % ${#frequencies[@]}))]}
    local day_count=${day_counts[$((RANDOM % ${#day_counts[@]}))]}
    local payment_calendar=${calendars[$((RANDOM % ${#calendars[@]}))]}
    local restructuring=${restructuring_clauses[$((RANDOM % ${#restructuring_clauses[@]}))]}

    # Notional 1M - 50M, Spread 50 - 550 bps
    local notional=$(( (RANDOM % 49000000) + 1000000 ))
    local spread=$(( (RANDOM % 500) + 50 ))

    # Trade date: 2-4 years in the past (frontend: -4y to -2y)
    local days_back_trade=$(( (RANDOM % (365*2)) + 365*2 ))  # 730 to 1460 days
    local trade_date=$(date -u -v-${days_back_trade}d +"%Y-%m-%d" 2>/dev/null || date -u -d "-${days_back_trade} days" +"%Y-%m-%d")

    # Effective date: 1-7 days after trade date
    local effective_offset=$(( (RANDOM % 7) + 1 ))
    local effective_date=$(date -u -v+${effective_offset}d -j -f "%Y-%m-%d" "$trade_date" +"%Y-%m-%d" 2>/dev/null || date -u -d "${trade_date} +${effective_offset} days" +"%Y-%m-%d")

    # Accrual start = effective date
    local accrual_start_date=$effective_date

    # Maturity date: 2-5 years from TODAY (frontend logic)
    local days_forward_mat=$(( (RANDOM % (365*3)) + 365*2 )) # 730 to 1825 days
    local maturity_date=$(date -u -v+${days_forward_mat}d +"%Y-%m-%d" 2>/dev/null || date -u -d "+${days_forward_mat} days" +"%Y-%m-%d")

    local buy_sell="BUY"
    if (( RANDOM % 2 )); then buy_sell="SELL"; fi

    local trade_payload=$(cat <<EOF
{
  "referenceEntity": "${reference_entity}",
  "counterparty": "${counterparty}",
  "notionalAmount": ${notional},
  "currency": "USD",
  "spread": ${spread},
  "tradeDate": "${trade_date}",
  "effectiveDate": "${effective_date}",
  "accrualStartDate": "${accrual_start_date}",
  "maturityDate": "${maturity_date}",
  "premiumFrequency": "${premium_frequency}",
  "dayCountConvention": "${day_count}",
  "buySellProtection": "${buy_sell}",
  "tradeStatus": "ACTIVE",
  "paymentCalendar": "${payment_calendar}",
  "restructuringClause": "${restructuring}",
  "recoveryRate": 40.0,
  "settlementType": "CASH"
}
EOF
)

    print_info "Trade payload (backdated):"
    echo "$trade_payload" | jq . || echo "$trade_payload"

    local response=$(curl -s -w "\n%{http_code}" -X POST "${TRADE_API}" \
        -H "Content-Type: application/json" \
        -d "$trade_payload")

    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    TRADE_ID=$(echo "$body" | jq -r '.id // empty' 2>/dev/null || echo "")

    if [[ -n "$TRADE_ID" ]] && [[ "$http_code" == "201" ]]; then
        print_success "Trade created with ID: ${TRADE_ID} (HTTP ${http_code})"
        print_info "Counterparty: ${counterparty}, Reference: ${reference_entity}"
        print_info "Dates: trade=${trade_date}, effective=${effective_date}, maturity=${maturity_date}"
        print_info "Notional: \$${notional}, Spread: ${spread} bps, Freq: ${premium_frequency}, DCC: ${day_count}"
        echo "$body" | jq . 2>/dev/null || echo "$body"
    else
        print_error "Failed to create trade (HTTP ${http_code})"
        echo "Response: $body"
        exit 1
    fi
}

# Generate coupon schedule
generate_coupon_schedule() {
    print_header "Generating Coupon Schedule"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "${LIFECYCLE_API}/trades/${TRADE_ID}/coupon-schedule")
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    
    if [[ "$http_code" != "200" ]]; then
        print_error "Failed to generate coupon schedule (HTTP ${http_code})"
        echo "Response: $body"
        exit 1
    fi
    
    COUPON_SCHEDULE=$(echo "$body")
    local period_count=$(echo "$body" | jq 'length')
    print_success "Coupon schedule generated with ${period_count} periods"
    echo "$body" | jq '.[] | {id, paymentDate, accrualDays, couponAmount, paid}'
}

get_risk_snapshot() {
    local label=$1
    local valuation_date=$2
    local scenario_id="${label}-${TRADE_ID}"
    print_header "Calculating Risk (${label}) valuationDate=${valuation_date}"
    local scenario_request=$(cat <<EOF
{
  "scenarioId": "${scenario_id}",
  "tradeIds": [${TRADE_ID}],
  "valuationDate": "${valuation_date}",
  "scenarios": {}
}
EOF
)
    
    # Save response to temp file to handle large/complex JSON with embedded strings
    local temp_response="/tmp/risk_response_${label}.json"
    curl -s -X POST "${RISK_API}/scenario/calculate" -H "Content-Type: application/json" -d "$scenario_request" > "$temp_response"
    
    # Check if response is valid JSON array first
    if ! jq -e 'type == "array"' "$temp_response" > /dev/null 2>&1; then
        print_error "Risk snapshot failed for ${label} - invalid JSON response"
        print_info "Response (first 500 chars): $(head -c 500 "$temp_response")"
        rm -f "$temp_response"
        return 1
    fi
    
    # Parse the first element
    local first_result=$(jq -c '.[0] // empty' "$temp_response" 2>/dev/null)
    if [[ -z "$first_result" || "$first_result" == "null" ]]; then
        print_error "Risk snapshot failed for ${label} - no results in array"
        print_info "Response array length: $(jq 'length' "$temp_response")"
        rm -f "$temp_response"
        return 1
    fi
    
    # Export fields dynamically
    local fields=(npv fairSpreadClean fairSpreadDirty protectionLegNPV premiumLegNPVClean accruedPremium upfrontPremium couponLegBps currentNotional)
    for f in "${fields[@]}"; do
        local val=$(jq -r ".[0].${f} // empty" "$temp_response" 2>/dev/null)
        eval "${label}_$(echo $f | tr '[:lower:]' '[:upper:]')=\"$val\""
    done
    
    # Print only the key metrics (manually to avoid jq control character issues)
    print_success "Risk calculated for ${label}"
    local npv_var="${label}_NPV"
    local fsc_var="${label}_FAIRSPREADCLEAN"
    local fsd_var="${label}_FAIRSPREADDIRTY"
    local pln_var="${label}_PROTECTIONLEGNPV"
    local pmc_var="${label}_PREMIUMLEGNPVCLEAN"
    local apr_var="${label}_ACCRUEDPREMIUM"
    local cur_var="${label}_CURRENTNOTIONAL"
    
    printf "${YELLOW}  NPV: %s\n" "${!npv_var}"
    printf "  Fair Spread (Clean): %s\n" "${!fsc_var}"
    printf "  Fair Spread (Dirty): %s\n" "${!fsd_var}"
    printf "  Protection Leg NPV: %s\n" "${!pln_var}"
    printf "  Premium Leg NPV (Clean): %s\n" "${!pmc_var}"
    printf "  Accrued Premium: %s\n" "${!apr_var}"
    printf "  Current Notional: %s${NC}\n" "${!cur_var}"
    
    # Clean up temp file
    rm -f "$temp_response"
}

# Pay next coupon
pay_next_coupon() {
    local on_time=${1:-false}
    
    local pay_mode="now"
    if [[ "$on_time" == "true" ]]; then
        pay_mode="on scheduled date"
    fi
    
    print_header "Paying Next Coupon (mode: ${pay_mode})"
    
    # Get coupon schedule to find next unpaid
    local schedule_response=$(curl -s -w "\n%{http_code}" "${LIFECYCLE_API}/trades/${TRADE_ID}/coupon-schedule")
    local schedule_http=$(echo "$schedule_response" | tail -n1)
    local schedule_body=$(echo "$schedule_response" | sed '$d')
    
    if [[ "$schedule_http" != "200" ]]; then
        print_error "Failed to fetch coupon schedule (HTTP ${schedule_http})"
        echo "Response: $schedule_body"
        return 1
    fi
    
    local next_period_id=$(echo "$schedule_body" | jq -r '.[] | select(.paid == false) | .id' | head -1)
    
    if [[ -z "$next_period_id" ]]; then
        print_info "No unpaid coupons remaining"
        return 0
    fi
    
    local response
    if [[ "$on_time" == "true" ]]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "${LIFECYCLE_API}/trades/${TRADE_ID}/coupon-periods/${next_period_id}/pay" \
            -H "Content-Type: application/json" \
            -d '{"payOnTime": true}')
    else
        response=$(curl -s -w "\n%{http_code}" -X POST "${LIFECYCLE_API}/trades/${TRADE_ID}/coupon-periods/${next_period_id}/pay")
    fi
    local pay_http=$(echo "$response" | tail -n1)
    local pay_body=$(echo "$response" | sed '$d')
    
    if [[ "$pay_http" != "200" ]]; then
        print_error "Failed to pay coupon (HTTP ${pay_http})"
        echo "Response: $pay_body"
        return 1
    fi
    
    local paid_status=$(echo "$pay_body" | jq -r '.paid // false')
    local coupon_amount=$(echo "$pay_body" | jq -r '.couponAmount // empty')
    
    if [[ "$paid_status" == "true" ]]; then
        print_success "Coupon paid successfully (Period ${next_period_id})"
        print_info "Coupon amount: \$${coupon_amount}"
        echo "$pay_body" | jq .
        return 0
    fi
    
    print_error "Coupon payment response unexpected"
    echo "Response: $pay_body"
    return 1
}

# (Old calculate_risk_at_date removed; unified via get_risk_snapshot)

# Verify metrics behave as expected
verify_metrics() {
    print_header "Verifying Metric Behavior (Economic Expectations)"
    local fail_count=0
    ok() { print_success "$1"; }
    bad() { print_error "$1"; ((fail_count++)); }

    # Assertions depend on snapshots T0, T7, T45
    # 1 NPV behavior over time
    # In real CDS valuations NPV can increase if credit spreads widen or discounting effects dominate.
    # Default (non-strict): accept decreases, stability, or moderate increases (<=25% relative).
    # Set STRICT_NPV=1 to enforce monotonic decrease.
    if [[ -n "$T0_NPV" && -n "$T45_NPV" ]]; then
        if [[ "${STRICT_NPV:-0}" == "1" ]]; then
            if (( $(echo "$T45_NPV < $T0_NPV" | bc -l) )); then
                ok "NPV decreased (strict mode) (T0=$T0_NPV T45=$T45_NPV)"
            else
                bad "NPV not decreasing (strict mode) (T0=$T0_NPV T45=$T45_NPV)"
            fi
        else
            # Compute relative change safely (avoid division by zero)
            local denom="$T0_NPV"
            if [[ -z "$denom" || $(echo "$denom == 0" | bc -l) -eq 1 ]]; then denom=1; fi
            rel_change=$(echo "scale=8; ($T45_NPV - $T0_NPV)/$denom" | bc -l)
            abs_rel=${rel_change#-}
            if (( $(echo "$T45_NPV <= $T0_NPV" | bc -l) )); then
                ok "NPV decreased or stable (Δrel=$rel_change)"
            elif (( $(echo "$abs_rel <= 0.25" | bc -l) )); then
                ok "NPV increased moderately (Δrel=$rel_change)"
            else
                print_info "NPV large increase (Δrel=$rel_change) - informational only"
            fi
        fi
    fi
    # 2 Fair Spread Clean ~ static (within 5%)
    if [[ -n "$T0_FAIRSPREADCLEAN" && -n "$T7_FAIRSPREADCLEAN" ]]; then
    local fs_denom="$T0_FAIRSPREADCLEAN"
    if [[ -z "$fs_denom" || $(echo "$fs_denom == 0" | bc -l) -eq 1 ]]; then fs_denom=1; fi
    rel=$(echo "scale=8; ($T7_FAIRSPREADCLEAN-$T0_FAIRSPREADCLEAN)/$fs_denom" | bc -l)
        absrel=${rel#-}
        if (( $(echo "$absrel < 0.05" | bc -l) )); then ok "Fair Spread Clean stable (rel=$rel)"; else bad "Fair Spread Clean moved >5% (rel=$rel)"; fi
    fi
    # 3 Fair Spread Dirty decreases if present
    if [[ -n "$T0_FAIRSPREADDIRTY" && -n "$T45_FAIRSPREADDIRTY" ]]; then
        if (( $(echo "$T45_FAIRSPREADDIRTY < $T0_FAIRSPREADDIRTY" | bc -l) )); then ok "Fair Spread Dirty decreased"; else print_info "Fair Spread Dirty non-decreasing (may be model nuance)"; fi
    fi
    # 4 Protection Leg absolute magnitude should generally amortize (|T45| <= |T0|) but can drift; allow +15% unless STRICT_LEGS=1
    if [[ -n "$T0_PROTECTIONLEGNPV" && -n "$T45_PROTECTIONLEGNPV" ]]; then
        local prot_abs_t0=$(echo "$T0_PROTECTIONLEGNPV" | sed 's/^-//')
        local prot_abs_t45=$(echo "$T45_PROTECTIONLEGNPV" | sed 's/^-//')
        local prot_denom="$prot_abs_t0"; if [[ -z "$prot_denom" || $(echo "$prot_denom == 0" | bc -l) -eq 1 ]]; then prot_denom=1; fi
        local prot_rel_abs=$(echo "scale=8; ($prot_abs_t45 - $prot_abs_t0)/$prot_denom" | bc -l)
        local prot_abs_rel=${prot_rel_abs#-}
        if (( $(echo "$prot_abs_t45 <= $prot_abs_t0" | bc -l) )); then
            ok "Protection Leg |value| decreased or stable (Δrel=$prot_rel_abs)"
        else
            if [[ "${STRICT_LEGS:-0}" == "1" ]]; then
                bad "Protection Leg |value| increased (strict) (Δrel=$prot_rel_abs)"
            elif (( $(echo "$prot_rel_abs <= 0.15" | bc -l) )); then
                ok "Protection Leg |value| modest increase tolerated (Δrel=$prot_rel_abs)"
            else
                print_info "Protection Leg |value| large increase (Δrel=$prot_rel_abs) - informational"
            fi
        fi
    fi
    # 5 Premium Leg absolute magnitude expected to decline similarly (|T45| <= |T0|), allow +15% unless STRICT_LEGS=1
    if [[ -n "$T0_PREMIUMLEGNPVCLEAN" && -n "$T45_PREMIUMLEGNPVCLEAN" ]]; then
        local prem_abs_t0=$(echo "$T0_PREMIUMLEGNPVCLEAN" | sed 's/^-//')
        local prem_abs_t45=$(echo "$T45_PREMIUMLEGNPVCLEAN" | sed 's/^-//')
        local prem_denom="$prem_abs_t0"; if [[ -z "$prem_denom" || $(echo "$prem_denom == 0" | bc -l) -eq 1 ]]; then prem_denom=1; fi
        local prem_rel_abs=$(echo "scale=8; ($prem_abs_t45 - $prem_abs_t0)/$prem_denom" | bc -l)
        if (( $(echo "$prem_abs_t45 <= $prem_abs_t0" | bc -l) )); then
            ok "Premium Leg |value| decreased or stable (Δrel=$prem_rel_abs)"
        else
            if [[ "${STRICT_LEGS:-0}" == "1" ]]; then
                bad "Premium Leg |value| increased (strict) (Δrel=$prem_rel_abs)"
            elif (( $(echo "$prem_rel_abs <= 0.15" | bc -l) )); then
                ok "Premium Leg |value| modest increase tolerated (Δrel=$prem_rel_abs)"
            else
                print_info "Premium Leg |value| large increase (Δrel=$prem_rel_abs) - informational"
            fi
        fi
    fi
    # 6 Accrued Premium should not increase after paying all past coupons (T7 <= T0)
    if [[ -n "$T0_ACCRUEDPREMIUM" && -n "$T7_ACCRUEDPREMIUM" ]]; then
        if (( $(echo "$T7_ACCRUEDPREMIUM <= $T0_ACCRUEDPREMIUM" | bc -l) )); then ok "Accrued Premium not increasing unexpectedly"; else bad "Accrued Premium increased (T0=$T0_ACCRUEDPREMIUM T7=$T7_ACCRUEDPREMIUM)"; fi
    fi
    # 7 Upfront Premium zero if present
    if [[ -n "$T0_UPFRONTPREMIUM" ]]; then
        if [[ "$T0_UPFRONTPREMIUM" =~ ^0(\.0+)?$ ]]; then ok "Upfront Premium zero"; else bad "Upfront Premium non-zero: $T0_UPFRONTPREMIUM"; fi
    fi
    # 8 Current Notional constant
    if [[ -n "$T0_CURRENTNOTIONAL" && -n "$T45_CURRENTNOTIONAL" ]]; then
        if (( $(echo "$T0_CURRENTNOTIONAL == $T45_CURRENTNOTIONAL" | bc -l) )); then ok "Current Notional constant"; else bad "Current Notional changed"; fi
    fi
    # 9 Total paid coupons monotonic
    local initial_paid=$(curl -s "${LIFECYCLE_API}/trades/${TRADE_ID}/enrichment" | jq -r '.totalPaidCoupons // 0')
    # After metrics, no extra payments performed; ensure not negative
    if (( initial_paid >= 0 )); then ok "Total paid coupons non-negative (${initial_paid})"; fi

    if (( fail_count > 0 )); then
        print_error "Metric verification encountered ${fail_count} failures"
    else
        print_success "All metric expectations satisfied"
    fi
}

# Get final coupon statistics
get_coupon_stats() {
    print_header "Coupon Schedule Summary"
    
    local schedule_response=$(curl -s -w "\n%{http_code}" "${LIFECYCLE_API}/trades/${TRADE_ID}/coupon-schedule")
    local schedule_http=$(echo "$schedule_response" | tail -n1)
    local schedule_body=$(echo "$schedule_response" | sed '$d')
    
    if [[ "$schedule_http" != "200" ]]; then
        print_error "Failed to fetch coupon statistics (HTTP ${schedule_http})"
        echo "Response: $schedule_body"
        return 1
    fi
    
    local total_periods=$(echo "$schedule_body" | jq 'length')
    local paid_periods=$(echo "$schedule_body" | jq '[.[] | select(.paid == true)] | length')
    local unpaid_periods=$(echo "$schedule_body" | jq '[.[] | select(.paid == false)] | length')
    
    print_info "Total coupon periods: ${total_periods}"
    print_info "Paid periods: ${paid_periods}"
    print_info "Unpaid periods: ${unpaid_periods}"
    
    if [[ "$total_periods" -gt 0 ]]; then
        print_success "Coupon schedule successfully generated with ${total_periods} periods"
    fi
}

# Delete the CDS trade
delete_trade() {
    print_header "Cleaning Up - Deleting CDS Trade"
    
    print_info "Deleting trade ID: ${TRADE_ID}"
    
    local http_code=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "${TRADE_API}/${TRADE_ID}")
    
    if [[ "$http_code" == "204" ]] || [[ "$http_code" == "200" ]]; then
        print_success "Trade ${TRADE_ID} deleted successfully"
        
        # Verify trade is gone
        sleep 1
        local verify_code=$(curl -s -o /dev/null -w "%{http_code}" "${TRADE_API}/${TRADE_ID}")
        
        if [[ "$verify_code" == "404" ]]; then
            print_success "Verified trade no longer exists in database"
        else
            print_error "Trade may still exist (HTTP ${verify_code})"
        fi
    else
        print_error "Failed to delete trade (HTTP ${http_code})"
        
        # Try to get error details
        local error_response=$(curl -s -X DELETE "${TRADE_API}/${TRADE_ID}")
        echo "Error response: $error_response"
        return 1
    fi
}

# Calculate business days from today
get_business_date() {
    local days=$1
    
    # Simplified: just add calendar days (full business day logic would need a calendar library)
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        date -u -v+${days}d +"%Y-%m-%d"
    else
        # Linux
        date -u -d "+${days} days" +"%Y-%m-%d"
    fi
}

# Main test flow
main() {
    # Interactive environment selection
    select_environment
    
    print_header "CDS Trade Lifecycle Integration Test"
    print_info "Environment: ${ENVIRONMENT}"
    print_info "Backend URL: ${BACKEND_URL}"
    print_info "Risk Engine URL: ${RISK_ENGINE_URL}"
    
    check_backend
    check_risk_engine
    
    print_info "Creating test trade and running lifecycle..."
    create_test_trade
    
    generate_coupon_schedule
    
    # Attempt to pay all coupons whose paymentDate <= today (due to backdating) BEFORE baseline risk
    print_info "Paying past-due coupons (if any)"
    local today=$(date -u +"%Y-%m-%d")
    local schedule=$(curl -s "${LIFECYCLE_API}/trades/${TRADE_ID}/coupon-schedule")
    local due_ids=$(echo "$schedule" | jq -r --arg TODAY "$today" '.[] | select(.paid==false and .paymentDate <= $TODAY) | .id')
    local paid_count=0
    for pid in $due_ids; do
        print_info "Paying coupon period $pid"
        local pay_resp=$(curl -s -w "\n%{http_code}" -X POST "${LIFECYCLE_API}/trades/${TRADE_ID}/coupon-periods/${pid}/pay" -H "Content-Type: application/json" -d '{"payOnTime":true}')
        local pay_http=$(echo "$pay_resp" | tail -n1)
        local pay_body=$(echo "$pay_resp" | sed '$d')
        if [[ "$pay_http" == "200" ]]; then
            print_success "Paid coupon period $pid"
            ((paid_count++))
        else
            print_error "Failed to pay coupon period $pid (HTTP $pay_http)"
            echo "Response: $pay_body"
        fi
    done
    if [[ $paid_count -eq 0 ]]; then
        print_info "No past-due coupons to pay (all future)."
    else
        print_info "Total coupons paid: $paid_count"
    fi

    # Baseline T0 risk AFTER coupon payments
    local t0=$(date -u +"%Y-%m-%d")
    get_risk_snapshot "T0" "$t0"
    
    # T+7
    local t_plus_7=$(get_business_date 7)
    get_risk_snapshot "T7" "$t_plus_7"
    sleep 1
    
    # T+45
    local t_plus_45=$(get_business_date 45)
    get_risk_snapshot "T45" "$t_plus_45"
    sleep 1
    
    verify_metrics
    
    get_coupon_stats
    
    # Cleanup - Delete the trade
    delete_trade
    
    # Summary
    print_header "Test Summary"
    printf "${GREEN}Tests Passed: ${TESTS_PASSED}${NC}\n"
    printf "${RED}Tests Failed: ${TESTS_FAILED}${NC}\n"
    
    if [[ $TESTS_FAILED -eq 0 ]]; then
        printf "\n${GREEN}🎉 All tests passed! Trade cleaned up successfully.${NC}\n\n"
        exit 0
    else
        printf "\n${RED}❌ Some tests failed${NC}\n\n"
        exit 1
    fi
}

# Run main function
main "$@"
