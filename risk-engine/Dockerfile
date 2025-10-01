FROM eclipse-temurin:21-jre

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/risk-engine-1.0.0.jar risk-engine.jar

EXPOSE 8082

CMD ["java", "-jar", "risk-engine.jar"]