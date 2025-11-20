# Adaptive Rate Limiting Guide

## Overview

Adaptive rate limiting automatically adjusts rate limits based on traffic patterns, system health, user behavior, and anomaly detection. This feature uses machine learning techniques to optimize rate limits without manual intervention.

## Table of Contents
- [Quick Start](#quick-start)
- [How It Works](#how-it-works)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Use Cases](#use-cases)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Quick Start

### Enable Adaptive Rate Limiting

Add to `application.properties`:

```properties
ratelimiter.adaptive.enabled=true
```

That's it! The system will start learning from traffic patterns and adapting limits automatically.

### Check Adaptive Status

```bash
curl http://localhost:8080/api/ratelimit/adaptive/your-key/status
```

Response:
```json
{
  "key": "api:checkout",
  "currentLimits": {
    "capacity": 1200,
    "refillRate": 120
  },
  "adaptiveStatus": {
    "mode": "ADAPTIVE",
    "confidence": 0.85,
    "recommendedLimits": {
      "capacity": 1200,
      "refillRate": 120
    },
    "reasoning": {
      "decision": "System stable with available capacity",
      "systemMetrics": "CPU: 45.0%, Memory: 60.0%, Response Time P95: 150ms, Error Rate: 0.050%",
      "trafficTrend": "Direction: INCREASING, Volatility: MEDIUM",
      "anomaly": "No anomalies detected",
      "userBehavior": "Avg Rate: 8.50 req/s, Burstiness: 0.85, Session: 180s"
    }
  },
  "timestamp": "2025-11-20T12:00:00Z"
}
```

## How It Works

### 1. Data Collection
The system continuously collects:
- **Traffic Events**: Every rate limit check is recorded
- **System Metrics**: CPU, memory, response times, error rates
- **User Behavior**: Request patterns, burstiness, session duration
- **Anomalies**: Statistical deviations from baseline

### 2. Analysis (Every 5 Minutes)
For each active key:
1. **Traffic Pattern Analysis**: Detect trends, seasonality, volatility
2. **System Health Check**: Monitor CPU, memory, response times
3. **Behavior Modeling**: Analyze user request patterns
4. **Anomaly Detection**: Identify unusual traffic (z-score based)

### 3. Decision Making
The ML model combines all signals and generates:
- **Should Adapt**: Boolean decision to change limits
- **Recommended Limits**: New capacity and refill rate
- **Confidence Score**: 0.0 to 1.0
- **Reasoning**: Human-readable explanation

### 4. Adaptation Rules

| Condition | Action | Reason |
|-----------|--------|--------|
| CPU >80% OR P95 >2s | Reduce 30% | System under stress |
| Critical anomaly detected | Reduce 40% | Protect against attack |
| High/Medium anomaly | Reduce 20% | Unusual traffic pattern |
| CPU <30% AND errors <0.1% | Increase 30% | System has capacity |
| CPU <50% AND errors <0.5% | Increase 10% | Stable with room |

### 5. Safety Constraints
- **Min Capacity**: 10 (configurable)
- **Max Capacity**: 100,000 (configurable)
- **Max Adjustment**: 2x original limit
- **Confidence Threshold**: 70% required
- **Manual Override**: Always respected

## Configuration

### Basic Configuration

```properties
# Enable/disable adaptive rate limiting
ratelimiter.adaptive.enabled=true

# Evaluation frequency (milliseconds)
ratelimiter.adaptive.evaluation-interval-ms=300000  # 5 minutes

# Minimum confidence for adaptation
ratelimiter.adaptive.min-confidence-threshold=0.7

# Maximum adjustment factor (2.0 = can double or halve limits)
ratelimiter.adaptive.max-adjustment-factor=2.0
```

### Safety Constraints

```properties
# Minimum allowed capacity
ratelimiter.adaptive.min-capacity=10

# Maximum allowed capacity
ratelimiter.adaptive.max-capacity=100000

# Learning window for pattern analysis
ratelimiter.adaptive.learning-window-days=30

# Minimum data points before adapting
ratelimiter.adaptive.min-data-points=1000
```

### Tuning for Your Use Case

#### High-Traffic APIs
```properties
ratelimiter.adaptive.evaluation-interval-ms=180000  # 3 minutes (faster response)
ratelimiter.adaptive.max-adjustment-factor=1.5  # More conservative
ratelimiter.adaptive.min-confidence-threshold=0.8  # Higher confidence required
```

#### Low-Traffic APIs
```properties
ratelimiter.adaptive.evaluation-interval-ms=600000  # 10 minutes (less overhead)
ratelimiter.adaptive.min-data-points=100  # Adapt with less data
```

#### Aggressive Optimization
```properties
ratelimiter.adaptive.max-adjustment-factor=3.0  # Allow 3x changes
ratelimiter.adaptive.min-confidence-threshold=0.6  # Lower threshold
```

## API Endpoints

### Get Adaptive Status
```bash
GET /api/ratelimit/adaptive/{key}/status
```

Returns current limits, recommendations, and reasoning.

### Set Manual Override
```bash
POST /api/ratelimit/adaptive/{key}/override
Content-Type: application/json

{
  "capacity": 2000,
  "refillRate": 200,
  "reason": "Black Friday promotion - pre-approved increase"
}
```

Disables adaptive behavior for this key and sets fixed limits.

### Remove Manual Override
```bash
DELETE /api/ratelimit/adaptive/{key}/override
```

Resumes adaptive behavior for the key.

### Get Adaptive Configuration
```bash
GET /api/ratelimit/adaptive/config
```

Returns current adaptive rate limiting configuration.

### Enhanced Rate Limit Check
```bash
POST /api/ratelimit/check
Content-Type: application/json

{
  "key": "api:checkout",
  "tokens": 1
}
```

Response includes optional `adaptiveInfo`:
```json
{
  "key": "api:checkout",
  "tokensRequested": 1,
  "allowed": true,
  "adaptiveInfo": {
    "originalLimits": {
      "capacity": 1000,
      "refillRate": 100
    },
    "currentLimits": {
      "capacity": 1200,
      "refillRate": 120
    },
    "adaptationReason": "System stable with available capacity",
    "adjustmentTimestamp": "2025-11-20T11:55:00Z",
    "nextEvaluationIn": "PT5M"
  }
}
```

## Use Cases

### 1. E-commerce Platform - Seasonal Adaptation

**Scenario**: Prepare for Black Friday automatically

```bash
# Before Black Friday (normal traffic)
GET /api/ratelimit/adaptive/api:checkout/status
# Response: capacity: 1000, mode: ADAPTIVE

# System learns from increasing traffic over days
# Automatically increases capacity as Black Friday approaches

# During Black Friday (high traffic)
GET /api/ratelimit/adaptive/api:checkout/status
# Response: capacity: 2500, mode: ADAPTIVE, reasoning: "Increasing trend detected"
```

### 2. API Gateway - Load-Based Protection

**Scenario**: Backend service degradation

```bash
# Normal operation
# CPU: 45%, Response Time: 200ms, Capacity: 2000

# Backend slows down (response time increases)
# Next evaluation cycle detects:
# CPU: 75%, Response Time: 1800ms

# System automatically reduces to protect backend
# New capacity: 1400 (30% reduction)
# Reasoning: "System approaching capacity limits"
```

### 3. SaaS Platform - Tier-Based Adaptation

**Scenario**: Different tiers with adaptive limits

```properties
# Configure base limits per tier
ratelimiter.patterns.api:premium:*.capacity=5000
ratelimiter.patterns.api:standard:*.capacity=1000
ratelimiter.patterns.api:free:*.capacity=100

# Adaptive adjusts within tier constraints
# Premium tier: 2500-10000 (5000 ± 2x)
# Standard tier: 500-2000 (1000 ± 2x)
# Free tier: 50-200 (100 ± 2x)
```

### 4. DDoS Protection - Anomaly Response

**Scenario**: Sudden traffic spike detected

```bash
# Normal traffic: 100 req/s
# Baseline established over 30 days

# Sudden spike: 1000 req/s (10x increase)
# Anomaly detector: z-score = 8.5 (CRITICAL)

# System response:
# - Reduce capacity by 40%
# - Alert operations team
# - Reasoning: "Critical anomaly: SPIKE detected"
```

## Best Practices

### 1. Start Conservative
```properties
# Initial rollout
ratelimiter.adaptive.enabled=false  # Start disabled
ratelimiter.adaptive.max-adjustment-factor=1.5  # Limit changes to 50%
ratelimiter.adaptive.min-confidence-threshold=0.8  # Require high confidence
```

### 2. Monitor Adaptations
```bash
# Check status regularly
watch -n 60 'curl http://localhost:8080/api/ratelimit/adaptive/my-key/status | jq'

# Look for:
# - Frequent adaptations (may need tuning)
# - Low confidence scores (insufficient data)
# - Unexpected reasoning
```

### 3. Use Manual Overrides for Events
```bash
# Known high-traffic event (product launch)
curl -X POST http://localhost:8080/api/ratelimit/adaptive/api:product/override \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 10000,
    "refillRate": 1000,
    "reason": "Product launch event - pre-approved capacity increase"
  }'

# After event
curl -X DELETE http://localhost:8080/api/ratelimit/adaptive/api:product/override
```

### 4. Gradual Rollout
1. Enable for low-risk keys first
2. Monitor for 1 week
3. Expand to more critical keys
4. Tune based on observations

### 5. Set Appropriate Constraints
```properties
# Critical API - conservative
ratelimiter.adaptive.max-capacity=5000  # Hard upper limit

# Internal API - aggressive
ratelimiter.adaptive.max-capacity=50000  # Allow high scaling
```

## Troubleshooting

### Limits Not Adapting

**Check 1**: Is adaptive enabled?
```properties
ratelimiter.adaptive.enabled=true
```

**Check 2**: Enough data collected?
```bash
# Requires minimum data points (default: 1000)
# Check status for data collection progress
curl http://localhost:8080/api/ratelimit/adaptive/my-key/status | jq '.adaptiveStatus.confidence'
# Low confidence (<0.5) = insufficient data
```

**Check 3**: Check logs
```bash
grep "Adaptive" application.log
# Look for: "Adaptive evaluation completed"
```

### Too Frequent Adaptations

**Solution**: Increase evaluation interval
```properties
ratelimiter.adaptive.evaluation-interval-ms=600000  # 10 minutes
```

### Adaptations Too Aggressive

**Solution 1**: Reduce max adjustment factor
```properties
ratelimiter.adaptive.max-adjustment-factor=1.3  # Max 30% change
```

**Solution 2**: Increase confidence threshold
```properties
ratelimiter.adaptive.min-confidence-threshold=0.85
```

### High Memory Usage

**Solution**: Limit history size
- Each key stores ~1KB of history
- Default max: 10,000 data points per key
- Consider clearing history for inactive keys

```bash
# System automatically cleans up based on:
ratelimiter.cleanupIntervalMs=60000  # Existing setting
```

### Unexpected Reasoning

**Debug**: Check component inputs
```bash
curl http://localhost:8080/api/ratelimit/adaptive/my-key/status | jq '.adaptiveStatus.reasoning'

# Examine:
# - systemMetrics: Are values accurate?
# - trafficTrend: Does direction make sense?
# - anomaly: False positive detection?
# - userBehavior: Unexpected patterns?
```

## Performance Considerations

### Latency Impact
- **Rate Limit Check**: +0.1ms (traffic recording)
- **Evaluation Cycle**: Runs in background thread
- **No user-facing latency** during normal operation

### Memory Usage
- **Baseline**: ~100MB
- **Per Active Key**: ~1KB
- **1000 active keys**: ~101MB total

### CPU Usage
- **Evaluation**: <1% CPU during 5-minute cycle
- **Negligible impact** on rate limiting performance

## Security Considerations

- **Manual Override Authentication**: Require proper authorization
- **Audit Logging**: All adaptations and overrides logged
- **Rate Limit Manual Overrides**: Prevent override abuse
- **DDoS Protection**: Anomaly detection provides automatic protection

## Next Steps

1. **Enable for Testing**: Start with `enabled=false`, monitor status
2. **Gradual Rollout**: Enable for low-risk keys first
3. **Monitor & Tune**: Adjust configuration based on observations
4. **Document Learnings**: Track what works for your traffic patterns

## Support

For issues or questions:
- Check logs: `grep "Adaptive" application.log`
- Review ADR-006: Adaptive Rate Limiting
- Open GitHub issue with adaptive status output
