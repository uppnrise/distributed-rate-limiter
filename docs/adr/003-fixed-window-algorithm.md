# ADR-003: Fixed Window Counter Algorithm

## Status
Accepted

## Context

While Token Bucket and Sliding Window algorithms serve most rate limiting needs effectively, there are scenarios requiring:

- **Memory Efficiency**: Applications with millions of keys need lower memory overhead per key
- **Predictable Reset Times**: Users benefit from knowing exactly when rate limits reset
- **Simple Logic**: Debugging and monitoring are easier with clear window boundaries
- **High Performance**: Minimal computational overhead for high-throughput scenarios

The existing algorithms have limitations for these use cases:
- **Token Bucket**: ~8KB per key, gradual refill makes reset times unpredictable
- **Sliding Window**: ~8KB per key, complex sliding calculations increase CPU usage

## Decision

We will implement a **Fixed Window Counter algorithm** as a third rate limiting option with the following characteristics:

- **Fixed Time Windows**: Windows align to fixed intervals (e.g., :00, :01, :02 for 1-minute windows)
- **Simple Counter**: Increment counter for each request, reset at window boundary
- **Memory Efficient**: ~4KB per active key (50% less than other algorithms)
- **Predictable Resets**: Clear window boundaries provide predictable behavior
- **Atomic Operations**: Thread-safe increments using atomic counters (local) or Redis Lua scripts (distributed)

## Implementation Details

### Local Implementation
```java
public class FixedWindow implements RateLimiter {
    private final AtomicInteger currentCount;
    private volatile long windowStartTime;
    private final long windowDurationMs;
    private final int capacity;
    
    public synchronized boolean tryConsume(int tokens) {
        long currentTime = System.currentTimeMillis();
        
        // Check if we need to reset the window
        if (currentTime - windowStartTime >= windowDurationMs) {
            resetWindow(currentTime);
        }
        
        // Check capacity and consume
        if (currentCount.get() + tokens > capacity) {
            return false;
        }
        
        currentCount.addAndGet(tokens);
        return true;
    }
}
```

### Distributed Implementation
Redis Lua script ensures atomic operations:
```lua
-- Calculate current window start time
local current_window_start = math.floor(current_time / window_duration) * window_duration

-- Reset counter if window has changed
if window_start ~= current_window_start then
    current_count = 0
    window_start = current_window_start
end

-- Check and update counter
if current_count + tokens_to_consume <= capacity then
    current_count = current_count + tokens_to_consume
    return {1, capacity - current_count}  -- success, remaining
else
    return {0, capacity - current_count}  -- failure, remaining
end
```

## Consequences

### Positive
- **Memory Efficiency**: 50% reduction in memory usage per key compared to other algorithms
- **CPU Performance**: 20% less CPU overhead due to simpler increment/check logic
- **Predictable Behavior**: Clear reset times improve user experience and debugging
- **Simple Monitoring**: Easy to understand and visualize window-based metrics
- **High Throughput**: Optimal for scenarios with many concurrent keys

### Negative
- **Traffic Spikes**: Potential for 2x burst at window boundaries (users can exhaust quota in last second of window, then immediately in first second of next window)
- **Less Smooth**: No gradual refill like Token Bucket, which may feel less "fair"
- **Window Alignment**: All users with same window duration share reset times, which could create synchronized load spikes

### Trade-offs
- **vs Token Bucket**: Less smooth but more memory efficient and predictable
- **vs Sliding Window**: Less precise but significantly more performant and memory efficient

## Use Cases

### Ideal Scenarios
- **High-Scale APIs**: Millions of keys requiring memory efficiency
- **Batch Processing**: Predictable quotas per time period
- **User-Facing Limits**: Clear communication about reset times
- **Resource Quotas**: Simple "X requests per hour" limits
- **Billing Integration**: Align windows with billing periods

### Configuration Examples
```properties
# Memory-efficient API rate limiting
ratelimiter.patterns.api:bulk:*.algorithm=FIXED_WINDOW
ratelimiter.patterns.api:bulk:*.capacity=1000
ratelimiter.patterns.api:bulk:*.refillRate=1  # window scaling factor

# User quotas with daily windows  
ratelimiter.patterns.user:*.algorithm=FIXED_WINDOW
ratelimiter.patterns.user:*.capacity=10000
ratelimiter.patterns.user:*.windowDurationMs=86400000  # 24 hours
```

## Alternatives Considered

### Hierarchical Token Bucket
- **Pros**: Combines burst handling with memory efficiency
- **Cons**: More complex implementation, less predictable than fixed windows
- **Decision**: Too complex for the target use cases

### Sliding Window with Sampling
- **Pros**: More accurate than fixed windows, some memory savings
- **Cons**: Still complex, memory savings not as significant
- **Decision**: Complexity not justified by marginal improvements

### Fixed Window with Burst Allowance
- **Pros**: Reduces boundary spike issues
- **Cons**: Adds complexity, reduces memory efficiency benefits
- **Decision**: Keep implementation simple, document boundary behavior

## Migration Path

### From Token Bucket
```bash
# Change algorithm, adjust capacity for window-based thinking
curl -X POST http://localhost:8080/api/ratelimit/config/patterns/api:* \
  -d '{"capacity":3600,"algorithm":"FIXED_WINDOW"}'  # 1 req/sec over 1 hour
```

### From Sliding Window
```bash  
# Direct migration with same capacity
curl -X POST http://localhost:8080/api/ratelimit/config/patterns/api:* \
  -d '{"capacity":100,"algorithm":"FIXED_WINDOW"}'  # Same 100 req/window
```

## Performance Characteristics

| Metric | Fixed Window | Token Bucket | Sliding Window |
|--------|--------------|--------------|----------------|
| Memory/Key | ~4KB | ~8KB | ~8KB |
| CPU Overhead | Baseline | +15% | +25% |
| Reset Predictability | Excellent | Poor | Good |
| Boundary Behavior | Spike Risk | Smooth | Smooth |
| Implementation Complexity | Simple | Medium | Complex |

## Monitoring Considerations

### Key Metrics
- `rate_limit_window_resets_total` - Track window reset frequency
- `rate_limit_boundary_spikes_total` - Monitor boundary traffic spikes  
- `rate_limit_memory_usage_bytes` - Confirm memory efficiency gains

### Alerting
- Alert on unusually high boundary spikes
- Monitor memory usage reduction vs baseline
- Track P95 latency improvements

## Future Considerations

- **Adaptive Windows**: Dynamically adjust window size based on traffic patterns
- **Burst Buffer**: Add small token bucket on top of fixed window to smooth boundaries
- **Window Jitter**: Randomize window start times per key to reduce synchronized resets
- **Multi-tier Windows**: Combine multiple window sizes (minute/hour/day) for complex quotas