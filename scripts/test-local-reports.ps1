# Local Testing Script for Allure Reporting
# This script validates the entire reporting pipeline locally

$ErrorActionPreference = "Stop"

Write-Host "CDS Platform - Local Reporting Test" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Run Frontend Tests
Write-Host "Step 1: Running Frontend Tests..." -ForegroundColor Yellow
Write-Host ""

Set-Location "$PSScriptRoot\..\frontend"

Write-Host "  Running unit tests..." -ForegroundColor Cyan
$unitResult = & npm run test:unit -- --passWithNoTests 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "    SUCCESS: Unit tests passed" -ForegroundColor Green
} else {
    Write-Host "    FAILED: Unit tests failed" -ForegroundColor Red
}

Write-Host ""

# Step 2: Validate Allure Results
Write-Host "Step 2: Validating Allure Results..." -ForegroundColor Yellow
Write-Host ""

$resultFiles = Get-ChildItem "allure-results\*-result.json" -ErrorAction SilentlyContinue
$containerFiles = Get-ChildItem "allure-results\*-container.json" -ErrorAction SilentlyContinue

Write-Host "  Results found:"
Write-Host "    Test results: $($resultFiles.Count)" -ForegroundColor White
Write-Host "    Test containers: $($containerFiles.Count)" -ForegroundColor White
Write-Host ""

if ($resultFiles.Count -eq 0) {
    Write-Host "  ERROR: No test results found!" -ForegroundColor Red
    exit 1
}

# Step 3: Validate JSON files
Write-Host "Step 3: Validating JSON integrity..." -ForegroundColor Yellow
Write-Host ""

$invalidFiles = 0
$validFiles = 0

foreach ($file in $resultFiles) {
    try {
        $content = Get-Content $file.FullName -Raw
        $json = $content | ConvertFrom-Json
        $validFiles++
    } catch {
        Write-Host "    WARNING: Invalid JSON - $($file.Name)" -ForegroundColor Yellow
        $invalidFiles++
    }
}

Write-Host "    Valid JSON files: $validFiles" -ForegroundColor Green
if ($invalidFiles -gt 0) {
    Write-Host "    Invalid JSON files: $invalidFiles" -ForegroundColor Yellow
}
Write-Host ""

# Step 4: Analyze Test Metadata
Write-Host "Step 4: Analyzing Test Metadata..." -ForegroundColor Yellow
Write-Host ""

$sampleResult = Get-Content $resultFiles[0].FullName -Raw | ConvertFrom-Json
Write-Host "  Sample test result:"
Write-Host "    Name: $($sampleResult.name)" -ForegroundColor White
Write-Host "    Status: $($sampleResult.status)" -ForegroundColor $(if($sampleResult.status -eq 'passed'){'Green'}else{'Red'})

# Extract story IDs from test names
$storyPattern = '\[story:([\w.-]+)\]'
$stories = @{}
$testsWithStory = 0

foreach ($file in $resultFiles) {
    $json = Get-Content $file.FullName -Raw | ConvertFrom-Json
    if ($json.name -match $storyPattern) {
        $storyId = $Matches[1]
        if (!$stories.ContainsKey($storyId)) {
            $stories[$storyId] = 0
        }
        $stories[$storyId]++
        $testsWithStory++
    }
}

Write-Host ""
Write-Host "  Story Coverage:"
Write-Host "    Tests with story tags: $testsWithStory / $($resultFiles.Count)" -ForegroundColor White
foreach ($story in $stories.GetEnumerator() | Sort-Object Name) {
    Write-Host "    - $($story.Key): $($story.Value) test(s)" -ForegroundColor Cyan
}
Write-Host ""

# Step 5: Check for Allure CLI
Write-Host "Step 5: Checking Allure CLI..." -ForegroundColor Yellow
Write-Host ""

$hasAllure = Get-Command allure -ErrorAction SilentlyContinue
if ($hasAllure) {
    Write-Host "  Allure CLI found: $($hasAllure.Source)" -ForegroundColor Green
    Write-Host "  Generating HTML report..." -ForegroundColor Cyan
    Write-Host ""
    
    # Create executor.json
    $executorJson = @{
        name = "Local Test Run"
        type = "local"
        buildName = "Local Build"
        reportName = "Local Test Report"
    } | ConvertTo-Json
    
    Set-Content -Path "allure-results\executor.json" -Value $executorJson -Force
    
    # Generate report
    & allure generate allure-results --clean -o allure-report
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  SUCCESS: HTML report generated" -ForegroundColor Green
        $reportPath = Resolve-Path "allure-report\index.html"
        Write-Host "  Report location: $reportPath" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "  Opening report in browser..." -ForegroundColor Cyan
        Start-Process $reportPath
    } else {
        Write-Host "  FAILED: Report generation failed" -ForegroundColor Red
    }
} else {
    Write-Host "  Allure CLI not found" -ForegroundColor Yellow
    Write-Host "  To install: npm install -g allure-commandline" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  You can still push to CI - reports will generate there!" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Testing Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  Frontend test results: $($resultFiles.Count)" -ForegroundColor White
Write-Host "  Stories covered: $($stories.Count)" -ForegroundColor White
Write-Host "  Tests with story tags: $testsWithStory" -ForegroundColor White
Write-Host "  Invalid JSON files: $invalidFiles" -ForegroundColor White
Write-Host ""
Write-Host "Ready to push to CI!" -ForegroundColor Green
