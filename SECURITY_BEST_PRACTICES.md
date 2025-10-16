# Security Best Practices - CDS Platform

## 🔐 Credential Management

### ❌ NEVER Do This:
```yaml
# BAD - Hardcoded credentials
datasource:
  username: cdsuser
  password: cdspass
```

### ✅ ALWAYS Do This:
```yaml
# GOOD - Environment variables with fallback defaults
datasource:
  username: ${DB_USERNAME:cdsuser}
  password: ${DB_PASSWORD:cdspass}
```

## 🔧 Environment Variable Configuration

### Local Development (.env file)
```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=cdsplatform
DB_USERNAME=cdsuser
DB_PASSWORD=your-local-password

# Risk Engine
RISK_ENGINE_URL=http://localhost:8082

# Backend
BACKEND_BASE_URL=http://localhost:8080
```

### Production Deployment
Set these as environment variables in your deployment platform:
- **Docker Compose**: Use `environment:` section or `.env` file
- **Kubernetes**: Use Secrets and ConfigMaps
- **Cloud Platforms**: Use managed secret services (AWS Secrets Manager, Azure Key Vault, etc.)

## 📋 Application Configuration Pattern

### Backend (application.yml)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:cdsplatform}
    username: ${DB_USERNAME:cdsuser}
    password: ${DB_PASSWORD}  # No default for passwords in production!
```

**Note:** 
- Use defaults for development convenience: `${VAR:default}`
- Remove defaults for sensitive values in production: `${VAR}`
- This way, production will fail fast if credentials are missing

## 🐳 Docker Compose Configuration

### ❌ Bad Practice:
```yaml
environment:
  - DB_PASSWORD=hardcoded123
```

### ✅ Good Practice:
```yaml
environment:
  - DB_PASSWORD=${DB_PASSWORD}  # Read from .env file or host environment
```

Or use Docker secrets:
```yaml
secrets:
  - db_password
```

## 🔍 Security Scanning

Our CI/CD pipeline automatically scans for:
1. ✅ Hardcoded passwords in YAML/properties files
2. ✅ Hardcoded API keys in Java code
3. ✅ Database credentials in connection strings
4. ✅ Secrets in configuration files

**Pipeline will FAIL if hardcoded credentials are detected!**

## 🛡️ Additional Security Measures

### 1. Never Commit Secrets
- Add `.env` files to `.gitignore`
- Use `.env.example` with dummy values
- Document required environment variables

### 2. Use Secret Management Tools
- **Development**: `.env` files (gitignored)
- **Staging/Production**: 
  - Kubernetes Secrets
  - HashiCorp Vault
  - AWS Secrets Manager
  - Azure Key Vault
  - Google Secret Manager

### 3. Rotate Credentials Regularly
- Database passwords: Every 90 days
- API keys: Every 180 days
- Service accounts: Every 90 days

### 4. Principle of Least Privilege
- Use read-only database users where possible
- Grant only necessary permissions
- Separate credentials for different environments

## 📚 References

- [OWASP - Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_CheatSheet.html)
- [12-Factor App - Config](https://12factor.net/config)
- [Spring Boot - Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)

---

## ✅ Checklist Before Committing

- [ ] No hardcoded passwords in configuration files
- [ ] All secrets use `${ENV_VAR}` or `${ENV_VAR:default}` pattern
- [ ] `.env` files are in `.gitignore`
- [ ] `.env.example` provided with dummy values
- [ ] Documentation updated with required environment variables
- [ ] CI/CD pipeline passes security scans

---

*Last Updated: October 16, 2025*
