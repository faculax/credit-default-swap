###############################
# Build Stage (Java + Maven)
###############################
# Use non-alpine (glibc) base so the JRE we copy is compatible with the ORE Debian base image.
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests -Dmaven.test.skip=true package

###############################
# Runtime Stage - Layer on top of pre-built ORE
###############################
FROM faculax/ore:v1.8.13.1 AS runtime

# Copy the full Temurin JRE from the build image (avoids apt-get and flaky layer)
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=build /opt/java/openjdk /opt/java/openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

# Lightweight curl install only if missing (defensive; ignore failure)
RUN (command -v curl >/dev/null 2>&1 || (apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*)) || echo "curl install skipped"

WORKDIR /app

# Copy built artifact (avoid hardcoding version via glob)
COPY --from=build /workspace/target/risk-engine-*.jar risk-engine.jar

# --- Risk Engine runtime configuration ---
# Updated to use real ORE binaries from the base image
ENV RISK_IMPL=ORE \
	ORE_BINARY_PATH=/usr/local/bin/ore \
	ORE_CONFIG_PATH=/app/ore/config/ore_cds.xml \
	ORE_WORK_DIR=/tmp/ore-work \
	ORE_TIMEOUT=60 \
	ORE_WARMUP_TIMEOUT=30 \
	ORE_RESTART_DELAY=5 \
	ORE_MAX_RESTARTS=3 \
	LD_LIBRARY_PATH=/usr/local/lib \
	BACKEND_BASE_URL=https://credit-default-swap-backend.onrender.com

 # Copy verification script (optional)
COPY scripts/verify-ore.sh /app/scripts/verify-ore.sh
RUN chmod +x /app/scripts/verify-ore.sh || true

# Create ORE configuration directory and working directory
RUN mkdir -p /app/ore/config /app/ore/market-data /tmp/ore-work/input /tmp/ore-work/output/logs

# Copy ORE config files (these should be in ore-resources directory within risk-engine)
COPY ore-resources/config/*.xml /app/ore/config/
COPY ore-resources/config/*.txt /app/ore/config/
COPY ore-resources/market-data/*.txt /app/ore/market-data/

# Fallback: copy src/main/resources if needed for additional files
COPY src/main/resources/ore/input /tmp/ore-work/input

# Verify ORE installation and find the correct binary path (non-fatal)
RUN ldconfig && /app/scripts/verify-ore.sh || echo "ORE verification completed with warnings"

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl -f http://localhost:8082/api/risk/health || exit 1

# Add entrypoint for auto-detecting ORE binary
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

ENTRYPOINT ["/app/docker-entrypoint.sh"]