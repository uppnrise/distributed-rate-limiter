# Rate Limiter Dashboard

A comprehensive, real-time dashboard for monitoring and managing distributed rate limiting systems. Built with modern web technologies to provide deep insights into rate limiting performance, algorithm comparison, and traffic analytics.

## 🚀 Features

### 📊 Real-time Monitoring Dashboard
- **Live Metrics**: Real-time tracking of active keys, requests per second, success rates
- **Interactive Charts**: Time-series visualization with 5-second updates
- **Algorithm Performance**: Compare Token Bucket, Sliding Window, Fixed Window, and Leaky Bucket algorithms
- **Activity Feed**: Live stream of rate limiting decisions and events
- **Health Monitoring**: Backend connectivity and Redis health status

### 🔧 Algorithm Simulation & Testing
- **Algorithm Comparison**: Side-by-side testing of different rate limiting algorithms
- **Traffic Pattern Simulation**: Generate steady, bursty, spike, or custom traffic patterns
- **Real-time Visualization**: Live charts showing token levels and request handling
- **Performance Analytics**: Detailed statistics on rejection rates, response times, and efficiency
- **Interactive Configuration**: Adjust capacity, refill rates, and time windows on the fly

### 📈 Advanced Analytics
- **Historical Data Analysis**: Performance trends over configurable time ranges (1h, 24h, 7d, 30d)
- **Top Keys Analysis**: Identify most active keys and their usage patterns
- **Algorithm Performance Metrics**: Memory usage, efficiency scores, and throughput comparison
- **Usage Trends**: Visualize traffic patterns and identify peak usage periods
- **Smart Alerts**: Automated notifications for spikes, degradation, and anomalies
- **Data Export**: CSV and JSON export functionality for external analysis

### 🔑 API Key Management
- **Comprehensive Key Management**: Create, view, edit, and delete API keys
- **Advanced Access Control**: IP whitelisting/blacklisting support
- **Usage Statistics**: Track total requests, success rates, and rate limiting events
- **Bulk Operations**: Mass activate, deactivate, delete, or export keys
- **Access Logs**: Detailed request history with IP addresses and response times
- **Key Regeneration**: Secure key rotation functionality

### ⚙️ Configuration Management
- **Global Configuration**: System-wide defaults for capacity, refill rates, and cleanup intervals
- **Per-Key Configuration**: Individual rate limiting rules for specific keys
- **Pattern-Based Rules**: Wildcard pattern matching for flexible configuration
- **Hierarchical Configuration**: Override global settings with specific rules
- **Configuration Visualization**: Interactive hierarchy display
- **Import/Export**: Backup and restore configurations via JSON/CSV

### 🧪 Load Testing Suite
- **Traffic Pattern Generation**: Constant, ramp-up, spike, and step-load patterns
- **Configurable Parameters**: Request rate, duration, concurrency, and timeout settings
- **Real-time Monitoring**: Live metrics during test execution
- **Performance Analysis**: P50, P95, P99 response times and success rates
- **Historical Test Results**: Compare performance across different test runs
- **Test Configuration Management**: Save and reuse test configurations

### 🎨 User Experience
- **Modern UI**: Clean, responsive design with dark/light theme support
- **Keyboard Shortcuts**: Efficient navigation with hotkeys (Alt+D for Dashboard, Alt+A for Algorithms, etc.)
- **Mobile Responsive**: Optimized for desktop, tablet, and mobile devices
- **Accessibility**: WCAG compliant with proper ARIA labels and keyboard navigation
- **Real-time Updates**: Live data streaming with WebSocket-like polling
- **Error Handling**: Graceful error states with retry mechanisms

## 🛠 Technology Stack

### Frontend
- **React 18** - Modern React with hooks and concurrent features
- **TypeScript** - Type-safe development with excellent IDE support
- **Vite** - Fast build tool with hot module replacement
- **React Router** - Client-side routing with nested routes
- **Tailwind CSS** - Utility-first CSS framework with custom design system
- **shadcn/ui** - High-quality, accessible UI component library
- **Recharts** - Composable charting library for React
- **React Query** - Server state management with caching
- **Sonner** - Beautiful toast notifications
- **Lucide React** - Consistent icon library

### Development Tools
- **ESLint** - Code linting with modern rules
- **TypeScript** - Static type checking
- **PostCSS** - CSS processing with autoprefixer
- **Lovable** - AI-powered development platform integration

### Backend Integration
- **REST API** - HTTP-based API communication
- **Real-time Polling** - Live data updates every 5 seconds
- **Error Handling** - Robust error handling with user feedback
- **Authentication** - Basic auth for admin endpoints
- **CORS Support** - Cross-origin resource sharing configuration

## 🚀 Quick Start

### Prerequisites
- Node.js 18+ and npm
- A distributed rate limiter backend running on `localhost:8080`

### Installation

