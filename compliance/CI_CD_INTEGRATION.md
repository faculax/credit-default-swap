# 🔄 DefectDojo CI/CD Integration Guide

This guide shows how to integrate DefectDojo into your CI/CD pipelines for automated vulnerability scanning and reporting.

---

## 📋 Prerequisites

1. **DefectDojo Instance Running** (self-hosted or cloud)
2. **API Token** from DefectDojo
3. **Product and Engagement IDs** configured

---

## 🔐 Get API Token

### Via UI:
1. Log in to DefectDojo
2. Navigate to: **User Menu** → **API v2 Key**
3. Click **Generate API Key**
4. Copy the token

### Via API:
```bash
curl -X POST http://localhost:8081/api/v2/api-token-auth/ \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

---

## 🎬 GitHub Actions Integration

### Step 1: Add Secrets to GitHub

Go to: **Repository Settings** → **Secrets and variables** → **Actions**

Add these secrets:
- `DEFECTDOJO_URL` - Your DefectDojo URL (e.g., http://localhost:8081)
- `DEFECTDOJO_TOKEN` - Your API token
- `DEFECTDOJO_PRODUCT_NAME` - Product name in DefectDojo

### Step 2: Create Workflow File

Create `.github/workflows/defectdojo-scan.yml`:

```yaml
name: Security Scan & DefectDojo Upload

on:
  push:
    branches: [main, develop, security-*]
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 2 * * 1'  # Weekly on Monday at 2 AM

jobs:
  security-scan:
    name: Run Security Scans
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'
      
      - name: Create Reports Directory
        run: mkdir -p backend/target/security-reports
      
      - name: Run OWASP Dependency Check
        run: |
          cd backend
          mvn org.owasp:dependency-check-maven:check \
            -DdataDirectory=./dependency-check-data \
            -Dformat=JSON \
            -DprettyPrint=true
          cp target/dependency-check-report.json target/security-reports/
        continue-on-error: true
      
      - name: Run SpotBugs
        run: |
          cd backend
          mvn compile spotbugs:spotbugs \
            -Dspotbugs.xmlOutput=true \
            -Dspotbugs.xmlOutputDirectory=target/security-reports
        continue-on-error: true
      
      - name: Run Checkstyle
        run: |
          cd backend
          mvn checkstyle:checkstyle
          cp target/checkstyle-result.xml target/security-reports/ || true
        continue-on-error: true
      
      - name: Run PMD
        run: |
          cd backend
          mvn pmd:pmd pmd:cpd
          cp target/pmd.xml target/security-reports/ || true
        continue-on-error: true
      
      - name: Upload Scan Results to DefectDojo
        env:
          DD_URL: ${{ secrets.DEFECTDOJO_URL }}
          DD_TOKEN: ${{ secrets.DEFECTDOJO_TOKEN }}
          PRODUCT_NAME: ${{ secrets.DEFECTDOJO_PRODUCT_NAME }}
        run: |
          chmod +x compliance/scripts/upload-to-defectdojo-ci.sh
          ./compliance/scripts/upload-to-defectdojo-ci.sh
      
      - name: Archive Scan Results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: security-scan-results
          path: backend/target/security-reports/
          retention-days: 30
      
      - name: Comment on PR (if applicable)
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: '🛡️ Security scans completed and uploaded to DefectDojo. Review findings at ${{ secrets.DEFECTDOJO_URL }}'
            })
```

### Step 3: Create CI Upload Script

Create `compliance/scripts/upload-to-defectdojo-ci.sh`:

```bash
#!/bin/bash
# CI/CD-specific upload script using API token directly

set -e

DD_URL="${DD_URL:-http://localhost:8081}"
TOKEN="${DD_TOKEN}"
PRODUCT_NAME="${PRODUCT_NAME:-Credit Default Swap Platform}"
ENGAGEMENT_NAME="CI/CD Scan - ${GITHUB_REF##*/} - $(date '+%Y-%m-%d %H:%M')"
REPORTS_PATH="backend/target/security-reports"

if [ -z "$TOKEN" ]; then
    echo "❌ DD_TOKEN environment variable not set"
    exit 1
fi

echo "🔍 Uploading to DefectDojo: $DD_URL"

# Get or create product
encoded_name=$(echo "$PRODUCT_NAME" | sed 's/ /%20/g')
response=$(curl -s -X GET "$DD_URL/api/v2/products/?name=$encoded_name" \
    -H "Authorization: Token $TOKEN")

if echo "$response" | grep -q '"count":[1-9]'; then
    PRODUCT_ID=$(echo "$response" | jq -r '.results[0].id')
    echo "✓ Found product (ID: $PRODUCT_ID)"
else
    echo "Creating product..."
    response=$(curl -s -X POST "$DD_URL/api/v2/products/" \
        -H "Authorization: Token $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\":\"$PRODUCT_NAME\",
            \"description\":\"Automated CI/CD security scanning\",
            \"prod_type\":1
        }")
    PRODUCT_ID=$(echo "$response" | jq -r '.id')
    echo "✓ Created product (ID: $PRODUCT_ID)"
