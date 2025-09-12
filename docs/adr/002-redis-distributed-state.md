# ADR-002: Redis for Distributed State

## Status
Accepted

## Context

The rate limiter needs to maintain consistent state across multiple application instances in a distributed environment. Each rate limit key (user, API endpoint, etc.) must have a shared view of token bucket state to prevent different instances from making independent rate limiting decisions.

Requirements:
- **Consistency**: All instances must see the same bucket state
- **Performance**: Sub-millisecond latency for rate limit checks
- **Scalability**: Support for millions of concurrent rate limit keys
- **Reliability**: Handle Redis failures gracefully
- **Atomic Operations**: Bucket operations must be atomic to prevent race conditions

## Decision

We will use **Redis** as the distributed state store for token bucket data with the following implementation:

- **Redis Hash**: Store bucket state as hash with fields: `tokens`, `lastRefillTime`, `capacity`, `refillRate`
- **Lua Scripts**: Implement atomic refill-and-consume operations
- **TTL Management**: Automatic expiration of unused buckets
- **Connection Pooling**: Use Redis connection pool for performance
- **Failover Strategy**: Fail-open when Redis is unavailable

## Implementation Strategy

### Redis Data Structure
```
Key: "bucket:{rateLimitKey}"
Hash Fields:
- tokens: current token count
- lastRefillTime: last refill timestamp (milliseconds)
- capacity: maximum bucket capacity
- refillRate: tokens per second
TTL: 3600 seconds (auto-cleanup)
```

### Lua Script for Atomic Operations
```lua
-- refill_and_consume.lua
local key = KEYS[1]
local tokensToConsume = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local refillRate = tonumber(ARGV[3])
local now = tonumber(ARGV[4])

local bucket = redis.call('HMGET', key, 'tokens', 'lastRefillTime')
local tokens = tonumber(bucket[1]) or capacity
local lastRefillTime = tonumber(bucket[2]) or now

-- Calculate refill
local timePassed = now - lastRefillTime
local tokensToAdd = math.floor((timePassed * refillRate) / 1000)
tokens = math.min(capacity, tokens + tokensToAdd)

-- Try to consume
local allowed = false
if tokens >= tokensToConsume then
    tokens = tokens - tokensToConsume
    allowed = true
end

-- Update bucket state
redis.call('HMSET', key, 'tokens', tokens, 'lastRefillTime', now)
redis.call('EXPIRE', key, 3600)

return {allowed and 1 or 0, tokens}
```

### Java Integration
```java
@Service
public class RedisTokenBucket {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<List> refillAndConsumeScript;
    
    public boolean tryConsume(String key, long tokensToConsume, 
                             long capacity, long refillRate) {
        try {
            List<Long> result = redisTemplate.execute(
                refillAndConsumeScript,
                Collections.singletonList("bucket:" + key),
                tokensToConsume, capacity, refillRate, 
                System.currentTimeMillis()
            );
            
            return result.get(0) == 1L;
        } catch (Exception e) {
            // Fail open on Redis errors
            log.warn("Redis error, allowing request: {}", e.getMessage());
            return true;
        }
    }
}
```

## Consequences

### Positive
- **True Distribution**: Consistent state across all application instances
- **High Performance**: Redis provides sub-millisecond operations
- **Atomic Operations**: Lua scripts ensure race-condition-free bucket updates
- **Auto Cleanup**: TTL prevents memory leaks from abandoned buckets
- **Proven Technology**: Redis is battle-tested for high-throughput scenarios
- **Observability**: Redis provides monitoring and debugging capabilities

### Negative
- **External Dependency**: Requires Redis infrastructure
- **Network Latency**: Each rate limit check requires network round-trip
- **Single Point of Failure**: Redis unavailability affects rate limiting
- **Memory Usage**: Redis memory grows with number of active rate limit keys
- **Operational Complexity**: Requires Redis monitoring, backup, and maintenance

### Risk Mitigation
- **Fail-Open Strategy**: Allow requests when Redis is unavailable
- **Connection Pooling**: Reuse Redis connections to minimize latency
- **Redis Clustering**: Use Redis Cluster for high availability
- **Circuit Breaker**: Implement circuit breaker pattern for Redis operations
- **Local Fallback**: Consider in-memory buckets as emergency fallback

## Alternatives Considered

### In-Memory with Clustering
- **Pros**: No external dependencies, lower latency
- **Cons**: Complex distributed consensus, memory limitations
- **Rejected**: Too complex to implement correctly

### Database (PostgreSQL/MySQL)
- **Pros**: ACID guarantees, familiar technology
- **Cons**: Higher latency, not optimized for high-frequency updates
- **Rejected**: Performance concerns for high-throughput scenarios

### Apache Kafka with State Stores
- **Pros**: Event-driven, scalable
- **Cons**: Complex setup, eventual consistency
- **Rejected**: Overkill for rate limiting use case

### Hazelcast IMDG
- **Pros**: Distributed in-memory, Java-native
- **Cons**: Additional complexity, less ecosystem support
- **Rejected**: Redis provides better performance and tooling

## Configuration

```properties
# Redis connection
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.database=0
spring.redis.timeout=1000ms

# Connection pool
spring.redis.lettuce.pool.max-active=20
spring.redis.lettuce.pool.max-idle=10
spring.redis.lettuce.pool.min-idle=5

# Rate limiter specific
ratelimiter.redis.keyPrefix=ratelimiter
ratelimiter.redis.defaultTtl=3600
ratelimiter.redis.failOpen=true
```

## Monitoring and Metrics

- **Redis connection pool metrics**: Active/idle connections
- **Operation latency**: Time for Redis operations
- **Error rates**: Failed Redis operations
- **Memory usage**: Redis memory consumption
- **Cache hit rates**: Bucket reuse efficiency

## Future Considerations

- **Redis Sentinel**: Implement automatic failover
- **Redis Cluster**: Scale beyond single Redis instance
- **Compression**: Compress bucket data for memory efficiency
- **Regional Deployment**: Distribute Redis across regions for lower latency