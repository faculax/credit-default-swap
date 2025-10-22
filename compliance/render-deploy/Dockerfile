# Use Ubuntu base to build self-contained DefectDojo
FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1 \
    DD_VERSION=2.36.0 \
    DOJO_ROOT=/opt/django-DefectDojo

# Install system dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    python3.10 \
    python3-pip \
    python3-dev \
    build-essential \
    libpq-dev \
    postgresql-14 \
    postgresql-client-14 \
    redis-server \
    nginx \
    supervisor \
    git \
    curl \
    libssl-dev \
    libffi-dev \
    libxml2-dev \
    libxslt1-dev \
    libjpeg-dev \
    zlib1g-dev \
    wkhtmltopdf \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Create application directory
RUN mkdir -p ${DOJO_ROOT} /var/log/supervisor /run/postgresql /run/nginx

# Clone DefectDojo
RUN git clone --branch ${DD_VERSION} --depth 1 https://github.com/DefectDojo/django-DefectDojo.git ${DOJO_ROOT}

# Install Python dependencies
WORKDIR ${DOJO_ROOT}
RUN pip3 install --no-cache-dir -r requirements.txt && \
    pip3 install --no-cache-dir uwsgi psycopg2-binary

# Create media and static directories
RUN mkdir -p ${DOJO_ROOT}/media ${DOJO_ROOT}/static

# Set up PostgreSQL to NOT use Render's disk mount (use tmpfs for data, persistent for backups)
RUN mkdir -p /app/pgdata /app/backups /app/logs

# Copy configuration files
COPY supervisord.conf /etc/supervisor/supervisord.conf
COPY nginx.conf /etc/nginx/sites-available/default
COPY uwsgi.ini ${DOJO_ROOT}/uwsgi.ini
COPY init-db.sh /docker-entrypoint.sh
COPY health-check.sh /health-check.sh

RUN chmod +x /docker-entrypoint.sh /health-check.sh && \
    rm -f /etc/nginx/sites-enabled/default && \
    ln -s /etc/nginx/sites-available/default /etc/nginx/sites-enabled/default

# Set permissions
RUN chown -R postgres:postgres /app/pgdata /run/postgresql && \
    chmod 0700 /app/pgdata

# Expose port
EXPOSE 10000

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/supervisord.conf"]