# Deployment Guide

This guide covers various deployment options for the Distributed Rate Limiter service.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Local Development](#local-development)
- [Docker Deployment](#docker-deployment)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Production Considerations](#production-considerations)
- [Monitoring and Observability](#monitoring-and-observability)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### System Requirements

- **Java**: OpenJDK 21 or later
- **Redis**: Version 6.0 or later
- **Memory**: Minimum 512MB RAM (2GB+ recommended for production)
- **CPU**: 2+ cores recommended
- **Network**: Low-latency connection to Redis

### Infrastructure Requirements

- **Redis Instance**: Standalone or clustered Redis deployment
- **Load Balancer**: For multiple application instances
- **Monitoring**: Prometheus/Grafana stack (recommended)
- **Logging**: Centralized logging solution (ELK, Splunk, etc.)

## Local Development

### Quick Start

1. **Clone the repository**:
   ```bash
   git clone https://github.com/uppnrise/distributed-rate-limiter.git
   cd distributed-rate-limiter
   ```

2. **Start Redis using Docker**:
   ```bash
   docker run -d --name redis -p 6379:6379 redis:7-alpine
   ```

3. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Verify deployment**:
   ```bash
   curl http://localhost:8080/actuator/health
   curl http://localhost:8080/swagger-ui/index.html
   ```

### Development Configuration

Create `application-dev.properties`:

```properties
# Development profile configuration
spring.profiles.active=dev

# Redis configuration
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.database=0

# Rate limiter defaults (permissive for development)
ratelimiter.capacity=100
ratelimiter.refillRate=20

# Logging
logging.level.dev.bnacar.distributedratelimiter=DEBUG
logging.level.org.springframework.data.redis=DEBUG

# Actuator endpoints (all enabled for development)
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always
```

## Docker Deployment

### Single Container with Docker Compose

The repository includes a complete Docker Compose setup:

```bash
# Start the entire stack
docker compose up -d

# Check services
docker compose ps

# View logs
docker compose logs -f app

# Stop services
docker compose down
```

### Building Custom Images

Build the application image:

```bash
# Build JAR
./mvnw clean package -DskipTests

# Build Docker image
docker build -t rate-limiter:latest .

# Run with custom image
docker run -d \
  --name rate-limiter \
  -p 8080:8080 \
  -e SPRING_REDIS_HOST=redis \
  --link redis:redis \
  rate-limiter:latest
```

### Production Docker Configuration

Create `docker-compose.prod.yml`:

```yaml
version: '3.8'
services:
  redis:
    image: redis:7-alpine
    restart: unless-stopped
    command: redis-server --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3

  app:
    image: rate-limiter:latest
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
      - JAVA_OPTS=-Xmx1g -Xms512m
    depends_on:
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  redis_data:
```

## Kubernetes Deployment

### ConfigMap

```yaml
# config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: rate-limiter-config
data:
  application.properties: |
    spring.profiles.active=production
    spring.redis.host=redis-service
    spring.redis.port=6379
    
    # Rate limiter configuration
    ratelimiter.capacity=10
    ratelimiter.refillRate=2
    ratelimiter.cleanupIntervalMs=60000
    
    # Actuator
    management.endpoints.web.exposure.include=health,metrics,prometheus
    management.endpoint.health.show-details=when-authorized
```

### Redis Deployment

```yaml
# redis.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
        command: ["redis-server"]
        args: ["--appendonly", "yes", "--maxmemory", "512mb"]
        resources:
          requests:
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        volumeMounts:
        - name: redis-storage
          mountPath: /data
      volumes:
      - name: redis-storage
        persistentVolumeClaim:
          claimName: redis-pvc

---
apiVersion: v1
kind: Service
metadata:
  name: redis-service
spec:
  selector:
    app: redis
  ports:
  - port: 6379
    targetPort: 6379

---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: redis-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
```

### Application Deployment

```yaml
# app.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rate-limiter
spec:
  replicas: 3
  selector:
    matchLabels:
      app: rate-limiter
  template:
    metadata:
      labels:
        app: rate-limiter
    spec:
      containers:
      - name: app
        image: rate-limiter:latest
        ports:
        - containerPort: 8080
        env:
        - name: JAVA_OPTS
          value: "-Xmx1g -Xms512m"
        resources:
          requests:
            memory: "512Mi"
            cpu: "200m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 15
          periodSeconds: 10
        volumeMounts:
        - name: config
          mountPath: /config
      volumes:
      - name: config
        configMap:
          name: rate-limiter-config

---
apiVersion: v1
kind: Service
metadata:
  name: rate-limiter-service
spec:
  selector:
    app: rate-limiter
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP

---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: rate-limiter-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: rate-limiter.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: rate-limiter-service
            port:
              number: 80
```

### Deploy to Kubernetes

```bash
# Apply configurations
kubectl apply -f config.yaml
kubectl apply -f redis.yaml
kubectl apply -f app.yaml

# Check deployment status
kubectl get pods
kubectl get services
kubectl get ingress

# View logs
kubectl logs -f deployment/rate-limiter
```

## Production Considerations

### Security

1. **API Key Management**:
   ```properties
   # Use environment variables for sensitive data
   ratelimiter.api.keys=${API_KEYS:}
   ```

2. **Network Security**:
   - Use TLS for all communications
   - Restrict Redis access to application instances only
   - Implement IP whitelisting if needed

3. **Authentication**:
   ```yaml
   # Add authentication to actuator endpoints
   management.security.enabled=true
   spring.security.user.name=${ADMIN_USERNAME:admin}
   spring.security.user.password=${ADMIN_PASSWORD:changeme}
   ```

### Performance Tuning

1. **JVM Options**:
   ```bash
   JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
   ```

2. **Redis Configuration**:
   ```bash
   # Redis performance tuning
   maxmemory 1gb
   maxmemory-policy allkeys-lru
   tcp-keepalive 60
   timeout 300
   ```

3. **Connection Pooling**:
   ```properties
   spring.redis.lettuce.pool.max-active=20
   spring.redis.lettuce.pool.max-idle=10
   spring.redis.lettuce.pool.min-idle=5
   spring.redis.lettuce.pool.max-wait=1000ms
   ```

### High Availability

1. **Redis Clustering**:
   ```properties
   spring.redis.cluster.nodes=redis1:6379,redis2:6379,redis3:6379
   spring.redis.cluster.max-redirects=3
   ```

2. **Application Clustering**:
   - Deploy multiple instances behind load balancer
   - Use session-less design (stateless)
   - Implement health checks

3. **Disaster Recovery**:
   - Regular Redis backups
   - Multi-region deployment option
   - Failover procedures documented

## Monitoring and Observability

### Metrics Collection

Prometheus configuration:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'rate-limiter'
    static_configs:
      - targets: ['rate-limiter:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
```

### Grafana Dashboard

Key metrics to monitor:

- Request rate and response times
- Rate limit violations per key
- Redis connection pool metrics
- JVM memory and GC metrics
- Error rates and circuit breaker status

### Alerting Rules

```yaml
# alerts.yml
groups:
  - name: rate-limiter
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"
          
      - alert: RedisConnectionFailure
        expr: redis_connected_clients == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Redis connection failure"
```

## Troubleshooting

### Common Issues

1. **Redis Connection Timeout**:
   ```bash
   # Check Redis connectivity
   redis-cli -h redis-host -p 6379 ping
   
   # Verify network connectivity
   telnet redis-host 6379
   ```

2. **High Memory Usage**:
   ```bash
   # Check Redis memory usage
   redis-cli info memory
   
   # Monitor JVM memory
   jstat -gc <pid>
   ```

3. **Performance Issues**:
   ```bash
   # Check Redis latency
   redis-cli --latency-history -h redis-host
   
   # Monitor application metrics
   curl http://localhost:8080/actuator/metrics
   ```

### Log Analysis

Enable debug logging for troubleshooting:

```properties
logging.level.dev.bnacar.distributedratelimiter=DEBUG
logging.level.org.springframework.data.redis=DEBUG
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

### Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Detailed health info
curl http://localhost:8080/actuator/health/redis

# Metrics endpoint
curl http://localhost:8080/actuator/metrics/rate.limiter.requests
```