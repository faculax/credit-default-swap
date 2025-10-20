# 🔧 How to Change SonarCloud from Default PERMISSIVE Profile

## Why SonarCloud Shows 0 Issues

**The default "Sonar way" profile is PERMISSIVE** - it only enables ~100 of 500+ available rules.

Most **security rules are DISABLED by default**, including:
- ❌ RSPEC-5145 (CRLF Injection)
- ❌ RSPEC-2245 (Weak Random)
- ❌ RSPEC-2076/2077 (SQL/Command Injection)
- ❌ Many other vulnerability and security hotspot rules

---

## ✅ Solution: Create Custom Security Profile

### Method 1: Via SonarCloud UI (Recommended - 10 minutes)

#### Step 1: Access Quality Profiles

```
🌐 https://sonarcloud.io/organizations/ayodeleoladeji/quality_profiles
```

#### Step 2: Create New Profile

1. Click **"Create"** button (top right)
2. Fill in form:
   - **Name:** `CDS Security Profile`
   - **Language:** `Java`
   - **Parent:** `Sonar way` (or `None` for full control)
3. Click **"Create"**

#### Step 3: Activate Security Rules

**Option A: Bulk Activate (Fastest)**

1. Click on your new `CDS Security Profile`
2. Click **"Activate More Rules"** button
3. In left filter panel:
   - **Type:** Check ✅ `Vulnerability`
   - **Type:** Check ✅ `Security Hotspot`
4. Click **"Bulk Change"** (bottom right)
5. Click **"Activate In [Your Profile]"**
6. Confirm activation

This will activate **150+ security rules** at once!

**Option B: Activate Specific Critical Rules (More Control)**

Search and manually activate these one by one:

| Search Term | Rule ID | Name | Why Critical |
|-------------|---------|------|--------------|
| `log injection` | **S5145** | Log forging vulnerability | CRLF Injection |
| `pseudorandom` | **S2245** | Weak random generators | Predictable Random |
| `sql injection` | **S2077** | SQL formatting security | SQL Injection |
| `command injection` | **S2076** | OS command vulnerability | Command Injection |
| `reflected xss` | **S5131** | XSS endpoint vulnerability | Cross-Site Scripting |
| `ssrf` | **S5144** | Server-side request forging | SSRF |
| `weak hash` | **S4790** | Weak cryptographic hash | Weak Crypto |
| `hardcoded credential` | **S2068** | Hardcoded passwords | Credential Leak |
| `ldap injection` | **S2078** | LDAP query vulnerability | LDAP Injection |
| `xpath injection` | **S2091** | XPath injection | XPath Injection |

For each rule:
1. Search for the term
2. Click on the rule
3. Click **"Activate"**
4. Select your profile
5. Choose severity (keep default or increase)

#### Step 4: Set as Default Profile

**Option A: Organization-wide Default**
1. Go back to Quality Profiles page
2. Find `CDS Security Profile`
3. Click **⋮** (three dots menu)
4. Select **"Set as Default"**
5. Confirm

Now ALL new projects will use this profile automatically.

**Option B: Per-Project Assignment**

For each existing project:

1. Navigate to project (e.g., `credit-default-swap-backend`)
2. Click **"Project Settings"** (bottom left)
3. Click **"Quality Profiles"**
4. Find **Java** row
5. Change from `Sonar way` to `CDS Security Profile`
6. Click **"Save"**

Repeat for:
- `ayodeleoladeji_credit-default-swap-backend`
- `ayodeleoladeji_credit-default-swap-gateway`
- `ayodeleoladeji_credit-default-swap-risk-engine`

#### Step 5: Verify Activation

1. Click on your `CDS Security Profile`
2. Check **"Active Rules"** count
   - **Before:** ~100-150 rules
   - **After:** 300-450+ rules
3. Filter by Type = `Vulnerability` → should see 50-100 rules
4. Filter by Type = `Security Hotspot` → should see 50-100 rules

---

### Method 2: Via API (Advanced - For Automation)

#### Prerequisites
```powershell
# Set your SonarCloud token
$env:SONAR_TOKEN = "your_token_here"
```

#### Create Profile via API

```powershell
# 1. Create new quality profile
$createProfileBody = @{
    language = "java"
    name = "CDS Security Profile"
} | ConvertTo-Json

Invoke-RestMethod -Uri "https://sonarcloud.io/api/qualityprofiles/create?organization=ayodeleoladeji" `
    -Method Post `
    -Headers @{Authorization = "Bearer $env:SONAR_TOKEN"} `
    -Body $createProfileBody `
    -ContentType "application/json"
```

#### Activate Security Rules in Bulk

```powershell
# 2. Search for security vulnerability rules
$rules = Invoke-RestMethod -Uri "https://sonarcloud.io/api/rules/search?languages=java&types=VULNERABILITY&ps=500" `
    -Headers @{Authorization = "Bearer $env:SONAR_TOKEN"}

