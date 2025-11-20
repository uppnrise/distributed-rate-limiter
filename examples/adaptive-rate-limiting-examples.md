# Adaptive Rate Limiting Examples

This document provides practical examples of using the adaptive rate limiting feature.

## Quick Start Example

### 1. Enable Adaptive Rate Limiting

Add to `application.properties`:
```properties
ratelimiter.adaptive.enabled=true
```

### 2. Make Some Rate Limit Requests

```bash
# Generate some traffic
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/ratelimit/check \
    -H "Content-Type: application/json" \
    -d '{"key":"api:test","tokens":1}'
  sleep 0.1
done
```

### 3. Check Adaptive Status

```bash
curl http://localhost:8080/api/ratelimit/adaptive/api:test/status | jq
```

Response:
```json
{
  "key": "api:test",
  "currentLimits": {
    "capacity": 10,
    "refillRate": 2
  },
  "adaptiveStatus": {
    "mode": "STATIC",
    "confidence": 0.3,
    "recommendedLimits": {
      "capacity": 10,
      "refillRate": 2
    },
    "reasoning": {}
  },
  "timestamp": "2025-11-20T12:30:00Z"
}
```

Note: Initially in STATIC mode because we need more data for learning.

## E-commerce Platform Example

### Scenario: Black Friday Traffic Surge

```bash
# Step 1: Configure base limits for checkout API
curl -X POST http://localhost:8080/api/ratelimit/config/keys/api:checkout \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 1000,
    "refillRate": 100
  }'

# Step 2: Generate normal traffic pattern
for i in {1..200}; do
  curl -X POST http://localhost:8080/api/ratelimit/check \
    -H "Content-Type: application/json" \
    -d '{"key":"api:checkout","tokens":1}' &
  sleep 0.05
done
wait

# Step 3: Wait for evaluation cycle (5 minutes)
sleep 300

# Step 4: Simulate increased traffic (Black Friday)
for i in {1..500}; do
  curl -X POST http://localhost:8080/api/ratelimit/check \
    -H "Content-Type: application/json" \
    -d '{"key":"api:checkout","tokens":1}' &
  sleep 0.02
done
wait

# Step 5: Check adaptive response
curl http://localhost:8080/api/ratelimit/adaptive/api:checkout/status | jq

# Expected: System detects increasing trend and may recommend higher limits
```

## API Gateway Example

### Scenario: Backend Service Degradation

```bash
# Step 1: Configure API gateway key
curl -X POST http://localhost:8080/api/ratelimit/config/keys/api:backend \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 2000,
    "refillRate": 200
  }'

# Step 2: Generate normal traffic
for i in {1..300}; do
  curl -X POST http://localhost:8080/api/ratelimit/check \
    -H "Content-Type: application/json" \
    -d '{"key":"api:backend","tokens":1}' &
  sleep 0.01
done
wait

# Step 3: When backend degrades (simulated by system metrics)
# The adaptive system will detect high CPU or response times
# and automatically reduce limits

# Check current status
curl http://localhost:8080/api/ratelimit/adaptive/api:backend/status | jq '.adaptiveStatus.reasoning'

# Expected reasoning might include:
# "System under stress - CPU: 82%, Response Time P95: 2100ms"
```

## Manual Override Example

### Scenario: Planned Marketing Campaign

```bash
# Step 1: Before campaign - set manual override
curl -X POST http://localhost:8080/api/ratelimit/adaptive/api:landing/override \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 5000,
    "refillRate": 500,
    "reason": "Email campaign launching - 50K recipients expected"
  }'

# Step 2: Verify override is active
curl http://localhost:8080/api/ratelimit/adaptive/api:landing/status | jq

# Expected: Shows override is active with reason

# Step 3: During campaign - test rate limiting
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key":"api:landing","tokens":1}' | jq

# Step 4: After campaign - remove override
curl -X DELETE http://localhost:8080/api/ratelimit/adaptive/api:landing/override

# Step 5: Verify adaptive mode resumed
curl http://localhost:8080/api/ratelimit/adaptive/api:landing/status | jq
```

## DDoS Protection Example

### Scenario: Sudden Traffic Spike Detection

```bash
# Step 1: Establish normal baseline
for i in {1..500}; do
  curl -X POST http://localhost:8080/api/ratelimit/check \
    -H "Content-Type: application/json" \
    -d '{"key":"api:public","tokens":1}' &
  sleep 0.1
done
wait

# Step 2: Wait for baseline to establish
sleep 300

# Step 3: Simulate sudden spike (10x normal traffic)
for i in {1..5000}; do
  curl -X POST http://localhost:8080/api/ratelimit/check \
    -H "Content-Type: application/json" \
    -d '{"key":"api:public","tokens":1}' &
  sleep 0.01
done
wait

# Step 4: Check anomaly detection
curl http://localhost:8080/api/ratelimit/adaptive/api:public/status | jq '.adaptiveStatus.reasoning.anomaly'

# Expected: "Detected: true, Severity: CRITICAL, Type: SPIKE"
```

## SaaS Platform - Tier-Based Example

### Scenario: Different Adaptive Behaviors per Tier

```bash
# Configure premium tier
curl -X POST http://localhost:8080/api/ratelimit/config/patterns/api:premium:* \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 5000,
    "refillRate": 500
  }'

# Configure standard tier  
curl -X POST http://localhost:8080/api/ratelimit/config/patterns/api:standard:* \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 1000,
    "refillRate": 100
  }'

# Test premium user
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key":"api:premium:user123","tokens":1}' | jq

# Test standard user
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key":"api:standard:user456","tokens":1}' | jq

# Check adaptive status for each
curl http://localhost:8080/api/ratelimit/adaptive/api:premium:user123/status | jq
curl http://localhost:8080/api/ratelimit/adaptive/api:standard:user456/status | jq
```

