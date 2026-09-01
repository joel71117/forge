# Forge Commerce Phase 5 Implementation Report

**Phase:** 5 - Distributed Systems, Kafka, Redis, Distributed Job Processing, and Eventual Consistency  
**Status:** Core implementation complete and validated  
**Report date:** 2026-08-24  
**Platform:** Spring Boot 4.1.0, Java 21, Maven, PostgreSQL 17, Redis 7.4, Kafka 3.9 KRaft

## 1. Executive Summary

Phase 5 transforms Forge from a primarily single-process application into a distributed modular monolith that can run as multiple Forge instances. PostgreSQL remains the authoritative store for transactional business state. Kafka provides durable asynchronous event transport and consumer-group processing. Redis provides cache and coordination capabilities, but is not used as the source of truth for orders, inventory, payments, or durable jobs.

The implementation includes:

- Durable transactional outbox processing.
- Kafka event publication, consumers, retry topics, and DLT handling.
- At-least-once processing with PostgreSQL-backed consumer deduplication.
- PostgreSQL-backed HTTP idempotency for duplicate requests.
- Distributed job persistence and execution across Forge instances.
- Redis cache-aside, invalidation, stampede protection, rate limiting, and locks.
- Owner-token locks and fencing-token protection for scheduled work.
- Asynchronous notification creation and delivery.
- Provider timeout, bulkhead, circuit-breaker, retry, and jitter controls.
- Event-driven order projections and replay.
- Correlation IDs and Micrometer operational metrics.
- Reproducible Docker Compose infrastructure for PostgreSQL, Redis, Kafka, and Forge A/B/C.
- Kafka laboratory and distributed failure experiment tooling.

## 2. Architecture Delivered

```text
Clients
   |
   +--> Forge A
   +--> Forge B
   +--> Forge C
          |
          +--> PostgreSQL: authoritative state, outbox, idempotency, deduplication
          +--> Redis: cache, rate limits, locks, fencing counters
          +--> Kafka: events, jobs, retry topics, DLTs, projections
```

The application remains a modular monolith. Multiple identical Forge processes share PostgreSQL, Redis, and Kafka rather than duplicating business logic across separate services.

### Responsibility boundaries

| Component | Responsibility |
|---|---|
| PostgreSQL | Orders, inventory, notifications, jobs, outbox rows, processed events, idempotency records, projections, notification attempts, scheduled ownership state |
| Kafka | Durable event transport, partitions, offsets, consumer groups, retry topics, dead-letter topics |
| Redis | Cache-aside data, rate-limit counters, short-lived lock leases, fencing counters, stampede coordination |
| Forge | Domain rules, transactional orchestration, event production, event consumers, projections, job handlers |

## 3. Implemented Changes

### 3.1 Event envelope and event publication

Implemented a common event envelope containing transport metadata and business payload fields, including:

- Globally unique event ID.
- Event type.
- Schema version.
- Occurrence timestamp.
- Producer.
- Correlation and causation identifiers.
- Aggregate type and aggregate ID.
- Business payload.

The application supports both an in-memory event publisher for non-distributed profiles and Kafka-backed publication for distributed operation. An outbox publisher allows the business transaction and event intent to commit together before Kafka publication occurs.

### 3.2 Transactional outbox

Implemented durable outbox processing with:

- PostgreSQL `outbox_events` storage.
- `PENDING`, `PUBLISHED`, and `FAILED` lifecycle states.
- Attempt counts, next-attempt timestamps, last-error storage, and published timestamps.
- Row claiming with `FOR UPDATE SKIP LOCKED`.
- Claim leases for recovering rows left in a processing state after a process failure.
- Multiple dispatcher instances safely working on different rows.
- Retention of published rows for investigation and reconciliation.
- Bounded retry delay with jitter.

Relevant implementation areas include `OutboxEventStore`, `OutboxEventPublisher`, `OutboxDispatcher`, and the V2/V4 database migrations.

### 3.3 Kafka consumers, retries, and DLTs

Implemented Kafka consumers for jobs, notifications, and order projections. Consumers use retry topics and dead-letter handling through Spring Kafka.

Retry behavior is configurable through:

```properties
forge.kafka.retry.attempts=4
forge.kafka.retry.delay-ms=1000
forge.kafka.retry.multiplier=2.0
forge.kafka.retry.max-delay-ms=8000
forge.kafka.retry.jitter-ms=250
```

