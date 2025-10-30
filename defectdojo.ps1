#!/usr/bin/env pwsh
# ===============================================================================
# DefectDojo Management Script
# ===============================================================================
# Simplifies DefectDojo operations for local development
# Usage: ./defectdojo.ps1 [command]
# ===============================================================================

param(
    [Parameter(Position = 0)]
    [ValidateSet('start', 'stop', 'restart', 'status', 'logs', 'scan', 'upload', 'upload-components', 'init', 'clean', 'clean-tests', 'help')]
    [string]$Command = 'help'
)

$ErrorActionPreference = "Stop"
$ComposeFile = "compliance/docker-compose.defectdojo.yml"

# ANSI Colors
$ColorGreen = ""
$ColorYellow = ""
$ColorRed = ""
$ColorBlue = ""
$ColorCyan = ""
$ColorReset = ""

function Write-Banner {
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════════════"
    Write-Host " DEFECTDOJO COMPLIANCE PLATFORM"
    Write-Host "═══════════════════════════════════════════════════════════════"
    Write-Host ""
}

function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message"
}

function Write-Info {
    param([string]$Message)
    Write-Host "▶ $Message"
}

function Write-Warning {
    param([string]$Message)
    Write-Host "⚠ $Message"
}

function Write-Error-Message {
    param([string]$Message)
    Write-Host "✗ $Message"
}

function Show-Help {
    Write-Banner
    Write-Host "COMMANDS:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  start              Start DefectDojo services"
    Write-Host "  stop               Stop DefectDojo services"
    Write-Host "  restart            Restart DefectDojo services"
    Write-Host "  status             Check service status"
    Write-Host "  logs               View service logs"
    Write-Host "  scan               Run security scans on all components"
    Write-Host "  upload             Upload scan results (unified product with tags)"
    Write-Host "  upload-components  Upload scan results (separate product per component)"
    Write-Host "  init               Initialize DefectDojo (database, admin user, environments)"
    Write-Host "  clean              Remove all containers and volumes"
    Write-Host "  clean-tests        Remove duplicate tests from DefectDojo"
    Write-Host "  help               Show this help message"
    Write-Host ""
    Write-Host "EXAMPLES:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  # Start DefectDojo and initialize parsers"
    Write-Host "  ./defectdojo.ps1 start"
    Write-Host "  ./defectdojo.ps1 init"
    Write-Host ""
    Write-Host "  # Run scans and upload results (unified view)"
    Write-Host "  ./defectdojo.ps1 scan"
    Write-Host "  ./defectdojo.ps1 upload"
    Write-Host ""
    Write-Host "  # Upload with component-based organization"
    Write-Host "  ./defectdojo.ps1 upload-components"
    Write-Host ""
    Write-Host "  # View logs"
    Write-Host "  ./defectdojo.ps1 logs"
    Write-Host ""
    Write-Host "ACCESS:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  URL:      http://localhost:8081"
    Write-Host "  Username: admin"
    Write-Host "  Password: admin"
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════════════"
    Write-Host ""
}

function Start-DefectDojo {
    Write-Banner
    Write-Info "Starting DefectDojo services..."
    
    try {
        docker-compose -f $ComposeFile up -d
        
        Write-Host ""
        Write-Success "DefectDojo services started successfully"
        Write-Host ""
        Write-Info "Waiting for services to initialize (this may take 30-60 seconds)..."
        Start-Sleep -Seconds 5
        
        Write-Host ""
        Write-Host "SERVICE STATUS:" -ForegroundColor Yellow
        docker-compose -f $ComposeFile ps
        
        Write-Host ""
        Write-Host "ACCESS DEFECTDOJO:"
        Write-Host "  URL:      http://localhost:8081"
        Write-Host "  Username: admin"
        Write-Host "  Password: admin"
        Write-Host ""
        Write-Warning "Note: First startup may take 2-3 minutes for database initialization"
        Write-Warning "After first start, run: .\defectdojo.ps1 init"
        Write-Host ""
        
    } catch {
        Write-Error-Message "Failed to start DefectDojo: $($_.Exception.Message)"
        exit 1
    }
}

