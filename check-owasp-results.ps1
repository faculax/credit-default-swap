# 🔍 OWASP Scan Monitoring Script
# Run this to check scan progress and results

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  OWASP Dependency Check - Status Monitor" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Check if scan report exists
$reportPath = "backend\target\dependency-check-report.json"

if (Test-Path $reportPath) {
    Write-Host "✓ Scan completed!" -ForegroundColor Green
    Write-Host ""
    
    try {
        $report = Get-Content $reportPath -Raw | ConvertFrom-Json
        
        # Basic stats
        Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Blue
        Write-Host "  SCAN SUMMARY" -ForegroundColor Blue
        Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Blue
        Write-Host ""
        Write-Host "  Total dependencies scanned: $($report.dependencies.Count)" -ForegroundColor Gray
        
        # Count vulnerabilities
        $vulnDeps = $report.dependencies | Where-Object { $_.vulnerabilities -and $_.vulnerabilities.Count -gt 0 }
        $totalVulns = ($vulnDeps | ForEach-Object { $_.vulnerabilities.Count } | Measure-Object -Sum).Sum
        
        Write-Host "  Dependencies with vulnerabilities: $($vulnDeps.Count)" -ForegroundColor $(if ($vulnDeps.Count -gt 0) { "Red" } else { "Green" })
        Write-Host "  Total CVEs found: $totalVulns" -ForegroundColor $(if ($totalVulns -gt 0) { "Red" } else { "Green" })
        Write-Host ""
        
        if ($vulnDeps.Count -gt 0) {
            Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Red
            Write-Host "  VULNERABILITIES DETECTED" -ForegroundColor Red
            Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Red
            Write-Host ""
            
            # Count by severity
            $allVulns = $vulnDeps | ForEach-Object { $_.vulnerabilities } | Where-Object { $_ }
            $critical = ($allVulns | Where-Object { $_.severity -eq 'CRITICAL' }).Count
            $high = ($allVulns | Where-Object { $_.severity -eq 'HIGH' }).Count
            $medium = ($allVulns | Where-Object { $_.severity -eq 'MEDIUM' }).Count
            $low = ($allVulns | Where-Object { $_.severity -eq 'LOW' }).Count
            
            Write-Host "  Severity Breakdown:" -ForegroundColor Yellow
            if ($critical -gt 0) { Write-Host "    🔴 CRITICAL: $critical" -ForegroundColor Red }
            if ($high -gt 0) { Write-Host "    🟠 HIGH:     $high" -ForegroundColor Red }
            if ($medium -gt 0) { Write-Host "    🟡 MEDIUM:   $medium" -ForegroundColor Yellow }
            if ($low -gt 0) { Write-Host "    🟢 LOW:      $low" -ForegroundColor Green }
            Write-Host ""
            
            # Show vulnerable dependencies
            Write-Host "  Vulnerable Dependencies:" -ForegroundColor Yellow
            Write-Host ""
            
            foreach ($dep in $vulnDeps | Sort-Object { -$_.vulnerabilities.Count } | Select-Object -First 10) {
                Write-Host "  📦 $($dep.fileName)" -ForegroundColor Cyan
                Write-Host "     CVEs: $($dep.vulnerabilities.Count)" -ForegroundColor Gray
                
                # Show top 3 CVEs for this dependency
                $topCves = $dep.vulnerabilities | Sort-Object { 
                    switch ($_.severity) {
                        'CRITICAL' { 4 }
                        'HIGH' { 3 }
                        'MEDIUM' { 2 }
                        'LOW' { 1 }
                        default { 0 }
                    }
                } -Descending | Select-Object -First 3
                
                foreach ($vuln in $topCves) {
                    $severityColor = switch ($vuln.severity) {
                        'CRITICAL' { 'Red' }
                        'HIGH' { 'Red' }
                        'MEDIUM' { 'Yellow' }
                        'LOW' { 'Green' }
                        default { 'Gray' }
                    }
                    
                    $cvssScore = if ($vuln.cvssv3 -and $vuln.cvssv3.baseScore) { 
                        "CVSS: $($vuln.cvssv3.baseScore)" 
                    } elseif ($vuln.cvssv2 -and $vuln.cvssv2.score) { 
                        "CVSS: $($vuln.cvssv2.score)" 
                    } else { 
                        "" 
                    }
                    
                    Write-Host "     • $($vuln.name) - $($vuln.severity) $cvssScore" -ForegroundColor $severityColor
                }
                Write-Host ""
            }
            
            # Check for Log4Shell specifically
            $log4jVuln = $vulnDeps | Where-Object { $_.fileName -like "*log4j*" }
            if ($log4jVuln) {
                Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Magenta
                Write-Host "  ⚠️  LOG4J DETECTED - CHECK FOR LOG4SHELL (CVE-2021-44228)" -ForegroundColor Magenta
                Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Magenta
                Write-Host ""
                foreach ($dep in $log4jVuln) {
                    Write-Host "  $($dep.fileName)" -ForegroundColor Yellow
                    $log4shell = $dep.vulnerabilities | Where-Object { $_.name -eq 'CVE-2021-44228' }
                    if ($log4shell) {
                        Write-Host "    🚨 CVE-2021-44228 (Log4Shell) FOUND!" -ForegroundColor Red
                        Write-Host "    This was added for TESTING - remove log4j-core 2.14.1!" -ForegroundColor Yellow
                    }
                }
                Write-Host ""
            }
            
            # Check for flexjson
            $flexjson = $report.dependencies | Where-Object { $_.fileName -like "*flexjson*" }
            if ($flexjson) {
                Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Blue
                Write-Host "  FLEXJSON STATUS" -ForegroundColor Blue
                Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Blue
                Write-Host ""
                Write-Host "  Flexjson detected: $($flexjson.fileName)" -ForegroundColor Cyan
                if ($flexjson.vulnerabilities -and $flexjson.vulnerabilities.Count -gt 0) {
                    Write-Host "  Vulnerabilities: $($flexjson.vulnerabilities.Count)" -ForegroundColor Red
                    $flexjson.vulnerabilities | ForEach-Object {
                        Write-Host "    • $($_.name) - $($_.severity)" -ForegroundColor Red
                    }
                } else {
                    Write-Host "  ❌ No CVEs found in NVD database" -ForegroundColor Yellow
                    Write-Host "  Note: Flexjson 3.3 may not have registered CVEs" -ForegroundColor Gray
                    Write-Host "  See: OWASP_FLEXJSON_EXPLAINED.md for details" -ForegroundColor Gray
                }
                Write-Host ""
            }
            
        } else {
            Write-Host "✓ No vulnerabilities detected!" -ForegroundColor Green
            Write-Host ""
        }
        
        Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  NEXT STEPS" -ForegroundColor Cyan
        Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "  View HTML Report:" -ForegroundColor Yellow
        Write-Host "    Start-Process backend\target\dependency-check-report.html" -ForegroundColor Gray
        Write-Host ""
        Write-Host "  Upload to DefectDojo:" -ForegroundColor Yellow
        Write-Host "    .\defectdojo.ps1 upload-components" -ForegroundColor Gray
        Write-Host ""
        Write-Host "  Remove test dependency (log4j 2.14.1):" -ForegroundColor Yellow
        Write-Host "    Edit backend\pom.xml and remove log4j-core dependency" -ForegroundColor Gray
        Write-Host ""
        
    } catch {
        Write-Host "❌ Error reading report: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Report may be corrupted. Try running scan again." -ForegroundColor Yellow
    }
    
} else {
    Write-Host "⏳ Scan not complete yet or hasn't run" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Check if scan is running:" -ForegroundColor Cyan
    Write-Host "  Look for Maven process in terminal" -ForegroundColor Gray
    Write-Host ""
    Write-Host "To start scan:" -ForegroundColor Cyan
    Write-Host "  cd backend" -ForegroundColor Gray
    Write-Host "  mvn clean compile org.owasp:dependency-check-maven:check" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Or use convenience script:" -ForegroundColor Cyan
    Write-Host "  .\defectdojo.ps1 scan" -ForegroundColor Gray
    Write-Host ""
}

Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
