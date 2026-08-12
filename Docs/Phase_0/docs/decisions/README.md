# Architecture Decision Records

Use ADRs for significant architectural decisions.

Template:

``` text
# ADR-XXX: Decision title

## Status
Proposed | Accepted | Superseded

## Context
What problem or requirement caused the decision?

## Decision
What was chosen?

## Alternatives
What other approaches were considered?

## Rationale
Why?

## Consequences
What becomes easier and harder?

## Revisit Conditions
When should this decision be reconsidered?
```

Potential ADRs:

``` text
ADR-001-modular-monolith
ADR-002-postgresql
ADR-003-inventory-concurrency-strategy
ADR-004-redis
ADR-005-kafka
ADR-006-outbox-pattern
ADR-007-idempotency-strategy
ADR-008-job-ownership
ADR-009-provider-failover
ADR-010-observability
ADR-011-service-decomposition
```

Do not create an ADR simply because a technology is popular. Record a
decision when an engineering problem has actually been investigated.
