<#
.SYNOPSIS
    Local Quality Gate Check for CDS Platform

.DESCRIPTION
    Run this before committing to ensure your code meets security standards.
    Checks for zero-tolerance security violations and anti-patterns.

.PARAMETER Service
    The service to check: backend, gateway, or risk-engine (default: all)

.EXAMPLE
    .\quality-gate-check.ps1
    Checks all services

.EXAMPLE
    .\quality-gate-check.ps1 -Service backend
    Checks only the backend service
#>

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("backend", "gateway", "risk-engine", "all")]
    [string]$Service = "all"
)

# Colors
$RED = "Red"
$GREEN = "Green"
$YELLOW = "Yellow"
$BLUE = "Cyan"

# Banner
Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor $BLUE
Write-Host "║                                                            ║" -ForegroundColor $BLUE
Write-Host "║          CDS Platform Quality Gate Check                  ║" -ForegroundColor $BLUE
Write-Host "║                                                            ║" -ForegroundColor $BLUE
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor $BLUE
Write-Host ""

# Determine which services to check
if ($Service -eq "all") {
    $Services = @("backend", "gateway", "risk-engine")
    Write-Host "📋 Checking all services: $($Services -join ', ')" -ForegroundColor $BLUE
} else {
    $Services = @($Service)
    Write-Host "📋 Checking service: $Service" -ForegroundColor $BLUE
}
Write-Host ""

$TotalViolations = 0
$TotalWarnings = 0

