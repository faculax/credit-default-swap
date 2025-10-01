###############################
# Build Stage
###############################
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

###############################
# Runtime Stage
###############################
FROM eclipse-temurin:21-jre

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy built artifact (avoid hardcoding version via glob)
COPY --from=build /workspace/target/risk-engine-*.jar risk-engine.jar

# --- Risk Engine runtime configuration ---
# Default to ORE implementation inside the container.
# Can be overridden at runtime: `docker run -e RISK_IMPL=STUB ...` if needed.
ENV RISK_IMPL=ORE \
	ORE_BINARY_PATH=/app/ore/bin/ore \
	ORE_CONFIG_PATH=/app/ore/config/ore.xml \
	ORE_WORK_DIR=/tmp/ore-work \
	ORE_TIMEOUT=10 \
	ORE_WARMUP_TIMEOUT=30 \
	ORE_RESTART_DELAY=5 \
	ORE_MAX_RESTARTS=3

# Create placeholder ORE structure so process manager doesn't immediately fail if binary not mounted
RUN mkdir -p /app/ore/bin /app/ore/config /tmp/ore-work \
	&& echo '#!/bin/sh' > /app/ore/bin/ore \
	&& echo 'echo "ORE placeholder daemon (no real calc) starting"' >> /app/ore/bin/ore \
	&& echo 'while true; do' >> /app/ore/bin/ore \
	&& echo '  if read -t 1 line; then' >> /app/ore/bin/ore \
	&& echo '    # Consume any burst of stdin (XML payload) without acting' >> /app/ore/bin/ore \
	&& echo '    while read -t 0.1 more; do :; done' >> /app/ore/bin/ore \
	&& echo '    echo "[placeholder] Ignoring calc request at $(date -u +%Y-%m-%dT%H:%M:%SZ)"' >> /app/ore/bin/ore \
	&& echo '  fi' >> /app/ore/bin/ore \
	&& echo '  sleep 1' >> /app/ore/bin/ore \
	&& echo 'done' >> /app/ore/bin/ore \
	&& chmod +x /app/ore/bin/ore \
	&& printf '%s\n' "<?xml version=\"1.0\"?><ORE><Setup><Parameter name=\"logLevel\">2</Parameter></Setup></ORE>" > /app/ore/config/ore.xml

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl -f http://localhost:8082/api/risk/health || exit 1

ENTRYPOINT ["java", "-jar", "risk-engine.jar"]