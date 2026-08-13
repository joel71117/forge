# Forge Glossary

**Aggregate** --- Consistency boundary containing entities and
invariants that must be protected together.

**At-least-once delivery** --- A message may be delivered one or more
times; consumers tolerate duplicates.

**Backpressure** --- Preventing producers from overwhelming downstream
capacity.

**Circuit breaker** --- Temporarily stops calls to a failing dependency
to prevent cascading failure.

**Consumer** --- Component that reads messages.

**Consumer group** --- Kafka consumers collectively processing
partitions of a topic.

**Correlation ID** --- Identifier connecting logs and operations for one
logical workflow.

**Dead-letter state/queue** --- Destination/state for work that cannot
be processed successfully after defined attempts or because failure is
permanent.

**Domain event** --- Immutable record describing a meaningful business
event that occurred.

**Eventual consistency** --- Different components may temporarily
disagree but converge through asynchronous propagation.

**Idempotency** --- Repeating the same logical operation produces the
same intended business effect as one execution.

**Idempotency key** --- Identifier used to recognize repeated attempts
of one logical operation.

**Inventory reservation** --- Temporary claim over inventory associated
with an order.

**Job** --- Unit of asynchronous work.

**Job attempt** --- One concrete execution attempt for a job.

**Lease** --- Time-bounded ownership of work that expires if not
renewed.

**MVCC** --- Multi-Version Concurrency Control, a database mechanism for
handling concurrent data versions.

**Optimistic locking** --- Update verifies data has not changed before
committing.

**Pessimistic locking** --- Explicitly locks data to prevent conflicting
operations.

**Provider** --- External system/implementation delivering payment,
email, SMS or push capability.

**Retry** --- Subsequent attempt after failure.

**Exponential backoff** --- Retry delay generally increases after each
failure.

**Jitter** --- Random variation added to retry delays to avoid
synchronized retry storms.

**Bulkhead** --- Isolates resource pools so one failure does not consume
all capacity.

**Rate limiting** --- Restricting request/operation frequency.

**Fair scheduling** --- Scheduling designed to prevent one workload from
monopolizing shared capacity.

**Distributed lock** --- Mechanism coordinating ownership across
distributed participants.

**Outbox** --- Pattern recording business state and an event durably
together before asynchronous publication.

**Inbox/processed-event record** --- Durable record of consumed event
IDs for duplicate detection.

**Readiness probe** --- Indicates whether an instance is ready for
traffic.

**Liveness probe** --- Indicates whether an instance is alive and should
remain running.

**P95/P99** --- Latency percentiles showing tail behavior.

**Graceful shutdown** --- Stop accepting work and safely finish/release
existing work.

**Orphaned work** --- Work marked active whose owner is no longer
available.

**Unknown outcome** --- System cannot determine whether an external
operation succeeded.

**Compensating action** --- Business operation that semantically offsets
a completed operation in a distributed workflow.

**Saga** --- Distributed workflow composed of local transactions plus
compensating actions.
