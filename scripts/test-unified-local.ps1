# Simple Unified Test Script - Run all tests and generate Allure report
# Handles errors gracefully and continues even if some tests fail

$ErrorActionPreference = "Continue"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "CDS Platform - Unified Test Report" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$rootDir = "C:\Users\AyodeleOladeji\Documents\dev\credit-default-swap"
$unifiedDir = "$rootDir\allure-results-unified"

# Clean previous results
Write-Host "Cleaning previous results..." -ForegroundColor Yellow
if (Test-Path $unifiedDir) { Remove-Item $unifiedDir -Recurse -Force }
if (Test-Path "$rootDir\allure-report") { Remove-Item "$rootDir\allure-report" -Recurse -Force }
New-Item -ItemType Directory -Path $unifiedDir -Force | Out-Null
Write-Host "  Done`n" -ForegroundColor Green

# BACKEND TESTS
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "STEP 1: Backend Tests (Unit + Integration)" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Set-Location "$rootDir\backend"

# Run unit tests first
Write-Host "Running unit tests...`n" -ForegroundColor Cyan
mvn clean test -B -q
$backendUnitExitCode = $LASTEXITCODE

if ($backendUnitExitCode -eq 0) {
    Write-Host "  [OK] Unit tests PASSED" -ForegroundColor Green
} else {
    Write-Host "  [WARN] Unit tests had failures (continuing)" -ForegroundColor Yellow
}

# Run integration tests
Write-Host "Running integration tests...`n" -ForegroundColor Cyan
mvn test -Pintegration-tests -B -q
$backendIntegrationExitCode = $LASTEXITCODE

if ($backendIntegrationExitCode -eq 0) {
    Write-Host "  [OK] Integration tests PASSED`n" -ForegroundColor Green
} else {
    Write-Host "  [WARN] Integration tests had failures (continuing)`n" -ForegroundColor Yellow
}

$backendExitCode = if ($backendUnitExitCode -eq 0 -and $backendIntegrationExitCode -eq 0) { 0 } else { 1 }

# Copy backend results
if (Test-Path "$rootDir\backend\target\allure-results") {
    $count = (Get-ChildItem "$rootDir\backend\target\allure-results" -File).Count
    Copy-Item "$rootDir\backend\target\allure-results\*" $unifiedDir -Force
    Write-Host "  Copied $count backend result files (unit + integration)`n" -ForegroundColor Cyan
}

# GATEWAY TESTS
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "STEP 2: Gateway Tests" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Set-Location "$rootDir\gateway"
Write-Host "Running Gateway tests...`n" -ForegroundColor Cyan

mvn test -B -q
$gatewayExitCode = $LASTEXITCODE

if ($gatewayExitCode -eq 0) {
    Write-Host "  [OK] Gateway tests PASSED`n" -ForegroundColor Green
} else {
    Write-Host "  [WARN] Gateway tests had failures (continuing)`n" -ForegroundColor Yellow
}

# Copy gateway results
if (Test-Path "$rootDir\gateway\target\allure-results") {
    $count = (Get-ChildItem "$rootDir\gateway\target\allure-results" -File).Count
    Copy-Item "$rootDir\gateway\target\allure-results\*" $unifiedDir -Force
    Write-Host "  Copied $count gateway result files`n" -ForegroundColor Cyan
}

# RISK ENGINE TESTS
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "STEP 3: Risk Engine Tests" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Set-Location "$rootDir\risk-engine"
Write-Host "Running Risk Engine tests...`n" -ForegroundColor Cyan

mvn test -B -q
$riskExitCode = $LASTEXITCODE

if ($riskExitCode -eq 0) {
    Write-Host "  [OK] Risk Engine tests PASSED`n" -ForegroundColor Green
} else {
    Write-Host "  [WARN] Risk Engine tests had failures (continuing)`n" -ForegroundColor Yellow
}

# Copy risk engine results
if (Test-Path "$rootDir\risk-engine\target\allure-results") {
    $count = (Get-ChildItem "$rootDir\risk-engine\target\allure-results" -File).Count
    Copy-Item "$rootDir\risk-engine\target\allure-results\*" $unifiedDir -Force
    Write-Host "  Copied $count risk-engine result files`n" -ForegroundColor Cyan
}

