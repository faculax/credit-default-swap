#!/bin/bash
# ===============================================================================
# DefectDojo Security Scan Uploader (Bash)
# ===============================================================================
# This script uploads security scan results to DefectDojo
# Supports: OWASP Dependency Check, SpotBugs, Checkstyle, PMD
# ===============================================================================

set -e

# Configuration
DD_URL="${DD_URL:-http://localhost:8081}"
DD_USER="${DD_USER:-admin}"
DD_PASSWORD="${DD_PASSWORD:-admin}"
PRODUCT_NAME="${PRODUCT_NAME:-Credit Default Swap Platform}"
ENGAGEMENT_NAME="Security Scan - $(date '+%Y-%m-%d %H:%M')"
REPORTS_PATH="${REPORTS_PATH:-../backend/target/security-reports}"

# Colors
COLOR_GREEN='\033[0;32m'
COLOR_YELLOW='\033[1;33m'
COLOR_RED='\033[0;31m'
COLOR_BLUE='\033[0;34m'
COLOR_CYAN='\033[0;36m'
COLOR_RESET='\033[0m'

# Logging functions
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

# Check for required tools
check_dependencies() {
    if ! command -v curl &> /dev/null; then
        log_error "curl is required but not installed"
        exit 1
    fi
    if ! command -v jq &> /dev/null; then
        log_warning "jq not found - install for better JSON parsing"
    fi
}

