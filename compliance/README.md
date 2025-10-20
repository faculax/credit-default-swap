# 🛡️ DefectDojo Security Compliance Integration

Complete DefectDojo integration for automated security vulnerability management and compliance tracking.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [Security Scanners](#security-scanners)
- [Workflow](#workflow)
- [Troubleshooting](#troubleshooting)
- [Advanced Configuration](#advanced-configuration)

---

## 🎯 Overview

This compliance folder contains a **fully automated DefectDojo setup** that integrates with your Spring Boot backend to:

✅ **Scan code** for vulnerabilities using industry-standard tools  
✅ **Upload findings** automatically to DefectDojo  
✅ **Track remediation** through DefectDojo's workflow  
✅ **Generate reports** for compliance and audits  
✅ **CI/CD ready** - integrate into GitHub Actions, Jenkins, etc.

---

## 🚀 Quick Start

### 1. Start DefectDojo

```powershell
./defectdojo.ps1 start
```

Wait ~60 seconds for all services to initialize, then access:
- **URL**: http://localhost:8081
- **Username**: `admin`
- **Password**: `admin`

### 2. Initialize Parsers (First Time Only)

```powershell
./defectdojo.ps1 init
```

This creates required test types in DefectDojo:
- Dependency Check Scan
- SpotBugs Scan
- PMD Scan
- Generic Findings Import

### 3. Run Security Scans

```powershell
./defectdojo.ps1 scan
```

This executes:
- ✅ **SpotBugs** with FindSecBugs - Security vulnerability detection
- ✅ **Checkstyle** - Code quality checks
- ✅ **PMD** with CPD - Code analysis and copy-paste detection
- ⚠️ **OWASP Dependency Check** - Requires NVD API key (see [Advanced Configuration](#owasp-dependency-check-setup))

### 4. Upload to DefectDojo

```powershell
./defectdojo.ps1 upload
```

Or with detailed output:

```powershell
./defectdojo.ps1 upload -Verbose
```

### 5. View Results

Open http://localhost:8081/engagement/1 (engagement ID varies)

---

## 🏗️ Architecture

```
compliance/
├── docker-compose.defectdojo.yml      # DefectDojo multi-container setup
├── scripts/
│   ├── upload-to-defectdojo.ps1       # Upload security reports to DefectDojo
│   └── init-defectdojo-parsers.ps1    # Initialize custom test types
├── docs/
│   ├── QUICKSTART.md                  # Quick reference guide
│   ├── TROUBLESHOOTING.md             # Common issues & solutions
│   └── API_REFERENCE.md               # DefectDojo API documentation
└── README.md                          # This file

root/
├── defectdojo.ps1                     # Main CLI tool (10 commands)
└── backend/
    ├── pom.xml                        # Maven with security scanner plugins
    └── target/security-reports/       # Generated scan results
        ├── spotbugs.xml               # ✅ Working
        ├── checkstyle-result.xml      # ⚠️ Import issues
        ├── pmd.xml                    # ⚠️ Import issues
        └── dependency-check-report.json  # ❌ Needs NVD API key
```

---

## 🔍 Security Scanners

### ✅ SpotBugs (WORKING)

**Purpose**: Static analysis for Java bug patterns and security vulnerabilities

**Plugins**:
- `spotbugs-maven-plugin:4.8.3.1`
- `findsecbugs-plugin:1.13.0` (security rules)

**Configuration** (pom.xml):
```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.8.3.1</version>
    <configuration>
        <effort>Max</effort>
        <threshold>Low</threshold>
        <xmlOutput>true</xmlOutput>
        <xmlOutputDirectory>${project.build.directory}/security-reports</xmlOutputDirectory>
        <outputFile>spotbugs.xml</outputFile>
        <includeFilterFile>${project.basedir}/spotbugs-security-include.xml</includeFilterFile>
        <plugins>
            <plugin>
                <groupId>com.h3xstream.findsecbugs</groupId>
                <artifactId>findsecbugs-plugin</artifactId>
                <version>1.13.0</version>
            </plugin>
        </plugins>
    </configuration>
</plugin>
```

**Detects**:
- SQL Injection (CWE-89)
- Path Traversal (CWE-22)
- CRLF Injection (CWE-117)
- Weak Cryptography (CWE-327)
- Insecure Random (CWE-330)
- XSS, XXE, Command Injection, etc.

**Status**: ✅ **Successfully uploading to DefectDojo**

---

### ⚠️ Checkstyle (IMPORT ISSUES)

**Purpose**: Code quality and style violations

**Configuration** (pom.xml):
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.0</version>
    <configuration>
        <configLocation>checkstyle.xml</configLocation>
        <outputFile>${project.build.directory}/security-reports/checkstyle-result.xml</outputFile>
    </configuration>
</plugin>
```

**Status**: ⚠️ **400 Bad Request when uploading via Generic Findings Import**

**Known Issue**: DefectDojo's Generic Findings Import parser may not fully support Checkstyle XML format.

**Workaround**: Manual review or use Checkstyle's JSON report format (requires plugin update).

---

### ⚠️ PMD (IMPORT ISSUES)

**Purpose**: Code quality, potential bugs, dead code, complexity

**Configuration** (pom.xml):
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-pmd-plugin</artifactId>
    <version>3.21.0</version>
    <configuration>
        <rulesets>
            <ruleset>/rulesets/java/quickstart.xml</ruleset>
        </rulesets>
        <targetDirectory>${project.build.directory}/security-reports</targetDirectory>
    </configuration>
</plugin>
```

**Status**: ⚠️ **500 Internal Server Error when uploading**

**Known Issue**: DefectDojo PMD parser may have compatibility issues with PMD 6.55.0 XML format.

**Next Steps**:
1. Check DefectDojo logs: `./defectdojo.ps1 logs uwsgi`
2. Test with sample PMD report
3. Consider custom parser or SARIF conversion

---

### ❌ OWASP Dependency Check (NEEDS SETUP)

**Purpose**: Identify known vulnerabilities (CVEs) in project dependencies

**Configuration** (pom.xml):
```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.9</version>
    <configuration>
        <format>ALL</format>
        <outputDirectory>${project.build.directory}/security-reports</outputDirectory>
        <failBuildOnCVSS>8</failBuildOnCVSS>
        <suppressionFile>${project.basedir}/owasp-suppressions.xml</suppressionFile>
    </configuration>
</plugin>
```

**Status**: ❌ **403/404 Error - NVD API Key Required**

**Error**:
```
Error updating the NVD Data; the NVD returned a 403 or 404 error
Consider using an NVD API Key
```

**Setup Required**: See [OWASP Dependency Check Setup](#owasp-dependency-check-setup) below.

---

## 🔄 Workflow

### Automated CI/CD Integration

```yaml
# .github/workflows/security-scan.yml
name: Security Scan

on: [push, pull_request]

jobs:
  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          
      - name: Run Security Scans
        run: |
          cd backend
          ./mvnw spotbugs:spotbugs checkstyle:checkstyle pmd:pmd
          
      - name: Start DefectDojo
        run: docker-compose -f compliance/docker-compose.defectdojo.yml up -d
        
      - name: Wait for DefectDojo
        run: sleep 60
        
      - name: Upload to DefectDojo
        run: ./compliance/scripts/upload-to-defectdojo.ps1
```

### Manual Workflow

```powershell
# 1. Start DefectDojo (if not running)
./defectdojo.ps1 start

# 2. Check status
./defectdojo.ps1 status

# 3. Run scans
./defectdojo.ps1 scan

# 4. Upload results
./defectdojo.ps1 upload -Verbose

# 5. View in browser
./defectdojo.ps1 ui

# 6. Stop when done
./defectdojo.ps1 stop
```

---

## 🐛 Troubleshooting

### DefectDojo Won't Start

**Symptom**: `docker-compose up` fails or containers exit

**Solution**:
```powershell
# Check Docker is running
docker ps

# View logs
./defectdojo.ps1 logs

# Full restart
./defectdojo.ps1 restart
```

### Database Not Initialized

**Symptom**: "No migrations pending" or blank UI

**Solution**:
```powershell
# Manual initialization
docker exec -it compliance-defectdojo-uwsgi-1 python manage.py migrate --noinput
docker exec -it compliance-defectdojo-uwsgi-1 python manage.py createsuperuser --noinput --username=admin --email=admin@example.com
docker exec -it compliance-defectdojo-uwsgi-1 python manage.py set_password admin --password=admin
```

### Upload Fails with Authentication Error

**Symptom**: "401 Unauthorized"

**Solution**:
1. Verify credentials in `defectdojo.ps1` (default: admin/admin)
2. Reset password:
```powershell
docker exec -it compliance-defectdojo-uwsgi-1 python manage.py set_password admin --password=admin
```

### Test Type Not Found

**Symptom**: "Scan type 'X' does not exist"

**Solution**:
```powershell
# Re-run parser initialization
./defectdojo.ps1 init
```

### PMD/Checkstyle Upload Fails

**Status**: Known issue - DefectDojo parser compatibility

**Temporary Workaround**:
- SpotBugs is the primary security scanner (working ✅)
- PMD/Checkstyle can be reviewed manually from `backend/target/security-reports/`

**Permanent Fix** (TODO):
1. Convert to SARIF format
2. Create custom DefectDojo parser
3. Use alternative upload tools (DefectDojo CLI, API direct)

---

## ⚙️ Advanced Configuration

### OWASP Dependency Check Setup

#### 1. Get NVD API Key (Free)

1. Visit https://nvd.nist.gov/developers/request-an-api-key
2. Submit request form
3. Check email for API key (usually instant)

#### 2. Configure Maven

Add to `backend/pom.xml`:

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.9</version>
    <configuration>
        <format>ALL</format>
        <outputDirectory>${project.build.directory}/security-reports</outputDirectory>
        <failBuildOnCVSS>8</failBuildOnCVSS>
        <nvdApiKey>${env.NVD_API_KEY}</nvdApiKey>  <!-- Add this line -->
    </configuration>
</plugin>
```

#### 3. Set Environment Variable

```powershell
# Windows PowerShell
$env:NVD_API_KEY="your-api-key-here"

# Add to profile for persistence
Add-Content $PROFILE '$env:NVD_API_KEY="your-api-key-here"'
```

#### 4. Test

```powershell
cd backend
./mvnw dependency-check:check
```

Expected output:
```
[INFO] Checking for updates
[INFO] Download Started for NVD CVE - 2024
[INFO] Download Complete for NVD CVE - 2024
```

#### 5. Create Suppressions File

Create `backend/owasp-suppressions.xml` for false positives:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
    <!-- Example: Suppress false positive -->
    <suppress>
        <notes>False positive - not using vulnerable component</notes>
        <packageUrl regex="true">^pkg:maven/org\.springframework\.boot/spring\-boot\-starter\-web@.*$</packageUrl>
        <cve>CVE-2024-12345</cve>
    </suppress>
</suppressions>
```

---

### Custom DefectDojo Configuration

#### Change Default Credentials

Edit `compliance/docker-compose.defectdojo.yml`:

```yaml
services:
  uwsgi:
    environment:
      - DD_ADMIN_USER=your_username
      - DD_ADMIN_PASSWORD=your_password
```

Then restart:

```powershell
./defectdojo.ps1 restart
```

#### Enable HTTPS

1. Generate SSL certificate
2. Update nginx service in `docker-compose.defectdojo.yml`
3. Mount certificate volumes
4. Update `defectdojo.ps1` URL to `https://`

---

## 📊 DefectDojo Features

### Product Management

- **Products**: Top-level entities (e.g., "Credit Default Swap Platform")
- **Engagements**: Time-boxed security assessments
- **Tests**: Individual scan results within an engagement
- **Findings**: Vulnerabilities discovered

### Workflow States

```
Active → Verified → False Positive → Mitigated → Risk Accepted → Closed
```

### Reporting

1. Navigate to **Products** → Your Product
2. Click **Metrics** → **Report Builder**
3. Select report type (PDF, HTML, JSON)
4. Download

### API Access

```powershell
# Get API token
$token = (Invoke-RestMethod -Uri "http://localhost:8081/api/v2/api-token-auth/" `
    -Method Post -ContentType "application/json" `
    -Body '{"username":"admin","password":"admin"}').token

# List all findings
Invoke-RestMethod -Uri "http://localhost:8081/api/v2/findings/" `
    -Headers @{Authorization="Token $token"}
```

---

## 📚 References

- **DefectDojo Docs**: https://defectdojo.github.io/django-DefectDojo/
- **SpotBugs**: https://spotbugs.github.io/
- **FindSecBugs**: https://find-sec-bugs.github.io/
- **OWASP Dependency Check**: https://jeremylong.github.io/DependencyCheck/
- **PMD**: https://pmd.github.io/
- **Checkstyle**: https://checkstyle.org/

---

## 🎉 Summary

### ✅ What's Working

- DefectDojo running on http://localhost:8081
- SpotBugs security scans (**25+ security rules**)
- Automated upload via PowerShell script
- Product/Engagement creation
- Test type initialization
- CLI tool with 10 commands

### ⚠️ Known Issues

- **Checkstyle**: 400 Bad Request (Generic Findings Import compatibility)
- **PMD**: 500 Internal Server Error (parser compatibility)
- **OWASP Dependency Check**: Needs NVD API key

### 🚀 Next Steps

1. **Get NVD API Key** → Enable OWASP Dependency Check
2. **Investigate PMD/Checkstyle** → Check DefectDojo logs, test parsers
3. **CI/CD Integration** → Add to GitHub Actions
4. **Custom Dashboards** → Configure DefectDojo metrics
5. **Security Policies** → Define SLAs for vulnerability remediation

---

## 🤝 Contributing

Found a bug? Have a suggestion? 

1. Check DefectDojo logs: `./defectdojo.ps1 logs`
2. Review troubleshooting section
3. Document findings
4. Update this README

---

**Last Updated**: 2025-10-20  
**DefectDojo Version**: 2.39.1  
**Status**: ✅ Operational (SpotBugs working, OWASP needs API key, PMD/Checkstyle need investigation)
