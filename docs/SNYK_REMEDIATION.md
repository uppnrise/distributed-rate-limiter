# Snyk dependency remediation

## Container remediation (2026-11-08)

The Dockerfile uses `eclipse-temurin:21.0.13_9-jdk` for the build stage and
`eclipse-temurin:21.0.13_9-jre-alpine-3.23` for runtime. The matching JDK tag
was verified against Docker Hub's published manifest before editing. Java 21,
Alpine 3.23, health checks and the non-root `appuser` are retained.

`apk upgrade --no-cache` is retained, followed by explicit minimum versions for
the affected packages. These constraints fail the build if patches are unavailable;
no findings are ignored. The base image alone still contains `zlib-1.3.2-r1`;
the repository upgrade supplies `1.4-r0`.

The fresh local Linux arm64 build installed:

| Package | Required minimum | Installed |
| --- | --- | --- |
| brotli-libs | 1.2.1-r0 | 1.2.1-r0 |
| libexpat | 2.10.0-r0 | 2.10.0-r0 |
| zlib | 1.4-r0 | 1.4-r0 |
| musl | 1.2.5-r24 | 1.2.5-r24 |
| musl-utils | 1.2.5-r24 | 1.2.5-r24 |
| libpng | 1.6.59-r0 | 1.6.59-r0 |

All patches were available from the configured Alpine `v3.23/main` repository;
`v3.23/community` remains configured. No edge or alternate repositories were added.

Validation evidence:

- `docker build --pull --no-cache -t distributed-rate-limiter:snyk-fix .` passed.
  Local image ID: `sha256:c980bc6b1818882d62a8947c4917a39cf3dacb7fada6fc14e43bb55b4ca27cbd`.
- Runtime inspection confirmed Alpine `3.23.8`, Temurin `21.0.13+9`, the package
  versions above, and UID/GID `10001` (`appuser`).
- An isolated smoke test with `redis:8-alpine` passed: `/actuator/health` returned
  `UP`, including Redis, and Docker reported the application container `healthy`.
- `./mvnw --batch-mode clean verify` passed under Java `21.0.8`: 557 tests,
  zero failures/errors, two skipped; JaCoCo coverage checks passed. The existing
  Docker image test now expects and exercises the updated runtime tag.
- Snyk CLI `1.1308.4` was invoked with
  `snyk container test distributed-rate-limiter:snyk-fix --file=Dockerfile --severity-threshold=low --json-file-output=/tmp/snyk-container-scan.json`.
  It failed with `SNYK-0005` / HTTP 401 (credentials not recognized or access not
  provisioned), before producing a vulnerability report. The failure log is
  `/tmp/snyk-container-scan.log`; no clean report was generated.

Acceptance remains pending a successful authenticated low-threshold scan proving
all 12 supplied findings absent. Separately retest the existing linked Snyk
Dockerfile project after it has ingested this branch/commit; its project URL/ID
is not recorded in this repository, and the available CLI credentials were rejected.
The Maven/npm remediation jobs do not refresh that Dockerfile snapshot.

