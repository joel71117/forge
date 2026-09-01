# Forge --- Phase 5 Implementation

## Distributed Systems, Kafka, Redis, Distributed Job Processing & Eventual Consistency

**Objective:** Transform Forge from a strong single-JVM application into
a distributed system with multiple application instances, durable event
processing, Redis-backed coordination, distributed job execution,
idempotency, retries, dead-letter handling, rate limiting, caching, and
eventual consistency.

The central mental model is:

> In a distributed system, you cannot assume that another process
> received your message, that a response means the operation happened
> exactly once, or that every node observes state at the same time.

------------------------------------------------------------------------

# 1. Phase 5 Goals

By the end of this phase you should understand and implement:

-   Kafka brokers, topics, partitions, offsets and consumer groups
-   consumer rebalancing and lag
-   at-least-once processing
-   idempotent consumers
-   transactional outbox
-   distributed job processing
-   retry topics and dead-letter topics
-   exponential backoff and jitter
-   Redis cache-aside
-   cache invalidation and stampede prevention
-   distributed rate limiting
-   distributed locks and ownership
-   fencing tokens
-   eventual consistency
-   event-driven projections
-   event replay
-   event versioning
-   circuit breakers
-   bulkheads
-   timeout and retry budgets
-   multi-instance inventory correctness
-   distributed observability
-   partial failure and recovery

------------------------------------------------------------------------

# 2. Architecture

Phase 4:

``` text
HTTP
 ↓
Spring Boot
 ↓
ThreadPoolExecutor
 ↓
PostgreSQL
```

Phase 5:

``` text
                         Clients
                            │
                            ▼
                    Load Balancer
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
          Forge A       Forge B       Forge C
              │             │             │
              ├─────────────┼─────────────┤
              │             │             │
              ▼             ▼             ▼
          PostgreSQL      Redis          Kafka
              │             │             │
              │             │      ┌──────┼────────┐
              │             │      ▼      ▼        ▼
              │             │    Jobs Notifications Events
              │             │      │      │        │
              │             │      └──┬───┴────────┘
              │             │         ▼
              │             │    Consumer Groups
              │             │
              └── Outbox ──┘
```

Keep Forge as a modular monolith initially. Run multiple instances
rather than immediately creating many repositories and services.

Responsibility boundaries:

``` text
PostgreSQL → authoritative transactional business state
Kafka      → durable asynchronous event transport/log
Redis      → low-latency cache and coordination state
Forge      → business logic/orchestration
```

------------------------------------------------------------------------

# 3. Distributed Failure Model

Create:

``` text
docs/distributed-systems.md
```

Document:

### Process failure

``` text
instance crashes
```

### Network failure

``` text
request lost
response lost
timeout
connection reset
```

### Dependency failure

``` text
PostgreSQL unavailable
Redis unavailable
Kafka unavailable
provider unavailable
```

### Message failure

``` text
duplicate
delayed
reordered
poison message
```

### Partial failure

``` text
operation succeeded
but response was lost
```

This last case must become part of your normal engineering reasoning.

------------------------------------------------------------------------

# 4. Reproducible Local Infrastructure

Use Docker Compose or equivalent to start:

``` text
PostgreSQL
Redis
Kafka
Forge instance A
Forge instance B
Forge instance C
```

The entire environment should be reproducible with one command.

Do not depend on manually installed infrastructure.

------------------------------------------------------------------------

# 5. Kafka Fundamentals

Learn:

``` text
broker
topic
partition
offset
producer
consumer
consumer group
leader
replica
retention
rebalance
```

Do not think of Kafka as simply a queue.

Understand it as a distributed append-only log where consumers maintain
progress.

------------------------------------------------------------------------

# 6. Kafka Laboratory

Create:

``` text
forge.learning.events
```

with:

``` text
3 partitions
```

Produce at least:

``` text
1000 events
```

Record:

