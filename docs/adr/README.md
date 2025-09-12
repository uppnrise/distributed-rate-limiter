# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records documenting key design decisions made during the development of the Distributed Rate Limiter.

## Index

- [ADR-001: Token Bucket Algorithm](./001-token-bucket-algorithm.md)
- [ADR-002: Redis for Distributed State](./002-redis-distributed-state.md)
- [ADR-003: Spring Boot Framework](./003-spring-boot-framework.md)
- [ADR-004: RESTful API Design](./004-restful-api-design.md)
- [ADR-005: Configuration Management](./005-configuration-management.md)
- [ADR-006: Error Handling Strategy](./006-error-handling-strategy.md)

## ADR Template

Each ADR follows this structure:

```markdown
# ADR-XXX: [Title]

## Status
[Proposed | Accepted | Deprecated | Superseded]

## Context
What is the issue that we're seeing that is motivating this decision?

## Decision
What is the change that we're proposing or doing?

## Consequences
What becomes easier or more difficult to do because of this change?

## Alternatives Considered
What other options were evaluated?
```

## Guidelines

- ADRs are immutable once accepted
- New decisions that override previous ones should reference the superseded ADR
- Keep ADRs focused on architectural decisions, not implementation details
- Include trade-offs and reasoning behind decisions