#!/bin/bash
set -e

echo "==> Starting DefectDojo all-in-one initialization..."

# Set default environment variables
export DD_SECRET_KEY="${DD_SECRET_KEY:-zzz-change-this-in-production-zzz}"
export DD_CREDENTIAL_AES_256_KEY="${DD_CREDENTIAL_AES_256_KEY:-yyy-change-this-credential-key-yyy}"
export DD_ALLOWED_HOSTS="${DD_ALLOWED_HOSTS:-*}"
export DD_DEBUG="${DD_DEBUG:-False}"
export DD_ADMIN_USER="${DD_ADMIN_USER:-admin}"
export DD_ADMIN_PASSWORD="${DD_ADMIN_PASSWORD:-admin}"
export DD_ADMIN_MAIL="${DD_ADMIN_MAIL:-admin@defectdojo.local}"
export DD_INITIALIZE="${DD_INITIALIZE:-true}"

# Generate secure database password if not set
export DB_PASSWORD="${DB_PASSWORD:-$(openssl rand -base64 32 | tr -d '=+/' | cut -c1-32)}"

# Set database connection details
export DD_DATABASE_URL="postgresql://defectdojo:${DB_PASSWORD}@127.0.0.1:5432/defectdojo"
export DD_CELERY_BROKER_URL="redis://127.0.0.1:6379/0"

# Export for supervisord programs
echo "export DD_DATABASE_URL='${DD_DATABASE_URL}'" > /etc/environment.dd
echo "export DD_CELERY_BROKER_URL='${DD_CELERY_BROKER_URL}'" >> /etc/environment.dd
echo "export DD_SECRET_KEY='${DD_SECRET_KEY}'" >> /etc/environment.dd
echo "export DD_CREDENTIAL_AES_256_KEY='${DD_CREDENTIAL_AES_256_KEY}'" >> /etc/environment.dd
echo "export DD_ALLOWED_HOSTS='${DD_ALLOWED_HOSTS}'" >> /etc/environment.dd
echo "export DD_CSRF_TRUSTED_ORIGINS='${DD_CSRF_TRUSTED_ORIGINS}'" >> /etc/environment.dd
echo "export DD_SESSION_COOKIE_SECURE='${DD_SESSION_COOKIE_SECURE:-True}'" >> /etc/environment.dd
echo "export DD_CSRF_COOKIE_SECURE='${DD_CSRF_COOKIE_SECURE:-True}'" >> /etc/environment.dd
echo "export DD_DEBUG='${DD_DEBUG}'" >> /etc/environment.dd
echo "export DD_STATIC_ROOT='/app/static'" >> /etc/environment.dd
echo "export DD_STATIC_URL='/static/'" >> /etc/environment.dd
echo "export DD_MEDIA_ROOT='/app/media'" >> /etc/environment.dd
echo "export DD_MEDIA_URL='/media/'" >> /etc/environment.dd
echo "export C_FORCE_ROOT='true'" >> /etc/environment.dd

chmod 600 /etc/environment.dd

# Function to wait for service with retry
wait_for_service() {
    local service=$1
    local check_command=$2
    local max_attempts=60
    local attempt=1
    
    echo "==> Waiting for ${service}..."
    while [ $attempt -le $max_attempts ]; do
        if eval "$check_command" > /dev/null 2>&1; then
            echo "==> ${service} is ready!"
            return 0
        fi
        echo "    Waiting for ${service}... (${attempt}/${max_attempts})"
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo "ERROR: ${service} failed to start after ${max_attempts} attempts"
    return 1
}

# Clean up any stale lock files
echo "==> Cleaning up stale lock files..."
rm -f /app/pgdata/postmaster.pid
rm -f /run/postgresql/.s.PGSQL.5432*

# Ensure directories exist with proper permissions
mkdir -p /app/pgdata /app/logs /app/backups /run/postgresql
chown -R postgres:postgres /app/pgdata /app/logs /run/postgresql
chmod 0700 /app/pgdata
chmod 0755 /app/logs

