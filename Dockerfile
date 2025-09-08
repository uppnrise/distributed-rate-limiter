# Multi-stage Docker build for Distributed Rate Limiter
# Build stage
FROM eclipse-temurin:21-jdk AS build

# Set working directory
WORKDIR /app

# Copy the entire project
COPY . .

# Make mvnw executable and build the application
RUN chmod +x mvnw && ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre AS runtime

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
# Use a different UID/GID to avoid conflicts with existing users
RUN groupadd -g 1001 appuser && \
    useradd -d /app -g appuser -u 1001 appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Configure JVM options for containerized environment
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Start the application with graceful shutdown
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]