fi

# Create engagement
today=$(date '+%Y-%m-%d')
response=$(curl -s -X POST "$DD_URL/api/v2/engagements/" \
    -H "Authorization: Token $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"product\":$PRODUCT_ID,
        \"name\":\"$ENGAGEMENT_NAME\",
        \"target_start\":\"$today\",
        \"target_end\":\"$today\",
        \"status\":\"In Progress\",
        \"engagement_type\":\"CI/CD\",
        \"branch_tag\":\"${GITHUB_REF##*/}\",
        \"commit_hash\":\"${GITHUB_SHA}\"
    }")
ENGAGEMENT_ID=$(echo "$response" | jq -r '.id')
echo "✓ Created engagement (ID: $ENGAGEMENT_ID)"

# Upload scan results
scan_date=$(date '+%Y-%m-%d')
uploaded_count=0

upload_file() {
    local file_path=$1
    local scan_type=$2
    
    if [ -f "$file_path" ]; then
        echo "📤 Uploading $(basename $file_path)..."
        http_code=$(curl -s -w "%{http_code}" -o /dev/null -X POST "$DD_URL/api/v2/import-scan/" \
            -H "Authorization: Token $TOKEN" \
            -F "scan_type=$scan_type" \
            -F "file=@$file_path" \
            -F "engagement=$ENGAGEMENT_ID" \
            -F "verified=true" \
            -F "active=true" \
            -F "scan_date=$scan_date")
        
        if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
            echo "✓ Uploaded successfully"
            ((uploaded_count++))
        else
            echo "⚠ Upload failed (HTTP $http_code)"
        fi
    else
        echo "⚠ File not found: $file_path"
    fi
}

# Upload each scan type
upload_file "$REPORTS_PATH/dependency-check-report.json" "Dependency Check Scan"
upload_file "$REPORTS_PATH/spotbugsXml.xml" "SpotBugs Scan"
upload_file "$REPORTS_PATH/checkstyle-result.xml" "Checkstyle Scan"
upload_file "$REPORTS_PATH/pmd.xml" "PMD Scan"

echo ""
echo "✅ Uploaded $uploaded_count scan report(s)"
echo "📊 View results: $DD_URL/engagement/$ENGAGEMENT_ID"
```

Make it executable:
```bash
chmod +x compliance/scripts/upload-to-defectdojo-ci.sh
```

---

## 🔵 Jenkins Integration

### Jenkinsfile Example

```groovy
pipeline {
    agent any
    
    environment {
        DD_URL = credentials('defectdojo-url')
        DD_TOKEN = credentials('defectdojo-token')
        PRODUCT_NAME = 'Credit Default Swap Platform'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Security Scans') {
            steps {
                dir('backend') {
                    sh '''
                        mkdir -p target/security-reports
                        
                        # OWASP Dependency Check
                        mvn org.owasp:dependency-check-maven:check \
                            -DdataDirectory=./dependency-check-data \
                            -Dformat=JSON
                        cp target/dependency-check-report.json target/security-reports/
                        
                        # SpotBugs
                        mvn compile spotbugs:spotbugs \
                            -Dspotbugs.xmlOutput=true \
                            -Dspotbugs.xmlOutputDirectory=target/security-reports
                        
                        # Checkstyle
                        mvn checkstyle:checkstyle
                        cp target/checkstyle-result.xml target/security-reports/ || true
                        
                        # PMD
                        mvn pmd:pmd pmd:cpd
                        cp target/pmd.xml target/security-reports/ || true
                    '''
                }
            }
        }
        
        stage('Upload to DefectDojo') {
            steps {
                sh '''
                    chmod +x compliance/scripts/upload-to-defectdojo-ci.sh
                    ./compliance/scripts/upload-to-defectdojo-ci.sh
                '''
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'backend/target/security-reports/**', allowEmptyArchive: true
        }
    }
}
```

---

## 🔵 GitLab CI Integration

### .gitlab-ci.yml Example

```yaml
stages:
  - scan
  - upload

variables:
  DD_URL: $DEFECTDOJO_URL
  DD_TOKEN: $DEFECTDOJO_TOKEN
  PRODUCT_NAME: "Credit Default Swap Platform"

security_scan:
  stage: scan
  image: maven:3.9-eclipse-temurin-17
  script:
    - cd backend
    - mkdir -p target/security-reports
    
    # OWASP Dependency Check
    - mvn org.owasp:dependency-check-maven:check -DdataDirectory=./dependency-check-data -Dformat=JSON
    - cp target/dependency-check-report.json target/security-reports/ || true
    
    # SpotBugs
    - mvn compile spotbugs:spotbugs -Dspotbugs.xmlOutput=true -Dspotbugs.xmlOutputDirectory=target/security-reports
    
    # Checkstyle
    - mvn checkstyle:checkstyle
    - cp target/checkstyle-result.xml target/security-reports/ || true
    
    # PMD
    - mvn pmd:pmd pmd:cpd
    - cp target/pmd.xml target/security-reports/ || true
  artifacts:
    paths:
      - backend/target/security-reports/
    expire_in: 30 days

