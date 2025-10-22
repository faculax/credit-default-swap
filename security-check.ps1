# CDS Platform Security Check Script
# Runs all security and quality checks locally

Write-Host "CDS Platform Security and Quality Analysis" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Continue"
$rootDir = $PSScriptRoot
$hasErrors = $false

# Function to print section headers
function Write-Section {
    param($text)
    Write-Host ""
    Write-Host ">> $text" -ForegroundColor Yellow
    Write-Host ("-" * 50) -ForegroundColor DarkGray
}

# Function to check if command exists
function Test-Command {
    param($command)
    $null = Get-Command $command -ErrorAction SilentlyContinue
    return $?
}

# Check prerequisites
Write-Section "Checking Prerequisites"

if (Test-Command "java") {
    $javaVersion = java -version 2>&1 | Select-String "version" | ForEach-Object { $_.Line }
    Write-Host "[OK] Java: $javaVersion" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Java not found. Please install Java 17 or higher." -ForegroundColor Red
    $hasErrors = $true
}

if (Test-Command "node") {
    $nodeVersion = node --version
    Write-Host "[OK] Node.js: $nodeVersion" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Node.js not found. Please install Node.js 18 or higher." -ForegroundColor Red
    $hasErrors = $true
}

if (Test-Command "npm") {
    $npmVersion = npm --version
    Write-Host "[OK] npm: v$npmVersion" -ForegroundColor Green
} else {
    Write-Host "[ERROR] npm not found. Please install npm." -ForegroundColor Red
    $hasErrors = $true
}

if ($hasErrors) {
    Write-Host ""
    Write-Host "[ERROR] Prerequisites check failed. Please install missing tools." -ForegroundColor Red
    exit 1
}

# Backend Services Security Checks
$services = @("backend", "gateway", "risk-engine")

foreach ($service in $services) {
    Write-Section "Analyzing $service Service"
    
    $servicePath = Join-Path $rootDir $service
    
    if (Test-Path $servicePath) {
        Push-Location $servicePath
        
        Write-Host "Running Maven security checks..." -ForegroundColor Gray
        
        # Check if Maven wrapper exists
        if (Test-Path "./mvnw") {
            # Compile the project
            Write-Host "  - Compiling..." -ForegroundColor Gray
            & ./mvnw clean compile -DskipTests -q
            
            # Run SpotBugs
            Write-Host "  - Running SpotBugs security analysis..." -ForegroundColor Gray
            & ./mvnw spotbugs:check -q
            if ($LASTEXITCODE -ne 0) {
                Write-Host "  [WARN] SpotBugs found issues in $service" -ForegroundColor Yellow
            } else {
                Write-Host "  [OK] SpotBugs: No issues found" -ForegroundColor Green
            }
            
            # Run OWASP Dependency Check (skip on local for speed, uncomment if needed)
            # Write-Host "  - Running OWASP dependency check..." -ForegroundColor Gray
            # & ./mvnw dependency-check:check -q
            # if ($LASTEXITCODE -ne 0) {
            #     Write-Host "  [WARN] OWASP found vulnerable dependencies in $service" -ForegroundColor Yellow
            # } else {
            #     Write-Host "  [OK] OWASP: No vulnerabilities found" -ForegroundColor Green
            # }
            
            # Run PMD
            Write-Host "  - Running PMD static analysis..." -ForegroundColor Gray
            & ./mvnw pmd:check -q
            if ($LASTEXITCODE -ne 0) {
                Write-Host "  [WARN] PMD found issues in $service" -ForegroundColor Yellow
            } else {
                Write-Host "  [OK] PMD: No issues found" -ForegroundColor Green
            }
            
            # Run Checkstyle
            Write-Host "  - Running Checkstyle..." -ForegroundColor Gray
            & ./mvnw checkstyle:check -q
            if ($LASTEXITCODE -ne 0) {
                Write-Host "  [WARN] Checkstyle found issues in $service" -ForegroundColor Yellow
            } else {
                Write-Host "  [OK] Checkstyle: No issues found" -ForegroundColor Green
            }
            
            Write-Host "[OK] $service analysis complete" -ForegroundColor Green
        } else {
            Write-Host "[ERROR] Maven wrapper not found in $service" -ForegroundColor Red
        }
        
        Pop-Location
    } else {
        Write-Host "[WARN] Service directory not found: $servicePath" -ForegroundColor Yellow
    }
}

# Frontend Security Checks
Write-Section "Analyzing Frontend"

$frontendPath = Join-Path $rootDir "frontend"

if (Test-Path $frontendPath) {
    Push-Location $frontendPath
    
    # Check if node_modules exists, install if not
    if (-not (Test-Path "node_modules")) {
        Write-Host "Installing npm dependencies..." -ForegroundColor Gray
        npm install
    }
    
    # Type checking
    Write-Host "  - Running TypeScript type check..." -ForegroundColor Gray
    npm run type-check
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [WARN] TypeScript type errors found" -ForegroundColor Yellow
    } else {
        Write-Host "  [OK] TypeScript: No type errors" -ForegroundColor Green
    }
    
    # Security linting
    Write-Host "  - Running security linting..." -ForegroundColor Gray
    npm run lint:security
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [WARN] ESLint found security issues" -ForegroundColor Yellow
    } else {
        Write-Host "  [OK] ESLint: No security issues" -ForegroundColor Green
    }
    
    # Format checking
    Write-Host "  - Checking code formatting..." -ForegroundColor Gray
    npm run format:check
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [WARN] Code formatting issues found (run 'npm run format' to fix)" -ForegroundColor Yellow
    } else {
        Write-Host "  [OK] Prettier: Code is properly formatted" -ForegroundColor Green
    }
    
    # npm audit
    Write-Host "  - Running npm audit..." -ForegroundColor Gray
    npm audit --audit-level=moderate
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [WARN] npm audit found vulnerable dependencies" -ForegroundColor Yellow
    } else {
        Write-Host "  [OK] npm audit: No vulnerabilities found" -ForegroundColor Green
    }
    
    Write-Host "[OK] Frontend analysis complete" -ForegroundColor Green
    
    Pop-Location
} else {
    Write-Host "[WARN] Frontend directory not found: $frontendPath" -ForegroundColor Yellow
}

# Summary
Write-Section "Security Analysis Complete"

Write-Host ""
Write-Host "REPORTS:" -ForegroundColor Cyan
Write-Host "  - Backend: backend/target/spotbugsXml.xml, backend/target/dependency-check-report.html" -ForegroundColor Gray
Write-Host "  - Gateway: gateway/target/spotbugsXml.xml, gateway/target/dependency-check-report.html" -ForegroundColor Gray
Write-Host "  - Risk Engine: risk-engine/target/spotbugsXml.xml, risk-engine/target/dependency-check-report.html" -ForegroundColor Gray
Write-Host "  - Frontend: Check console output above" -ForegroundColor Gray
Write-Host ""
Write-Host "TIPS:" -ForegroundColor Cyan
Write-Host "  - Review any warnings or errors above" -ForegroundColor Gray
Write-Host "  - Fix critical and high severity issues before committing" -ForegroundColor Gray
Write-Host "  - Run this script regularly during development" -ForegroundColor Gray
Write-Host "  - See SECURITY_SETUP.md for detailed documentation" -ForegroundColor Gray
Write-Host ""
Write-Host "[SUCCESS] All checks completed!" -ForegroundColor Green
