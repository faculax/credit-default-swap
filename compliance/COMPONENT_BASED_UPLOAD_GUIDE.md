# 🧩 Component-Based DefectDojo Upload Guide

## Overview

The component-based upload approach creates **separate products** in DefectDojo for each service/component in your architecture. This provides:

✅ **Better organization** - Each component has its own product page  
✅ **Clearer tracking** - See security trends per component over time  
✅ **Focused reporting** - Generate component-specific security reports  
✅ **Team alignment** - Assign different teams to different components  

---

## 📦 Components in Credit Default Swap Platform

The script automatically organizes scans into these components:

### 1. **Backend API** 
   - **Description**: Java REST API backend service with Spring Boot
   - **Tag**: `backend-api`
   - **Scans**:
     - SpotBugs Static Analysis
     - OWASP Dependency Check
     - PMD Code Analysis
     - Checkstyle Quality Check
   - **Files**:
     - `backend/target/spotbugsXml.xml`
     - `backend/target/dependency-check-report.json`
     - `backend/target/security-reports/pmd.xml`
     - `backend/target/checkstyle-result.xml`

### 2. **Web Frontend**
   - **Description**: React/JavaScript web application
   - **Tag**: `frontend-web`
   - **Scans**:
     - npm audit - Dependency Vulnerabilities
     - ESLint - Code Security Issues
     - Retire.js - Vulnerable Libraries
   - **Files**:
     - `frontend/audit-npm.json`
     - `frontend/eslint-security.json`
     - `frontend/retire-report.json`

### 3. **API Gateway**
   - **Description**: Spring Cloud Gateway - API routing and security
   - **Tag**: `gateway`
   - **Scans**:
     - SpotBugs Static Analysis
     - OWASP Dependency Check
   - **Files**:
     - `gateway/target/spotbugsXml.xml`
     - `gateway/target/dependency-check-report.json`

### 4. **Risk Engine**
   - **Description**: Credit risk calculation and analysis service
   - **Tag**: `risk-engine`
   - **Scans**:
     - SpotBugs Static Analysis
     - OWASP Dependency Check
   - **Files**:
     - `risk-engine/target/spotbugsXml.xml`
     - `risk-engine/target/dependency-check-report.json`

---

## 🚀 Quick Start

### Option 1: Using the Component-Based Script

```powershell
# Run security scans (if not already done)
.\defectdojo.ps1 scan

# Upload with component-based organization
.\defectdojo-component.ps1
```

### Option 2: Using the Standard Script with Component Mode

```powershell
# Upload with component-based grouping (separate engagements per component)
.\compliance\scripts\upload-to-defectdojo.ps1 -GroupByComponent
```

---

## 📋 Script Comparison

| Feature | `defectdojo.ps1 upload` | `defectdojo-component.ps1` |
|---------|------------------------|----------------------------|
| **Organization** | Single product, tags | Separate products per component |
| **Products Created** | 1 | 4+ (one per component) |
| **Engagements** | 1 per run | 1 per component per run |
| **Best For** | Quick unified view | Detailed component tracking |
| **Reporting** | Filter by tags | Native product reports |
| **Team Assignment** | Shared | Per-component |

---

## 🎯 When to Use Each Approach

### Use **Standard Upload** (`defectdojo.ps1 upload`) When:
- You want a unified view of all security findings
- You're doing quick iterative development
- Your team manages all components together
- You need fast uploads with minimal organization

### Use **Component-Based Upload** (`defectdojo-component.ps1`) When:
- You have separate teams managing different components
- You need detailed per-component security metrics
- You want to track component-specific security trends over time
- You're preparing reports for stakeholders by component
- You have different security SLAs per component

---

## 🔧 Customization

### Adding a New Component

Edit `defectdojo-component.ps1` and add a new section:

```powershell
# Component 5: Authentication Service
$productId = Get-OrCreateProduct `
    -ProductName "$BaseProductName - Auth Service" `
    -Description "Authentication and authorization service" `
    -ComponentTag "auth-service" `
    -Headers $headers

if ($null -ne $productId) {
    $script:productUrls += @{
        Name = "Auth Service"
        Url = "$DefectDojoUrl/product/$productId"
    }
    
    $engagementId = New-Engagement `
        -ProductId $productId `
        -EngagementName "Auth Service Scan - $(Get-Date -Format 'yyyy-MM-dd HH:mm')" `
        -Description "Automated security scans for authentication service" `
        -Headers $headers
    
    if ($null -ne $engagementId) {
        Invoke-ScanUpload `
            -EngagementId $engagementId `
            -ScanType "SpotBugs Scan" `
            -FilePath (Join-Path $ProjectRoot "auth-service\target\spotbugsXml.xml") `
            -ScanDisplayName "SpotBugs Static Analysis" `
            -Headers $headers | Out-Null
    }
}
```

### Changing the Base Product Name

```powershell
.\defectdojo-component.ps1 -BaseProductName "MyApp"
```

This will create products like:
- MyApp - Backend API
- MyApp - Web Frontend
- MyApp - API Gateway
- MyApp - Risk Engine

---

## 📊 Viewing Results in DefectDojo

### Per-Component View

After running the component upload, you'll see separate products:

1. **Navigate to**: http://localhost:8081/product
2. **You'll see**:
   - CDS Platform - Backend API
   - CDS Platform - Web Frontend
   - CDS Platform - API Gateway
   - CDS Platform - Risk Engine

3. **Click any product** to see:
   - Security metrics specific to that component
   - Findings history over time
   - Engagement details
   - Component-specific reports

### Unified Dashboard View

To see all components together:

1. Navigate to: http://localhost:8081
2. Use the main dashboard
3. Filter by tag: `backend-api`, `frontend-web`, `gateway`, `risk-engine`

---

## 🔄 Workflow Integration

### Daily Development Workflow

```powershell
# 1. Make code changes
# 2. Run scans
.\defectdojo.ps1 scan

