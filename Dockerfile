# Multi-stage Dockerfile for kdb-url-shortener
# Builder stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

# Only copy gradle wrapper and build files first for better caching
COPY gradle ./gradle
COPY gradlew gradlew.bat ./
COPY settings.gradle.kts build.gradle.kts gradle.properties ./

# Pre-download dependencies
RUN ./gradlew --version && ./gradlew --no-daemon clean build -x test || true

# Now copy the source
COPY src ./src

# Build installable distribution (binaries go to build/install/<project>)
RUN ./gradlew --no-daemon clean installDist

# Runner stage
FROM eclipse-temurin:21-jre AS runner

# Default environment as requested
ENV APP_ENV=prod \
    JAVA_OPTS="" \
    APP_RUN_MIGRATIONS="false"

# Create app directory
WORKDIR /opt/app

# Copy the installed distribution from builder
# Project name is defined in settings.gradle.kts as kdb-url-shortener
COPY --from=builder /workspace/build/install/kdb-url-shortener /opt/app

# Expose service port
EXPOSE 8080

# Health by container orchestrator; app provides /health endpoint

# ENTRYPOINT to run the app; allow JAVA_OPTS to inject JVM args if needed
ENTRYPOINT ["/bin/sh", "-c", "exec ./bin/kdb-url-shortener $JAVA_OPTS"]
