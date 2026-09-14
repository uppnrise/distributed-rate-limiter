#!/bin/bash

# GitHub Release Preparation Script
# Prepares artifacts for GitHub Releases page

set -e

# Configuration
VERSION="1.4.1"
RELEASE_DIR="release-artifacts"
PROJECT_NAME="distributed-rate-limiter"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Preparing GitHub Release v${VERSION}${NC}"
echo "============================================="

# Clean and build with tests
echo -e "${BLUE}📦 Building project with tests...${NC}"
./mvnw clean install

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Build successful${NC}"

# Create release directory
echo -e "${BLUE}📁 Creating release artifacts...${NC}"
rm -rf ${RELEASE_DIR}
mkdir -p ${RELEASE_DIR}

# Copy main JAR
cp target/${PROJECT_NAME}-${VERSION}.jar ${RELEASE_DIR}/

# Create checksums
cd ${RELEASE_DIR}
echo -e "${BLUE}🔐 Creating checksums...${NC}"
sha256sum ${PROJECT_NAME}-${VERSION}.jar > ${PROJECT_NAME}-${VERSION}.jar.sha256
md5sum ${PROJECT_NAME}-${VERSION}.jar > ${PROJECT_NAME}-${VERSION}.jar.md5

# Create quick start script
echo -e "${BLUE}📋 Creating quick start script...${NC}"
cat > quick-start.sh << 'EOF'
#!/bin/bash

# Distributed Rate Limiter v1.4.1 Quick Start
# This script helps you start the rate limiter quickly

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🚀 Distributed Rate Limiter v1.4.1 Quick Start${NC}"
echo "=============================================="
echo ""

# Check Java version
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed${NC}"
    echo "Please install Java 21+ and try again"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${RED}❌ Java 21+ required, found Java ${JAVA_VERSION}${NC}"
    echo "Please upgrade Java and try again"
    exit 1
fi

echo -e "${GREEN}✅ Java ${JAVA_VERSION} detected${NC}"

# Check if Redis is available (optional check)
if command -v redis-cli &> /dev/null; then
    if redis-cli ping &> /dev/null; then
        echo -e "${GREEN}✅ Redis server is running${NC}"
    else
        echo -e "${YELLOW}⚠️ Redis server not detected on localhost:6379${NC}"
        echo "The application will try to connect to Redis on startup"
        echo "If you don't have Redis running, start it with:"
        echo "  docker run -d -p 6379:6379 redis:8-alpine"
    fi
else
    echo -e "${YELLOW}⚠️ Redis CLI not found${NC}"
    echo "Make sure Redis server is running on localhost:6379"
    echo "Quick Redis setup: docker run -d -p 6379:6379 redis:8-alpine"
fi

echo ""
echo -e "${BLUE}🎯 Starting application...${NC}"
echo "📊 Health check will be available at: http://localhost:8080/actuator/health"
echo "📖 API documentation at: http://localhost:8080/swagger-ui/index.html"
echo "⏹️  Press Ctrl+C to stop"
echo ""

# Start the application
java -jar distributed-rate-limiter-1.4.1.jar
EOF

chmod +x quick-start.sh

# Create Docker quick start
cat > docker-quick-start.sh << 'EOF'
#!/bin/bash

# Distributed Rate Limiter v1.4.1 Docker Quick Start

set -e

echo "🐳 Distributed Rate Limiter v1.4.1 - Docker Quick Start"
echo "======================================================"

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed"
    echo "Please install Docker and try again"
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose is not available"
    echo "Please install Docker Compose and try again"
    exit 1
fi

echo "✅ Docker detected"

# Create a simple docker-compose.yml
cat > docker-compose.yml << 'COMPOSE_EOF'
version: '3.8'

services:
  redis:
    image: redis:8-alpine
    container_name: rate-limiter-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3

  rate-limiter:
    image: ghcr.io/uppnrise/distributed-rate-limiter:1.4.1
    container_name: rate-limiter-app
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_PROFILES_ACTIVE=production
    depends_on:
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

COMPOSE_EOF

echo "📦 Starting services with Docker Compose..."
docker-compose up -d

echo ""
echo "✅ Services started successfully!"
echo ""
echo "📊 Service URLs:"
echo "  🌐 Rate Limiter API: http://localhost:8080"
echo "  📋 Health Check: http://localhost:8080/actuator/health"
echo "  📖 API Documentation: http://localhost:8080/swagger-ui/index.html"
echo "  🗄️ Redis: localhost:6379"
echo ""
echo "📋 Useful commands:"
echo "  📜 View logs: docker-compose logs -f"
echo "  ⏹️  Stop services: docker-compose down"
echo "  🔄 Restart: docker-compose restart"
EOF

