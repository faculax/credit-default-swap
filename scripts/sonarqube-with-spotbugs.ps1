#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Run SpotBugs + SonarQube analysis with full security scanning
    
.DESCRIPTION
    This script ensures SpotBugs runs BEFORE SonarQube, so SonarQube
    can import SpotBugs findings into its analysis dashboard.
    
.EXAMPLE
    .\sonarqube-with-spotbugs.ps1 -Service backend
    .\sonarqube-with-spotbugs.ps1 -Service risk-engine
#>

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('backend', 'gateway', 'risk-engine', 'all')]
    [string]$Service,
    
    [Parameter(Mandatory=$false)]
    [string]$SonarUrl = "http://localhost:9000",
    
    [Parameter(Mandatory=$false)]
    [string]$SonarToken = $env:SONAR_TOKEN
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "🔐 SpotBugs + SonarQube Integrated Security Analysis" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

function Run-SecurityAnalysis {
    param([string]$ServicePath)
    
    $ServiceName = Split-Path -Leaf $ServicePath
    
    Write-Host "📦 Analyzing: $ServiceName" -ForegroundColor Yellow
    Write-Host ""
    
    Push-Location $ServicePath
    
    try {
        # Step 1: Clean previous reports
        Write-Host "🧹 Cleaning previous reports..." -ForegroundColor Cyan
        if (Test-Path "target/spotbugsXml.xml") {
            Remove-Item "target/spotbugsXml.xml" -Force
            Write-Host "✅ Removed old SpotBugs report" -ForegroundColor Green
        }
        
        # Step 2: Compile the code
        Write-Host ""
        Write-Host "🔨 Compiling $ServiceName..." -ForegroundColor Cyan
        & ./mvnw clean compile -DskipTests -q
        if ($LASTEXITCODE -ne 0) {
            Write-Host "❌ Compilation failed!" -ForegroundColor Red
            return $false
        }
        Write-Host "✅ Compilation successful" -ForegroundColor Green
        
        # Step 3: Run SpotBugs with FindSecBugs plugin
        Write-Host ""
        Write-Host "🐛 Running SpotBugs security analysis..." -ForegroundColor Cyan
        Write-Host "   (Using FindSecBugs plugin for CRLF, SQL injection, etc.)" -ForegroundColor Gray
        & ./mvnw spotbugs:spotbugs -q
        if ($LASTEXITCODE -ne 0) {
            Write-Host "⚠️  SpotBugs completed with warnings" -ForegroundColor Yellow
        } else {
            Write-Host "✅ SpotBugs analysis complete" -ForegroundColor Green
        }
        
        # Step 4: Verify SpotBugs report exists
        if (Test-Path "target/spotbugsXml.xml") {
            $xmlContent = Get-Content "target/spotbugsXml.xml" -Raw
            $bugCount = ([regex]::Matches($xmlContent, '<BugInstance')).Count
            
            Write-Host ""
            Write-Host "📊 SpotBugs Report Generated:" -ForegroundColor Cyan
            Write-Host "   Location: target/spotbugsXml.xml" -ForegroundColor Gray
            Write-Host "   Total Issues: $bugCount" -ForegroundColor $(if ($bugCount -gt 0) { "Yellow" } else { "Green" })
            
            # Parse by severity
            $criticalCount = ([regex]::Matches($xmlContent, "rank='[1-9]'")).Count
            $mediumCount = ([regex]::Matches($xmlContent, "rank='1[0-4]'")).Count
            $lowCount = ([regex]::Matches($xmlContent, "rank='(15|16|17|18|19|20)'")).Count
            
            Write-Host "   🔴 Critical (Rank 1-9): $criticalCount" -ForegroundColor $(if ($criticalCount -gt 0) { "Red" } else { "Gray" })
            Write-Host "   🟡 Medium (Rank 10-14): $mediumCount" -ForegroundColor $(if ($mediumCount -gt 0) { "Yellow" } else { "Gray" })
            Write-Host "   🟢 Low (Rank 15-20): $lowCount" -ForegroundColor $(if ($lowCount -gt 0) { "White" } else { "Gray" })
            
            # Check for ZERO-TOLERANCE violations
            Write-Host ""
            Write-Host "🚨 Checking ZERO-TOLERANCE violations..." -ForegroundColor Cyan
            
            $crlfCount = ([regex]::Matches($xmlContent, "CRLF_INJECTION_LOGS")).Count
            $randomCount = ([regex]::Matches($xmlContent, "PREDICTABLE_RANDOM")).Count
            $caseCount = ([regex]::Matches($xmlContent, "DM_CONVERT_CASE")).Count
            $sqlCount = ([regex]::Matches($xmlContent, "SQL_INJECTION")).Count
            
            Write-Host "   CRLF Injection (CWE-117): $crlfCount" -ForegroundColor $(if ($crlfCount -gt 0) { "Red" } else { "Green" })
            Write-Host "   Predictable Random (CWE-330): $randomCount" -ForegroundColor $(if ($randomCount -gt 0) { "Red" } else { "Green" })
            Write-Host "   Improper Case Conversion (CWE-176): $caseCount" -ForegroundColor $(if ($caseCount -gt 0) { "Red" } else { "Green" })
            Write-Host "   SQL Injection: $sqlCount" -ForegroundColor $(if ($sqlCount -gt 0) { "Red" } else { "Green" })
            
            $totalCritical = $crlfCount + $randomCount + $caseCount + $sqlCount
            
            if ($totalCritical -gt 0) {
                Write-Host ""
                Write-Host "❌ CRITICAL: $totalCritical zero-tolerance violations found!" -ForegroundColor Red
                Write-Host "   Review: AGENTS.md for remediation steps" -ForegroundColor Yellow
            } else {
                Write-Host ""
                Write-Host "✅ No zero-tolerance violations detected" -ForegroundColor Green
            }
        } else {
            Write-Host "⚠️  WARNING: SpotBugs report not generated!" -ForegroundColor Yellow
            Write-Host "   SonarQube will not import SpotBugs findings" -ForegroundColor Gray
            return $false
        }
        
        # Step 5: Run JaCoCo test coverage
        Write-Host ""
        Write-Host "📊 Running test coverage analysis..." -ForegroundColor Cyan
        & ./mvnw test jacoco:report -q
        if ($LASTEXITCODE -eq 0) {
            if (Test-Path "target/site/jacoco/jacoco.xml") {
                Write-Host "✅ Coverage report generated" -ForegroundColor Green
            }
        } else {
            Write-Host "⚠️  Some tests failed, continuing with coverage data" -ForegroundColor Yellow
        }
        
        # Step 6: Run SonarQube scan (imports SpotBugs findings)
        Write-Host ""
        Write-Host "📡 Running SonarQube analysis (importing SpotBugs findings)..." -ForegroundColor Cyan
        
        if (-not $SonarToken) {
            Write-Host "⚠️  SONAR_TOKEN not set - attempting anonymous scan" -ForegroundColor Yellow
        }
        
        $sonarArgs = @(
            "sonar:sonar",
            "-Dsonar.host.url=$SonarUrl"
        )
        
        if ($SonarToken) {
            $sonarArgs += "-Dsonar.token=$SonarToken"
        }
        
        & ./mvnw $sonarArgs -q
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ SonarQube analysis complete" -ForegroundColor Green
            Write-Host ""
            Write-Host "🌐 View results at: $SonarUrl/dashboard?id=credit-default-swap-$ServiceName" -ForegroundColor Cyan
            return $true
        } else {
            Write-Host "❌ SonarQube analysis failed" -ForegroundColor Red
            return $false
        }
        
    } catch {
        Write-Host "❌ Error analyzing $ServiceName : $_" -ForegroundColor Red
        return $false
    } finally {
        Pop-Location
    }
}

