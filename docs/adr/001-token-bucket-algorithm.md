# ADR-001: Rate Limiting Algorithms

## Status
Accepted

## Context

We need to implement rate limiting algorithms that provide:
- Fair distribution of requests over time
- Burst handling capabilities  
- Predictable behavior under load
- Efficient implementation for high-throughput scenarios
- General-purpose API protection with intuitive configuration

After evaluating multiple rate limiting algorithms, we need a primary algorithm that balances performance, usability, and burst tolerance for typical API rate limiting scenarios.

## Decision

We will implement **Token Bucket Algorithm** as the primary rate limiting algorithm with the following characteristics:

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

### Sliding Window Algorithm
- **Pros**: More precise timing than Token Bucket, prevents window boundary issues
- **Cons**: Higher memory usage, more complex calculations
- **Decision**: Implemented as alternative for scenarios requiring strict rate enforcement

### Fixed Window Counter Algorithm  
- **Pros**: Memory efficient (~4KB per key), predictable reset times
- **Cons**: Potential for boundary traffic spikes, less smooth traffic distribution
- **Decision**: Implemented for high-scale scenarios with memory constraints

### Leaky Bucket
- **Pros**: Enforces constant output rate, excellent for traffic shaping
- **Cons**: No burst allowance, more complex queue management, higher memory usage
- **Decision**: Implemented as specialized algorithm - see [ADR-004](./004-leaky-bucket-algorithm.md)

### Hierarchical Token Bucket
- **Pros**: Multi-level rate limiting
- **Cons**: Implementation complexity
- **Decision**: Deferred - can be built on top of existing algorithms

## Algorithm Comparison

| Algorithm | Memory/Key | CPU Overhead | Burst Handling | Reset Predictability | Use Case |
|-----------|------------|--------------|----------------|---------------------|----------|
| Token Bucket | ~8KB | Baseline | Excellent | Poor | General APIs |
| Sliding Window | ~8KB | +25% | Good | Good | Critical APIs |
| Fixed Window | ~4KB | -20% | Boundary Risk | Excellent | High Scale |

## Future Considerations

- Monitor algorithm performance across different load patterns
- Consider adaptive algorithm selection based on key behavior
- Evaluate hybrid approaches combining multiple algorithms
- Implement algorithm migration utilities for production transitions