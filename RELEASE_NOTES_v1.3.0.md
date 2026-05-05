# Distributed Rate Limiter v1.3.0 Release Notes

Release date: 2026-05-05

## Summary

`v1.3.0` is the next minor release after `v1.2.0`. It bundles new adaptive and time-based rate limiting capabilities with a substantial maintenance pass across the backend, dashboard, Docker images, and test infrastructure.

## Highlights

### Adaptive rate limiting

- Adds adaptive controls driven by traffic patterns, anomaly detection, and system metrics.
- Exposes adaptive behavior through the API and dashboard flows already present in `main`.

### Time-based dynamic rate limiting

- Adds schedule-aware rate limit changes and transition support.
- Includes dashboard support for managing schedules and viewing time-based behavior.

### Documentation and API coverage

- Expands OpenAPI and Swagger coverage.
- Improves deployment guides, ADRs, and client examples.

### Maintenance and security

- Upgrades Spring Boot to `3.5.11`.
- Upgrades springdoc to `2.8.15`.
- Clears dashboard audit findings and lint errors.
- Moves runtime and CI examples to `redis:8-alpine`.
- Pins Docker images to `eclipse-temurin:21.0.10_7-jdk` and `eclipse-temurin:21.0.10_7-jre`.

## Compatibility

- No intentional public API breaking changes.
- Existing documented REST endpoints, dashboard routes, and deployment ports remain unchanged.
- Redis 8 is now the default runtime and CI image.
- Redis 7 compatibility remains covered by explicit smoke tests.

## Validation

- `docker build -t distributed-rate-limiter:temurin-pin-test .`
- `./mvnw -q test`
- Dashboard security and lint maintenance had already been validated before merge with:
  - `npm run build`
  - `npm run lint`
  - `npm audit --audit-level=moderate`

## Upgrade notes

- If you deploy pinned image tags in Kubernetes or Compose, update them to `v1.3.0`.
- If you still run Redis 7 in production, keep it under observation during rollout even though compatibility coverage remains in place.
- If you rely on local release helper scripts, use the refreshed `1.3.0` metadata in the repository rather than older `1.0.0` or `1.2.0` examples.
