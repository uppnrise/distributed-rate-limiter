# ADR-004: Leaky Bucket Algorithm for Traffic Shaping

## Status
Accepted

## Context

While Token Bucket, Sliding Window, and Fixed Window algorithms handle most rate limiting scenarios effectively, there are specific use cases requiring **traffic shaping** with constant output rates:

- **SLA Compliance**: APIs requiring guaranteed consistent processing rates
- **Downstream Protection**: Protecting backend systems from traffic bursts
- **Network-like Behavior**: Emulating network traffic shaping for testing
- **Quality of Service**: Ensuring predictable response times under varying load
- **Resource Smoothing**: Converting bursty input into steady output flow

The existing algorithms have limitations for traffic shaping:
- **Token Bucket**: Allows bursts which can overwhelm downstream systems
- **Sliding Window**: Permits uneven distribution within the window
- **Fixed Window**: Can create traffic spikes at window boundaries

## Decision

We implement **Leaky Bucket Algorithm** as a specialized rate limiting option for traffic shaping with the following characteristics:

- **Request Queuing**: Incoming requests are queued rather than immediately rejected
- **Constant Output Rate**: Processes requests at a steady, configurable rate regardless of input bursts
- **Traffic Shaping**: Converts irregular input traffic into smooth, predictable output
- **Queue Management**: Configurable queue capacity with overflow handling
- **Timeout Handling**: Requests exceeding queue time limits are rejected

## Implementation Details

### Core Components
```java
public class LeakyBucket implements RateLimiter {
    private final int queueCapacity;
    private final double leakRatePerSecond;
    private final long maxQueueTimeMs;
    private final BlockingQueue<QueuedRequest> requestQueue;
    private final ScheduledExecutorService leakExecutor;
    
    public CompletableFuture<Boolean> tryConsumeAsync(int tokens) {
        // Queue request for processing at leak rate
        // Return future that completes when processed
    }
    
    private void processRequests() {
        // Process queued requests at constant rate
        // Maintain steady output regardless of input bursts
    }
}
```

### Key Features
- **Asynchronous Processing**: Returns `CompletableFuture<Boolean>` for non-blocking operations
- **Queue Overflow Protection**: Rejects requests when queue capacity exceeded
- **Timeout Management**: Automatically rejects requests exceeding queue time limits
- **Graceful Shutdown**: Proper cleanup of executor threads and pending requests

## Consequences

### Positive
- **Predictable Output**: Guaranteed constant processing rate for SLA compliance
- **Traffic Smoothing**: Eliminates downstream impact of traffic bursts
- **Quality of Service**: Consistent response times under varying load
- **Flexible Queuing**: Configurable queue capacity and timeout handling
- **Non-blocking**: Asynchronous API prevents thread blocking

### Negative
- **Higher Memory Usage**: ~16KB per active key (2x Token Bucket due to queuing)
- **Increased CPU Overhead**: +40% due to continuous queue processing
- **Complexity**: More complex than other algorithms, harder to debug
- **Request Delays**: All requests experience processing delay (no immediate approval)
- **Queue Management**: Risk of request timeouts under heavy load

## Configuration Parameters

- `queueCapacity`: Maximum number of queued requests (default: 100)
- `leakRatePerSecond`: Processing rate in requests/second (default: 10)
- `maxQueueTimeMs`: Maximum time requests can wait in queue (default: 5000ms)
- `threadPoolSize`: Number of leak processing threads (default: 2)

## Use Cases

### Choose Leaky Bucket When:
- **SLA Requirements**: Need guaranteed consistent processing rates
- **Downstream Protection**: Backend systems can't handle traffic bursts  
- **Traffic Shaping**: Converting bursty API calls into steady backend load
- **Quality of Service**: Predictable response times are critical
- **Testing Scenarios**: Simulating network-like behavior for load testing

### Don't Choose Leaky Bucket When:
- **User Experience Priority**: Users expect immediate responses
- **Memory Constraints**: Limited memory for request queuing
- **Simple Rate Limiting**: Basic throttling without traffic shaping needs
- **High-Scale Scenarios**: Millions of concurrent keys (memory overhead)

## Performance Characteristics

| Metric | Value | Comparison to Token Bucket |
|--------|-------|---------------------------|
| **Memory per Key** | ~16KB | +100% (due to queuing) |
| **CPU Overhead** | +40% | Higher (continuous processing) |
| **Latency** | Variable (queue delay) | Higher (no immediate responses) |
| **Throughput** | Constant (by design) | Lower peak, consistent average |
| **Burst Handling** | Queue + Shape | No bursts (traffic shaping) |

## Monitoring and Metrics

Key metrics for Leaky Bucket monitoring:
- `queue_size`: Current number of queued requests
- `queue_wait_time`: Average time requests spend in queue  
- `overflow_rejections`: Requests rejected due to queue capacity
- `timeout_rejections`: Requests rejected due to queue timeout
- `processing_rate`: Actual vs configured leak rate

## Future Considerations

- **Adaptive Leak Rate**: Dynamic rate adjustment based on downstream capacity
- **Priority Queuing**: Multiple priority levels for different request types
- **Backpressure Signaling**: Notify clients when queues are approaching capacity
- **Queue Persistence**: Redis-backed queues for distributed deployments
- **Circuit Breaker Integration**: Automatic queue draining during downstream failures

## Related ADRs

- [ADR-001: Token Bucket Algorithm](./001-token-bucket-algorithm.md) - Primary rate limiting algorithm
- [ADR-002: Redis Distributed State](./002-redis-distributed-state.md) - Distributed state management
- [ADR-003: Fixed Window Counter Algorithm](./003-fixed-window-algorithm.md) - Memory-efficient alternative