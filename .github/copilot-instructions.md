# Distributed Rate Limiter

Production-ready distributed rate limiting service with REST API, **five-algorithm support** (Token Bucket, Sliding Window, Fixed Window, Leaky Bucket, Composite), Redis backend, comprehensive monitoring, and 265+ tests.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

**📚 For comprehensive documentation, see:**
- `README.md` - Complete project overview and quick start
- `docs/API.md` - Full API documentation
- `docs/adr/` - Architecture Decision Records (ADRs)
- `docs/examples/` - Client integration examples
- `docs/runbook/` - Operations and troubleshooting

## Architecture Overview

### Core Components
- **Rate Limiting Algorithms**: 
  - `TokenBucket.java` (primary - burst tolerance)
  - `SlidingWindow.java` (strict enforcement)
  - `FixedWindow.java` (memory efficient)
  - `LeakyBucket.java` (traffic shaping)
  - `CompositeRateLimiter.java` (multi-algorithm composition)
- **Distributed Backend**: `RedisRateLimiterBackend.java` for production, `InMemoryRateLimiterBackend.java` for testing
- **Service Layer**: `RateLimiterService.java` coordinates rate limit checks with `DistributedRateLimiterService.java` and `CompositeRateLimiterService.java`
- **Configuration**: `ConfigurationResolver.java` handles dynamic per-key and pattern-based limits
- **Controllers**: 6 REST endpoints - RateLimit, Admin, Config, Metrics, Performance, Benchmark

### Request Flow
1. `RateLimitController.checkRateLimit()` receives POST `/api/ratelimit/check`
2. `RateLimiterService` resolves configuration via `ConfigurationResolver`
3. Backend (`RedisRateLimiterBackend` or `InMemoryRateLimiterBackend`) performs algorithm check
4. Response includes `{allowed: boolean, tokensRequested: int, key: string}` or enhanced `CompositeRateLimitResponse` with component details

### Key Architecture Decisions (ADRs)
- **Token Bucket Algorithm** (ADR-001): Primary algorithm for burst handling and predictable behavior
- **Redis Distributed State** (ADR-002): Atomic Lua scripts for consistency, TTL for cleanup
- **Fixed Window Algorithm** (ADR-003): Memory-efficient alternative for high-scale scenarios
- **Leaky Bucket Algorithm** (ADR-004): Traffic shaping specialization for constant output rates
- **Composite Rate Limiting** (ADR-005): Multi-algorithm composition with configurable combination logic
- **Configuration Hierarchy**: Per-key (exact) > Pattern-based (wildcards) > Global defaults
- **Fail-Open Strategy**: Service continues with in-memory backend when Redis unavailable

### API Structure (18 Endpoints)
- **Rate Limiting**: `/api/ratelimit/check` (core functionality)
- **Configuration**: `/api/ratelimit/config/*` (runtime config management)
- **Admin Operations**: `/api/admin/*` (key resets, shutdown)
- **Performance**: `/api/performance/*` (real-time metrics)
- **Benchmarking**: `/api/benchmark/*` (load testing)
- **Health/Metrics**: `/actuator/*` (Spring Boot actuator endpoints)

### Algorithm Selection Guide
- **Token Bucket** (default): Best for general APIs with burst tolerance
- **Sliding Window**: Use for strict rate enforcement and critical APIs  
- **Fixed Window**: Choose for memory efficiency and high-scale scenarios
- **Leaky Bucket**: Select for traffic shaping and constant output rates
- **Composite**: Use for enterprise scenarios requiring multiple algorithms (API calls + bandwidth + compliance limits)

**📋 See `docs/adr/` for detailed algorithm comparison and decision rationale**

### Composite Rate Limiting (New Feature)

**Multi-Algorithm Composition**: Combine multiple algorithms with configurable logic
```json
{
  "key": "enterprise:customer:123",
  "algorithm": "COMPOSITE",
  "compositeConfig": {
    "limits": [
      {"name": "api_calls", "algorithm": "TOKEN_BUCKET", "capacity": 1000, "refillRate": 100},
      {"name": "bandwidth", "algorithm": "LEAKY_BUCKET", "capacity": 50, "refillRate": 5}
    ],
    "combinationLogic": "ALL_MUST_PASS"
  }
}
```