```bash
# Clone the repository
git clone <repository-url>
cd rate-limiter-dashboard

# Install dependencies
npm install

# Start development server
npm run dev
```

The application will be available at `http://localhost:8081`

### Backend Requirements

The dashboard expects a rate limiter backend with the following endpoints:

#### Rate Limiting API
- `POST /api/ratelimit/check` - Check rate limit for a key
- `GET /api/ratelimit/config` - Get current configuration
- `POST /api/ratelimit/config/keys/{key}` - Update key-specific config
- `POST /api/ratelimit/config/patterns/{pattern}` - Update pattern config

#### Admin API (Basic Auth: admin/changeme)
- `GET /admin/keys` - Get active keys and statistics

#### Monitoring API
- `GET /metrics` - System metrics and performance data
- `GET /actuator/health` - Health check endpoint

See [API_INTEGRATION.md](./API_INTEGRATION.md) for complete API documentation.

## 📁 Project Structure

```
src/
├── components/           # Reusable React components
│   ├── ui/              # shadcn/ui base components
│   ├── layout/          # Layout components (Sidebar, TopNav)
│   ├── dashboard/       # Dashboard-specific components
│   ├── algorithms/      # Algorithm simulation components
│   ├── analytics/       # Analytics and reporting components
│   ├── apikeys/         # API key management components
│   ├── configuration/   # Configuration management components
│   └── loadtest/        # Load testing components
├── contexts/            # React context providers
├── hooks/               # Custom React hooks
├── lib/                 # Utility functions and configurations
├── pages/               # Page-level components
├── services/            # API service layers
├── types/               # TypeScript type definitions
├── utils/               # Helper functions and simulators
└── App.tsx             # Main application component
```

## 🎯 Key Components

### Dashboard (`/`)
Real-time monitoring with live metrics, charts, and activity feeds.

### Algorithms (`/algorithms`)
Interactive algorithm comparison with traffic simulation and performance analysis.

### Analytics (`/analytics`)
Historical performance analysis with trends, top keys, and alerts.

### Configuration (`/configuration`)
Comprehensive configuration management with global, key-specific, and pattern-based rules.

### API Keys (`/api-keys`)
Complete API key lifecycle management with usage tracking and access control.

### Load Testing (`/load-testing`)
Performance testing suite with configurable traffic patterns and detailed metrics.

## 🔧 Configuration

### Environment Variables
```bash
# Backend API base URL (default: http://localhost:8080)
VITE_API_BASE_URL=http://localhost:8080

# Enable development features
VITE_DEV_MODE=true
```

### Vite Configuration
The project uses Vite with:
- React SWC plugin for fast compilation
- Path aliases (@/* for src/*)
- Development server on port 8081
- Component tagging for development

### Theme Customization
The design system is built on Tailwind CSS with custom tokens:
- CSS custom properties for colors and spacing
- Light/dark theme support
- Responsive breakpoints
- Custom animations and transitions

## 📊 Performance Features

### Real-time Updates
- 5-second polling interval for live metrics
- Efficient data caching with React Query
- Optimized re-renders with React.memo and useMemo
- Background updates without UI blocking

### Algorithm Simulation
- Accurate implementations of all major rate limiting algorithms
- Configurable traffic patterns (steady, bursty, spike, custom)
- Real-time visualization with 60-point sliding window
- Performance metrics calculation (rejection rate, response time, efficiency)

### Load Testing
- Client-side traffic simulation
- Configurable request patterns and concurrency
- Real-time metrics collection and visualization
- Historical test result comparison

## 🛡 Security Features

- **Input Validation**: Client-side validation with server-side verification
- **XSS Protection**: Sanitized user inputs and safe HTML rendering
- **CORS Handling**: Proper cross-origin request configuration
- **Authentication**: Basic auth for admin endpoints
- **Rate Limiting**: Built-in protection against API abuse

## 🚀 Deployment

### Development
```bash
npm run dev
```

### Production Build
```bash
npm run build
npm run preview
```

### Docker (Optional)
```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build
EXPOSE 8080
CMD ["npm", "run", "preview"]
```

## 📖 API Integration

The dashboard integrates with a distributed rate limiter backend. See [API_INTEGRATION.md](./API_INTEGRATION.md) for:
- Complete API endpoint documentation
- Request/response schemas
- Authentication requirements
- Error handling guidelines
- Backend setup instructions

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes with conventional commits (`git commit -m 'feat: add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Commit Convention
- `feat:` - New features
- `fix:` - Bug fixes
- `docs:` - Documentation updates
- `style:` - Code formatting
- `refactor:` - Code restructuring
- `test:` - Test additions/updates
- `chore:` - Maintenance tasks

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [shadcn/ui](https://ui.shadcn.com/) for the excellent component library
- [Recharts](https://recharts.org/) for beautiful, composable charts
- [Tailwind CSS](https://tailwindcss.com/) for the utility-first CSS framework
- [Lucide](https://lucide.dev/) for the comprehensive icon library
