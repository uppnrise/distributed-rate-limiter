# Geographic Rate Limiting

This documentation describes the geographic rate limiting feature implementation for the distributed rate limiter.

## Overview

Geographic rate limiting allows applying different rate limits based on the geographic location of incoming requests. This enables:

- **Regional Compliance**: Different limits for GDPR vs non-GDPR regions
- **Cost Optimization**: Lower limits for expensive international traffic  
- **Performance Optimization**: Higher limits for users closer to data centers
- **Security Enhancement**: Stricter limits for high-risk geographic regions
- **Business Strategy**: Premium service tiers for specific markets

## Quick Start

### 1. Enable Geographic Rate Limiting

Add to `application.properties`:
```properties
ratelimiter.geographic.enabled=true
```

### 2. Basic Usage

Send requests with geographic information via CDN headers or client info:

```bash
# With CloudFlare headers
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "CF-IPCountry: US" \
  -H "CF-IPContinent: NA" \
  -H "Content-Type: application/json" \
  -d '{"key":"api:user:123","tokens":1}'

# With AWS CloudFront headers  
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "CloudFront-Viewer-Country: DE" \
  -H "Content-Type: application/json" \
  -d '{"key":"api:user:123","tokens":1}'

# With explicit client info
curl -X POST http://localhost:8080/api/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{
    "key":"api:user:123",
    "tokens":1,
    "clientInfo": {
      "sourceIP": "92.168.1.100",
      "countryCode": "DE",
      "region": "EU"
    }
  }'
```

### 3. Manage Geographic Rules

```bash
# View current rules
curl http://localhost:8080/api/ratelimit/geographic/rules

# Add a new rule
curl -X POST http://localhost:8080/api/ratelimit/geographic/rules \
  -H "Content-Type: application/json" \
  -d '{
    "id": "eu-gdpr",
    "name": "EU GDPR Compliance",
    "keyPattern": "api:*",
    "complianceZone": "GDPR", 
    "limits": {
      "capacity": 500,
      "refillRate": 50,
      "algorithm": "TOKEN_BUCKET"
    },
    "priority": 100
  }'

# Test geographic detection
curl "http://localhost:8080/api/ratelimit/geographic/detect?sourceIP=92.168.1.100"
```

## Architecture

### Core Components

1. **GeoLocationService**: Detects geographic location from IP addresses and headers
2. **CDNHeaderParser**: Parses CloudFlare, AWS, Azure CDN headers  
3. **GeographicConfigurationResolver**: Manages and resolves geographic rules
4. **GeographicRateLimitService**: Orchestrates geographic rate limiting
5. **GeographicRateLimitController**: REST API for rule management

### Geographic Detection Priority

1. **CDN Headers** (CloudFlare → AWS → Azure → Generic)
2. **MaxMind GeoIP2** (when database available)
3. **IP Range Detection** (basic local/private IP detection)
4. **Fallback to Unknown**

### Configuration Resolution Priority

1. **Geographic Rules** (by priority score)
2. **Standard Per-Key Rules** 
3. **Standard Pattern Rules**
4. **Global Default Configuration**

## Geographic Rules

### Rule Structure

```json
{
  "id": "unique-rule-id",
  "name": "Human readable name",
  "keyPattern": "api:*",           // Rate limit key pattern
  "countryCode": "US",             // ISO 3166-1 alpha-2 
  "region": "NA",                  // Geographic region
  "complianceZone": "GDPR",        // Compliance zone
  "limits": {
    "capacity": 1000,
    "refillRate": 100,
    "algorithm": "TOKEN_BUCKET"
  },
  "priority": 100,                 // Higher = more priority
  "validFrom": "2024-01-01T00:00:00Z",
  "validUntil": "2024-12-31T23:59:59Z",
  "enabled": true
}
```

### Compliance Zones

- **GDPR**: EU countries (AT, BE, BG, HR, CY, CZ, DK, EE, FI, FR, DE, GR, HU, IE, IT, LV, LT, LU, MT, NL, PL, PT, RO, SK, SI, ES, SE)
- **CCPA**: California (US-CA)
- **PIPEDA**: Canada (CA)
- **LGPD**: Brazil (BR) 
- **PDPA_SG**: Singapore (SG)
- **DPA_UK**: United Kingdom (GB)