This repository uses the [snyk-auto-remediate reusable workflow](https://github.com/uppnrise/snyk-auto-remediate)
for repository-local dependency scans and exact, Snyk-evidenced upgrades. The workflow
and engine are both pinned to commit `4e2006f18df4f1bf7f8a7914cae69371f56f8e62`
from the upstream revision with passing CI and remediation runs. Update both references together when upgrading.

## Repository setup

The dashboard overrides transitive `minimatch` to `^11.0.1` (locked to `11.0.5`)
for GHSA-g3hv-75wg-m249. ESLint and TypeScript ESLint currently request vulnerable
10.x releases, so a regular lockfile refresh cannot supply the fixed major version.
Remove the override when both dependency chains request patched versions themselves.
Validated with Node.js 24: `npm ci --ignore-scripts`, `npm audit --audit-level=low`
(zero vulnerabilities), `npm run lint`, and `npm run build`. No audit ignores or
severity-threshold changes are used.

1. Add `SNYK_TOKEN` under **Settings → Secrets and variables → Actions**. Use a
   Snyk token with access to scan this repository's dependencies.
2. Under **Settings → Actions → General → Workflow permissions**, enable
   **Allow GitHub Actions to create and approve pull requests**. The caller requests
   `contents: write`, `pull-requests: write`, `issues: write`, and
   `security-events: write`; organization policy must allow these permissions.
3. Ensure GitHub code scanning is available for SARIF uploads. JSON and SARIF reports
   are also uploaded as Actions artifacts, including when remediation fails.
4. Optionally add `SNYK_REMEDIATION_PAT` as an Actions secret. It is passed to the
   engine as `GH_PAT`; without it, the engine uses the caller's `GITHUB_TOKEN`.
   For a fine-grained PAT, limit access to this repository and grant read/write
   **Contents**, **Pull requests**, **Issues**, and **Code scanning alerts** permissions.

GitHub normally suppresses new workflow runs for pushes and PRs created with
`GITHUB_TOKEN`. Configure the optional PAT when remediation PRs must automatically
run the existing CI/CD pipeline and required branch checks. Post-fix validation
inside the remediation job still runs before publishing changes with either token.
See [GitHub's workflow triggering rules](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow).
The workflow does not merge PRs; review and merge them through the usual process.

## First run and schedule

Open **Actions → Snyk Auto-Remediation → Run workflow**, select the feature branch
to validate the integration before merging, and leave `dry_run` enabled. The reusable
workflow scans `main` even when its caller is dispatched from another branch.

Review the job summaries and `snyk-remediation-maven-*` and
`snyk-remediation-dashboard-*` artifacts. Dry-run scans and reports planned changes,
but does not apply upgrades, run post-fix validation, or mutate GitHub PRs/issues.
Run again with `dry_run` disabled to apply verified upgrades and publish PRs.

After this workflow reaches the default branch, it runs every Monday at **03:00 UTC**
with dry-run disabled. Manual runs default to dry-run. A run with no applicable
upgrades succeeds; it is not treated as an automation failure.

## Project scopes and validation

| Project | Directory | Manager | Report ID | Validation after fixes |
| --- | --- | --- | --- | --- |
| Backend | `.` | Maven | `maven` | `./mvnw --batch-mode clean verify` with Java 21 |
| Dashboard | `examples/web-dashboard` | npm | `dashboard` | `npm ci --ignore-scripts && npm run lint && npm run build` with Node.js 24 |

The engine detects manifests only at the selected directory's top level, so both
projects have separate jobs and stable report IDs. The Maven suite uses
Testcontainers on the reusable workflow's Ubuntu runner. The dashboard currently
has no `test` script; lint and the production build provide its post-fix checks.

Both jobs scan severities `low` and above, re-scan CLI-evidenced fixes, and publish
separate remediation branches/PRs. The reusable workflow serializes concurrent runs
for each repository, target branch, and report ID. Tests must pass before pushing.
Generated local JSON/SARIF reports are ignored by Git and retained as Actions artifacts.

Local CLI inventory is used without `SNYK_ORG_ID` or `snyk-project-ids`. Copilot
fallback issues are disabled, preserving the previous workflow's behavior.
Findings without an exact upgrade remain in reports for manual review. Live-mode
workflow failures can still open or update an `automation-failure` issue per report
ID; the engine closes that alert after a successful run.

The dashboard uses patched Zod 4 and a compatible form resolver. The temporary
`SNYK-JS-ZOD-20510278` ignore has been removed. Neither project suppresses Snyk
findings; both scan at the low-severity threshold.

## Refreshing GitHub Security alerts

Every push to `main` triggers a scan-only remediation run. This publishes complete
SARIF reports, including valid reports with zero results, under the existing
`Snyk Auto-Remediation` tool and `snyk-auto-remediation-maven` /
`snyk-auto-remediation-dashboard` categories. The uploader resolves the scanned
checkout's commit and waits for GitHub to process the report. Scan failures must
fail the job rather than publish an empty clean report.

The CI/CD pipeline separately retains its dependency scans, PR checks and report
artifacts. Its CLI follows Snyk's stable channel to avoid removed binary versions.
The push-triggered remediation workflow supplies the GitHub code-scanning updates;
artifact uploads alone do not resolve alerts.

After merging a dependency fix, check both remediation jobs on that exact `main`
commit, then verify alerts #44, #45 and #46 are fixed. Do not dismiss an alert to
substitute for a successful scan. Refresh the existing Snyk SCM project separately
if its inventory has not yet picked up the merge.

Weekly runs still attempt fixes and publish validated remediation PRs; manual runs
default to scan-only mode. Findings without an exact upgrade remain in reports for
manual review.

## Authentication failures

`SNYK-0005` / HTTP 401 means Snyk rejected the configured `SNYK_TOKEN`; it is not a
clean vulnerability result. Replace that repository Actions secret with a valid
API token provisioned for the Snyk account/organization, then rerun CI and the
scan-only remediation workflow. The Snyk GitHub integration uses separate
credentials, so its passing PR check does not validate the Actions secret.
Never upload an empty SARIF report or dismiss alerts to bypass this failure.

## Additional backend advisories

Both Jackson BOMs are overridden to 2.22.4 and 3.2.4 for the critical
CVE-2026-110879 advisories `SNYK-JAVA-COMFASTERXMLJACKSONCORE-21761837` and
`SNYK-JAVA-TOOLSJACKSONCORE-21761838`.

Jakarta Validation API is explicitly overridden to `4.0.0-M1` for
`SNYK-JAVA-JAKARTAVALIDATION-20936881`. Snyk currently lists no stable fixed
version. This prerelease override was approved subject to compatibility tests;
keep the Java 21 validation/controller tests passing and replace it with a stable
fixed release when one is available. Do not remove the override while the Boot BOM
still selects affected 3.1.1.
