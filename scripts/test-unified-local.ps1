# Unified Local Testing Script - Backend + Frontend Allure Report
# This script runs all tests and generates a single unified Allure report

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CDS Platform - Unified Local Test Report" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$rootDir = Split-Path $PSScriptRoot -Parent
$unifiedResultsDir = "$rootDir\allure-results-unified"

# Clean up previous results
Write-Host "Cleaning up previous results..." -ForegroundColor Yellow
if (Test-Path $unifiedResultsDir) {
    Remove-Item $unifiedResultsDir -Recurse -Force
}
New-Item -ItemType Directory -Path $unifiedResultsDir -Force | Out-Null

if (Test-Path "$rootDir\allure-report") {
    Remove-Item "$rootDir\allure-report" -Recurse -Force
}

Write-Host "  Unified results directory: $unifiedResultsDir" -ForegroundColor Cyan
Write-Host ""

# ============================================
# STEP 1: Run Backend Tests (3 services)
# ============================================
Write-Host "STEP 1: Running Backend Tests" -ForegroundColor Yellow
Write-Host "==============================" -ForegroundColor Yellow
Write-Host ""

Set-Location "$rootDir\backend"

Write-Host "  Building and testing backend services..." -ForegroundColor Cyan
$backendResult = & mvn clean test -B 2>&1
$backendSuccess = $LASTEXITCODE -eq 0

if ($backendSuccess) {
    Write-Host "    SUCCESS: Backend tests passed" -ForegroundColor Green
} else {
    Write-Host "    WARNING: Some backend tests failed (continuing anyway)" -ForegroundColor Yellow
}

# Copy backend Allure results
$backendAllureDir = "$rootDir\backend\target\allure-results"
if (Test-Path $backendAllureDir) {
    $backendFiles = Get-ChildItem $backendAllureDir -File
    Write-Host "    Copying $($backendFiles.Count) backend result files..." -ForegroundColor Cyan
    Copy-Item "$backendAllureDir\*" $unifiedResultsDir -Force
    Write-Host "    Backend results copied" -ForegroundColor Green
} else {
    Write-Host "    WARNING: No backend Allure results found at $backendAllureDir" -ForegroundColor Yellow
}

Write-Host ""

# ============================================
# STEP 2: Run Gateway Tests
# ============================================
Write-Host "STEP 2: Running Gateway Tests" -ForegroundColor Yellow
Write-Host "==============================" -ForegroundColor Yellow
Write-Host ""

Set-Location "$rootDir\gateway"

Write-Host "  Building and testing gateway..." -ForegroundColor Cyan
$gatewayResult = & mvn clean test -B 2>&1
$gatewaySuccess = $LASTEXITCODE -eq 0

if ($gatewaySuccess) {
    Write-Host "    SUCCESS: Gateway tests passed" -ForegroundColor Green
} else {
    Write-Host "    WARNING: Some gateway tests failed (continuing anyway)" -ForegroundColor Yellow
}

# Copy gateway Allure results
$gatewayAllureDir = "$rootDir\gateway\target\allure-results"
if (Test-Path $gatewayAllureDir) {
    $gatewayFiles = Get-ChildItem $gatewayAllureDir -File
    Write-Host "    Copying $($gatewayFiles.Count) gateway result files..." -ForegroundColor Cyan
    Copy-Item "$gatewayAllureDir\*" $unifiedResultsDir -Force
    Write-Host "    Gateway results copied" -ForegroundColor Green
} else {
    Write-Host "    WARNING: No gateway Allure results found at $gatewayAllureDir" -ForegroundColor Yellow
}

Write-Host ""

# ============================================
# STEP 3: Run Risk Engine Tests
# ============================================
Write-Host "STEP 3: Running Risk Engine Tests" -ForegroundColor Yellow
Write-Host "==================================" -ForegroundColor Yellow
Write-Host ""

Set-Location "$rootDir\risk-engine"

Write-Host "  Building and testing risk-engine..." -ForegroundColor Cyan
$riskResult = & mvn clean test -B 2>&1
$riskSuccess = $LASTEXITCODE -eq 0

if ($riskSuccess) {
    Write-Host "    SUCCESS: Risk engine tests passed" -ForegroundColor Green
} else {
    Write-Host "    WARNING: Some risk engine tests failed (continuing anyway)" -ForegroundColor Yellow
}

# Copy risk-engine Allure results
$riskAllureDir = "$rootDir\risk-engine\target\allure-results"
if (Test-Path $riskAllureDir) {
    $riskFiles = Get-ChildItem $riskAllureDir -File
    Write-Host "    Copying $($riskFiles.Count) risk-engine result files..." -ForegroundColor Cyan
    Copy-Item "$riskAllureDir\*" $unifiedResultsDir -Force
    Write-Host "    Risk engine results copied" -ForegroundColor Green
} else {
    Write-Host "    WARNING: No risk-engine Allure results found at $riskAllureDir" -ForegroundColor Yellow
}

Write-Host ""

# ============================================
# STEP 4: Run Frontend Tests
# ============================================
Write-Host "STEP 4: Running Frontend Tests" -ForegroundColor Yellow
Write-Host "===============================" -ForegroundColor Yellow
Write-Host ""

Set-Location "$rootDir\frontend"

Write-Host "  Running frontend unit tests..." -ForegroundColor Cyan
$frontendResult = & npm run test:unit -- --passWithNoTests 2>&1
$frontendSuccess = $LASTEXITCODE -eq 0

if ($frontendSuccess) {
    Write-Host "    SUCCESS: Frontend tests passed" -ForegroundColor Green
} else {
    Write-Host "    WARNING: Some frontend tests failed (continuing anyway)" -ForegroundColor Yellow
}

