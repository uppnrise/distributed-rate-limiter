# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2025-10-23

### Added
- **🎨 Interactive Web Dashboard**: Modern React-based monitoring and management interface
  - Real-time metrics visualization with 5-second polling
  - Live monitoring dashboard with system metrics, algorithm distribution, and activity feed
  - Load testing interface connected to backend `/api/benchmark/run` endpoint
  - Configuration management with CRUD operations for global, per-key, and pattern-based limits
  - API Keys management page with active keys tracking and admin controls
  - Analytics page with historical trends (demo/preview feature)
  - Interactive algorithm comparison tool with educational simulations
  - Built with React 18, TypeScript, Vite, Tailwind CSS, shadcn/ui
  - 6 comprehensive dashboard screenshots included in documentation
  
- **🚰 Leaky Bucket Algorithm**: New traffic shaping algorithm for constant output rates
  - Queue-based processing at constant rate with no bursts allowed
  - Ideal for downstream service protection and SLA compliance
  - Redis backend support with atomic Lua scripts
  - In-memory implementation for development
  - Comprehensive test coverage and benchmarking
  - Complete documentation with examples (cURL, Java, Python, Node.js, Go)
  - ADR-004 documenting design decisions and use cases
  
- **🔄 Composite Rate Limiting**: Multi-algorithm composition with configurable logic
  - Combine multiple algorithms (Token Bucket, Sliding Window, Fixed Window, Leaky Bucket)
  - 5 combination logic types: ALL_MUST_PASS, ANY_CAN_PASS, WEIGHTED_AVERAGE, HIERARCHICAL_AND, PRIORITY_BASED
  - Component-level results with detailed breakdown
  - Scope support (USER, TENANT, GLOBAL, API, BANDWIDTH)
  - Weight and priority-based evaluation
  - Enterprise use cases: SaaS platforms, financial systems, gaming, IoT
  - Complete examples and documentation
  - ADR-005 with architecture decision rationale
  
- **🌍 Geographic Rate Limiting**: Location-aware rate limiting with compliance zone support
  - Multi-CDN header support (CloudFlare, AWS CloudFront, Azure CDN)
  - Automatic compliance zone detection (GDPR, CCPA, PIPEDA)
  - Country and region-based rate limiting rules
  - REST API endpoints for geographic rule management
  - Fallback logic for unknown locations
  - Performance optimized with <2ms additional latency
  - Complete integration examples and documentation
  
- **CORS Support**: Global CORS configuration for web dashboard frontend
  - Support for localhost:3000, localhost:5173, 127.0.0.1, IPv6 origins
  - Comprehensive headers (Authorization, Content-Type, X-Requested-With)
  - All HTTP methods (GET, POST, PUT, DELETE, OPTIONS)
  - Credential support and preflight caching

### Changed
- **Enhanced Documentation**: 
  - Main README updated with web dashboard section and comprehensive screenshots
  - Dashboard README with backend integration architecture and feature status
  - All API examples updated with Composite and Geographic rate limiting
  - Complete screenshot gallery for all dashboard pages
  - Updated copilot instructions with 5-algorithm support
  
- **Code Modernization**:
  - Replaced wildcard imports with specific imports across all services and controllers
  - Modernized test mocking to use @TestConfiguration
  - Updated Redis connection API to modern Spring Data Redis patterns
  - Replaced deprecated Spring test assertion methods
  - Improved API parameter handling with comprehensive model tests
  - Locale-independent toString implementations
  
- **Configuration System**:
  - Enhanced configuration resolution for algorithm-specific settings
  - Support for composite algorithm configuration via REST API
  - Geographic rule configuration with priority-based matching
  - Pattern-based configuration for geographic rules

### Fixed
- **CI/CD Test Failures**:
  - Fixed Redis connection pool test for CI/CD environments
  - Fixed ConcurrentPerformanceTest.testEndurance test stability
  - Added missing CompositeRateLimiterService mock
  - Made geographic components conditional to fix test failures
  
- **Frontend Issues**:
  - Resolved CORS issues between frontend and backend
  - Synchronized frontend TypeScript models with backend DTOs
  - Fixed Configuration page data loading issue
  - Added missing proxy routes for /admin and /metrics endpoints
  - Removed unused mock data generators from Dashboard and LoadTesting pages
  
- **Code Quality**:
  - Improved test coverage for geographic rate limiting
  - Enhanced error handling and validation for algorithm configuration
  - Better memory cleanup for expired windows

### Documentation
- **Architecture Decision Records (ADRs)**:
  - ADR-004: Leaky Bucket Algorithm design and use cases
  - ADR-005: Composite Rate Limiting architecture
  - Updated ADR README with complete algorithm comparison
  
- **API Documentation**:
  - Complete API reference with 18 endpoints
  - Geographic rate limiting endpoints documentation
  - Composite rate limiting configuration examples
  - Web dashboard integration guide
  
- **Client Examples**:
  - Updated Java, Python, Node.js, Go, cURL examples
  - Leaky Bucket usage patterns
  - Composite rate limiting integration examples
  - Geographic rate limiting with CDN headers
  
- **Deployment Guides**:
  - Web dashboard deployment instructions
  - Docker Compose setup for full stack
  - Kubernetes manifests updates
  - Production configuration recommendations

### Technical
- Total commits: 46 since v1.1.0
- New Java classes: LeakyBucket.java, RedisLeakyBucket.java, CompositeRateLimiter.java, CompositeRateLimiterService.java, GeographicRateLimitingController.java
- New React components: 20+ dashboard components with TypeScript
- Test coverage: Maintained >85% with new algorithm tests
- Performance: Maintained 50,000+ RPS with <2ms P95 latency
- Memory efficiency: Leaky Bucket 50% less memory than Token Bucket

### Breaking Changes
None - all changes are backward compatible. Existing configurations continue to work with default Token Bucket algorithm.

### Migration Notes
- Web dashboard is optional - backend continues to function independently
- New algorithms require explicit configuration - defaults remain unchanged
- Geographic rate limiting is opt-in via header detection
- Composite rate limiting requires specific configuration with `compositeConfig`

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

[1.2.0]: https://github.com/uppnrise/distributed-rate-limiter/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/uppnrise/distributed-rate-limiter/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/uppnrise/distributed-rate-limiter/releases/tag/v1.0.0