chmod +x docker-quick-start.sh

# Create installation guide
cat > INSTALLATION.md << 'EOF'
# Installation Guide - Distributed Rate Limiter v1.4.1

## Quick Installation Options

### Option 1: JAR File (Recommended)

**Requirements:**
- Java 21 or higher
- Redis server (local or remote)

**Steps:**
1. Download the JAR file from GitHub Releases
2. Ensure Redis is running: `docker run -d -p 6379:6379 redis:8-alpine`
3. Run: `java -jar distributed-rate-limiter-1.4.1.jar`
4. Test: `curl http://localhost:8080/actuator/health`

**Quick Start Script:**
```bash
chmod +x quick-start.sh
./quick-start.sh
```

### Option 2: Docker

**Requirements:**
- Docker and Docker Compose

**Steps:**
1. Run the Docker quick start script: `./docker-quick-start.sh`
2. Or manually: `docker run -p 8080:8080 ghcr.io/uppnrise/distributed-rate-limiter:1.4.1`

### Option 3: Build from Source

**Requirements:**
- Java 21 or higher
- Maven 3.8+
- Git

**Steps:**
```bash
git clone https://github.com/uppnrise/distributed-rate-limiter.git
cd distributed-rate-limiter
./mvnw clean install
java -jar target/distributed-rate-limiter-1.4.1.jar
```

## Configuration

### Basic Configuration
The application uses sensible defaults but can be customized:

```bash
java -jar distributed-rate-limiter-1.4.1.jar \
  --spring.data.redis.host=your-redis-host \
  --spring.data.redis.port=6379 \
  --server.port=8080
```

### Environment Variables
```bash
export SPRING_DATA_REDIS_HOST=your-redis-host
export SPRING_DATA_REDIS_PORT=6379
export SERVER_PORT=8080
java -jar distributed-rate-limiter-1.4.1.jar
```

### Configuration File
Create `application.properties`:
```properties
spring.data.redis.host=your-redis-host
spring.data.redis.port=6379
server.port=8080
```

## Verification

After starting the application:

1. **Health Check**: `curl http://localhost:8080/actuator/health`
2. **API Documentation**: Open http://localhost:8080/swagger-ui/index.html
3. **Test Rate Limiting**:
   ```bash
   curl -X POST http://localhost:8080/api/ratelimit/check \
     -H "Content-Type: application/json" \
     -d '{"key": "test-user", "tokens": 1}'
   ```

## Production Deployment

See the main README.md for production deployment guides including:
- Kubernetes manifests
- Docker Compose for production
- Configuration management
- Monitoring setup
- Performance tuning

## Troubleshooting

### Common Issues

**Issue**: Application fails to start with Redis connection error
**Solution**: Ensure Redis is running and accessible
```bash
docker run -d -p 6379:6379 redis:8-alpine
```

**Issue**: Java version error
**Solution**: Install Java 21+
```bash
# Check version
java -version

# Should show Java 21 or higher
```

**Issue**: Port 8080 already in use
**Solution**: Use a different port
```bash
java -jar distributed-rate-limiter-1.4.1.jar --server.port=8081
```

### Getting Help

- 📖 Documentation: README.md in the repository
- 🐛 Issues: GitHub Issues page
- 💬 Discussions: GitHub Discussions
- 📧 Contact: Check repository for contact information
EOF

cd ..

# Create release notes
cat > ${RELEASE_DIR}/RELEASE_NOTES.md << 'EOF'
# Distributed Rate Limiter v1.4.1 - Release Notes

Release date: 2026-09-14

## Summary

`v1.4.1` is a patch release focused on resolving Snyk-reported container, Kubernetes manifest, and dependency security findings. No public API or configuration changes.

## Highlights

### Container security

- Bumped the Docker base image from `eclipse-temurin:21.0.11_10` to `21.0.12_8` (JDK build stage and Alpine 3.23 JRE runtime stage), which resolves High-severity CVEs in the Alpine OS packages `expat` (2.8.3-r0 → 2.8.4-r0), `sqlite-libs` (→ 3.53.4-r0), `p11-kit-trust` (0.25.5-r2 → 0.26.2-r0), and `openssl`/`libssl3`/`libcrypto3` (3.5.7-r0 → 3.5.8-r0).