``` text
event ID
key
partition
offset
timestamp
```

Restart consumers and observe where consumption resumes.

------------------------------------------------------------------------

# 7. Partition Ordering

Produce events using keys such as:

``` text
customer-1
customer-2
customer-3
```

Demonstrate:

``` text
same key
→ same partition
→ ordered processing within that partition
```

Then demonstrate:

``` text
different partitions
→ no global ordering guarantee
```

This should influence Forge partition-key decisions.

------------------------------------------------------------------------

# 8. Consumer Groups

Run:

``` text
3 consumers
3 partitions
```

using one consumer group.

Then run:

``` text
4 consumers
3 partitions
```

Observe that the extra consumer has no partition to process.

Learn:

> Useful Kafka consumer parallelism is bounded by partition parallelism.

------------------------------------------------------------------------

# 9. Rebalancing

Start three consumers.

Then:

``` text
kill one
restart one
add another
```

Observe:

``` text
rebalance
partition reassignment
temporary pause
possible duplicate processing
```

Document what happens to in-flight work.

------------------------------------------------------------------------

# 10. Offset Experiments

Test these sequences:

### Case A

``` text
consume
DB update
crash
commit offset
```

### Case B

``` text
consume
commit offset
DB update
crash
```

Determine what can happen after restart.

This demonstrates why:

``` text
Kafka offset
+
PostgreSQL transaction
```

are not automatically one atomic operation.

------------------------------------------------------------------------

# 11. At-Least-Once Processing

Design every Forge Kafka consumer assuming:

``` text
the same event can arrive more than once
```

Therefore:

``` text
consumer processing must be idempotent
```

Do not attempt to solve duplicates purely through consumer
configuration.

------------------------------------------------------------------------

# 12. Transactional Outbox

Implement:

``` text
business transaction
       │
       ├── business state
       │
       └── outbox_event
                │
                ▼
              COMMIT
                │
                ▼
         outbox publisher
                │
                ▼
              Kafka
```

The key property is:

> Business state and the intent to publish the event are committed in
> the same PostgreSQL transaction.

------------------------------------------------------------------------

# 13. Outbox Schema

Create:

``` text
outbox_events
-------------
id
aggregate_type
aggregate_id
event_type
schema_version
payload
created_at
published_at
attempt_count
next_attempt_at
status
last_error
```

Initial states:

``` text
PENDING
PUBLISHED
FAILED
```

Do not immediately delete published records. Retain enough information
for debugging and reconciliation.

------------------------------------------------------------------------

# 14. Outbox Publisher

Implement:

``` text
find pending events
 ↓
claim rows
 ↓
publish to Kafka
 ↓
mark published
```

Run the publisher on all Forge instances.

Prevent multiple instances from processing the same row concurrently.

Investigate:

``` sql
SELECT ...
FOR UPDATE SKIP LOCKED
```

and compare with alternative claiming strategies.

------------------------------------------------------------------------

# 15. SKIP LOCKED Experiment

Create:

``` text
100 pending rows
3 workers
```

Each worker should claim different rows.

Observe:

``` text
worker A locks rows
worker B skips them
worker C skips them
```

Connect this experiment to Phase 3 pessimistic locking.

------------------------------------------------------------------------

# 16. Distributed Job Processing

Move Forge jobs from:

``` text
Phase 4 BlockingQueue
```

to:

``` text
Job API
 ↓
PostgreSQL
 ↓
Outbox
 ↓
Kafka
 ↓
Consumer Group
 ↓
multiple Forge instances
 ↓
JobHandler
```

This is the first real distributed version of the Forge job engine.

------------------------------------------------------------------------

# 17. Event Envelope

Create a common event envelope:

``` json
{
  "eventId": "...",
  "eventType": "JOB_SUBMITTED",
  "schemaVersion": 1,
  "occurredAt": "...",
  "producer": "forge",
  "correlationId": "...",
  "causationId": "...",
  "aggregateType": "JOB",
  "aggregateId": "...",
  "payload": {}
}
```

