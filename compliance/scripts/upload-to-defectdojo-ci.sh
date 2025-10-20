#!/bin/bash
# ===============================================================================
# DefectDojo CI/CD Upload Script
# ===============================================================================
# Optimized for CI/CD environments with API token authentication
# Usage: Set DD_TOKEN, DD_URL environment variables
# ===============================================================================

set -e

# Configuration from environment
DD_URL="${DD_URL:-http://localhost:8081}"
TOKEN="${DD_TOKEN}"
PRODUCT_NAME="${PRODUCT_NAME:-Credit Default Swap Platform}"
ENGAGEMENT_NAME="${ENGAGEMENT_NAME:-CI/CD Scan - $(date '+%Y-%m-%d %H:%M')}"
REPORTS_PATH="${REPORTS_PATH:-backend/target/security-reports}"

# Branch and commit info (for GitHub Actions/GitLab CI)
BRANCH="${GITHUB_REF_NAME:-${CI_COMMIT_REF_NAME:-unknown}}"
COMMIT="${GITHUB_SHA:-${CI_COMMIT_SHA:-unknown}}"
BUILD_URL="${GITHUB_SERVER_URL:-${CI_PROJECT_URL:-unknown}}/${GITHUB_REPOSITORY:-${CI_PROJECT_PATH:-unknown}}/actions/runs/${GITHUB_RUN_ID:-${CI_PIPELINE_ID:-unknown}}"

# Colors
COLOR_GREEN='\033[0;32m'
COLOR_YELLOW='\033[1;33m'
COLOR_RED='\033[0;31m'
COLOR_BLUE='\033[0;34m'
COLOR_RESET='\033[0m'

log_step() {
    echo -e "${COLOR_BLUE}▶ $1${COLOR_RESET}"
}

log_success() {
    echo -e "${COLOR_GREEN}✓ $1${COLOR_RESET}"
}

log_warning() {
    echo -e "${COLOR_YELLOW}⚠ $1${COLOR_RESET}"
}

log_error() {
    echo -e "${COLOR_RED}✗ $1${COLOR_RESET}"
}

# Validate required environment variables
if [ -z "$TOKEN" ]; then
    log_error "DD_TOKEN environment variable not set"
    log_error "Set it in your CI/CD secrets configuration"
    exit 1
fi

echo ""
log_step "DefectDojo CI/CD Upload"
echo ""
log_step "Configuration:"
echo "  URL: $DD_URL"
echo "  Product: $PRODUCT_NAME"
echo "  Branch: $BRANCH"
echo "  Commit: ${COMMIT:0:8}"
echo ""

# Check if jq is available
if ! command -v jq &> /dev/null; then
    log_warning "jq not found - using grep for JSON parsing"
    USE_JQ=false
else
    USE_JQ=true
fi

# ===============================================================================
# Get or Create Product
# ===============================================================================
log_step "Getting or creating product..."

encoded_name=$(echo "$PRODUCT_NAME" | sed 's/ /%20/g')
response=$(curl -s -X GET "$DD_URL/api/v2/products/?name=$encoded_name" \
    -H "Authorization: Token $TOKEN")

if [ "$USE_JQ" = true ]; then
    count=$(echo "$response" | jq -r '.count')
    if [ "$count" -gt 0 ]; then
        PRODUCT_ID=$(echo "$response" | jq -r '.results[0].id')
        log_success "Found existing product (ID: $PRODUCT_ID)"
    else
        create_product=true
    fi
