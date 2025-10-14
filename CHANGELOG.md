# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2024-10-14

### Added
- **Fixed Window Counter Algorithm**: New memory-efficient rate limiting algorithm
  - 50% less memory usage compared to Token Bucket and Sliding Window
  - Predictable reset times with fixed interval boundaries
  - High-performance atomic operations
- Algorithm selection via configuration (`ratelimiter.algorithm` property)
- New Redis Lua script for atomic Fixed Window operations (`fixed-window.lua`)
- Enhanced API endpoint with algorithm parameter support
- Comprehensive test suite for Fixed Window algorithm
- Algorithm comparison and demonstration utilities
- Performance benchmarking for all three algorithms

### Changed
- Enhanced configuration resolution for algorithm-specific settings
- Updated API documentation with algorithm selection examples
- Improved error handling and validation for algorithm configuration
- Enhanced client examples with Fixed Window usage patterns

### Documentation
- **ADR-003**: Fixed Window Counter algorithm design document
- Updated performance comparisons and memory usage analysis
- Enhanced API documentation with algorithm examples
- Updated client integration guides (Java, Python, Node.js, Go, cURL)

### Technical
- `FixedWindow.java`: Local in-memory implementation
- `RedisFixedWindow.java`: Distributed Redis implementation
- Enhanced algorithm factory pattern for runtime selection
- Improved memory cleanup for expired windows
- Optimized Redis connection pooling

## [1.0.0] - 2024-10-01

### Added
- Initial release of Distributed Rate Limiter service
- **Token Bucket Algorithm**: Primary rate limiting algorithm with burst support
- **Sliding Window Algorithm**: Alternative algorithm for smooth rate limiting
- Redis backend for distributed rate limiting with atomic Lua scripts
- In-memory backend for development and testing
- RESTful API with 18 endpoints:
  - Rate limiting check endpoint (`/api/ratelimit/check`)
  - Configuration management (`/api/ratelimit/config/*`)
  - Admin operations (`/api/admin/*`)
  - Performance monitoring (`/api/performance/*`)
  - Benchmarking tools (`/api/benchmark/*`)
  - Health and metrics (`/actuator/*`)
- Configuration hierarchy: per-key > pattern-based > global defaults
- Comprehensive monitoring with Micrometer and Prometheus integration
- Docker support with multi-stage builds
- Kubernetes deployment manifests
- Extensive test suite (265+ tests) with Testcontainers integration

### Documentation
- **ADR-001**: Token Bucket algorithm selection rationale
- **ADR-002**: Redis distributed state management design
- Complete API documentation with Swagger/OpenAPI
- Client integration examples for multiple languages
- Performance and configuration guides
- Deployment and operations documentation

### Infrastructure
- Maven build system with Java 21 support
- CI/CD pipeline configuration
- Security scanning with OWASP dependency check
- Code coverage reporting with JaCoCo
- Multi-architecture Docker images (amd64, arm64)
- Production-ready logging and health checks

[1.1.0]: https://github.com/uppnrise/distributed-rate-limiter/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/uppnrise/distributed-rate-limiter/releases/tag/v1.0.0