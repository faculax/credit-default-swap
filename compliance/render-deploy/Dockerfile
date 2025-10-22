FROM defectdojo/defectdojo-django:latest

USER root

# Install PostgreSQL, Redis, nginx, and supervisor (Debian-based)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    postgresql \
    postgresql-client \
    redis-server \
    nginx \
    supervisor && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create necessary directories
RUN mkdir -p /var/log/supervisor \
    /run/postgresql \
    /run/nginx \
    /var/lib/postgresql/data \
    /app/media

# Create postgres user if it doesn't exist
RUN useradd -r -s /bin/bash postgres || true

# Set permissions for PostgreSQL
RUN chown -R postgres:postgres /var/lib/postgresql/data /run/postgresql && \
    chmod 0700 /var/lib/postgresql/data

# Initialize PostgreSQL database as postgres user
RUN su postgres -c "initdb -D /var/lib/postgresql/data"

# Copy configuration files
COPY supervisord.conf /etc/supervisord.conf
COPY nginx.conf /etc/nginx/http.d/default.conf
COPY init-db.sh /docker-entrypoint.sh

RUN chmod +x /docker-entrypoint.sh

# Expose port (Render uses PORT environment variable, defaults to 10000)
EXPOSE 10000

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]