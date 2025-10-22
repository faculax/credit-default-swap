#!/bin/bash
# Health check script for Render
# Returns 0 (success) only when all services are fully operational

set -e

# Check if initialization is complete
if [ ! -f /tmp/init_complete ]; then
    echo "UNHEALTHY: Initialization not complete"
    exit 1
fi

# Check PostgreSQL
if ! pg_isready -h 127.0.0.1 -U postgres > /dev/null 2>&1; then
    echo "UNHEALTHY: PostgreSQL not responding"
    exit 1
fi

# Check Redis
if ! redis-cli -h 127.0.0.1 ping 2>&1 | grep -q "PONG"; then
    echo "UNHEALTHY: Redis not responding"
    exit 1
fi

# Check uwsgi/Django application
if ! curl -s -f -o /dev/null -w "%{http_code}" http://127.0.0.1:3031/login | grep -q "200\|302"; then
    echo "UNHEALTHY: Django application not responding"
    exit 1
fi

# Check nginx
if ! curl -s -f -o /dev/null http://127.0.0.1:10000/login; then
    echo "UNHEALTHY: Nginx not responding"
    exit 1
fi

# All checks passed
echo "HEALTHY: All services operational"
exit 0