**Combination Logic Types**:
- `ALL_MUST_PASS`: AND operation (all components must allow)
- `ANY_CAN_PASS`: OR operation (any component allows)
- `WEIGHTED_AVERAGE`: Score-based with weights (>50% threshold)
- `HIERARCHICAL_AND`: Scope-ordered (USER → TENANT → GLOBAL)
- `PRIORITY_BASED`: High-priority first, fail-fast evaluation

**Enhanced Response**:
```json
{
  "allowed": false,
  "componentResults": {
    "api_calls": {"allowed": false, "currentTokens": 0, "capacity": 1000},
    "bandwidth": {"allowed": true, "currentTokens": 45, "capacity": 50}
  },
  "limitingComponent": "api_calls",
  "combinationResult": {
    "logic": "ALL_MUST_PASS", "overallScore": 0.0,
    "componentScores": {"api_calls": 0.0, "bandwidth": 1.0}
  }
}
```

**Use Cases**: SaaS platforms (API + bandwidth + compliance), Financial systems (rate + volume + velocity), Gaming (actions + chat + resources), IoT (commands + transfer + connections)

### Configuration Hierarchy (application.properties)
```properties
# Global defaults
ratelimiter.capacity=10
ratelimiter.refillRate=2

# Per-key exact matches (highest priority)
ratelimiter.keys.premium_user.capacity=50

# Pattern-based with wildcards (medium priority)
ratelimiter.patterns.user:*.capacity=20
ratelimiter.patterns.api:*.capacity=100
```

## Development Workflow

### Environment Setup (macOS)
```bash
# Install Java 21 (CRITICAL - will not compile with Java 17)
brew install openjdk@21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# Verify version
java -version  # Must show 21.x.x
```

### Build & Test Commands
- **Full build**: `./mvnw clean install` (120s timeout - includes 76+ test classes)
- **Quick compile**: `./mvnw clean compile` (30s after dependencies cached)
- **Run tests only**: `./mvnw test` (60s - includes Testcontainers Redis startup)
- **Security scan**: `./mvnw dependency-check:check` (OWASP vulnerability scanning)
- **Code coverage**: `./mvnw jacoco:report` (generates `target/site/jacoco/index.html`)

### Application Startup
- **Development**: `./mvnw spring-boot:run` (port 8080, 2.2s startup)
- **With Redis**: `docker-compose up -d redis && ./mvnw spring-boot:run`
- **Production JAR**: `java -jar target/distributed-rate-limiter-1.1.0.jar`

### API Testing
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html (18 documented endpoints)
- **Rate limit check**: `curl -X POST http://localhost:8080/api/ratelimit/check -H "Content-Type: application/json" -d '{"key":"test","tokensRequested":1}'`
- **Config management**: `curl http://localhost:8080/api/ratelimit/config` (view current settings)
- **Load testing**: `curl -X POST http://localhost:8080/api/benchmark/load-test -d '{"requests":1000,"concurrency":10,"key":"test"}'`
- **Health check**: `curl http://localhost:8080/actuator/health`
- **Metrics**: `curl http://localhost:8080/actuator/prometheus` (Micrometer metrics)

### Client Integration Examples
- **Java/Spring Boot**: WebClient-based with retry logic (`docs/examples/java-client.md`)
- **Python**: Requests/aiohttp with exponential backoff (`docs/examples/python-client.md`)
- **Node.js**: Express middleware pattern (`docs/examples/nodejs-client.md`)
- **Go**: Native HTTP client with circuit breaker (`docs/examples/go-client.md`)
- **cURL**: Comprehensive command-line examples (`docs/examples/curl-examples.md`)
- **Composite Rate Limiting**: Multi-algorithm examples (`examples/composite-rate-limiting.md`)

## Testing Strategy

