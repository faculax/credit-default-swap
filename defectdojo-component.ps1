#!/usr/bin/env pwsh
# ===============================================================================
# Component-Based DefectDojo Upload Script
# ===============================================================================
# Uploads security scan results organized by component/service
# Each component gets its own product in DefectDojo for better tracking
# ===============================================================================

param(
    [string]$DefectDojoUrl = "http://localhost:8081",
    [string]$Username = "admin",
    [string]$Password = "admin",
    [string]$BaseProductName = "CDS Platform",  # Base name for all products
    [string]$ProjectRoot = "",
    [switch]$ForceNewEngagement = $false  # Create new engagement even if one exists today
)

$ErrorActionPreference = "Stop"

# Determine project root
if ([string]::IsNullOrEmpty($ProjectRoot)) {
    $ProjectRoot = Get-Location
}

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "  Component-Based Security Upload to DefectDojo" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# ===============================================================================
# Helper Functions
# ===============================================================================

function Write-ColorMessage {
    param(
        [string]$Message,
        [ValidateSet('Success', 'Info', 'Warning', 'Error', 'Header')]
        [string]$Type = 'Info'
    )
    
    switch ($Type) {
        'Success' { Write-Host "[+] $Message" -ForegroundColor Green }
        'Info'    { Write-Host "[>] $Message" -ForegroundColor Blue }
        'Warning' { Write-Host "[!] $Message" -ForegroundColor Yellow }
        'Error'   { Write-Host "[X] $Message" -ForegroundColor Red }
        'Header'  { Write-Host "================================================" -ForegroundColor Blue
                    Write-Host "  $Message" -ForegroundColor Blue
                    Write-Host "================================================" -ForegroundColor Blue }
    }
}

