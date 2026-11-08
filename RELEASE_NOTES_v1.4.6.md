# Distributed Rate Limiter v1.4.6

Release date: 2026-11-08

## Changes

- Supersedes v1.4.5 with the dashboard minimatch audit fix: enforce ^11.0.1, locked to 11.0.5, for GHSA-g3hv-75wg-m249. No findings are ignored.

- Upgrade the build and Alpine 3.23 runtime to Eclipse Temurin 21.0.13+9.
- Retain Alpine security upgrades and require patched minimum versions of brotli-libs, libexpat, zlib, musl, musl-utils and libpng. The build fails if patches are unavailable.
- Preserve non-root UID/GID 10001, health checks and application API behavior.
- Includes the dependency updates already on main since v1.4.4; see docs/SNYK_REMEDIATION.md for details, including the Jakarta Validation 4.0.0-M1 override.

## Validation and limitations

The dashboard fix passed CI tests, coverage, low-threshold npm audit (zero vulnerabilities), lint, production build, and Snyk dependency scans. Dependency scans are separate from the pending container scan. The deployment bundle includes the built dashboard and a dashboard source archive.

The container remediation passed a fresh image build, all six package-version checks and a Redis-backed health/non-root smoke test. Java 21 Maven verification passed (557 tests, zero failures, two skipped).

Snyk container verification is pending: the CLI returned SNYK-0005 / HTTP 401. This release does not claim a clean Snyk scan or confirmation that all 12 findings are absent. The linked Snyk Dockerfile project also requires an authenticated retest.

## Artifacts

The executable JAR requires Java 21 and Redis. The Docker image archive is Linux arm64, built and tested locally. Load it with `gzip -dc distributed-rate-limiter-1.4.6-docker-image.tar.gz | docker load`.

The deployment bundle includes the JAR, image archive, Compose configuration, Dockerfile, startup scripts, Kubernetes manifests and documentation. Verify downloads against CHECKSUMS.txt. GitHub supplies source archives for the release tag.