# Main execution
$services = if ($Service -eq "all") {
    @("backend", "gateway", "risk-engine")
} else {
    @($Service)
}

$results = @{}

foreach ($svc in $services) {
    $servicePath = Join-Path $PSScriptRoot ".." $svc
    
    if (Test-Path $servicePath) {
        Write-Host ""
        Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
        $results[$svc] = Run-SecurityAnalysis -ServicePath $servicePath
        Write-Host "═══════════════════════════════════════════════════" -ForegroundColor Cyan
    } else {
        Write-Host "⚠️  Service directory not found: $servicePath" -ForegroundColor Yellow
        $results[$svc] = $false
    }
}

# Final summary
Write-Host ""
Write-Host ""
Write-Host "📋 Analysis Summary" -ForegroundColor Cyan
Write-Host "===================" -ForegroundColor Cyan

foreach ($svc in $results.Keys) {
    $status = if ($results[$svc]) { "✅ PASS" } else { "❌ FAIL" }
    $color = if ($results[$svc]) { "Green" } else { "Red" }
    Write-Host "$status - $svc" -ForegroundColor $color
}

Write-Host ""
Write-Host "💡 Next Steps:" -ForegroundColor Cyan
Write-Host "   1. Open SonarQube UI: $SonarUrl" -ForegroundColor Gray
Write-Host "   2. Check 'Security Hotspots' and 'Issues' tabs" -ForegroundColor Gray
Write-Host "   3. SpotBugs findings are now integrated into SonarQube" -ForegroundColor Gray
Write-Host ""

$failCount = ($results.Values | Where-Object { $_ -eq $false }).Count
exit $failCount