# Copy frontend Allure results
$frontendAllureDir = "$rootDir\frontend\allure-results"
if (Test-Path $frontendAllureDir) {
    $frontendFiles = Get-ChildItem $frontendAllureDir -File
    Write-Host "    Copying $($frontendFiles.Count) frontend result files..." -ForegroundColor Cyan
    Copy-Item "$frontendAllureDir\*" $unifiedResultsDir -Force
    Write-Host "    Frontend results copied" -ForegroundColor Green
} else {
    Write-Host "    WARNING: No frontend Allure results found at $frontendAllureDir" -ForegroundColor Yellow
}

Write-Host ""

# ============================================
# STEP 5: Validate and Analyze Results
# ============================================
Write-Host "STEP 5: Analyzing Unified Results" -ForegroundColor Yellow
Write-Host "==================================" -ForegroundColor Yellow
Write-Host ""

Set-Location $rootDir

$allResultFiles = Get-ChildItem "$unifiedResultsDir\*-result.json" -ErrorAction SilentlyContinue
$allContainerFiles = Get-ChildItem "$unifiedResultsDir\*-container.json" -ErrorAction SilentlyContinue

Write-Host "  Total results collected:" -ForegroundColor Cyan
Write-Host "    Test results: $($allResultFiles.Count)" -ForegroundColor White
Write-Host "    Test containers: $($allContainerFiles.Count)" -ForegroundColor White
Write-Host ""

if ($allResultFiles.Count -eq 0) {
    Write-Host "  ERROR: No test results found!" -ForegroundColor Red
    exit 1
}

# Validate JSON integrity
$invalidFiles = 0
$validFiles = 0
$testsByService = @{}
$testsByStory = @{}

foreach ($file in $allResultFiles) {
    try {
        $content = Get-Content $file.FullName -Raw
        $json = $content | ConvertFrom-Json
        $validFiles++
        
        # Extract service from labels
        $serviceLabel = $json.labels | Where-Object { $_.name -eq "service" } | Select-Object -First 1
        $service = if ($serviceLabel) { $serviceLabel.value } else { "unknown" }
        
        if (!$testsByService.ContainsKey($service)) {
            $testsByService[$service] = 0
        }
        $testsByService[$service]++
        
        # Extract story ID from test name
        if ($json.name -match '\[story:([\w.-]+)\]') {
            $storyId = $Matches[1]
            if (!$testsByStory.ContainsKey($storyId)) {
                $testsByStory[$storyId] = 0
            }
            $testsByStory[$storyId]++
        }
    } catch {
        Write-Host "    WARNING: Invalid JSON - $($file.Name)" -ForegroundColor Yellow
        $invalidFiles++
    }
}

Write-Host "  JSON Validation:" -ForegroundColor Cyan
Write-Host "    Valid files: $validFiles" -ForegroundColor Green
if ($invalidFiles -gt 0) {
    Write-Host "    Invalid files: $invalidFiles" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "  Tests by Service:" -ForegroundColor Cyan
foreach ($service in $testsByService.GetEnumerator() | Sort-Object Name) {
    Write-Host "    $($service.Key): $($service.Value) test(s)" -ForegroundColor White
}
Write-Host ""

Write-Host "  Tests by Story:" -ForegroundColor Cyan
foreach ($story in $testsByStory.GetEnumerator() | Sort-Object Name) {
    Write-Host "    $($story.Key): $($story.Value) test(s)" -ForegroundColor White
}
Write-Host ""

# ============================================
# STEP 6: Generate Unified Allure Report
# ============================================
Write-Host "STEP 6: Generating Unified Allure Report" -ForegroundColor Yellow
Write-Host "=========================================" -ForegroundColor Yellow
Write-Host ""

$hasAllure = Get-Command allure -ErrorAction SilentlyContinue
if (!$hasAllure) {
    Write-Host "  ERROR: Allure CLI not found!" -ForegroundColor Red
    Write-Host "  Install with: npm install -g allure-commandline" -ForegroundColor Yellow
    exit 1
}

# Create executor.json
$executorJson = @{
    name = "Local Test Run"
    type = "local"
    buildName = "Unified Local Build"
    reportName = "CDS Platform - Unified Test Report"
    reportUrl = "http://localhost:8080"
} | ConvertTo-Json

Set-Content -Path "$unifiedResultsDir\executor.json" -Value $executorJson -Force
Write-Host "  Created executor.json" -ForegroundColor Green

# Generate report
Write-Host "  Generating HTML report..." -ForegroundColor Cyan
$reportGenResult = & allure generate $unifiedResultsDir --clean -o allure-report 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "  SUCCESS: Unified report generated" -ForegroundColor Green
    Write-Host ""
    
    # Display summary
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Report Generation Complete!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Summary:" -ForegroundColor Cyan
    Write-Host "  Total test results: $($allResultFiles.Count)" -ForegroundColor White
    Write-Host "  Services tested: $($testsByService.Count)" -ForegroundColor White
    Write-Host "  Stories covered: $($testsByStory.Count)" -ForegroundColor White
    Write-Host "  Valid JSON files: $validFiles" -ForegroundColor White
    Write-Host ""
    Write-Host "Report location: $rootDir\allure-report\index.html" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Opening report in browser..." -ForegroundColor Cyan
    
    # Open report
    & allure open allure-report
    
} else {
    Write-Host "  FAILED: Report generation failed" -ForegroundColor Red
    Write-Host "  Error output:" -ForegroundColor Yellow
    Write-Host $reportGenResult
    exit 1
}
