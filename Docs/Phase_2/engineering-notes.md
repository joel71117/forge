# Phase 2 Engineering Notes

## Boundaries

HTTP controllers deserialize request DTOs and map responses. Application services coordinate repositories, invoke domain methods, and publish events. The domain remains plain Java and owns invariants such as `Order.cancel()` and `Inventory.reserve()`.

## Spring

`ForgeApplication` bootstraps component scanning and enables `ForgeProperties`. Constructor injection makes dependencies explicit and keeps services unit-testable. The main beans are singleton-scoped; mutable aggregate state is protected by the application service for the in-memory concurrency exercise.

## REST decisions

The API is versioned under `/api/v1`. Creation returns `201 Created`; missing resources return `404`; malformed requests return `400`; invalid state transitions and insufficient inventory return `409`. Cancellation is an operation endpoint so clients cannot assign arbitrary status values.

## Idempotency

Order, job, and notification creation require `Idempotency-Key`. The in-memory implementation synchronizes the check-and-create operation and replays the existing aggregate. This is process-local only: a restart or second application instance has a separate store.

## Observability

Actuator exposes health, info, and metrics, with liveness/readiness probes enabled. `CorrelationIdFilter` accepts or generates `X-Correlation-Id`, stores it in MDC, and returns it to the client. The console logging pattern includes the MDC value.

## Concurrency experiment

`InventoryReservationConcurrencyTest` runs 100 concurrent reservations against inventory of 10. The service serializes the compound read-modify-write operation, so successful reservations cannot exceed 10. `ConcurrentHashMap` alone would not make that compound operation atomic.

## Persistence limitation

Repositories are ports with in-memory adapters. Data disappears on restart, and separate processes do not share inventory or idempotency state. Phase 3 must replace adapters with durable persistence and define a database transaction around order creation, inventory reservation, and idempotency recording.