function Stop-DefectDojo {
    Write-Banner
    Write-Info "Stopping DefectDojo services..."
    
    try {
        docker-compose -f $ComposeFile down
        Write-Success "DefectDojo services stopped"
    } catch {
        Write-Error-Message "Failed to stop DefectDojo: $($_.Exception.Message)"
        exit 1
    }
}

function Restart-DefectDojo {
    Write-Banner
    Write-Info "Restarting DefectDojo services..."
    
    Stop-DefectDojo
    Start-Sleep -Seconds 2
    Start-DefectDojo
}

function Show-Status {
    Write-Banner
    Write-Info "DefectDojo Service Status:"
    Write-Host ""
    
    docker-compose -f $ComposeFile ps
    
    Write-Host ""
    Write-Info "Testing DefectDojo availability..."
    
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8081/login" -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            Write-Success "DefectDojo is accessible at http://localhost:8081"
        }
    } catch {
        Write-Warning "DefectDojo may not be fully initialized yet"
        Write-Info "Run: ./defectdojo.ps1 logs"
    }
}

function Show-Logs {
    Write-Banner
    Write-Info "Showing DefectDojo logs (Ctrl+C to exit)..."
    Write-Host ""
    
    docker-compose -f $ComposeFile logs -f
}

function Scan-JavaService {
    param(
        [string]$ServiceName,
        [string]$ServicePath
    )
    
    if (-not (Test-Path $ServicePath)) {
        Write-Warning "$ServiceName service not found at $ServicePath"
        return
    }
    
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host " $ServiceName SCANS (Java/Maven)" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""
    
    Push-Location $ServicePath
    
    try {
        Write-Info "Creating reports directory..."
        New-Item -ItemType Directory -Force -Path "target/security-reports" | Out-Null
        
        Write-Host ""
        Write-Info "Running OWASP Dependency Check..."
        & mvn org.owasp:dependency-check-maven:check "-Dformat=JSON" "-DprettyPrint=true"
        
        if (Test-Path "target/dependency-check-report.json") {
            Copy-Item "target/dependency-check-report.json" "target/security-reports/" -Force
            Write-Success "OWASP Dependency Check complete"
        }
        
        Write-Host ""
        Write-Info "Running SpotBugs security analysis..."
        & mvn compile spotbugs:spotbugs "-Dspotbugs.xmlOutput=true" "-Dspotbugs.xmlOutputDirectory=target"
        
        if (Test-Path "target/spotbugsXml.xml") {
            Write-Success "SpotBugs analysis complete"
        }
        
        Write-Host ""
        Write-Info "Running Checkstyle..."
        & mvn checkstyle:checkstyle
        if (Test-Path "target/checkstyle-result.xml") {
            Copy-Item "target/checkstyle-result.xml" "target/security-reports/" -Force
            Write-Success "Checkstyle complete"
        }
        
        Write-Success "$ServiceName scans completed!"
        
    } catch {
        Write-Warning "$ServiceName security scan encountered issues: $($_.Exception.Message)"
        Write-Info "Continuing with available results..."
    } finally {
        Pop-Location
    }
    
    Write-Host ""
}