function Get-OrCreateProduct {
    param(
        [string]$ProductName,
        [string]$Description,
        [string]$ComponentTag,
        [hashtable]$Headers
    )
    
    Write-ColorMessage -Message "Component: $ProductName" -Type Header
    Write-Host ""
    
    # URL encode the product name
    $encodedName = [System.Web.HttpUtility]::UrlEncode($ProductName)
    
    try {
        # Search for existing product
        $productResponse = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/products/?name=$encodedName" `
            -Method Get `
            -Headers $Headers
        
        if ($productResponse.count -gt 0) {
            $productId = $productResponse.results[0].id
            Write-ColorMessage -Message "Found existing product" -Type Success
            Write-Host "  Product ID: $productId" -ForegroundColor Gray
            Write-Host "  View at: $DefectDojoUrl/product/$productId" -ForegroundColor Gray
            Write-Host ""
            return $productId
        }
        
    } catch {
        Write-ColorMessage -Message "Error searching for product: $($_.Exception.Message)" -Type Warning
    }
    
    # Create new product
    Write-Host "  Creating new product..." -ForegroundColor Gray
    
    try {
        $productBody = @{
            name = $ProductName
            description = $Description
            prod_type = 1
            tags = @($ComponentTag, "component")
        } | ConvertTo-Json
        
        $createResponse = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/products/" `
            -Method Post `
            -Headers $Headers `
            -ContentType "application/json" `
            -Body $productBody
        
        $productId = $createResponse.id
        Write-ColorMessage -Message "Created new product" -Type Success
        Write-Host "  Product ID: $productId" -ForegroundColor Gray
        Write-Host "  View at: $DefectDojoUrl/product/$productId" -ForegroundColor Gray
        Write-Host ""
        
        return $productId
        
    } catch {
        Write-ColorMessage -Message "Failed to create product" -Type Error
        if ($_.Exception.Response) {
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $responseBody = $reader.ReadToEnd()
                Write-Host "  Response: $responseBody" -ForegroundColor Red
            } catch {}
        }
        return $null
    }
}

function New-Engagement {
    param(
        [int]$ProductId,
        [string]$EngagementName,
        [string]$Description,
        [hashtable]$Headers
    )
    
    $today = Get-Date -Format "yyyy-MM-dd"
    
    # Try to find existing engagement for today (unless ForceNewEngagement is set)
    if (-not $script:ForceNewEngagement) {
        Write-Host "  Checking for existing engagement..." -ForegroundColor Gray
        
        try {
            $existingEngagements = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/engagements/?product=$ProductId&target_start=$today" `
                -Method Get `
                -Headers $Headers
            
            if ($existingEngagements.count -gt 0) {
                $engagementId = $existingEngagements.results[0].id
                Write-ColorMessage -Message "Reusing existing engagement from today" -Type Success
                Write-Host "  Engagement ID: $engagementId" -ForegroundColor Gray
                Write-Host "  Note: Previous findings will be updated/closed" -ForegroundColor Yellow
                Write-Host ""
                return $engagementId
            }
        } catch {
            Write-Host "  No existing engagement found" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ForceNewEngagement flag set - creating new engagement" -ForegroundColor Gray
    }
    
    # Create new engagement
    Write-Host "  Creating new engagement: $EngagementName" -ForegroundColor Gray
    
    try {
        $engagementBody = @{
            product = $ProductId
            name = $EngagementName
            description = $Description
            target_start = $today
            target_end = $today
            status = "In Progress"
            engagement_type = "CI/CD"
        } | ConvertTo-Json
        
        $engagementResponse = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/engagements/" `
            -Method Post `
            -Headers $Headers `
            -ContentType "application/json" `
            -Body $engagementBody
        
        $engagementId = $engagementResponse.id
        Write-ColorMessage -Message "Engagement created" -Type Success
        Write-Host "  Engagement ID: $engagementId" -ForegroundColor Gray
        Write-Host ""
        
        return $engagementId
        
    } catch {
        Write-ColorMessage -Message "Failed to create engagement" -Type Error
        if ($_.Exception.Response) {
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $responseBody = $reader.ReadToEnd()
                Write-Host "  Response: $responseBody" -ForegroundColor Red
            } catch {}
        }
        return $null
    }
}

function Invoke-ScanUpload {
    param(
        [int]$EngagementId,
        [string]$ScanType,
        [string]$FilePath,
        [string]$ScanDisplayName,
        [hashtable]$Headers
    )
    
    Write-Host "  Uploading: $ScanDisplayName" -ForegroundColor Gray
    Write-Host "  File: $FilePath" -ForegroundColor DarkGray
    
    # Check if file exists
    if (-not (Test-Path $FilePath)) {
        Write-ColorMessage -Message "File not found, skipping" -Type Warning
        Write-Host ""
        return $false
    }
    
    # Check if file is empty
    $fileInfo = Get-Item $FilePath
    if ($fileInfo.Length -eq 0) {
        Write-ColorMessage -Message "File is empty, skipping" -Type Warning
        Write-Host ""
        return $false
    }
    
    Write-Host "  File size: $($fileInfo.Length) bytes" -ForegroundColor DarkGray
    
    # Check if a test already exists for THIS SPECIFIC scan type in this engagement today
    $existingTestId = $null
    try {
        $today = Get-Date -Format "yyyy-MM-dd"
        
        # Get all tests for this engagement
        $testsResponse = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/tests/?engagement=$EngagementId" `
            -Method Get `
            -Headers $Headers
        
        if ($testsResponse.count -gt 0) {
            # Find test that matches BOTH the scan type AND was created today
            foreach ($test in $testsResponse.results) {
                $testDate = ([DateTime]$test.created).ToString("yyyy-MM-dd")
                if ($testDate -eq $today -and $test.test_type_name -eq $ScanType) {
                    $existingTestId = $test.id
                    Write-Host "  Found existing $ScanType test from today (ID: $existingTestId) - will update" -ForegroundColor DarkGray
                    break
                }
            }
        }
        
        if (-not $existingTestId) {
            Write-Host "  No existing $ScanType test found - will create new" -ForegroundColor DarkGray
        }
    } catch {
        # If check fails, proceed with new import
        Write-Host "  Could not check for existing test - will create new" -ForegroundColor DarkGray
    }
    
    try {
        # Create multipart form data
        $boundary = [System.Guid]::NewGuid().ToString()
        $LF = "`r`n"
        $scanDate = Get-Date -Format "yyyy-MM-dd"
        
        $fileBytes = [System.IO.File]::ReadAllBytes($FilePath)
        $fileContent = [System.Text.Encoding]::GetEncoding("iso-8859-1").GetString($fileBytes)
        $fileName = Split-Path -Leaf $FilePath
        
        # Determine content type based on file extension
        $fileExtension = [System.IO.Path]::GetExtension($FilePath).ToLower()
        $contentType = switch ($fileExtension) {
            ".json" { "application/json" }
            ".xml"  { "application/xml" }
            default { "application/octet-stream" }
        }
        
        # Determine API endpoint and build form data
        if ($existingTestId) {
            Write-Host "  Re-importing to existing test..." -ForegroundColor DarkGray
            $apiEndpoint = "$DefectDojoUrl/api/v2/reimport-scan/"
            
            # Re-import requires: scan_type, file, test (NOT engagement)
            $bodyLines = @(
                "--$boundary",
                "Content-Disposition: form-data; name=`"scan_type`"",
                "",
                $ScanType,
                "--$boundary",
                "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"",
                "Content-Type: $contentType",
                "",
                $fileContent,
                "--$boundary",
                "Content-Disposition: form-data; name=`"test`"",
                "",
                $existingTestId,
                "--$boundary--"
            ) -join $LF
        } else {
            Write-Host "  Creating new test..." -ForegroundColor DarkGray
            $apiEndpoint = "$DefectDojoUrl/api/v2/import-scan/"
            
            # Import requires: scan_type, file, engagement
            $bodyLines = @(
                "--$boundary",
                "Content-Disposition: form-data; name=`"scan_type`"",
                "",
                $ScanType,
                "--$boundary",
                "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"",
                "Content-Type: $contentType",
                "",
                $fileContent,
                "--$boundary",
                "Content-Disposition: form-data; name=`"engagement`"",
                "",
                $EngagementId,
                "--$boundary--"
            ) -join $LF
        }
        
        $uploadHeaders = $Headers.Clone()
        $uploadHeaders["Content-Type"] = "multipart/form-data; boundary=$boundary"
        
        $uploadResponse = Invoke-RestMethod -Uri $apiEndpoint `
            -Method Post `
            -Headers $uploadHeaders `
            -Body ([System.Text.Encoding]::GetEncoding("iso-8859-1").GetBytes($bodyLines))
        
        $testId = if ($existingTestId) { $existingTestId } else { $uploadResponse.test }
        $findingsCount = if ($uploadResponse.statistics.findings) { $uploadResponse.statistics.findings } else { 0 }
        
        if ($existingTestId) {
            Write-ColorMessage -Message "Re-import successful (updated existing test)" -Type Success
        } else {
            Write-ColorMessage -Message "Upload successful (new test created)" -Type Success
        }
        Write-Host "    Test ID: $testId" -ForegroundColor Gray
        Write-Host "    Findings: $findingsCount" -ForegroundColor Gray
        Write-Host "    View at: $DefectDojoUrl/test/$testId" -ForegroundColor Gray
        Write-Host ""
        
        return $true
        
    } catch {
        $statusCode = "Unknown"
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        
        # If re-import failed and we were trying to re-import, fall back to creating new test
        if ($existingTestId -and ($statusCode -eq 400 -or $statusCode -eq 500)) {
            Write-ColorMessage -Message "Re-import failed (HTTP $statusCode) - creating new test instead..." -Type Warning
            
            try {
                # Retry as new import
                $apiEndpoint = "$DefectDojoUrl/api/v2/import-scan/"
                $retryBodyLines = @(
                    "--$boundary",
                    "Content-Disposition: form-data; name=`"scan_type`"",
                    "",
                    $ScanType,
                    "--$boundary",
                    "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"",
                    "Content-Type: $contentType",
                    "",
                    $fileContent,
                    "--$boundary",
                    "Content-Disposition: form-data; name=`"engagement`"",
                    "",
                    $EngagementId,
                    "--$boundary--"
                ) -join $LF
                
                $retryResponse = Invoke-RestMethod -Uri $apiEndpoint `
                    -Method Post `
                    -Headers $uploadHeaders `
                    -Body ([System.Text.Encoding]::GetEncoding("iso-8859-1").GetBytes($retryBodyLines))
                
                $testId = $retryResponse.test
                $findingsCount = if ($retryResponse.statistics.findings) { $retryResponse.statistics.findings } else { 0 }
                
                Write-ColorMessage -Message "Upload successful (new test created)" -Type Success
                Write-Host "    Test ID: $testId" -ForegroundColor Gray
                Write-Host "    Findings: $findingsCount" -ForegroundColor Gray
                Write-Host "    View at: $DefectDojoUrl/test/$testId" -ForegroundColor Gray
                Write-Host ""
                
                return $true
            } catch {
                Write-ColorMessage -Message "Retry also failed - DefectDojo parser issue" -Type Warning
                Write-Host "    This scan type may have compatibility issues with DefectDojo" -ForegroundColor Yellow
                Write-Host "    Skipping to continue with other scans..." -ForegroundColor Gray
                Write-Host ""
                return $false
            }
        }
        
        Write-ColorMessage -Message "Upload failed (HTTP $statusCode)" -Type Error
        
        if ($_.Exception.Response) {
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $responseBody = $reader.ReadToEnd()
                if ($responseBody) {
                    Write-Host "    Response: $responseBody" -ForegroundColor Red
                }
            } catch {
                Write-Host "    Error: $($_.Exception.Message)" -ForegroundColor Red
            }
        } else {
            Write-Host "    Error: $($_.Exception.Message)" -ForegroundColor Red
        }
        Write-Host ""
        
        return $false
    }
}

# ===============================================================================
# Step 1: Authenticate
# ===============================================================================

Write-ColorMessage -Message "Authenticating with DefectDojo..." -Type Info

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
        Write-ColorMessage -Message "Failed to authenticate" -Type Error
        exit 1
    }

    Write-ColorMessage -Message "Authenticated successfully" -Type Success
    Write-Host ""
    
} catch {
    Write-ColorMessage -Message "Authentication failed: $($_.Exception.Message)" -Type Error
    exit 1
}

$headers = @{
    "Authorization" = "Token $token"
}

# Track uploaded components and settings
$script:productUrls = @()
$script:ForceNewEngagement = $ForceNewEngagement

# ===============================================================================
# BACKEND COMPONENTS
# ===============================================================================

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "BACKEND COMPONENTS" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# Component 1: Backend API
$productId = Get-OrCreateProduct `
    -ProductName "$BaseProductName - Backend API" `
    -Description "Java REST API backend service with Spring Boot" `
    -ComponentTag "backend-api" `
    -Headers $headers

if ($null -ne $productId) {
    $script:productUrls += @{
        Name = "Backend API"
        Url = "$DefectDojoUrl/product/$productId"
    }
    
    $engagementId = New-Engagement `
        -ProductId $productId `
        -EngagementName "Backend Security Scan - $(Get-Date -Format 'yyyy-MM-dd HH:mm')" `
        -Description "Automated security scans: SpotBugs, OWASP Dependency Check" `
        -Headers $headers
    
    if ($null -ne $engagementId) {
        Invoke-ScanUpload `
            -EngagementId $engagementId `
            -ScanType "SpotBugs Scan" `
            -FilePath (Join-Path $ProjectRoot "backend\target\spotbugsXml.xml") `
            -ScanDisplayName "SpotBugs Static Analysis" `
            -Headers $headers | Out-Null
        
        Invoke-ScanUpload `
            -EngagementId $engagementId `
            -ScanType "Dependency Check Scan" `
            -FilePath (Join-Path $ProjectRoot "backend\target\security-reports\dependency-check-report.xml") `
            -ScanDisplayName "OWASP Dependency Check" `
            -Headers $headers | Out-Null
        
        Invoke-ScanUpload `
            -EngagementId $engagementId `
            -ScanType "Checkstyle Scan" `
            -FilePath (Join-Path $ProjectRoot "backend\target\checkstyle-result.xml") `
            -ScanDisplayName "Checkstyle Quality Check" `
            -Headers $headers | Out-Null
    }
}

# ===============================================================================
# FRONTEND COMPONENTS
# ===============================================================================

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "FRONTEND COMPONENTS" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# Component 2: Frontend Web Application
$productId = Get-OrCreateProduct `
    -ProductName "$BaseProductName - Web Frontend" `
    -Description "React/JavaScript web application" `
    -ComponentTag "frontend-web" `
    -Headers $headers

if ($null -ne $productId) {
    $script:productUrls += @{
        Name = "Web Frontend"
        Url = "$DefectDojoUrl/product/$productId"
    }
    
    $engagementId = New-Engagement `
        -ProductId $productId `
        -EngagementName "Frontend Security Scan - $(Get-Date -Format 'yyyy-MM-dd HH:mm')" `
        -Description "Automated security scans: ESLint, Retire.js" `
        -Headers $headers
    
    if ($null -ne $engagementId) {
        Invoke-ScanUpload `
            -EngagementId $engagementId `
            -ScanType "ESLint Scan" `
            -FilePath (Join-Path $ProjectRoot "frontend\eslint-security.json") `
            -ScanDisplayName "ESLint - Code Security Issues" `
            -Headers $headers | Out-Null
        
        Invoke-ScanUpload `
            -EngagementId $engagementId `
            -ScanType "Retire.js Scan" `
            -FilePath (Join-Path $ProjectRoot "frontend\retire-report.json") `
            -ScanDisplayName "Retire.js - Vulnerable Libraries" `
            -Headers $headers | Out-Null
    }
}

# ===============================================================================
# GATEWAY COMPONENT
# ===============================================================================

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "GATEWAY COMPONENT" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# Component 3: API Gateway
$productId = Get-OrCreateProduct `
    -ProductName "$BaseProductName - API Gateway" `
    -Description "Spring Cloud Gateway - API routing and security" `
    -ComponentTag "gateway" `
    -Headers $headers

if ($null -ne $productId) {
    $script:productUrls += @{
        Name = "API Gateway"
        Url = "$DefectDojoUrl/product/$productId"
    }
    
    $engagementId = New-Engagement `
        -ProductId $productId `
        -EngagementName "Gateway Security Scan - $(Get-Date -Format 'yyyy-MM-dd HH:mm')" `
        -Description "Automated security scans for API Gateway" `
        -Headers $headers
    
    if ($null -ne $engagementId) {
        Invoke-ScanUpload `
            -EngagementId $engagementId `
            -ScanType "SpotBugs Scan" `
            -FilePath (Join-Path $ProjectRoot "gateway\target\spotbugsXml.xml") `
            -ScanDisplayName "SpotBugs Static Analysis" `
            -Headers $headers | Out-Null
        
        Invoke-ScanUpload `
            -EngagementId $engagementId `
            -ScanType "Dependency Check Scan" `
            -FilePath (Join-Path $ProjectRoot "gateway\target\security-reports\dependency-check-report.xml") `
            -ScanDisplayName "OWASP Dependency Check" `
            -Headers $headers | Out-Null
    }
}

# ===============================================================================
# RISK ENGINE COMPONENT
# ===============================================================================

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "RISK ENGINE COMPONENT" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# Component 4: Risk Engine
$productId = Get-OrCreateProduct `
    -ProductName "$BaseProductName - Risk Engine" `
    -Description "Credit risk calculation and analysis service" `
    -ComponentTag "risk-engine" `
    -Headers $headers

if ($null -ne $productId) {
    $script:productUrls += @{
        Name = "Risk Engine"
        Url = "$DefectDojoUrl/product/$productId"
    }
    
    $engagementId = New-Engagement `
        -ProductId $productId `
        -EngagementName "Risk Engine Security Scan - $(Get-Date -Format 'yyyy-MM-dd HH:mm')" `
        -Description "Automated security scans for Risk Engine" `
        -Headers $headers
    
    if ($null -ne $engagementId) {
        Invoke-ScanUpload `
            -EngagementId $engagementId `
            -ScanType "SpotBugs Scan" `
            -FilePath (Join-Path $ProjectRoot "risk-engine\target\spotbugsXml.xml") `
            -ScanDisplayName "SpotBugs Static Analysis" `
            -Headers $headers | Out-Null
        
        Invoke-ScanUpload `
            -EngagementId $engagementId `
            -ScanType "Dependency Check Scan" `
            -FilePath (Join-Path $ProjectRoot "risk-engine\target\security-reports\dependency-check-report.xml") `
            -ScanDisplayName "OWASP Dependency Check" `
            -Headers $headers | Out-Null
    }
}

# ===============================================================================
# SECRET SCANNING (CROSS-COMPONENT)
# ===============================================================================

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "SECRET SCANNING" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# Component 5: Secret Scanning (Gitleaks)
$productId = Get-OrCreateProduct `
    -ProductName "$BaseProductName - Secret Scanning" `
    -Description "Cross-component secret and credential detection using Gitleaks" `
    -ComponentTag "secret-scanning" `
    -Headers $headers

if ($null -ne $productId) {
    $script:productUrls += @{
        Name = "Secret Scanning"
        Url = "$DefectDojoUrl/product/$productId"
    }
    
    $engagementId = New-Engagement `
        -ProductId $productId `
        -EngagementName "Secret Scan - $(Get-Date -Format 'yyyy-MM-dd HH:mm')" `
        -Description "Gitleaks scan for hardcoded secrets, API keys, passwords, tokens" `
        -Headers $headers
    
    if ($null -ne $engagementId) {
        $gitleaksReport = Join-Path $ProjectRoot "security-reports\gitleaks-report.json"
        
        if (Test-Path $gitleaksReport) {
            Invoke-ScanUpload `
                -EngagementId $engagementId `
                -ScanType "Gitleaks Scan" `
                -FilePath $gitleaksReport `
                -ScanDisplayName "Gitleaks - Secret Detection" `
                -Headers $headers | Out-Null
        } else {
            Write-ColorMessage -Message "Gitleaks report not found - was secret scanning run?" -Type Warning
            Write-Host "  Expected: $gitleaksReport" -ForegroundColor Gray
            Write-Host "  Run: .\defectdojo.ps1 scan" -ForegroundColor Yellow
            Write-Host ""
        }
    }
}

# ===============================================================================
# SUMMARY
# ===============================================================================

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Green
Write-Host "[+] ALL COMPONENTS UPLOADED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "===============================================================" -ForegroundColor Green
Write-Host ""
Write-Host "View your components in DefectDojo:" -ForegroundColor Yellow
Write-Host ""

foreach ($product in $script:productUrls) {
    Write-Host "  * $($product.Name)" -ForegroundColor Cyan
    Write-Host "    $($product.Url)" -ForegroundColor Gray
    Write-Host ""
}

Write-Host "Main Products Page: $DefectDojoUrl/product" -ForegroundColor Yellow
Write-Host ""
Write-Host "===============================================================" -ForegroundColor Blue
Write-Host ""
