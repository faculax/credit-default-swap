# ⚡ DefectDojo - 60 Second Quick Start

## 🚀 Start DefectDojo

```powershell
# Windows
./defectdojo.ps1 start

First Time run
./defectdojo.ps1 start
./defectdojo.ps1 init

# Linux/Mac
cd compliance && make start
```

## 🌐 Access

**URL:** http://localhost:8081  
**Username:** admin  
**Password:** admin  

⚠️ **Change password immediately!**

## 🔍 Run First Scan

```powershell
# Windows
./defectdojo.ps1 scan
./defectdojo.ps1 upload-components

# Linux/Mac
cd compliance
make scan
make upload
```

## 📊 View Results

Open http://localhost:8081 in your browser

## 🆘 Need Help?

```powershell
# Windows
./defectdojo.ps1 help

# Linux/Mac
cd compliance && make help
```

**Full docs:** `compliance/README.md`

---

**That's it! You're scanning for vulnerabilities! 🛡️**
