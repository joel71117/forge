# ADR-011: Worker Backpressure

## Decision

Use a bounded `ArrayBlockingQueue` and reject submissions when worker and queue capacity are exhausted.

## Rationale

Silent unlimited buffering converts overload into memory pressure. Rejection makes capacity visible to callers and keeps the overload policy explicit.
