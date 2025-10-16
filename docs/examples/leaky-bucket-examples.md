# Leaky Bucket Algorithm Examples

The Leaky Bucket algorithm enforces constant output rates through request queuing, making it ideal for traffic shaping and downstream system protection.

## Basic Usage

### API Gateway Traffic Shaping

```bash
# Configure leaky bucket for API gateway
curl -X POST http://localhost:8080/api/ratelimit/config/patterns/gateway:* \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 50,
    "refillRate": 10,
    "algorithm": "LEAKY_BUCKET"
  }'

# Process exactly 10 requests per second, queue up to 50 requests
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{
    "key": "gateway:service_a",
    "tokens": 1
  }'
```

### Database Connection Pool Protection

```bash
# Ensure database receives exactly 5 queries per second
curl -X POST http://localhost:8080/api/ratelimit/config/keys/db:connection_pool \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 20,
    "refillRate": 5,
    "algorithm": "LEAKY_BUCKET"
  }'

curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{
    "key": "db:connection_pool",
    "tokens": 1
  }'
```

## Configuration Examples

### application.properties

```properties
# Default leaky bucket configuration
ratelimiter.algorithm=LEAKY_BUCKET
ratelimiter.capacity=10
ratelimiter.refillRate=2

# Traffic shaping for downstream services
ratelimiter.patterns.downstream:*.algorithm=LEAKY_BUCKET
ratelimiter.patterns.downstream:*.capacity=50
ratelimiter.patterns.downstream:*.refillRate=10

# Database protection
ratelimiter.patterns.db:*.algorithm=LEAKY_BUCKET
ratelimiter.patterns.db:*.capacity=20
ratelimiter.patterns.db:*.refillRate=5

# Message queue rate limiting
ratelimiter.patterns.queue:*.algorithm=LEAKY_BUCKET
ratelimiter.patterns.queue:*.capacity=100
ratelimiter.patterns.queue:*.refillRate=25
```

## Use Cases

### 1. Microservice Rate Limiting

Protect downstream microservices with consistent request rates:

```bash
# Service A can handle 15 requests/second
curl -X POST http://localhost:8080/api/ratelimit/config/keys/service:payment \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 30,
    "refillRate": 15,
    "algorithm": "LEAKY_BUCKET"
  }'

# Test the rate limiting
for i in {1..20}; do
  curl -X POST http://localhost:8080/api/ratelimit/check \
    -H "Content-Type: application/json" \
    -d "{\"key\": \"service:payment\", \"tokens\": 1}" &
done
```

### 2. SLA Compliance

Ensure consistent processing rates for service agreements:

```bash
# Premium tier: 100 requests/second, 200 queue capacity
curl -X POST http://localhost:8080/api/ratelimit/config/keys/tier:premium \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 200,
    "refillRate": 100,
    "algorithm": "LEAKY_BUCKET"
  }'

# Standard tier: 25 requests/second, 50 queue capacity
curl -X POST http://localhost:8080/api/ratelimit/config/keys/tier:standard \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 50,
    "refillRate": 25,
    "algorithm": "LEAKY_BUCKET"
  }'
```

### 3. Load Testing with Consistent Rates

Use leaky bucket for generating consistent load patterns:

```bash
# Benchmark with exactly 50 requests per second
curl -X POST http://localhost:8080/api/benchmark/load-test \
  -H "Content-Type: application/json" \
  -d '{
    "requests": 1000,
    "concurrency": 10,
    "key": "loadtest:consistent",
    "algorithm": "LEAKY_BUCKET",
    "capacity": 100,
    "refillRate": 50
  }'
```

## Comparison with Other Algorithms

### Traffic Patterns

```bash
# Token Bucket - allows bursts
curl -X POST http://localhost:8080/api/ratelimit/config/keys/burst:allowed \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 10,
    "refillRate": 2,
    "algorithm": "TOKEN_BUCKET"
  }'

# Leaky Bucket - constant rate, no bursts
curl -X POST http://localhost:8080/api/ratelimit/config/keys/burst:prevented \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 10,
    "refillRate": 2,
    "algorithm": "LEAKY_BUCKET"
  }'

# Send 10 requests rapidly to both
for algorithm in "burst:allowed" "burst:prevented"; do
  echo "Testing $algorithm:"
  for i in {1..10}; do
    response=$(curl -s -X POST http://localhost:8080/api/ratelimit/check \
      -H "Content-Type: application/json" \
      -d "{\"key\": \"$algorithm\", \"tokens\": 1}")
    echo "Request $i: $response"
  done
  echo ""
done
```

## Monitoring and Metrics

### Check Queue Status

```bash
# Get current queue size and available capacity
curl http://localhost:8080/api/ratelimit/config/keys/gateway:service_a

# Response includes:
# - capacity: Total queue capacity
# - refillRate: Processing rate (tokens/second)
# - currentTokens: Available queue space
# - algorithm: LEAKY_BUCKET
```

### Performance Metrics

```bash
# View processing metrics
curl http://localhost:8080/actuator/metrics/ratelimit.requests.allowed
curl http://localhost:8080/actuator/metrics/ratelimit.requests.denied
curl http://localhost:8080/actuator/metrics/ratelimit.processing.time
```

## Redis Distributed Mode

When running with Redis, leaky bucket queues are shared across instances:

```bash
# Enable Redis mode
export SPRING_PROFILES_ACTIVE=redis

# Start application
java -jar target/distributed-rate-limiter-1.0.0.jar

# All instances share the same queue state
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{
    "key": "shared:queue",
    "tokens": 1
  }'
```

## Best Practices

### 1. Queue Sizing

- **Small queues** (capacity < 10): Low latency, frequent rejections
- **Medium queues** (capacity 10-100): Balanced throughput and latency
- **Large queues** (capacity > 100): High throughput, higher latency

### 2. Rate Selection

- **Conservative rates**: Ensure downstream systems can handle the load
- **Monitor processing**: Watch for queue buildup indicating backpressure
- **Adjust based on SLAs**: Match leak rate to service level requirements

### 3. Error Handling

```java
try {
    boolean allowed = rateLimiterService.isAllowed("service:critical", 1);
    if (!allowed) {
        // Handle rate limit gracefully
        return ResponseEntity.status(429)
            .header("Retry-After", "1")
            .body("Rate limit exceeded - request queued");
    }
    // Process request
} catch (Exception e) {
    // Handle service errors
    logger.warn("Rate limiter error, allowing request", e);
    // Fail open for availability
}
```

### 4. Configuration Management

```bash
# Monitor configuration effectiveness
curl http://localhost:8080/api/admin/buckets

# Adjust capacity based on observed patterns
curl -X PUT http://localhost:8080/api/ratelimit/config/keys/service:payment \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 60,
    "refillRate": 20,
    "algorithm": "LEAKY_BUCKET"
  }'
```

## Troubleshooting

### High Queue Sizes

```bash
# Check current queue status
curl http://localhost:8080/api/performance/metrics

# If queues are consistently full:
# 1. Increase leak rate (if downstream can handle it)
# 2. Increase queue capacity (for burst tolerance)
# 3. Scale downstream services
# 4. Add circuit breakers
```

### Performance Issues

```bash
# Monitor processing times
curl http://localhost:8080/api/performance/realtime

# Leaky bucket adds small overhead for queue management
# Typical impact: 1-2ms additional latency per request
```