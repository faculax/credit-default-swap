# ⚡ DefectDojo Quick Reference

## 🚀 Common Commands

```powershell
# Start/Stop
./defectdojo.ps1 start          # Start all services
./defectdojo.ps1 stop           # Stop all services
./defectdojo.ps1 restart        # Restart services
./defectdojo.ps1 status         # Check health

# Scan & Upload
./defectdojo.ps1 scan           # Run security scans
./defectdojo.ps1 upload         # Upload to DefectDojo
./defectdojo.ps1 upload -Verbose  # Detailed output

# Maintenance
./defectdojo.ps1 logs           # View all logs
./defectdojo.ps1 logs uwsgi     # View Django logs
./defectdojo.ps1 init           # Initialize parsers
./defectdojo.ps1 clean          # Remove all data (⚠️ destructive)
./defectdojo.ps1 help           # Show help
```

---

## 🌐 Access

**URL**: http://localhost:8081  
**Username**: `admin`  
**Password**: `admin`

---

## 📁 Important Paths

```
compliance/
├── docker-compose.defectdojo.yml    # Docker config
├── scripts/
│   ├── upload-to-defectdojo.ps1     # Upload script
│   └── init-defectdojo-parsers.ps1  # Parser setup
└── README.md                        # Full documentation

backend/target/security-reports/
├── spotbugs.xml                     # ✅ Uploading successfully
├── checkstyle-result.xml            # ⚠️ Import issues
├── pmd.xml                          # ⚠️ Import issues
└── dependency-check-report.json     # ❌ Needs NVD API key
```

---

## 🔍 Security Scanners

| Scanner | Status | Report File | Upload Status |
|---------|--------|-------------|---------------|
| **SpotBugs** | ✅ Working | `spotbugs.xml` | ✅ Uploading |
| **Checkstyle** | ⚠️ Configured | `checkstyle-result.xml` | ⚠️ 400 Error |
| **PMD** | ⚠️ Configured | `pmd.xml` | ⚠️ 500 Error |
| **OWASP DC** | ❌ Needs API Key | `dependency-check-report.json` | ❌ Not generated |

---

## 🐛 Quick Troubleshooting

### DefectDojo won't start
```powershell
docker ps                           # Check Docker running
./defectdojo.ps1 restart            # Full restart
./defectdojo.ps1 logs               # View error logs
```

### Upload fails
```powershell
# Check DefectDojo is running
./defectdojo.ps1 status

# Re-initialize parsers
./defectdojo.ps1 init

# Upload with verbose logging
./defectdojo.ps1 upload -Verbose
```

### Database issues
```powershell
# Manual database reset
docker exec -it compliance-defectdojo-uwsgi-1 python manage.py migrate --noinput
docker exec -it compliance-defectdojo-uwsgi-1 python manage.py createsuperuser --noinput --username=admin --email=admin@example.com
docker exec -it compliance-defectdojo-uwsgi-1 python manage.py set_password admin --password=admin
```

---

## 🎯 Typical Workflow

```powershell
# 1. Start (first time or after reboot)
./defectdojo.ps1 start

# 2. Initialize parsers (first time only)
./defectdojo.ps1 init

# 3. Regular usage
./defectdojo.ps1 scan                    # Run scans
./defectdojo.ps1 upload                  # Upload results
# Open http://localhost:8081 to view

# 4. When done
./defectdojo.ps1 stop
```

---

## 📊 Viewing Results

1. Open http://localhost:8081
2. Login with `admin` / `admin`
3. Navigate to **Products** → **Credit Default Swap Platform**
4. Click on latest **Engagement**
5. View **Findings** tab

---

## 🔑 Get NVD API Key (OWASP Dependency Check)

1. Visit: https://nvd.nist.gov/developers/request-an-api-key
2. Fill form → Get key instantly
3. Set environment variable:
   ```powershell
   $env:NVD_API_KEY="your-key-here"
   ```
4. Add to `backend/pom.xml`:
   ```xml
   <nvdApiKey>${env.NVD_API_KEY}</nvdApiKey>
   ```
5. Run scan:
   ```powershell
   ./defectdojo.ps1 scan
   ```

---

## 📞 Need Help?

- **Full Docs**: `compliance/README.md`
- **Implementation Details**: `compliance/IMPLEMENTATION_COMPLETE.md`
- **Logs**: `./defectdojo.ps1 logs`
- **Status**: `./defectdojo.ps1 status`

---

**Last Updated**: 2025-10-20  
**Status**: ✅ Operational (SpotBugs working)
