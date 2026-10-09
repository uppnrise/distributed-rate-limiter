# Snyk dependency remediation

This repository uses the [snyk-auto-remediate reusable workflow](https://github.com/uppnrise/snyk-auto-remediate)
for repository-local dependency scans and exact, Snyk-evidenced upgrades. The workflow
and engine are both pinned to commit `e4194db787ff0712f20a4c9094045d501257d36d`
from release candidate `v1.0.0-rc.2`. Update both references together when upgrading.

## Repository setup

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

The existing CI/CD pipeline retains its dependency scans, PR checks, and deployment
jobs. Remediation runs independently, so a failing security scan does not prevent
the scheduled workflow from attempting a fix.
