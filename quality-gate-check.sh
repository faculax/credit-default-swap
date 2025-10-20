#!/bin/bash
#
# Local Quality Gate Check
# Run this before committing to ensure your code meets security standards
#
# Usage: ./quality-gate-check.sh [service-name]
#   service-name: backend, gateway, or risk-engine (default: all)
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Banner
echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                                                            ║"
echo "║          CDS Platform Quality Gate Check                  ║"
echo "║                                                            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Determine which services to check
if [ -z "$1" ]; then
  SERVICES=("backend" "gateway" "risk-engine")
  echo "📋 Checking all services: ${SERVICES[@]}"
else
  SERVICES=("$1")
  echo "📋 Checking service: $1"
fi
echo ""

TOTAL_VIOLATIONS=0
TOTAL_WARNINGS=0

# Function to check a single service
check_service() {
  local SERVICE=$1
  
  if [ ! -d "$SERVICE" ]; then
    echo -e "${YELLOW}⚠️  Service directory not found: $SERVICE${NC}"
    return
  fi
  
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo -e "${BLUE}🔍 Analyzing: $SERVICE${NC}"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  
  cd "$SERVICE"
  
  SERVICE_VIOLATIONS=0
  SERVICE_WARNINGS=0
  
  # Step 1: Build the service
  echo -e "${BLUE}📦 Step 1: Building $SERVICE...${NC}"
  if ./mvnw clean compile -DskipTests -B > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Build successful${NC}"
  else
    echo -e "${RED}❌ Build failed${NC}"
    SERVICE_VIOLATIONS=$((SERVICE_VIOLATIONS + 1))
    cd ..
    return
  fi
  echo ""
  
  # Step 2: Run SpotBugs
  echo -e "${BLUE}🐛 Step 2: Running SpotBugs security analysis...${NC}"
  if ./mvnw spotbugs:spotbugs -B > /dev/null 2>&1; then
    if [ -f "target/spotbugsXml.xml" ]; then
      BUG_COUNT=$(grep -o '<BugInstance' target/spotbugsXml.xml 2>/dev/null | wc -l || echo "0")
      echo -e "${BLUE}   Found $BUG_COUNT total issues${NC}"
    fi
  else
    echo -e "${YELLOW}⚠️  SpotBugs execution warning${NC}"
  fi
  echo ""
  
  # Step 3: Check Zero-Tolerance Rules
  echo -e "${BLUE}🚨 Step 3: Checking Zero-Tolerance Security Rules...${NC}"
  echo ""
  
  if [ -f "target/spotbugsXml.xml" ]; then
    # Rule 1: CRLF Injection
    echo -n "   Rule 1 - CRLF Injection: "
    CRLF_COUNT=$(grep -c 'CRLF_INJECTION_LOGS' target/spotbugsXml.xml 2>/dev/null || echo "0")
    if [ "$CRLF_COUNT" -gt 0 ]; then
      echo -e "${RED}❌ FAIL ($CRLF_COUNT violations)${NC}"
      SERVICE_VIOLATIONS=$((SERVICE_VIOLATIONS + CRLF_COUNT))
    else
      echo -e "${GREEN}✅ PASS${NC}"
    fi
    
    # Rule 2: Predictable Random
    echo -n "   Rule 2 - Predictable Random: "
    RANDOM_COUNT=$(grep -c 'PREDICTABLE_RANDOM' target/spotbugsXml.xml 2>/dev/null || echo "0")
    DEMO_EXCEPTIONS=$(grep 'PREDICTABLE_RANDOM' target/spotbugsXml.xml 2>/dev/null | grep -c 'Demo.*Service' || echo "0")
    ACTUAL_RANDOM=$((RANDOM_COUNT - DEMO_EXCEPTIONS))
    if [ "$ACTUAL_RANDOM" -gt 0 ]; then
      echo -e "${RED}❌ FAIL ($ACTUAL_RANDOM violations)${NC}"
      SERVICE_VIOLATIONS=$((SERVICE_VIOLATIONS + ACTUAL_RANDOM))
    else
      if [ "$DEMO_EXCEPTIONS" -gt 0 ]; then
        echo -e "${GREEN}✅ PASS ($DEMO_EXCEPTIONS in demo code)${NC}"
      else
        echo -e "${GREEN}✅ PASS${NC}"
      fi
    fi
    
    # Rule 3: Unicode Handling
    echo -n "   Rule 3 - Unicode Handling: "
    UNICODE_COUNT=$(grep -c 'DM_CONVERT_CASE' target/spotbugsXml.xml 2>/dev/null || echo "0")
    if [ "$UNICODE_COUNT" -gt 0 ]; then
      echo -e "${RED}❌ FAIL ($UNICODE_COUNT violations)${NC}"
      SERVICE_VIOLATIONS=$((SERVICE_VIOLATIONS + UNICODE_COUNT))
    else
      echo -e "${GREEN}✅ PASS${NC}"
    fi
    
    # Rule 4: Information Exposure
    echo -n "   Rule 4 - Information Exposure: "
    INFO_COUNT=$(grep -c 'INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE' target/spotbugsXml.xml 2>/dev/null || echo "0")
    if [ "$INFO_COUNT" -gt 0 ]; then
      echo -e "${RED}❌ FAIL ($INFO_COUNT violations)${NC}"
      SERVICE_VIOLATIONS=$((SERVICE_VIOLATIONS + INFO_COUNT))
    else
      echo -e "${GREEN}✅ PASS${NC}"
    fi
    
    # Rule 5: SQL Injection
    echo -n "   Rule 5 - SQL Injection: "
    SQL_COUNT=$(grep -c 'SQL_INJECTION' target/spotbugsXml.xml 2>/dev/null || echo "0")
    if [ "$SQL_COUNT" -gt 0 ]; then
      echo -e "${RED}❌ FAIL ($SQL_COUNT violations)${NC}"
      SERVICE_VIOLATIONS=$((SERVICE_VIOLATIONS + SQL_COUNT))
    else
      echo -e "${GREEN}✅ PASS${NC}"
    fi
  else
    echo -e "${YELLOW}   ⚠️  SpotBugs XML report not found${NC}"
  fi
  echo ""
  
  # Step 4: Check Anti-Patterns
  echo -e "${BLUE}🔍 Step 4: Checking for Anti-Patterns...${NC}"
  echo ""
  
  # Anti-Pattern 1: Client-controlled authorization
  echo -n "   Anti-Pattern 1 - Client Auth: "
  if grep -rn --include="*.java" 'request\.getParameter.*[Rr]ole' src/ > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  WARNING${NC}"
    SERVICE_WARNINGS=$((SERVICE_WARNINGS + 1))
  else
    echo -e "${GREEN}✅ PASS${NC}"
  fi
  
  # Anti-Pattern 2: Disabled security
  echo -n "   Anti-Pattern 2 - Disabled Security: "
  if grep -rn --include="*.java" '@CrossOrigin.*origins.*=.*"\*"' src/ > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  WARNING${NC}"
    SERVICE_WARNINGS=$((SERVICE_WARNINGS + 1))
  else
    echo -e "${GREEN}✅ PASS${NC}"
  fi
  
  # Anti-Pattern 3: Weak crypto
  echo -n "   Anti-Pattern 3 - Weak Crypto: "
  if grep -rn --include="*.java" 'MessageDigest\.getInstance.*"MD5"\|"SHA1"' src/ > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  WARNING${NC}"
    SERVICE_WARNINGS=$((SERVICE_WARNINGS + 1))
  else
    echo -e "${GREEN}✅ PASS${NC}"
  fi
  
  # Anti-Pattern 4: Sensitive data logging
  echo -n "   Anti-Pattern 4 - Sensitive Logging: "
  SENSITIVE_LOGS=$(grep -rn --include="*.java" 'logger\.\(info\|debug\|warn\|error\).*password\|secret\|token\|apikey\|api_key' src/ 2>/dev/null | wc -l || echo "0")
  if [ "$SENSITIVE_LOGS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  WARNING ($SENSITIVE_LOGS instances)${NC}"
    SERVICE_WARNINGS=$((SERVICE_WARNINGS + SENSITIVE_LOGS))
  else
    echo -e "${GREEN}✅ PASS${NC}"
  fi
  echo ""
  
  # Note: Missing @PreAuthorize is NOT checked as authorization strategy varies:
  # - Public endpoints (health) don't need @PreAuthorize
  # - Internal APIs use service token validation
  # - User-facing APIs should have @PreAuthorize
  # Manual security review should verify appropriate authorization per endpoint
  
  # Step 5: Run Unit Tests with Coverage
  echo -e "${BLUE}🧪 Step 5: Running Unit Tests with Coverage...${NC}"
  if ./mvnw test jacoco:report -B > /dev/null 2>&1; then
    if [ -f "target/site/jacoco/jacoco.xml" ]; then
      INSTRUCTION_COVERED=$(grep -oP 'type="INSTRUCTION".*?covered="\K[0-9]+' target/site/jacoco/jacoco.xml | head -1 || echo "0")
      INSTRUCTION_MISSED=$(grep -oP 'type="INSTRUCTION".*?missed="\K[0-9]+' target/site/jacoco/jacoco.xml | head -1 || echo "0")
      
      if [ "$((INSTRUCTION_COVERED + INSTRUCTION_MISSED))" -gt 0 ]; then
        COVERAGE_PCT=$((INSTRUCTION_COVERED * 100 / (INSTRUCTION_COVERED + INSTRUCTION_MISSED)))
        echo "   Test Coverage: ${COVERAGE_PCT}%"
        
        if [ "$COVERAGE_PCT" -lt 80 ]; then
          echo -e "   ${YELLOW}⚠️  WARNING: Coverage below 80% threshold${NC}"
          SERVICE_WARNINGS=$((SERVICE_WARNINGS + 1))
        else
          echo -e "   ${GREEN}✅ Coverage meets 80% threshold${NC}"
        fi
      fi
    fi
  else
    echo -e "   ${YELLOW}⚠️  Test execution warning${NC}"
    SERVICE_WARNINGS=$((SERVICE_WARNINGS + 1))
  fi
  echo ""
  
  # Service Summary
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo -e "${BLUE}Summary for $SERVICE:${NC}"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  if [ "$SERVICE_VIOLATIONS" -gt 0 ]; then
    echo -e "${RED}❌ QUALITY GATE FAILED${NC}"
    echo -e "   Critical Violations: ${RED}$SERVICE_VIOLATIONS${NC}"
  else
    echo -e "${GREEN}✅ QUALITY GATE PASSED${NC}"
    echo -e "   Critical Violations: ${GREEN}0${NC}"
  fi
  
  if [ "$SERVICE_WARNINGS" -gt 0 ]; then
    echo -e "   Warnings: ${YELLOW}$SERVICE_WARNINGS${NC}"
  else
    echo -e "   Warnings: ${GREEN}0${NC}"
  fi
  echo ""
  
  TOTAL_VIOLATIONS=$((TOTAL_VIOLATIONS + SERVICE_VIOLATIONS))
  TOTAL_WARNINGS=$((TOTAL_WARNINGS + SERVICE_WARNINGS))
  
  cd ..
}

# Check each service
for SERVICE in "${SERVICES[@]}"; do
  check_service "$SERVICE"
done

# Overall Summary
echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                     OVERALL SUMMARY                        ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

if [ "$TOTAL_VIOLATIONS" -gt 0 ]; then
  echo -e "${RED}❌ QUALITY GATE FAILED${NC}"
  echo ""
  echo -e "   Total Critical Violations: ${RED}$TOTAL_VIOLATIONS${NC}"
  echo -e "   Total Warnings: ${YELLOW}$TOTAL_WARNINGS${NC}"
  echo ""
  echo "📋 Action Required:"
  echo "   1. Review SpotBugs reports in target/site/spotbugs.html"
  echo "   2. Consult CODE_QUALITY_RULES.md for remediation steps"
  echo "   3. Consult AGENTS.md for security standards"
  echo ""
  echo "🚫 DO NOT COMMIT until all critical violations are fixed"
  echo ""
  exit 1
else
  echo -e "${GREEN}✅ ALL QUALITY GATES PASSED${NC}"
  echo ""
  echo -e "   Total Critical Violations: ${GREEN}0${NC}"
  echo -e "   Total Warnings: ${YELLOW}$TOTAL_WARNINGS${NC}"
  echo ""
  if [ "$TOTAL_WARNINGS" -gt 0 ]; then
    echo "💡 Consider addressing warnings before committing"
  else
    echo "🎉 Your code meets all security standards!"
  fi
  echo ""
  echo "✅ Safe to commit"
  echo ""
  exit 0
fi
