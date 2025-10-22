# Use official DefectDojo image as base and add PostgreSQL/Redis
FROM defectdojo/defectdojo-django:2.36.0

USER root

ENV DEBIAN_FRONTEND=noninteractive

# Install Node.js 20.x (LTS), PostgreSQL, Redis, nginx, and supervisor
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    gnupg \
    ca-certificates \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends \
    nodejs \
    postgresql \
    postgresql-client \
    redis-server \
    nginx \
    supervisor \
    curl \
    locales \
    && npm install -g yarn \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8

# Create necessary directories with proper permissions
RUN mkdir -p /var/log/supervisor \
    /run/postgresql \
    /run/nginx \
    /app/pgdata \
    /app/backups \
    /app/logs \
    /app/static \
    /app/media && \
    chown -R postgres:postgres /app/pgdata /app/logs /run/postgresql && \
    chmod 0700 /app/pgdata && \
    chmod 0755 /app/logs /app/static /app/media

# Set DOJO_ROOT to match DefectDojo official image
ENV DOJO_ROOT=/app

# Install node_modules - DefectDojo frontend dependencies
# The official DefectDojo image expects these in /app/components/node_modules
WORKDIR /app/components
COPY package.json yarn.lock* ./
RUN yarn install --production --frozen-lockfile && \
    yarn cache clean

WORKDIR /app

# Copy configuration files
COPY supervisord.conf /etc/supervisor/supervisord.conf
COPY nginx.conf /etc/nginx/sites-available/default
COPY uwsgi.ini ${DOJO_ROOT}/uwsgi.ini
COPY init-db.sh /docker-entrypoint.sh
COPY health-check.sh /health-check.sh

RUN chmod +x /docker-entrypoint.sh /health-check.sh && \
    rm -f /etc/nginx/sites-enabled/default && \
    ln -s /etc/nginx/sites-available/default /etc/nginx/sites-enabled/default

# Expose port
EXPOSE 10000

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/supervisord.conf"]