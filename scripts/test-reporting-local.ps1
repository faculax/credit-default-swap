# Local Testing Script for Allure Reporting
# This script validates the entire reporting pipeline locally

$ErrorActionPreference = "Stop"
$ReportRoot = Join-Path $PSScriptRoot ".."

Write-Host "🧪 CDS Platform - Local Reporting Test" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

# Function to check if a command exists
function Test-Command {
    param($Command)
    try {
        Get-Command $Command -ErrorAction Stop | Out-Null
        return $true
    } catch {
        return $false
    }
}

# Step 1: Validate Prerequisites
Write-Host "📋 Step 1: Checking Prerequisites..." -ForegroundColor Yellow
Write-Host ""

$hasNode = Test-Command "node"
$hasNpm = Test-Command "npm"
$hasMaven = Test-Command "mvn"
$hasAllure = Test-Command "allure"

Write-Host "  ✓ Node.js: $(if($hasNode){'✅ Installed'}else{'❌ Missing'})"
Write-Host "  ✓ npm: $(if($hasNpm){'✅ Installed'}else{'❌ Missing'})"
Write-Host "  ✓ Maven: $(if($hasMaven){'✅ Installed'}else{'❌ Missing'})"
Write-Host "  ✓ Allure CLI: $(if($hasAllure){'✅ Installed'}else{'⚠️  Missing (optional - can install via npm)'})"
Write-Host ""

if (!$hasNode -or !$hasNpm) {
    Write-Host "❌ Node.js and npm are required!" -ForegroundColor Red
    exit 1
}

# Step 2: Install Allure CLI if missing
if (!$hasAllure) {
    Write-Host "📦 Step 2: Installing Allure CLI..." -ForegroundColor Yellow
    Write-Host ""
    try {
        npm install -g allure-commandline --save-dev
        Write-Host "  ✅ Allure CLI installed successfully" -ForegroundColor Green
        Write-Host ""
    } catch {
        Write-Host "  ⚠️  Failed to install Allure CLI. You can:" -ForegroundColor Yellow
        Write-Host "     - Run: npm install -g allure-commandline"
        Write-Host "     - Or download from: https://github.com/allure-framework/allure2/releases"
        Write-Host "     - Then add to PATH"
        Write-Host ""
        Write-Host "  Continuing without local report generation..." -ForegroundColor Yellow
        Write-Host ""
    }
}

# Step 3: Run Frontend Tests
Write-Host "🎯 Step 3: Running Frontend Tests..." -ForegroundColor Yellow
Write-Host ""

Push-Location (Join-Path $ReportRoot "frontend")

Write-Host "  Running unit tests..." -ForegroundColor Cyan
npm run test:unit -- --passWithNoTests 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "    ✅ Unit tests passed" -ForegroundColor Green
} else {
    Write-Host "    ❌ Unit tests failed" -ForegroundColor Red
}

Write-Host "  Running integration tests..." -ForegroundColor Cyan
npm run test:integration -- --passWithNoTests 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "    ✅ Integration tests passed" -ForegroundColor Green
} else {
    Write-Host "    ❌ Integration tests failed" -ForegroundColor Red
}

Write-Host ""

# Step 4: Validate Allure Results
Write-Host "🔍 Step 4: Validating Allure Results..." -ForegroundColor Yellow
Write-Host ""

$resultFiles = Get-ChildItem "allure-results\*-result.json" -ErrorAction SilentlyContinue
$containerFiles = Get-ChildItem "allure-results\*-container.json" -ErrorAction SilentlyContinue

Write-Host "  📊 Results found:"
Write-Host "    - Test results: $($resultFiles.Count)" -ForegroundColor White
Write-Host "    - Test containers: $($containerFiles.Count)" -ForegroundColor White
Write-Host ""

if ($resultFiles.Count -eq 0) {
    Write-Host "  ❌ No test results found!" -ForegroundColor Red
    Pop-Location
    exit 1
}

# Validate JSON files
Write-Host "  Validating JSON integrity..." -ForegroundColor Cyan
$invalidFiles = 0
foreach ($file in $resultFiles) {
    try {
        $content = Get-Content $file.FullName -Raw
        $json = $content | ConvertFrom-Json
        
        # Check for story tags in test name
        if ($json.name -match '\[story:([\w.-]+)\]') {
            $storyId = $Matches[1]
            # Story tag found but not in labels - this is expected for Jest
        }
    } catch {
        Write-Host "    ⚠️  Invalid JSON: $($file.Name)" -ForegroundColor Yellow
        $invalidFiles++
    }
}

if ($invalidFiles -eq 0) {
    Write-Host "    ✅ All JSON files are valid" -ForegroundColor Green
} else {
    Write-Host "    ⚠️  Found $invalidFiles invalid JSON files" -ForegroundColor Yellow
}
Write-Host ""

# Step 5: Analyze Test Metadata
Write-Host "📊 Step 5: Analyzing Test Metadata..." -ForegroundColor Yellow
Write-Host ""