# ===============================================================================
# Step 1: Get API Token
# ===============================================================================
get_api_token() {
    log_step "Authenticating with DefectDojo..."
    
    response=$(curl -s -X POST "$DD_URL/api/v2/api-token-auth/" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$DD_USER\",\"password\":\"$DD_PASSWORD\"}")
    
    if command -v jq &> /dev/null; then
        TOKEN=$(echo "$response" | jq -r '.token')
    else
        TOKEN=$(echo "$response" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    fi
    
    if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
        log_error "Failed to obtain API token"
        log_warning "Make sure DefectDojo is running: docker-compose -f compliance/docker-compose.defectdojo.yml up -d"
        exit 1
    fi
    
    log_success "Authentication successful"
}

# ===============================================================================
# Step 2: Get or Create Product
# ===============================================================================
get_or_create_product() {
    log_step "Checking for product '$PRODUCT_NAME'..."
    
    encoded_name=$(echo "$PRODUCT_NAME" | sed 's/ /%20/g')
    response=$(curl -s -X GET "$DD_URL/api/v2/products/?name=$encoded_name" \
        -H "Authorization: Token $TOKEN")
    
    if command -v jq &> /dev/null; then
        count=$(echo "$response" | jq -r '.count')
        if [ "$count" -gt 0 ]; then
            PRODUCT_ID=$(echo "$response" | jq -r '.results[0].id')
            log_success "Found existing product (ID: $PRODUCT_ID)"
        else
            create_product
        fi
    else
        if echo "$response" | grep -q '"count":[1-9]'; then
            PRODUCT_ID=$(echo "$response" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
            log_success "Found existing product (ID: $PRODUCT_ID)"
        else
            create_product
        fi
    fi
}

create_product() {
    log_step "Creating new product..."
    
    response=$(curl -s -X POST "$DD_URL/api/v2/products/" \
        -H "Authorization: Token $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\":\"$PRODUCT_NAME\",
            \"description\":\"Credit Default Swap platform - Spring Boot microservices with PostgreSQL\",
            \"prod_type\":1,
            \"lifecycle\":\"production\",
            \"origin\":\"local_development\"
        }")
    
    if command -v jq &> /dev/null; then
        PRODUCT_ID=$(echo "$response" | jq -r '.id')
    else
        PRODUCT_ID=$(echo "$response" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    fi
    
    log_success "Created new product (ID: $PRODUCT_ID)"
}

# ===============================================================================
# Step 3: Create Engagement
# ===============================================================================
create_engagement() {
    log_step "Creating engagement '$ENGAGEMENT_NAME'..."
    
    today=$(date '+%Y-%m-%d')
    
    response=$(curl -s -X POST "$DD_URL/api/v2/engagements/" \
        -H "Authorization: Token $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"product\":$PRODUCT_ID,
            \"name\":\"$ENGAGEMENT_NAME\",
            \"description\":\"Automated security scan from local development environment\",
            \"target_start\":\"$today\",
            \"target_end\":\"$today\",
            \"status\":\"In Progress\",
            \"engagement_type\":\"CI/CD\"
        }")
    
    if command -v jq &> /dev/null; then
        ENGAGEMENT_ID=$(echo "$response" | jq -r '.id')
    else
        ENGAGEMENT_ID=$(echo "$response" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    fi
    
    log_success "Created engagement (ID: $ENGAGEMENT_ID)"
}

# ===============================================================================
# Step 4: Upload Scan Results
# ===============================================================================
upload_scan_results() {
    log_step "Uploading scan results from '$REPORTS_PATH'..."
    
    scan_date=$(date '+%Y-%m-%d')
    uploaded_count=0
    
    # OWASP Dependency Check
    if [ -f "$REPORTS_PATH/dependency-check-report.json" ]; then
        log_step "Uploading OWASP Dependency Check..."
        if upload_file "$REPORTS_PATH/dependency-check-report.json" "Dependency Check Scan"; then
            log_success "Uploaded OWASP Dependency Check"
            ((uploaded_count++))
        else
            log_warning "Failed to upload OWASP Dependency Check"
        fi
    else
        log_warning "File not found: $REPORTS_PATH/dependency-check-report.json"
    fi
    
    # SpotBugs
    if [ -f "$REPORTS_PATH/spotbugsXml.xml" ]; then
        log_step "Uploading SpotBugs..."
        if upload_file "$REPORTS_PATH/spotbugsXml.xml" "SpotBugs Scan"; then
            log_success "Uploaded SpotBugs"
            ((uploaded_count++))
        else
            log_warning "Failed to upload SpotBugs"
        fi
    else
        log_warning "File not found: $REPORTS_PATH/spotbugsXml.xml"
    fi
    
    # Checkstyle
    if [ -f "$REPORTS_PATH/checkstyle-result.xml" ]; then
        log_step "Uploading Checkstyle..."
        if upload_file "$REPORTS_PATH/checkstyle-result.xml" "Checkstyle Scan"; then
            log_success "Uploaded Checkstyle"
            ((uploaded_count++))
        else
            log_warning "Failed to upload Checkstyle"
        fi
    else
        log_warning "File not found: $REPORTS_PATH/checkstyle-result.xml"
    fi
    
    # PMD
    if [ -f "$REPORTS_PATH/pmd.xml" ]; then
        log_step "Uploading PMD..."
        if upload_file "$REPORTS_PATH/pmd.xml" "PMD Scan"; then
            log_success "Uploaded PMD"
            ((uploaded_count++))
        else
            log_warning "Failed to upload PMD"
        fi
    else
        log_warning "File not found: $REPORTS_PATH/pmd.xml"
    fi
    
    echo ""
    log_success "Uploaded $uploaded_count scan report(s)"
}

upload_file() {
    local file_path=$1
    local scan_type=$2
    
    response=$(curl -s -w "\n%{http_code}" -X POST "$DD_URL/api/v2/import-scan/" \
        -H "Authorization: Token $TOKEN" \
        -F "scan_type=$scan_type" \
        -F "file=@$file_path" \
        -F "engagement=$ENGAGEMENT_ID" \
        -F "verified=true" \
        -F "active=true" \
        -F "scan_date=$scan_date")
    
    http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        return 0
    else
        return 1
    fi
}

# ===============================================================================
# Print Summary
# ===============================================================================
print_summary() {
    echo ""
    echo -e "${COLOR_CYAN}═══════════════════════════════════════════════════════════════${COLOR_RESET}"
    echo -e "${COLOR_CYAN} DEFECTDOJO UPLOAD COMPLETE${COLOR_RESET}"
    echo -e "${COLOR_CYAN}═══════════════════════════════════════════════════════════════${COLOR_RESET}"
    echo ""
    echo -e "${COLOR_YELLOW}📊 View Results:${COLOR_RESET}"
    echo -e "   URL: ${COLOR_BLUE}$DD_URL/engagement/$ENGAGEMENT_ID${COLOR_RESET}"
    echo ""
    echo -e "${COLOR_YELLOW}🔐 Login Credentials:${COLOR_RESET}"
    echo -e "   Username: ${COLOR_BLUE}$DD_USER${COLOR_RESET}"
    echo -e "   Password: ${COLOR_BLUE}$DD_PASSWORD${COLOR_RESET}"
    echo ""
    echo -e "${COLOR_CYAN}═══════════════════════════════════════════════════════════════${COLOR_RESET}"
}

# ===============================================================================
# Main Execution
# ===============================================================================
main() {
    echo ""
    echo -e "${COLOR_CYAN}═══════════════════════════════════════════════════════════════${COLOR_RESET}"
    echo -e "${COLOR_CYAN} DEFECTDOJO SCAN UPLOADER${COLOR_RESET}"
    echo -e "${COLOR_CYAN}═══════════════════════════════════════════════════════════════${COLOR_RESET}"
    echo ""
    
    check_dependencies
    get_api_token
    get_or_create_product
    create_engagement
    upload_scan_results
    print_summary
}

# Run main function
main