Separate:

``` text
transport metadata
```

from:

``` text
business payload
```

Every event must have a globally unique `eventId`.

------------------------------------------------------------------------

# 18. Idempotent Consumer

Create:

``` text
processed_events
----------------
event_id
consumer_name
processed_at
result
```

Add:

``` text
UNIQUE(event_id, consumer_name)
```

Use a transaction:

``` text
BEGIN
insert processed event
perform business operation
COMMIT
```

If the same event is received again:

``` text
unique constraint
→ already processed
→ skip duplicate business effect
```

------------------------------------------------------------------------

# 19. Idempotency Race

Run:

``` text
Instance A receives event X
Instance B receives event X
```

at the same time.

Both attempt to insert the same processing record.

The database must decide the winner.

Do not use:

``` text
check exists
then insert
```

because Phase 3 already demonstrated the race.

------------------------------------------------------------------------

# 20. HTTP Idempotency

Support:

``` http
Idempotency-Key: abc123
```

for operations such as:

``` text
create order
submit job
send notification
```

Test:

``` text
same key + same request
```

and:

``` text
same key + different request
```

The first should return the same logical result.

The second should be rejected.

------------------------------------------------------------------------

# 21. Idempotency State Machine

Model:

``` text
NEW
 ↓
PROCESSING
 ↓
COMPLETED
```

and:

``` text
PROCESSING
 ↓
FAILED
```

Define behavior for a duplicate request while the original request is:

``` text
PROCESSING
COMPLETED
FAILED
```

Do not return "already processed" blindly for every state.

------------------------------------------------------------------------

# 22. Kafka Retry and DLT

Create:

``` text
main topic
 ↓
retry handling
 ↓
dead-letter topic
```

Classify failures.

### Retryable

``` text
temporary network failure
provider timeout
temporary database failure
dependency unavailable
```

### Non-retryable

``` text
invalid payload
schema violation
business-rule violation
unknown event type
```

Never retry permanent failures indefinitely.

------------------------------------------------------------------------

# 23. Exponential Backoff

Implement:

``` text
attempt 1 → 1s
attempt 2 → 2s
attempt 3 → 4s
attempt 4 → 8s
```

with:

``` text
maximum delay
maximum attempts
jitter
```

Jitter is important to prevent synchronized retry storms.

------------------------------------------------------------------------

# 24. Dead Letter Topic

Use something like:

``` text
forge.jobs.DLT
```

Record:

``` text
original topic
partition
offset
event ID
attempt count
error
timestamp
original payload
```

A DLT is an operational recovery mechanism, not a trash can.

------------------------------------------------------------------------

# 25. DLT Replay

Create an administrative replay operation:

``` text
DLT event
 ↓
inspect/fix
 ↓
republish
 ↓
normal processing
```

Require deliberate operator action.

Do not create an automatic infinite replay loop.

------------------------------------------------------------------------

# 26. Poison Message

Create a message that always fails.

Verify:

``` text
main topic
 ↓
retry
 ↓
retry
 ↓
retry
 ↓
DLT
```

Verify unrelated work continues.

------------------------------------------------------------------------

# 27. Kafka Partition-Key Design

Use meaningful keys:

``` text
productId
orderId
customerId
jobId
```

Ask:

``` text
What must be ordered?
What can be parallel?
What can create a hot partition?
```

For inventory, key by:

``` text
productId
```

so state changes for the same product remain ordered.

Do not use random event IDs where business ordering is required.

------------------------------------------------------------------------

# 28. Hot Partition Experiment

Create:

``` text
90% of events → product-1
10% → 1000 other products
```

Observe:

``` text
partition imbalance
consumer utilization
throughput
```

Document the tradeoff between:

``` text
ordering
parallelism
hot partitions
```

------------------------------------------------------------------------