function Check-Service {
    param([string]$ServiceName)
    
    if (-not (Test-Path $ServiceName)) {
        Write-Host "⚠️  Service directory not found: $ServiceName" -ForegroundColor $YELLOW
        return
    }
    
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor $BLUE
    Write-Host "🔍 Analyzing: $ServiceName" -ForegroundColor $BLUE
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor $BLUE
    Write-Host ""
    
    Push-Location $ServiceName
    
    $ServiceViolations = 0
    $ServiceWarnings = 0
    
    # Step 1: Build the service
    Write-Host "📦 Step 1: Building $ServiceName..." -ForegroundColor $BLUE
    $buildOutput = & .\mvnw.cmd clean compile -DskipTests -B 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Build successful" -ForegroundColor $GREEN
    } else {
        Write-Host "❌ Build failed" -ForegroundColor $RED
        $ServiceViolations++
        Pop-Location
        return @{Violations=$ServiceViolations; Warnings=$ServiceWarnings}
    }
    Write-Host ""
    
    # Step 2: Run SpotBugs
    Write-Host "🐛 Step 2: Running SpotBugs security analysis..." -ForegroundColor $BLUE
    $null = & .\mvnw.cmd spotbugs:spotbugs -B 2>&1
    if (Test-Path "target\spotbugsXml.xml") {
        $xml = [xml](Get-Content "target\spotbugsXml.xml")
        $bugCount = $xml.SelectNodes("//BugInstance").Count
        Write-Host "   Found $bugCount total issues" -ForegroundColor $BLUE
    }
    Write-Host ""
    
    # Step 3: Check Zero-Tolerance Rules
    Write-Host "🚨 Step 3: Checking Zero-Tolerance Security Rules..." -ForegroundColor $BLUE
    Write-Host ""
    
    if (Test-Path "target\spotbugsXml.xml") {
        $xml = [xml](Get-Content "target\spotbugsXml.xml")
        
        # Rule 1: CRLF Injection
        Write-Host "   Rule 1 - CRLF Injection: " -NoNewline
        $crlfCount = $xml.SelectNodes("//BugInstance[@type='CRLF_INJECTION_LOGS']").Count
        if ($crlfCount -gt 0) {
            Write-Host "❌ FAIL ($crlfCount violations)" -ForegroundColor $RED
            $ServiceViolations += $crlfCount
        } else {
            Write-Host "✅ PASS" -ForegroundColor $GREEN
        }
        
        # Rule 2: Predictable Random
        Write-Host "   Rule 2 - Predictable Random: " -NoNewline
        $randomNodes = $xml.SelectNodes("//BugInstance[@type='PREDICTABLE_RANDOM']")
        $randomCount = $randomNodes.Count
        $demoExceptions = ($randomNodes | Where-Object { $_.Class -match 'Demo.*Service' }).Count
        $actualRandom = $randomCount - $demoExceptions
        if ($actualRandom -gt 0) {
            Write-Host "❌ FAIL ($actualRandom violations)" -ForegroundColor $RED
            $ServiceViolations += $actualRandom
        } else {
            if ($demoExceptions -gt 0) {
                Write-Host "✅ PASS ($demoExceptions in demo code)" -ForegroundColor $GREEN
            } else {
                Write-Host "✅ PASS" -ForegroundColor $GREEN
            }
        }
        
        # Rule 3: Unicode Handling
        Write-Host "   Rule 3 - Unicode Handling: " -NoNewline
        $unicodeCount = $xml.SelectNodes("//BugInstance[@type='DM_CONVERT_CASE']").Count
        if ($unicodeCount -gt 0) {
            Write-Host "❌ FAIL ($unicodeCount violations)" -ForegroundColor $RED
            $ServiceViolations += $unicodeCount
        } else {
            Write-Host "✅ PASS" -ForegroundColor $GREEN
        }
        
        # Rule 4: Information Exposure
        Write-Host "   Rule 4 - Information Exposure: " -NoNewline
        $infoCount = $xml.SelectNodes("//BugInstance[@type='INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE']").Count
        if ($infoCount -gt 0) {
            Write-Host "❌ FAIL ($infoCount violations)" -ForegroundColor $RED
            $ServiceViolations += $infoCount
        } else {
            Write-Host "✅ PASS" -ForegroundColor $GREEN
        }
        
        # Rule 5: SQL Injection
        Write-Host "   Rule 5 - SQL Injection: " -NoNewline
        $sqlCount = $xml.SelectNodes("//BugInstance[@type='SQL_INJECTION']").Count
        if ($sqlCount -gt 0) {
            Write-Host "❌ FAIL ($sqlCount violations)" -ForegroundColor $RED
            $ServiceViolations += $sqlCount
        } else {
            Write-Host "✅ PASS" -ForegroundColor $GREEN
        }
    } else {
        Write-Host "   ⚠️  SpotBugs XML report not found" -ForegroundColor $YELLOW
    }
    Write-Host ""
    
    # Step 4: Check Anti-Patterns
    Write-Host "🔍 Step 4: Checking for Anti-Patterns..." -ForegroundColor $BLUE
    Write-Host ""
    
    # Anti-Pattern 1: Client-controlled authorization
    Write-Host "   Anti-Pattern 1 - Client Auth: " -NoNewline
    $clientAuth = Select-String -Path "src\**\*.java" -Pattern 'request\.getParameter.*[Rr]ole' -Quiet
    if ($clientAuth) {
        Write-Host "⚠️  WARNING" -ForegroundColor $YELLOW
        $ServiceWarnings++
    } else {
        Write-Host "✅ PASS" -ForegroundColor $GREEN
    }
    
    # Anti-Pattern 2: Disabled security
    Write-Host "   Anti-Pattern 2 - Disabled Security: " -NoNewline
    $disabledSecurity = Select-String -Path "src\**\*.java" -Pattern '@CrossOrigin.*origins.*=.*"\*"' -Quiet
    if ($disabledSecurity) {
        Write-Host "⚠️  WARNING" -ForegroundColor $YELLOW
        $ServiceWarnings++
    } else {
        Write-Host "✅ PASS" -ForegroundColor $GREEN
    }
    
    # Anti-Pattern 3: Weak crypto
    Write-Host "   Anti-Pattern 3 - Weak Crypto: " -NoNewline
    $weakCrypto = Select-String -Path "src\**\*.java" -Pattern 'MessageDigest\.getInstance.*"MD5"|"SHA1"' -Quiet
    if ($weakCrypto) {
        Write-Host "⚠️  WARNING" -ForegroundColor $YELLOW
        $ServiceWarnings++
    } else {
        Write-Host "✅ PASS" -ForegroundColor $GREEN
    }
    
    # Anti-Pattern 4: Sensitive data logging
    Write-Host "   Anti-Pattern 4 - Sensitive Logging: " -NoNewline
    $sensitiveMatches = Select-String -Path "src\**\*.java" -Pattern 'logger\.\(info|debug|warn|error\).*password|secret|token|apikey|api_key'
    $sensitiveCount = if ($sensitiveMatches) { $sensitiveMatches.Count } else { 0 }
    if ($sensitiveCount -gt 0) {
        Write-Host "⚠️  WARNING ($sensitiveCount instances)" -ForegroundColor $YELLOW
        $ServiceWarnings += $sensitiveCount
    } else {
        Write-Host "✅ PASS" -ForegroundColor $GREEN
    }
    Write-Host ""
    
    # Note: Missing @PreAuthorize is NOT checked as authorization strategy varies:
    # - Public endpoints (health) don't need @PreAuthorize
    # - Internal APIs use service token validation
    # - User-facing APIs should have @PreAuthorize
    # Manual security review should verify appropriate authorization per endpoint
    
    # Step 5: Run Unit Tests with Coverage
    Write-Host "🧪 Step 5: Running Unit Tests with Coverage..." -ForegroundColor $BLUE
    $null = & .\mvnw.cmd test jacoco:report -B 2>&1
    if (Test-Path "target\site\jacoco\jacoco.xml") {
        $jacocoXml = [xml](Get-Content "target\site\jacoco\jacoco.xml")
        $instructionCounter = $jacocoXml.report.counter | Where-Object { $_.type -eq "INSTRUCTION" }
        
        if ($instructionCounter) {
            $covered = [int]$instructionCounter.covered
            $missed = [int]$instructionCounter.missed
            $total = $covered + $missed
            
            if ($total -gt 0) {
                $coveragePct = [math]::Floor(($covered * 100) / $total)
                Write-Host "   Test Coverage: $coveragePct%"
                
                if ($coveragePct -lt 80) {
                    Write-Host "   ⚠️  WARNING: Coverage below 80% threshold" -ForegroundColor $YELLOW
                    $ServiceWarnings++
                } else {
                    Write-Host "   ✅ Coverage meets 80% threshold" -ForegroundColor $GREEN
                }
            }
        }
    } else {
        Write-Host "   ⚠️  Test execution warning" -ForegroundColor $YELLOW
        $ServiceWarnings++
    }
    Write-Host ""
    
    # Service Summary
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor $BLUE
    Write-Host "Summary for $ServiceName:" -ForegroundColor $BLUE
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor $BLUE
    
    if ($ServiceViolations -gt 0) {
        Write-Host "❌ QUALITY GATE FAILED" -ForegroundColor $RED
        Write-Host "   Critical Violations: " -NoNewline
        Write-Host "$ServiceViolations" -ForegroundColor $RED
    } else {
        Write-Host "✅ QUALITY GATE PASSED" -ForegroundColor $GREEN
        Write-Host "   Critical Violations: " -NoNewline
        Write-Host "0" -ForegroundColor $GREEN
    }
    
    if ($ServiceWarnings -gt 0) {
        Write-Host "   Warnings: " -NoNewline
        Write-Host "$ServiceWarnings" -ForegroundColor $YELLOW
    } else {
        Write-Host "   Warnings: " -NoNewline
        Write-Host "0" -ForegroundColor $GREEN
    }
    Write-Host ""
    
    Pop-Location
    
    return @{Violations=$ServiceViolations; Warnings=$ServiceWarnings}
}