function Run-SecurityScan {
    Write-Banner
    Write-Info "Running security scans on all components..."
    Write-Host ""
    
    # Backend scans (with full dependency checks)
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host " BACKEND SCANS (Java/Maven)" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""
    
    Push-Location backend
    
    try {
        Write-Info "Creating reports directory..."
        New-Item -ItemType Directory -Force -Path "target/security-reports" | Out-Null
        
        Write-Host ""
        Write-Info "Running OWASP Dependency Check..."
        & mvn org.owasp:dependency-check-maven:check "-Dformat=JSON" "-DprettyPrint=true"
        
        if (Test-Path "target/dependency-check-report.json") {
            Copy-Item "target/dependency-check-report.json" "target/security-reports/" -Force
            Write-Success "OWASP Dependency Check complete"
        }
        
        Write-Host ""
        Write-Info "Running SpotBugs security analysis..."
        & mvn compile spotbugs:spotbugs "-Dspotbugs.xmlOutput=true" "-Dspotbugs.xmlOutputDirectory=target"
        Write-Success "SpotBugs analysis complete"
        
        Write-Host ""
        Write-Info "Running Checkstyle..."
        & mvn checkstyle:checkstyle
        if (Test-Path "target/checkstyle-result.xml") {
            Copy-Item "target/checkstyle-result.xml" "target/security-reports/" -Force
            Write-Success "Checkstyle complete"
        }
        
        Write-Success "Backend scans completed!"
        
    } catch {
        Write-Error-Message "Backend security scan failed: $($_.Exception.Message)"
        exit 1
    } finally {
        Pop-Location
    }
    
    # Frontend scans
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host " FRONTEND SCANS (npm audit, ESLint, Retire.js)" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""
    
    Push-Location frontend
    
    try {
        Write-Info "Running comprehensive frontend security scans..."
        
        # Run all security scans
        npm run security:scan
        
        # Display results summary
        if (Test-Path "audit-npm.json") {
            try {
                $auditData = Get-Content "audit-npm.json" -Raw | ConvertFrom-Json
                if ($auditData.metadata) {
                    $vulnerabilities = $auditData.metadata.vulnerabilities
                    $total = ($vulnerabilities.info -as [int]) + ($vulnerabilities.low -as [int]) + 
                             ($vulnerabilities.moderate -as [int]) + ($vulnerabilities.high -as [int]) + 
                             ($vulnerabilities.critical -as [int])
                    
                    Write-Success "NPM Audit complete"
                    Write-Host "  📊 Total vulnerabilities: $total" -ForegroundColor Cyan
                    Write-Host "     Critical: $($vulnerabilities.critical)" -ForegroundColor Red
                    Write-Host "     High: $($vulnerabilities.high)" -ForegroundColor Yellow
                    Write-Host "     Moderate: $($vulnerabilities.moderate)" -ForegroundColor Yellow
                    Write-Host "     Low: $($vulnerabilities.low)" -ForegroundColor Gray
                    Write-Host "     Info: $($vulnerabilities.info)" -ForegroundColor Gray
                }
            } catch {
                Write-Info "NPM Audit report generated"
            }
        }
        
        if (Test-Path "eslint-security.json") {
            try {
                $eslintData = Get-Content "eslint-security.json" -Raw | ConvertFrom-Json
                $issuesCount = ($eslintData | ForEach-Object { $_.messages.Count } | Measure-Object -Sum).Sum
                Write-Success "ESLint Security scan complete - $issuesCount issues"
            } catch {
                Write-Info "ESLint Security report generated"
            }
        }
        
        if (Test-Path "retire-report.json") {
            Write-Success "Retire.js scan complete"
        }
        
        Write-Success "Frontend scans completed!"
        
    } catch {
        Write-Warning "Frontend security scan encountered issues: $($_.Exception.Message)"
        Write-Info "Continuing with available results..."
    } finally {
        Pop-Location
    }
    
    # Gateway scans
    Scan-JavaService -ServiceName "Gateway" -ServicePath "gateway"
    
    # Risk Engine scans
    Scan-JavaService -ServiceName "Risk-Engine" -ServicePath "risk-engine"
    
    # Secret Scanning with Gitleaks
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host " SECRET SCANNING (Gitleaks)" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""
    
    try {
        # Check if gitleaks is installed
        $gitleaksInstalled = Get-Command gitleaks -ErrorAction SilentlyContinue
        
        if (-not $gitleaksInstalled) {
            Write-Warning "Gitleaks not installed - skipping secret scanning"
            Write-Info "Install with: .\install-gitleaks.ps1  OR  choco install gitleaks"
            Write-Host ""
        } else {
            Write-Info "Running Gitleaks secret scan..."
            
            # Create security reports directory
            New-Item -ItemType Directory -Force -Path "security-reports" | Out-Null
            
            # Check if config exists
            $configParam = ""
            if (Test-Path ".gitleaks.toml") {
                $configParam = "--config=.gitleaks.toml"
                Write-Host "  Using config: .gitleaks.toml" -ForegroundColor Gray
            }
            
            # Run gitleaks detect (JSON format for DefectDojo)
            Write-Host "  Scanning for secrets..." -ForegroundColor Gray
            
            $gitleaksArgs = @(
                "detect",
                "--source", ".",
                "--report-format", "json",
                "--report-path", "security-reports/gitleaks-report.json",
                "--verbose"
            )
            
            if ($configParam) {
                $gitleaksArgs += $configParam
            }
            
            # Run gitleaks (exit code 1 means secrets found, which is OK for reporting)
            # Capture output but don't fail on exit code 1
            $gitleaksOutput = gitleaks detect --source . --config=.gitleaks.toml --report-path security-reports/gitleaks-report.json --report-format json --verbose 2>&1
            $gitleaksExitCode = $LASTEXITCODE
            
            if (Test-Path "security-reports/gitleaks-report.json") {
                Write-Success "Gitleaks scan complete"
                
                # Show summary
                try {
                    $gitleaksContent = Get-Content "security-reports/gitleaks-report.json" -Raw
                    if ($gitleaksContent -and $gitleaksContent.Trim() -ne "" -and $gitleaksContent -ne "null" -and $gitleaksContent -ne "[]") {
                        $gitleaksData = $gitleaksContent | ConvertFrom-Json
                        if ($gitleaksData -and $gitleaksData.Count -gt 0) {
                            $secretCount = ($gitleaksData | Measure-Object).Count
                            Write-Host "  ⚠️  Found $secretCount potential secret(s)!" -ForegroundColor Red
                            Write-Host "  Review: security-reports/gitleaks-report.json" -ForegroundColor Yellow
                            
                            # Show first few findings
                            $gitleaksData | Select-Object -First 3 | ForEach-Object {
                                Write-Host "    • $($_.File):$($_.StartLine) - $($_.RuleID)" -ForegroundColor Yellow
                            }
                        } else {
                            Write-Host "  ✓ No secrets detected" -ForegroundColor Green
                        }
                    } else {
                        Write-Host "  ✓ No secrets detected" -ForegroundColor Green
                    }
                } catch {
                    Write-Host "  Scan completed (see report for details)" -ForegroundColor Gray
                }
            } else {
                Write-Warning "Gitleaks report not generated"
            }
        }
    } catch {
        Write-Warning "Gitleaks scan encountered issues: $($_.Exception.Message)"
        Write-Info "Continuing with other scans..."
    }
    
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Green
    Write-Success "All security scans completed!"
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Green
    Write-Host ""
    Write-Info "Results saved to:"
    Write-Host "  Backend:" -ForegroundColor Cyan
    Write-Host "    - SpotBugs:    backend/target/spotbugsXml.xml" -ForegroundColor Gray
    Write-Host "    - OWASP:       backend/target/dependency-check-report.json" -ForegroundColor Gray
    Write-Host "    - Checkstyle:  backend/target/checkstyle-result.xml" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  Frontend:" -ForegroundColor Cyan
    Write-Host "    - ESLint:      frontend/eslint-security.json" -ForegroundColor Gray
    Write-Host "    - Retire.js:   frontend/retire-report.json" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  Gateway:" -ForegroundColor Cyan
    Write-Host "    - SpotBugs:    gateway/target/spotbugsXml.xml" -ForegroundColor Gray
    Write-Host "    - OWASP:       gateway/target/dependency-check-report.json" -ForegroundColor Gray
    Write-Host "    - Checkstyle:  gateway/target/checkstyle-result.xml" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  Risk-Engine:" -ForegroundColor Cyan
    Write-Host "    - SpotBugs:    risk-engine/target/spotbugsXml.xml" -ForegroundColor Gray
    Write-Host "    - OWASP:       risk-engine/target/dependency-check-report.json" -ForegroundColor Gray
    Write-Host "    - Checkstyle:  risk-engine/target/checkstyle-result.xml" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  Secret Scanning:" -ForegroundColor Cyan
    Write-Host "    - Gitleaks:    security-reports/gitleaks-report.json" -ForegroundColor Gray
    Write-Host ""
    Write-Info "Next step: Run './defectdojo.ps1 upload-components' to upload results to DefectDojo"
}

