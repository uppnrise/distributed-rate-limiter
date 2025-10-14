# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records documenting key design decisions made during the development of the Distributed Rate Limiter.

## Index

- [ADR-001: Rate Limiting Algorithms](./001-token-bucket-algorithm.md)
- [ADR-002: Redis for Distributed State](./002-redis-distributed-state.md)
- [ADR-003: Fixed Window Counter Algorithm](./003-fixed-window-algorithm.md)
- [ADR-004: Spring Boot Framework](./004-spring-boot-framework.md)
- [ADR-005: RESTful API Design](./005-restful-api-design.md)
- [ADR-006: Configuration Management](./006-configuration-management.md)
- [ADR-007: Error Handling Strategy](./007-error-handling-strategy.md)

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