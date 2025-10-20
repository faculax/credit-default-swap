#!/usr/bin/env pwsh
# ===============================================================================
# Cleanup Duplicate Tests in DefectDojo
# ===============================================================================
# Removes duplicate tests within engagements, keeping only the most recent
# ===============================================================================

param(
    [string]$DefectDojoUrl = "http://localhost:8081",
    [string]$Username = "admin",
    [string]$Password = "admin"
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "  DefectDojo Duplicate Test Cleanup" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# ===============================================================================
# Authenticate
# ===============================================================================

Write-Host "[>] Authenticating..." -ForegroundColor Blue

try {
    $authBody = @{
        username = $Username
        password = $Password
    } | ConvertTo-Json

    $authResponse = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/api-token-auth/" `
        -Method Post `
        -ContentType "application/json" `
        -Body $authBody

    $token = $authResponse.token

    if ([string]::IsNullOrEmpty($token)) {
        Write-Host "[X] Failed to authenticate" -ForegroundColor Red
        exit 1
    }

    Write-Host "[+] Authenticated successfully" -ForegroundColor Green
    Write-Host ""
    
} catch {
    Write-Host "[X] Authentication failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Token $token"
}

# ===============================================================================
# Find and Clean Duplicate Tests
# ===============================================================================

Write-Host "[>] Fetching all tests..." -ForegroundColor Blue

try {
    $allTests = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/tests/" `
        -Method Get `
        -Headers $headers
    
    Write-Host "[+] Found $($allTests.count) total tests" -ForegroundColor Green
    Write-Host ""
    
    # Group tests by engagement and test type
    $testGroups = $allTests.results | Group-Object -Property engagement, test_type_name
    
    $duplicatesFound = 0
    $duplicatesDeleted = 0
    
    foreach ($group in $testGroups) {
        if ($group.Count -gt 1) {
            $duplicatesFound += ($group.Count - 1)
            
            # Sort by created date (newest first)
            $sortedTests = $group.Group | Sort-Object -Property created -Descending
            
            $engagementId = $sortedTests[0].engagement
            $testTypeName = $sortedTests[0].test_type_name
            $keepTest = $sortedTests[0]
            
            Write-Host "[!] Found $($group.Count) duplicate tests in engagement $engagementId for '$testTypeName'" -ForegroundColor Yellow
            Write-Host "    Keeping test ID $($keepTest.id) (created: $($keepTest.created))" -ForegroundColor Gray
            
            # Delete older duplicates
            for ($i = 1; $i -lt $sortedTests.Count; $i++) {
                $oldTest = $sortedTests[$i]
                Write-Host "    Deleting test ID $($oldTest.id) (created: $($oldTest.created))..." -ForegroundColor DarkGray
                
                try {
                    Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/tests/$($oldTest.id)/" `
                        -Method Delete `
                        -Headers $headers | Out-Null
                    
                    $duplicatesDeleted++
                    Write-Host "      [+] Deleted" -ForegroundColor Green
                    
                } catch {
                    Write-Host "      [X] Failed to delete: $($_.Exception.Message)" -ForegroundColor Red
                }
            }
            
            Write-Host ""
        }
    }
    
    # Summary
    Write-Host "===============================================================" -ForegroundColor Green
    Write-Host "[+] CLEANUP COMPLETE" -ForegroundColor Green
    Write-Host "===============================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "  Total tests: $($allTests.count)" -ForegroundColor Cyan
    Write-Host "  Duplicates found: $duplicatesFound" -ForegroundColor Yellow
    Write-Host "  Duplicates deleted: $duplicatesDeleted" -ForegroundColor Green
    Write-Host ""
    
    if ($duplicatesDeleted -eq 0) {
        Write-Host "[+] No duplicates found - your DefectDojo is clean!" -ForegroundColor Green
    } else {
        Write-Host "[+] Cleaned up $duplicatesDeleted duplicate test(s)" -ForegroundColor Green
    }
    Write-Host ""
    
} catch {
    Write-Host "[X] Error during cleanup: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
