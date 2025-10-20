<#
.SYNOPSIS
    SonarQube Setup Script for CDS Platform

.DESCRIPTION
    Sets up SonarQube Community Edition with PostgreSQL

.EXAMPLE
    .\setup-sonarqube.ps1
#>

$ErrorActionPreference = "Continue"

$BLUE = "Cyan"
$GREEN = "Green"
$YELLOW = "Yellow"
$RED = "Red"

Write-Host ""
Write-Host "" -ForegroundColor $BLUE
Write-Host "                                                            " -ForegroundColor $BLUE
Write-Host "          SonarQube Setup for CDS Platform                 " -ForegroundColor $BLUE
Write-Host "                                                            " -ForegroundColor $BLUE
Write-Host "" -ForegroundColor $BLUE
Write-Host ""

# Check if Docker is running
$dockerInfo = docker info 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Docker is running" -ForegroundColor $GREEN
    Write-Host ""
} else {
    Write-Host "❌ Docker is not running. Please start Docker Desktop/Rancher Desktop and try again." -ForegroundColor $RED
    exit 1
}

Write-Host "📋 Checking system requirements..." -ForegroundColor $BLUE
Write-Host ""

$computerMemory = Get-CimInstance Win32_OperatingSystem
$freeMemoryMB = [math]::Round($computerMemory.FreePhysicalMemory / 1024, 0)

if ($freeMemoryMB -lt 2048) {
    Write-Host "  Warning: Less than 2GB memory available." -ForegroundColor $YELLOW
    Write-Host "   Available: ${freeMemoryMB}MB" -ForegroundColor $YELLOW
} else {
    Write-Host " Memory: ${freeMemoryMB}MB available" -ForegroundColor $GREEN
}

Write-Host ""
Write-Host " Starting SonarQube..." -ForegroundColor $BLUE
Write-Host "This may take 2-3 minutes on first run..." -ForegroundColor $YELLOW
Write-Host ""

docker-compose -f docker-compose.sonarqube.yml up -d

Write-Host ""
Write-Host " Waiting for SonarQube to initialize..." -ForegroundColor $YELLOW
Write-Host ""

$retryCount = 0
$maxRetries = 60

while ($retryCount -lt $maxRetries) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:9000/api/system/status" -Method Get -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.status -eq "UP") {
            break
        }
    } catch {
        # Continue waiting
    }
    
    Write-Host "." -NoNewline
    Start-Sleep -Seconds 5
    $retryCount++
}

Write-Host ""
Write-Host ""

if ($retryCount -ge $maxRetries) {
    Write-Host " SonarQube failed to start" -ForegroundColor $RED
    Write-Host "Check logs: docker-compose -f docker-compose.sonarqube.yml logs" -ForegroundColor $YELLOW
    exit 1
}

Write-Host " SonarQube is ready!" -ForegroundColor $GREEN
Write-Host ""
Write-Host "" -ForegroundColor $BLUE
Write-Host " Access Information" -ForegroundColor $GREEN
Write-Host "" -ForegroundColor $BLUE
Write-Host ""
Write-Host "    URL:      http://localhost:9000" -ForegroundColor $BLUE
Write-Host "    Username: admin" -ForegroundColor $BLUE
Write-Host "    Password: admin" -ForegroundColor $BLUE
Write-Host ""
Write-Host "     IMPORTANT: Change the default password immediately!" -ForegroundColor $YELLOW
Write-Host ""

$openBrowser = Read-Host "Would you like to open SonarQube in your browser? (y/n)"

if ($openBrowser -eq 'y' -or $openBrowser -eq 'Y') {
    Start-Process "http://localhost:9000"
}

Write-Host ""
Write-Host "" -ForegroundColor $BLUE
Write-Host " Next Steps" -ForegroundColor $GREEN
Write-Host "" -ForegroundColor $BLUE
Write-Host ""
Write-Host "   1. Login to SonarQube at http://localhost:9000"
Write-Host "   2. Change admin password (User Menu  My Account  Security)"
Write-Host "   3. Generate API token (User Menu  My Account  Security  Generate Tokens)"
Write-Host "      Name: 'GitHub Actions', Type: 'Global Analysis Token'"
Write-Host "   4. Add GitHub Secrets:"
Write-Host "      SONAR_HOST_URL = http://localhost:9000"
Write-Host "      SONAR_TOKEN = <your-token>"
Write-Host "   5. Push code to trigger workflow"
Write-Host ""
Write-Host "" -ForegroundColor $BLUE
Write-Host "  Useful Commands" -ForegroundColor $GREEN
Write-Host "" -ForegroundColor $BLUE
Write-Host ""
Write-Host "   View logs:        docker-compose -f docker-compose.sonarqube.yml logs -f"
Write-Host "   Stop SonarQube:   docker-compose -f docker-compose.sonarqube.yml down"
Write-Host "   Restart:          docker-compose -f docker-compose.sonarqube.yml restart"
Write-Host "   Reset (delete):   docker-compose -f docker-compose.sonarqube.yml down -v"
Write-Host ""
Write-Host " Setup complete! SonarQube is ready for code analysis." -ForegroundColor $GREEN
Write-Host ""