function Upload-Results {
    Write-Banner
    Write-Info "Uploading scan results to DefectDojo with component tags..."
    Write-Host ""
    
    $uploadScript = "compliance/scripts/upload-to-defectdojo.ps1"
    
    if (Test-Path $uploadScript) {
        & $uploadScript
    } else {
        Write-Error-Message "Upload script not found: $uploadScript"
        exit 1
    }
}

function Upload-ComponentBased {
    Write-Banner
    Write-Info "Uploading scan results with component-based organization..."
    Write-Info "(Creates separate products per component)"
    Write-Host ""
    
    $uploadScript = ".\defectdojo-component.ps1"
    
    if (Test-Path $uploadScript) {
        & $uploadScript
    } else {
        Write-Error-Message "Component upload script not found: $uploadScript"
        Write-Info "Expected location: $(Join-Path (Get-Location) 'defectdojo-component.ps1')"
        exit 1
    }
}

function Initialize-DefectDojo {
    Write-Banner
    Write-Info "Initializing DefectDojo (database, users, parsers, etc.)..."
    Write-Host ""
    
    $initScript = "compliance/scripts/init-defectdojo.ps1"
    
    if (Test-Path $initScript) {
        & $initScript
    } else {
        Write-Error-Message "Initialization script not found: $initScript"
        Write-Info "Expected location: $initScript"
        exit 1
    }
}

