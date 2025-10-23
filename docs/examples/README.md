# API Usage Examples

This directory contains code examples demonstrating how to integrate with the Distributed Rate Limiter API in various programming languages.

## 🎨 Interactive Web Dashboard

**NEW!** Try our comprehensive web dashboard for real-time monitoring and management:

📁 **Location**: [`/examples/web-dashboard`](../../examples/web-dashboard/)

### Features
- 📊 **Real-time Monitoring** - Live metrics, charts, and activity feeds
- 🔧 **Algorithm Comparison** - Interactive testing of all 5 algorithms
- 📈 **Advanced Analytics** - Historical trends and performance insights
- 🔑 **API Key Management** - Complete lifecycle management with usage tracking
- ⚙️ **Configuration UI** - Visual configuration management
- 🧪 **Load Testing** - Integrated performance testing suite

### Quick Start
```bash
cd examples/web-dashboard
npm install
npm run dev
# Dashboard available at http://localhost:5173
```

See the [Dashboard README](../../examples/web-dashboard/README.md) for complete documentation.

---

## Quick Start

All examples demonstrate the basic rate limiting flow:

1. **Check Rate Limit**: Send a POST request to `/api/ratelimit/check`
2. **Handle Response**: Process the response to determine if the request should proceed
3. **Implement Backoff**: Apply appropriate delays when rate limited

## Available Examples

### 🎨 Interactive Dashboard
- [Web Dashboard](../../examples/web-dashboard/README.md) - **NEW!** Full-featured monitoring and management UI

### 💻 Client Libraries
- [Java/Spring Boot](./java-client.md) - Complete integration example
- [Python](./python-client.md) - Simple requests-based client
- [Node.js](./nodejs-client.md) - Express.js middleware example
- [Go](./go-client.md) - Native HTTP client implementation

### 📝 Testing & Examples
- [cURL](./curl-examples.md) - Command-line testing examples
- [Leaky Bucket](./leaky-bucket-examples.md) - Traffic shaping examples
- [Composite Rate Limiting](../../examples/composite-rate-limiting.md) - Multi-algorithm examples (**NEW**)

## Authentication

Most examples include optional API key authentication:

```bash
# Without API key (if not required)
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key":"user:123","tokens":1}'

# With API key
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key":"user:123","tokens":1,"apiKey":"your-api-key"}'
```

## Response Handling

All clients should handle these HTTP status codes:

- `200 OK` - Request allowed, proceed
- `429 Too Many Requests` - Rate limit exceeded, apply backoff
- `401 Unauthorized` - Invalid API key
- `403 Forbidden` - IP address blocked

Example response:
```json
{
  "key": "user:123",
  "tokensRequested": 1,
  "allowed": true
}
```

## Composite Rate Limiting (**NEW**)

For advanced scenarios requiring multiple algorithm combinations:

```bash
# Enterprise SaaS with API calls + bandwidth limits
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{
    "key": "enterprise:customer:123",
    "algorithm": "COMPOSITE",
    "tokens": 1,
    "compositeConfig": {
      "limits": [
        {"name": "api_calls", "algorithm": "TOKEN_BUCKET", "capacity": 1000, "refillRate": 100},
        {"name": "bandwidth", "algorithm": "LEAKY_BUCKET", "capacity": 50, "refillRate": 5}
      ],
      "combinationLogic": "ALL_MUST_PASS"
    }
  }'
```

Enhanced response with component details:
```json
{
  "allowed": true,
  "componentResults": {
    "api_calls": {"allowed": true, "currentTokens": 999, "capacity": 1000},
    "bandwidth": {"allowed": true, "currentTokens": 49, "capacity": 50}
  },
  "combinationResult": {
    "logic": "ALL_MUST_PASS",
    "overallScore": 1.0
  }
}
```

See [Composite Rate Limiting Examples](../../examples/composite-rate-limiting.md) for comprehensive usage scenarios.