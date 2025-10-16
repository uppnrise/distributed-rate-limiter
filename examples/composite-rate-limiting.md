# Composite Rate Limiting Examples

This document demonstrates how to use the new composite rate limiting feature with multiple algorithms and combination logic.

## Basic Composite Configuration

### Multi-Algorithm Rate Limiting

```bash
# Example: Enterprise API with both request rate and bandwidth limits
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{
    "key": "enterprise:customer:123",
    "tokens": 1,
    "algorithm": "COMPOSITE",
    "compositeConfig": {
      "limits": [
        {
          "name": "api_calls",
          "algorithm": "TOKEN_BUCKET",
          "scope": "API",
          "capacity": 1000,
          "refillRate": 100,
          "weight": 1.0,
          "priority": 1
        },
        {
          "name": "bandwidth",
          "algorithm": "LEAKY_BUCKET",
          "scope": "BANDWIDTH", 
          "capacity": 100,
          "refillRate": 50,
          "weight": 1.0,
          "priority": 2
        }
      ],
      "combinationLogic": "ALL_MUST_PASS"
    }
  }'
```

### Hierarchical Rate Limiting

```bash
# Example: User limits within tenant limits
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{
    "key": "user:john_doe",
    "tokens": 5,
    "algorithm": "COMPOSITE",
    "compositeConfig": {
      "hierarchical": true,
      "limits": [
        {
          "name": "user_limit",
          "algorithm": "TOKEN_BUCKET",
          "scope": "USER",
          "capacity": 100,
          "refillRate": 10,
          "weight": 1.0,
          "priority": 1,
          "keyPattern": "user:{key}"
        },
        {
          "name": "tenant_limit",
          "algorithm": "SLIDING_WINDOW",
          "scope": "TENANT",
          "capacity": 5000,
          "refillRate": 500,
          "weight": 1.0,
          "priority": 2,
          "keyPattern": "tenant:acme_corp"
        }
      ],
      "combinationLogic": "HIERARCHICAL_AND"
    }
  }'
```

## Combination Logic Types

### ALL_MUST_PASS (AND Logic)
All rate limit components must allow the request.
```json
{
  "combinationLogic": "ALL_MUST_PASS"
}
```

### ANY_CAN_PASS (OR Logic)
At least one rate limit component must allow the request.
```json
{
  "combinationLogic": "ANY_CAN_PASS"
}
```

### WEIGHTED_AVERAGE
Uses component weights to calculate overall score (threshold: 50%).
```json
{
  "combinationLogic": "WEIGHTED_AVERAGE",
  "weights": {
    "api_calls": 3.0,
    "bandwidth": 1.0
  }
}
```

### HIERARCHICAL_AND
Checks components in hierarchical order: USER → TENANT → GLOBAL.
```json
{
  "combinationLogic": "HIERARCHICAL_AND",
  "hierarchical": true
}
```

### PRIORITY_BASED
Checks highest priority components first, fails fast on first denial.
```json
{
  "combinationLogic": "PRIORITY_BASED"
}
```

## Response Format

The composite rate limiter returns detailed information about each component:

```json
{
  "key": "enterprise:customer:123",
  "tokensRequested": 1,
  "allowed": true,
  "componentResults": {
    "api_calls": {
      "allowed": true,
      "currentTokens": 999,
      "capacity": 1000,
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

When rate limited:

```json
{
  "key": "enterprise:customer:123",
  "tokensRequested": 1,
  "allowed": false,
  "componentResults": {
    "api_calls": {
      "allowed": false,
      "currentTokens": 0,
      "capacity": 1000,
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

## Use Cases

### SaaS Platform Multi-Tier Limiting
- **API calls per minute**: Token bucket for burst handling
- **Bandwidth per second**: Leaky bucket for traffic shaping  
- **Compliance limits**: Fixed window for regulatory requirements

### Financial Systems
- **Transaction rate**: Sliding window for precise control
- **Transaction volume**: Token bucket with daily refill
- **Velocity checks**: Leaky bucket for suspicious activity detection

### Gaming Platforms
- **Actions per second**: Token bucket for gameplay
- **Chat messages**: Fixed window to prevent spam
- **Resource usage**: Composite limits for CPU/memory/network

### IoT Platforms
- **Device commands**: Token bucket per device
- **Data transfer**: Leaky bucket for bandwidth management
- **Connection limits**: Fixed window for concurrent connections