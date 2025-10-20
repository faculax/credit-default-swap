#!/usr/bin/env pwsh
# ===============================================================================
# DefectDojo Duplicate Cleanup Script
# ===============================================================================
# Removes duplicate engagements from the same day, keeping only the most recent
# ===============================================================================

param(
    [string]$DefectDojoUrl = "http://localhost:8081",
    [string]$Username = "admin",
    [string]$Password = "admin",
    [switch]$DryRun = $false,  # Show what would be deleted without actually deleting
    [string]$Date = ""  # Optional: specific date (yyyy-MM-dd), default is today
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "  DefectDojo Duplicate Engagement Cleanup" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

if ($DryRun) {
    Write-Host "[DRY RUN MODE] No changes will be made" -ForegroundColor Yellow
    Write-Host ""
}

# Determine target date
if ([string]::IsNullOrEmpty($Date)) {
    $targetDate = Get-Date -Format "yyyy-MM-dd"
    Write-Host "Target Date: $targetDate (today)" -ForegroundColor Cyan
} else {
    $targetDate = $Date
    Write-Host "Target Date: $targetDate" -ForegroundColor Cyan
}
Write-Host ""

# ===============================================================================
# Authenticate
# ===============================================================================

Write-Host "[>] Authenticating with DefectDojo..." -ForegroundColor Blue

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
# Find and Clean Duplicates
# ===============================================================================

Write-Host "[>] Finding products..." -ForegroundColor Blue

try {
    $productsResponse = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/products/" `
        -Method Get `
        -Headers $headers
    
    $totalProducts = $productsResponse.count
    Write-Host "[+] Found $totalProducts product(s)" -ForegroundColor Green
    Write-Host ""
    
} catch {
    Write-Host "[X] Failed to get products: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$totalDuplicates = 0
$totalDeleted = 0
$totalErrors = 0

foreach ($product in $productsResponse.results) {
    Write-Host "================================================" -ForegroundColor Blue
    Write-Host "Product: $($product.name) (ID: $($product.id))" -ForegroundColor Cyan
    Write-Host "================================================" -ForegroundColor Blue
    
    # Get engagements for the target date
    try {
        $engagementsResponse = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/engagements/?product=$($product.id)&target_start=$targetDate" `
            -Method Get `
            -Headers $headers
        
        $engagementCount = $engagementsResponse.count
        
        if ($engagementCount -eq 0) {
            Write-Host "  [>] No engagements found for $targetDate" -ForegroundColor Gray
            Write-Host ""
            continue
        }
        
        if ($engagementCount -eq 1) {
            Write-Host "  [OK] Only 1 engagement found - no duplicates" -ForegroundColor Green
            Write-Host "       Engagement: $($engagementsResponse.results[0].name)" -ForegroundColor Gray
            Write-Host ""
            continue
        }
        
        # Found duplicates
        Write-Host "  [!] Found $engagementCount engagements for $targetDate" -ForegroundColor Yellow
        $totalDuplicates += ($engagementCount - 1)
        
        # Sort by created date (descending) to keep the most recent
        $sortedEngagements = $engagementsResponse.results | Sort-Object -Property created -Descending
        
        $toKeep = $sortedEngagements[0]
        $toDelete = $sortedEngagements | Select-Object -Skip 1
        
        Write-Host ""
        Write-Host "  [KEEP] Engagement ID: $($toKeep.id)" -ForegroundColor Green
        Write-Host "         Name: $($toKeep.name)" -ForegroundColor Gray
        Write-Host "         Created: $($toKeep.created)" -ForegroundColor Gray
        Write-Host ""
        
        foreach ($engagement in $toDelete) {
            if ($DryRun) {
                Write-Host "  [DRY RUN] Would delete Engagement ID: $($engagement.id)" -ForegroundColor Yellow
            } else {
                Write-Host "  [DELETE] Engagement ID: $($engagement.id)" -ForegroundColor Red
            }
            Write-Host "           Name: $($engagement.name)" -ForegroundColor Gray
            Write-Host "           Created: $($engagement.created)" -ForegroundColor Gray
            
            if (-not $DryRun) {
                try {
                    Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/engagements/$($engagement.id)/" `
                        -Method Delete `
                        -Headers $headers | Out-Null
                    
                    Write-Host "           [+] Deleted successfully" -ForegroundColor Green
                    $totalDeleted++
                    
                } catch {
                    Write-Host "           [X] Failed to delete: $($_.Exception.Message)" -ForegroundColor Red
                    $totalErrors++
                }
            }
            
            Write-Host ""
        }
        
    } catch {
        Write-Host "  [X] Error processing product: $($_.Exception.Message)" -ForegroundColor Red
        $totalErrors++
    }
    
    Write-Host ""
}

# ===============================================================================
# Summary
# ===============================================================================

Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "  Cleanup Summary" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

if ($DryRun) {
    Write-Host "[DRY RUN] Summary of what would be deleted:" -ForegroundColor Yellow
    Write-Host "  Total duplicates found: $totalDuplicates" -ForegroundColor White
    Write-Host ""
    Write-Host "To actually delete, run without -DryRun:" -ForegroundColor Yellow
    Write-Host "  .\cleanup-duplicates.ps1" -ForegroundColor White
} else {
    Write-Host "Products processed: $totalProducts" -ForegroundColor Cyan
    Write-Host "Duplicates found: $totalDuplicates" -ForegroundColor Yellow
    Write-Host "Engagements deleted: $totalDeleted" -ForegroundColor Green
    
    if ($totalErrors -gt 0) {
        Write-Host "Errors encountered: $totalErrors" -ForegroundColor Red
    }
    
    Write-Host ""
    
    if ($totalDeleted -gt 0) {
        Write-Host "[+] Cleanup completed successfully!" -ForegroundColor Green
    } else {
        Write-Host "[+] No duplicates to clean up" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "View products: $DefectDojoUrl/product" -ForegroundColor Cyan
Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""
