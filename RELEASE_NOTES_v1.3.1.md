# Distributed Rate Limiter v1.3.1 Release Notes

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

## Compatibility

- No intentional public API breaking changes.
- Existing documented REST endpoints, dashboard routes, ports, and container defaults remain unchanged.
- This release is intended as a drop-in patch upgrade from `v1.3.0`.

## Validation

- `./mvnw -q test`
- `cd examples/web-dashboard && npm run build`
- `./prepare-github-release.sh`

## Upgrade notes

- Update pinned application version references from `v1.3.0` to `v1.3.1`.
- If you consume generated release artifacts, replace prior `1.3.0` JAR, checksum, and helper scripts with the regenerated `1.3.1` set.
