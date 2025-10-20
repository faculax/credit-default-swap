# 🚀 Deploying SonarQube to Render

## Overview

**Render** is a cloud platform that makes it easy to deploy containerized applications. This guide will help you deploy SonarQube to Render so your team can access it from anywhere.

---

## 📋 Prerequisites

- [ ] Render account (sign up at https://render.com - free tier available)
- [ ] Credit card for verification (required even for free tier)
- [ ] GitHub account (for connecting repository)

---

## 🎯 Deployment Options

### **Option 1: Docker Hub (Recommended - Easiest)**
Use the official SonarQube image from Docker Hub (no credentials needed for public images).

### **Option 2: Custom Build**
Build and deploy from your own repository.

---

## 🚀 Option 1: Deploy from Docker Hub (Recommended)

### Step 1: Create PostgreSQL Database on Render

1. **Login to Render** → https://dashboard.render.com
2. Click **New +** → **PostgreSQL**
3. Configure:
   ```
   Name: sonarqube-db
   Database: sonar
   User: sonar
   Region: Choose closest to you (e.g., Oregon US West)
   Plan: Free (or Starter for production)
   ```
4. Click **Create Database**
5. **Save the connection details** (you'll need these):
   - Internal Database URL
   - External Database URL

### Step 2: Create SonarQube Web Service

1. Click **New +** → **Web Service**
2. Select **Deploy an existing image from a registry**
3. Configure:

#### **Image Configuration:**
```
Image URL: docker.io/sonarqube:lts-community
```

**Credentials:** Leave blank (public image, no credentials needed)

#### **Service Details:**
```
Name: sonarqube
Region: Same as your database
Instance Type: Standard (minimum 2GB RAM recommended)
```

#### **Environment Variables:**
Click **Add Environment Variable** and add these:

| Key | Value |
|-----|-------|
| `SONAR_JDBC_URL` | `jdbc:postgresql://[your-db-host]:5432/sonar` |
| `SONAR_JDBC_USERNAME` | `sonar` |
| `SONAR_JDBC_PASSWORD` | `[your-db-password]` |
| `SONAR_ES_BOOTSTRAP_CHECKS_DISABLE` | `true` |

**Get the JDBC URL from Step 1:**
- Copy the **Internal Database URL** from your PostgreSQL database
- Format: `postgresql://user:password@host:port/database`
- Convert to: `jdbc:postgresql://host:port/database`
- Example: 
  - Internal URL: `postgresql://sonar:abc123@dpg-xyz.oregon-postgres.render.com:5432/sonar`
  - JDBC URL: `jdbc:postgresql://dpg-xyz.oregon-postgres.render.com:5432/sonar`

#### **Health Check:**
```
Health Check Path: /api/system/status
```

#### **Port:**
```
Port: 9000
```

4. Click **Create Web Service**
5. Wait for deployment (5-10 minutes on first run)

### Step 3: Configure GitHub Secrets

Once deployed, Render will give you a URL like: `https://sonarqube-xyz.onrender.com`

1. Go to your GitHub repository: https://github.com/faculax/credit-default-swap/settings/secrets/actions
2. Update/Add secrets:
   ```
   SONAR_HOST_URL = https://sonarqube-xyz.onrender.com
   SONAR_TOKEN = <generate-after-first-login>
   ```

### Step 4: First Login

1. Visit your Render SonarQube URL: `https://sonarqube-xyz.onrender.com`
2. Login: `admin` / `admin`
3. **Change password immediately**
4. Generate API token (User Menu → My Account → Security → Generate Tokens)
5. Add token to GitHub secrets (Step 3)

---

## 🎨 Option 2: Deploy with Custom Configuration

If you want more control, create a custom Dockerfile:

### Step 1: Create Dockerfile for Render

Create `Dockerfile.render` in your repository root:

```dockerfile
FROM sonarqube:lts-community

# Add custom plugins (optional)
# COPY plugins/*.jar /opt/sonarqube/extensions/plugins/

# Custom configuration (optional)
# COPY sonar.properties /opt/sonarqube/conf/

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=5 \
  CMD wget -qO- http://localhost:9000/api/system/status | grep -q '"status":"UP"'

EXPOSE 9000
```

### Step 2: Create Blueprint File (render.yaml)

Create `render.yaml` in your repository root:

```yaml
services:
  # PostgreSQL Database
  - type: pserv
    name: sonarqube-db
    env: docker
    plan: free  # or starter for production
    ipAllowList: []
    databases:
      - name: sonar
        user: sonar

  # SonarQube Web Service
  - type: web
    name: sonarqube
    env: docker
    region: oregon
    plan: standard  # 2GB RAM minimum
    dockerfilePath: ./Dockerfile.render
    envVars:
      - key: SONAR_JDBC_URL
        fromDatabase:
          name: sonarqube-db
          property: connectionString
      - key: SONAR_JDBC_USERNAME
        value: sonar
      - key: SONAR_JDBC_PASSWORD
        fromDatabase:
          name: sonarqube-db
          property: password
      - key: SONAR_ES_BOOTSTRAP_CHECKS_DISABLE
        value: "true"
    healthCheckPath: /api/system/status
```

### Step 3: Deploy from GitHub

1. Login to Render → **New +** → **Blueprint**
2. Connect your GitHub repository
3. Select branch: `security-compliance`
4. Render will automatically detect `render.yaml`
5. Click **Apply**
6. Wait for deployment

---

## 💰 Pricing Considerations

### **Free Tier:**
- ❌ **Not recommended for SonarQube** (insufficient resources)
- 512MB RAM (SonarQube needs 2GB+)
- Spins down after inactivity

### **Starter Tier ($7/month per service):**
- ✅ **Minimum for SonarQube**
- 2GB RAM
- Always on
- Good for small teams

### **Standard Tier ($25/month):**
- ✅ **Recommended for production**
- 4GB RAM
- Better performance
- Suitable for larger teams

### **Total Cost:**
```
PostgreSQL: $7/month (Starter)
SonarQube:  $25/month (Standard recommended)
Total:      $32/month
```

**Alternative:** Use **Render's free PostgreSQL** + **Standard SonarQube** = ~$25/month

---

## 🔧 Configuration Tips

### 1. **Increase Memory for Render**

In your `render.yaml` or Render dashboard:
```yaml
plan: standard  # 2GB RAM
# or
plan: pro       # 4GB RAM
```

### 2. **Persistent Storage**

SonarQube stores data in PostgreSQL, but also needs disk space for Elasticsearch. Render provides:
- **Disk:** Add persistent disk in Render dashboard
  - Size: 10GB minimum
  - Mount Path: `/opt/sonarqube/data`

### 3. **Custom Domain** (Optional)

1. Render Dashboard → Your SonarQube service → **Settings**
2. Scroll to **Custom Domain**
3. Add: `sonarqube.yourdomain.com`
4. Update DNS records as instructed
5. Update GitHub secret `SONAR_HOST_URL`

### 4. **Environment Variables Best Practices**

Use Render's **Secret Files** for sensitive data:
1. Render Dashboard → Service → **Environment**
2. Add secret file: `/opt/sonarqube/.env`
3. Content:
   ```
   SONAR_JDBC_PASSWORD=your-secure-password
   ```

---

## 🔒 Security Best Practices

### 1. **Use Strong Database Password**
Generate a strong password for PostgreSQL:
```bash
openssl rand -base64 32
```

### 2. **Enable HTTPS** (Render does this automatically)
Your URL will be: `https://sonarqube-xyz.onrender.com`

### 3. **Restrict Access** (Optional)
Use Render's **IP Allowlist** feature:
1. Service → **Settings** → **IP Allowlist**
2. Add your office/home IP addresses
3. Add GitHub Actions IP ranges if needed

### 4. **Backup Database**
Render provides automatic backups for paid PostgreSQL plans.

---

## 📊 Monitoring & Logs

### View Logs:
1. Render Dashboard → Your service → **Logs**
2. Or use Render CLI:
   ```bash
   render logs -s sonarqube
   ```

### Health Checks:
Render automatically monitors `/api/system/status`:
- Healthy: Returns 200 with `"status":"UP"`
- Unhealthy: Service will restart automatically

---

## 🐛 Troubleshooting

### Issue 1: "Cannot connect to PostgreSQL"

**Solution:**
- Verify `SONAR_JDBC_URL` format
- Use **Internal Database URL** (not external)
- Check database is in same region

### Issue 2: "Out of Memory"

**Solution:**
- Upgrade to Standard or Pro plan (2GB+ RAM)
- Check Render Dashboard → Metrics

### Issue 3: "Service keeps restarting"

**Solution:**
- Check logs: Render Dashboard → Logs
- Increase startup timeout: `startCommand` in render.yaml
- Verify PostgreSQL connection

### Issue 4: "Slow performance"

**Solution:**
- Upgrade to Pro plan (4GB RAM)
- Add persistent disk
- Use dedicated PostgreSQL (Starter+ plan)

---

## 🔄 Updating SonarQube on Render

### Automatic Updates:
Render can auto-deploy on image updates:
1. Service → **Settings** → **Auto-Deploy**
2. Enable: "Auto-deploy on new image"

### Manual Update:
1. Render Dashboard → Service → **Manual Deploy**
2. Click **Deploy latest commit** or **Clear build cache & deploy**

---

## 📝 Quick Start Summary

### For Render Deployment:

**1. Image URL:**
```
docker.io/sonarqube:lts-community
```

**2. Credentials:**
```
Leave blank (public image)
```

**3. Environment Variables:**
```
SONAR_JDBC_URL=jdbc:postgresql://[db-host]:5432/sonar
SONAR_JDBC_USERNAME=sonar
SONAR_JDBC_PASSWORD=[from-render-postgres]
SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true
```

**4. Health Check:**
```
/api/system/status
```

**5. Port:**
```
9000
```

**6. Minimum Plan:**
```
Standard ($25/month) - 2GB RAM
```

---

## 🎯 Alternative: SonarCloud (Recommended for Teams)

If Render costs are a concern, consider **SonarCloud** (official hosted SonarQube):

### **SonarCloud Benefits:**
- ✅ Free for open-source projects
- ✅ No infrastructure management
- ✅ Automatic updates
- ✅ Enterprise features
- ✅ Better performance

### **SonarCloud Pricing:**
- **Free**: Open-source projects
- **Paid**: $10/month per 100K lines of code (private repos)

### **Quick Setup:**
1. Sign up: https://sonarcloud.io
2. Connect GitHub repository
3. Update GitHub secrets:
   ```
   SONAR_HOST_URL=https://sonarcloud.io
   SONAR_ORGANIZATION=your-org
   SONAR_TOKEN=<from-sonarcloud>
   ```
4. Update workflow to use SonarCloud action

**See:** `.github/workflows/security-sonarqube.yml` and add:
```yaml
- name: SonarCloud Scan
  uses: SonarSource/sonarcloud-github-action@master
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

---

## 💡 Recommendation

### **For Your CDS Platform:**

**Best Option:** **SonarCloud** (if budget allows)
- ✅ Less maintenance
- ✅ Better performance
- ✅ Professional support
- Cost: ~$10-20/month (estimate for your codebase size)

**Second Best:** **Render Deployment**
- ✅ Full control
- ✅ Self-hosted
- Cost: ~$32/month

**For Learning/Development:** **Local Docker** (current setup)
- ✅ Free
- ✅ Fast iteration
- ✅ No external dependencies

---

## 📚 Additional Resources

- **Render Documentation**: https://render.com/docs
- **SonarQube Docker**: https://hub.docker.com/_/sonarqube
- **SonarCloud**: https://sonarcloud.io
- **Render Community**: https://community.render.com

---

## ✅ Summary

**To deploy to Render, provide:**
1. **Image URL**: `docker.io/sonarqube:lts-community`
2. **Credentials**: Leave blank (public image)
3. **Environment**: Configure PostgreSQL connection
4. **Plan**: Minimum Standard ($25/month)

**Need help?** Follow the step-by-step guide above or consider SonarCloud for easier deployment.

---

*Last updated: October 20, 2025*
