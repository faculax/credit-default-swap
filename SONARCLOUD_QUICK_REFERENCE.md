# 🎯 SonarCloud Quick Reference - Enable Security Scanning

## The Problem
- **SpotBugs:** 114 issues found ✅
- **SonarCloud:** 0 issues shown ❌
- **Cause:** SonarCloud's default quality profile doesn't enable security rules

---

## ⚡ Quick Fix (15 minutes)

### Step 1: Enable Security Rules (10 min)
1. Open: https://sonarcloud.io/organizations/ayodeleoladeji/quality_profiles
2. Click **Activate More Rules**
3. Search: `security`
4. Select ALL rules with Type = `Vulnerability` or `Security Hotspot`
5. Click **Bulk Activate**

### Step 2: Set Strict Quality Gate (5 min)
1. Open: https://sonarcloud.io/organizations/ayodeleoladeji/quality_gates
2. Edit existing OR create new gate
3. Add condition: `Security Rating on New Code worse than A → FAIL`
4. Add condition: `Security Hotspots Reviewed < 100% → FAIL`
5. Set as default

### Step 3: Push & Test
```powershell
git add .
git commit -m "config(sonarcloud): Enable security analysis"
git push
```

---

## 📊 Expected Results

| Before | After |
|--------|-------|
| 0 issues | 50-100+ issues |
| Quality Gate: PASSED | Quality Gate: FAILED (until fixed) |
| No security hotspots | 10-30 hotspots to review |
| Coverage: Unknown | Coverage: Measured (40-60%) |

---

## 🔍 Critical Security Rules to Enable

| Rule | Name | Detects |
|------|------|---------|
| **S5145** | Log forging | CRLF Injection |
| **S2245** | Pseudorandom generators | Weak Random |
| **S2077** | SQL query formatting | SQL Injection |
| **S2076** | OS command execution | Command Injection |
| **S5131** | Reflected XSS | XSS Vulnerabilities |
| **S5144** | Server-side request forging | SSRF |
| **S4790** | Weak hash algorithms | Weak Crypto |

---

## 🎯 SonarCloud URLs

- **Quality Profiles:** https://sonarcloud.io/organizations/ayodeleoladeji/quality_profiles
- **Quality Gates:** https://sonarcloud.io/organizations/ayodeleoladeji/quality_gates  
- **Projects:** https://sonarcloud.io/organizations/ayodeleoladeji/projects
- **Rules Catalog:** https://rules.sonarsource.com/java

---

## ✅ Verification Checklist

- [ ] Custom quality profile created with security rules
- [ ] Quality profile set as default for organization
- [ ] All 3 projects using custom profile (backend, gateway, risk-engine)
- [ ] Quality gate has security conditions
- [ ] Quality gate set as default
- [ ] GitHub Actions workflow triggered
- [ ] SonarCloud dashboard shows issues

---

**See SONARCLOUD_ACTION_PLAN.md for detailed instructions.**