# 29. Consumer Lag

Produce events faster than consumers can process them.

Measure:

``` text
consumer lag
processing rate
production rate
lag recovery time
```

Scale consumers and observe recovery.

Create metrics and alerts around lag.

------------------------------------------------------------------------

# 30. Redis Integration

Use Redis for:

``` text
cache
rate limiting
short-lived coordination
distributed locks
```

Do not make Redis the authoritative store for:

``` text
orders
inventory
payments
durable job state
```

PostgreSQL remains authoritative for core transactional state.

------------------------------------------------------------------------

# 31. Cache-Aside

Implement:

``` text
read
 ↓
Redis hit?
 ├── yes → return
 └── no
      ↓
 PostgreSQL
      ↓
 Redis
      ↓
 return
```

For writes:

``` text
PostgreSQL update
 ↓
cache invalidation
```

Document TTL and invalidation rules.

------------------------------------------------------------------------

# 32. Cache Stampede

Expire a hot key with:

``` text
100 concurrent requests
```

Observe:

``` text
100 DB reads
```

Then implement one mitigation:

``` text
single-flight
distributed lock
jittered expiration
```

Compare the behavior.

------------------------------------------------------------------------

# 33. Cache Invalidation Race

Reproduce a stale-cache scenario where:

``` text
T1 updates DB
T2 reads old cache
T1 invalidates cache
T2 writes stale data
```

or an equivalent ordering problem.

Document why:

> Cache invalidation is a consistency problem.

------------------------------------------------------------------------

# 34. Redis TTL

Test different TTLs.

Measure:

``` text
cache hit ratio
database load
staleness window
```

Choose TTL based on business tolerance for stale data rather than
arbitrary convention.

------------------------------------------------------------------------

# 35. Distributed Rate Limiting

Implement a Redis-backed rate limiter.

Example:

``` text
100 requests/minute/customer
```

Run:

``` text
Forge A
Forge B
Forge C
```

Verify the effective limit is shared across instances.

Compare:

``` text
fixed window
sliding window
token bucket
```

Document:

``` text
accuracy
burst behavior
memory
complexity
failure behavior
```

------------------------------------------------------------------------

# 36. Distributed Lock

Implement a Redis distributed-lock experiment with:

``` text
lock key
owner token
lease duration
release only by owner
```

Never simply:

``` text
DEL lockKey
```

because the original owner may have lost the lease and another owner may
now hold it.

------------------------------------------------------------------------

# 37. Lock Expiry Experiment

Reproduce:

``` text
A acquires lock
A pauses
lease expires
B acquires lock
A resumes
```

Observe why a lease-based lock alone does not make stale workers
harmless.

------------------------------------------------------------------------

# 38. Fencing Tokens

Study and implement fencing tokens.

Concept:

``` text
lock acquisition #41
lock acquisition #42
```

Downstream state accepts only operations carrying the latest valid
fencing token.

This demonstrates an important principle:

> A stale worker must be prevented from mutating protected state even if
> it wakes up after its lease expires.

------------------------------------------------------------------------

# 39. Distributed Scheduled Jobs

Run the same scheduled task on:

``` text
Forge A
Forge B
Forge C
```

Without coordination:

``` text
A executes
B executes
C executes
```

Add distributed coordination.

Kill the current owner and verify another instance can take over.

The underlying operation must remain idempotent even with the lock.

------------------------------------------------------------------------

# 40. Eventual Consistency

Create an asynchronous read model:

``` text
Order created
 ↓
OrderCreated
 ↓
Kafka
 ↓
Projection consumer
 ↓
order_summary
```

The write model is updated first.

The projection becomes consistent later.

Measure the propagation delay.

------------------------------------------------------------------------

# 41. Projection Table

Create:

``` text
order_summary
-------------
order_id
customer_id
status
total_amount
item_count
aggregate_version
last_event_id
updated_at
```

