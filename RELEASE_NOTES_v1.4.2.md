# Distributed Rate Limiter v1.4.2 Release Notes

Release date: 2026-09-16

## Summary

`v1.4.2` is a patch release focused on resolving Snyk-reported code and Kubernetes manifest security findings. No public API or configuration changes.

## Highlights

### Application security

- Resolved Snyk Code (CWE-79, Cross-Site Scripting) findings in `GeographicRateLimitController`, `RateLimitConfigController`, and `AdminController`, where unsanitized request URL/body input (rule name, rule ID, rate-limit key, pattern) was reflected directly into plain-text HTTP responses. All reflected values are now HTML-escaped via `HtmlUtils.htmlEscape()` before being included in the response body.

### Kubernetes hardening

- Set `imagePullPolicy: Always` on the `redis` and `redis-exporter` containers in `k8s/base/redis.yaml` (SNYK-CC-K8S-42), matching the convention already used in `deployment.yaml` and `backup-cronjob.yaml`.

### Documentation refresh

- Updated release examples and artifact references to `v1.4.2`.

## Compatibility

- No intentional public API breaking changes.
- Existing rate limiting endpoints and configuration keys remain compatible.
- This release is intended as a drop-in patch upgrade from `v1.4.1`.

## Validation

- `./mvnw clean install`: full suite passing (522 tests, 0 failures, 0 errors, 2 skipped).
- Verified reflected values (rule name/ID, rate-limit key/pattern) are HTML-escaped in `addRule`, `removeRule`, `updateKeyConfiguration`, `removeKeyConfiguration`, `updatePatternConfiguration`, `removePatternConfiguration`, and `removeKeyLimits` responses.
- Validated `k8s/base/redis.yaml` YAML syntax and confirmed `imagePullPolicy: Always` is set on both containers.

## Upgrade Notes

- Update pinned application version references from `v1.4.1` to `v1.4.2`.
- Re-apply `k8s/base/redis.yaml` to pick up the `imagePullPolicy` change.
