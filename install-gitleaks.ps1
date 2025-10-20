#!/usr/bin/env pwsh
# ===============================================================================
# Gitleaks Installation Script for Windows
# ===============================================================================
# Installs Gitleaks secret scanner for detecting hardcoded credentials
# ===============================================================================

param(
    [string]$Version = "8.18.4",  # Latest stable version
    [string]$InstallPath = "C:\tools\gitleaks",
    [switch]$AddToPath = $true
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "  Gitleaks Installation" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# Check if already installed
$existingGitleaks = Get-Command gitleaks -ErrorAction SilentlyContinue
if ($existingGitleaks) {
    Write-Host "[OK] Gitleaks is already installed!" -ForegroundColor Green
    Write-Host "     Location: $($existingGitleaks.Source)" -ForegroundColor Gray
    
    try {
        $currentVersion = & gitleaks version 2>&1
        Write-Host "     Version: $currentVersion" -ForegroundColor Gray
    } catch {}
    
    Write-Host ""
    $response = Read-Host "Reinstall anyway? (y/N)"
    if ($response -ne 'y' -and $response -ne 'Y') {
        Write-Host "Installation cancelled." -ForegroundColor Yellow
        exit 0
    }
}

Write-Host "[>] Installing Gitleaks v$Version..." -ForegroundColor Blue
Write-Host ""

# Create installation directory
Write-Host "[>] Creating installation directory: $InstallPath" -ForegroundColor Blue
New-Item -ItemType Directory -Force -Path $InstallPath | Out-Null
Write-Host "[+] Directory created" -ForegroundColor Green

# Download Gitleaks
$downloadUrl = "https://github.com/gitleaks/gitleaks/releases/download/v$Version/gitleaks_${Version}_windows_x64.zip"
$zipFile = Join-Path $env:TEMP "gitleaks.zip"

Write-Host ""
Write-Host "[>] Downloading Gitleaks from GitHub..." -ForegroundColor Blue
Write-Host "    URL: $downloadUrl" -ForegroundColor Gray

try {
    Invoke-WebRequest -Uri $downloadUrl -OutFile $zipFile -UseBasicParsing
    Write-Host "[+] Download complete" -ForegroundColor Green
} catch {
    Write-Host "[X] Failed to download Gitleaks" -ForegroundColor Red
    Write-Host "    Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Alternative installation methods:" -ForegroundColor Yellow
    Write-Host "  1. Using Chocolatey: choco install gitleaks" -ForegroundColor White
    Write-Host "  2. Using Scoop: scoop install gitleaks" -ForegroundColor White
    Write-Host "  3. Manual download: https://github.com/gitleaks/gitleaks/releases" -ForegroundColor White
    exit 1
}

# Extract
Write-Host ""
Write-Host "[>] Extracting files..." -ForegroundColor Blue
try {
    Expand-Archive -Path $zipFile -DestinationPath $InstallPath -Force
    Write-Host "[+] Extraction complete" -ForegroundColor Green
} catch {
    Write-Host "[X] Failed to extract files" -ForegroundColor Red
    Write-Host "    Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Clean up
Remove-Item $zipFile -Force
Write-Host "[+] Cleaned up temporary files" -ForegroundColor Green

# Add to PATH
if ($AddToPath) {
    Write-Host ""
    Write-Host "[>] Adding to PATH..." -ForegroundColor Blue
    
    $currentPath = [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::User)
    
    if ($currentPath -notlike "*$InstallPath*") {
        $newPath = "$currentPath;$InstallPath"
        [Environment]::SetEnvironmentVariable("Path", $newPath, [EnvironmentVariableTarget]::User)
        $env:Path += ";$InstallPath"
        Write-Host "[+] Added to PATH" -ForegroundColor Green
        Write-Host "    Note: Restart terminal for PATH changes to take effect" -ForegroundColor Yellow
    } else {
        Write-Host "[OK] Already in PATH" -ForegroundColor Green
    }
}

# Verify installation
Write-Host ""
Write-Host "[>] Verifying installation..." -ForegroundColor Blue

$gitleaksExe = Join-Path $InstallPath "gitleaks.exe"
if (Test-Path $gitleaksExe) {
    Write-Host "[+] Gitleaks installed successfully!" -ForegroundColor Green
    Write-Host "    Location: $gitleaksExe" -ForegroundColor Gray
    
    try {
        $versionOutput = & $gitleaksExe version 2>&1
        Write-Host "    Version: $versionOutput" -ForegroundColor Gray
    } catch {
        Write-Host "    (Could not verify version)" -ForegroundColor Yellow
    }
} else {
    Write-Host "[X] Installation failed - executable not found" -ForegroundColor Red
    exit 1
}

# Summary
Write-Host ""
Write-Host "===============================================================" -ForegroundColor Green
Write-Host "  Installation Complete!" -ForegroundColor Green
Write-Host "===============================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Quick Start:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  # Test installation (restart terminal first if PATH was updated)" -ForegroundColor White
Write-Host "  gitleaks version" -ForegroundColor Cyan
Write-Host ""
Write-Host "  # Scan current repository" -ForegroundColor White
Write-Host "  gitleaks detect --source . --verbose" -ForegroundColor Cyan
Write-Host ""
Write-Host "  # Run with config file" -ForegroundColor White
Write-Host "  gitleaks detect --source . --config .gitleaks.toml --verbose" -ForegroundColor Cyan
Write-Host ""
Write-Host "  # Generate report" -ForegroundColor White
Write-Host "  gitleaks detect --source . --report-format json --report-path gitleaks-report.json" -ForegroundColor Cyan
Write-Host ""
Write-Host "Documentation:" -ForegroundColor Yellow
Write-Host "  https://github.com/gitleaks/gitleaks" -ForegroundColor White
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Restart your terminal (if PATH was updated)" -ForegroundColor White
Write-Host "  2. Run: gitleaks detect --source . --verbose" -ForegroundColor White
Write-Host "  3. Check: SECRET_SCANNING_GUIDE.md for full guide" -ForegroundColor White
Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""