Update it exclusively through events.

------------------------------------------------------------------------

# 42. Projection Replay

Delete the projection.

Replay the event history.

Rebuild:

``` text
order_summary
```

Compare the rebuilt result with the expected current state.

This makes event replay a concrete engineering capability.

------------------------------------------------------------------------

# 43. Out-of-Order Events

Create:

``` text
revision 5 → CONFIRMED
revision 6 → SHIPPED
```

Deliver revision 6 before revision 5.

Prevent stale revision 5 from overwriting revision 6.

Prefer:

``` text
aggregate revision/sequence
```

over blindly trusting timestamps.

------------------------------------------------------------------------

# 44. Event Versioning

Create:

``` text
schemaVersion = 1
```

Then introduce:

``` text
schemaVersion = 2
```

Test rolling compatibility.

Document:

``` text
backward compatibility
forward compatibility
consumer evolution
```

Initially JSON is acceptable, but document the limitations versus
Avro/Protobuf/schema registries.

------------------------------------------------------------------------

# 45. Inventory Events

Publish:

``` text
InventoryReserved
InventoryReleased
InventoryAdjusted
```

Use them to drive:

``` text
audit
order state
analytics
```

without direct coupling between consumers and the inventory application
layer.

------------------------------------------------------------------------

# 46. Notification Pipeline

Implement:

``` text
OrderConfirmed
 ↓
Kafka
 ↓
Notification consumer
 ↓
Notification job
 ↓
Provider
```

The order API must not wait for:

``` text
email
SMS
push
```

This is the first real asynchronous notification architecture.

------------------------------------------------------------------------

# 47. Notification Idempotency

Define a logical identity such as:

``` text
orderId + notificationType + channel
```

or another domain-appropriate key.

Duplicate events must not cause unnecessary duplicate notifications.

Keep:

``` text
event delivery
```

separate from:

``` text
provider delivery
```

------------------------------------------------------------------------

# 48. Distributed Job State

Define ownership explicitly:

``` text
PostgreSQL
→ durable job state

Kafka
→ event transport

Worker
→ temporary execution ownership
```

Avoid allowing two independent systems to become authoritative for the
same business state.

------------------------------------------------------------------------

# 49. Exactly-Once Analysis

Document:

``` text
at-most-once
at-least-once
effectively-once
exactly-once
```

Do not claim "exactly once" merely because Kafka supports transactions.

For many Forge workflows:

``` text
at-least-once delivery
+
idempotent processing
=
effectively-once business effect
```

provided the idempotency boundary is correctly designed.

------------------------------------------------------------------------

# 50. Resilience Patterns

Implement experimentally:

``` text
timeout
retry
exponential backoff
jitter
circuit breaker
bulkhead
```

Use them around simulated notification providers.

Circuit states:

``` text
CLOSED
 ↓ repeated failures
OPEN
 ↓ wait
HALF_OPEN
 ↓ success
CLOSED
```

Understand the distinct purpose of each mechanism.

------------------------------------------------------------------------

# 51. Retry Storm

Make a dependency fail.

Send:

``` text
100 requests
```

with aggressive retry.

Measure:

``` text
load amplification
latency
queue growth
recovery time
```

Then add:

``` text
bounded retries
exponential backoff
jitter
circuit breaker
```

Compare recovery.

------------------------------------------------------------------------

# 52. Timeout and Retry Budgets

For every dependency document:

``` text
timeout
retry owner
maximum attempts
maximum retry duration
backoff
jitter
```

Avoid layered retry multiplication:

``` text
HTTP retry
 ↓
service retry
 ↓
Kafka retry
 ↓
provider retry
```

which can amplify load dramatically.

------------------------------------------------------------------------

# 53. Network Failure Experiments

Simulate between:

``` text
Forge ↔ PostgreSQL
Forge ↔ Redis
Forge ↔ Kafka
Forge ↔ provider
```

Failures:

