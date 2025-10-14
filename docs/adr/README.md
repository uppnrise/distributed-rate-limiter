# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records documenting key design decisions made during the development of the Distributed Rate Limiter.

## Index

- [ADR-001: Token Bucket Algorithm](./001-token-bucket-algorithm.md) - Primary rate limiting algorithm
- [ADR-002: Redis for Distributed State](./002-redis-distributed-state.md) - Distributed state management  
- [ADR-003: Fixed Window Counter Algorithm](./003-fixed-window-algorithm.md) - Memory-efficient alternative
- [ADR-004: Leaky Bucket Algorithm](./004-leaky-bucket-algorithm.md) - Traffic shaping specialization

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