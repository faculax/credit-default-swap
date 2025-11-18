#!/usr/bin/env pwsh
# ReportPortal Startup Script for Windows/Linux/macOS
# Starts ReportPortal Docker stack and initializes CDS Platform project

param(
    [switch]$Clean,
    [switch]$Status,
    [switch]$Stop,
    [switch]$Logs,
    [switch]$Reset
)

$ErrorActionPreference = "Stop"

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$PROJECT_ROOT = Split-Path -Parent $SCRIPT_DIR
$COMPOSE_FILE = Join-Path $PROJECT_ROOT "docker-compose.reportportal.yml"

# Color functions
function Write-Success { Write-Host $args -ForegroundColor Green }
function Write-Info { Write-Host $args -ForegroundColor Cyan }
function Write-Warning { Write-Host $args -ForegroundColor Yellow }
function Write-Error { Write-Host $args -ForegroundColor Red }

# Display banner
function Show-Banner {
    Write-Host "`n" -NoNewline
    Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║         ReportPortal - CDS Platform Test Tracking         ║" -ForegroundColor Cyan
    Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
}

# Check prerequisites
function Test-Prerequisites {
    Write-Info "🔍 Checking prerequisites..."
    
    # Check Docker
    try {
        $dockerVersion = docker --version
        Write-Success "  ✓ Docker: $dockerVersion"
    } catch {
        Write-Error "  ✗ Docker not found. Please install Docker Desktop."
        exit 1
    }
    
    # Check Docker Compose
    try {
        $composeVersion = docker compose version
        Write-Success "  ✓ Docker Compose: $composeVersion"
    } catch {
        Write-Error "  ✗ Docker Compose not found."
        exit 1
    }
    
    # Check if Docker is running
    try {
        docker ps | Out-Null
        Write-Success "  ✓ Docker daemon is running"
    } catch {
        Write-Error "  ✗ Docker daemon is not running. Please start Docker Desktop."
        exit 1
    }
    
    Write-Host ""
}

# Show status
function Show-Status {
    Write-Info "📊 ReportPortal Status:`n"
    docker compose -f $COMPOSE_FILE ps
    Write-Host ""
    
    # Check if UI is accessible
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080" -Method Head -TimeoutSec 2 -UseBasicParsing -ErrorAction SilentlyContinue
        Write-Success "✓ UI accessible at: http://localhost:8080"
        Write-Info "  Login: default / 1q2w3e (change on first login)"
    } catch {
        Write-Warning "⚠ UI not yet accessible at http://localhost:8080"
    }
    
    Write-Host ""
}

# Stop ReportPortal
function Stop-ReportPortal {
    Write-Info "🛑 Stopping ReportPortal..."
    docker compose -f $COMPOSE_FILE down
    Write-Success "✓ ReportPortal stopped"
}

# Show logs
function Show-Logs {
    Write-Info "📋 ReportPortal Logs (press Ctrl+C to exit):`n"
    docker compose -f $COMPOSE_FILE logs -f
}

# Reset (clean all data)
function Reset-ReportPortal {
    Write-Warning "⚠️  WARNING: This will DELETE ALL ReportPortal data!"
    Write-Host "This includes:"
    Write-Host "  • All test launches and results"
    Write-Host "  • All projects and users"
    Write-Host "  • All attachments and logs"
    Write-Host ""
    
    $confirmation = Read-Host "Type 'YES' to confirm reset"
    
    if ($confirmation -ne "YES") {
        Write-Info "Reset cancelled."
        return
    }
    
    Write-Info "`n🗑️  Stopping and removing ReportPortal..."
    docker compose -f $COMPOSE_FILE down -v
    
    Write-Success "✓ ReportPortal reset complete. All data deleted."
}

# Start ReportPortal
function Start-ReportPortal {
    param([bool]$clean = $false)
    
    if ($clean) {
        Write-Info "🧹 Cleaning previous containers..."
        docker compose -f $COMPOSE_FILE down
        Write-Host ""
    }
    
    Write-Info "🚀 Starting ReportPortal services..."
    Write-Host "This may take 2-3 minutes on first run (downloading images)...`n"
    
    docker compose -f $COMPOSE_FILE up -d
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to start ReportPortal"
        exit 1
    }
    
    Write-Host ""
    Write-Info "⏳ Waiting for services to be healthy..."
    
    $maxWait = 120
    $waited = 0
    $interval = 5
    
    while ($waited -lt $maxWait) {
        Start-Sleep -Seconds $interval
        $waited += $interval
        
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080" -Method Head -TimeoutSec 2 -UseBasicParsing -ErrorAction SilentlyContinue
            if ($response.StatusCode -eq 200) {
                Write-Success "`n✓ ReportPortal is ready!"
                break
            }
        } catch {
            Write-Host "." -NoNewline
        }
    }
    
    if ($waited -ge $maxWait) {
        Write-Warning "`n⚠ ReportPortal didn't respond within $maxWait seconds"
        Write-Info "Check logs with: .\scripts\reportportal-start.ps1 -Logs"
    }
    
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║                    REPORTPORTAL READY                      ║" -ForegroundColor Green
    Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""
    Write-Success "🌐 Web UI:          http://localhost:8080"
    Write-Success "📡 API:             http://localhost:8585"
    Write-Success "🐰 RabbitMQ Admin:  http://localhost:15672"
    Write-Success "📦 MinIO Console:   http://localhost:9001"
    Write-Host ""
    Write-Info "📝 Default Login:"
    Write-Host "   Username: default"
    Write-Host "   Password: 1q2w3e"
    Write-Warning "   ⚠️  Change password on first login!"
    Write-Host ""
    Write-Info "📚 Next Steps:"
    Write-Host "   1. Open http://localhost:8080"
    Write-Host "   2. Login with default credentials"
    Write-Host "   3. Create project: 'cds-platform'"
    Write-Host "   4. Get API token: Profile -> API Keys"
    Write-Host "   5. Configure test-evidence-framework/reportportal.json"
    Write-Host ""
    Write-Info "💡 Useful Commands:"
    Write-Host "   Status:  .\scripts\reportportal-start.ps1 -Status"
    Write-Host "   Logs:    .\scripts\reportportal-start.ps1 -Logs"
    Write-Host "   Stop:    .\scripts\reportportal-start.ps1 -Stop"
    Write-Host "   Reset:   .\scripts\reportportal-start.ps1 -Reset"
    Write-Host ""
}

# Main execution
Show-Banner
Test-Prerequisites

if ($Status) {
    Show-Status
} elseif ($Stop) {
    Stop-ReportPortal
} elseif ($Logs) {
    Show-Logs
} elseif ($Reset) {
    Reset-ReportPortal
} else {
    Start-ReportPortal -clean $Clean
}

