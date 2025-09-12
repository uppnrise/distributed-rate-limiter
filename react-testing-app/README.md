# Rate Limiter Testing Dashboard

A modern React TypeScript application for testing and monitoring the distributed rate limiter service in real-time.

## 🚀 Features

### Dashboard Overview
- **Real-time Metrics**: System health, total requests, success rates
- **Visual Status Indicators**: Connection status and Redis connectivity
- **Recent Test Results**: Summary of completed test scenarios

### Interactive Testing Panel
- **Single Request Testing**: Send individual requests with custom parameters
- **Scenario-Based Testing**: Pre-configured test scenarios including:
  - Basic Rate Limiting
  - Burst Traffic Simulation  
  - Multi-User Fair Usage
  - API Abuse Detection
  - High Volume Testing
- **Real-time Results**: Live feedback on request success/failure

### Real-time Monitoring
- **Live Charts**: Success rate and error rate trends over time
- **Request Distribution**: Visual breakdown of allowed vs denied requests
- **Active Key Tracking**: Monitor individual key usage and limits
- **Polling Controls**: Start/stop real-time data updates

## 🛠️ Technology Stack

- **React 18** with TypeScript
- **Vite** for fast development and building
- **Tailwind CSS** for styling
- **Recharts** for data visualization
- **Axios** for HTTP requests

## 📋 Prerequisites

- Node.js 18+ and npm
- Distributed Rate Limiter service running on `http://localhost:8080`

## 🚀 Getting Started

### 1. Environment Setup

Copy the environment configuration:
```bash
cp .env.example .env
```

Edit `.env` to match your rate limiter service URL:
```env
VITE_RATE_LIMITER_URL=http://localhost:8080
# VITE_API_KEY=your-api-key-here  # Optional
```

### 2. Install Dependencies

```bash
npm install
```

### 3. Start Development Server

```bash
npm run dev
```

The application will be available at `http://localhost:5173`

### 4. Start Rate Limiter Service

Ensure the backend service is running:
```bash
# In the parent directory
./mvnw spring-boot:run
```

## 🧪 Usage Guide

### Dashboard Tab
- View system overview metrics
- Check connection status
- Review recent test results

### Testing Tab
- **Single Request**: Test individual requests with custom key and token values
- **Scenario Testing**: Select predefined scenarios and run automated tests
- **Results Display**: View real-time test results and response details

### Monitoring Tab
- **Start/Stop Monitoring**: Toggle real-time data polling
- **Charts**: View success rate trends and request distribution
- **Active Keys**: Monitor currently rate-limited keys

## 📊 Test Scenarios

### 1. Basic Rate Limiting
- Tests simple user rate limiting behavior
- Single user, sequential requests
- Demonstrates token bucket refill

### 2. Burst Traffic Simulation
- Simulates e-commerce flash sale traffic
- High-frequency concurrent requests
- Tests burst handling capabilities

### 3. Multi-User Fair Usage
- Multiple user keys with individual limits
- Tests fair distribution of rate limits
- Concurrent user simulation

### 4. API Abuse Detection
- Rapid-fire requests exceeding normal usage
- Tests rate limiter's abuse protection
- High-frequency violation scenarios

### 5. High Volume Testing
- Sustained high load testing
- Tests system stability under load
- Performance and reliability validation

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_RATE_LIMITER_URL` | Rate limiter service URL | `http://localhost:8080` |
| `VITE_API_KEY` | Optional API key for authentication | - |

### Customizing Test Scenarios

Edit `src/utils/testScenarios.ts` to add custom test scenarios:

```typescript
{
  id: 'custom-test',
  name: 'Custom Test',
  description: 'Your custom test description',
  config: {
    key: 'custom:key',
    tokens: 1,
    requestCount: 20,
    intervalMs: 500,
    concurrent: false,
  },
}
```

## 🏗️ Building for Production

```bash
npm run build
```

Built files will be in the `dist/` directory. Serve with any static file server:

```bash
# Example with Python
python -m http.server 8000 -d dist

# Example with Node.js serve
npx serve dist
```

## 🔍 API Integration

The application integrates with these rate limiter endpoints:

- `POST /api/ratelimit/check` - Check rate limits
- `GET /metrics` - System metrics
- `GET /actuator/health` - Health status
- `POST /api/ratelimit/config/*` - Configuration management
- `DELETE /api/admin/*` - Administrative operations

## 🐛 Troubleshooting

### Connection Issues
- Verify rate limiter service is running on the configured URL
- Check for CORS configuration in the backend service
- Ensure Redis is running and accessible to the backend

### Build Issues
- Run `npm install` to ensure all dependencies are installed
- Check Node.js version (requires 18+)
- Clear node_modules and reinstall if needed

### Runtime Issues
- Check browser console for error messages
- Verify API responses in Network tab
- Ensure environment variables are set correctly

## 🤝 Contributing

1. Add new components in `src/components/`
2. Create hooks for API interactions in `src/hooks/`
3. Add type definitions in `src/types/`
4. Update test scenarios in `src/utils/testScenarios.ts`

## 📝 License

This project follows the same license as the parent distributed-rate-limiter project.
