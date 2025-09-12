# ADR-001: Token Bucket Algorithm

## Status
Accepted

## Context

We need to implement a rate limiting algorithm that provides:
- Fair distribution of requests over time
- Burst handling capabilities
- Predictable behavior under load
- Efficient implementation for high-throughput scenarios

Several rate limiting algorithms were considered:
- **Fixed Window**: Simple but allows traffic spikes at window boundaries
- **Sliding Window**: More accurate but computationally expensive
- **Token Bucket**: Allows bursts up to capacity, smooths traffic over time
- **Leaky Bucket**: Enforces constant output rate, no burst allowance

## Decision

We will implement the **Token Bucket algorithm** for rate limiting with the following characteristics:

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

### Fixed Window Counter
- **Pros**: Simple implementation, low memory usage
- **Cons**: Traffic spikes at window boundaries, uneven rate limiting
- **Rejected**: Does not provide smooth rate limiting

### Sliding Window Log
- **Pros**: Most accurate rate limiting
- **Cons**: High memory usage, complex implementation
- **Rejected**: Too complex for our performance requirements

### Leaky Bucket
- **Pros**: Enforces constant output rate
- **Cons**: No burst allowance, more complex queue management
- **Rejected**: Too restrictive for typical API usage patterns

## Configuration Parameters

- `capacity`: Maximum bucket size (default: 10 tokens)
- `refillRate`: Tokens per second (default: 2 tokens/second)
- `cleanupIntervalMs`: Cleanup frequency for unused buckets (default: 60000ms)

## Future Considerations

- Monitor bucket memory usage and implement more aggressive cleanup if needed
- Consider adaptive capacity based on historical usage patterns
- Evaluate sliding window hybrid approach for critical endpoints