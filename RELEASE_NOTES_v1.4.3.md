# Distributed Rate Limiter v1.4.3 Release Notes

Release date: 2026-09-16

## Summary

`v1.4.3` is a patch release focused on resolving Snyk-reported code security findings. No public API or configuration changes.

## Highlights

### Application security

- Resolved a Regular Expression Injection / ReDoS finding (CWE-400) in `ScheduleManagerService`, `ConfigurationResolver`, and `GeographicRateLimitConfig`. All three compiled user-controlled wildcard patterns into a regex and matched with `String.matches()`, which is vulnerable to catastrophic backtracking on crafted `*`-heavy patterns. Replaced with a new `dev.bnacar.distributedratelimiter.util.WildcardPatternMatcher` that implements a linear-time, backtracking-free glob algorithm plus a defensive max input length guard.
- Resolved a CRLF/HTTP header injection finding (CWE-113) in `CorrelationIdFilter`. Incoming `X-Correlation-ID`/`X-Trace-ID` request header values were reflected directly into response headers and MDC. They are now validated against a safe identifier charset (`[a-zA-Z0-9-]{1,128}`) before being reflected; anything else, including CR/LF, is discarded and replaced with a generated UUID.

### Testing

- Added `WildcardPatternMatcherTest`: matching parity with the old regex behavior, literal handling of regex metacharacters, null/length guards, and timing assertions proving no catastrophic backtracking on adversarial patterns.
- Added `CorrelationIdFilterTest` cases for CRLF injection attempts, disallowed characters, oversized input, and valid IDs passing through unchanged.

### Documentation refresh

- Updated release examples and artifact references to `v1.4.3`.

## Compatibility

- No intentional public API breaking changes.
- Existing rate limiting endpoints and configuration keys remain compatible, including wildcard pattern matching semantics (`*` behaves identically to the previous regex-based implementation for all legitimate patterns).
- This release is intended as a drop-in patch upgrade from `v1.4.2`.

## Validation

- `./mvnw clean install`: full suite passing (544 tests, 0 failures, 0 errors, 2 skipped), including 22 new tests for this release.
- Verified `WildcardPatternMatcher` resolves adversarial `*`-heavy patterns in well under 2 seconds with no exponential backtracking.
- Verified `CorrelationIdFilter` rejects CRLF and disallowed-character header values, replacing them with a generated UUID instead of reflecting them.

## Upgrade Notes

- Update pinned application version references from `v1.4.2` to `v1.4.3`.
- No configuration or API changes are required.
