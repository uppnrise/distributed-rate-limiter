# CI/CD Pipeline Documentation

This document describes the CI/CD pipeline setup for the Distributed Rate Limiter project.

## Pipeline Overview

The CI/CD pipeline is implemented using GitHub Actions and includes the following stages:

1. **Test & Code Coverage** - Runs all tests and generates code coverage reports
2. **Static Analysis** - Performs code quality analysis using SpotBugs, PMD, and Checkstyle
3. **Security Scanning** - Scans dependencies for known vulnerabilities using OWASP
4. **Docker Build & Push** - Builds and pushes Docker images to GitHub Container Registry
5. **Quality Gate** - Enforces quality thresholds and fails the build if not met

## Quality Gates

The pipeline enforces the following quality gates:

- **Code Coverage**: Minimum 80% instruction coverage and 75% branch coverage
- **Tests**: All tests must pass
- **Static Analysis**: SpotBugs and PMD checks must pass
- **Security**: No high/critical vulnerabilities (CVSS >= 7)
- **Build**: Application must compile successfully

## Workflows

### Main CI/CD Pipeline (`.github/workflows/ci-cd.yml`)

Triggered on:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches
- Manual dispatch

**Jobs:**
- `test` - Runs tests with Redis service and generates coverage reports
- `static-analysis` - Runs SpotBugs, PMD, and Checkstyle analysis
- `security-scan` - Performs OWASP dependency vulnerability scanning
- `build-and-push` - Builds and pushes Docker images (only on push to main/develop)
- `quality-gate` - Validates all quality gates

### Pipeline Validation (`.github/workflows/pipeline-test.yml`)

Triggered on:
- Workflow dispatch
- Changes to workflow files or pom.xml

Validates that the pipeline configuration is correct and all required tools are properly configured.

## Docker Image

The pipeline builds multi-stage Docker images with:
- Build stage using `eclipse-temurin:21-jdk`
- Runtime stage using `eclipse-temurin:21-jre`
- Non-root user for security
- Health check configuration
- Optimized JVM settings for containers

Images are pushed to GitHub Container Registry with tags:
- `latest` (for main branch)
- `<branch-name>` (for branch builds)
- `<branch-name>-<sha>` (for commit-specific builds)

## Security Features

- **Dependency Scanning**: OWASP Dependency Check with CVSS threshold of 7
- **Container Scanning**: Trivy vulnerability scanner for Docker images
- **Code Analysis**: SpotBugs static analysis for potential security issues
- **SARIF Integration**: Security results uploaded to GitHub Security tab

## Coverage Reporting

Code coverage is collected using JaCoCo and can be integrated with external services like Codecov. The pipeline:
- Generates HTML and XML coverage reports
- Enforces minimum coverage thresholds
- Uploads coverage data as artifacts
- Fails builds that don't meet coverage requirements

## Maven Plugins Configuration

The following plugins are configured in `pom.xml`:

- **JaCoCo** (`jacoco-maven-plugin`): Code coverage analysis
- **SpotBugs** (`spotbugs-maven-plugin`): Static analysis for bugs
- **PMD** (`maven-pmd-plugin`): Code quality analysis
- **Checkstyle** (`maven-checkstyle-plugin`): Code style checking
- **OWASP** (`dependency-check-maven`): Dependency vulnerability scanning

## Running Locally

To run the same checks locally:

```bash
# Full build with coverage
./mvnw clean verify -Pcoverage

# Static analysis
./mvnw clean compile spotbugs:check pmd:check

# Security scan
./mvnw org.owasp:dependency-check-maven:check

# Code style check
./mvnw checkstyle:check
```

## Pipeline Tests

The pipeline includes validation tests in `src/test/java/dev/bnacar/distributedratelimiter/pipeline/`:

- `PipelineValidationTest` - Validates pipeline configuration and file structure
- `DockerImageTest` - Tests Docker configuration and image requirements

These tests ensure that the CI/CD setup is correct and will work as expected.

## Troubleshooting

### Common Issues

1. **Coverage Threshold Failures**: Ensure new code has adequate test coverage
2. **Static Analysis Violations**: Fix SpotBugs or PMD reported issues
3. **Security Vulnerabilities**: Update dependencies or add suppressions to `owasp-suppressions.xml`
4. **Docker Build Failures**: Check Dockerfile syntax and build context

### Suppressing False Positives

Security scan false positives can be suppressed by adding entries to `owasp-suppressions.xml`:

```xml
<suppress>
    <notes>Explanation of why this is a false positive</notes>
    <packageUrl regex="true">^pkg:maven/group/artifact/.*$</packageUrl>
    <cve>CVE-YYYY-NNNNN</cve>
</suppress>
```

## Future Enhancements

Potential improvements to the pipeline:

- Integration with SonarQube for additional code quality metrics
- Deployment to staging/production environments
- Performance testing automation
- Integration with monitoring and alerting systems
- Advanced security scanning with additional tools