# Forge Non-Functional Requirements

Initial numbers are engineering targets for repeatable experiments, not
production guarantees.

## Performance

-   **NFR-PERF-001:** Target p95 \< 500 ms for normal synchronous APIs
    under the initial benchmark.
-   **NFR-PERF-002:** Measure p50, p95 and p99.
-   **NFR-PERF-003:** Async submission APIs must not wait for actual
    execution.
-   **NFR-PERF-004:** Queues and worker resources must be bounded.

## Scalability

-   **NFR-SCALE-001:** Stateless API instances must scale horizontally.
-   **NFR-SCALE-002:** Job workers must scale horizontally.
-   **NFR-SCALE-003:** Notification workers must scale horizontally.
-   **NFR-SCALE-004:** Kafka consumers/partitions must scale
    independently where appropriate.
-   **NFR-SCALE-005:** Final system-design target: 100 million jobs/day.

## Availability

-   **NFR-AVAIL-001:** A worker failure must not silently lose accepted
    work.
-   **NFR-AVAIL-002:** Provider failure should not necessarily take down
    notifications.
-   **NFR-AVAIL-003:** API instances should be independently
    replaceable.
-   **NFR-AVAIL-004:** Prefer graceful degradation to cascading failure.

## Reliability

-   **NFR-REL-001:** Accepted jobs are durable.
-   **NFR-REL-002:** Duplicate messages do not corrupt business state.
-   **NFR-REL-003:** Retry policies distinguish transient/permanent
    failure.
-   **NFR-REL-004:** Recovery mechanisms are idempotent.
-   **NFR-REL-005:** Business invariants survive failures.

## Consistency

-   **NFR-CONS-001:** Inventory correctness requires strong protection
    against overselling.
-   **NFR-CONS-002:** Payment correctness must prevent accidental
    duplicate charges.
-   **NFR-CONS-003:** Notifications may be eventually consistent with
    order state.
-   **NFR-CONS-004:** Metrics/analytics may be eventually consistent.
-   **NFR-CONS-005:** Consistency choices must be documented per
    workflow.

## Observability

-   **NFR-OBS-001:** Health endpoints exist.
-   **NFR-OBS-002:** Technical and business metrics exist.
-   **NFR-OBS-003:** Logs are structured.
-   **NFR-OBS-004:** Requests have correlation IDs.
-   **NFR-OBS-005:** Async workflows propagate trace context where
    appropriate.
-   **NFR-OBS-006:** Distributed tracing is supported.
-   **NFR-OBS-007:** Important queues expose depth, age, throughput, lag
    and failure metrics.

## Security

-   Authentication and authorization are enforced server-side.
-   Secrets are externalized.
-   Sensitive data is excluded from logs.
-   Administrative APIs are separately authorized.
-   Public/high-risk APIs support rate limiting.

## Maintainability

-   Modules have explicit responsibilities.
-   Business logic is isolated from infrastructure details.
-   New notification channels do not require rewriting existing
    channels.
-   New job types do not require an ever-growing conditional dispatcher.
-   Important architectural choices are recorded as ADRs.

## Testability

-   Domain logic is unit-testable without infrastructure.
-   Infrastructure-sensitive behavior uses realistic integration tests.
-   Concurrency behavior is testable under controlled load.
-   Failure modes are reproducible.
-   End-to-end workflows are automated.

## Recoverability

-   Jobs survive worker crashes.
-   Scheduled work survives application restarts.
-   Orphaned reservations/jobs are detectable.
-   UNKNOWN payments can be reconciled.

## Fairness

-   One customer cannot monopolize workers.
-   Low-priority work cannot be permanently starved.
-   Per-customer workload is measurable.

## Deployment

-   Local infrastructure is reproducible with containers.
-   Configuration is externalized.
-   Readiness/liveness are exposed.
-   Instances support rolling replacement.

## Initial measurable targets

``` text
API p95 < 500 ms
Normal error rate < 1%
No accepted job silently lost
No successful inventory oversell
No unbounded in-memory work queue
No secrets in logs
Duplicate events produce one logical business effect
```

All claims must eventually be measured rather than assumed.
