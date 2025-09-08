# Docker Usage Guide

This guide explains how to run the Distributed Rate Limiter application using Docker.

## Prerequisites

- Docker installed and running
- Docker Compose v2+

## Quick Start

### 1. Using Docker Compose (Recommended)

Start the entire stack (application + Redis):

```bash
docker compose up
```

This will:
- Start a Redis container with persistence
- Build and start the application container
- Configure networking between services
- Set up health checks for both services

The application will be available at `http://localhost:8080`

### 2. Development Mode

Start only Redis for development:

```bash
# Start Redis
docker compose up redis -d

# Run application locally
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.data.redis.host=localhost
```

### 3. Production Deployment

For production, you can use the pre-built Docker image:

```bash
# Build the application image
docker build -t distributed-rate-limiter:latest .

# Start with docker-compose
docker compose up -d
```

## Configuration

### Environment Variables

The application supports the following environment variables for Docker:

```bash
# Redis Configuration
SPRING_DATA_REDIS_HOST=redis          # Redis hostname
SPRING_DATA_REDIS_PORT=6379           # Redis port
SPRING_DATA_REDIS_TIMEOUT=2000ms      # Connection timeout

# Application Configuration
SPRING_PROFILES_ACTIVE=docker         # Use Docker profile
SERVER_SHUTDOWN=graceful              # Enable graceful shutdown
SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=30s

# Rate Limiter Configuration
RATELIMITER_CAPACITY=10               # Default bucket capacity
RATELIMITER_REFILLRATE=2              # Default refill rate
```

### Profiles

- `default`: Uses embedded configuration
- `docker`: Optimized for containerized deployment
- `test`: For testing with Testcontainers

## Health Checks

The application includes built-in health checks:

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Expected response:
{
  "status": "UP",
  "components": {
    "redis": {"status": "UP", "details": {"version": "7.4.5"}},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

## Testing the Application

### Rate Limiting API

```bash
# Test rate limiting
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key":"user123","tokens":1}'

# Expected response:
{"key":"user123","tokensRequested":1,"allowed":true}
```

### Metrics Endpoint

```bash
# Get application metrics
curl http://localhost:8080/metrics

# Expected response:
{
  "totalAllowedRequests": 0,
  "totalDeniedRequests": 0,
  "redisConnected": true,
  "keyMetrics": {}
}
```

## Docker Image Details

### Multi-stage Build

The Dockerfile uses a multi-stage build:

1. **Build Stage**: Uses `eclipse-temurin:21-jdk` to compile the application
2. **Runtime Stage**: Uses `eclipse-temurin:21-jre` for smaller image size

### Security Features

- Runs as non-root user (`appuser:1000`)
- Minimal runtime dependencies
- Health check integrated
- Proper signal handling for graceful shutdown

### Resource Configuration

- JVM Memory: 256MB initial, 512MB max
- Health check: 30s interval, 3 retries
- Startup period: 40s (to allow dependency startup)

## Troubleshooting

### Common Issues

1. **Connection refused errors**
   ```bash
   # Check if Redis is running
   docker compose ps
   
   # Check logs
   docker compose logs redis
   docker compose logs app
   ```

2. **Health check failures**
   ```bash
   # Check application logs
   docker compose logs app
   
   # Manual health check
   docker compose exec app curl -f http://localhost:8080/actuator/health
   ```

3. **Port conflicts**
   ```bash
   # Use different ports
   docker compose up -p 8081:8080
   ```

4. **Docker build fails with network errors**
   
   If you encounter errors like "Failed to fetch https://repo.maven.apache.org/maven2/..." during Docker build:
   
   ```bash
   # Option 1: Build JAR locally first
   ./mvnw package -DskipTests -B
   docker build -t distributed-rate-limiter:latest .
   
   # Option 2: Use Docker build with network mode
   docker build --network=host -t distributed-rate-limiter:latest .
   
   # Option 3: Use Docker Compose which handles networking
   docker compose build
   ```
   
   This is common in restricted network environments where Maven repositories might be blocked during the Docker build process.

5. **User/Group conflicts in Docker**
   
   If you see "GID '1000' already exists" error during build:
   
   The Dockerfile has been updated to use UID/GID 1001 to avoid conflicts with existing system users. This should resolve automatically with the current Dockerfile.

### Debugging

```bash
# Interactive debugging
docker compose exec app bash

# View logs
docker compose logs -f app

# Check container resources
docker stats
```

## Data Persistence

Redis data is persisted in a Docker volume:

```bash
# List volumes
docker volume ls

# Backup Redis data
docker compose exec redis redis-cli BGSAVE

# Volume location
docker volume inspect distributed-rate-limiter_redis-data
```

## Scaling

To scale the application:

```bash
# Scale application containers
docker compose up --scale app=3

# Note: Redis should not be scaled (use Redis Cluster for HA)
```

## Production Considerations

1. **Resource Limits**: Set appropriate CPU/memory limits
2. **Monitoring**: Use external monitoring solutions
3. **Logging**: Configure centralized logging
4. **Backups**: Regular Redis backup strategy
5. **Security**: Use secrets management for sensitive configuration
6. **Networks**: Use custom networks for isolation
7. **Load Balancing**: Use external load balancer for multiple instances