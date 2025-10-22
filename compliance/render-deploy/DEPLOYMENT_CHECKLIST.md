# 🚀 Pre-Deployment Checklist

## Before Deploying to Render

### 1. Configuration Review
- [ ] Updated `DD_CSRF_TRUSTED_ORIGINS` in render.yaml or plan to set in dashboard
- [ ] Reviewed all environment variables in render.yaml
- [ ] Set `DD_ADMIN_PASSWORD` to non-default value (via Render dashboard after deploy)
- [ ] Chose appropriate Render plan (Standard minimum recommended)
- [ ] Selected appropriate disk size (10GB default, adjust if needed)

### 2. File Verification
- [ ] All files present: Dockerfile, init-db.sh, supervisord.conf, uwsgi.ini, nginx.conf, health-check.sh, render.yaml
- [ ] All shell scripts have execute permissions (handled by Dockerfile)
- [ ] No sensitive data committed to repository

### 3. Security Verification
- [ ] No hardcoded passwords in any files
- [ ] render.yaml uses `generateValue: true` for secrets
- [ ] render.yaml uses `sync: false` for sensitive values
- [ ] HTTPS enforcement configured (DD_SESSION_COOKIE_SECURE=True)
- [ ] Security headers present in nginx.conf

### 4. Repository Setup
- [ ] Code pushed to GitHub/GitLab
- [ ] Repository connected to Render account
- [ ] render.yaml in correct path (compliance/render-deploy/)

## During Deployment

### 5. Monitor Build Process
- [ ] Watch build logs for errors
- [ ] Verify all apt packages install successfully
- [ ] Confirm DefectDojo source clone succeeds
- [ ] Check Python dependencies install without errors
- [ ] Note any warnings (some are expected)

Expected build time: **10-15 minutes** first deployment

### 6. Monitor Initialization
- [ ] Watch for "Starting DefectDojo initialization..." message
- [ ] Verify PostgreSQL initialization completes
- [ ] Verify Redis starts successfully
- [ ] Confirm Django migrations run without errors
- [ ] Check static files collection succeeds
- [ ] Verify superuser creation

Expected initialization time: **2-3 minutes**

### 7. Health Check Validation
- [ ] Service shows "Live" status (not restarting)
- [ ] No repeated restart loops
- [ ] Health check endpoint returns 200

## After Deployment

