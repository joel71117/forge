# Distributed Systems Guide

## Architecture

PostgreSQL remains the source of truth for transactional business state. Kafka is the durable event log, and Redis is used for cache and short-lived coordination. Forge remains a modular monolith while multiple instances share these dependencies.

The local stack is started with `docker compose -f infrastructure/compose.yaml up -d`. Application clients use `localhost:5432`, `localhost:6379`, and `localhost:9092` when Forge runs on the host.

The implementation includes a versioned event envelope, a PostgreSQL outbox, `SKIP LOCKED` claiming, Kafka dispatch, processed-event deduplication, and opt-in Redis lock and rate-limit primitives. Job and order repositories still require durable adapters before the complete distributed workflow is production-ready.

## Kafka

Kafka is an append-only log. Topics contain partitions, and partitions contain ordered offsets. Consumer groups divide partitions among consumers. Ordering is guaranteed within one partition, not globally. Use product, order, customer, or job identifiers as keys when those aggregates must remain ordered.

Useful consumer parallelism cannot exceed partition count. A consumer crash before offset commit causes redelivery. Consumer lag measures produced work not yet processed. Local Kafka uses one KRaft broker and replication factor one; this is for learning, not availability.

## Transactional Outbox

A business transaction writes state and an `outbox_events` row in PostgreSQL. A scheduled dispatcher claims pending rows with `FOR UPDATE SKIP LOCKED`, publishes them to Kafka, and marks them published.

Claiming prevents concurrent dispatchers from selecting the same row at the same time. A crash after Kafka publish but before the database update can still publish a duplicate, so consumers must be idempotent. Published rows are retained for reconciliation and debugging.

The current legacy in-memory order and job repositories must be replaced with durable adapters before claiming full atomicity for those aggregates.

## Idempotency and Eventual Consistency

At-least-once delivery means a consumer may receive the same event more than once. `processed_events` uses `(event_id, consumer_name)` as a database primary key. `ProcessedEventStore` inserts this key with `ON CONFLICT DO NOTHING`, then performs the business operation in the same transaction.

HTTP idempotency keys must be stored with a request fingerprint and logical result in shared durable state. The same key and body returns the original result; the same key with a different body is a conflict. A check-then-insert sequence is unsafe under concurrent requests.

The write model commits first. Kafka then delivers an event to a projection consumer, which updates a read model later. During propagation, the read model can temporarily disagree with PostgreSQL. Projection updates must deduplicate event IDs and reject revisions older than the stored aggregate version. Replay deletes and rebuilds the projection from retained event history. Event envelopes carry `schemaVersion` so consumers can evolve deliberately.

## Redis Cache, Limits, and Locks

Redis is suitable for cache-aside reads, rate limits, and short-lived locks. PostgreSQL remains authoritative for orders, inventory, payments, and durable jobs.

Cache-aside reads Redis first, loads PostgreSQL on a miss, then writes Redis with a business-appropriate TTL. Writes commit PostgreSQL before invalidating the key. Redis failure policy must be explicit; core correctness must continue without trusting stale or missing cache data.

The fixed-window limiter is shared across instances through Redis `INCR` and `EXPIRE`. It is simple but has boundary bursts; sliding-window or token-bucket behavior can be added when accuracy requires it.

An in-process lock cannot coordinate multiple Forge instances. `RedisDistributedLock` uses a random owner token, a lease, and a compare-and-delete Lua script so one owner cannot release another owner’s lock.

A lease expiry does not stop a paused worker. Each acquisition also receives a fencing token; protected downstream state must reject operations carrying an older token. Inventory correctness remains a PostgreSQL transactional invariant, not a Redis-lock assumption.

## Retries and Resilience

Retry only transient failures. Use bounded exponential backoff with jitter, for example 1s, 2s, 4s, and 8s, with maximum attempts and a duration budget. Invalid payloads and business-rule failures go to a dead-letter topic instead of retrying forever.

Timeouts bound waiting, retries recover transient faults, circuit breakers stop load during outages, and bulkheads isolate dependency capacity. One layer must own retries to avoid multiplicative retry storms.

## Failure Policies

| Dependency | Failure policy |
|---|---|
| PostgreSQL | Fail core transactional requests; retry only transient connection failures within a bounded budget. |
| Kafka | Continue committed business work through the outbox; dispatch later. |
| Redis cache | Fall back to PostgreSQL where safe; never use Redis as business truth. |
| Redis rate limiter | Choose fail-open or fail-closed per endpoint and document the risk. |
| Provider | Keep notification work pending and retry with backoff; dead-letter permanent failures. |

Every policy needs a timeout, retry owner, maximum attempts, and an observable recovery signal.

The failure model includes process failures, lost or delayed network responses, independent dependency outages, duplicate or malformed messages, and partial success where the response is lost. The outbox protects database-to-Kafka intent; consumers still need deduplication because publishing after a crash can happen more than once.

## Observability

Propagate correlation ID, causation ID, event ID, and job ID from HTTP through the outbox and Kafka consumers. Record structured logs for publish, claim, retry, duplicate, and dead-letter decisions.

Useful low-cardinality metrics include outbox pending count, publish failures, event processing latency, duplicate count, DLT count, Redis hit ratio, rate-limit rejects, and circuit state. Do not use event IDs, request IDs, users, or jobs as metric label values.

Forge exposes the current outbox backlog as the Actuator metric `forge.outbox.pending`. Query it with `GET /actuator/metrics/forge.outbox.pending` while the local profile is running.

## Benchmarks

Results depend on host CPU, Docker resources, and database configuration. Record fresh measurements with the benchmark commands rather than treating sample values as universal.

| Scenario | Setup | Primary measurements |
|---|---|---|
| Inventory contention | 500 reservations, stock 100 | successes, invariant violations, latency |
| Outbox throughput | 1000 pending rows, 3 dispatchers | rows/sec, publish failures, recovery time |
| Cache stampede | 100 concurrent misses | database loads, cache hits, propagation delay |
| Rate limiting | requests split across A/B/C | accepted/rejected requests, Redis errors |
| Retry storm | failing provider, 100 requests | provider calls, queue depth, recovery time |

Compare baseline and mitigated runs under the same Docker resource limits. Do not publish event IDs, user IDs, or request IDs as metric labels.