## Monitoring Example

### Check All Adaptive Configuration

```bash
# Get adaptive configuration
curl http://localhost:8080/api/ratelimit/adaptive/config | jq

# Response shows:
{
  "enabled": true,
  "evaluationIntervalMs": 300000,
  "minConfidenceThreshold": 0.7,
  "maxAdjustmentFactor": 2.0,
  "minCapacity": 10,
  "maxCapacity": 100000
}
```

### Monitor Adaptive Info in Rate Limit Responses

```bash
# Make request and see adaptive info
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key":"api:monitored","tokens":1}' | jq

# Response includes:
{
  "key": "api:monitored",
  "tokensRequested": 1,
  "allowed": true,
  "adaptiveInfo": {
    "originalLimits": {
      "capacity": 1000,
      "refillRate": 100
    },
    "currentLimits": {
      "capacity": 1100,
      "refillRate": 110
    },
    "adaptationReason": "System stable with available capacity",
    "adjustmentTimestamp": "2025-11-20T12:25:00Z",
    "nextEvaluationIn": "PT5M"
  }
}
```

## Integration Test Script

Save this as `test-adaptive.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"
KEY="test:adaptive:$(date +%s)"

echo "Testing Adaptive Rate Limiting"
echo "==============================="
echo "Using key: $KEY"
echo ""

# 1. Configure key
echo "1. Configuring key..."
curl -s -X POST "$BASE_URL/api/ratelimit/config/keys/$KEY" \
  -H "Content-Type: application/json" \
  -d '{"capacity": 100, "refillRate": 10}' | jq

# 2. Generate traffic
echo ""
echo "2. Generating traffic (100 requests)..."
for i in {1..100}; do
  curl -s -X POST "$BASE_URL/api/ratelimit/check" \
    -H "Content-Type: application/json" \
    -d "{\"key\":\"$KEY\",\"tokens\":1}" > /dev/null
  sleep 0.05
done

# 3. Check status
echo ""
echo "3. Checking adaptive status..."
curl -s "$BASE_URL/api/ratelimit/adaptive/$KEY/status" | jq

# 4. Set override
echo ""
echo "4. Setting manual override..."
curl -s -X POST "$BASE_URL/api/ratelimit/adaptive/$KEY/override" \
  -H "Content-Type: application/json" \
  -d '{"capacity": 200, "refillRate": 20, "reason": "Test override"}' | jq

# 5. Verify override
echo ""
echo "5. Verifying override..."
curl -s "$BASE_URL/api/ratelimit/adaptive/$KEY/status" | jq

# 6. Remove override
echo ""
echo "6. Removing override..."
curl -s -X DELETE "$BASE_URL/api/ratelimit/adaptive/$KEY/override" | jq

echo ""
echo "Test complete!"
```

Run it:
```bash
chmod +x test-adaptive.sh
./test-adaptive.sh
```

## Performance Test

Test the performance impact:

```bash
# Baseline (without adaptive)
ab -n 10000 -c 100 -p request.json -T application/json \
  http://localhost:8080/api/ratelimit/check

# With adaptive enabled
# (Results should show <1ms difference)
```

Where `request.json` contains:
```json
{"key":"perf:test","tokens":1}
```

## Troubleshooting Examples

### Check Why Limits Aren't Adapting

```bash
# Get full status with reasoning
curl http://localhost:8080/api/ratelimit/adaptive/my-key/status | jq

# Check confidence level
curl http://localhost:8080/api/ratelimit/adaptive/my-key/status | \
  jq '.adaptiveStatus.confidence'

# If confidence < 0.7, need more data
# Generate more traffic and wait for evaluation cycle
```

### View Adaptation Reasoning

```bash
# Get detailed reasoning
curl http://localhost:8080/api/ratelimit/adaptive/my-key/status | \
  jq '.adaptiveStatus.reasoning'

# Example output:
{
  "decision": "System stable with available capacity",
  "systemMetrics": "CPU: 45.0%, Memory: 60.0%, Response Time P95: 150ms",
  "trafficTrend": "Direction: INCREASING, Volatility: MEDIUM",
  "anomaly": "No anomalies detected",
  "userBehavior": "Avg Rate: 8.5 req/s, Burstiness: 0.85"
}
```

## Best Practices Examples

### Conservative Start

```properties
# Start with conservative settings
ratelimiter.adaptive.enabled=true
ratelimiter.adaptive.max-adjustment-factor=1.3  # Max 30% change
ratelimiter.adaptive.min-confidence-threshold=0.8  # High confidence required
ratelimiter.adaptive.evaluation-interval-ms=600000  # 10 minutes
```

### Aggressive Optimization

```properties
# For stable systems that can handle changes
ratelimiter.adaptive.max-adjustment-factor=2.5  # Allow larger changes
ratelimiter.adaptive.min-confidence-threshold=0.6  # Lower threshold
ratelimiter.adaptive.evaluation-interval-ms=180000  # 3 minutes
```

## Summary

These examples demonstrate:
- ✅ Basic configuration and usage
- ✅ Real-world use cases (e-commerce, API gateway, SaaS, DDoS)
- ✅ Manual override for planned events
- ✅ Monitoring and troubleshooting
- ✅ Integration testing
- ✅ Performance testing
- ✅ Best practices

For more details, see:
- [Adaptive Rate Limiting Guide](../ADAPTIVE_RATE_LIMITING.md)
- [ADR-006](../adr/006-adaptive-rate-limiting.md)
- [API Documentation](http://localhost:8080/swagger-ui/index.html)