``` text
timeout
connection refused
slow response
connection reset
temporary outage
```

Observe:

``` text
threads
connection pools
queues
Kafka lag
retries
latency
```

------------------------------------------------------------------------

# 54. Dependency Failure Policies

Explicitly decide:

### Redis unavailable

Can the application fall back to PostgreSQL?

### Kafka unavailable

Can business transactions continue through the outbox?

### Provider unavailable

Can notification jobs remain pending/retry?

### Rate limiter unavailable

Should the system fail open or closed?

Document the decision for each.

------------------------------------------------------------------------

# 55. Distributed Observability

Trace:

``` text
HTTP request
 ↓
PostgreSQL transaction
 ↓
outbox event
 ↓
Kafka
 ↓
consumer
 ↓
job
 ↓
provider
```

Propagate:

``` text
correlationId
causationId
eventId
jobId
trace/span context
```

Create metrics for:

``` text
outbox pending count
outbox publish latency
Kafka publish failures
consumer lag
event processing latency
duplicate events
DLT count
job retries
Redis hit ratio
rate-limit rejects
circuit state
provider latency
```

Avoid high-cardinality metric labels such as:

``` text
eventId
jobId
requestId
userId
```

------------------------------------------------------------------------

# 56. Multi-Instance Inventory

Run:

``` text
Forge A
Forge B
Forge C
```

with:

``` text
stock = 100
```

Generate:

``` text
500 concurrent reservation requests
```

Verify:

``` text
successful reservations <= 100
available_quantity >= 0
```

The authoritative correctness mechanism remains PostgreSQL.

Kafka communicates events; Redis may assist with performance or
coordination but must not replace the transactional invariant.

------------------------------------------------------------------------

# 57. Distributed Idempotency Test

Send the same:

``` text
Idempotency-Key
```

concurrently to different Forge instances.

Expected:

``` text
one logical operation
```

not:

``` text
one operation per instance
```

This proves idempotency must live in shared durable state.

------------------------------------------------------------------------

# 58. Distributed Debugging Exercise

Build:

``` text
POST /orders
 ↓
PostgreSQL
 ↓
outbox
 ↓
Kafka
 ↓
notification consumer
 ↓
job
 ↓
provider
```

Introduce:

``` text
consumer crash
provider timeout
duplicate event
Kafka delay
Redis failure
```

Reconstruct the workflow using:

``` text
logs
metrics
traces
database state
Kafka offsets/lag
```

Do not debug distributed failures from a single service log.

------------------------------------------------------------------------

# 59. Load Balancing and Statelessness

Run:

``` text
Forge A
Forge B
Forge C
```

behind a local reverse proxy/load balancer.

Verify requests can reach different instances.

Then ensure correctness does not depend on:

``` text
instance memory
static variables
local maps
local queues
```

Durable/shared state belongs in:

``` text
PostgreSQL
Redis
Kafka
```

according to its responsibility.

------------------------------------------------------------------------

# 60. Required Documentation

Create:

``` text
docs/distributed-systems.md
```

ADRs:

``` text
docs/architecture-decisions.md
```

Each ADR must contain:

``` text
context
decision
alternatives
tradeoffs
consequences
```

Also create:

``` text
docs/incident-reports.md
```

and document at least five distributed failure experiments.

------------------------------------------------------------------------

# 61. Required Failure Experiments

Before Phase 6, reproduce and document:

1.  Duplicate Kafka delivery after consumer crash.
2.  Lost-event scenario without an outbox.
3.  Outbox recovery after Kafka outage.
4.  Consumer rebalance during processing.
5.  Poison message → DLT.
6.  DLT replay.
7.  Hot Kafka partition.
8.  Consumer lag and recovery.
9.  Cache stampede.
10. Redis outage with business correctness preserved.
11. Distributed rate limiting across three instances.
12. Redis lock expiry with stale owner.
13. Fencing-token protection.
14. Eventual-consistency propagation delay.
15. Out-of-order events.
16. Retry storm versus bounded backoff+jitter.
17. Circuit breaker state transitions.
18. Multi-instance inventory reservation.
19. Multi-instance idempotency race.
20. Multi-instance scheduled-job ownership.