function Clean-DefectDojo {
    Write-Banner
    Write-Warning "This will remove all DefectDojo containers, volumes, and data!"
    Write-Host ""
    
    $confirm = Read-Host "Are you sure? Type 'yes' to confirm"
    
    if ($confirm -eq "yes") {
        Write-Info "Removing DefectDojo containers and volumes..."
        
        try {
            docker-compose -f $ComposeFile down -v
            Write-Success "DefectDojo cleaned up successfully"
            Write-Info "All data has been removed. Run './defectdojo.ps1 start' to recreate."
        } catch {
            Write-Error-Message "Failed to clean DefectDojo: $($_.Exception.Message)"
            exit 1
        }
    } else {
        Write-Info "Cleanup cancelled"
    }
}

function Clean-DuplicateTests {
    Write-Banner
    Write-Info "Running duplicate test cleanup..."
    Write-Host ""
    
    $cleanupScript = ".\cleanup-duplicate-tests.ps1"
    
    if (Test-Path $cleanupScript) {
        & $cleanupScript
    } else {
        Write-Error-Message "Cleanup script not found: $cleanupScript"
        exit 1
    }
}

# ===============================================================================
# Main Command Router
# ===============================================================================
switch ($Command) {
    'start' { Start-DefectDojo }
    'stop' { Stop-DefectDojo }
    'restart' { Restart-DefectDojo }
    'status' { Show-Status }
    'logs' { Show-Logs }
    'scan' { Run-SecurityScan }
    'upload' { Upload-Results }
    'upload-components' { Upload-ComponentBased }
    'init' { Initialize-DefectDojo }
    'clean' { Clean-DefectDojo }
    'clean-tests' { Clean-DuplicateTests }
    'help' { Show-Help }
    default { Show-Help }
}

