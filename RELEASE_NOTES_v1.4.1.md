# Distributed Rate Limiter v1.4.1 Release Notes

Release date: 2026-09-14

## Summary

`v1.4.1` is a patch release focused on resolving Snyk-reported container and Kubernetes manifest security findings. No public API or configuration changes.

## Highlights

### Container security

- Bumped the Docker base image from `eclipse-temurin:21.0.11_10` to `21.0.12_8` (JDK build stage and Alpine 3.23 JRE runtime stage), which resolves High-severity CVEs in the Alpine OS packages:
  - `expat`/`libexpat`: 2.8.3-r0 → 2.8.4-r0 (CVE-2026-76957, CVE-2026-66046, CVE-2026-76641, CVE-2026-76956, and others)
  - `sqlite-libs`: → 3.53.4-r0 (CVE-2026-11824, CVE-2026-11822)
  - `p11-kit-trust`: 0.25.5-r2 → 0.26.2-r0 (CVE-2026-2100)
  - `openssl`/`libssl3`/`libcrypto3`: 3.5.7-r0 → 3.5.8-r0 (CVE-2026-14456, CVE-2026-14457)

### Kubernetes hardening

- Added a missing liveness/readiness probe (`httpGet /metrics:9121`) to the `redis-exporter` sidecar container in `k8s/base/redis.yaml` (SNYK-CC-K8S-41).
- Changed the Redis pod's `runAsUser`/`runAsGroup`/`fsGroup` from `999` to `10001`, and added explicit container-level `securityContext` overrides on both `redis` and `redis-exporter`, to eliminate a potential UID clash with host user IDs (SNYK-CC-K8S-11). This also aligns Redis with the UID convention already used in the main application deployment.

### Dependency security

- Bumped the `netty.version` BOM override from `4.2.17.Final` to `4.2.18.Final`, resolving a Medium-severity Snyk finding (SNYK-JAVA-IONETTY-19778369, HTTP Request Smuggling) in `netty-codec-http`, pulled in transitively via `gatling-charts-highcharts` → `gatling-recorder` (test-scope only, load-testing tooling).

### Documentation refresh

- Updated release examples and artifact references to `v1.4.1`.

## Compatibility

- No intentional public API breaking changes.
- Existing rate limiting endpoints and configuration keys remain compatible.
- This release is intended as a drop-in patch upgrade from `v1.4.0`.
- If deploying `k8s/base/redis.yaml` to an existing cluster, note that the Redis pod's UID changed from `999` to `10001`; the PVC-backed `/data` volume is re-owned automatically via `fsGroup` on pod (re)scheduling.

## Validation

- `./mvnw test`: full suite passing (522 tests, 0 failures, 0 errors, 2 skipped).
- `docker build --no-cache` verified the updated base image builds successfully.
- Verified patched package versions inside the built image: `libexpat-2.8.4-r0`, `sqlite-libs-3.53.4-r0`, `p11-kit-trust-0.26.2-r0`, `openssl/libssl3/libcrypto3-3.5.8-r0`.
- Runtime smoke test: booted the packaged image and confirmed `/api/ratelimit/check` responds correctly.
- Validated `k8s/base/redis.yaml` YAML syntax and confirmed liveness/readiness probes and security context settings render as expected.

## Upgrade Notes

- Update pinned application version references from `v1.4.0` to `v1.4.1`.
- Rebuild and redeploy the Docker image to pick up the patched Alpine packages.
- Re-apply `k8s/base/redis.yaml` to pick up the Kubernetes hardening changes.
