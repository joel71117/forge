# Architecture Decisions

This document combines ADR-013 through ADR-023. Each section records the context, decision, alternatives, tradeoffs, and consequences for one distributed-systems decision.

## ADR-013: Kafka Transport

### Context
Forge needs durable asynchronous transport with partitioned consumption.

### Decision
Use Kafka topics with business aggregate keys and consumer groups.

### Alternatives
A database queue would simplify operations but loses Kafka replay and partition semantics.

### Tradeoffs
Kafka adds operational complexity and asynchronous consistency.

### Consequences
Consumers must tolerate duplicates, lag, rebalances, and replay.

## ADR-014: Transactional Outbox

### Context
A business transaction and Kafka publish cannot share one atomic transaction reliably.

### Decision
Persist event intent in PostgreSQL and publish it asynchronously.

### Alternatives
Direct publish risks lost events; distributed transactions add coupling.

### Tradeoffs
Events may be published more than once and require deduplication.

### Consequences
PostgreSQL remains authoritative and outbox rows are retained for replay.

## ADR-015: Idempotent Consumers

### Context
Kafka delivery is at least once around database transactions.

### Decision
Record `(event_id, consumer_name)` in PostgreSQL before applying effects.

### Alternatives
Consumer-side memory is lost on restart and does not coordinate instances.

### Tradeoffs
The processed-event table requires retention and operational cleanup policy.

### Consequences
Duplicate deliveries become no-op business effects.

## ADR-016: Kafka Partition Keys

### Context
Ordering is guaranteed only within a partition.

### Decision
Use aggregate IDs as keys; inventory changes should use product IDs.

### Alternatives
Random keys maximize distribution but destroy business ordering.

### Tradeoffs
A hot aggregate can create partition imbalance.

### Consequences
Consumers can parallelize independent aggregates while preserving local order.

## ADR-017: Redis Cache-Aside

### Context
Product reads need lower latency without making Redis authoritative.

### Decision
Read cache first, load PostgreSQL on miss, cache with a bounded TTL, and evict after writes.

### Alternatives
Read-through caching hides ownership; no cache increases database load.

### Tradeoffs
Stale reads and invalidation races remain possible.

### Consequences
Redis outage fails open to PostgreSQL and core state remains durable.

## ADR-018: Distributed Rate Limiting

### Context
Limits must apply across Forge instances.

### Decision
Use a Redis fixed-window counter scoped by client address and route.

### Alternatives
Local counters are not shared; sliding windows cost more Redis operations.

### Tradeoffs
Fixed windows permit boundary bursts. Redis failure currently fails open.

### Consequences
Limits are configurable through application properties.

## ADR-019: Distributed Locks

### Context
Scheduled ownership cannot use JVM locks across instances.

### Decision
Use Redis leases with random owner tokens and fencing counters.

### Alternatives
Database advisory locks are durable but less suitable for short coordination leases.

### Tradeoffs
Lease expiry can leave stale workers; protected writes must validate fencing tokens.

### Consequences
Lock release is owner-checked and scheduled state rejects older fencing tokens.

## ADR-020: Eventual Consistency

### Context
Read projections update after the transactional write model.

### Decision
Build order summaries from retained outbox events and ignore older aggregate versions.

### Alternatives
Synchronous reads are strongly consistent but couple consumers to the write model.

### Tradeoffs
Clients may observe propagation delay.

### Consequences
Projection replay can rebuild the read model from event history.

## ADR-021: Retry and DLT Strategy

### Context
Transient provider failures need bounded recovery; poison messages must stop retrying.

### Decision
Use bounded exponential Kafka retry topics and deliberate DLT replay.

### Alternatives
Infinite retries preserve eventual attempts but create retry storms.

### Tradeoffs
Operators must inspect and deliberately replay failed payloads.

### Consequences
Replay returns payloads to the normal topic and can be audited externally.

## ADR-022: Provider Resilience

### Context
Notification providers can be slow, unavailable, or overloaded.

### Decision
Compose timeout, bulkhead, circuit breaker, and bounded retry ownership around providers.

### Alternatives
Retries alone amplify outages; no isolation lets one provider consume all workers.

### Tradeoffs
Open circuits temporarily reject work and require recovery tuning.

### Consequences
Provider failures become durable notification failures and Kafka can retry delivery.

## ADR-023: Distributed Observability

### Context
Asynchronous failures cannot be diagnosed from one request log.

### Decision
Propagate correlation IDs and expose outbox, job, and application health metrics.

### Alternatives
Logs alone are difficult to aggregate and quantify.

### Tradeoffs
Metrics must avoid high-cardinality event, request, and user labels.

### Consequences
Kafka lag and provider-specific telemetry remain deployment-level follow-up work.
