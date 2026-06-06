# Distributed Rate Limiter v1.3.2 Release Notes

Release date: 2026-06-06

## Summary

`v1.3.2` is a patch release focused on safer API responses and deployment-friendly CORS configuration.

## Highlights

### Deployment flexibility

- Moves CORS configuration into centralized `ratelimiter.cors.*` properties.
- Removes duplicated controller-level CORS annotations so one configuration applies consistently across the API.
- Supports environment-specific origin overrides through normal Spring property binding and environment variables.

### API hardening

- Replaces raw exception messages in API responses with safe client-facing error messages.
- Adds centralized exception handling for uncaught API failures while preserving detailed server-side logs.

### Documentation refresh

- Updates README release examples and versioned artifact references to `v1.3.2`.
- Documents the new CORS properties in the configuration and deployment guides.

## Compatibility

- No intentional public API breaking changes.
- Existing rate limiting endpoints and configuration keys remain compatible.
- This release is intended as a drop-in patch upgrade from `v1.3.1`.

## Validation

- `./mvnw clean verify jacoco:report jacoco:check`
- `./mvnw -Dtest=CorsConfigurationPropertiesTest,WebCorsConfigurationTest test`

## Upgrade notes

- Update pinned artifact references from `v1.3.1` to `v1.3.2`.
- If you rely on browser-based clients, review `ratelimiter.cors.allowed-origins` and `ratelimiter.cors.allowed-origin-patterns` for your deployment environments.
