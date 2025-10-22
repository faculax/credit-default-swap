#!/bin/bash
set -e

echo "Starting DefectDojo..."

# Clean up stale lock files
rm -f /var/lib/postgresql/data/postmaster.pid
rm -f /run/postgresql/.s.PGSQL.*

# Ensure proper ownership
chown -R postgres:postgres /var/lib/postgresql /run/postgresql

# Export environment variables
export DD_DATABASE_URL="postgresql://defectdojo:defectdojo@127.0.0.1:5432/defectdojo"
export DD_CELERY_BROKER_URL="redis://127.0.0.1:6379/0"
export DD_SECRET_KEY="${DD_SECRET_KEY:-zzz-change-this-in-production-zzz}"
export DD_CREDENTIAL_AES_256_KEY="${DD_CREDENTIAL_AES_256_KEY:-yyy-change-this-credential-key-yyy}"
export DD_ALLOWED_HOSTS="${DD_ALLOWED_HOSTS:-*}"
export DD_CSRF_TRUSTED_ORIGINS="${DD_CSRF_TRUSTED_ORIGINS:-}"
export DD_DEBUG="${DD_DEBUG:-False}"
export DD_ADMIN_USER="${DD_ADMIN_USER:-admin}"
export DD_ADMIN_PASSWORD="${DD_ADMIN_PASSWORD:-admin}"
export DD_ADMIN_MAIL="${DD_ADMIN_MAIL:-admin@defectdojo.local}"
export DD_SESSION_COOKIE_SECURE="${DD_SESSION_COOKIE_SECURE:-True}"
export DD_CSRF_COOKIE_SECURE="${DD_CSRF_COOKIE_SECURE:-True}"
export DD_SESSION_COOKIE_HTTPONLY="${DD_SESSION_COOKIE_HTTPONLY:-True}"
export DD_CSRF_COOKIE_HTTPONLY="${DD_CSRF_COOKIE_HTTPONLY:-False}"
export DD_SESSION_COOKIE_SAMESITE="${DD_SESSION_COOKIE_SAMESITE:-Lax}"
export DD_CSRF_COOKIE_SAMESITE="${DD_CSRF_COOKIE_SAMESITE:-Lax}"

# Start supervisord in background temporarily
/usr/bin/supervisord -c /etc/supervisord.conf &
SUPERVISOR_PID=$!

# Wait for PostgreSQL
echo "Waiting for PostgreSQL..."
for i in {1..60}; do
    if su - postgres -c "pg_isready -h 127.0.0.1" > /dev/null 2>&1; then
        echo "PostgreSQL ready"
        break
    fi
    sleep 2
done

# Wait for Redis
echo "Waiting for Redis..."
for i in {1..30}; do
    if redis-cli -h 127.0.0.1 ping > /dev/null 2>&1; then
        echo "Redis ready"
        break
    fi
    sleep 1
done

# Setup database if needed
INIT_MARKER="/var/lib/postgresql/data/.defectdojo_initialized"
if [ ! -f "${INIT_MARKER}" ]; then
    echo "First run - initializing DefectDojo..."
    
    # Create database
    su - postgres -c "psql -h 127.0.0.1" <<-EOSQL || true
        CREATE DATABASE defectdojo;
        CREATE USER defectdojo WITH PASSWORD 'defectdojo';
        GRANT ALL PRIVILEGES ON DATABASE defectdojo TO defectdojo;
        ALTER USER defectdojo CREATEDB;
EOSQL
    
    # Grant permissions
    su - postgres -c "psql -h 127.0.0.1 -d defectdojo" <<-EOSQL
        GRANT ALL ON SCHEMA public TO defectdojo;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO defectdojo;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO defectdojo;
EOSQL
    
    # Run migrations
    cd /app
    python manage.py migrate --noinput
    python manage.py collectstatic --noinput --clear
    
    # Create superuser
    DJANGO_SUPERUSER_PASSWORD="${DD_ADMIN_PASSWORD}" python manage.py createsuperuser --noinput --username "${DD_ADMIN_USER}" --email "${DD_ADMIN_MAIL}" || true
    
    # Load fixtures
    python manage.py loaddata initial_banner_conf || true
    python manage.py loaddata initial_system_settings || true
    python manage.py loaddata product_type || true
    python manage.py loaddata test_type || true
    
    touch "${INIT_MARKER}"
    echo "✓ Initialization complete"
fi

# Stop background supervisord
kill $SUPERVISOR_PID
wait $SUPERVISOR_PID || true
sleep 2

# Clean up again
rm -f /var/lib/postgresql/data/postmaster.pid

echo "Starting services..."
exec /usr/bin/supervisord -c /etc/supervisord.conf