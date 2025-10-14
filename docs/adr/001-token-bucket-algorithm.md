# ADR-001: Rate Limiting Algorithms

## Status
Accepted (Updated with multiple algorithm support)

## Context

We need to implement rate limiting algorithms that provide:
- Fair distribution of requests over time
- Support for different use cases and performance requirements
- Burst handling capabilities
- Predictable behavior under load
- Efficient implementation for high-throughput scenarios

Several rate limiting algorithms were considered, each with different characteristics and trade-offs.

## Decision

We will implement **multiple rate limiting algorithms** to support different use cases:

### 1. Token Bucket Algorithm (Primary)
**Best for**: General-purpose API rate limiting with burst tolerance

- **Capacity**: Maximum number of tokens the bucket can hold
- **Refill Rate**: Number of tokens added per unit of time
- **Token Consumption**: Requests consume tokens from the bucket
- **Burst Handling**: Allows bursts up to the bucket capacity
- **Gradual Refill**: Tokens are added continuously over time

## Implementation Details

```java
public class TokenBucket {
    private volatile long tokens;
    private volatile long lastRefillTime;
    private final long capacity;
    private final long refillRate;
    
    public boolean tryConsume(long tokensToConsume) {
        refill();
        if (tokens >= tokensToConsume) {
            tokens -= tokensToConsume;
            return true;
        }
        return false;
    }
    
    private void refill() {
        long now = System.currentTimeMillis();
        long timePassed = now - lastRefillTime;
        long tokensToAdd = (timePassed * refillRate) / 1000;
        tokens = Math.min(capacity, tokens + tokensToAdd);
        lastRefillTime = now;
    }
}
```

## Consequences

### Positive
- **Burst Tolerance**: Clients can make burst requests up to bucket capacity
- **Smooth Traffic**: Refill rate controls average request rate over time
- **Fair Resource Allocation**: Each key gets its own bucket
- **Efficient**: O(1) time complexity for rate limit checks
- **Intuitive Configuration**: Capacity and refill rate are easy to understand

### Negative
- **Memory Usage**: Each active key requires its own bucket state
- **Burst Abuse**: Clients could consume all tokens immediately and wait for refill
- **Time Precision**: Relies on system time for accurate refill calculations

## Alternatives Considered

### 2. Sliding Window Algorithm
**Best for**: Strict rate enforcement with precise timing

- **Implementation**: Tracks request timestamps within a rolling window
- **Memory**: ~8KB per active key  
- **Behavior**: More precise than Token Bucket, prevents window boundary issues
- **Use Cases**: Critical APIs requiring strict adherence to rate limits

### 3. Fixed Window Counter Algorithm  
**Best for**: Memory-efficient rate limiting with predictable resets

- **Implementation**: Simple counter that resets at fixed time intervals
- **Memory**: ~4KB per active key (50% reduction)
- **Behavior**: Clear reset boundaries, potential for boundary traffic spikes
- **Use Cases**: High-scale scenarios, simple quotas, memory-constrained environments

## Algorithm Comparison

| Algorithm | Memory/Key | CPU Overhead | Burst Handling | Reset Predictability | Use Case |
|-----------|------------|--------------|----------------|---------------------|----------|
| Token Bucket | ~8KB | Baseline | Excellent | Poor | General APIs |
| Sliding Window | ~8KB | +25% | Good | Good | Critical APIs |
| Fixed Window | ~4KB | -20% | Boundary Risk | Excellent | High Scale |
| Leaky Bucket | ~16KB | +40% | None (Traffic Shaping) | Excellent | Traffic Shaping |

## Alternatives Considered

### Leaky Bucket (Now Implemented)
- **Pros**: Enforces constant output rate, excellent for traffic shaping
- **Cons**: No burst allowance, more complex queue management, higher memory usage
- **Decision**: Implemented for specialized traffic shaping use cases

### Hierarchical Token Bucket
- **Pros**: Multi-level rate limiting
- **Cons**: Implementation complexity
- **Decision**: Deferred - can be built on top of existing algorithms

## Configuration Parameters

- `capacity`: Maximum bucket size (default: 10 tokens)
- `refillRate`: Tokens per second (default: 2 tokens/second)
- `cleanupIntervalMs`: Cleanup frequency for unused buckets (default: 60000ms)

## Algorithm Selection Guidelines

### Choose Token Bucket When:
- General-purpose API rate limiting
- Burst tolerance is desired
- User experience is priority
- Memory usage is not a constraint

### Choose Sliding Window When:
- Strict rate enforcement is critical
- Preventing abuse is top priority
- Precise timing control is needed
- Moderate scale (thousands of keys)

### Choose Fixed Window When:
- Memory efficiency is critical
- High scale (millions of keys)
- Simple quotas are sufficient
- Predictable reset times are valued
- Budget/resource constraints exist

### Choose Leaky Bucket When:
- Traffic shaping is required
- Constant output rate is critical
- Downstream system protection is priority
- SLA compliance with consistent processing rates
- Network-like behavior is desired

## Future Considerations

- Monitor algorithm performance across different load patterns
- Consider adaptive algorithm selection based on key behavior
- Evaluate hybrid approaches combining multiple algorithms
- Implement algorithm migration utilities for production transitions