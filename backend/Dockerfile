# ---------- Stage 1: Build ----------
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy only what's needed to cache dependencies
COPY pom.xml .
COPY src ./src

# Build the app (creates the JAR)
RUN mvn clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy the built JAR from the previous stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port 80, since we will override to this port
EXPOSE 80

# Set environment variable to override Spring Boot server port to 80
ENV SERVER_PORT=80

# Set Risk Engine URL for Render deployment (routes via gateway or direct to risk-engine)
ENV RISK_ENGINE_URL=https://credit-default-swap-risk-engine.onrender.com

# Set environment variables for Render deployment
ENV SPRING_DATASOURCE_URL='jdbc:postgresql://dpg-d38q7j7fte5s73canqjg-a:5432/fund_smith_postgres?sslmode=require'
ENV SPRING_DATASOURCE_USERNAME=fund_smith_postgres_user
ENV SPRING_DATASOURCE_PASSWORD=w3A3gwvbcHGc8mHJQO5Fu40D50k5crp5
ENV SERVER_PORT=80

# Flyway configuration for public schema
ENV SPRING_FLYWAY_ENABLED=true
ENV SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
ENV SPRING_FLYWAY_DEFAULT_SCHEMA=public
ENV SPRING_FLYWAY_SCHEMAS=public
ENV SPRING_FLYWAY_OUT_OF_ORDER=true
ENV SPRING_FLYWAY_IGNORE_MISSING_MIGRATIONS=true
ENV SPRING_FLYWAY_IGNORE_IGNORED_MIGRATIONS=true
ENV SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false

# JPA/Hibernate configuration
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=none
ENV SPRING_JPA_HIBERNATE_DEFAULT_SCHEMA=public

# Debug logging for troubleshooting
ENV LOGGING_LEVEL_FLYWAY=DEBUG
ENV LOGGING_LEVEL_JDBC=DEBUG
ENV LOGGING_LEVEL_APP=DEBUG

# Run the app
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]

