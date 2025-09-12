# Distributed Rate Limiter

A distributed token bucket rate limiter implementation in Java with comprehensive API documentation and examples.

## Features

- **Token bucket algorithm** for fair rate limiting with burst support
- **Distributed state** using Redis for multi-instance deployments
- **Configurable capacity and refill rate** per key or pattern
- **Thread-safe** implementation with atomic operations
- **RESTful API** with OpenAPI/Swagger documentation
- **Comprehensive monitoring** with actuator endpoints
- **Security features** including API key authentication and IP filtering

## 📚 Documentation

### API Documentation
- **[Interactive API Documentation](http://localhost:8080/swagger-ui/index.html)** - Swagger UI (when running)
- **[OpenAPI Specification](http://localhost:8080/v3/api-docs)** - Machine-readable API spec (when running)

### Usage Examples
- **[Java/Spring Boot Integration](docs/examples/java-client.md)** - Complete integration example
- **[Python Client](docs/examples/python-client.md)** - Flask/FastAPI integration
- **[Node.js Client](docs/examples/nodejs-client.md)** - Express.js middleware
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

