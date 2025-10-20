<#
.SYNOPSIS
    SonarQube Setup Script for CDS Platform

.DESCRIPTION
    Sets up SonarQube Community Edition with PostgreSQL for the Credit Default Swap platform

.EXAMPLE
    .\setup-sonarqube.ps1
#>

$ErrorActionPreference = "Stop"

# Colors
$BLUE = "Cyan"
$GREEN = "Green"
$YELLOW = "Yellow"
$RED = "Red"

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor $BLUE
Write-Host "║                                                            ║" -ForegroundColor $BLUE
Write-Host "║          SonarQube Setup for CDS Platform                 ║" -ForegroundColor $BLUE
Write-Host "║                                                            ║" -ForegroundColor $BLUE
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor $BLUE
Write-Host ""

# Check if Docker is running
try {
    $null = docker info 2>&1
    Write-Host "✓ Docker is running" -ForegroundColor $GREEN
    Write-Host ""
}
catch {
    Write-Host "❌ Docker is not running. Please start Docker Desktop and try again." -ForegroundColor $RED
    exit 1
}

# Check system requirements
Write-Host "📋 Checking system requirements..." -ForegroundColor $BLUE
Write-Host ""

# Get available memory
$computerMemory = Get-CimInstance Win32_OperatingSystem
$freeMemoryMB = [math]::Round($computerMemory.FreePhysicalMemory / 1024, 0)

if ($freeMemoryMB -lt 2048) {
    Write-Host "⚠️  Warning: Less than 2GB memory available. SonarQube may run slowly." -ForegroundColor $YELLOW
    Write-Host "   Available: ${freeMemoryMB}MB" -ForegroundColor $YELLOW
} else {
    Write-Host "✓ Memory: ${freeMemoryMB}MB available" -ForegroundColor $GREEN
}

# Note: vm.max_map_count is not applicable on Windows
# Docker Desktop for Windows handles this automatically

Write-Host ""

# Start SonarQube
Write-Host "🚀 Starting SonarQube..." -ForegroundColor $BLUE
Write-Host "This may take 2-3 minutes on first run..." -ForegroundColor $YELLOW
Write-Host ""

docker-compose -f docker-compose.sonarqube.yml up -d

Write-Host ""
Write-Host "⏳ Waiting for SonarQube to initialize..." -ForegroundColor $YELLOW
Write-Host ""

# Wait for SonarQube to be ready
$retryCount = 0
$maxRetries = 60

while ($retryCount -lt $maxRetries) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:9000/api/system/status" -Method Get -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.status -eq "UP") {
            break
        }
    }
    catch {
        # Continue waiting
    }
    
    Write-Host "." -NoNewline
    Start-Sleep -Seconds 5
    $retryCount++
}

Write-Host ""
Write-Host ""

if ($retryCount -ge $maxRetries) {
    Write-Host "❌ SonarQube failed to start within expected time" -ForegroundColor $RED
    Write-Host "Check logs with: docker-compose -f docker-compose.sonarqube.yml logs" -ForegroundColor $YELLOW
    exit 1
}

Write-Host "✅ SonarQube is ready!" -ForegroundColor $GREEN
Write-Host ""

# Display access information
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor $BLUE
Write-Host "📍 Access Information" -ForegroundColor $GREEN
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor $BLUE
Write-Host ""
Write-Host "   🌐 URL:      " -NoNewline
Write-Host "http://localhost:9000" -ForegroundColor $BLUE
Write-Host "   👤 Username: " -NoNewline
Write-Host "admin" -ForegroundColor $BLUE
Write-Host "   🔑 Password: " -NoNewline
Write-Host "admin" -ForegroundColor $BLUE
Write-Host ""
Write-Host "   ⚠️  IMPORTANT: Change the default password immediately!" -ForegroundColor $YELLOW
Write-Host ""

# Prompt to open browser
$openBrowser = Read-Host "Would you like to open SonarQube in your browser? (y/n)"

if ($openBrowser -eq 'y' -or $openBrowser -eq 'Y') {
    Start-Process "http://localhost:9000"
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor $BLUE
Write-Host "📚 Next Steps" -ForegroundColor $GREEN
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor $BLUE
Write-Host ""
Write-Host "   1. Login to SonarQube at http://localhost:9000"
Write-Host "   2. Change admin password:"
Write-Host "      → User Menu (top right) → My Account → Security → Change Password"
Write-Host ""
Write-Host "   3. Generate an API token:"
Write-Host "      → User Menu → My Account → Security → Generate Tokens"
Write-Host "      → Name: 'GitHub Actions'"
Write-Host "      → Type: 'Global Analysis Token' or 'User Token'"
Write-Host "      → Click 'Generate' → Copy the token"
Write-Host ""
Write-Host "   4. Add GitHub Secrets:"
Write-Host "      → Repository Settings → Secrets and variables → Actions"
Write-Host "      → Add: SONAR_HOST_URL = http://localhost:9000"
Write-Host "      → Add: SONAR_TOKEN = <your-token-from-step-3>"
Write-Host ""
Write-Host "   5. (Optional) Add Snyk token for dependency scanning:"
Write-Host "      → Sign up at https://snyk.io (free tier available)"
Write-Host "      → Get API token from Account Settings"
Write-Host "      → Add: SNYK_TOKEN = <your-snyk-token>"
Write-Host ""
Write-Host "   6. Run your first analysis:"
Write-Host "      → Push code to trigger GitHub Actions workflow"
Write-Host "      → OR run locally:"
Write-Host "        cd backend"
Write-Host "        .\mvnw clean verify sonar:sonar \"
Write-Host "          -Dsonar.host.url=http://localhost:9000 \"
Write-Host "          -Dsonar.token=<your-token>"
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor $BLUE
Write-Host "🛠️  Useful Commands" -ForegroundColor $GREEN
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor $BLUE
Write-Host ""
Write-Host "   View logs:" -ForegroundColor $YELLOW
Write-Host "   " -NoNewline
Write-Host "docker-compose -f docker-compose.sonarqube.yml logs -f" -ForegroundColor $BLUE
Write-Host ""
Write-Host "   Stop SonarQube:" -ForegroundColor $YELLOW
Write-Host "   " -NoNewline
Write-Host "docker-compose -f docker-compose.sonarqube.yml down" -ForegroundColor $BLUE
Write-Host ""
Write-Host "   Restart SonarQube:" -ForegroundColor $YELLOW
Write-Host "   " -NoNewline
Write-Host "docker-compose -f docker-compose.sonarqube.yml restart" -ForegroundColor $BLUE
Write-Host ""
Write-Host "   Remove all data (reset):" -ForegroundColor $YELLOW
Write-Host "   " -NoNewline
Write-Host "docker-compose -f docker-compose.sonarqube.yml down -v" -ForegroundColor $BLUE
Write-Host ""
Write-Host "   Check SonarQube status:" -ForegroundColor $YELLOW
Write-Host "   " -NoNewline
Write-Host "Invoke-RestMethod http://localhost:9000/api/system/status" -ForegroundColor $BLUE
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor $BLUE
Write-Host ""
Write-Host "✅ Setup complete! SonarQube is ready for code analysis." -ForegroundColor $GREEN
Write-Host ""
