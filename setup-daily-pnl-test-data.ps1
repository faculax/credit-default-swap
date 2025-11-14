# Daily P&L Test Data Setup Script
# This script creates test trades and runs EOD jobs to generate P&L data

Write-Host "================================" -ForegroundColor Cyan
Write-Host "Daily P&L Test Data Setup" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080/api"

# Function to create a trade
function Create-Trade {
    param(
        [string]$referenceEntity,
        [decimal]$notional,
        [int]$spread,
        [string]$maturityDate,
        [string]$tradeDate,
        [string]$buySellProtection,
        [string]$counterparty
    )
    
    # Calculate effective date (T+1 from trade date for standard CDS)
    $effectiveDate = ([datetime]::Parse($tradeDate)).AddDays(1).ToString("yyyy-MM-dd")
    $accrualStartDate = $effectiveDate
    
    $body = @{
        referenceEntity = $referenceEntity
        notionalAmount = $notional
        spread = $spread
        maturityDate = $maturityDate
        tradeDate = $tradeDate
        effectiveDate = $effectiveDate
        accrualStartDate = $accrualStartDate
        buySellProtection = $buySellProtection
        counterparty = $counterparty
        currency = "USD"
        premiumFrequency = "QUARTERLY"
        dayCountConvention = "ACT_360"
        paymentCalendar = "NYC"
        tradeStatus = "ACTIVE"
        recoveryRate = 40.00
        settlementType = "CASH"
        isCleared = $false
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Method POST -Uri "$baseUrl/cds-trades" -ContentType "application/json" -Body $body
        Write-Host "[OK] Created trade: $referenceEntity ($buySellProtection $notional)" -ForegroundColor Green
        return $response.id
    } catch {
        Write-Host "[ERROR] Failed to create trade: $referenceEntity" -ForegroundColor Red
        Write-Host "  Error: $_" -ForegroundColor Red
        return $null
    }
}

# Function to trigger EOD job
function Run-EODJob {
    param(
        [string]$date,
        [string]$label
    )
    
    Write-Host "Running EOD job for $label ($date)..." -ForegroundColor Yellow
    
    try {
        $uri = "$baseUrl/eod/valuation-jobs/trigger?valuationDate=$date&triggeredBy=TEST_SCRIPT&dryRun=false"
        $response = Invoke-RestMethod -Method POST -Uri $uri
        Write-Host "[OK] EOD job started - Job ID: $($response.jobId)" -ForegroundColor Green
        return $response.jobId
    } catch {
        Write-Host "[ERROR] Failed to start EOD job for $date" -ForegroundColor Red
        Write-Host "  Error: $_" -ForegroundColor Red
        return $null
    }
}

# Wait for backend to be ready
Write-Host "Checking if backend is ready..." -ForegroundColor Yellow
$retries = 0
$maxRetries = 10
while ($retries -lt $maxRetries) {
    try {
        $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 2
        if ($health.status -eq "UP") {
            Write-Host "[OK] Backend is ready!" -ForegroundColor Green
            break
        }
    } catch {
        $retries++
        Write-Host "  Waiting for backend... (attempt $retries/$maxRetries)" -ForegroundColor Gray
        Start-Sleep -Seconds 3
    }
}

if ($retries -eq $maxRetries) {
    Write-Host "[ERROR] Backend is not responding. Please make sure Docker containers are running." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 1: Creating Test Trades" -ForegroundColor Cyan
Write-Host "-----------------------------" -ForegroundColor Cyan

# Create 5 diverse test trades
$trades = @()
$trades += Create-Trade -referenceEntity "Apple Inc" -notional 10000000 -spread 150 -maturityDate "2028-12-20" -tradeDate "2025-09-15" -buySellProtection "BUY" -counterparty "Goldman Sachs"
$trades += Create-Trade -referenceEntity "Tesla Inc" -notional 15000000 -spread 320 -maturityDate "2029-06-20" -tradeDate "2025-10-01" -buySellProtection "SELL" -counterparty "JP Morgan"
$trades += Create-Trade -referenceEntity "Microsoft Corp" -notional 20000000 -spread 120 -maturityDate "2030-12-20" -tradeDate "2025-10-10" -buySellProtection "BUY" -counterparty "Morgan Stanley"
$trades += Create-Trade -referenceEntity "Amazon.com Inc" -notional 8000000 -spread 180 -maturityDate "2027-09-20" -tradeDate "2025-10-20" -buySellProtection "BUY" -counterparty "Citibank"
$trades += Create-Trade -referenceEntity "General Electric" -notional 12000000 -spread 280 -maturityDate "2028-03-20" -tradeDate "2025-11-01" -buySellProtection "SELL" -counterparty "Bank of America"

$successfulTrades = ($trades | Where-Object { $_ -ne $null }).Count
Write-Host ""
Write-Host "Created $successfulTrades out of 5 trades" -ForegroundColor $(if ($successfulTrades -eq 5) { "Green" } else { "Yellow" })

if ($successfulTrades -eq 0) {
    Write-Host "[ERROR] No trades were created. Exiting." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 2: Running EOD Jobs" -ForegroundColor Cyan
Write-Host "------------------------" -ForegroundColor Cyan
Write-Host ""

# Get yesterday's date (T-1)
$yesterday = (Get-Date).AddDays(-1).ToString("yyyy-MM-dd")
# Get today's date (T)
$today = (Get-Date).ToString("yyyy-MM-dd")

# Run EOD job for T-1 (creates baseline valuations)
$job1 = Run-EODJob -date $yesterday -label "T-1 (Yesterday)"

if ($job1) {
    Write-Host "  Waiting 30 seconds for T-1 job to complete..." -ForegroundColor Gray
    Start-Sleep -Seconds 30
}

Write-Host ""

# Run EOD job for T (calculates P&L by comparing T vs T-1)
$job2 = Run-EODJob -date $today -label "T (Today)"

if ($job2) {
    Write-Host "  Waiting 30 seconds for T job to complete..." -ForegroundColor Gray
    Start-Sleep -Seconds 30
}

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "What to do next:" -ForegroundColor Yellow
Write-Host "1. Open your browser to: http://localhost:3000" -ForegroundColor White
Write-Host "2. Click the 'Daily P&L' button in the navigation" -ForegroundColor White
Write-Host "3. Select today's date: $today" -ForegroundColor White
Write-Host "4. View P&L results with full attribution breakdown!" -ForegroundColor White
Write-Host ""
Write-Host "Summary Statistics:" -ForegroundColor Yellow
Write-Host "  Trades Created: $successfulTrades" -ForegroundColor White
Write-Host "  Total Notional: " -NoNewline -ForegroundColor White
Write-Host "`$65,000,000" -ForegroundColor Cyan
Write-Host "  T-1 Job: $(if ($job1) { '[OK] Completed' } else { '[FAILED]' })" -ForegroundColor $(if ($job1) { "Green" } else { "Red" })
Write-Host "  T Job: $(if ($job2) { '[OK] Completed' } else { '[FAILED]' })" -ForegroundColor $(if ($job2) { "Green" } else { "Red" })
Write-Host ""
