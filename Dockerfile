# Multi-stage Docker build for Distributed Rate Limiter
# Build stage
FROM eclipse-temurin:21.0.11_10-jdk AS build

# Set working directory
WORKDIR /app

# Copy the entire project
COPY . .

# Make mvnw executable and build the application
RUN chmod +x mvnw && ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21.0.11_10-jre-alpine-3.23 AS runtime

# Apply Alpine security fixes, then create a locked-down high-numbered system user.
RUN apk upgrade --no-cache && \
    addgroup -S -g 10001 appuser && \
    adduser -S -D -H -u 10001 -G appuser appuser

# Set working directory
WORKDIR /app

# Copy the built JAR with its final ownership, avoiding an extra writable layer.
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Configure JVM options for containerized environment
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Start the application with graceful shutdown
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