The retry policy provides exponential backoff, maximum attempts, maximum delay, and jitter to reduce synchronized retry storms. DLT handlers preserve failed processing as an operational condition instead of retrying permanently invalid work forever.

The administrative DLT replay endpoint requires:

- A non-empty DLT payload.
- `X-Admin-Actor` request metadata.
- `X-Replay-Reason` request metadata.

Replay actions are logged with actor, reason, destination, and payload length. The payload itself is not written to the audit log.

### 3.4 At-least-once processing and event deduplication

Implemented PostgreSQL-backed processed-event deduplication using a unique `(event_id, consumer_name)` constraint. Each consumer records its processing claim and performs its business operation through the same transactional boundary.

This protects against duplicate deliveries caused by retries, consumer restarts, partition rebalancing, or a crash between business processing and offset acknowledgement.

### 3.5 HTTP idempotency

Implemented shared HTTP idempotency for durable operations such as order creation, job submission, and notification creation.

The implementation:

- Requires an idempotency key for configured operations.
- Stores request fingerprints using SHA-256.
- Returns the existing logical result for the same key and same request.
- Rejects reuse of a key with a different request body.
- Models processing, completed, and failed states.
- Uses database uniqueness rather than a check-then-insert race-prone pattern.

### 3.6 Distributed job processing

Moved durable job submission away from JVM-only coordination and into the distributed path:

```text
Job API -> PostgreSQL job row -> outbox event -> Kafka -> consumer group -> JobHandler
```

Implemented or updated:

- JDBC job persistence.
- Job lifecycle state handling.
- Durable job attempts.
- Retry and failure state handling.
- Kafka `JobSubmitted` consumer.
- Consumer deduplication.
- Multiple Forge instances sharing job work through Kafka consumer groups.
- Existing executor backpressure and retry behavior.

### 3.7 Notification pipeline

Implemented the asynchronous notification pipeline:

```text
OrderConfirmed
   -> OrderConfirmedNotificationConsumer
   -> NotificationCreated
   -> NotificationCreatedConsumer
   -> NotificationDeliveryService
   -> resilient provider adapter
```

Notification delivery now persists attempt records with:

- Provider name.
- Attempt number.
- Start and finish timestamps.
- Processing status.
- Provider reference.
- Error code and message.

The V8 migration creates `notification_attempts`, and both JDBC and in-memory repositories support attempt persistence.

### 3.8 Resilience controls

Implemented provider protection using:

- Timeout enforcement through a cancellable executor call.
- Bulkhead concurrency limits.
- Circuit breaker states: closed, open, and half-open.
- Configurable Kafka retry budgets and jitter.
- Failure status persistence before the failure is rethrown.

The provider adapter is intentionally a simulated provider for the learning environment, but its resilience boundary is structured for replacement with an external provider client.

### 3.9 Redis cache-aside and invalidation

Implemented Redis cache-aside behavior with:

- Configurable positive TTL.
- Cache hit deserialization.
- Loader fallback on cache miss.
- Database/application loader integration point.
- Explicit cache eviction.
- Distributed load lock for stampede reduction.
- Owner-token Lua release so one process cannot delete another process's load lock.
- Fallback to the loader when Redis is unavailable.

Redis is not used as authoritative business storage.

### 3.10 Distributed rate limiting

Implemented a Redis-backed fixed-window rate limiter with:

- Shared counters across Forge A/B/C.
- Configurable request limit.
- Configurable window duration.
- `X-RateLimit-Limit` and `X-RateLimit-Remaining` headers.
- `Retry-After` on rejection.
- HTTP 429 responses after the configured limit.
- Fail-open behavior when Redis is unavailable.

The interceptor is conditional, so applications can start successfully when rate limiting is disabled.

### 3.11 Distributed locks and fencing tokens

Implemented Redis lease-based locks with:

- Lock key.
- Random owner token.
- Lease duration.
- Redis fencing counter.
- Owner-checked Lua release.

Implemented fenced scheduled ownership for the shared `job-maintenance` task. PostgreSQL accepts a scheduled ownership update only when the presented fencing token is newer than the stored token. This prevents a stale worker from mutating protected state after its Redis lease has expired and another worker has taken ownership.

