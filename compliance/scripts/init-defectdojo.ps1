#!/usr/bin/env pwsh
# DefectDojo Initialization Script
# Automates all setup steps for a fresh DefectDojo instance

param(
    [string]$DefectDojoUrl = "http://localhost:8081",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "admin",
    [string]$AdminEmail = "admin@localhost",
    [switch]$Verbose
)

$ErrorActionPreference = "Continue"

function Write-Step { param([string]$Message); Write-Host " $Message" -ForegroundColor Blue }
function Write-Success { param([string]$Message); Write-Host " $Message" -ForegroundColor Green }
function Write-Error-Msg { param([string]$Message); Write-Host " $Message" -ForegroundColor Red }
function Write-Warning-Msg { param([string]$Message); Write-Host " $Message" -ForegroundColor Yellow }

Write-Host ""
Write-Host "" -ForegroundColor Cyan
Write-Host " DEFECTDOJO INITIALIZATION" -ForegroundColor Cyan
Write-Host "" -ForegroundColor Cyan
Write-Host ""

# Wait for containers
Write-Step "Waiting for DefectDojo containers..."
$maxAttempts = 30
for ($i = 1; $i -le $maxAttempts; $i++) {
    $containers = docker ps --filter "name=defectdojo" --format "{{.Names}},{{.Status}}" | Out-String
    if ($containers -match "defectdojo-postgres.*healthy" -and $containers -match "defectdojo-uwsgi.*Up") {
        Write-Success "Containers are ready"
        break
    }
    Write-Host "  Waiting... (attempt $i/$maxAttempts)" -ForegroundColor Yellow
    Start-Sleep -Seconds 2
}

# Run migrations
Write-Step "Running database migrations..."
$null = docker exec defectdojo-uwsgi python manage.py migrate --noinput 2>&1
Write-Success "Database migrations completed"

# Create admin user
Write-Step "Creating admin user..."
$null = docker exec defectdojo-uwsgi python manage.py createsuperuser --noinput --username $AdminUsername --email $AdminEmail 2>&1
Write-Success "Admin user created"

# Set password and get token
Write-Step "Setting password and generating API token..."
$script:apiToken = $null
$cmd = "from django.contrib.auth.models import User; from rest_framework.authtoken.models import Token; u = User.objects.get(username='$AdminUsername'); u.set_password('$AdminPassword'); u.save(); t,c = Token.objects.get_or_create(user=u); print('TOKEN:' + t.key)"
$tokenOut = docker exec defectdojo-uwsgi python manage.py shell -c $cmd 2>&1 | Out-String
if ($tokenOut -match 'TOKEN:([a-f0-9]{40})') {
    $script:apiToken = $matches[1]
    Write-Success "API token generated"
} else {
    Write-Error-Msg "Failed to generate API token"
    exit 1
}

# Wait for API
Write-Step "Waiting for DefectDojo API..."
$headers = @{ "Authorization" = "Token $script:apiToken" }
for ($i = 1; $i -le 20; $i++) {
    try {
        $null = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/users/" -Headers $headers -TimeoutSec 5
        Write-Success "DefectDojo API is ready"
        break
    } catch {
        Write-Host "  Waiting... (attempt $i/20)" -ForegroundColor Yellow
        Start-Sleep -Seconds 3
    }
}

# Create product type
Write-Step "Creating product type..."
$headers['Content-Type'] = 'application/json'
try {
    $ptResp = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/product_types/" -Headers $headers -Method Get
    if ($ptResp.count -eq 0) {
        $ptBody = @{ name = "Application"; critical_product = $false } | ConvertTo-Json
        $pt = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/product_types/" -Headers $headers -Method Post -Body $ptBody
        Write-Success "Product type created (ID: $($pt.id))"
    } else {
        Write-Success "Product type already exists"
    }
} catch {
    Write-Error-Msg "Failed to create product type: $($_.Exception.Message)"
    exit 1
}

# Create development environment
Write-Step "Creating development environment..."
try {
    $envResp = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/development_environments/" -Headers $headers -Method Get
    if ($envResp.count -eq 0) {
        $envBody = @{ name = "Development" } | ConvertTo-Json
        $env = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/development_environments/" -Headers $headers -Method Post -Body $envBody
        Write-Success "Development environment created (ID: $($env.id))"
    } else {
        Write-Success "Development environment already exists"
    }
} catch {
    Write-Error-Msg "Failed to create environment: $($_.Exception.Message)"
    exit 1
}

# Create additional environments
Write-Step "Creating additional environments..."
@("Testing", "Staging", "Production") | ForEach-Object {
    try {
        $envBody = @{ name = $_ } | ConvertTo-Json
        $null = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/development_environments/" -Headers $headers -Method Post -Body $envBody
        if ($Verbose) { Write-Host "  + $_" -ForegroundColor Green }
    } catch {
        if ($Verbose) { Write-Host "  ~ $_ (may already exist)" -ForegroundColor Yellow }
    }
}
Write-Success "Additional environments created"

# Verify parsers
Write-Step "Verifying scan parsers..."
try {
    $ttResp = Invoke-RestMethod -Uri "$DefectDojoUrl/api/v2/test_types/" -Headers $headers -Method Get
    $required = @("SpotBugs Scan", "Dependency Check Scan")
    $found = ($ttResp.results | Where-Object { $required -contains $_.name }).Count
    Write-Success "Found $found/$($required.Count) required parsers"
} catch {
    Write-Warning-Msg "Could not verify parsers"
}

Write-Host ""
Write-Host "" -ForegroundColor Green
Write-Host " DEFECTDOJO INITIALIZATION COMPLETE" -ForegroundColor Green
Write-Host "" -ForegroundColor Green
Write-Host ""
Write-Host " Admin Credentials:" -ForegroundColor Cyan
Write-Host "   URL:      $DefectDojoUrl"
Write-Host "   Username: $AdminUsername"
Write-Host "   Password: $AdminPassword"
Write-Host ""
Write-Host " API Token:" -ForegroundColor Cyan
Write-Host "   $script:apiToken"
Write-Host ""
Write-Host " Next Steps:" -ForegroundColor Cyan
Write-Host "   1. Upload scans:  .\defectdojo.ps1 upload"
Write-Host "   2. View results:  $DefectDojoUrl"
Write-Host ""
Write-Host "" -ForegroundColor Green
Write-Host ""
