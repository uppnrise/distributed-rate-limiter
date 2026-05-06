#!/bin/bash

# GitHub Release Preparation Script
# Prepares artifacts for GitHub Releases page

set -e

# Configuration
VERSION="1.3.1"
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

# Distributed Rate Limiter v1.3.1 Quick Start
# This script helps you start the rate limiter quickly

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🚀 Distributed Rate Limiter v1.3.1 Quick Start${NC}"
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
java -jar distributed-rate-limiter-1.3.1.jar
EOF

chmod +x quick-start.sh

# Create Docker quick start
cat > docker-quick-start.sh << 'EOF'
#!/bin/bash

# Distributed Rate Limiter v1.3.1 Docker Quick Start

set -e

echo "🐳 Distributed Rate Limiter v1.3.1 - Docker Quick Start"
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
    image: ghcr.io/uppnrise/distributed-rate-limiter:1.3.1
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
# Installation Guide - Distributed Rate Limiter v1.3.1

## Quick Installation Options

### Option 1: JAR File (Recommended)

**Requirements:**
- Java 21 or higher
- Redis server (local or remote)

**Steps:**
1. Download the JAR file from GitHub Releases
2. Ensure Redis is running: `docker run -d -p 6379:6379 redis:8-alpine`
3. Run: `java -jar distributed-rate-limiter-1.3.1.jar`
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
2. Or manually: `docker run -p 8080:8080 ghcr.io/uppnrise/distributed-rate-limiter:1.3.1`

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
java -jar target/distributed-rate-limiter-1.3.1.jar
```

## Configuration

### Basic Configuration
The application uses sensible defaults but can be customized:

```bash
java -jar distributed-rate-limiter-1.3.1.jar \
  --spring.data.redis.host=your-redis-host \
  --spring.data.redis.port=6379 \
  --server.port=8080
```

### Environment Variables
```bash
export SPRING_DATA_REDIS_HOST=your-redis-host
export SPRING_DATA_REDIS_PORT=6379
export SERVER_PORT=8080
java -jar distributed-rate-limiter-1.3.1.jar
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
java -jar distributed-rate-limiter-1.3.1.jar --server.port=8081
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
# Distributed Rate Limiter v1.3.1 - Release Notes

Release date: 2026-05-06

## Summary

`v1.3.1` is a patch release focused on correctness and dashboard usability after `v1.3.0`.

## Highlights

### Backend correctness

- Fixes token bucket refill accumulation so sustained traffic no longer loses partial refill progress during frequent checks.
- Preserves the existing public API while making benchmark and rate-limit behavior more representative under steady load.

### Dashboard reliability

- Fixes the backend health banner so the dashboard works with minimal Spring Boot actuator health responses.
- Reworks recent activity to show a stable current snapshot on page load and real live deltas afterward.
- Removes fabricated algorithm performance and latency values where the backend does not provide that data yet.

### Load-testing improvements

- Fixes benchmark pacing from the dashboard so configured aggregate request rate and duration map more closely to backend execution.
- Improves live progress reporting and final summaries with clearer request-count and throughput context.
- Marks latency metrics as unavailable until the backend benchmark endpoint exposes real response-time measurements.

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
wget https://github.com/uppnrise/distributed-rate-limiter/releases/download/v1.3.1/distributed-rate-limiter-1.3.1.jar
java -jar distributed-rate-limiter-1.3.1.jar
```

### Docker
```bash
docker run -p 8080:8080 ghcr.io/uppnrise/distributed-rate-limiter:1.3.1
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
- **SHA256**: See `distributed-rate-limiter-1.3.1.jar.sha256`
- **MD5**: See `distributed-rate-limiter-1.3.1.jar.md5`

## Breaking Changes
None.

## Upgrade Notes  
Update pinned application version references from `v1.3.0` to `v1.3.1`.

## Known Issues
Latency metrics are not included yet in the backend benchmark endpoint, so release helper dashboards and reports correctly mark them as unavailable.

## What's Next (v1.3.1)
- Add backend latency measurements to benchmark responses.
- Continue dashboard refinement around live test reporting and activity history.
- Keep backend and dashboard maintenance moving in small patch releases.

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
