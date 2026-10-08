# =============================================================================
# Stage 1: Build Stage (Multi-Module Maven Project)
# =============================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy only the POM files first to leverage Docker layer caching for dependencies
COPY pom.xml .
COPY module-core/pom.xml module-core/
COPY module-analysis/pom.xml module-analysis/
COPY module-ingestion/pom.xml module-ingestion/
COPY module-integration/pom.xml module-integration/
COPY module-migration/pom.xml module-migration/
COPY app-runner/pom.xml app-runner/

# Download dependencies offline (cache layer)
RUN mvn dependency:go-offline -B

# Copy the actual source code
COPY . .

# Build the application (skipping tests for production image generation)
RUN mvn clean package -DskipTests -B

# =============================================================================
# Stage 2: Minimal Runtime Stage
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: Create a non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the built fat JAR from the builder stage
# (Assuming app-runner is the main entry point module)
COPY --from=builder /build/app-runner/target/*.jar app.jar

# Security: Ensure the non-root user owns the app binary
RUN chown -R appuser:appgroup /app

# Switch to the non-root user
USER appuser

# Environment Variables
ENV PORT=8080
ENV SERVER_PORT=${PORT}
ENV SPRING_PROFILES_ACTIVE=prod

# Expose the configurable port
EXPOSE ${PORT}

# Application Health Check (Relies on spring-boot-starter-actuator)
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -q -O - http://localhost:${PORT}/actuator/health/liveness || exit 1

# Launch the application securely
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