upload_to_defectdojo:
  stage: upload
  image: curlimages/curl:latest
  dependencies:
    - security_scan
  script:
    - chmod +x compliance/scripts/upload-to-defectdojo-ci.sh
    - ./compliance/scripts/upload-to-defectdojo-ci.sh
  only:
    - main
    - develop
```

---

## 🔵 Azure DevOps Integration

### azure-pipelines.yml Example

```yaml
trigger:
  - main
  - develop

pool:
  vmImage: 'ubuntu-latest'

variables:
  DD_URL: $(DEFECTDOJO_URL)
  DD_TOKEN: $(DEFECTDOJO_TOKEN)
  PRODUCT_NAME: 'Credit Default Swap Platform'

stages:
- stage: SecurityScan
  displayName: 'Security Scanning'
  jobs:
  - job: RunScans
    displayName: 'Run Security Scans'
    steps:
    - task: Maven@4
      displayName: 'OWASP Dependency Check'
      inputs:
        mavenPomFile: 'backend/pom.xml'
        goals: 'org.owasp:dependency-check-maven:check'
        options: '-DdataDirectory=./dependency-check-data -Dformat=JSON'
      continueOnError: true
    
    - task: Maven@4
      displayName: 'SpotBugs Analysis'
      inputs:
        mavenPomFile: 'backend/pom.xml'
        goals: 'compile spotbugs:spotbugs'
        options: '-Dspotbugs.xmlOutput=true'
      continueOnError: true
    
    - task: Bash@3
      displayName: 'Upload to DefectDojo'
      inputs:
        targetType: 'filePath'
        filePath: 'compliance/scripts/upload-to-defectdojo-ci.sh'
      env:
        DD_URL: $(DD_URL)
        DD_TOKEN: $(DD_TOKEN)
        PRODUCT_NAME: $(PRODUCT_NAME)
    
    - task: PublishBuildArtifacts@1
      displayName: 'Publish Scan Results'
      inputs:
        pathToPublish: 'backend/target/security-reports'
        artifactName: 'security-scan-results'
```

---

## 📊 Best Practices

### 1. **Separate Environments**
- Use different DefectDojo products for dev/staging/prod
- Tag engagements with branch names and commit hashes

### 2. **Automate Regularly**
- Run scans on every PR
- Schedule full scans weekly
- Upload results automatically

### 3. **Set Quality Gates**
- Fail builds on critical vulnerabilities
- Require security review for high-severity issues
- Track remediation progress

### 4. **Notifications**
- Configure DefectDojo to alert on new high/critical findings
- Integrate with Slack, Teams, or email
- Assign findings to responsible team members

### 5. **Trend Analysis**
- Compare scans over time
- Track metric improvements
- Generate compliance reports

---

## 🔒 Security Considerations

### Protect API Tokens
- ✅ Use CI/CD secrets management
- ✅ Rotate tokens regularly
- ✅ Limit token permissions
- ❌ Never commit tokens to Git

### Network Security
- ✅ Use HTTPS for production DefectDojo
- ✅ Restrict DefectDojo access with firewall rules
- ✅ Use VPN for remote access
- ✅ Enable API rate limiting

### Data Privacy
- ✅ Review what data is uploaded
- ✅ Sanitize logs and outputs
- ❌ Don't upload production secrets or credentials
- ✅ Use separate instances for different sensitivity levels

---

## 📈 Monitoring & Metrics

Track these metrics in DefectDojo:

- **Total Vulnerabilities** - Trend over time
- **Critical/High Findings** - Require immediate attention
- **Mean Time to Remediate (MTTR)** - Track fix efficiency
- **Scan Coverage** - Ensure all services are scanned
- **False Positive Rate** - Improve scanner accuracy

---

## 🆘 Troubleshooting CI/CD

### Issue: Upload fails in CI

**Check:**
1. Is `DD_TOKEN` secret configured correctly?
2. Is DefectDojo accessible from CI runners?
3. Are scan result files generated?

**Debug:**
```bash
# Add to CI script
ls -la backend/target/security-reports/
curl -v http://defectdojo.example.com/login
```

### Issue: Rate limiting errors

**Solution:**
- Add delays between uploads
- Increase DefectDojo rate limits
- Use bulk import API

---

## 📚 Additional Resources

- **DefectDojo CI/CD Docs:** https://defectdojo.github.io/django-DefectDojo/integrations/
- **GitHub Actions Marketplace:** https://github.com/marketplace?category=security
- **OWASP DevSecOps:** https://owasp.org/www-project-devsecops-guideline/

---

**🚀 Happy Automating!**
