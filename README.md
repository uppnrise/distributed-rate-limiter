# Distributed Rate Limiter

A distributed token bucket rate limiter implementation in Java with comprehensive API documentation and examples.

## Features

- **Token bucket algorithm** for fair rate limiting with burst support
- **Distributed state** using Redis for multi-instance deployments
- **Configurable capacity and refill rate** per key or pattern
- **Thread-safe** implementation with atomic operations
- **RESTful API** with OpenAPI/Swagger documentation
- **Comprehensive monitoring** with actuator endpoints and performance tracking
- **Administrative controls** for runtime configuration management
- **Performance benchmarking** tools for load testing and optimization
- **Security features** including API key authentication and IP filtering

## 🏗️ Architecture Overview

### System Architecture

![Architecture Overview](docs/images/architecture-overview.svg)

The distributed rate limiter uses a token bucket algorithm with Redis as the backend for sharing state across multiple application instances. Each component is designed for high availability and scalability.

### Rate Limiting Flow

![Rate Limit Sequence](docs/images/rate-limit-sequence.svg)

The rate limiting flow follows a simple but effective sequence:
1. Client sends rate limit check request
2. Service retrieves configuration for the key
3. Current token bucket state is fetched from Redis
4. Tokens are refilled based on elapsed time
5. Request is allowed/denied based on available tokens
6. Bucket state is updated in Redis
7. Response is returned to client

### Health Check Response