# FRONTEND TESTS
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "STEP 4: Frontend Tests" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Set-Location "$rootDir\frontend"
Write-Host "Running Frontend tests...`n" -ForegroundColor Cyan

npm run test:unit -- --passWithNoTests --watchAll=false 2>&1 | Out-Null
$frontendExitCode = $LASTEXITCODE

if ($frontendExitCode -eq 0) {
    Write-Host "  [OK] Frontend tests PASSED`n" -ForegroundColor Green
} else {
    Write-Host "  [WARN] Frontend tests had failures (continuing)`n" -ForegroundColor Yellow
}

# Copy frontend results
if (Test-Path "$rootDir\frontend\allure-results") {
    $count = (Get-ChildItem "$rootDir\frontend\allure-results" -File).Count
    Copy-Item "$rootDir\frontend\allure-results\*" $unifiedDir -Force
    Write-Host "  Copied $count frontend result files`n" -ForegroundColor Cyan
}

# ANALYZE RESULTS
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "STEP 5: Analyzing Results" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Set-Location $rootDir

$allResults = Get-ChildItem "$unifiedDir\*-result.json" -ErrorAction SilentlyContinue
Write-Host "  Total test results: $($allResults.Count)`n" -ForegroundColor Cyan

# Count by service
$services = @{}
foreach ($file in $allResults) {
    $json = Get-Content $file.FullName -Raw | ConvertFrom-Json
    $serviceLabel = $json.labels | Where-Object { $_.name -eq "service" } | Select-Object -First 1
    $service = if ($serviceLabel) { $serviceLabel.value } else { "unknown" }
    if (!$services.ContainsKey($service)) { $services[$service] = 0 }
    $services[$service]++
}

Write-Host "  Tests by Service:" -ForegroundColor Cyan
foreach ($s in $services.GetEnumerator() | Sort-Object Name) {
    Write-Host "    $($s.Key): $($s.Value) tests" -ForegroundColor White
}
Write-Host ""

# GENERATE REPORT
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "STEP 6: Generating Allure Report" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow

# Create executor.json
$executor = @{
    name = "Local Test Run"
    type = "local"
    buildName = "Unified Build"
    reportName = "CDS Platform - All Services"
} | ConvertTo-Json
Set-Content -Path "$unifiedDir\executor.json" -Value $executor -Force

Write-Host "Generating HTML report...`n" -ForegroundColor Cyan
allure generate $unifiedDir --clean -o allure-report 2>&1 | Out-Null

if ($LASTEXITCODE -eq 0) {
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "[OK] REPORT GENERATED SUCCESSFULLY!" -ForegroundColor Green
    Write-Host "========================================`n" -ForegroundColor Green
    
    Write-Host "Summary:" -ForegroundColor Cyan
    Write-Host "  Backend:    $(if($backendExitCode -eq 0){'[OK] PASSED'}else{'[WARN] FAILED'})" -ForegroundColor $(if($backendExitCode -eq 0){'Green'}else{'Yellow'})
    Write-Host "  Gateway:    $(if($gatewayExitCode -eq 0){'[OK] PASSED'}else{'[WARN] FAILED'})" -ForegroundColor $(if($gatewayExitCode -eq 0){'Green'}else{'Yellow'})
    Write-Host "  Risk Eng:   $(if($riskExitCode -eq 0){'[OK] PASSED'}else{'[WARN] FAILED'})" -ForegroundColor $(if($riskExitCode -eq 0){'Green'}else{'Yellow'})
    Write-Host "  Frontend:   $(if($frontendExitCode -eq 0){'[OK] PASSED'}else{'[WARN] FAILED'})" -ForegroundColor $(if($frontendExitCode -eq 0){'Green'}else{'Yellow'})
    Write-Host "  Total Results: $($allResults.Count)" -ForegroundColor White
    Write-Host "`nReport: $rootDir\allure-report\index.html`n" -ForegroundColor Cyan
    
    Write-Host "Opening report in browser..." -ForegroundColor Cyan
    allure open allure-report
} else {
    Write-Host "[ERROR] Report generation failed" -ForegroundColor Red
}