### 8. Access Verification
- [ ] Can access DefectDojo URL (https://your-app.onrender.com)
- [ ] Login page loads with proper styling
- [ ] Can login with admin credentials
- [ ] Dashboard loads successfully
- [ ] No console errors in browser dev tools

### 9. Functional Testing
- [ ] Create test product
- [ ] Create test engagement
- [ ] Upload sample scan result (any format)
- [ ] View findings
- [ ] Check that background tasks work (Celery)

### 10. Configuration Finalization
- [ ] Change admin password (first login or Settings → Users)
- [ ] Configure email settings (if needed)
- [ ] Set up additional users
- [ ] Configure notification settings
- [ ] Review security settings

### 11. Security Hardening
- [ ] Update DD_ALLOWED_HOSTS from `*` to actual domain
- [ ] Verify DD_CSRF_TRUSTED_ORIGINS includes your Render URL
- [ ] Enable 2FA for admin account
- [ ] Review and configure user roles
- [ ] Set up IP allowlisting if needed (Render dashboard)

### 12. Monitoring Setup
- [ ] Set up external uptime monitoring
- [ ] Configure log aggregation (if desired)
- [ ] Review Render metrics dashboard
- [ ] Set up alerts for service downtime
- [ ] Document backup procedures

## Troubleshooting Checklist

### Service Won't Start
- [ ] Check build logs for errors
- [ ] Verify all required files copied to container
- [ ] Check environment variables are set
- [ ] Review init logs for PostgreSQL errors
- [ ] Verify disk mount is healthy

### Health Check Failing
- [ ] Check if initialization is still running (wait 3 minutes)
- [ ] Verify PostgreSQL is running (check logs)
- [ ] Verify Redis is running (check logs)
- [ ] Check uwsgi/Django is responding (check logs)
- [ ] Verify nginx is running (check logs)

### Login Issues
- [ ] Verify admin password is set correctly
- [ ] Check DD_CSRF_TRUSTED_ORIGINS includes your URL
- [ ] Check DD_ALLOWED_HOSTS includes your domain
- [ ] Clear browser cookies/cache
- [ ] Try incognito/private browsing mode

### Static Files Not Loading
- [ ] Check logs for collectstatic errors
- [ ] Verify nginx config has correct paths
- [ ] Check file permissions in container
- [ ] Verify DD_INITIALIZE=true on first run
- [ ] Try accessing /static/admin/css/base.css directly

### Database Connection Errors
- [ ] Check PostgreSQL is running: `pg_isready -h 127.0.0.1`
- [ ] Verify DD_DATABASE_URL is set correctly
- [ ] Check PostgreSQL logs: `/app/logs/postgresql.log`
- [ ] Verify disk mount is healthy
- [ ] Check database was created: `psql -h 127.0.0.1 -U postgres -l`

### Celery Not Processing Tasks
- [ ] Check celery-worker logs
- [ ] Verify Redis is running: `redis-cli -h 127.0.0.1 ping`
- [ ] Check DD_CELERY_BROKER_URL is set
- [ ] Check environment variables loaded in supervisord
- [ ] Try restarting celery: `supervisorctl restart celery-worker`

## Validation Commands

Run these in Render Shell (Dashboard → Shell tab):

```bash
# Check all services
supervisorctl status

# Check PostgreSQL
pg_isready -h 127.0.0.1 && echo "PostgreSQL OK" || echo "PostgreSQL FAIL"

# Check Redis
redis-cli -h 127.0.0.1 ping && echo "Redis OK" || echo "Redis FAIL"

# Check Django
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:3031/login && echo " - Django OK" || echo " - Django FAIL"

# Check nginx
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:10000/login && echo " - nginx OK" || echo " - nginx FAIL"

# Run full health check
/health-check.sh && echo "All services healthy" || echo "Health check failed"

# Check environment variables loaded
cat /etc/environment.dd

# Check Django can connect to database
cd /opt/django-DefectDojo && python3 manage.py check --database default
```

## Success Criteria

✅ All checklist items completed  
✅ Service status: "Live" (not restarting)  
✅ Health check: Passing  
✅ Can login and navigate DefectDojo  
✅ Can upload scan results  
✅ Background tasks processing (Celery)  
✅ Static files loading correctly  
✅ No errors in logs (warnings OK)  
✅ Response times acceptable (<1s for most pages)  

## Rollback Plan

If deployment fails critically:

1. **Stop the service** (Dashboard → Settings → Delete Service)
2. **Review logs** to identify root cause
3. **Fix issues** in code
4. **Test locally** with Docker if possible
5. **Redeploy** with fixes

Note: Render disk persists between deploys, so database data is safe.

## Post-Deployment Maintenance

### Daily
- [ ] Check service is "Live"
- [ ] Review error logs for anomalies

### Weekly
- [ ] Review disk usage (Dashboard → Metrics)
- [ ] Check for failed Celery tasks
- [ ] Verify backups are working

### Monthly
- [ ] Update DefectDojo to latest version
- [ ] Review and rotate secrets if needed
- [ ] Check for security updates
- [ ] Review user access and permissions

### Quarterly
- [ ] Full security audit
- [ ] Performance review
- [ ] Capacity planning (disk, plan size)
- [ ] Disaster recovery test

---

**Questions or Issues?**
- Check README.md for detailed troubleshooting
- Review Render logs for specific error messages
- Consult DefectDojo documentation: https://documentation.defectdojo.com
- Render support: https://render.com/support