### Example Rules

```properties
# EU/GDPR region limits
ratelimiter.geographic.rules[0].name=eu-gdpr-limits
ratelimiter.geographic.rules[0].compliance-zone=GDPR
ratelimiter.geographic.rules[0].key-pattern=api:*
ratelimiter.geographic.rules[0].capacity=500
ratelimiter.geographic.rules[0].refill-rate=50
ratelimiter.geographic.rules[0].priority=100

# US premium region limits  
ratelimiter.geographic.rules[1].name=us-premium-limits
ratelimiter.geographic.rules[1].country-code=US
ratelimiter.geographic.rules[1].key-pattern=api:*
ratelimiter.geographic.rules[1].capacity=2000
ratelimiter.geographic.rules[1].refill-rate=200
ratelimiter.geographic.rules[1].priority=90
```

## API Reference

### Rate Limiting Endpoint

**POST** `/api/ratelimit/check`

Enhanced request with geographic support:

```json
{
  "key": "api:user:123",
  "tokens": 1,
  "apiKey": "optional-api-key",
  "clientInfo": {
    "sourceIP": "92.168.1.100",
    "countryCode": "DE", 
    "region": "EU",
    "city": "Berlin",
    "timezone": "Europe/Berlin",
    "headers": {
      "CF-IPCountry": "DE",
      "CF-IPContinent": "EU"
    }
  }
}
```

Enhanced response with geographic information:

```json
{
  "key": "api:user:123",
  "tokensRequested": 1,
  "allowed": true,
  "geoInfo": {
    "detectedCountry": "Germany",
    "detectedRegion": "EU", 
    "detectedCity": "Berlin",
    "complianceZone": "GDPR",
    "appliedRule": "geo:DE:GDPR",
    "appliedLimits": {
      "capacity": 500,
      "refillRate": 50,
      "algorithm": "TOKEN_BUCKET"
    },
    "detectionSource": "CDN_HEADER_CLOUDFLARE",
    "isVpnOrProxy": false
  }
}
```

### Geographic Management API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/ratelimit/geographic/rules` | GET | List all geographic rules |
| `/api/ratelimit/geographic/rules` | POST | Add new geographic rule |
| `/api/ratelimit/geographic/rules/{id}` | DELETE | Remove geographic rule |
| `/api/ratelimit/geographic/detect` | GET | Test geographic detection |
| `/api/ratelimit/geographic/stats` | GET | Get geographic statistics |
| `/api/ratelimit/geographic/cache/clear` | POST | Clear geographic caches |

## CDN Integration

### CloudFlare

```nginx
# NGINX configuration to pass CloudFlare headers
location /api/ratelimit {
    proxy_set_header CF-IPCountry $http_cf_ipcountry;
    proxy_set_header CF-IPContinent $http_cf_ipcontinent; 
    proxy_set_header CF-IPCity $http_cf_ipcity;
    proxy_set_header CF-Timezone $http_cf_timezone;
    proxy_pass http://rate-limiter;
}
```

### AWS CloudFront

```yaml
# CloudFront distribution configuration
ViewerProtocolPolicy: redirect-to-https
Headers:
  - CloudFront-Viewer-Country
  - CloudFront-Viewer-Country-Region
```

### Azure CDN

```json
{
  "headers": {
    "X-MS-Country-Code": "US",
    "X-Country-Code": "US"
  }
}
```

## Configuration Options

```properties
# Enable/disable geographic rate limiting
ratelimiter.geographic.enabled=false

# MaxMind GeoIP2 database (optional)
ratelimiter.geographic.ip-database-path=/data/GeoLite2-City.mmdb
ratelimiter.geographic.update-interval-hours=24

# Caching configuration
ratelimiter.geographic.cache-size=10000
ratelimiter.geographic.cache-ttl-hours=1

# VPN/Proxy detection (future)
ratelimiter.geographic.vpn-detection.enabled=false
ratelimiter.geographic.vpn-detection.strict-mode=false
```

## Performance Considerations

- **Geolocation Latency**: <1ms with CDN headers, <2ms with IP lookup
- **Memory Usage**: ~50MB for GeoIP2 database (when used)
- **Cache Hit Rate**: 95%+ for repeated IPs with LRU cache
- **Processing Overhead**: <2ms additional latency per request

