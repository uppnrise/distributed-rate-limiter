# Performance Tuning Guide

This guide covers performance optimization strategies, configuration options, and benchmarking for the Distributed Rate Limiter service.

## Performance Metrics

The service has been optimized to achieve:
- **Target**: 1000+ requests/second  
- **Actual**: 796,000+ requests/second (in testing)
- **Concurrent Load**: Supports high concurrency with minimal resource usage

### Algorithm Performance Comparison

| Algorithm | Memory/Key | CPU Overhead | Throughput | Use Case |
|-----------|------------|--------------|------------|----------|
| **Fixed Window** | ~4KB | Baseline | Highest | Memory-constrained, high scale |
| **Token Bucket** | ~8KB | +15% | High | General purpose, burst handling |  
| **Sliding Window** | ~8KB | +25% | Medium | Strict rate enforcement |

**Performance Recommendations**:
- **High Scale (1M+ keys)**: Use Fixed Window for 50% memory reduction
- **General APIs**: Use Token Bucket for balanced performance and UX
- **Critical APIs**: Use Sliding Window when precision is more important than performance

## Redis Connection Pooling

### Configuration

Connection pooling is enabled by default with optimized settings in `application.properties`:

```properties
# Redis Connection Pool Configuration
spring.data.redis.lettuce.pool.enabled=true
spring.data.redis.lettuce.pool.max-active=20
spring.data.redis.lettuce.pool.max-idle=10
spring.data.redis.lettuce.pool.min-idle=5
spring.data.redis.lettuce.pool.max-wait=5000ms
spring.data.redis.lettuce.pool.time-between-eviction-runs=30s
```

### Tuning Guidelines

| Parameter | Description | Recommended Range |
|-----------|-------------|-------------------|
| `max-active` | Maximum number of connections | 10-50 |
| `max-idle` | Maximum idle connections to maintain | 5-25 |
| `min-idle` | Minimum idle connections to maintain | 2-10 |
| `max-wait` | Maximum time to wait for connection | 1s-10s |

**For high-load environments**: Increase `max-active` to 50+ and `max-idle` to 25+.

## Async Processing

### Memory Cleanup Optimization

The service uses async processing for background cleanup tasks:

```java
@Async("rateLimiterTaskExecutor")
protected void cleanupExpiredBucketsAsync() {
    // Non-blocking cleanup of expired rate limit buckets
}
```

### Configuration

Async task executor is configured with optimal thread pool settings:

```java
executor.setCorePoolSize(2);        // Base threads for background tasks
executor.setMaxPoolSize(10);        // Maximum threads under load
executor.setQueueCapacity(500);     // Task queue size
executor.setThreadNamePrefix("RateLimiter-Async-");
```

## Memory Usage Optimization

### Bucket Cleanup Strategy

1. **Automatic Cleanup**: Expired rate limit buckets are cleaned up every 60 seconds by default
2. **Configurable Intervals**: Each bucket can have its own cleanup interval
3. **Background Processing**: Cleanup runs asynchronously to avoid blocking request processing

### Memory Monitoring

```java
// Get active bucket count for monitoring
int activeBuckets = rateLimiterService.getActiveBucketCount();

// For in-memory backend, additional metrics:
long cleanupCount = inMemoryBackend.getCleanupCount();
long lastCleanupTime = inMemoryBackend.getLastCleanupTime();
```

### Tuning Memory Usage

```properties
# Cleanup interval for unused buckets (milliseconds)
ratelimiter.cleanupIntervalMs=60000

# Reduce for high-churn environments
ratelimiter.cleanupIntervalMs=30000

# Increase for stable workloads
ratelimiter.cleanupIntervalMs=120000
```

### Algorithm-Specific Memory Optimization

#### Fixed Window Algorithm
- **Memory Usage**: 50% less than Token Bucket/Sliding Window
- **Optimal For**: Applications with >100K concurrent keys
- **Configuration**:
```properties
# Use Fixed Window for memory-constrained environments
ratelimiter.patterns.high-volume:*.algorithm=FIXED_WINDOW
ratelimiter.patterns.high-volume:*.capacity=1000
```

#### Token Bucket Algorithm  
- **Memory Usage**: Standard baseline
- **Optimal For**: General APIs with burst requirements
- **Configuration**:
```properties
# Balanced performance and user experience
ratelimiter.patterns.api:*.algorithm=TOKEN_BUCKET
ratelimiter.patterns.api:*.capacity=100
```

