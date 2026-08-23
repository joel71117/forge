# ADR-010: In-Process Job Executor

## Decision

Use one bounded `ThreadPoolExecutor` and a handler registry inside each JVM during Phase 4.

## Consequences

Jobs have local ownership and named workers, retries, backpressure, and graceful shutdown. A queue is not shared between application instances, so durable distributed ownership is intentionally deferred to Phase 5.
