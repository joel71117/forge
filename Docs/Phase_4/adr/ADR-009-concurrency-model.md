# ADR-009: Concurrency Model

## Decision

Use ordinary Java concurrency primitives behind application boundaries. Prefer bounded executors, explicit ownership, deterministic tests, and domain state transitions.

## Rationale

The system must control capacity and understand happens-before behavior before adding distributed coordination. Concurrency remains an implementation detail unless the API contract requires asynchronous behavior.