else
    if echo "$response" | grep -q '"count":[1-9]'; then
        PRODUCT_ID=$(echo "$response" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        log_success "Found existing product (ID: $PRODUCT_ID)"
    else
        create_product=true
    fi
fi

if [ "$create_product" = true ]; then
    log_step "Creating new product..."
    
    response=$(curl -s -X POST "$DD_URL/api/v2/products/" \
        -H "Authorization: Token $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\":\"$PRODUCT_NAME\",
            \"description\":\"Automated CI/CD security scanning\",
            \"prod_type\":1,
            \"lifecycle\":\"production\",
            \"origin\":\"ci_cd\"
        }")
    
    if [ "$USE_JQ" = true ]; then
        PRODUCT_ID=$(echo "$response" | jq -r '.id')
    else
        PRODUCT_ID=$(echo "$response" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    fi
    
    log_success "Created new product (ID: $PRODUCT_ID)"
fi

# ===============================================================================
# Create Engagement
# ===============================================================================
log_step "Creating engagement..."

today=$(date '+%Y-%m-%d')

response=$(curl -s -X POST "$DD_URL/api/v2/engagements/" \
    -H "Authorization: Token $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"product\":$PRODUCT_ID,
        \"name\":\"$ENGAGEMENT_NAME\",
        \"description\":\"CI/CD automated scan\nBranch: $BRANCH\nCommit: $COMMIT\nBuild: $BUILD_URL\",
        \"target_start\":\"$today\",
        \"target_end\":\"$today\",
        \"status\":\"In Progress\",
        \"engagement_type\":\"CI/CD\",
        \"branch_tag\":\"$BRANCH\",
        \"commit_hash\":\"$COMMIT\",
        \"build_id\":\"${GITHUB_RUN_ID:-${CI_PIPELINE_ID:-unknown}}\"
    }")

if [ "$USE_JQ" = true ]; then
    ENGAGEMENT_ID=$(echo "$response" | jq -r '.id')
else
    ENGAGEMENT_ID=$(echo "$response" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
fi

log_success "Created engagement (ID: $ENGAGEMENT_ID)"

# ===============================================================================
# Upload Scan Results
# ===============================================================================
log_step "Uploading scan results from '$REPORTS_PATH'..."
echo ""

scan_date=$(date '+%Y-%m-%d')
uploaded_count=0
failed_count=0

upload_file() {
    local file_path=$1
    local scan_type=$2
    local description=$3
    
    if [ -f "$file_path" ]; then
        log_step "Uploading $description..."
        
        response=$(curl -s -w "\n%{http_code}" -X POST "$DD_URL/api/v2/import-scan/" \
            -H "Authorization: Token $TOKEN" \
            -F "scan_type=$scan_type" \
            -F "file=@$file_path" \
            -F "engagement=$ENGAGEMENT_ID" \
            -F "verified=true" \
            -F "active=true" \
            -F "scan_date=$scan_date" \
            -F "branch_tag=$BRANCH" \
            -F "commit_hash=$COMMIT" \
            -F "build_id=${GITHUB_RUN_ID:-${CI_PIPELINE_ID:-unknown}}")
        
        http_code=$(echo "$response" | tail -n1)
        
        if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
            log_success "Uploaded $description"
            ((uploaded_count++))
        else
            log_warning "Failed to upload $description (HTTP $http_code)"
            ((failed_count++))
        fi
    else
        log_warning "File not found: $file_path"
        ((failed_count++))
    fi
}

# Upload each scan type
upload_file "$REPORTS_PATH/dependency-check-report.json" "Dependency Check Scan" "OWASP Dependency Check"
upload_file "$REPORTS_PATH/spotbugsXml.xml" "SpotBugs Scan" "SpotBugs"
upload_file "$REPORTS_PATH/checkstyle-result.xml" "Checkstyle Scan" "Checkstyle"
upload_file "$REPORTS_PATH/pmd.xml" "PMD Scan" "PMD"

# ===============================================================================
# Summary
# ===============================================================================
echo ""
echo "═══════════════════════════════════════════════════════════════"
echo " UPLOAD SUMMARY"
echo "═══════════════════════════════════════════════════════════════"
echo ""
log_success "Uploaded: $uploaded_count scan(s)"
if [ $failed_count -gt 0 ]; then
    log_warning "Failed: $failed_count scan(s)"
fi
echo ""
echo "📊 View Results:"
echo "   $DD_URL/engagement/$ENGAGEMENT_ID"
echo ""
echo "═══════════════════════════════════════════════════════════════"

# Exit with error if all uploads failed
if [ $uploaded_count -eq 0 ] && [ $failed_count -gt 0 ]; then
    log_error "All uploads failed"
    exit 1
fi

# Exit with warning if some uploads failed
if [ $failed_count -gt 0 ]; then
    exit 2  # Non-zero but not critical failure
fi

exit 0
