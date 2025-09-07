# Distributed Rate Limiter

A Java Spring Boot distributed token bucket rate limiter implementation with Redis support and comprehensive testing via Testcontainers.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively

### Bootstrap the development environment:
1. **Install Java 21** (CRITICAL - project requires Java 21, not Java 17):
   ```bash
   sudo apt update && sudo apt install -y openjdk-21-jdk
   sudo update-alternatives --config java  # Select option 0 for Java 21
   sudo update-alternatives --config javac # Select option 0 for Java 21
   export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
   ```

2. **Verify Java version**:
   ```bash
   java -version  # Should show OpenJDK 21.x.x
   javac -version # Should show javac 21.x.x
   ```

### Build and test the repository:
- **Full build with tests**: `./mvnw clean install` -- takes 36 seconds with dependency downloads. NEVER CANCEL. Set timeout to 120+ seconds.
- **Compile only**: `./mvnw clean compile` -- takes 4 seconds after dependencies downloaded. NEVER CANCEL. Set timeout to 60+ seconds.
- **Run tests**: `./mvnw test` -- takes 11 seconds including Testcontainers Redis startup. NEVER CANCEL. Set timeout to 60+ seconds.
- **Check code style**: `./mvnw checkstyle:check` -- takes 15 seconds. Expect 26+ style violations in current codebase.

### Run the application:
- **Start application**: `./mvnw spring-boot:run` -- starts in 2.2 seconds on port 8080
- **Start on different port**: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`
- **Health check**: `curl http://localhost:8080/actuator/health` (may show DOWN if Redis not running)
- **Actuator endpoints**: `curl http://localhost:8080/actuator`

## Validation

### Manual testing scenarios:
1. **Build validation**: Always run `./mvnw clean install` after making changes to ensure compilation and tests pass
2. **Application startup**: Start the application with `./mvnw spring-boot:run` and verify startup logs show "Started DistributedRateLimiterApplication in X.X seconds"
3. **Health endpoint**: Test `curl http://localhost:8080/actuator/health` responds with JSON status
4. **Code quality**: Run `./mvnw checkstyle:check` to validate code style (expect failures in current codebase)

### Dependencies and requirements:
- **Java 21**: REQUIRED - project will not compile with Java 17 or earlier
- **Docker**: Required for Testcontainers integration in tests
- **Maven wrapper**: Use `./mvnw` commands, not global `mvn`
- **Internet access**: Required for Maven dependency downloads

### Pre-commit validation:
- Always run `./mvnw clean install` before committing changes
- Run `./mvnw checkstyle:check` to identify style issues (but don't block on existing violations)
- For critical changes to TokenBucket class, run specific tests: `./mvnw test -Dtest=TokenBucketTest`

## Common tasks

### Project structure:
```
.
├── README.md                    # Basic project documentation
├── LICENSE.md                   # MIT license
├── pom.xml                     # Maven configuration (Spring Boot 3.5.4, Java 21)
├── mvnw / mvnw.cmd            # Maven wrapper scripts
├── renovate.json              # Dependency update configuration
└── src/
    ├── main/java/dev/bnacar/distributedratelimiter/
    │   ├── DistributedRateLimiterApplication.java  # Main Spring Boot application
    │   └── ratelimit/
    │       └── TokenBucket.java                    # Core rate limiting implementation
    ├── main/resources/
    │   └── application.properties                  # Spring configuration
    └── test/java/dev/bnacar/distributedratelimiter/
        ├── DistributedRateLimiterApplicationTests.java  # Integration tests
        ├── TestcontainersConfiguration.java             # Redis test configuration
        ├── TestDistributedRateLimiterApplication.java   # Test runner
        └── ratelimit/
            └── TokenBucketTest.java                      # Unit tests for TokenBucket
```

### Key components:
- **TokenBucket.java**: Core rate limiting algorithm with thread-safe token consumption
- **DistributedRateLimiterApplication.java**: Spring Boot main class with web server and Redis integration
- **TestcontainersConfiguration.java**: Redis container setup for integration testing
- **TokenBucketTest.java**: Comprehensive unit tests including concurrency and timing tests

### Maven dependencies:
- Spring Boot 3.5.4 (web, actuator, data-redis)
- Redis for distributed state
- Testcontainers for integration testing
- Awaitility for time-based testing
- JUnit 5 for testing

### Build timing expectations:
- **NEVER CANCEL**: All builds and tests may take longer than default timeouts
- First build: 36+ seconds (dependency downloads)
- Subsequent builds: 4-8 seconds
- Test execution: 11+ seconds (includes Docker container startup)
- Checkstyle: 15 seconds

### Known issues:
- Default Java 17 installation will cause build failure - must upgrade to Java 21
- Checkstyle reports 26+ violations in current codebase - this is expected
- Application health endpoint shows DOWN status when Redis is not available
- Tests require Docker for Testcontainers Redis integration

### Redis configuration:
- Tests automatically start Redis container via Testcontainers
- Production application expects Redis connection (Spring Data Redis)
- No explicit Redis configuration in application.properties (uses defaults)

### Troubleshooting:
- **"release version 21 not supported"**: Java version issue, install and configure Java 21
- **Port 8080 already in use**: Kill existing process or use different port
- **Testcontainers failures**: Ensure Docker is running and accessible
- **Long build times**: Normal for first run with dependency downloads, use appropriate timeouts