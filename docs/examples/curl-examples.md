# cURL Examples

This document provides cURL command examples for testing the Distributed Rate Limiter API.

## Basic Rate Limit Check

### Simple Rate Limit Check

```bash
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{
    "key": "user:123",
    "tokens": 1
  }'
```

**Expected Response (200 OK):**
```json
{
  "key": "user:123",
  "tokensRequested": 1,
  "allowed": true
}
```

### With API Key

```bash
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{
    "key": "user:123",
    "tokens": 1,
    "apiKey": "your-api-key-here"
  }'
```

### Multiple Tokens

```bash
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{
    "key": "api:expensive-operation",
    "tokens": 5
  }'
```

## Rate Limit Exceeded Example

Rapidly send multiple requests to trigger rate limiting:

```bash
# Send 15 requests quickly (bucket capacity is typically 10)
for i in {1..15}; do
  echo "Request $i:"
  curl -X POST http://localhost:8080/api/ratelimit/check \
    -H "Content-Type: application/json" \
    -d '{
      "key": "test:user",
      "tokens": 1
    }' | jq .
  echo ""
done
```

**Rate Limited Response (429 Too Many Requests):**
```json
{
  "key": "test:user",
  "tokensRequested": 1,
  "allowed": false
}
```

## Configuration Management

### Get Current Configuration

```bash
curl -X GET http://localhost:8080/api/ratelimit/config | jq .
```

**Response:**
```json
{
  "capacity": 10,
  "refillRate": 2,
  "cleanupIntervalMs": 60000,
  "keys": {},
  "patterns": {}
}
```

### Update Default Configuration

```bash
curl -X POST http://localhost:8080/api/ratelimit/config/default \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 20,
    "refillRate": 5
  }'
```

### Set Per-Key Configuration

```bash
curl -X POST http://localhost:8080/api/ratelimit/config/keys/premium_user \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 100,
    "refillRate": 20
  }'
```

### Set Pattern Configuration

```bash
curl -X POST http://localhost:8080/api/ratelimit/config/patterns/api:* \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 50,
    "refillRate": 10
  }'
```

### Get Configuration Statistics

```bash
curl -X GET http://localhost:8080/api/ratelimit/config/stats | jq .
```

### Reload Configuration

```bash
curl -X POST http://localhost:8080/api/ratelimit/config/reload
```

## Administrative Operations

> **Note**: Admin endpoints require HTTP Basic Authentication. Configure admin credentials:
> ```properties
> spring.security.user.name=${ADMIN_USERNAME:admin}
> spring.security.user.password=${ADMIN_PASSWORD:changeme}
> ```

### Get All Active Keys

```bash
curl -u admin:changeme -X GET http://localhost:8080/admin/keys | jq .
```

**Response:**
```json
{
  "keys": [
    {
      "key": "user:123",
      "capacity": 10,
      "refillRate": 2,
      "cleanupIntervalMs": 60000,
      "algorithm": "TOKEN_BUCKET",
      "lastAccessTime": 1673123456789,
      "active": true
    }
  ],
  "totalKeys": 1,
  "activeKeys": 1
}
```

### Get Key Configuration

```bash
curl -u admin:changeme -X GET http://localhost:8080/admin/limits/user:123 | jq .
```

### Update Key Limits

```bash
curl -u admin:changeme -X PUT http://localhost:8080/admin/limits/premium_user \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 100,
    "refillRate": 25,
    "cleanupIntervalMs": 30000,
    "algorithm": "TOKEN_BUCKET"
  }'
```

### Remove Key Limits

```bash
curl -u admin:changeme -X DELETE http://localhost:8080/admin/limits/old_user
```

## Performance Monitoring

### Store Performance Baseline

```bash
curl -X POST http://localhost:8080/api/performance/baseline \
  -H "Content-Type: application/json" \
  -d '{
    "testName": "rate-limiter-load-test",
    "timestamp": "2024-01-15T10:30:00Z",
    "averageResponseTime": 15.5,
    "throughputPerSecond": 1250.0,
    "successRate": 98.5,
    "maxResponseTime": 45.2,
    "p95ResponseTime": 25.0,
    "errorRate": 1.5
  }'
```

### Analyze Performance Regression

```bash
curl -X POST http://localhost:8080/api/performance/regression/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "testName": "rate-limiter-load-test",
    "timestamp": "2024-01-15T11:00:00Z",
    "averageResponseTime": 18.7,
    "throughputPerSecond": 1180.0,
    "successRate": 97.2,
    "maxResponseTime": 52.1,
    "p95ResponseTime": 28.5,
    "errorRate": 2.8
  }' | jq .
```

### Get Performance Trend

```bash
curl -X GET "http://localhost:8080/api/performance/trend/rate-limiter-load-test?limit=10" | jq .
```

## Benchmarking

### Run Performance Benchmark

```bash
curl -X POST http://localhost:8080/api/benchmark/run \
  -H "Content-Type: application/json" \
  -d '{
    "concurrentThreads": 10,
    "requestsPerThread": 100,
    "durationSeconds": 30,
    "keyPrefix": "benchmark",
    "tokensPerRequest": 1,
    "delayBetweenRequestsMs": 0
  }' | jq .
```

**Response:**
```json
{
  "totalRequests": 1000,
  "successCount": 850,
  "errorCount": 0,
  "durationSeconds": 30.2,
  "throughputPerSecond": 28.15,
  "successRate": 85.0,
  "concurrentThreads": 10,
  "requestsPerThread": 100
}
```

### Benchmark Health Check

```bash
curl -X GET http://localhost:8080/api/benchmark/health
```

## System Metrics

### Get Custom Metrics

```bash
curl -X GET http://localhost:8080/metrics | jq .
```

