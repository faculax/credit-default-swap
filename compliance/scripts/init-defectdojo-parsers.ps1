#!/usr/bin/env pwsh
# DefectDojo Parser Initialization Script
# Ensures all required test types exist in DefectDojo

param(
    [string]$DefectDojoUrl = "http://localhost:8081",
    [string]$Username = "admin",
    [string]$Password = "admin"
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host " DEFECTDOJO PARSER INITIALIZATION" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# Authenticate
Write-Host "Authenticating with DefectDojo..." -ForegroundColor Blue

try {
    $authBody = @{ username = $Username; password = $Password } | ConvertTo-Json
    $authResponse = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/api-token-auth/" `
        -Method Post -ContentType "application/json" -Body $authBody
    $token = $authResponse.token
    Write-Host "Authentication successful" -ForegroundColor Green
} catch {
    Write-Host "Authentication failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$headers = @{ "Authorization" = "Token $token" }

# Get existing test types
Write-Host "Checking existing test types..." -ForegroundColor Blue

try {
    $testTypesResponse = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/test_types/?limit=200" `
        -Method Get -Headers $headers
    $existingTypes = $testTypesResponse.results | ForEach-Object { $_.name }
    Write-Host "  Found $($existingTypes.Count) existing test types" -ForegroundColor Gray
} catch {
    Write-Host "Failed to get test types: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Define required test types
$requiredTestTypes = @(
    @{ name = "Dependency Check Scan"; static_tool = $true; dynamic_tool = $false; active = $true },
    @{ name = "SpotBugs Scan"; static_tool = $true; dynamic_tool = $false; active = $true },
    @{ name = "PMD Scan"; static_tool = $true; dynamic_tool = $false; active = $true },
    @{ name = "Generic Findings Import"; static_tool = $true; dynamic_tool = $false; active = $true }
)

# Create missing test types
Write-Host "Verifying required test types..." -ForegroundColor Blue

$created = 0
$existing = 0

foreach ($testType in $requiredTestTypes) {
    if ($existingTypes -contains $testType.name) {
        Write-Host "  OK: $($testType.name) - already exists" -ForegroundColor Green
        $existing++
    } else {
        try {
            $body = $testType | ConvertTo-Json
            $createResponse = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/test_types/" `
                -Method Post -Headers $headers -ContentType "application/json" -Body $body
            Write-Host "  NEW: $($testType.name) - created (ID: $($createResponse.id))" -ForegroundColor Cyan
            $created++
        } catch {
            Write-Host "  ERROR: $($testType.name) - failed to create: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

# Summary
Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host " INITIALIZATION COMPLETE" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor Yellow
Write-Host "  Existing test types: $existing" -ForegroundColor White
Write-Host "  Created test types:  $created" -ForegroundColor White
Write-Host "  Total test types:    $($existing + $created)" -ForegroundColor White
Write-Host ""

if ($created -gt 0) {
    Write-Host "DefectDojo is now configured with all required parsers!" -ForegroundColor Green
} else {
    Write-Host "All required parsers were already configured!" -ForegroundColor Green
}
Write-Host ""