# Initialize PostgreSQL if needed
if [ ! -f /app/pgdata/PG_VERSION ]; then
    echo "==> Initializing PostgreSQL database cluster..."
    
    # Use C locale for maximum compatibility
    su - postgres -c "/usr/lib/postgresql/15/bin/initdb -D /app/pgdata -E UTF8 --locale=C" || {
        echo "ERROR: PostgreSQL initialization failed"
        exit 1
    }
    
    # Configure PostgreSQL
    echo "==> Configuring PostgreSQL..."
    cat >> /app/pgdata/postgresql.conf <<EOF
listen_addresses = '127.0.0.1'
port = 5432
max_connections = 100
shared_buffers = 128MB
EOF
    
    cat > /app/pgdata/pg_hba.conf <<EOF
# TYPE  DATABASE        USER            ADDRESS                 METHOD
local   all             all                                     trust
host    all             all             127.0.0.1/32            trust
host    all             all             ::1/128                 trust
EOF
    
    echo "==> PostgreSQL initialization complete"
fi

# Start PostgreSQL
echo "==> Starting PostgreSQL..."
su - postgres -c "/usr/lib/postgresql/15/bin/pg_ctl -D /app/pgdata -l /app/logs/postgresql.log start" || {
    echo "ERROR: Failed to start PostgreSQL"
    cat /app/logs/postgresql.log
    exit 1
}

# Wait for PostgreSQL
wait_for_service "PostgreSQL" "su - postgres -c 'pg_isready -h 127.0.0.1'" || exit 1

# Create database and user on first run
if [ ! -f /app/pgdata/.db_initialized ]; then
    echo "==> Creating database and user..."
    su - postgres -c "psql -h 127.0.0.1" <<-EOSQL
    CREATE USER defectdojo WITH PASSWORD '${DB_PASSWORD}';
    CREATE DATABASE defectdojo OWNER defectdojo;
    GRANT ALL PRIVILEGES ON DATABASE defectdojo TO defectdojo;
    ALTER USER defectdojo CREATEDB;
EOSQL
    
    if [ $? -ne 0 ]; then
        echo "ERROR: Database creation failed"
        exit 1
    fi
    
    # Grant schema permissions
    su - postgres -c "psql -h 127.0.0.1 -d defectdojo" <<-EOSQL
    GRANT ALL ON SCHEMA public TO defectdojo;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO defectdojo;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO defectdojo;
EOSQL
    
    touch /app/pgdata/.db_initialized
    echo "==> Database created successfully"
fi

# Start Redis
echo "==> Starting Redis..."
/usr/bin/redis-server --daemonize yes --bind 127.0.0.1 --port 6379 --loglevel notice || {
    echo "ERROR: Failed to start Redis"
    exit 1
}

wait_for_service "Redis" "redis-cli -h 127.0.0.1 ping | grep -q PONG" || exit 1

# Ensure static and media directories exist
mkdir -p /app/static /app/media
chmod 0755 /app/static /app/media

# Set Django environment variables for static files BEFORE running collectstatic
export DD_STATIC_ROOT='/app/static'
export DD_MEDIA_ROOT='/app/media'
export DD_STATIC_URL='/static/'
export DD_MEDIA_URL='/media/'

# Always collect static files on startup
echo "==> Collecting static files..."
cd /app

python3 manage.py collectstatic --noinput --clear || {
    echo "ERROR: Static file collection failed"
    exit 1
}

# Link node_modules packages to static (DefectDojo's JS dependencies)
echo "==> Checking for node_modules..."
if [ -d /app/components/node_modules ]; then
    echo "Found /app/components/node_modules"
    
    # List first 10 packages for debugging
    echo "Sample packages in node_modules:"
    ls -1 /app/components/node_modules | head -10
    
    echo ""
    echo "==> Linking node_modules packages to static directory..."
    
    # Use ls to list directories, then iterate
    ls -1 /app/components/node_modules | while IFS= read -r package_name; do
        # Check if it's a directory and not .bin
        if [ -d "/app/components/node_modules/$package_name" ] && [ "$package_name" != ".bin" ]; then
            target="/app/static/$package_name"
            source="/app/components/node_modules/$package_name"
            
            # Remove existing if present
            rm -rf "$target" 2>/dev/null || true
            
            # Create symlink
            if ln -sf "$source" "$target"; then
                echo "  ✓ $package_name"
            fi
        fi
    done
    
    # Count total symlinks created
    total_links=$(find /app/static -maxdepth 1 -type l 2>/dev/null | wc -l)
    echo ""
    echo "Node modules linking complete"
    echo "Total symlinks in static: $total_links"
