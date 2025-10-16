# Quick validation script to verify security configurations

Write-Host "Validating Security Configuration Files" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

$rootDir = $PSScriptRoot
$allValid = $true

function Test-FileExists {
    param($path, $description)
    if (Test-Path $path) {
        Write-Host "OK: $description" -ForegroundColor Green
        return $true
    } else {
        Write-Host "MISSING: $description" -ForegroundColor Red
        Write-Host "  Expected: $path" -ForegroundColor Gray
        return $false
    }
}

function Test-XmlFile {
    param($path, $description)
    if (Test-Path $path) {
        try {
            [xml]$xml = Get-Content $path
            Write-Host "OK: $description (valid XML)" -ForegroundColor Green
            return $true
        } catch {
            Write-Host "ERROR: $description (invalid XML)" -ForegroundColor Red
            return $false
        }
    } else {
        Write-Host "MISSING: $description" -ForegroundColor Red
        return $false
    }
}

function Test-JsonFile {
    param($path, $description)
    if (Test-Path $path) {
        try {
            $json = Get-Content $path -Raw | ConvertFrom-Json
            Write-Host "OK: $description (valid JSON)" -ForegroundColor Green
            return $true
        } catch {
            Write-Host "ERROR: $description (invalid JSON)" -ForegroundColor Red
            return $false
        }
    } else {
        Write-Host "MISSING: $description" -ForegroundColor Red
        return $false
    }
}

# GitHub Actions Workflow
Write-Host "GitHub Actions:" -ForegroundColor Yellow
$allValid = (Test-FileExists (Join-Path $rootDir ".github\workflows\cds-security-quality.yml") "GitHub Actions workflow") -and $allValid
Write-Host ""

# Backend Service
Write-Host "Backend Service:" -ForegroundColor Yellow
$allValid = (Test-XmlFile (Join-Path $rootDir "backend\pom.xml") "backend/pom.xml") -and $allValid
$allValid = (Test-XmlFile (Join-Path $rootDir "backend\spotbugs-security-include.xml") "backend/spotbugs-security-include.xml") -and $allValid
$allValid = (Test-XmlFile (Join-Path $rootDir "backend\owasp-suppressions.xml") "backend/owasp-suppressions.xml") -and $allValid
$allValid = (Test-XmlFile (Join-Path $rootDir "backend\checkstyle.xml") "backend/checkstyle.xml") -and $allValid
Write-Host ""

# Gateway Service
Write-Host "Gateway Service:" -ForegroundColor Yellow
$allValid = (Test-XmlFile (Join-Path $rootDir "gateway\pom.xml") "gateway/pom.xml") -and $allValid
$allValid = (Test-XmlFile (Join-Path $rootDir "gateway\spotbugs-security-include.xml") "gateway/spotbugs-security-include.xml") -and $allValid
$allValid = (Test-XmlFile (Join-Path $rootDir "gateway\owasp-suppressions.xml") "gateway/owasp-suppressions.xml") -and $allValid
$allValid = (Test-XmlFile (Join-Path $rootDir "gateway\checkstyle.xml") "gateway/checkstyle.xml") -and $allValid
Write-Host ""

# Risk Engine Service
Write-Host "Risk Engine Service:" -ForegroundColor Yellow
$allValid = (Test-XmlFile (Join-Path $rootDir "risk-engine\pom.xml") "risk-engine/pom.xml") -and $allValid
$allValid = (Test-XmlFile (Join-Path $rootDir "risk-engine\spotbugs-security-include.xml") "risk-engine/spotbugs-security-include.xml") -and $allValid
$allValid = (Test-XmlFile (Join-Path $rootDir "risk-engine\owasp-suppressions.xml") "risk-engine/owasp-suppressions.xml") -and $allValid
$allValid = (Test-XmlFile (Join-Path $rootDir "risk-engine\checkstyle.xml") "risk-engine/checkstyle.xml") -and $allValid
Write-Host ""

# Frontend
Write-Host "Frontend:" -ForegroundColor Yellow
$allValid = (Test-JsonFile (Join-Path $rootDir "frontend\package.json") "frontend/package.json") -and $allValid
$allValid = (Test-FileExists (Join-Path $rootDir "frontend\.eslintrc.security.js") "frontend/.eslintrc.security.js") -and $allValid
$allValid = (Test-JsonFile (Join-Path $rootDir "frontend\.prettierrc.json") "frontend/.prettierrc.json") -and $allValid
Write-Host ""

# Documentation and Scripts
Write-Host "Documentation and Scripts:" -ForegroundColor Yellow
$allValid = (Test-FileExists (Join-Path $rootDir "SECURITY_SETUP.md") "SECURITY_SETUP.md") -and $allValid
$allValid = (Test-FileExists (Join-Path $rootDir "security-check.ps1") "security-check.ps1") -and $allValid
$allValid = (Test-FileExists (Join-Path $rootDir ".gitignore") ".gitignore") -and $allValid
Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Cyan
if ($allValid) {
    Write-Host "All security configuration files are valid!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next Steps:" -ForegroundColor Cyan
    Write-Host "1. Install frontend dependencies: cd frontend; npm install" -ForegroundColor Gray
    Write-Host "2. Run security checks: .\security-check.ps1" -ForegroundColor Gray
    Write-Host "3. Review SECURITY_SETUP.md for detailed usage" -ForegroundColor Gray
    Write-Host "4. Commit and push changes to trigger CI/CD pipeline" -ForegroundColor Gray
    exit 0
} else {
    Write-Host "Some configuration files are missing or invalid." -ForegroundColor Red
    Write-Host "Please review the errors above." -ForegroundColor Gray
    exit 1
}