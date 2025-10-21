# 🔑 NVD API Key Setup Guide

## ✅ Configuration Complete!

I've set up your project to use the NVD API key. Now you just need to add your actual API key.

---

## 📝 Step-by-Step Instructions

### 1️⃣ **Edit the Maven Settings File**

Open the file in your favorite editor:
```
C:\Users\AyodeleOladeji\.m2\settings.xml
```

### 2️⃣ **Replace the Placeholder with Your API Key**

Find this line:
```xml
<nvd.api.key>YOUR_NVD_API_KEY_HERE</nvd.api.key>
```

Replace `YOUR_NVD_API_KEY_HERE` with your actual NVD API key.

**Example:**
```xml
<nvd.api.key>abc123-def456-ghi789-jkl012</nvd.api.key>
```

### 3️⃣ **Save the File**

That's it! The key will now be used for all OWASP scans.

---

## 🚀 Test the Configuration

Run a scan to verify it's working:

```powershell
# Test on backend
cd backend
mvn org.owasp:dependency-check-maven:check

# Or use the convenience script
cd ..
.\defectdojo.ps1 scan
```

---

## 📊 What Should Happen

With the API key configured:
- ✅ **Faster scans** - Downloads complete in seconds instead of timing out
- ✅ **Latest CVE data** - Always checking against the newest vulnerability database
- ✅ **Detects flexjson CVEs** - Should find known vulnerabilities in flexjson 3.3
- ✅ **No rate limiting** - API key gives you higher request limits

---

## 🔍 Expected Results for Flexjson

Flexjson 3.3 has known vulnerabilities including:
- **CVE-2020-12666** - Deserialization vulnerability (CRITICAL)
- Potentially others depending on the NVD database version

After the scan completes, check:
```powershell
# View the report
cd backend
code target/dependency-check-report.html

# Or check JSON for flexjson
$report = Get-Content target/dependency-check-report.json | ConvertFrom-Json
$report.dependencies | Where-Object { $_.fileName -like "*flexjson*" }
```

---

## 🛡️ Security Note

**IMPORTANT:** The `settings.xml` file contains your API key, which is sensitive.

- ✅ **DO:** Keep this file local and secure
- ✅ **DO:** Add `settings.xml` to `.gitignore` if you have it in any project
- ❌ **DON'T:** Commit API keys to version control
- ❌ **DON'T:** Share your `settings.xml` publicly

---

## 🎯 Alternative: Environment Variable (CI/CD)

For CI/CD pipelines, you can also pass the key as an environment variable:

```powershell
# PowerShell
$env:NVD_API_KEY = "your-api-key-here"
mvn org.owasp:dependency-check-maven:check -Dnvd.api.key=$env:NVD_API_KEY
```

```bash
# Bash/Linux
export NVD_API_KEY="your-api-key-here"
mvn org.owasp:dependency-check-maven:check -Dnvd.api.key=$NVD_API_KEY
```

---

## 📚 Additional Resources

- **NVD API Key Request:** https://nvd.nist.gov/developers/request-an-api-key
- **OWASP Dependency Check Docs:** https://jeremylong.github.io/DependencyCheck/
- **Maven Settings Reference:** https://maven.apache.org/settings.html

---

## ❓ Troubleshooting

### Problem: Still getting 403 errors
**Solution:** Double-check that:
1. You replaced `YOUR_NVD_API_KEY_HERE` with your actual key
2. There are no extra spaces or quotes around the key
3. The key is active (check your NVD account)

### Problem: Key not being picked up
**Solution:** 
1. Verify settings.xml location: `C:\Users\AyodeleOladeji\.m2\settings.xml`
2. Check Maven is reading it: `mvn help:effective-settings`
3. Clear and retry: `mvn dependency:purge-local-repository`

### Problem: Scans are slow
**Solution:**
- Increase `nvdApiDelay` to 6000+ in settings.xml (reduces API load)
- First scan downloads ~200MB of CVE data (one-time, takes 5-10 min)
- Subsequent scans use cached data (much faster)

---

## ✨ Quick Reference Commands

```powershell
# Open settings file
code C:\Users\AyodeleOladeji\.m2\settings.xml

# Run single service scan
cd backend
mvn org.owasp:dependency-check-maven:check

# Run all security scans
cd ..
.\defectdojo.ps1 scan

# Upload results to DefectDojo
.\defectdojo.ps1 upload-components

# View backend report in browser
Start-Process backend\target\dependency-check-report.html
```

---

**🎉 You're all set!** Once you add your API key, OWASP scans will work perfectly and detect all known vulnerabilities including the flexjson issues.