![Health Check](https://github.com/user-attachments/assets/f1fd9923-7402-4cc8-84c7-04a15301cdc4)

The health endpoint provides real-time status information about the application and its dependencies.

### Interactive API Documentation

![Swagger UI](https://github.com/user-attachments/assets/89ccf6fa-9144-43c0-9fbd-2893d3daab7b)

Complete API documentation is available through Swagger UI, providing interactive testing capabilities for all endpoints.

### Performance Metrics

![Performance Metrics](docs/images/performance-metrics.svg)

Real-time performance monitoring shows request rates, response times, and system resource utilization.

### Monitoring Dashboard

![Monitoring Dashboard](docs/images/monitoring-dashboard.svg)

Comprehensive Grafana dashboard provides production-ready monitoring with alerts and SLA tracking.

### Configuration Management

![Configuration Dashboard](docs/images/configuration-dashboard.svg)

Web-based configuration interface allows real-time updates to rate limiting rules without service restarts.

### Kubernetes Deployment

![Kubernetes Deployment](docs/images/kubernetes-deployment.svg)

Production-ready Kubernetes deployment with auto-scaling, health checks, and persistent storage.

### Metrics Endpoint

![Metrics](https://github.com/user-attachments/assets/db440d91-dfa7-4dc1-875b-679a6f20340b)

Prometheus-compatible metrics endpoint provides detailed application and business metrics.

## 🌟 Open Source Community

![Open Source](docs/images/community-open-source.svg)

This project is proudly open source under the MIT License. We welcome contributions, bug reports, and feature requests from the community.

## 📚 Documentation

### API Documentation
- **[Interactive API Documentation](http://localhost:8080/swagger-ui/index.html)** - Swagger UI (when running)
- **[OpenAPI Specification](http://localhost:8080/v3/api-docs)** - Machine-readable API spec (when running)
- **[Complete API Reference](docs/API.md)** - Comprehensive API documentation with examples

> **Note**: The API provides 18 endpoints covering rate limiting, configuration management, administrative operations, performance monitoring, benchmarking, and system metrics.

### Usage Examples
- **[Java/Spring Boot Integration](docs/examples/java-client.md)** - Complete integration example
- **[Python Client](docs/examples/python-client.md)** - Flask/FastAPI integration
- **[Node.js Client](docs/examples/nodejs-client.md)** - Express.js middleware
- **[Go Client](docs/examples/go-client.md)** - Native HTTP client with middleware
- **[cURL Examples](docs/examples/curl-examples.md)** - Command-line testing

### Architecture & Design
- **[Architecture Decision Records](docs/adr/README.md)** - Design decisions and rationale
- **[Token Bucket Algorithm](docs/adr/001-token-bucket-algorithm.md)** - Algorithm choice explanation
- **[Redis Integration](docs/adr/002-redis-distributed-state.md)** - Distributed state design

### Deployment & Operations
- **[Deployment Guide](docs/deployment/README.md)** - Docker, Kubernetes, and production deployment
- **[Configuration Guide](CONFIGURATION.md)** - Detailed configuration options
- **[Docker Usage](DOCKER.md)** - Container deployment instructions
- **[Performance Guide](PERFORMANCE.md)** - Optimization and tuning
- **[Load Testing Guide](LOAD-TESTING.md)** - Benchmarking and performance testing

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

Start the entire stack with one command:

```bash
git clone https://github.com/uppnrise/distributed-rate-limiter.git
cd distributed-rate-limiter
docker compose up -d
```

The application will be available at:
- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **Health Check**: http://localhost:8080/actuator/health

### Option 2: Local Development

1. **Prerequisites**: Java 21, Redis running on localhost:6379

2. **Clone and run**:
   ```bash
   git clone https://github.com/uppnrise/distributed-rate-limiter.git
   cd distributed-rate-limiter
   ./mvnw spring-boot:run
   ```

### Test the API

```bash
# Check if a request is allowed
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key":"user:123","tokens":1}'

# Expected response
{
  "key": "user:123",
  "tokensRequested": 1,
  "allowed": true
}
```

## 🔧 Configuration

### Basic Configuration

The rate limiter supports hierarchical configuration:

1. **Per-key configuration** (highest priority)
2. **Pattern-based configuration** (e.g., `user:*`, `api:v1:*`)
3. **Default configuration** (fallback)

### Example Configuration

```properties
# Default limits
ratelimiter.capacity=10
ratelimiter.refillRate=2

# Per-key overrides
ratelimiter.keys.premium_user.capacity=100
ratelimiter.keys.premium_user.refillRate=20

# Pattern-based configuration
ratelimiter.patterns.api:*.capacity=50
ratelimiter.patterns.user:*.capacity=20
```

### Dynamic Configuration

Update configuration at runtime via REST API:

```bash
# Update default limits
curl -X POST http://localhost:8080/api/ratelimit/config/default \
  -H "Content-Type: application/json" \
  -d '{"capacity":20,"refillRate":5}'

# Set limits for specific keys
curl -X POST http://localhost:8080/api/ratelimit/config/keys/vip_user \
  -H "Content-Type: application/json" \
  -d '{"capacity":200,"refillRate":50}'
```

## 🛡️ API Endpoints

The application provides a comprehensive REST API with the following endpoints:

### Rate Limiting Operations
- `POST /api/ratelimit/check` - Check if request is allowed for a key
- `GET /api/ratelimit/config` - Get current rate limiter configuration
- `POST /api/ratelimit/config/default` - Update default configuration
- `POST /api/ratelimit/config/keys/{key}` - Set configuration for specific key
- `POST /api/ratelimit/config/patterns/{pattern}` - Set configuration for key pattern
- `DELETE /api/ratelimit/config/keys/{key}` - Remove key-specific configuration
- `DELETE /api/ratelimit/config/patterns/{pattern}` - Remove pattern configuration
- `POST /api/ratelimit/config/reload` - Reload configuration and clear caches
- `GET /api/ratelimit/config/stats` - Get configuration statistics

### Administrative Operations
- `GET /admin/keys` - List all active rate limiting keys with statistics
- `GET /admin/limits/{key}` - Get current limits for a specific key
- `PUT /admin/limits/{key}` - Update limits for a specific key
- `DELETE /admin/limits/{key}` - Remove limits for a specific key

### Performance Monitoring
- `POST /api/performance/baseline` - Store performance baseline
- `POST /api/performance/regression/analyze` - Analyze performance regression
- `POST /api/performance/baseline/store-and-analyze` - Store baseline and analyze
- `GET /api/performance/baseline/{testName}` - Get historical baselines
- `GET /api/performance/trend/{testName}` - Get performance trend data
- `GET /api/performance/health` - Performance monitoring health check

### Benchmarking
- `POST /api/benchmark/run` - Run performance benchmark
- `GET /api/benchmark/health` - Benchmark service health check

### Metrics and Monitoring
- `GET /metrics` - Get system metrics
- `GET /actuator/health` - Application health status
- `GET /actuator/metrics` - Detailed application metrics
- `GET /actuator/prometheus` - Prometheus-compatible metrics

### API Documentation
- `GET /swagger-ui/index.html` - Interactive API documentation
- `GET /v3/api-docs` - OpenAPI specification (JSON)
```

## 📊 Monitoring

### Health and Metrics

- **Health**: `/actuator/health` - Service health status
- **Metrics**: `/actuator/metrics` - Application metrics
- **Prometheus**: `/actuator/prometheus` - Prometheus-compatible metrics

### Key Metrics

- `rate.limiter.requests.total` - Total rate limit checks
- `rate.limiter.requests.allowed` - Allowed requests
- `rate.limiter.requests.denied` - Denied requests
- `redis.connection.pool.active` - Active Redis connections

## 🛡️ Security

### API Key Authentication

```bash
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{
    "key": "user:123",
    "tokens": 1,
    "apiKey": "your-api-key"
  }'
```

### IP Address Filtering

Configure IP whitelist/blacklist in `application.properties`:

```properties
ratelimiter.security.ip.whitelist=192.168.1.0/24,10.0.0.0/8
ratelimiter.security.ip.blacklist=192.168.1.100
```

## 🏗️ Development

### Building from Source

```bash
# Build JAR
./mvnw clean package

# Run tests (requires Docker for integration tests)
./mvnw test

# Check code style
./mvnw checkstyle:check
```

### Running Tests

```bash
# Unit tests only
./mvnw test -Dtest=\!*IntegrationTest

# Integration tests (requires Redis)
./mvnw test -Dtest=*IntegrationTest

# API documentation tests
./mvnw test -Dtest=ApiDocumentationTest
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Update documentation
6. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.

## 🆘 Support

- **Documentation**: Check the [docs/](docs/) directory for comprehensive guides
- **Issues**: Report bugs and request features via [GitHub Issues](https://github.com/uppnrise/distributed-rate-limiter/issues)
- **Examples**: See [docs/examples/](docs/examples/) for integration examples