### Kubernetes hardening

- Added a missing liveness/readiness probe to the `redis-exporter` sidecar container in `k8s/base/redis.yaml`.
- Changed the Redis pod's `runAsUser`/`runAsGroup`/`fsGroup` from `999` to `10001` (and added explicit container-level `securityContext` overrides) to avoid a UID clash with host user IDs, aligning with the UID convention already used in the main application deployment.

### Dependency security

- Bumped the `netty.version` BOM override from `4.2.17.Final` to `4.2.18.Final`, resolving a Medium-severity Snyk finding (SNYK-JAVA-IONETTY-19778369, HTTP Request Smuggling) in `netty-codec-http`, a transitive test-scope dependency pulled in via Gatling load-testing tooling.

### Documentation refresh

- Updates release examples and artifact references to `v1.4.1`.

### Deployment Options
- **JAR File**: Self-contained Spring Boot application (42MB)
- **Docker Image**: Container-ready with optimized layers
- **Docker Compose**: Complete stack with Redis
- **Kubernetes**: Production-ready manifests

### Documentation & Examples
- **Complete API Reference** - OpenAPI/Swagger documentation
- **Integration Examples** - Java, Python, Node.js, Go clients
- **Deployment Guides** - Docker, Kubernetes, traditional deployment
- **Configuration Guide** - Detailed configuration options

## Download & Installation

### JAR File (Recommended)
```bash
# Download and run
wget https://github.com/uppnrise/distributed-rate-limiter/releases/download/v1.4.1/distributed-rate-limiter-1.4.1.jar
java -jar distributed-rate-limiter-1.4.1.jar
```

### Docker
```bash
docker run -p 8080:8080 ghcr.io/uppnrise/distributed-rate-limiter:1.4.1
```

### Quick Start Scripts
- `quick-start.sh` - JAR quick start with dependency checks
- `docker-quick-start.sh` - Docker Compose setup
- `INSTALLATION.md` - Complete installation guide

## Requirements
- **Java 21+** (OpenJDK or Oracle JDK)
- **Redis server** (local or remote)
- **2GB RAM minimum** for production usage

## File Checksums
- **SHA256**: See `distributed-rate-limiter-1.4.1.jar.sha256`
- **MD5**: See `distributed-rate-limiter-1.4.1.jar.md5`

## Breaking Changes
None. This release is intended as a drop-in upgrade from `v1.4.0`.

## Upgrade Notes
- Update pinned application version references from `v1.4.0` to `v1.4.1`.
- No configuration or API changes are required. If deploying `k8s/base/redis.yaml` to an existing cluster, note that the Redis pod's UID changed from `999` to `10001`; the PVC-backed `/data` volume is re-owned automatically via `fsGroup` on pod (re)scheduling.

## Known Issues
Load-test artifacts still do not include backend latency measurements, so helper dashboards and reports continue to mark those metrics as unavailable.

## What's Next (post v1.4.1)
- Continue routine dependency and security maintenance.
- Add backend latency measurements to benchmark responses.
- Keep backend hardening work moving in small releases.

## Support
- 📖 **Documentation**: Complete guides included
- 🐛 **Issues**: GitHub Issues for bug reports
- 💡 **Feature Requests**: GitHub Discussions
- 🤝 **Contributing**: See CONTRIBUTING.md in repository
EOF

# Summary
echo ""
echo -e "${GREEN}🎉 GitHub Release v${VERSION} artifacts prepared!${NC}"
echo "=================================================="
echo ""
echo -e "${BLUE}📦 Created artifacts:${NC}"
ls -la "${RELEASE_DIR}"
echo ""
echo -e "${BLUE}📋 File sizes:${NC}"
du -h "${RELEASE_DIR}"/*
echo ""
echo -e "${BLUE}🔐 Checksums:${NC}"
cat "${RELEASE_DIR}"/*.sha256
echo ""
echo -e "${BLUE}🚀 Next steps:${NC}"
echo "1. Go to: https://github.com/uppnrise/distributed-rate-limiter/releases"
echo "2. Click 'Create a new release'"
echo "3. Tag: v${VERSION}"
echo "4. Upload all files from ${RELEASE_DIR}/"
echo "5. Use RELEASE_NOTES.md content for the description"
echo ""
echo -e "${YELLOW}💡 Tip: Test the quick-start.sh script before releasing!${NC}"