### 3.12 Eventual consistency and projections

Implemented an asynchronous order summary projection:

```text
Order event -> Kafka -> OrderProjectionConsumer -> order_summary
```

The projection includes order ID, customer, status, total, item count, aggregate version, last event ID, and update timestamp.

Implemented:

- Event-driven projection updates.
- Aggregate-version checks to prevent stale events overwriting newer state.
- Projection replay administration.
- Rebuilding the projection from retained event history.

### 3.13 Inventory events

Implemented inventory reservation lifecycle events:

- `InventoryReserved`.
- `InventoryReleased`.
- `InventoryConsumed`.

Inventory correctness continues to rely on PostgreSQL transactional and locking behavior. Inventory events decouple downstream audit, analytics, and order-state consumers from the inventory application service.

The original Phase 5 planning document also names `InventoryAdjusted`. That event is not currently implemented; this is the only notable strict plan-compliance gap in the event list and should be added if that exact event contract is required.

### 3.14 Correlation and observability

Implemented correlation ID propagation through the HTTP filter and logging pattern. Logs include the correlation ID where available.

Implemented Micrometer metrics for:

- Pending outbox events.
- Job queue depth.
- Submitted jobs.
- Failed jobs.
- Kafka consumer lag by topic and consumer group.

Kafka lag polling uses the Kafka `AdminClient` and preserves the last known value when Kafka is unavailable. Metrics are enabled conditionally with Kafka consumer operation.

## 4. Infrastructure and Deployment Changes

### Docker Compose files

Implemented reproducible infrastructure for:

- PostgreSQL 17.
- Redis 7.4 Alpine with AOF enabled.
- Kafka 3.9.0 in KRaft mode.
- Forge A on port 8081.
- Forge B on port 8082.
- Forge C on port 8083.

The distributed Compose profile configures all Forge instances to share the same PostgreSQL, Redis, and Kafka services. Health checks and dependency ordering are included.

### Database migrations

Phase 5 migrations include:

| Migration | Purpose |
|---|---|
| V2 | Distributed messaging tables |
| V3 | Durable jobs table |
| V4 | Outbox claim lease support |
| V5 | Idempotency and order projection tables |
| V6 | Orders and notifications tables |
| V7 | Scheduled task ownership and fencing state |
| V8 | Notification attempts |

### Configuration and packaging

Added or updated:

- Kafka and Redis feature flags.
- Local profile dependency configuration.
- Retry and jitter properties.
- Observability polling interval.
- Scheduled ownership interval and lease.
- Redis cache TTL.
- Rate-limit window and limit.
- Dockerfile for Forge instance images.
- Kafka and Redis Compose support files.

## 5. Tests and Experiments Added

### 5.1 Standard automated tests

The normal Maven suite covers the existing domain and application behavior plus new Phase 5 unit tests, including:

- Resilient notification provider success and failure.
- Provider timeout behavior.
- Bulkhead rejection.
- Circuit breaker opening.
- Notification attempt domain behavior.
- Redis cache hit behavior.
- Redis cache eviction.
- Redis rate-limit enforcement.
- Fenced scheduled task database predicate.
- Existing inventory, order, job, controller, and concurrency tests.

### 5.2 PostgreSQL distributed integration tests

`DistributedConcurrencyIT` is gated behind `forge.integration-tests=true` and runs against the local PostgreSQL instance. It validates distributed race behavior, including:

- Concurrent inventory reservation correctness.
- Shared idempotency behavior.

The test run completed with 2 tests, 0 failures, and 0 errors. PostgreSQL 17.11 was available during validation, and Flyway applied migrations through V8.

### 5.3 Existing database experiments

The repository also contains gated experiments for:

- PostgreSQL inventory contention.
- Hibernate optimistic locking.
- Hikari connection-pool saturation.
- Pessimistic, optimistic, atomic, and naive update strategies.

These tests remain opt-in because they require live PostgreSQL resources and are intended to produce measurements rather than run on every build.

### 5.4 Kafka laboratory harness

Added `KafkaExperimentTest`, gated behind `forge.kafka.tests=true`.

The harness:

1. Creates a unique four-partition Kafka topic.
2. Produces 1,000 keyed events.
3. Records partition distribution.
4. Starts two consumers in one consumer group.
5. Observes assignment and rebalance callbacks.
6. Measures initial and draining lag.
7. Consumes all 1,000 events.
8. Deletes the temporary topic during cleanup.

