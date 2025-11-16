#!/bin/bash

# Generate PR Comment from Allure Results
# This script parses unified Allure results and creates a markdown summary
# suitable for posting as a PR comment

set -e

RESULTS_DIR="${1:-allure-results-unified}"
OUTPUT_FILE="${2:-pr-comment.md}"
REPORT_URL="${3:-}"

echo "📊 Generating PR comment from Allure results..."
echo "Results directory: $RESULTS_DIR"
echo "Output file: $OUTPUT_FILE"

# Initialize counters
declare -A service_passed
declare -A service_failed
declare -A service_broken
declare -A service_skipped
declare -A service_total

# Function to extract service name from labels
get_service_name() {
  local json_file="$1"
  # Extract feature label which represents the service
  local service=$(jq -r '.labels[] | select(.name == "feature") | .value' "$json_file" 2>/dev/null || echo "Unknown")
  
  # Fallback to extracting from test name if no feature label
  if [ "$service" = "Unknown" ] || [ -z "$service" ]; then
    local test_name=$(jq -r '.name' "$json_file" 2>/dev/null)
    if [[ $test_name == *"Backend"* ]] || [[ $test_name == *"backend"* ]]; then
      service="Backend Service"
    elif [[ $test_name == *"Frontend"* ]] || [[ $test_name == *"frontend"* ]]; then
      service="Frontend Service"
    elif [[ $test_name == *"Gateway"* ]] || [[ $test_name == *"gateway"* ]]; then
      service="Gateway Service"
    elif [[ $test_name == *"Risk"* ]] || [[ $test_name == *"risk"* ]]; then
      service="Risk Engine Service"
    else
      service="Unknown Service"
    fi
  fi
  
  echo "$service"
}

# Parse all test result files
echo "Parsing test results..."
total_tests=0

for result_file in "$RESULTS_DIR"/*-result.json; do
  if [ ! -f "$result_file" ]; then
    continue
  fi
  
  # Extract status and service
  status=$(jq -r '.status' "$result_file" 2>/dev/null || echo "unknown")
  service=$(get_service_name "$result_file")
  
  # Increment counters
  ((service_total["$service"]++)) || service_total["$service"]=1
  ((total_tests++))
  
  case "$status" in
    passed)
      ((service_passed["$service"]++)) || service_passed["$service"]=1
      ;;
    failed)
      ((service_failed["$service"]++)) || service_failed["$service"]=1
      ;;
    broken)
      ((service_broken["$service"]++)) || service_broken["$service"]=1
      ;;
    skipped)
      ((service_skipped["$service"]++)) || service_skipped["$service"]=1
      ;;
  esac
done

# Calculate overall statistics
total_passed=0
total_failed=0
total_broken=0
total_skipped=0

for service in "${!service_total[@]}"; do
  total_passed=$((total_passed + ${service_passed["$service"]:-0}))
  total_failed=$((total_failed + ${service_failed["$service"]:-0}))
  total_broken=$((total_broken + ${service_broken["$service"]:-0}))
  total_skipped=$((total_skipped + ${service_skipped["$service"]:-0}))
done

# Calculate pass rate
if [ $total_tests -gt 0 ]; then
  pass_rate=$(awk "BEGIN {printf \"%.1f\", ($total_passed / $total_tests) * 100}")
else
  pass_rate="0.0"
fi

# Determine overall status emoji
if [ $total_failed -eq 0 ] && [ $total_broken -eq 0 ]; then
  status_emoji="✅"
  status_text="All tests passed"
elif [ $total_failed -gt 0 ] || [ $total_broken -gt 0 ]; then
  status_emoji="❌"
  status_text="Some tests failed"
else
  status_emoji="⚠️"
  status_text="Tests completed with warnings"
fi

echo "Generating markdown comment..."

# Generate markdown comment
cat > "$OUTPUT_FILE" << EOF
## $status_emoji Test Results Summary

**$status_text** - Pass rate: **${pass_rate}%** (${total_passed}/${total_tests} tests)

### 📊 Overall Statistics

| Status | Count | Percentage |
|--------|-------|------------|
| ✅ Passed | $total_passed | $(awk "BEGIN {printf \"%.1f%%\", ($total_passed / $total_tests) * 100}") |
| ❌ Failed | $total_failed | $(awk "BEGIN {printf \"%.1f%%\", ($total_failed / $total_tests) * 100}") |
| 💥 Broken | $total_broken | $(awk "BEGIN {printf \"%.1f%%\", ($total_broken / $total_tests) * 100}") |
| ⏭️ Skipped | $total_skipped | $(awk "BEGIN {printf \"%.1f%%\", ($total_skipped / $total_tests) * 100}") |
| **Total** | **$total_tests** | **100%** |

### 🎯 Results by Service

| Service | Total | ✅ Passed | ❌ Failed | 💥 Broken | ⏭️ Skipped | Pass Rate |
|---------|-------|-----------|-----------|-----------|------------|-----------|
EOF

# Add service rows (sorted)
for service in $(printf '%s\n' "${!service_total[@]}" | sort); do
  total=${service_total["$service"]}
  passed=${service_passed["$service"]:-0}
  failed=${service_failed["$service"]:-0}
  broken=${service_broken["$service"]:-0}
  skipped=${service_skipped["$service"]:-0}
  
  if [ $total -gt 0 ]; then
    rate=$(awk "BEGIN {printf \"%.1f%%\", ($passed / $total) * 100}")
  else
    rate="N/A"
  fi
  
  # Service status emoji
  if [ $failed -eq 0 ] && [ $broken -eq 0 ]; then
    service_emoji="✅"
  else
    service_emoji="❌"
  fi
  
  echo "| $service_emoji **$service** | $total | $passed | $failed | $broken | $skipped | $rate |" >> "$OUTPUT_FILE"
done

# Add report link if provided
if [ -n "$REPORT_URL" ]; then
  cat >> "$OUTPUT_FILE" << EOF

### 📈 Full Report

🔗 **[View Detailed Allure Report]($REPORT_URL)**

The complete report includes:
- Detailed test execution history
- Test behavior categorization (Features & Stories)
- Failure trends and statistics
- Test duration analytics
- Attachments and logs

EOF
fi

# Add footer
cat >> "$OUTPUT_FILE" << EOF
---
*Report generated at $(date -u +"%Y-%m-%d %H:%M:%S UTC")*
EOF

echo "✅ PR comment generated: $OUTPUT_FILE"
echo ""
echo "Preview:"
cat "$OUTPUT_FILE"