------------------------------------------------------------------------

# 62. Definition of Done

Phase 5 is complete when:

-   [ ] Forge runs as multiple application instances.
-   [ ] Local infrastructure is reproducible.
-   [ ] Kafka topics and partitions are deliberately designed.
-   [ ] Consumer groups are understood.
-   [ ] Rebalancing has been observed.
-   [ ] Offset behavior is understood.
-   [ ] At-least-once processing is understood.
-   [ ] Duplicate delivery has been reproduced.
-   [ ] Transactional outbox is implemented.
-   [ ] Multiple outbox publishers can safely run.
-   [ ] `SKIP LOCKED` has been investigated.
-   [ ] Distributed jobs run through Kafka.
-   [ ] Events use a standard envelope.
-   [ ] Event IDs are unique.
-   [ ] Idempotent consumers are implemented.
-   [ ] HTTP idempotency is implemented.
-   [ ] Retryable and permanent failures are classified.
-   [ ] Exponential backoff and jitter are implemented.
-   [ ] DLT exists.
-   [ ] DLT replay exists.
-   [ ] Poison messages are handled.
-   [ ] Kafka partition keys are deliberate.
-   [ ] Consumer lag is measured.
-   [ ] Redis cache-aside is implemented.
-   [ ] Cache stampede has been reproduced and mitigated.
-   [ ] Cache invalidation behavior is understood.
-   [ ] Distributed rate limiting is implemented.
-   [ ] Distributed locking has been experimentally implemented.
-   [ ] Lock ownership is protected.
-   [ ] Lock expiry failure has been demonstrated.
-   [ ] Fencing tokens are understood.
-   [ ] Distributed scheduled-job ownership works.
-   [ ] Eventual-consistency projection exists.
-   [ ] Projection replay works.
-   [ ] Out-of-order events are handled.
-   [ ] Event versioning is documented.
-   [ ] Circuit breaker is implemented experimentally.
-   [ ] Notification bulkheads exist.
-   [ ] Timeout and retry budgets are documented.
-   [ ] Network failures have been simulated.
-   [ ] PostgreSQL remains authoritative for core business invariants.
-   [ ] Distributed inventory works across instances.
-   [ ] Multi-instance idempotency works.
-   [ ] Distributed correlation works across asynchronous boundaries.
-   [ ] Kafka/outbox health is observable.
-   [ ] Failure matrix is documented.
-   [ ] Architecture decisions are documented.
-   [ ] All distributed failure experiments pass.
-   [ ] Static analysis passes.
-   [ ] Integration tests pass.

------------------------------------------------------------------------

# 63. Exit Questions

You should be able to answer these without memorized definitions.

### Distributed Systems

1.  What makes distributed systems harder than multithreaded systems?
2.  What is partial failure?
3.  Why can a successful HTTP response be ambiguous?
4.  What does at-least-once delivery mean?
5.  Why is exactly-once difficult?

### Kafka

6.  What is a partition?
7.  Why does a key affect partition placement?
8.  What ordering does Kafka actually guarantee?
9.  What is a consumer group?
10. Why can't ten consumers meaningfully process one three-partition
    topic in parallel?
11. What causes a rebalance?
12. What is consumer lag?
13. What happens if a consumer crashes before committing its offset?

### Outbox

14. Why can DB + Kafka not simply share one Spring transaction?
15. What problem does the transactional outbox solve?
16. Why can an outbox event still be published twice?
17. Why must the Kafka consumer therefore remain idempotent?

### Idempotency

18. What is idempotency?
19. Why is check-then-insert unsafe?
20. Why does a database unique constraint help?
21. What should happen when the same idempotency key is used with a
    different request body?