### Test Categories (76+ test classes)
- **Unit Tests**: `TokenBucketTest`, `SlidingWindowTest`, `FixedWindowTest`, `LeakyBucketTest`, `CompositeRateLimiterTest`, `ConfigurationResolverTest`
- **Integration Tests**: `DistributedRateLimiterServiceTest`, `RedisConnectionPoolTest` 
- **Performance Tests**: `ConcurrentPerformanceTest`, `MemoryUsageTest`, `LoadTestSuite`
- **Controller Tests**: Each of 6 controllers has dedicated test class
- **Documentation Tests**: `ApiDocumentationTest`, `DocumentationCompletenessTest`

### Key Test Patterns
- **Testcontainers**: All Redis integration tests use `@Testcontainers` with automatic container lifecycle
- **Concurrent Testing**: Use `CompletableFuture` and `CountDownLatch` for race condition validation
- **Timing Tests**: `Awaitility.await()` for time-based assertions (token refill, cleanup)
- **Property-Based**: Multiple configuration scenarios tested via `@ParameterizedTest`

### Performance Validation
- **Load Testing**: `./mvnw test -Dtest=*LoadTest*` (requires 16GB+ RAM)
- **Memory Testing**: `./mvnw test -Dtest=MemoryUsageTest` (validates <200MB baseline)
- **Regression Detection**: Built-in performance regression detection with configurable thresholds

## Production Concerns

### Deployment Artifacts
- **JAR**: `./build-release.sh` creates production JAR with embedded Tomcat
- **Docker**: Multi-stage build, Alpine base, non-root user, health checks included
- **K8s Manifests**: Complete Kubernetes deployment in `k8s/` with Redis, monitoring

### Monitoring & Observability  
- **Metrics**: Micrometer + Prometheus (custom rate limit metrics in `MetricsService`)
- **Health Checks**: Custom health indicators for Redis connectivity and rate limiter status
- **Structured Logging**: JSON logs via Logstash encoder, correlation IDs for tracing
- **Security**: IP-based rate limiting, API key validation, CORS configuration

### Configuration Management
- **Environment Profiles**: `application-docker.properties` for containerized deployment
- **Dynamic Config**: Runtime configuration changes via `/api/ratelimit/config/*` endpoints
- **Hierarchy Resolution**: Per-key exact > Pattern wildcards > Global defaults
- **Redis Data Structure**: Hash fields (`tokens`, `lastRefillTime`, `capacity`, `refillRate`) with TTL
- **Lua Scripts**: Atomic refill-and-consume operations prevent race conditions
- **Redis Failover**: Automatic fallback to in-memory backend if Redis unavailable

### Operations & Monitoring
- **Incident Response**: P0/P1/P2 severity levels with defined response times (`docs/runbook/`)
- **Health Indicators**: Custom health checks for Redis connectivity and bucket operations
- **Performance Metrics**: Built-in regression detection with configurable thresholds
- **Load Testing**: Integrated benchmarking APIs with concurrent request simulation
- **Deployment**: Complete Kubernetes manifests in `k8s/` with Redis, monitoring, RBAC

### Common Operations
- **Scale Testing**: Use `/api/benchmark/load-test` endpoints for load validation
- **Config Validation**: Check current limits via `/api/ratelimit/config` 
- **Performance Monitoring**: `/api/performance/metrics` for real-time statistics
- **Admin Operations**: `/api/admin/shutdown`, `/api/admin/keys/{key}/reset`
- **Dynamic Config**: Update limits via POST `/api/ratelimit/config/keys/{key}` or `/api/ratelimit/config/patterns/{pattern}`
- **Troubleshooting**: Use runbook procedures in `docs/runbook/README.md` for incident response

## Troubleshooting

### Build Issues
- **Java 21 Required**: Build fails with "release version 21 not supported" on older Java
- **Docker Required**: Tests fail if Docker daemon not accessible for Testcontainers
- **Memory Limits**: Full test suite requires 4GB+ heap (set `MAVEN_OPTS=-Xmx4g`)

### Runtime Issues  
- **Redis Connection**: Health endpoint shows DOWN if Redis unreachable (check `spring.data.redis.*`)
- **Port Conflicts**: Default port 8080, override with `--server.port=8081`
- **Performance**: Use connection pooling (`spring.data.redis.lettuce.pool.*`) for production loads

**📚 For comprehensive troubleshooting procedures, see `docs/runbook/README.md`**