#!/bin/bash
set -e

echo "Starting DefectDojo initialization..."

# Initialize PostgreSQL if not already initialized
if [ ! -f /var/lib/postgresql/data/PG_VERSION ]; then
    echo "Initializing PostgreSQL database..."
    chown -R postgres:postgres /var/lib/postgresql/data
    chmod 0700 /var/lib/postgresql/data
    su - postgres -c "/usr/lib/postgresql/*/bin/initdb -D /var/lib/postgresql/data"
fi

# Configure PostgreSQL to listen on localhost
echo "host all all 127.0.0.1/32 trust" >> /var/lib/postgresql/data/pg_hba.conf
echo "listen_addresses = '127.0.0.1'" >> /var/lib/postgresql/data/postgresql.conf

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

# Stop PostgreSQL
kill $PG_PID
wait $PG_PID || true

# Set environment variables for DefectDojo
export DD_DATABASE_URL="postgresql://defectdojo:defectdojo@127.0.0.1:5432/defectdojo"
export DD_CELERY_BROKER_URL="redis://127.0.0.1:6379/0"
export DD_SECRET_KEY="${DD_SECRET_KEY:-zzz-change-this-in-production-zzz}"
export DD_CREDENTIAL_AES_256_KEY="${DD_CREDENTIAL_AES_256_KEY:-yyy-change-this-credential-key-yyy}"
export DD_ALLOWED_HOSTS="${DD_ALLOWED_HOSTS:-*}"
export DD_DEBUG="${DD_DEBUG:-False}"
export DD_ADMIN_USER="${DD_ADMIN_USER:-admin}"
export DD_ADMIN_PASSWORD="${DD_ADMIN_PASSWORD:-admin}"
export DD_INITIALIZE="${DD_INITIALIZE:-true}"

echo "Initialization complete. Starting services..."

# Execute the command passed to the script (supervisord)
exec "$@"