### Redis

22. When should Redis be cache rather than source of truth?
23. What is cache-aside?
24. What causes cache stampede?
25. What causes stale cache data?
26. What happens if Redis is unavailable?

### Distributed Locks

27. Why is an in-process lock insufficient across instances?
28. Why does a Redis lock need an owner token?
29. What happens when a lease expires while the original owner is still
    running?
30. Why are fencing tokens useful?
31. Why should core inventory correctness not depend solely on a Redis
    lock?

### Eventual Consistency

32. What is eventual consistency?
33. Why can a projection temporarily disagree with the write model?
34. How can a projection handle duplicate events?
35. How can it handle out-of-order events?
36. Why is an aggregate revision better than blindly trusting
    timestamps?

### Resilience

37. What is a retry storm?
38. Why use exponential backoff?
39. Why add jitter?
40. What is a circuit breaker?
41. What is a bulkhead?
42. Why should retry ownership be defined explicitly?

### System Design

43. Where is the source of truth for inventory?
44. What does Kafka own?
45. What does Redis own?
46. What does PostgreSQL own?
47. What happens if Kafka is down after an order commits?
48. What happens if a consumer crashes after committing the DB
    transaction but before committing the Kafka offset?
49. What happens if Redis goes down?
50. How does Forge recover when one application instance dies?

------------------------------------------------------------------------

# 64. Suggested Git Commit Sequence

``` text
feat(infra): add reproducible distributed local environment
feat(kafka): add topic configuration
feat(kafka): add producer and consumer laboratory
test(kafka): demonstrate partition ordering
test(kafka): demonstrate consumer rebalancing
feat(messaging): add event envelope
feat(outbox): add outbox schema
feat(outbox): add transactional event recording
feat(outbox): add publisher
feat(outbox): add concurrent row claiming
feat(job): move jobs to kafka
feat(job): add idempotent consumer
feat(job): add retry and dead-letter handling
feat(job): add dlt replay
feat(redis): add cache-aside
feat(redis): add distributed rate limiter
feat(redis): add distributed lock experiment
feat(redis): add fencing token experiment
feat(events): add eventual-consistency projection
feat(events): add event replay
feat(events): add event versioning
feat(resilience): add provider circuit breaker
feat(resilience): add provider bulkheads
feat(observability): add distributed correlation
feat(observability): add kafka/outbox metrics
test(distributed): add multi-instance inventory tests
test(distributed): add idempotency race tests
test(distributed): add failure injection
test(distributed): add kafka crash/recovery tests
docs(distributed): document architecture and failures
```

------------------------------------------------------------------------

# 65. Phase 5 → Phase 6

Phase 6 should move from:

``` text
distributed application
```

to:

``` text
production-grade distributed platform
```

Likely topics:

``` text
service decomposition
API gateway
service-to-service communication
service discovery
load balancing
OpenTelemetry
centralized logging
configuration management
secrets
containers
Kubernetes
health probes
rolling deployments
horizontal scaling
resource limits
autoscaling
chaos engineering
SLIs
SLOs
error budgets
incident response
```

Phase 5 teaches:

> I understand how distributed components behave.

Phase 6 should teach:

> I can operate a distributed system reliably.

------------------------------------------------------------------------

# 66. Final Rule

> Never hide distributed-system failure behind an abstraction you do not
> understand.

When you see:

``` text
Kafka listener
Redis lock
cache
retry
circuit breaker
outbox
distributed job
```

you should be able to reason about:

``` text
ownership
durability
ordering
delivery guarantee
duplication
failure
retry
timeout
backpressure
consistency
recovery
observability
```

The goal is not:

> "I know Kafka and Redis."

The goal is:

> **I can design a distributed workflow where failures, duplicates,
> retries, stale state, partial completion, and instance crashes are
> expected and explicitly handled.**