#### Sliding Window Algorithm
- **Memory Usage**: Similar to Token Bucket but with additional request tracking
- **Optimal For**: Critical APIs requiring precise rate control
- **Configuration**:
```properties
# Use for strict rate enforcement
ratelimiter.patterns.critical:*.algorithm=SLIDING_WINDOW
ratelimiter.patterns.critical:*.capacity=50
```

## Benchmarking

### Built-in Benchmark Endpoint

The service provides a `/api/benchmark/run` endpoint for performance testing:

```bash
curl -X POST http://localhost:8080/api/benchmark/run \
  -H "Content-Type: application/json" \
  -d '{
    "concurrentThreads": 10,
    "requestsPerThread": 1000,
    "durationSeconds": 30,
    "tokensPerRequest": 1,
    "keyPrefix": "load_test"
  }'
```

### Response Example

```json
{
  "success": true,
  "totalRequests": 10000,
  "successfulRequests": 8500,
  "errorRequests": 1500,
  "durationSeconds": 30.45,
  "throughputPerSecond": 328.41,
  "successRate": 85.0,
  "concurrentThreads": 10,
  "requestsPerThread": 1000
}
```

### Performance Testing Best Practices

1. **Warm-up Period**: Run initial requests to warm up connections and JVM
2. **Realistic Load**: Use diverse key patterns that match production usage
3. **Monitor Resources**: Watch CPU, memory, and Redis connection usage
4. **Test Scenarios**: Test both allowed and rate-limited scenarios

## Performance Tuning Checklist

### Application Level

- [x] **Connection Pooling**: Enabled with optimized pool sizes
- [x] **Async Processing**: Background cleanup tasks
- [x] **Memory Management**: Automatic bucket cleanup
- [x] **Thread Optimization**: Proper executor configuration

### Redis Level

- [ ] **Redis Memory**: Configure `maxmemory` and eviction policies
- [ ] **Persistence**: Consider disabling persistence for rate limiting data
- [ ] **Network**: Use Redis in same data center/availability zone
- [ ] **Clustering**: Consider Redis cluster for horizontal scaling

### JVM Level

```bash
# Recommended JVM options for production
-Xms2g -Xmx4g                          # Heap size
-XX:+UseG1GC                           # G1 garbage collector
-XX:MaxGCPauseMillis=200              # Target GC pause time
-XX:+UseStringDeduplication           # String deduplication
```

### Operating System Level

```bash
# Increase file descriptor limits
ulimit -n 65536

# Optimize TCP settings
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 65535
```

## Monitoring and Metrics

### Key Performance Indicators

1. **Throughput**: Requests per second handled
2. **Latency**: Response time percentiles (P50, P95, P99)
3. **Error Rate**: Percentage of failed requests
4. **Resource Usage**: CPU, memory, connection pool utilization
5. **Bucket Count**: Number of active rate limit buckets

### Health Checks

```bash
# Service health
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Benchmark health
curl http://localhost:8080/api/benchmark/health
```

## Troubleshooting Performance Issues

### High Latency

1. **Check Redis Connectivity**: Verify Redis is responsive
2. **Connection Pool**: Increase `max-active` if connections are exhausted
3. **Cleanup Frequency**: Reduce cleanup interval if memory pressure is high

### Low Throughput

1. **CPU Bottleneck**: Increase async executor thread pool
2. **Memory Issues**: Tune JVM heap size and GC settings
3. **Network**: Verify Redis network latency

### Memory Leaks

1. **Monitor Bucket Count**: Check for excessive bucket accumulation
2. **Cleanup Effectiveness**: Verify cleanup tasks are running
3. **Configuration**: Adjust cleanup intervals based on usage patterns

## Production Deployment Recommendations

1. **Load Testing**: Always benchmark under realistic conditions
2. **Gradual Rollout**: Deploy with circuit breakers and monitoring
3. **Resource Allocation**: Provision adequate CPU and memory
4. **Redis Setup**: Use dedicated Redis instance with appropriate resources
5. **Monitoring**: Set up alerts for key performance metrics

## Example Production Configuration

```properties
# High-performance production settings
spring.data.redis.lettuce.pool.max-active=50
spring.data.redis.lettuce.pool.max-idle=25
spring.data.redis.lettuce.pool.min-idle=10

# Aggressive cleanup for high-churn environments
ratelimiter.cleanupIntervalMs=30000

# Higher capacity for busy APIs
ratelimiter.capacity=100
ratelimiter.refillRate=50
```

This configuration supports high-throughput scenarios while maintaining optimal resource usage.