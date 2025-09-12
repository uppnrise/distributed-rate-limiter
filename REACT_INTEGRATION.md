# React Testing App Integration Guide

This guide explains how to run and use the React testing application with the distributed rate limiter service.

## 🚀 Quick Start

### 1. Start the Rate Limiter Service

```bash
# Ensure Java 21 is installed and set as default
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Start the Spring Boot service
./mvnw spring-boot:run
```

The service will start on `http://localhost:8080`

### 2. Start the React Testing App

```bash
# Navigate to the React app directory
cd react-testing-app

# Install dependencies (first time only)
npm install

# Start the development server
npm run dev
```

The React app will be available at `http://localhost:5173`

## 🧪 Testing Workflow

### Step 1: Verify Connection
1. Open the React app in your browser
2. Check the connection status indicator in the top-right corner
3. It should show "UP" if the backend service is running

### Step 2: Dashboard Overview
1. View the Dashboard tab to see system metrics
2. Check total requests, success rate, and system health
3. Observe real-time updates as you perform tests

### Step 3: Single Request Testing
1. Go to the Testing tab
2. Use the "Single Request Test" section to send individual requests
3. Try different keys and token values
4. Observe the response status (allowed/denied)

### Step 4: Scenario Testing
1. Select a predefined test scenario (e.g., "Basic Rate Limiting")
2. Click "Run Scenario Test" to execute automated testing
3. Watch the real-time results in the test results section
4. Results will also appear in the Dashboard tab

### Step 5: Real-time Monitoring
1. Switch to the Monitoring tab
2. Click "Start Monitoring" to begin real-time data collection
3. Run tests while monitoring to see live charts
4. Observe success rate trends and request distribution

## 📊 Understanding the Results

### Dashboard Metrics
- **Total Requests**: Cumulative requests processed
- **Success Rate**: Percentage of requests allowed
- **Denied Requests**: Count of rate-limited requests
- **System Health**: Service and Redis connection status

### Test Results
- **✅ ALLOWED**: Request was within rate limits
- **❌ DENIED**: Request exceeded rate limits
- **Response Time**: Time taken to process request
- **Key Information**: Which rate limit key was tested

### Charts and Visualizations
- **Success Rate Over Time**: Trend line showing rate limit effectiveness
- **Request Distribution**: Pie chart of allowed vs denied requests
- **Error Rate Trend**: Line chart showing rate limit violations
- **Active Keys**: Table of currently monitored rate limit keys

## 🔧 Configuration Testing

### Default Rate Limits
The service starts with default rate limits. You can test these by:
1. Making rapid requests with the same key
2. Observing when rate limiting kicks in
3. Watching tokens refill over time

### Custom Rate Limits
You can configure custom rate limits via the backend API:
```bash
# Set custom limits for a specific key
curl -X POST http://localhost:8080/api/ratelimit/config/keys/test:user \
  -H "Content-Type: application/json" \
  -d '{"capacity": 5, "refillRate": 1}'
```

### Testing Different Scenarios
Each predefined scenario tests different aspects:
- **Basic Rate Limiting**: Normal user behavior
- **Burst Traffic**: High-frequency requests (e-commerce sale)
- **Multi-User Fair Usage**: Multiple concurrent users
- **API Abuse Detection**: Malicious rapid-fire requests
- **High Volume Testing**: Sustained load testing

## 🐛 Troubleshooting

### Common Issues

#### Backend Service Not Starting
- Verify Java 21 is installed: `java -version`
- Check port 8080 is available: `lsof -i :8080`
- Ensure Redis is running (if configured)

#### React App Connection Errors
- Verify backend is running on `http://localhost:8080`
- Check browser console for CORS errors
- Ensure `.env` file has correct `VITE_RATE_LIMITER_URL`

#### No Metrics Showing
- Click the refresh button in the React app header
- Check if backend `/metrics` endpoint is accessible
- Verify there's some test traffic to generate metrics

### Advanced Debugging

#### Backend Logs
Monitor the Spring Boot console for:
- Rate limit violations (WARN level)
- Request processing times
- Redis connection status

#### Browser Developer Tools
- **Network Tab**: Check API request/response details
- **Console Tab**: Look for JavaScript errors
- **Application Tab**: Verify environment variables

#### API Testing with curl
Test backend endpoints directly:
```bash
# Health check
curl http://localhost:8080/actuator/health

# Rate limit test
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{"key":"test:user","tokens":1}'

# Get metrics
curl http://localhost:8080/metrics
```

## 📈 Performance Monitoring

### Key Metrics to Watch
- **Response Times**: Should be low (< 50ms typically)
- **Success Rate**: Should remain high under normal load
- **Memory Usage**: Monitor backend JVM metrics
- **Redis Performance**: Check Redis connection and latency

### Load Testing
Use the React app's scenario testing to:
1. Run multiple test scenarios simultaneously
2. Monitor system behavior under load
3. Identify rate limiting effectiveness
4. Test system recovery after high load

### Production Considerations
- Set appropriate rate limits for your use case
- Monitor Redis memory usage and persistence
- Configure proper logging and alerting
- Consider scaling strategies for high traffic

## 🔗 API Reference

The React app integrates with these key endpoints:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/ratelimit/check` | POST | Check if request is rate limited |
| `/metrics` | GET | Get system metrics |
| `/actuator/health` | GET | Check service health |
| `/api/ratelimit/config/default` | POST | Update default rate limits |
| `/api/ratelimit/config/keys/{key}` | POST | Set key-specific limits |
| `/api/admin/buckets` | DELETE | Clear all rate limit buckets |

For complete API documentation, visit `/swagger-ui/index.html` when the backend service is running.