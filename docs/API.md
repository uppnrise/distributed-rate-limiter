# API Reference Documentation

This document provides comprehensive documentation for all available endpoints in the Distributed Rate Limiter API.

## Table of Contents

- [Authentication](#authentication)
- [Rate Limiting Operations](#rate-limiting-operations)
- [Configuration Management](#configuration-management)
- [Administrative Operations](#administrative-operations)
- [Performance Monitoring](#performance-monitoring)
- [Benchmarking](#benchmarking)
- [Metrics and Health](#metrics-and-health)
- [Error Handling](#error-handling)
- [Request/Response Examples](#request-response-examples)

## Authentication

### API Key Authentication

Optional API key authentication for rate limiting requests:

```json
{
  "key": "user:123",
  "tokens": 1,
  "apiKey": "your-api-key-here"
}
```

### Admin Authentication

Administrative endpoints require proper authentication credentials. Configure admin access in `application.properties`:

```properties
# Admin authentication
spring.security.user.name=${ADMIN_USERNAME:admin}
spring.security.user.password=${ADMIN_PASSWORD:changeme}
```

Access admin endpoints with HTTP Basic Authentication:

```bash
curl -u admin:changeme http://localhost:8080/admin/keys
```

## Rate Limiting Operations

### Check Rate Limit

**Endpoint**: `POST /api/ratelimit/check`

**Description**: Check if a request is allowed for the given key based on configured rate limits.

**Request Body**:
```json
{
  "key": "user:123",
  "tokens": 1,
  "apiKey": "optional-api-key"
}
```

**Response**:
```json
{
  "key": "user:123",
  "tokensRequested": 1,
  "allowed": true
}
```

**Status Codes**:
- `200 OK` - Request allowed
- `429 Too Many Requests` - Rate limit exceeded
- `401 Unauthorized` - Invalid API key
- `403 Forbidden` - IP address blocked

### Composite Rate Limiting (**NEW**)

**Endpoint**: `POST /api/ratelimit/check` (with composite configuration)

**Description**: Check rate limits using multiple algorithms with configurable combination logic for sophisticated rate limiting scenarios.

**Request Body**:
```json
{
  "key": "enterprise:customer:123",
  "tokens": 1,
  "algorithm": "COMPOSITE",
  "compositeConfig": {
    "limits": [
      {
        "name": "api_calls",
        "algorithm": "TOKEN_BUCKET",
        "capacity": 10000,
        "refillRate": 1000,
        "scope": "API",
        "weight": 1.0,
        "priority": 1
      },
      {
        "name": "bandwidth",
        "algorithm": "LEAKY_BUCKET",
        "capacity": 100,
        "refillRate": 50,
        "scope": "BANDWIDTH",
        "weight": 1.0,
        "priority": 2
      }
    ],
    "combinationLogic": "ALL_MUST_PASS",
    "weights": {
      "api_calls": 1.0,
      "bandwidth": 1.0
    },
    "hierarchical": false
  }
}
```

**Response (Allowed)**:
```json
{
  "key": "enterprise:customer:123",
  "tokensRequested": 1,
  "allowed": true,
  "componentResults": {
    "api_calls": {
      "allowed": true,
      "currentTokens": 9999,
      "capacity": 10000,
      "scope": "API"
    },
    "bandwidth": {
      "allowed": true,
      "currentTokens": 99,
      "capacity": 100,
      "scope": "BANDWIDTH"
    }
  },
  "limitingComponent": null,
  "combinationResult": {
    "logic": "ALL_MUST_PASS",
    "overallScore": 1.0,
    "componentScores": {
      "api_calls": 1.0,
      "bandwidth": 1.0
    }
  }
}
```

**Response (Rate Limited)**:
```json
{
  "key": "enterprise:customer:123",
  "tokensRequested": 1,
  "allowed": false,
  "componentResults": {
    "api_calls": {
      "allowed": false,
      "currentTokens": 0,
      "capacity": 10000,
      "scope": "API"
    },
    "bandwidth": {
      "allowed": true,
      "currentTokens": 50,
      "capacity": 100,
      "scope": "BANDWIDTH"
    }
  },
  "limitingComponent": "api_calls",
  "combinationResult": {
    "logic": "ALL_MUST_PASS",
    "overallScore": 0.0,
    "componentScores": {
      "api_calls": 0.0,
      "bandwidth": 1.0
    }
  }
}
```

**Combination Logic Types**:
- `ALL_MUST_PASS`: AND operation - all components must allow the request
- `ANY_CAN_PASS`: OR operation - at least one component allows the request
- `WEIGHTED_AVERAGE`: Score-based evaluation using component weights (>50% threshold)
- `HIERARCHICAL_AND`: Scope-ordered evaluation (USER → TENANT → GLOBAL)
- `PRIORITY_BASED`: High-priority components checked first with fail-fast logic

**Use Cases**:
- **SaaS Platforms**: API calls + bandwidth + compliance limits
- **Financial Systems**: Transaction rate + volume + velocity checks
- **Gaming Platforms**: Actions per second + chat messages + resource usage
- **IoT Platforms**: Device commands + data transfer + connection limits

## Configuration Management

### Get Current Configuration

**Endpoint**: `GET /api/ratelimit/config`

**Description**: Retrieve current rate limiter configuration including default settings, per-key configurations, and patterns.

**Response**:
```json
{
  "capacity": 10,
  "refillRate": 2,
  "cleanupIntervalMs": 60000,
  "algorithm": "TOKEN_BUCKET",
  "keys": {
    "premium_user": {
      "capacity": 100,
      "refillRate": 20,
      "algorithm": "TOKEN_BUCKET"
    }
  },
  "patterns": {
    "api:*": {
      "capacity": 50,
      "refillRate": 10,
      "algorithm": "SLIDING_WINDOW"
    },
    "batch:*": {
      "capacity": 1000,
      "refillRate": 100,
      "algorithm": "FIXED_WINDOW"
    }
  }
}
```

### Update Default Configuration

**Endpoint**: `POST /api/ratelimit/config/default`

**Description**: Update default rate limiting configuration that applies to all keys without specific overrides.

**Request Body**:
```json
{
  "capacity": 20,
  "refillRate": 5,
  "cleanupIntervalMs": 30000,
  "algorithm": "TOKEN_BUCKET"
}
```

### Set Per-Key Configuration

**Endpoint**: `POST /api/ratelimit/config/keys/{key}`

**Description**: Set specific rate limiting configuration for a particular key.

**Path Parameters**:
- `key` - The rate limiting key (e.g., "premium_user", "user:123")

**Request Body**:
```json
{
  "capacity": 100,
  "refillRate": 25,
  "cleanupIntervalMs": 30000,
  "algorithm": "SLIDING_WINDOW"
}
```

### Set Pattern Configuration

**Endpoint**: `POST /api/ratelimit/config/patterns/{pattern}`

**Description**: Set rate limiting configuration for keys matching a pattern.

**Path Parameters**:
- `pattern` - The key pattern (e.g., "api:*", "user:*")

**Request Body**:
```json
{
  "capacity": 50,
  "refillRate": 15
}
```

### Remove Key Configuration

**Endpoint**: `DELETE /api/ratelimit/config/keys/{key}`

**Description**: Remove specific configuration for a key, reverting to pattern or default configuration.

### Remove Pattern Configuration

**Endpoint**: `DELETE /api/ratelimit/config/patterns/{pattern}`

**Description**: Remove pattern-based configuration.

### Reload Configuration

**Endpoint**: `POST /api/ratelimit/config/reload`

**Description**: Clear configuration caches and reload settings from current configuration.

### Get Configuration Statistics

**Endpoint**: `GET /api/ratelimit/config/stats`

**Description**: Get statistics about current configuration state.

**Response**:
```json
{
  "cacheSize": 25,
  "bucketCount": 15,
  "keyConfigCount": 3,
  "patternConfigCount": 2
}
```

## Administrative Operations

> **Note**: All admin endpoints require HTTP Basic Authentication.

### List Active Keys

**Endpoint**: `GET /admin/keys`

**Description**: List all currently active rate limiting keys with their statistics.

**Authentication**: Required

**Response**:
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

### Get Key Limits

**Endpoint**: `GET /admin/limits/{key}`

**Description**: Get current rate limiting configuration for a specific key.

**Authentication**: Required

**Path Parameters**:
- `key` - The rate limiting key

**Response**:
```json
{
  "key": "user:123",
  "capacity": 10,
  "refillRate": 2,
  "cleanupIntervalMs": 60000,
  "algorithm": "TOKEN_BUCKET"
}
```

### Update Key Limits

**Endpoint**: `PUT /admin/limits/{key}`

**Description**: Update rate limiting configuration for a specific key with immediate effect.

**Authentication**: Required

**Path Parameters**:
- `key` - The rate limiting key

**Request Body**:
```json
{
  "capacity": 50,
  "refillRate": 10,
  "cleanupIntervalMs": 30000,
  "algorithm": "TOKEN_BUCKET"
}
```

**Available Algorithms**:
- `TOKEN_BUCKET` - Allows bursts, gradual refill (default)
- `SLIDING_WINDOW` - Precise rate control within rolling window
- `FIXED_WINDOW` - Memory-efficient with predictable resets

### Remove Key Limits

**Endpoint**: `DELETE /admin/limits/{key}`

**Description**: Remove custom configuration for a key and clear its active bucket.

**Authentication**: Required

**Path Parameters**:
- `key` - The rate limiting key

## Performance Monitoring

### Store Performance Baseline

**Endpoint**: `POST /api/performance/baseline`

**Description**: Store a new performance baseline for future regression analysis.

**Request Body**:
```json
{
  "testName": "rate-limiter-load-test",
  "timestamp": "2024-01-15T10:30:00Z",
  "averageResponseTime": 15.5,
  "throughputPerSecond": 1250.0,
  "successRate": 98.5,
  "maxResponseTime": 45.2,
  "p95ResponseTime": 25.0,
  "errorRate": 1.5
}
```

### Analyze Performance Regression

**Endpoint**: `POST /api/performance/regression/analyze`

**Description**: Compare current performance against historical baselines to detect regressions.

**Query Parameters**:
- `responseTimeThreshold` (optional) - Custom response time regression threshold (percentage)
- `throughputThreshold` (optional) - Custom throughput regression threshold (percentage)
- `successRateThreshold` (optional) - Custom success rate regression threshold (percentage)

**Request Body**: Same as baseline format

**Response**:
```json
{
  "testName": "rate-limiter-load-test",
  "regressionDetected": true,
  "severity": "HIGH",
  "responseTimeRegression": {
    "current": 18.7,
    "baseline": 15.5,
    "change": 20.6,
    "threshold": 20.0,
    "isRegression": true
  },
  "throughputRegression": {
    "current": 1180.0,
    "baseline": 1250.0,
    "change": -5.6,
    "threshold": 15.0,
    "isRegression": false
  },
  "summary": "Performance regression detected in response time"
}
```

### Store and Analyze

**Endpoint**: `POST /api/performance/baseline/store-and-analyze`

**Description**: Perform regression analysis and then store the baseline for future comparisons.

### Get Performance Baselines

**Endpoint**: `GET /api/performance/baseline/{testName}`

**Description**: Retrieve historical performance baselines for a test.

**Path Parameters**:
- `testName` - Name of the test

**Query Parameters**:
- `limit` (optional, default: 10) - Maximum number of baselines to return

### Get Performance Trend

**Endpoint**: `GET /api/performance/trend/{testName}`

**Description**: Get performance trend data over time.

**Path Parameters**:
- `testName` - Name of the test

**Query Parameters**:
- `limit` (optional, default: 20) - Maximum number of data points

### Performance Health Check

**Endpoint**: `GET /api/performance/health`

**Description**: Check if performance monitoring service is operational.

## Benchmarking

### Run Benchmark

**Endpoint**: `POST /api/benchmark/run`

**Description**: Execute a performance benchmark with configurable parameters.

**Request Body**:
```json
{
  "concurrentThreads": 10,
  "requestsPerThread": 100,
  "durationSeconds": 30,
  "keyPrefix": "benchmark",
  "tokensPerRequest": 1,
  "delayBetweenRequestsMs": 0
}
```

**Response**:
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

**Endpoint**: `GET /api/benchmark/health`

**Description**: Check if benchmark service is operational.

## Metrics and Health

### Get System Metrics

**Endpoint**: `GET /metrics`

**Description**: Retrieve comprehensive system metrics.

**Response**:
```json
{
  "keyMetrics": {
    "user:123": {
      "requestCount": 150,
      "allowedCount": 145,
      "deniedCount": 5,
      "lastAccess": "2024-01-15T10:30:00Z"
    }
  },
  "redisConnected": true,
  "totalAllowedRequests": 1450,
  "totalDeniedRequests": 50
}
```

### Application Health

**Endpoint**: `GET /actuator/health`

**Description**: Spring Boot application health status.

### Detailed Metrics

**Endpoint**: `GET /actuator/metrics`

**Description**: Detailed application metrics from Spring Boot Actuator.

### Prometheus Metrics

**Endpoint**: `GET /actuator/prometheus`

**Description**: Prometheus-compatible metrics for monitoring integration.

## Error Handling

### Standard Error Response

All endpoints return consistent error responses:

```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request parameter",
  "path": "/api/ratelimit/check"
}
```

### Common Status Codes

| Code | Description | Common Scenarios |
|------|-------------|------------------|
| 200  | OK | Successful operation |
| 400  | Bad Request | Invalid request data, missing required fields |
| 401  | Unauthorized | Invalid or missing API key |
| 403  | Forbidden | IP address blocked, admin access denied |
| 404  | Not Found | Resource not found |
| 429  | Too Many Requests | Rate limit exceeded |
| 500  | Internal Server Error | System error, database connectivity issues |

### Rate Limiting Error Details

When rate limit is exceeded (HTTP 429), the response includes details:

```json
{
  "key": "user:123",
  "tokensRequested": 5,
  "allowed": false,
  "retryAfter": 30,
  "remainingCapacity": 0
}
```

## Request/Response Examples

### Complete Rate Limiting Flow

1. **Check if request is allowed**:
   ```bash
   curl -X POST http://localhost:8080/api/ratelimit/check \
     -H "Content-Type: application/json" \
     -d '{"key":"user:123","tokens":1}'
   ```

2. **If rate limited, wait and configure higher limits**:
   ```bash
   # Set higher limits for this user
   curl -X POST http://localhost:8080/api/ratelimit/config/keys/user:123 \
     -H "Content-Type: application/json" \
     -d '{"capacity":50,"refillRate":10}'
   ```

3. **Monitor performance**:
   ```bash
   curl http://localhost:8080/metrics
   ```

### Batch Configuration Update

```bash
# Update default limits
curl -X POST http://localhost:8080/api/ratelimit/config/default \
  -H "Content-Type: application/json" \
  -d '{"capacity":20,"refillRate":5}'

# Set API endpoint pattern limits
curl -X POST http://localhost:8080/api/ratelimit/config/patterns/api:v1:* \
  -H "Content-Type: application/json" \
  -d '{"capacity":100,"refillRate":20}'

# Reload to apply changes
curl -X POST http://localhost:8080/api/ratelimit/config/reload
```

### Performance Monitoring Workflow

```bash
# Store baseline after load test
curl -X POST http://localhost:8080/api/performance/baseline \
  -H "Content-Type: application/json" \
  -d '{
    "testName": "daily-load-test",
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
    "averageResponseTime": 15.2,
    "throughputPerSecond": 1200.0,
    "successRate": 99.1
  }'

# Later, analyze new results for regression
curl -X POST http://localhost:8080/api/performance/regression/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "testName": "daily-load-test",
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
    "averageResponseTime": 18.7,
    "throughputPerSecond": 1100.0,
    "successRate": 97.8
  }'
```

## API Integration Patterns

### Circuit Breaker Pattern

```bash
# Check service health before making requests
health=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)

if [ "$health" = "200" ]; then
  # Service is healthy, proceed with rate limit check
  curl -X POST http://localhost:8080/api/ratelimit/check \
    -H "Content-Type: application/json" \
    -d '{"key":"user:123","tokens":1}'
else
  echo "Service unavailable, circuit breaker open"
fi
```

### Graceful Degradation

```bash
# Try rate limit check with fallback
response=$(curl -s -w "%{http_code}" -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key":"user:123","tokens":1}')

http_code="${response: -3}"
body="${response%???}"

case $http_code in
  200)
    echo "Request allowed"
    ;;
  429)
    echo "Rate limited - applying backoff"
    sleep 1
    ;;
  *)
    echo "Service error - allowing request (fail open)"
    ;;
esac
```

This API reference provides comprehensive documentation for integrating with the Distributed Rate Limiter service. For interactive exploration of the API, visit the Swagger UI at `/swagger-ui/index.html` when the service is running.