### Get Application Health

```bash
curl -X GET http://localhost:8080/actuator/health | jq .
```

### Get Detailed Metrics

```bash
curl -X GET http://localhost:8080/actuator/metrics | jq .

# Get specific metric
curl -X GET http://localhost:8080/actuator/metrics/rate.limiter.requests | jq .
```

### Get Prometheus Metrics

```bash
curl -X GET http://localhost:8080/actuator/prometheus
```
```

## Health and Monitoring

### Application Health

```bash
curl -X GET http://localhost:8080/actuator/health | jq .
```

### Metrics

```bash
# Prometheus metrics
curl -X GET http://localhost:8080/actuator/prometheus

# JSON metrics
curl -X GET http://localhost:8080/actuator/metrics | jq .

# Specific metric
curl -X GET http://localhost:8080/actuator/metrics/rate.limiter.requests | jq .
```

## Testing Scenarios

### Load Testing

Test with multiple concurrent users:

```bash
#!/bin/bash
# load_test.sh

# Function to make requests for a specific user
test_user() {
  local user_id=$1
  local requests=$2
  
  for i in $(seq 1 $requests); do
    curl -s -X POST http://localhost:8080/api/ratelimit/check \
      -H "Content-Type: application/json" \
      -d "{\"key\":\"user:$user_id\",\"tokens\":1}" | \
      jq -r ".allowed" &
  done
}

# Test 5 users making 20 requests each
for user in {1..5}; do
  test_user $user 20 &
done

wait
```

### Pattern Testing

Test pattern-based configuration:

```bash
# Set pattern for API endpoints
curl -X POST http://localhost:8080/api/ratelimit/config/patterns/api:v1:* \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 30,
    "refillRate": 8
  }'

# Test various API endpoints that match the pattern
endpoints=("users" "orders" "products" "analytics")

for endpoint in "${endpoints[@]}"; do
  echo "Testing api:v1:$endpoint"
  curl -X POST http://localhost:8080/api/ratelimit/check \
    -H "Content-Type: application/json" \
    -d "{\"key\":\"api:v1:$endpoint\",\"tokens\":1}" | jq .
done
```

### Error Handling

Test various error conditions:

```bash
# Invalid JSON
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key": "test", invalid_json}'

# Missing required field
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{
    "tokens": 1
  }'

# Invalid token count
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{
    "key": "test",
    "tokens": 0
  }'
```

## Scripted Tests

### Complete API Test Suite

```bash
#!/bin/bash
# api_test_suite.sh

BASE_URL="http://localhost:8080"
API_KEY="your-api-key"

echo "=== Distributed Rate Limiter API Test Suite ==="

# Test 1: Basic rate limit check
echo "Test 1: Basic rate limit check"
response=$(curl -s -X POST $BASE_URL/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key":"test:basic","tokens":1}')
echo "Response: $response"
allowed=$(echo $response | jq -r '.allowed')
if [ "$allowed" = "true" ]; then
  echo "✓ PASS"
else
  echo "✗ FAIL"
fi
echo ""

# Test 2: Configuration management
echo "Test 2: Get configuration"
config=$(curl -s -X GET $BASE_URL/api/ratelimit/config)
echo "Current config: $config"
echo ""

# Test 3: Health check
echo "Test 3: Health check"
health=$(curl -s -X GET $BASE_URL/actuator/health)
status=$(echo $health | jq -r '.status')
if [ "$status" = "UP" ]; then
  echo "✓ PASS - Service is healthy"
else
  echo "✗ FAIL - Service health check failed"
fi
echo ""

# Test 4: Rate limiting behavior
echo "Test 4: Rate limiting behavior"
allowed_count=0
denied_count=0

for i in {1..20}; do
  response=$(curl -s -X POST $BASE_URL/api/ratelimit/check \
    -H "Content-Type: application/json" \
    -d '{"key":"test:behavior","tokens":1}')
  
  allowed=$(echo $response | jq -r '.allowed')
  if [ "$allowed" = "true" ]; then
    ((allowed_count++))
  else
    ((denied_count++))
  fi
done

echo "Allowed: $allowed_count, Denied: $denied_count"
if [ $denied_count -gt 0 ]; then
  echo "✓ PASS - Rate limiting is working"
else
  echo "⚠ WARNING - No requests were denied"
fi

echo ""
echo "=== Test Suite Complete ==="
```

Make the script executable and run it:

```bash
chmod +x api_test_suite.sh
./api_test_suite.sh
```

## Performance Testing

### Simple Throughput Test

```bash
#!/bin/bash
# throughput_test.sh

BASE_URL="http://localhost:8080"
DURATION=30  # seconds
CONCURRENT=10

echo "Running throughput test for ${DURATION}s with ${CONCURRENT} concurrent connections"

# Generate request file
cat > request.json << EOF
{
  "key": "perf:test:USER_ID",
  "tokens": 1
}
EOF

# Run Apache Bench (if available)
if command -v ab &> /dev/null; then
  ab -n 1000 -c $CONCURRENT -T 'application/json' \
     -p request.json \
     $BASE_URL/api/ratelimit/check
else
  echo "Apache Bench (ab) not available. Install with: apt-get install apache2-utils"
fi

# Cleanup
rm -f request.json
```

### Wrk Load Test (if wrk is available)

```bash
# Install wrk first: https://github.com/wg/wrk
wrk -t4 -c100 -d30s --timeout 10s \
  -s post_request.lua \
  http://localhost:8080/api/ratelimit/check
```

Where `post_request.lua` contains:
```lua
wrk.method = "POST"
wrk.body   = '{"key":"wrk:test","tokens":1}'
wrk.headers["Content-Type"] = "application/json"
```