The live Kafka run completed with 1 test, 0 failures, and 0 errors against the Compose broker.

### 5.5 Distributed failure experiments

Added `infrastructure/run-failure-experiments.sh`. The runner builds and starts the distributed Compose stack, checks all three Forge instances, then exercises:

- Forge B stop and restart.
- Redis stop and restart.
- Kafka stop and restart.

It records timestamped results in `docs/incident-reports.md`, including health responses, pass/fail outcomes, and final container status.

The recorded run on 2026-08-23 passed all checks:

- Forge A and Forge C remained healthy while Forge B was stopped.
- Forge B recovered after restart.
- The application health endpoint remained available during Redis outage.
- The application recovered after Redis restart.
- The application health endpoint remained available during Kafka outage.
- The application recovered after Kafka restart.

### 5.6 Documentation and operational analysis

Added or completed:

- Distributed architecture documentation.
- Distributed failure model.
- Cache, idempotency, locking, outbox, retry, and eventual-consistency notes.
- Distributed failure matrix.
- Distributed observability notes.
- Distributed benchmark scenarios.
- ADR-013 through ADR-023.
- Five incident reports covering Kafka outage, duplicate delivery, poison messages and DLT, Redis outage, and provider timeout.
- Timestamped measured failure-experiment output.

## 6. Validation Commands

### Compile

```bash
./mvnw -q -DskipTests compile
```

### Normal test suite

```bash
./mvnw -q test
```

### PostgreSQL distributed integration test

```bash
./mvnw -q -Dforge.integration-tests=true -Dtest=DistributedConcurrencyIT test
```

### Kafka experiment

Start Kafka, then run:

```bash
./mvnw -q -Dforge.kafka.tests=true -Dtest=KafkaExperimentTest test
```

### Compose validation

```bash
docker compose -f infrastructure/distributed-compose.yaml config -q
```

### Failure experiments

```bash
infrastructure/run-failure-experiments.sh
```

## 7. Validation Results

| Validation | Result |
|---|---|
| Java compilation | Passed |
| Static diagnostics on changed files | Passed |
| Normal Maven test suite | Passed |
| Focused notification tests | 5 passed |
| Focused Redis and fencing tests | 4 passed |
| PostgreSQL distributed integration test | 2 passed |
| Kafka experiment against live broker | 1 passed |
| Distributed Compose syntax validation | Passed |
| Three Forge instance startup | Passed |
| Forge instance outage/recovery | Passed |
| Redis outage/recovery | Passed |
| Kafka outage/recovery | Passed |

The test output includes expected Mockito dynamic-agent warnings from the current test setup. These warnings did not cause failures.

## 8. Final Status and Known Caveats

### Complete

The core distributed production path, infrastructure, resilience behavior, operational controls, tests, Kafka experiment harness, and live failure experiment runner are implemented and validated.

### Known caveats

1. `InventoryAdjusted` is named in the original Phase 5 plan but is not currently published by the inventory service. `InventoryReleased` and `InventoryConsumed` are implemented.
2. The notification provider is a simulated adapter. Timeout, bulkhead, circuit-breaker, and persistence behavior are real, but no external provider integration is configured.
3. The Redis rate limiter uses a fixed-window counter. Sliding-window and token-bucket alternatives are documented as design comparisons rather than separate implementations.
4. The Kafka laboratory uses a four-partition temporary topic to make partition distribution observable. The production learning topic configuration remains separately configurable.
5. The failure runner validates health and recovery. It does not automatically assert every business-level invariant, such as an outbox row's exact attempt history, for every injected outage.
6. Kafka consumer lag metrics preserve the last known value during AdminClient failures; alert thresholds and an external metrics backend are deployment concerns.

## 9. Conclusion

Phase 5 successfully delivers a reproducible distributed Forge environment and the application foundations required for at-least-once event processing, durable asynchronous work, idempotency, Redis coordination, eventual consistency, resilience, and partial-failure recovery.

The implementation is ready for continued hardening or Phase 6 work. The only strict event-level requirement from the original Phase 5 plan that remains to be implemented is `InventoryAdjusted`; all other tracked Phase 5 implementation and validation items have evidence in code, tests, infrastructure, or recorded experiment output.
