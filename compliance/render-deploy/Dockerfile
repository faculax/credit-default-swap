# Use official DefectDojo image as base and add PostgreSQL/Redis
FROM defectdojo/defectdojo-django:2.36.0

USER root

ENV DEBIAN_FRONTEND=noninteractive

# Install PostgreSQL, Redis, nginx, and supervisor
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    postgresql \
    postgresql-client \
    redis-server \
    nginx \
    supervisor \
    curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Create necessary directories
RUN mkdir -p /var/log/supervisor \
    /run/postgresql \
    /run/nginx \
    /app/pgdata \
    /app/backups \
    /app/logs

# Set DOJO_ROOT to match DefectDojo official image
ENV DOJO_ROOT=/app

# Copy configuration files
COPY supervisord.conf /etc/supervisor/supervisord.conf
COPY nginx.conf /etc/nginx/sites-available/default
COPY uwsgi.ini ${DOJO_ROOT}/uwsgi.ini
COPY init-db.sh /docker-entrypoint.sh
COPY health-check.sh /health-check.sh

RUN chmod +x /docker-entrypoint.sh /health-check.sh && \
    rm -f /etc/nginx/sites-enabled/default && \
    ln -s /etc/nginx/sites-available/default /etc/nginx/sites-enabled/default

# Set permissions for PostgreSQL
RUN chown -R postgres:postgres /app/pgdata /run/postgresql && \
    chmod 0700 /app/pgdata

# Expose port
EXPOSE 10000

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/supervisord.conf"]