## Security & Privacy

### Data Protection

- **IP Anonymization**: IPs hashed after geolocation (configurable)
- **Data Retention**: Configurable retention periods
- **Consent Management**: Respects user privacy preferences  
- **Audit Logging**: Geographic decisions logged for compliance

### Security Features

- **VPN Detection**: Identify and handle VPN/proxy traffic (future)
- **Tor Network**: Special handling for Tor exit nodes (future)
- **Geographic Anomalies**: Alert on unusual patterns (future)

## Monitoring & Observability

### Metrics

```bash
# View geographic statistics
curl http://localhost:8080/api/ratelimit/geographic/stats

# Prometheus metrics (when enabled)
curl http://localhost:8080/actuator/prometheus | grep geo
```

### Sample Response

```json
{
  "geoLocationCache": {
    "cacheSize": 1547,
    "maxCacheSize": 10000
  },
  "geographicConfigCache": {
    "cacheSize": 23,
    "maxCacheSize": 1000  
  },
  "totalRules": 4,
  "detectionSources": {
    "CDN_HEADER_CLOUDFLARE": 1203,
    "CDN_HEADER_AWS": 344,
    "IP_RANGE_LOCAL": 156,
    "UNKNOWN": 12
  }
}
```

## Troubleshooting

### Common Issues

1. **Geographic rules not applying**
   - Check `ratelimiter.geographic.enabled=true`
   - Verify rule priority and patterns
   - Test detection: `/api/ratelimit/geographic/detect`

2. **Performance issues**
   - Monitor cache hit rates
   - Increase cache size if needed
   - Use CDN headers instead of IP lookup

3. **Incorrect country detection**
   - Verify CDN header configuration
   - Check IP address format
   - Review detection priority order

### Debug Tools

```bash
# Test geographic detection
curl "http://localhost:8080/api/ratelimit/geographic/detect" \
  -H "CF-IPCountry: US"

# Clear caches if rules aren't updating
curl -X POST http://localhost:8080/api/ratelimit/geographic/cache/clear

# View all current rules
curl http://localhost:8080/api/ratelimit/geographic/rules
```

## Use Cases

### 1. SaaS Platform with Regional Compliance

```bash
# GDPR compliance - moderate limits for EU
POST /api/ratelimit/check
{
  "key": "api:user:eu:123",
  "tokens": 1,
  "clientInfo": {"countryCode": "DE", "region": "EU"}
}
# Response: capacity: 500, appliedRule: "geo:DE:GDPR"

# US premium - higher limits  
POST /api/ratelimit/check
{
  "key": "api:user:us:456", 
  "tokens": 1,
  "clientInfo": {"countryCode": "US", "region": "NA"}
}
# Response: capacity: 2000, appliedRule: "geo:US:NONE"
```

### 2. E-commerce with Regional Pricing

```bash
# Black Friday US - temporary high limits
{
  "name": "black_friday_us",
  "countryCode": "US",
  "keyPattern": "ecommerce:*",
  "capacity": 5000,
  "validFrom": "2024-11-29T00:00:00Z",
  "validUntil": "2024-11-29T23:59:59Z"
}
```

### 3. Financial Services with Compliance

```bash
# Strict limits for high-risk regions
{
  "name": "high_risk_countries",
  "countryCode": "XX", // Multiple countries via compliance zone
  "keyPattern": "finance:*",
  "capacity": 50,
  "priority": 200
}
```

## Migration Guide

### From Standard Rate Limiting

1. **Enable geographic features**:
   ```properties
   ratelimiter.geographic.enabled=true
   ```

2. **Existing keys continue to work** - no breaking changes

3. **Add geographic rules gradually**:
   ```bash
   # Start with high-priority rules for specific regions
   curl -X POST /api/ratelimit/geographic/rules -d '{...}'
   ```

4. **Monitor and adjust**:
   ```bash
   # Track geographic statistics 
   curl /api/ratelimit/geographic/stats
   ```

### Best Practices

1. **Start with CDN headers** for best performance
2. **Use compliance zones** for legal requirements  
3. **Set appropriate priorities** for rule conflicts
4. **Monitor cache hit rates** for performance
5. **Test with various geographic scenarios**
6. **Have fallback rules** for unknown regions