# 3. Upload to DefectDojo (choose one)
.\defectdojo-component.ps1              # Component-based
# OR
.\defectdojo.ps1 upload                 # Standard unified
```

### CI/CD Pipeline Integration

```yaml
# Example GitHub Actions workflow
- name: Run Security Scans
  run: ./defectdojo.ps1 scan

- name: Upload to DefectDojo
  run: |
    ./defectdojo-component.ps1 `
      -DefectDojoUrl ${{ secrets.DEFECTDOJO_URL }} `
      -Username ${{ secrets.DEFECTDOJO_USER }} `
      -Password ${{ secrets.DEFECTDOJO_PASSWORD }}
```

---

## 📈 Reporting Benefits

### Component-Based Reporting Advantages

1. **Executive Reports**: Show security posture per service
2. **Team Metrics**: Track each team's security performance
3. **Trend Analysis**: Identify which components need attention
4. **SLA Tracking**: Different security requirements per component
5. **Budget Allocation**: Justify security investment per component

### Example Reports You Can Generate

- "Backend API Security Trends - Last 6 Months"
- "Frontend Vulnerabilities by Severity"
- "Gateway OWASP Top 10 Coverage"
- "Risk Engine Dependency Vulnerabilities"

---

## 🛠️ Troubleshooting

### Issue: Products Not Created

**Solution**: Ensure DefectDojo is running
```powershell
.\defectdojo.ps1 status
```

### Issue: Scan Files Not Found

**Solution**: Run scans first
```powershell
.\defectdojo.ps1 scan
```

### Issue: Authentication Failed

**Solution**: Check DefectDojo credentials
```powershell
# Default credentials
Username: admin
Password: admin
URL: http://localhost:8081
```

### Issue: Upload Fails for Specific Component

**Solution**: Check if scan file exists
```powershell
# Check if backend scans exist
Test-Path backend\target\spotbugsXml.xml
Test-Path backend\target\dependency-check-report.json

# Check if frontend scans exist
Test-Path frontend\audit-npm.json
Test-Path frontend\eslint-security.json
```

---

## 🔐 Security Considerations

### Credentials Management

For production use, avoid hardcoding credentials:

```powershell
# Use secure credentials
$securePassword = Read-Host "Password" -AsSecureString
$credentials = New-Object System.Management.Automation.PSCredential("admin", $securePassword)

.\defectdojo-component.ps1 `
    -Username $credentials.UserName `
    -Password $credentials.GetNetworkCredential().Password
```

Or use environment variables:

```powershell
$env:DD_USER = "admin"
$env:DD_PASSWORD = "your-secure-password"

.\defectdojo-component.ps1 `
    -Username $env:DD_USER `
    -Password $env:DD_PASSWORD
```

---

## 📚 Additional Resources

- **DefectDojo API Documentation**: http://localhost:8081/api/v2/doc/
- **Standard Upload Guide**: See `upload-to-defectdojo.ps1`
- **Security Setup**: See `SECURITY_SETUP.md`
- **Quick Start**: See `QUICK_START.md`

---

## 💡 Tips & Best Practices

1. **Consistent Naming**: Keep component names consistent across runs
2. **Tag Strategy**: Use meaningful tags for filtering
3. **Regular Scans**: Run component scans on every commit
4. **Review Findings**: Check component findings in daily standups
5. **Archive Old Engagements**: Keep DefectDojo clean by archiving old scans
6. **Component Owners**: Assign product owners in DefectDojo settings
7. **Notifications**: Set up DefectDojo notifications per component

---

## 🎯 Next Steps

1. Run your first component-based upload:
   ```powershell
   .\defectdojo.ps1 scan
   .\defectdojo-component.ps1
   ```

2. Explore the DefectDojo UI:
   - View each component's product page
   - Compare findings across components
   - Generate component-specific reports

3. Customize for your needs:
   - Add new components
   - Modify component descriptions
   - Add additional scan types

4. Integrate with CI/CD:
   - Add to GitHub Actions
   - Configure automated notifications
   - Set up quality gates per component

---

**Questions or issues?** Check the main README.md or the AGENTS.md guide.
