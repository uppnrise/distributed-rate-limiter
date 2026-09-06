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
FROM eclipse-temurin:21.0.11_10-jre-ubi10-minimal AS runtime

# Set working directory
WORKDIR /app

# Copy the built JAR with its final ownership, avoiding an extra writable layer.
COPY --from=build --chown=10001:0 /app/target/*.jar app.jar

# Switch to non-root user
USER 10001

# Expose port
EXPOSE 8080

# Configure JVM options for containerized environment
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl --fail --silent --show-error http://localhost:8080/actuator/health > /dev/null || exit 1

# Start the application with graceful shutdown
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