else
    echo "WARNING: /app/components/node_modules directory not found"
    echo "Checking /app/components contents:"
    ls -la /app/components/ 2>/dev/null || echo "Directory doesn't exist"
fi

# Verify static files were collected
echo "==> Verifying static files..."
echo "DD_STATIC_ROOT is set to: ${DD_STATIC_ROOT}"
echo "DD_STATIC_URL is set to: ${DD_STATIC_URL}"

if [ -d /app/static ]; then
    echo "Static directory exists!"
    echo "Contents of /app/static:"
    ls -lah /app/static | head -20
    echo ""
    echo "Looking for jquery:"
    find /app/static -name "jquery.js" -o -name "jquery.min.js" 2>/dev/null | head -5
    echo ""
    echo "Total files collected: $(find /app/static -type f 2>/dev/null | wc -l)"
    echo "Directory permissions:"
    ls -ld /app/static
    
    # Fix permissions to ensure nginx can read
    echo "Fixing permissions for nginx..."
    chmod -R 755 /app/static
    chown -R root:root /app/static
    
    # Test specific file paths that the browser is requesting
    echo ""
    echo "Verifying browser-requested file paths:"
    for file in "jquery/dist/jquery.js" "jszip/dist/jszip.min.js" "moment/min/moment.min.js" "bootstrap/dist/css/bootstrap.min.css"; do
        if [ -f "/app/static/$file" ] || [ -L "/app/static/$file" ]; then
            echo "✓ $file"
        else
            echo "✗ $file MISSING"
        fi
    done
    
    # List all top-level directories in static (including symlinks)
    echo ""
    echo "Static directory contents (with symlinks):"
    ls -lah /app/static/ | head -30
else
    echo "ERROR: Static directory not found at /app/static!"
    echo "Checking what exists in /app:"
    ls -la /app/ | grep -E "(static|media)"
fi

# Ensure media directory exists with proper permissions
mkdir -p /app/media
chmod -R 755 /app/media
chown -R root:root /app/media

# Test nginx configuration
echo ""
echo "==> Testing nginx configuration..."
nginx -t || {
    echo "ERROR: nginx configuration test failed"
    exit 1
}

echo "==> Static file collection complete"

# Run Django initialization on first run
if [ "${DD_INITIALIZE}" = "true" ] && [ ! -f /app/pgdata/.django_initialized ]; then
    echo "==> Running Django migrations..."
    
    python3 manage.py migrate --noinput || {
        echo "ERROR: Migrations failed"
        exit 1
    }
    
    echo "==> Loading initial data..."
    for fixture in initial_banner_conf initial_system_settings product_type test_type \
                   development_environment system_settings benchmark_type benchmark_category \
                   benchmark_requirement language_type objects_review regulation; do
        python3 manage.py loaddata $fixture || echo "Warning: Failed to load $fixture"
    done
    
    echo "==> Creating superuser..."
    python3 manage.py shell <<PYEOF || echo "Warning: Superuser creation failed"
from django.contrib.auth.models import User
if not User.objects.filter(username='${DD_ADMIN_USER}').exists():
    User.objects.create_superuser('${DD_ADMIN_USER}', '${DD_ADMIN_MAIL}', '${DD_ADMIN_PASSWORD}')
    print('Superuser created')
else:
    print('Superuser already exists')
PYEOF
    
    python3 manage.py loaddata initial_surveys || echo "Warning: Surveys already loaded"
    
    touch /app/pgdata/.django_initialized
    echo "==> Django initialization complete"
fi

# Create flag file for health checks
echo "ready" > /tmp/init_complete

echo "==> Initialization complete. Starting supervisord..."

# Execute supervisord
exec "$@"