$sampleResult = Get-Content $resultFiles[0].FullName -Raw | ConvertFrom-Json
Write-Host "  Sample test result:"
Write-Host "    Name: $($sampleResult.name)" -ForegroundColor White
Write-Host "    Status: $($sampleResult.status)" -ForegroundColor $(if($sampleResult.status -eq 'passed'){'Green'}else{'Red'})
Write-Host "    Labels: $($sampleResult.labels.Count)" -ForegroundColor White

# Extract story IDs from test names
$storyPattern = '\[story:([\w.-]+)\]'
$stories = @{}
foreach ($file in $resultFiles) {
    $json = Get-Content $file.FullName -Raw | ConvertFrom-Json
    if ($json.name -match $storyPattern) {
        $storyId = $Matches[1]
        if (!$stories.ContainsKey($storyId)) {
            $stories[$storyId] = 0
        }
        $stories[$storyId]++
    }
}

Write-Host ""
Write-Host "  📋 Story Coverage:"
foreach ($story in $stories.GetEnumerator() | Sort-Object Name) {
    Write-Host "    - $($story.Key): $($story.Value) test(s)" -ForegroundColor White
}
Write-Host ""

# Step 6: Generate HTML Report (if Allure CLI available)
if (Test-Command "allure") {
    Write-Host "📄 Step 6: Generating HTML Report..." -ForegroundColor Yellow
    Write-Host ""
    
    # Create executor.json to prevent NullPointerException
    $executorJson = @{
        name = "Local Test Run"
        type = "local"
        buildName = "Local Build"
        reportName = "Local Test Report"
    } | ConvertTo-Json
    
    Set-Content -Path "allure-results\executor.json" -Value $executorJson
    
    try {
        allure generate allure-results --clean -o allure-report 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✅ HTML report generated successfully" -ForegroundColor Green
            Write-Host "  📂 Report location: $(Join-Path (Get-Location) 'allure-report\index.html')" -ForegroundColor Cyan
            Write-Host ""
            
            # Open report in browser
            Write-Host "  🌐 Opening report in browser..." -ForegroundColor Cyan
            $reportPath = Join-Path (Get-Location) "allure-report\index.html"
            Start-Process $reportPath
        } else {
            Write-Host "  ❌ Failed to generate HTML report" -ForegroundColor Red
        }
    } catch {
        Write-Host "  ❌ Error generating report: $_" -ForegroundColor Red
    }
} else {
    Write-Host "📄 Step 6: HTML Report Generation (Skipped)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  ℹ️  Allure CLI not available. To generate reports:" -ForegroundColor Cyan
    Write-Host "     npm install -g allure-commandline"
    Write-Host "     allure generate allure-results -o allure-report"
    Write-Host "     allure open allure-report"
    Write-Host ""
}

Pop-Location

# Step 7: Backend Tests (Optional)
Write-Host "🎯 Step 7: Backend Tests (Optional)..." -ForegroundColor Yellow
Write-Host ""

if ($hasMaven) {
    $backendServices = @("backend", "gateway", "risk-engine")
    
    foreach ($service in $backendServices) {
        $servicePath = Join-Path $ReportRoot $service
        if (Test-Path $servicePath) {
            Write-Host "  Testing $service..." -ForegroundColor Cyan
            Push-Location $servicePath
            
            mvn test -DskipTests=false -Dmaven.test.failure.ignore=true 2>&1 | Out-Null
            
            $allureResultsPath = "target\allure-results"
            if (Test-Path $allureResultsPath) {
                $serviceResults = Get-ChildItem "$allureResultsPath\*-result.json" -ErrorAction SilentlyContinue
                Write-Host "    ✅ Generated $($serviceResults.Count) test results" -ForegroundColor Green
            } else {
                Write-Host "    ⚠️  No Allure results found" -ForegroundColor Yellow
            }
            
            Pop-Location
        }
    }
} else {
    Write-Host "  ⚠️  Maven not installed - skipping backend tests" -ForegroundColor Yellow
}
Write-Host ""

# Summary
Write-Host "✅ Testing Complete!" -ForegroundColor Green
Write-Host "===================" -ForegroundColor Green
Write-Host ""
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  - Frontend test results: $($resultFiles.Count)" -ForegroundColor White
Write-Host "  - Stories covered: $($stories.Count)" -ForegroundColor White
Write-Host "  - Invalid JSON files: $invalidFiles" -ForegroundColor White
Write-Host ""

if (Test-Command "allure") {
    Write-Host "💡 Next Steps:" -ForegroundColor Cyan
    Write-Host "  - View the report in your browser"
    Write-Host "  - Or run: allure open frontend/allure-report"
    Write-Host ""
} else {
    Write-Host "💡 To generate HTML reports:" -ForegroundColor Cyan
    Write-Host "  npm install -g allure-commandline"
    Write-Host "  cd frontend"
    Write-Host "  allure generate allure-results -o allure-report"
    Write-Host "  allure open allure-report"
    Write-Host ""
}

Write-Host "🚀 Ready to push to CI!" -ForegroundColor Green
