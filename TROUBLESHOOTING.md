# React Integration Troubleshooting Guide

This guide helps resolve common issues when integrating the React testing application with the distributed rate limiter service.

## Common Error: 403 Forbidden

### Symptoms
```
POST http://localhost:8080/api/ratelimit/check 403 (Forbidden)
```

### Root Causes & Solutions

#### 1. Missing or Invalid API Key

**Problem**: The rate limiter requires a valid API key for authentication.

**Solution**: Configure the React app with a valid API key:

```bash
# react-testing-app/.env
VITE_RATE_LIMITER_URL=http://127.0.0.1:8080
VITE_API_KEY=api-key-1
```

**Valid API Keys** (configured in `application.properties`):
- `api-key-1`
- `api-key-2` 
- `premium-key-123`

#### 2. IP Address Whitelist Issues

**Problem**: The application has IP whitelisting enabled and your IP isn't allowed.

**Current Whitelist** (in `application.properties`):
```properties
ratelimiter.security.ip.whitelist=127.0.0.1,::1
```

**Solutions**:
1. **Use IPv4 localhost**: Connect to `127.0.0.1:8080` instead of `localhost:8080`
2. **Add your IP**: Update the whitelist in `application.properties`
3. **Disable IP restrictions** (development only):
   ```properties
   ratelimiter.security.ip.whitelist=
   ```

#### 3. CORS (Cross-Origin Resource Sharing) Issues

**Problem**: Browser blocks requests from React dev server to API server.

**Symptoms**:
- CORS preflight (OPTIONS) requests fail
- Console shows CORS policy errors

**Solution**: The application includes CORS configuration supporting:
- `http://localhost:5173` (Vite default)
- `http://localhost:3000` (Create React App default)
- `http://127.0.0.1:5173` (IPv4)
- `http://127.0.0.1:3000` (IPv4)

## Quick Resolution Steps

1. **Check API Key Configuration**:
   ```bash
   # Verify .env file exists with API key
   cat react-testing-app/.env
   ```

2. **Use IPv4 Localhost**:
   ```bash
   # Update .env to use IPv4
   VITE_RATE_LIMITER_URL=http://127.0.0.1:8080
   ```

3. **Test API Endpoint Manually**:
   ```bash
   curl -X POST http://127.0.0.1:8080/api/ratelimit/check \
     -H "Content-Type: application/json" \
     -d '{"key": "test", "tokens": 1, "apiKey": "api-key-1"}'
   ```

4. **Verify CORS Preflight**:
   ```bash
   curl -X OPTIONS http://127.0.0.1:8080/api/ratelimit/check \
     -H "Origin: http://127.0.0.1:5173" \
     -H "Access-Control-Request-Method: POST"
   ```

## Development vs Production

### Development Configuration
- IP whitelist: `127.0.0.1,::1` (localhost only)
- API keys: Simple keys for testing
- CORS: Enabled for dev servers

### Production Configuration
- IP whitelist: Specific production IPs
- API keys: Secure, rotated keys
- CORS: Limited to production domains

## Security Configuration Reference

### API Key Settings
```properties
# Enable/disable API key validation
ratelimiter.security.api-keys.enabled=true

# Valid API keys (comma-separated)
ratelimiter.security.api-keys.valid-keys=api-key-1,api-key-2,premium-key-123
```

### IP Security Settings
```properties
# Allowed IPs (empty = allow all)
ratelimiter.security.ip.whitelist=127.0.0.1,::1

# Blocked IPs
ratelimiter.security.ip.blacklist=

# Request size limits
ratelimiter.security.max-request-size=1MB
```

### Security Headers
```properties
# Enable security headers
ratelimiter.security.headers.enabled=true
```

## Testing the Integration

1. **Start the Rate Limiter**:
   ```bash
   ./mvnw spring-boot:run
   ```

2. **Start the React App**:
   ```bash
   cd react-testing-app
   npm install
   npm run dev
   ```

3. **Access the Dashboard**:
   Open `http://127.0.0.1:5173` in your browser

4. **Test Rate Limiting**:
   - Use the "Single Request Test" with default values
   - Run scenario tests to verify functionality
   - Monitor real-time metrics

## Advanced Troubleshooting

### Enable Debug Logging
```properties
# Add to application.properties
logging.level.dev.bnacar.distributedratelimiter.security=DEBUG
logging.level.dev.bnacar.distributedratelimiter.controller=DEBUG
```

### Check Request Headers
Use browser dev tools or curl with `-v` flag to inspect:
- Request headers (especially Origin)
- Response headers (CORS headers)
- Request body (API key included)

### Verify IP Detection
The application logs the detected client IP. Check logs for:
```
INFO d.b.d.security.IpSecurityService - Client IP detected: 127.0.0.1
```

## Environment Variables Reference

### React App (.env)
```bash
# Required: Rate limiter service URL
VITE_RATE_LIMITER_URL=http://127.0.0.1:8080

# Required: Valid API key for authentication
VITE_API_KEY=api-key-1

# Optional: WebSocket URL for real-time features
# VITE_WS_URL=ws://127.0.0.1:8080/ws
```

### Spring Boot (application.properties)
See `application.properties` for complete configuration options.

## Support

If you encounter issues not covered by this guide:

1. Check application logs for detailed error messages
2. Verify network connectivity between React and Spring Boot
3. Test API endpoints directly with curl
4. Review security configuration settings
5. Check for conflicting security filters or middleware