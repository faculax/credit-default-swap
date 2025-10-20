#!/usr/bin/env pwsh
# verify-sonarcloud-setup.ps1
# Verifies SonarCloud project setup and configuration

Write-Host "🔍 SonarCloud Setup Verification" -ForegroundColor Cyan
Write-Host "================================`n" -ForegroundColor Cyan

# Check if SONAR_TOKEN is available (from environment or GitHub Actions)
$sonarToken = $env:SONAR_TOKEN
if (-not $sonarToken) {
    Write-Host "⚠️  SONAR_TOKEN not found in environment" -ForegroundColor Yellow
    Write-Host "   This is expected when running locally." -ForegroundColor Gray
    Write-Host "   The token is stored in GitHub Secrets for Actions.`n" -ForegroundColor Gray
} else {
    Write-Host "✅ SONAR_TOKEN found in environment`n" -ForegroundColor Green
}

# Expected projects
$organization = "ayodeleoladeji"
$projects = @(
    "ayodeleoladeji_credit-default-swap-backend",
    "ayodeleoladeji_credit-default-swap-gateway",
    "ayodeleoladeji_credit-default-swap-risk-engine",
    "ayodeleoladeji_credit-default-swap-frontend"
)

Write-Host "📊 Expected SonarCloud Projects:" -ForegroundColor Cyan
Write-Host "Organization: $organization`n" -ForegroundColor White

foreach ($project in $projects) {
    $projectUrl = "https://sonarcloud.io/project/overview?id=$project"
    Write-Host "  • $project" -ForegroundColor White
    Write-Host "    URL: $projectUrl" -ForegroundColor Gray
}

Write-Host "`n🔗 Quick Links:" -ForegroundColor Cyan
Write-Host "  • Organization: https://sonarcloud.io/organizations/$organization/projects" -ForegroundColor White
Write-Host "  • Account Settings: https://sonarcloud.io/account/security" -ForegroundColor White

Write-Host "`n📋 Setup Checklist:" -ForegroundColor Cyan
Write-Host "  [ ] Login to SonarCloud (https://sonarcloud.io)" -ForegroundColor Yellow
Write-Host "  [ ] Create organization: $organization" -ForegroundColor Yellow
Write-Host "  [ ] Create 4 projects (see SONARCLOUD_PROJECT_SETUP.md)" -ForegroundColor Yellow
Write-Host "  [ ] Set main branch for each project (main or security-compliance)" -ForegroundColor Yellow
Write-Host "  [ ] Generate SONAR_TOKEN in SonarCloud" -ForegroundColor Yellow
Write-Host "  [ ] Add SONAR_TOKEN to GitHub Secrets" -ForegroundColor Yellow
Write-Host "  [ ] Trigger workflow: git push or manual run" -ForegroundColor Yellow
Write-Host "  [ ] Verify data appears on SonarCloud dashboard" -ForegroundColor Yellow

# Check if gh CLI is available for checking GitHub secrets
if (Get-Command gh -ErrorAction SilentlyContinue) {
    Write-Host "`n🔐 Checking GitHub Secrets..." -ForegroundColor Cyan
    
    try {
        $secrets = gh secret list 2>&1
        if ($secrets -match "SONAR_TOKEN") {
            Write-Host "✅ SONAR_TOKEN secret exists in GitHub repository" -ForegroundColor Green
        } else {
            Write-Host "❌ SONAR_TOKEN secret NOT found in GitHub repository" -ForegroundColor Red
            Write-Host "   Add it at: Settings → Secrets and variables → Actions" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "⚠️  Unable to check GitHub secrets (may need authentication)" -ForegroundColor Yellow
    }
} else {
    Write-Host "`n💡 Tip: Install GitHub CLI (gh) to auto-check secrets" -ForegroundColor Gray
    Write-Host "   Visit: https://cli.github.com/" -ForegroundColor Gray
}

# Check workflow file exists
$workflowFile = ".github/workflows/security-sonarcloud.yml"
if (Test-Path $workflowFile) {
    Write-Host "`n✅ Workflow file exists: $workflowFile" -ForegroundColor Green
    
    # Validate project keys in workflow match expected
    $workflowContent = Get-Content $workflowFile -Raw
    
    Write-Host "`n🔍 Validating project keys in workflow..." -ForegroundColor Cyan
    $allKeysFound = $true
    
    foreach ($project in $projects) {
        if ($workflowContent -match $project) {
            Write-Host "  ✅ $project" -ForegroundColor Green
        } else {
            Write-Host "  ❌ $project - NOT FOUND IN WORKFLOW!" -ForegroundColor Red
            $allKeysFound = $false
        }
    }
    
    if ($allKeysFound) {
        Write-Host "`n✅ All project keys correctly configured in workflow" -ForegroundColor Green
    } else {
        Write-Host "`n❌ Some project keys missing from workflow file!" -ForegroundColor Red
    }
    
} else {
    Write-Host "`n❌ Workflow file not found: $workflowFile" -ForegroundColor Red
}

# Check for sonar-project.properties files
Write-Host "`n📁 Checking for sonar-project.properties files..." -ForegroundColor Cyan
$services = @("backend", "gateway", "risk-engine", "frontend")
foreach ($service in $services) {
    $sonarPropsFile = "$service/sonar-project.properties"
    if (Test-Path $sonarPropsFile) {
        Write-Host "  ✅ $sonarPropsFile exists" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  $sonarPropsFile not found (optional)" -ForegroundColor Yellow
    }
}

Write-Host "`n📚 Documentation:" -ForegroundColor Cyan
Write-Host "  • Full setup guide: SONARCLOUD_PROJECT_SETUP.md" -ForegroundColor White
Write-Host "  • SonarCloud Docs: https://docs.sonarcloud.io/" -ForegroundColor White

Write-Host "`n🎯 Next Steps:" -ForegroundColor Cyan
Write-Host "  1. Follow SONARCLOUD_PROJECT_SETUP.md to create projects" -ForegroundColor White
Write-Host "  2. Run: git commit --allow-empty -m `"chore: trigger SonarCloud`"" -ForegroundColor White
Write-Host "  3. Run: git push" -ForegroundColor White
Write-Host "  4. Check: https://sonarcloud.io/organizations/$organization/projects" -ForegroundColor White

Write-Host "`n✨ Setup verification complete!" -ForegroundColor Green