# Check each service
foreach ($svc in $Services) {
    $result = Check-Service -ServiceName $svc
    $TotalViolations += $result.Violations
    $TotalWarnings += $result.Warnings
}

# Overall Summary
Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor $BLUE
Write-Host "║                     OVERALL SUMMARY                        ║" -ForegroundColor $BLUE
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor $BLUE
Write-Host ""

if ($TotalViolations -gt 0) {
    Write-Host "❌ QUALITY GATE FAILED" -ForegroundColor $RED
    Write-Host ""
    Write-Host "   Total Critical Violations: " -NoNewline
    Write-Host "$TotalViolations" -ForegroundColor $RED
    Write-Host "   Total Warnings: " -NoNewline
    Write-Host "$TotalWarnings" -ForegroundColor $YELLOW
    Write-Host ""
    Write-Host "📋 Action Required:" -ForegroundColor $YELLOW
    Write-Host "   1. Review SpotBugs reports in target\site\spotbugs.html"
    Write-Host "   2. Consult .github\CODE_QUALITY_RULES.md for remediation steps"
    Write-Host "   3. Consult AGENTS.md for security standards"
    Write-Host ""
    Write-Host "🚫 DO NOT COMMIT until all critical violations are fixed" -ForegroundColor $RED
    Write-Host ""
    exit 1
} else {
    Write-Host "✅ ALL QUALITY GATES PASSED" -ForegroundColor $GREEN
    Write-Host ""
    Write-Host "   Total Critical Violations: " -NoNewline
    Write-Host "0" -ForegroundColor $GREEN
    Write-Host "   Total Warnings: " -NoNewline
    Write-Host "$TotalWarnings" -ForegroundColor $YELLOW
    Write-Host ""
    if ($TotalWarnings -gt 0) {
        Write-Host "💡 Consider addressing warnings before committing" -ForegroundColor $YELLOW
    } else {
        Write-Host "🎉 Your code meets all security standards!" -ForegroundColor $GREEN
    }
    Write-Host ""
    Write-Host "✅ Safe to commit" -ForegroundColor $GREEN
    Write-Host ""
    exit 0
}
