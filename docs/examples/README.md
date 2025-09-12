# API Usage Examples

This directory contains code examples demonstrating how to integrate with the Distributed Rate Limiter API in various programming languages.

## Quick Start

All examples demonstrate the basic rate limiting flow:

1. **Check Rate Limit**: Send a POST request to `/api/ratelimit/check`
2. **Handle Response**: Process the response to determine if the request should proceed
3. **Implement Backoff**: Apply appropriate delays when rate limited

## Available Examples

- [Java/Spring Boot](./java-client.md) - Complete integration example
- [Python](./python-client.md) - Simple requests-based client
- [Node.js](./nodejs-client.md) - Express.js middleware example
- [cURL](./curl-examples.md) - Command-line testing examples
- [Go](./go-client.md) - Native HTTP client implementation

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