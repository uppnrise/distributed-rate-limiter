# Distributed Rate Limiter v1.4.0 Release Notes

Release date: 2026-09-05

## Summary

`v1.4.0` is a minor release focused on third-party dependency modernization and security hardening across the Maven backend, build tooling, and the web dashboard's npm dependencies. No public API or configuration changes.

## Highlights

### Dependency upgrades

- Upgraded the Spring Boot parent to **4.1.1** (from 4.1.0).
- Bumped security-relevant BOM overrides: Jackson 2.x to 2.22.2, Jackson 3.x to 3.2.2, Logback to 1.6.3, Netty to 4.2.17.Final, Tomcat to 11.0.25.
- Upgraded `springdoc-openapi-starter-webmvc-ui` to 3.1.0, `geoip2` to 5.2.0 (unused in application code, zero runtime risk), and `gatling-charts-highcharts` to 3.15.1.
- Upgraded build plugins: `maven-compiler-plugin` (3.16.0), `jacoco-maven-plugin` (0.8.15), `spotbugs-maven-plugin` (4.10.4.0), `org.owasp:dependency-check-maven` (13.0.0), `scala-maven-plugin` (4.9.10), `gatling-maven-plugin` (4.21.11).
- Kept `scala-library` on the latest stable 2.13.x release (2.13.17); the only newer release is a Scala 3 pre-release, intentionally excluded.

### Security hardening

- Applied verified Snyk npm remediation for the Maven build tooling and resolved remaining Snyk container and Kubernetes manifest findings.
- Resolved web dashboard npm audit findings on two occasions, including newly-disclosed advisories in `browserslist`, `@humanfs/node`, and `postcss-selector-parser`.
- Stabilized Codecov project/patch status reporting.

### Documentation refresh

- Updates README release examples and versioned artifact references to `v1.4.0`.

## Compatibility

- No intentional public API breaking changes.
- Existing rate limiting endpoints and configuration keys remain compatible.
- This release is intended as a drop-in minor upgrade from `v1.3.2`.

## Validation

- `./mvnw clean compile`
- `./mvnw clean package -DskipTests`
- `./mvnw test` (full suite; Testcontainers/Redis-dependent tests require a local Docker daemon)
- Runtime smoke test: booted the packaged JAR and confirmed `/actuator/health/liveness` and `/api/ratelimit/check` respond correctly.

## Upgrade notes

- Update pinned artifact references from `v1.3.2` to `v1.4.0`.
- No configuration or API changes are required.
