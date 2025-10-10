# Contributing to Distributed Rate Limiter

Thank you for your interest in contributing to the Distributed Rate Limiter! We welcome contributions from developers of all experience levels.

## 🚀 Quick Start

1. **Fork** the repository
2. **Clone** your fork: `git clone https://github.com/your-username/distributed-rate-limiter.git`
3. **Install** Java 21 and Docker
4. **Run tests**: `./mvnw test`
5. **Start contributing**!

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Environment](#development-environment)
- [Making Changes](#making-changes)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)
- [Submitting Changes](#submitting-changes)
- [Release Process](#release-process)

## 📖 Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](https://www.contributor-covenant.org/version/2/1/code_of_conduct/). By participating, you are expected to uphold this code.

## 🏁 Getting Started

### Prerequisites

- **Java 21+** (OpenJDK recommended)
- **Docker** (for integration tests and Redis)
- **Git** for version control
- **IDE** with Java support (IntelliJ IDEA, VS Code, Eclipse)

### Development Environment Setup

```bash
# Clone your fork
git clone https://github.com/your-username/distributed-rate-limiter.git
cd distributed-rate-limiter

# Verify Java 21
java -version

# Run initial build
./mvnw clean install

# Start Redis for local testing
docker run -d --name redis -p 6379:6379 redis:7-alpine

# Run the application
./mvnw spring-boot:run
```

### Project Structure

```
distributed-rate-limiter/
├── src/main/java/                 # Application source code
│   ├── controller/                # REST API controllers
│   ├── ratelimit/                # Core rate limiting logic
│   ├── monitoring/               # Metrics and monitoring
│   └── security/                 # Security components
├── src/test/java/                # Test source code
│   ├── ratelimit/                # Unit tests
│   ├── controller/               # Integration tests
│   └── loadtest/                 # Performance tests
├── docs/                         # Documentation
│   ├── examples/                 # Client integration examples
│   ├── adr/                      # Architecture Decision Records
│   └── deployment/               # Deployment guides
└── k8s/                          # Kubernetes manifests
```

## 🔧 Making Changes

### Branch Naming

- **Feature**: `feature/description-of-feature`
- **Bug Fix**: `fix/description-of-fix`
- **Documentation**: `docs/description-of-change`
- **Performance**: `perf/description-of-improvement`

### Coding Standards

#### Java Code Style
- Follow **Google Java Style Guide**
- Use **4 spaces** for indentation
- **Line length**: 120 characters maximum
- **Imports**: Use wildcard imports for 5+ classes from same package

#### Code Quality Tools
```bash
# Run checkstyle
./mvnw checkstyle:check

# Run PMD static analysis
./mvnw pmd:check

# Run SpotBugs
./mvnw spotbugs:check

# Run all quality checks
./mvnw verify
```

#### Best Practices
- **Single Responsibility**: Each class should have one reason to change
- **Dependency Injection**: Use Spring's `@Autowired` constructors
- **Immutability**: Prefer immutable objects where possible
- **Null Safety**: Use `Optional<T>` for nullable return values
- **Logging**: Use SLF4J with structured logging patterns

### Architecture Guidelines

#### Rate Limiting Components
- **Algorithm Implementation**: Extend `RateLimitAlgorithm` interface
- **Backend Integration**: Implement `RateLimiterBackend` for new storage
- **Configuration**: Add properties to `RateLimiterConfiguration`
- **Metrics**: Use `MetricsService` for custom measurements

#### Redis Integration
- Use **Lua scripts** for atomic operations
- Implement **TTL** for automatic cleanup
- Handle **connection failures** gracefully
- Test with **Testcontainers** Redis

## 🧪 Testing Guidelines

### Test Categories

#### Unit Tests
- **File Pattern**: `*Test.java`
- **Location**: `src/test/java/`
- **Coverage**: Minimum 80% line coverage
- **Frameworks**: JUnit 5, Mockito, AssertJ

```java
@ExtendWith(MockitoExtension.class)
class TokenBucketTest {
    
    @Test
    void shouldConsumeTokensWhenAvailable() {
        // Given
        TokenBucket bucket = new TokenBucket(10, 2);
        
        // When
        boolean result = bucket.tryConsume(5);
        
        // Then
        assertThat(result).isTrue();
    }
}
```

#### Integration Tests
- **File Pattern**: `*IntegrationTest.java`
- **Redis**: Use `@Testcontainers` with Redis container
- **Spring Context**: Use `@SpringBootTest` for full application context
- **Test Slices**: Use `@WebMvcTest` for controller-only tests

```java
@SpringBootTest
@Testcontainers
class RateLimitControllerIntegrationTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    
    @Test
    void shouldRateLimitRequests() {
        // Integration test implementation
    }
}
```

#### Performance Tests
- **File Pattern**: `*PerformanceTest.java`
- **Load Testing**: Use `CompletableFuture` for concurrent requests
- **Metrics**: Validate latency and throughput requirements
- **Memory**: Monitor memory usage during high load

### Running Tests

```bash
# All tests
./mvnw test

# Specific test class
./mvnw test -Dtest=TokenBucketTest

# Integration tests only
./mvnw test -Dtest=*IntegrationTest

# Performance tests (requires more resources)
./mvnw test -Dtest=*PerformanceTest -Dmaven.test.redirectTestOutputToFile=false

# With code coverage
./mvnw test jacoco:report
# View coverage: target/site/jacoco/index.html
```

### Test Data and Mocking

- **Use builders** for complex test objects
- **Mock external dependencies** (Redis calls in unit tests)
- **Use test profiles** for different configurations
- **Cleanup resources** in `@AfterEach` methods

## 📚 Documentation

### Code Documentation
- **JavaDoc**: Document public APIs and complex methods
- **Comments**: Explain **why**, not **what**
- **ADRs**: Document significant architectural decisions in `docs/adr/`

### User Documentation
- **API Changes**: Update `docs/API.md`
- **Configuration**: Update `CONFIGURATION.md` for new properties
- **Examples**: Add client examples for new features
- **README**: Update feature list and performance metrics

### Documentation Standards
```java
/**
 * Attempts to consume the specified number of tokens from the bucket.
 *
 * @param tokensToConsume number of tokens to consume (must be positive)
 * @return true if tokens were available and consumed, false otherwise
 * @throws IllegalArgumentException if tokensToConsume is negative or zero
 */
public boolean tryConsume(long tokensToConsume) {
    // Implementation
}
```

## 📤 Submitting Changes

### Pull Request Process

1. **Create feature branch** from `develop`
2. **Make changes** following coding standards
3. **Add tests** for new functionality
4. **Update documentation** as needed
5. **Run full test suite**: `./mvnw clean verify`
6. **Submit pull request** to `develop` branch

### Pull Request Template

```markdown
## Description
Brief description of changes and motivation.

## Type of Change
- [ ] Bug fix (non-breaking change)
- [ ] New feature (non-breaking change)
- [ ] Breaking change (fix or feature causing existing functionality to change)
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Performance tests pass (if applicable)
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] Tests added for new functionality
```

### Review Process

1. **Automated Checks**: CI/CD pipeline must pass
2. **Code Review**: At least one maintainer approval required
3. **Testing**: All tests must pass including new ones
4. **Documentation**: Must be updated for user-facing changes

## 🚀 Release Process

### Semantic Versioning

We follow [Semantic Versioning](https://semver.org/):
- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

### Release Checklist

- [ ] All tests passing
- [ ] Documentation updated
- [ ] Performance benchmarks validated
- [ ] Security scan completed
- [ ] Docker image builds successfully
- [ ] Release notes prepared

## 🏷️ Issue Labels

- `bug` - Something isn't working
- `enhancement` - New feature or request
- `documentation` - Documentation improvements
- `performance` - Performance-related changes
- `security` - Security-related issues
- `good first issue` - Good for newcomers
- `help wanted` - Extra attention needed

## 💬 Communication

### Getting Help

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: Questions and general discussion
- **Documentation**: Check `docs/` directory first

### Asking Good Questions

1. **Search existing issues** first
2. **Provide context**: What are you trying to achieve?
3. **Include details**: Version, environment, error messages
4. **Minimal reproduction**: Simplest example that shows the issue

## 🎯 Areas for Contribution

### High Impact Areas
- **Performance optimization**: Algorithm improvements, caching
- **New algorithms**: Sliding window, leaky bucket implementations
- **Client libraries**: SDKs for different languages
- **Monitoring**: Enhanced metrics and dashboards
- **Documentation**: Tutorials, examples, guides

### Good First Issues
- **Test coverage**: Add tests for edge cases
- **Documentation**: Fix typos, improve examples
- **Configuration**: Add validation for properties
- **Logging**: Improve log messages and structure

## 🙏 Recognition

Contributors will be:
- **Listed** in release notes
- **Mentioned** in README acknowledgments
- **Invited** to project discussions
- **Credited** in significant feature announcements

## 📞 Contact

- **Maintainer**: Burhanettin Nacar
- **GitHub**: [@uppnrise](https://github.com/uppnrise)
- **Issues**: [GitHub Issues](https://github.com/uppnrise/distributed-rate-limiter/issues)

---

Thank you for contributing to make distributed rate limiting better for everyone! 🎉