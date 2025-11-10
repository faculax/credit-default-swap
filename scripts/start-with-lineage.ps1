# Start CDS Platform with OpenLineage (Marquez)
# This script starts all services including the lineage visualization UI

Write-Host "=== Starting CDS Platform with OpenLineage (Marquez) ===" -ForegroundColor Cyan
Write-Host ""

# Start all services
Write-Host "Starting Docker services..." -ForegroundColor Green
docker-compose up -d

Write-Host ""
Write-Host "Waiting for services to be healthy..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Check service status
Write-Host ""
Write-Host "=== Service Status ===" -ForegroundColor Cyan
docker-compose ps

Write-Host ""
Write-Host "=== Checking Service Health ===" -ForegroundColor Cyan

# Check backend
try {
    $null = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 5
    Write-Host "✓ Backend:      HEALTHY" -ForegroundColor Green
} catch {
    Write-Host "✗ Backend:      NOT READY" -ForegroundColor Yellow
}

# Check Marquez
try {
    $null = Invoke-RestMethod -Uri "http://localhost:5001/healthcheck" -TimeoutSec 5
    Write-Host "✓ Marquez API:  HEALTHY" -ForegroundColor Green
} catch {
    Write-Host "✗ Marquez API:  NOT READY" -ForegroundColor Yellow
}

# Check Marquez Web
try {
    $null = Invoke-WebRequest -Uri "http://localhost:3001" -TimeoutSec 5 -UseBasicParsing
    Write-Host "✓ Marquez Web:  HEALTHY" -ForegroundColor Green
} catch {
    Write-Host "✗ Marquez Web:  NOT READY" -ForegroundColor Yellow
}

# Check frontend
try {
    $null = Invoke-WebRequest -Uri "http://localhost:3000" -TimeoutSec 5 -UseBasicParsing
    Write-Host "✓ Frontend:     HEALTHY" -ForegroundColor Green
} catch {
    Write-Host "✗ Frontend:     NOT READY" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Access Points ===" -ForegroundColor Cyan
Write-Host "CDS Platform Frontend:  http://localhost:3000" -ForegroundColor White
Write-Host "CDS Platform Backend:   http://localhost:8080" -ForegroundColor White
Write-Host "Marquez UI (Lineage):   http://localhost:3001" -ForegroundColor White
Write-Host "Marquez API:            http://localhost:5000" -ForegroundColor White
Write-Host "Gateway:                http://localhost:8081" -ForegroundColor White

Write-Host ""
Write-Host "=== Quick Commands ===" -ForegroundColor Cyan
Write-Host "View logs:              docker-compose logs -f" -ForegroundColor Gray
Write-Host "Stop services:          docker-compose down" -ForegroundColor Gray
Write-Host "Test lineage:           .\scripts\test-data-lineage.ps1" -ForegroundColor Gray
Write-Host "Check Marquez health:   Invoke-RestMethod http://localhost:8080/api/lineage/marquez/health" -ForegroundColor Gray

Write-Host ""
Write-Host "=== Next Steps ===" -ForegroundColor Cyan
Write-Host "1. Open Marquez UI:     Start-Process http://localhost:3001" -ForegroundColor White
Write-Host "2. Create a trade:      Use the CDS frontend or API" -ForegroundColor White
Write-Host "3. View lineage graph:  Navigate to 'credit-default-swap' namespace in Marquez" -ForegroundColor White

Write-Host ""
Write-Host "✓ All services started!" -ForegroundColor Green
Write-Host ""

# Optionally open Marquez UI
$openUI = Read-Host "Open Marquez UI in browser? (Y/n)"
if ($openUI -ne 'n' -and $openUI -ne 'N') {
    Start-Process "http://localhost:3001"
}
