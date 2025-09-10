# Load Testing & Performance Baseline Documentation

This document describes the load testing infrastructure and performance baseline management for the Distributed Rate Limiter service.

## Overview

The load testing suite provides comprehensive performance testing capabilities including:
- Automated load tests using Gatling
- Performance regression detection
- CI/CD integration with GitHub Actions
- Performance baseline tracking
- Stress testing under extreme conditions

## Load Testing Infrastructure

### Gatling Load Tests

The project uses [Gatling](https://gatling.io/) for professional-grade load testing with the following test scenarios:

#### BasicLoadTest
- **Purpose**: Standard load testing for everyday performance validation
- **Configuration**:
  - Duration: 30 seconds (configurable via `load.test.duration`)
  - Max Users: 50 (configurable via `load.test.maxUsers`)
  - Ramp-up: 10 seconds (configurable via `load.test.rampUp`)
- **Scenarios**:
  - Health check validation
  - Rate limiting functionality
  - Benchmark endpoint testing
  - Metrics collection

#### StressTest
- **Purpose**: Extreme load testing to identify system limits
- **Configuration**:
  - Duration: 60 seconds (configurable via `stress.test.duration`)
  - Max Users: 200 (configurable via `stress.test.maxUsers`)
  - Ramp-up: 20 seconds (configurable via `stress.test.rampUp`)
- **Scenarios**:
  - High-frequency rate limiting
  - Burst traffic patterns
  - Concurrent benchmark execution
  - Mixed workload simulation
  - Resource exhaustion testing

### Running Load Tests

#### Local Execution

```bash
# Start the application
./mvnw spring-boot:run

# Run basic load test
./mvnw gatling:test -Dgatling.simulationClass=dev.bnacar.distributedratelimiter.loadtest.BasicLoadTest

# Run stress test
./mvnw gatling:test -Dgatling.simulationClass=dev.bnacar.distributedratelimiter.loadtest.StressTest

# Custom configuration
./mvnw gatling:test \
  -Dgatling.simulationClass=dev.bnacar.distributedratelimiter.loadtest.BasicLoadTest \
  -Dload.test.baseUrl=http://localhost:8080 \
  -Dload.test.duration=60 \
  -Dload.test.maxUsers=100
```

#### CI/CD Execution

Load tests are automatically executed in GitHub Actions for:
- Push events to `main` or `develop` branches
- Pull requests targeting these branches

The CI/CD pipeline:
1. Starts the application with Redis service
2. Waits for application readiness
3. Runs basic load tests on all builds
4. Runs stress tests on main branch only
5. Stores performance baselines
6. Uploads test results as artifacts

## Performance Baselines

### Current Performance Targets

Based on the existing benchmark infrastructure and testing, the following performance targets have been established:

| Metric | Target | Threshold |
|--------|--------|-----------|
| **Response Time (Average)** | < 250ms | < 500ms |
| **Response Time (Max)** | < 500ms | < 1000ms |
| **Throughput** | > 500 req/sec | > 250 req/sec |
| **Success Rate** | > 95% | > 90% |

### Baseline Storage

Performance baselines are stored in JSON format and include:
- Test name and timestamp
- Response time metrics (average, max)
- Throughput measurements
- Success rate percentages
- Build metadata (commit hash, build number)
- Environment information

### Performance Regression Detection

The system automatically detects performance regressions using configurable thresholds:

| Regression Type | Default Threshold | Severity Levels |
|-----------------|-------------------|-----------------|
| **Response Time Increase** | 20% | Minor (20-30%), Moderate (30-40%), Major (40-60%), Critical (>60%) |
| **Throughput Decrease** | 15% | Minor (15-22%), Moderate (22-30%), Major (30-45%), Critical (>45%) |
| **Success Rate Decrease** | 5% | Minor (5-7%), Moderate (7-10%), Major (10-15%), Critical (>15%) |

## API Endpoints

### Performance Monitoring

#### Store Performance Baseline
```http
POST /api/performance/baseline
Content-Type: application/json

{
  "testName": "BasicLoadTest",
  "averageResponseTime": 150.0,
  "maxResponseTime": 300.0,
  "throughputPerSecond": 800.0,
  "successRate": 96.5,
  "commitHash": "abc123",
  "buildNumber": "456",
  "environment": "CI"
}
```

#### Analyze Performance Regression
```http
POST /api/performance/regression/analyze
Content-Type: application/json

{
  "testName": "BasicLoadTest",
  "averageResponseTime": 180.0,
  "maxResponseTime": 350.0,
  "throughputPerSecond": 750.0,
  "successRate": 94.0
}
```

#### Get Performance Trends
```http
GET /api/performance/trend/BasicLoadTest?limit=20
```

### Existing Benchmark Endpoints

#### Run Performance Benchmark
```http
POST /api/benchmark/run
Content-Type: application/json

{
  "concurrentThreads": 10,
  "requestsPerThread": 1000,
  "durationSeconds": 30,
  "tokensPerRequest": 1,
  "keyPrefix": "load_test"
}
```

## Configuration

### System Properties

#### Load Test Configuration
- `load.test.baseUrl`: Target application URL (default: http://localhost:8080)
- `load.test.duration`: Test duration in seconds (default: 30)
- `load.test.maxUsers`: Maximum concurrent users (default: 50)
- `load.test.rampUp`: Ramp-up duration in seconds (default: 10)
- `load.test.responseTime.threshold`: Response time threshold in ms (default: 500)
- `load.test.successRate.threshold`: Success rate threshold percentage (default: 95.0)

#### Stress Test Configuration
- `stress.test.baseUrl`: Target application URL
- `stress.test.duration`: Test duration in seconds (default: 60)
- `stress.test.maxUsers`: Maximum concurrent users (default: 200)
- `stress.test.rampUp`: Ramp-up duration in seconds (default: 20)
- `stress.test.plateau`: Plateau duration in seconds (default: 30)

#### Regression Detection
- `performance.baseline.storage.path`: Baseline storage file path (default: ./target/performance-baselines.json)

### Environment Variables for CI/CD

- `LOAD_TEST_BASE_URL`: Application URL for load testing
- `LOAD_TEST_DURATION`: Test duration
- `LOAD_TEST_MAX_USERS`: Maximum users
- `LOAD_TEST_RESPONSE_TIME_THRESHOLD`: Response time threshold
- `LOAD_TEST_SUCCESS_RATE_THRESHOLD`: Success rate threshold

## Test Results and Reporting

### Gatling Reports

Gatling generates comprehensive HTML reports including:
- Request response time distribution
- Response time percentiles over time
- Active users over time
- Requests per second
- Error rate analysis

Reports are available in: `target/gatling/<timestamp>/index.html`

### Performance Baseline Reports

Performance baselines are stored in JSON format and can be analyzed using:
- Trend analysis endpoints
- Regression detection API
- Custom reporting tools

### CI/CD Artifacts

The following artifacts are uploaded in CI/CD:
- `load-test-results`: Complete load test execution results
- `gatling-reports`: Gatling HTML reports
- `performance-baselines.json`: Historical performance data

## Best Practices

### Load Test Design
1. **Realistic Scenarios**: Use diverse keys and request patterns that match production usage
2. **Gradual Ramp-up**: Avoid sudden load spikes that don't reflect real traffic
3. **Appropriate Duration**: Run tests long enough to reach steady state
4. **Resource Monitoring**: Monitor CPU, memory, and Redis during tests

### Performance Baseline Management
1. **Regular Updates**: Update baselines after significant changes
2. **Environment Consistency**: Ensure test environments are consistent
3. **Trend Analysis**: Monitor long-term performance trends
4. **Alert Thresholds**: Set appropriate thresholds for regression detection

### CI/CD Integration
1. **Fail Fast**: Configure appropriate failure thresholds
2. **Artifact Storage**: Preserve load test results for analysis
3. **Performance Gates**: Include performance criteria in quality gates
4. **Notification**: Set up alerts for performance regressions

## Troubleshooting

### Common Issues

#### Load Test Failures
- **Connection Refused**: Ensure application is running and accessible
- **Timeout Errors**: Increase test timeouts or reduce load
- **High Error Rates**: Check application logs and Redis connectivity

#### Performance Regressions
- **False Positives**: Adjust regression thresholds
- **Environment Differences**: Ensure consistent test environments
- **Resource Constraints**: Monitor system resources during tests

#### CI/CD Issues
- **Test Timeouts**: Increase GitHub Actions timeout settings
- **Resource Limits**: Monitor runner resource usage
- **Artifact Upload Failures**: Check artifact size and GitHub limits

### Monitoring and Debugging

#### Application Health
```bash
# Check application status
curl http://localhost:8080/actuator/health

# Monitor metrics
curl http://localhost:8080/actuator/metrics

# Performance monitoring
curl http://localhost:8080/api/performance/health
```

#### Load Test Debugging
- Review Gatling simulation logs
- Monitor application logs during tests
- Check Redis connectivity and performance
- Analyze system resource usage

## Performance History

### Baseline Establishment (Initial)
- **Date**: 2024-12-XX
- **Commit**: Initial load testing implementation
- **Results**:
  - Average Response Time: ~150ms
  - Throughput: ~800 req/sec
  - Success Rate: ~95%
- **Environment**: CI/GitHub Actions

### Future Baselines
Future performance measurements will be tracked and compared against these initial baselines to detect regressions and improvements over time.

## Related Documentation

- [PERFORMANCE.md](./PERFORMANCE.md) - Detailed performance tuning guide
- [CI-CD.md](./CI-CD.md) - CI/CD pipeline documentation
- [README.md](./README.md) - General project information

## Support

For questions about load testing or performance issues:
1. Check existing GitHub issues
2. Review Gatling documentation
3. Analyze performance baseline trends
4. Create new issues with detailed load test results