# 3. Activate each rule
foreach ($rule in $rules.rules) {
    Write-Host "Activating $($rule.key)..."
    
    Invoke-RestMethod -Uri "https://sonarcloud.io/api/qualityprofiles/activate_rule" `
        -Method Post `
        -Headers @{Authorization = "Bearer $env:SONAR_TOKEN"} `
        -Body @{
            key = "CDS Security Profile"
            organization = "ayodeleoladeji"
            rule = $rule.key
            severity = $rule.severity
        }
}

# 4. Repeat for Security Hotspots
$hotspots = Invoke-RestMethod -Uri "https://sonarcloud.io/api/rules/search?languages=java&types=SECURITY_HOTSPOT&ps=500" `
    -Headers @{Authorization = "Bearer $env:SONAR_TOKEN"}

foreach ($rule in $hotspots.rules) {
    Write-Host "Activating $($rule.key)..."
    
    Invoke-RestMethod -Uri "https://sonarcloud.io/api/qualityprofiles/activate_rule" `
        -Method Post `
        -Headers @{Authorization = "Bearer $env:SONAR_TOKEN"} `
        -Body @{
            key = "CDS Security Profile"
            organization = "ayodeleoladeji"
            rule = $rule.key
        }
}
```

#### Set as Default

```powershell
# 5. Set as default profile
Invoke-RestMethod -Uri "https://sonarcloud.io/api/qualityprofiles/set_default" `
    -Method Post `
    -Headers @{Authorization = "Bearer $env:SONAR_TOKEN"} `
    -Body @{
        language = "java"
        qualityProfile = "CDS Security Profile"
        organization = "ayodeleoladeji"
    }
```

---

## 🔍 How to Verify It's Working

### Check 1: Profile Rules Count

```
Before: Sonar way → ~120 active rules
After: CDS Security Profile → 300-450+ active rules
```

### Check 2: Run New Analysis

```powershell
# Trigger new analysis
cd backend
./mvnw clean test sonar:sonar `
    -Dsonar.organization=ayodeleoladeji `
    -Dsonar.host.url=https://sonarcloud.io
```

### Check 3: View Dashboard

After analysis completes (~5 minutes):

```
🌐 https://sonarcloud.io/dashboard?id=ayodeleoladeji_credit-default-swap-backend
```

**Expected results:**
- **Issues:** 50-100+ (was 0)
- **Security Hotspots:** 10-30 (was 0)
- **Vulnerabilities:** 5-20 (was 0)
- **Quality Gate:** FAILED ❌ (was PASSED ✅)

---

## 🎯 Quick Comparison: Before vs After

| Metric | Default "Sonar way" | Custom Security Profile |
|--------|---------------------|-------------------------|
| **Active Rules** | ~120 rules | 300-450+ rules |
| **Vulnerability Rules** | ~20 rules | 50-100 rules |
| **Security Hotspot Rules** | ~10 rules | 50-100 rules |
| **S5145 (CRLF)** | ❌ Disabled | ✅ Enabled |
| **S2245 (Random)** | ❌ Disabled | ✅ Enabled |
| **S2077 (SQL)** | ❌ Disabled | ✅ Enabled |
| **Detection Rate** | ~10% of issues | ~90% of issues |

---

## 🚨 Common Issues & Solutions

### Issue: Can't find "Activate More Rules" button
**Solution:** You need **Admin** permissions on the organization.
- Go to: https://sonarcloud.io/organizations/ayodeleoladeji/members
- Verify you're an admin
- If not, request admin access or use organization owner account

### Issue: "Bulk Change" option grayed out
**Solution:** Select at least one rule first.
- Apply filters (Type = Vulnerability)
- Wait for rules to load
- "Bulk Change" should become clickable

### Issue: Changes don't apply to existing projects
**Solution:** Manually assign profile to each project.
- Project Settings → Quality Profiles → Select new profile
- Or set new profile as organization default BEFORE creating projects

### Issue: Analysis still shows 0 issues after enabling rules
**Solution:** Clear SonarCloud cache and re-analyze.
```powershell
# Force clean analysis
cd backend
./mvnw clean test sonar:sonar `
    -Dsonar.scm.disabled=true `
    -Dsonar.organization=ayodeleoladeji
```

---

## 📊 Expected Timeline

| Step | Time | Difficulty |
|------|------|------------|
| Create profile | 2 min | Easy |
| Bulk activate security rules | 3 min | Easy |
| Set as default | 1 min | Easy |
| Assign to 3 projects | 3 min | Easy |
| Trigger new analysis | 5-10 min | Easy |
| Review results | 10-30 min | Medium |
| **TOTAL** | **25-50 min** | **Easy** |

---

## ✅ Success Criteria

You'll know it's working when:

1. ✅ Quality Profile shows 300+ active rules
2. ✅ Dashboard shows 50-100+ issues (not 0)
3. ✅ Security Hotspots tab shows 10-30 items
4. ✅ Vulnerabilities detected (Blocker/Critical severity)
5. ✅ Quality Gate FAILS (because issues need fixing)
6. ✅ Issues match SpotBugs findings (CRLF, Random, etc.)

---

## 🔗 Quick Links

- **Your Quality Profiles:** https://sonarcloud.io/organizations/ayodeleoladeji/quality_profiles
- **Java Rules Catalog:** https://rules.sonarsource.com/java
- **Your Projects:** https://sonarcloud.io/organizations/ayodeleoladeji/projects
- **Documentation:** https://docs.sonarcloud.io/improving/quality-profiles/

---

## 📝 Step-by-Step Checklist

- [ ] Open Quality Profiles page
- [ ] Click "Create" → Name: `CDS Security Profile`
- [ ] Click "Activate More Rules"
- [ ] Filter: Type = Vulnerability ✅
- [ ] Filter: Type = Security Hotspot ✅
- [ ] Click "Bulk Change" → "Activate"
- [ ] Set profile as default
- [ ] Assign to backend project
- [ ] Assign to gateway project
- [ ] Assign to risk-engine project
- [ ] Push code to trigger workflow
- [ ] Wait 10 minutes for analysis
- [ ] Check dashboard - should see 50-100+ issues!

---

**Next:** After enabling the profile, see `SONARCLOUD_ACTION_PLAN.md` for complete testing instructions.

**Last Updated:** January 20, 2025
