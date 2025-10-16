# Configuration Management

The distributed rate limiter supports flexible configuration management with per-key overrides, pattern-based configurations, and dynamic reloading.

## Configuration Hierarchy

Configuration is resolved in the following order of precedence:

1. **Exact key match** - Specific configuration for an exact key
2. **Pattern match** - Configuration for keys matching a pattern (first match wins)
3. **Default configuration** - Fallback configuration for all other keys

## Configuration Options

### Application Properties

Configure default limits in `application.properties`:

```properties
# Default limits (applied to all keys unless overridden)
ratelimiter.capacity=10
ratelimiter.refillRate=2
ratelimiter.cleanupIntervalMs=60000
ratelimiter.algorithm=TOKEN_BUCKET

# Per-key overrides (exact key matching)
ratelimiter.keys.premium_user.capacity=50
ratelimiter.keys.premium_user.refillRate=10
ratelimiter.keys.premium_user.algorithm=TOKEN_BUCKET

# Pattern-based configurations (supports * wildcard)
ratelimiter.patterns.user:*.capacity=20
ratelimiter.patterns.user:*.refillRate=5
ratelimiter.patterns.user:*.algorithm=TOKEN_BUCKET

ratelimiter.patterns.api:*.capacity=100
ratelimiter.patterns.api:*.refillRate=50
ratelimiter.patterns.api:*.algorithm=SLIDING_WINDOW

ratelimiter.patterns.batch:*.capacity=1000
ratelimiter.patterns.batch:*.refillRate=100
ratelimiter.patterns.batch:*.algorithm=FIXED_WINDOW

# Traffic shaping with Leaky Bucket
ratelimiter.patterns.traffic:*.capacity=50
ratelimiter.patterns.traffic:*.refillRate=10
ratelimiter.patterns.traffic:*.algorithm=LEAKY_BUCKET

# Composite rate limiting (set algorithm to COMPOSITE and use API for detailed config)
ratelimiter.patterns.enterprise:*.algorithm=COMPOSITE
ratelimiter.patterns.enterprise:*.capacity=10000
ratelimiter.patterns.enterprise:*.refillRate=1000

ratelimiter.patterns.*:admin.capacity=1000
ratelimiter.patterns.*:admin.refillRate=500
```

### Pattern Matching

Patterns support the `*` wildcard character:

- `user:*` matches `user:123`, `user:abc`, etc.
- `*:admin` matches `user:admin`, `system:admin`, etc.
- `api:v1:*` matches `api:v1:users`, `api:v1:orders`, etc.
- `*` matches any key

## Dynamic Configuration API

### Get Current Configuration

```bash
curl http://localhost:8080/api/ratelimit/config
```

### Update Per-Key Configuration

```bash
curl -X POST http://localhost:8080/api/ratelimit/config/keys/premium_user \
  -H "Content-Type: application/json" \
  -d '{"capacity": 50, "refillRate": 10, "cleanupIntervalMs": 30000}'
```

### Update Pattern Configuration

```bash
curl -X POST http://localhost:8080/api/ratelimit/config/patterns/api:* \
  -H "Content-Type: application/json" \
  -d '{"capacity": 100, "refillRate": 50}'
```

### Update Default Configuration

```bash
curl -X POST http://localhost:8080/api/ratelimit/config/default \
  -H "Content-Type: application/json" \
  -d '{"capacity": 20, "refillRate": 5}'
```

### Remove Configurations

```bash
# Remove per-key configuration
curl -X DELETE http://localhost:8080/api/ratelimit/config/keys/premium_user

# Remove pattern configuration
curl -X DELETE http://localhost:8080/api/ratelimit/config/patterns/api:*
```

### Reload Configuration

Clear all caches and buckets to pick up configuration changes:

```bash
curl -X POST http://localhost:8080/api/ratelimit/config/reload
```

### Configuration Statistics

```bash
curl http://localhost:8080/api/ratelimit/config/stats
```

Returns:
```json
{
  "cacheSize": 5,
  "bucketCount": 3,
  "keyConfigCount": 2,
  "patternConfigCount": 1
}
```

## Examples

### Basic Rate Limiting

```bash
# Check rate limit for a user (uses default configuration)
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key": "user:123", "tokens": 1}'
```

### Pattern-Based Configuration

After setting up pattern `user:*` with capacity 20:

```bash
# Both requests use the same pattern configuration
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key": "user:alice", "tokens": 1}'

curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key": "user:bob", "tokens": 1}'
```

### Per-Key Override

```bash
# Set specific configuration for a VIP user
curl -X POST http://localhost:8080/api/ratelimit/config/keys/user:vip \
  -H "Content-Type: application/json" \
  -d '{"capacity": 1000, "refillRate": 100}'

# This user gets the VIP configuration, not the pattern
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key": "user:vip", "tokens": 50}'
```

## Algorithm Configuration

### Available Algorithms

The rate limiter supports four algorithms, each optimized for different scenarios:

#### TOKEN_BUCKET (Default)
- **Best for**: General API rate limiting with burst handling
- **Memory**: ~8KB per active key
- **Behavior**: Allows bursts up to capacity, smooth refill over time

#### SLIDING_WINDOW  
- **Best for**: Strict rate enforcement with precise timing
- **Memory**: ~8KB per active key
- **Behavior**: Tracks requests within sliding time window

#### FIXED_WINDOW
- **Best for**: Memory-efficient rate limiting with predictable resets
- **Memory**: ~4KB per active key (50% less than other algorithms)
- **Behavior**: Counter resets at fixed intervals, clear boundaries

#### LEAKY_BUCKET
- **Best for**: Traffic shaping and consistent output rates
- **Memory**: ~16KB per active key (due to queue storage)
- **Behavior**: Queue-based processing at constant rate, regardless of input bursts

### Algorithm Selection Examples

```properties
# High-throughput APIs - use Fixed Window for memory efficiency
ratelimiter.patterns.api:high-volume:*.algorithm=FIXED_WINDOW
ratelimiter.patterns.api:high-volume:*.capacity=1000

# Critical APIs - use Sliding Window for precise control
ratelimiter.patterns.api:critical:*.algorithm=SLIDING_WINDOW
ratelimiter.patterns.api:critical:*.capacity=100

# User-facing APIs - use Token Bucket for good UX
ratelimiter.patterns.user:*.algorithm=TOKEN_BUCKET
ratelimiter.patterns.user:*.capacity=50

# Traffic shaping - use Leaky Bucket for consistent output
ratelimiter.patterns.gateway:*.algorithm=LEAKY_BUCKET
ratelimiter.patterns.gateway:*.capacity=100
ratelimiter.patterns.gateway:*.refillRate=20
```

## Notes

- Configuration changes are applied immediately but only affect new buckets
- Use the reload endpoint to clear existing buckets and apply changes to all keys
- Partial configuration is supported (e.g., only setting capacity will use defaults for other values)
- Configuration is stored in memory and will be lost on application restart unless persisted in application.properties
- Algorithm changes require a configuration reload to take effect on existing keys