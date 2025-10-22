#!/bin/bash
set -e

echo "Starting DefectDojo initialization..."

# Clean up any stale PostgreSQL lock files
rm -f /var/lib/postgresql/data/postmaster.pid
rm -f /run/postgresql/.s.PGSQL.5432
rm -f /run/postgresql/.s.PGSQL.5432.lock

# Initialize PostgreSQL if not already initialized
if [ ! -f /var/lib/postgresql/data/PG_VERSION ]; then
    echo "Initializing PostgreSQL database..."
    chown -R postgres:postgres /var/lib/postgresql/data
    chmod 0700 /var/lib/postgresql/data
    su - postgres -c "/usr/lib/postgresql/*/bin/initdb -D /var/lib/postgresql/data"
fi

# Configure PostgreSQL to listen on localhost
if ! grep -q "host all all 127.0.0.1/32 trust" /var/lib/postgresql/data/pg_hba.conf; then
    echo "host all all 127.0.0.1/32 trust" >> /var/lib/postgresql/data/pg_hba.conf
fi
if ! grep -q "listen_addresses" /var/lib/postgresql/data/postgresql.conf; then
    echo "listen_addresses = '127.0.0.1'" >> /var/lib/postgresql/data/postgresql.conf
fi

# Start PostgreSQL temporarily to set it up
su - postgres -c "/usr/lib/postgresql/*/bin/postgres -D /var/lib/postgresql/data" &
PG_PID=$!

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to start..."
sleep 5
for i in {1..30}; do
    if su - postgres -c "pg_isready -h 127.0.0.1" > /dev/null 2>&1; then
        echo "PostgreSQL is ready"
        break
    fi
    echo "Waiting for PostgreSQL... ($i/30)"
    sleep 2
done

# Create database and user if not exists
echo "Setting up database..."
su - postgres -c "psql -h 127.0.0.1" <<-EOSQL
    SELECT 'CREATE DATABASE defectdojo' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'defectdojo')\gexec
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'defectdojo') THEN
            CREATE USER defectdojo WITH PASSWORD 'defectdojo';
        END IF;
    END
    \$\$;
    GRANT ALL PRIVILEGES ON DATABASE defectdojo TO defectdojo;
    ALTER USER defectdojo CREATEDB;
EOSQL

# Grant schema permissions on the defectdojo database
su - postgres -c "psql -h 127.0.0.1 -d defectdojo" <<-EOSQL
    GRANT ALL ON SCHEMA public TO defectdojo;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO defectdojo;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO defectdojo;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO defectdojo;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO defectdojo;
EOSQL

# Set environment variables for DefectDojo
export DD_DATABASE_URL="postgresql://defectdojo:defectdojo@127.0.0.1:5432/defectdojo"
export DD_CELERY_BROKER_URL="redis://127.0.0.1:6379/0"
export DD_SECRET_KEY="${DD_SECRET_KEY:-zzz-change-this-in-production-zzz}"
export DD_CREDENTIAL_AES_256_KEY="${DD_CREDENTIAL_AES_256_KEY:-yyy-change-this-credential-key-yyy}"
export DD_ALLOWED_HOSTS="${DD_ALLOWED_HOSTS:-*}"
export DD_DEBUG="${DD_DEBUG:-False}"
export DD_ADMIN_USER="${DD_ADMIN_USER:-admin}"
export DD_ADMIN_PASSWORD="${DD_ADMIN_PASSWORD:-admin}"
export DD_ADMIN_MAIL="${DD_ADMIN_MAIL:-admin@defectdojo.local}"
export DD_INITIALIZE="${DD_INITIALIZE:-true}"

# Start Redis for migrations
echo "Starting Redis..."
/usr/bin/redis-server --daemonize yes --bind 127.0.0.1 --port 6379

# Run Django migrations and initialization
INIT_MARKER="/var/lib/postgresql/data/.defectdojo_initialized"

if [ "${DD_INITIALIZE}" = "true" ] && [ ! -f "${INIT_MARKER}" ]; then
    echo "Running Django migrations and initialization (this may take several minutes)..."
    cd /app
    
    # Run migrations with verbose output
    echo "Step 1/4: Running database migrations..."
    python manage.py migrate --noinput --verbosity 1
    
    if [ $? -ne 0 ]; then
        echo "ERROR: Migrations failed!"
        exit 1
    fi
    
    echo "Step 2/4: Collecting static files..."
    python manage.py collectstatic --noinput --clear
    
    echo "Step 3/4: Creating superuser..."
    DJANGO_SUPERUSER_PASSWORD="${DD_ADMIN_PASSWORD}" python manage.py createsuperuser --noinput --username "${DD_ADMIN_USER}" --email "${DD_ADMIN_MAIL}" 2>&1 || echo "Superuser already exists"
    
    echo "Step 4/4: Loading initial data fixtures..."
    python manage.py loaddata initial_banner_conf 2>&1 || echo "Banner conf skipped"
    python manage.py loaddata initial_system_settings 2>&1 || echo "System settings skipped"
    python manage.py loaddata product_type 2>&1 || echo "Product type skipped"
    python manage.py loaddata test_type 2>&1 || echo "Test type skipped"
    python manage.py loaddata development_environment 2>&1 || echo "Dev environment skipped"
    python manage.py loaddata system_settings 2>&1 || echo "System settings skipped"
    python manage.py loaddata benchmark_type 2>&1 || echo "Benchmark type skipped"
    python manage.py loaddata benchmark_category 2>&1 || echo "Benchmark category skipped"
    python manage.py loaddata benchmark_requirement 2>&1 || echo "Benchmark requirement skipped"
    python manage.py loaddata language_type 2>&1 || echo "Language type skipped"
    python manage.py loaddata objects_review 2>&1 || echo "Objects review skipped"
    python manage.py loaddata regulation 2>&1 || echo "Regulation skipped"
    python manage.py loaddata initial_surveys 2>&1 || echo "Surveys skipped"
    
    # Mark as initialized
    touch "${INIT_MARKER}"
    echo "✓ DefectDojo initialization completed successfully!"
elif [ -f "${INIT_MARKER}" ]; then
    echo "DefectDojo already initialized, skipping setup..."
    cd /app
    # Still collect static files on restart
    echo "Collecting static files..."
    python manage.py collectstatic --noinput --clear 2>&1 || echo "Static files collection skipped"
fi

# Stop temporary services
echo "Stopping temporary services..."
if [ -n "$PG_PID" ]; then
    kill -TERM $PG_PID 2>/dev/null || true
    # Wait for PostgreSQL to shut down gracefully
    for i in {1..10}; do
        if ! kill -0 $PG_PID 2>/dev/null; then
            break
        fi
        sleep 1
    done
    # Force kill if still running
    kill -9 $PG_PID 2>/dev/null || true
    wait $PG_PID 2>/dev/null || true
fi

redis-cli shutdown 2>/dev/null || true
sleep 2

# Clean up lock files again before starting supervisord
rm -f /var/lib/postgresql/data/postmaster.pid
rm -f /run/postgresql/.s.PGSQL.5432
rm -f /run/postgresql/.s.PGSQL.5432.lock

echo "===================================================================="
echo "Initialization complete. Starting all services via supervisord..."
echo "===================================================================="

# Execute the command passed to the script (supervisord)
exec "$@"