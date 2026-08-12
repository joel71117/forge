# Forge Commerce Platform — Distributed Backend Engineering Roadmap

## Project Goal

Build a single serious Spring Boot project that combines:

1. **Distributed job processing**
2. **Notification delivery**
3. **Distributed inventory/order processing**

The purpose is **not** to build a typical CRUD e-commerce application.

The purpose is to use the domain to develop deeper ability in:

- Java
- Spring Boot
- Databases
- Concurrency
- Distributed systems
- System design
- Debugging
- Observability
- Performance engineering
- Failure handling
- Architecture

The target outcome is:

> I can look at a backend requirement, identify the consistency, concurrency, failure, scalability and observability problems, design the system, and then implement it in Java/Spring.

---

# 1. Overall Project Concept

The final system is a distributed commerce backend where:

- Users purchase products.
- Products have inventory.
- Orders reserve inventory.
- Payments are simulated.
- Background jobs execute asynchronously.
- Notifications are delivered through email, SMS and push.
- Jobs can be scheduled, retried and recovered.
- Services communicate asynchronously through Kafka.
- Redis is used where appropriate for caching, coordination and rate limiting.
- PostgreSQL provides durable transactional storage.
- Prometheus, Grafana and OpenTelemetry provide observability.
- Docker and eventually Kubernetes provide deployment infrastructure.

A possible final architecture:

```text
                         ┌──────────────────┐
                         │     Client       │
                         └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │   API Gateway    │
                         └────────┬─────────┘
                                  │
                 ┌────────────────┼─────────────────┐
                 │                │                 │
                 ▼                ▼                 ▼
          ┌────────────┐   ┌────────────┐   ┌──────────────┐
          │   Order    │   │ Inventory  │   │    User      │
          │  Service   │   │  Service   │   │   Service    │
          └─────┬──────┘   └─────┬──────┘   └──────────────┘
                │                │
                └────────┬───────┘
                         ▼
                    ┌─────────┐
                    │  Kafka  │
                    └────┬────┘
                         │
          ┌──────────────┼─────────────────┐
          │              │                 │
          ▼              ▼                 ▼
    ┌───────────┐  ┌────────────┐   ┌───────────────┐
    │   Job     │  │Notification│   │    Payment    │
    │ Platform  │  │  Platform  │   │    Service    │
    └─────┬─────┘  └─────┬──────┘   └───────────────┘
          │              │
          ▼              ▼
     ┌─────────┐    ┌──────────────┐
     │ Workers │    │ Email/SMS/   │
     │         │    │ Push Providers│
     └─────────┘    └──────────────┘

       PostgreSQL     Redis       Kafka
           │            │           │
           └────────────┴───────────┘
                         │
                   Observability
                Prometheus/Grafana
                OpenTelemetry
```

**Important:** this is the destination, not the starting architecture.

The system should evolve gradually as engineering problems appear.

---

# 2. Core Development Philosophy

The project should follow:

```text
Java
 ↓
Spring Boot
 ↓
Database
 ↓
Concurrency
 ↓
Caching
 ↓
Messaging
 ↓
Distributed processing
 ↓
Failure handling
 ↓
Observability
 ↓
Performance
 ↓
Distributed architecture
 ↓
System design
```

At every major stage:

```text
Build
  ↓
Break
  ↓
Investigate
  ↓
Redesign
  ↓
Rebuild
```

The objective is to understand **why** the system behaves the way it does, rather than merely learning which technology to use.

---

# 3. Overall Roadmap

| Phase | Main Subject |
|---|---|
| 0 | Requirements & engineering fundamentals |
| 1 | Domain modelling |
| 2 | Java core + modular monolith |
| 3 | PostgreSQL mastery |
| 4 | Inventory concurrency |
| 5 | Order workflow |
| 6 | Job processing engine |
| 7 | Redis |
| 8 | Kafka & event-driven architecture |
| 9 | Notification platform |
| 10 | Distributed workflows |
| 11 | Failure engineering |
| 12 | Observability |
| 13 | Performance & load testing |
| 14 | Microservices & infrastructure |
| 15 | Final system-design challenge |

---

# PHASE 0 — Requirements and Engineering Fundamentals

## Goal

Learn to convert vague requirements into concrete engineering requirements.

Start with:

> Users can purchase products. Products have inventory. Orders reserve inventory. Orders can be processed asynchronously. Users receive notifications about important order events.

## Actors

Initially:

```text
User
Admin
System
Worker
```

## Initial entities

```text
User
Product
Inventory
Order
OrderItem
Job
Notification
```

Later:

```text
Payment
InventoryReservation
JobAttempt
NotificationAttempt
Provider
ScheduledJob
OutboxEvent
```

## First deliverable

Create:

```text
/docs
    requirements.md
    domain-model.md
    architecture.md
    decisions/
```

Before coding, be able to explain:

- What is an order?
- What is an inventory reservation?
- What happens when inventory is unavailable?
- What happens when an order fails?
- What is a job?
- What is a notification?
- What does "processed" mean?

This phase teaches requirements analysis and domain thinking.

---

# PHASE 1 — Domain-Driven Java

Build the core domain as a Java application before introducing Kafka or Redis.

Focus on:

```text
Interfaces
Abstract classes
Composition
Enums
Records
Immutability
Generics
Collections
Streams
Functional interfaces
Exceptions
```

## Example abstraction

Instead of:

```java
if (notificationType == EMAIL) {
    ...
}
```

design:

```java
interface NotificationSender {
    NotificationResult send(Notification notification);
}
```

Then:

```text
EmailSender
SmsSender
PushSender
```

This should naturally lead to understanding:

- Strategy pattern
- Factory pattern
- Builder pattern
- State pattern
- Dependency inversion
- SOLID
- Polymorphism
- Composition

Do not memorize patterns first. Encounter the design problem first.

---

# PHASE 2 — Spring Boot Modular Monolith

Create the first real application.

Suggested structure:

```text
forge
├── api
├── order
├── inventory
├── notification
├── job
├── user
└── common
```

Use:

```text
ONE Spring Boot application
ONE PostgreSQL database
```

This is intentional.

## Initial APIs

```text
POST /products
GET /products/{id}

POST /inventory
GET /inventory/{productId}

POST /orders
GET /orders/{id}

POST /jobs
GET /jobs/{id}

GET /notifications/{id}
```

The goal is not CRUD. The important part is domain behavior and business rules.

---

# PHASE 3 — PostgreSQL Deep Dive

This phase is extremely important.

Do not merely use JPA. Understand what the database and Hibernate are doing underneath.

## Indexes

Experiment with:

```sql
EXPLAIN ANALYZE
```

Learn:

```text
B-tree
Composite indexes
Index selectivity
Covering indexes
Index scans
Sequential scans
```

## Transactions

Experiment with:

```text
READ COMMITTED
REPEATABLE READ
SERIALIZABLE
```

Create actual concurrent transactions and observe behavior.

## Locking

Experiment with:

```sql
SELECT ... FOR UPDATE
```

Deliberately create a deadlock and investigate it.

## Optimistic locking

Add a:

```text
version
```

column and use optimistic concurrency.

## Connection pool

Understand:

```text
HikariCP
```

and what happens when:

```text
100 requests
↓
10 DB connections
```

## Deliverable

Create:

```text
/docs/database-experiments.md
```

Document your actual experiments and observations, not copied definitions.

---

# PHASE 4 — Inventory Concurrency

This is the first major engineering challenge.

Suppose:

```text
Product A
Inventory = 10
```

100 users simultaneously attempt:

```text
Buy 1
```

The system must never produce:

```text
Inventory = -5
```

or sell more than 10 units.

## Version 1 — Naive

```text
read inventory
check quantity
update inventory
```

Break it with concurrent requests.

## Version 2 — Transaction

Add:

```java
@Transactional
```

Test again.

## Version 3 — Pessimistic locking

Use:

```sql
SELECT ... FOR UPDATE
```

Test again.

## Version 4 — Optimistic locking

Use a:

```text
version
```

column.

Test again.

## Version 5 — Atomic SQL

For example:

```sql
UPDATE inventory
SET quantity = quantity - 1
WHERE product_id = ?
AND quantity >= 1;
```

Compare all approaches.

The goal is to actually understand:

- Database concurrency
- Transactions
- Isolation
- Row locks
- Optimistic locking
- Pessimistic locking
- Atomic operations

---

# PHASE 5 — Order System

Implement an order lifecycle.

## Basic flow

```text
Create Order
     ↓
Validate products
     ↓
Check inventory
     ↓
Reserve inventory
     ↓
Create order
     ↓
Order = CREATED
```

Then:

```text
CREATED
   ↓
CONFIRMED
   ↓
PROCESSING
   ↓
COMPLETED
```

Failure:

```text
CREATED
   ↓
FAILED
```

Cancellation:

```text
CONFIRMED
   ↓
CANCELLED
```

Design this as a state machine.

Ask:

> Which transitions are legal?

For example:

```text
COMPLETED → CREATED
```

should normally be illegal.

Do not scatter status checks throughout the code.

---

# PHASE 6 — Build the Job Processing Engine

Now introduce the distributed job-processing component.

Initially do not use Kafka.

Build:

```text
Job API
   ↓
Job Queue
   ↓
Worker Pool
   ↓
Job Executor
```

## Job model

```text
Job
 ├── id
 ├── type
 ├── payload
 ├── priority
 ├── status
 ├── scheduledAt
 ├── retryCount
 ├── maxRetries
 ├── createdAt
 └── completedAt
```

## Java concurrency

Implement workers using concepts such as:

```text
ExecutorService
ThreadPoolExecutor
BlockingQueue
ConcurrentHashMap
AtomicInteger
Future
CompletableFuture
```

Start with:

```text
10 worker threads
```

and process jobs concurrently.

Explore:

- Thread pools
- Queue capacity
- Worker starvation
- Backpressure
- Bounded vs unbounded queues
- Task cancellation
- Graceful shutdown

---

# PHASE 7 — Job Scheduling

Support:

```text
Execute immediately
Execute later
Retry after failure
Periodic jobs
```

Examples:

```text
send notification in 5 minutes
```

or:

```text
generate report at 2 AM
```

Learn:

```text
ScheduledExecutorService
Spring Scheduler
Cron expressions
Delayed jobs
```

Then ask:

> What happens if two application instances both execute the same scheduled job?

This introduces distributed scheduling.

---

# PHASE 8 — Redis

Introduce Redis only where it provides an engineering benefit.

## Use case 1 — Cache

```text
Product
 ↓
Redis
 ↓
PostgreSQL
```

Learn:

```text
Cache-aside
TTL
Cache invalidation
Cache stampede
Stale data
```

## Use case 2 — Distributed lock

For example:

```text
Scheduled job
     ↓
Multiple application instances
     ↓
Only one should execute
```

Experiment with distributed locking.

## Use case 3 — Rate limiting

Implement:

```text
100 requests/minute/user
```

Learn:

```text
Fixed window
Sliding window
Token bucket
```

## Use case 4 — Idempotency

Implement:

```text
Idempotency-Key
```

for operations such as order creation.

This teaches why clients retry requests and why servers must safely handle duplicates.

---

# PHASE 9 — Kafka

Introduce Kafka for asynchronous communication.

Example:

```text
Order Service
      │
      ▼
OrderCreated
      │
      ▼
    Kafka
      │
      ├──────────────┐
      ▼              ▼
Inventory       Notification
Consumer          Consumer
```

Learn deeply:

```text
Topic
Partition
Offset
Consumer
Consumer group
Rebalance
Ordering
Retention
Replication
Consumer lag
```

Do not treat Kafka as simply:

> "A place to send messages."

Understand the delivery and ordering model.

---

# PHASE 10 — Event-Driven Order Processing

Redesign parts of the order flow.

Instead of:

```text
POST /orders

Order Service
    ↓
Inventory Service
    ↓
Notification Service
    ↓
response
```

use:

```text
POST /orders
     ↓
Create Order
     ↓
Publish OrderCreated
     ↓
Return
```

Then:

```text
OrderCreated
      ↓
Kafka
      ├── Inventory
      ├── Notification
      └── Analytics
```

Now you encounter:

> Eventual consistency

instead of requiring everything to be immediately consistent.

---

# PHASE 11 — Combine Job Processing + Notifications

Turn notifications into a real asynchronous subsystem.

```text
OrderCreated
     ↓
Notification Job
     ↓
Job Queue
     ↓
Notification Worker
     ↓
Provider
```

Support:

```text
EMAIL
SMS
PUSH
```

## Notification model

```text
Notification
 ├── id
 ├── userId
 ├── type
 ├── template
 ├── payload
 ├── status
 ├── priority
 └── attempts
```

---

# Notification Retry System

If:

```text
Provider
   ↓
timeout
```

the system should implement:

```text
attempt 1
   ↓
wait
   ↓
attempt 2
   ↓
wait
   ↓
attempt 3
   ↓
DLQ
```

Implement:

```text
Exponential backoff
Jitter
Maximum retries
Dead-letter queue
```

Then add provider failover:

```text
Provider A
   ↓ failure
Provider B
   ↓ failure
Provider C
```

This teaches practical distributed-system resilience.

---

# PHASE 12 — Idempotency

Make idempotency a major theme.

Consider:

```text
OrderCreated
     ↓
Notification Worker
     ↓
Provider
     ↓
Success
     ↓
Application crashes
```

Kafka redelivers:

```text
OrderCreated
```

The notification must not accidentally be sent twice.

Design around:

```text
Event ID
Notification ID
Idempotency key
Processed-event table
Unique constraints
Idempotent database operations
```

Understand:

> At-least-once delivery + idempotent processing

is often more practical than trying to achieve global exactly-once behavior.

---

# PHASE 13 — Distributed Order Workflow

Introduce a simulated payment service.

The service should deliberately be unreliable:

```text
success
failure
timeout
duplicate response
```

The workflow becomes:

```text
Order
 ↓
Inventory Reservation
 ↓
Payment
 ↓
Order Confirmation
 ↓
Notification
```

Now investigate:

## Scenario 1

```text
Inventory succeeds
Payment fails
```

Inventory must be released.

## Scenario 2

```text
Payment succeeds
Order service crashes
```

How do you recover?

## Scenario 3

```text
Payment request times out
```

Did payment happen or not?

Should you retry?

This introduces Saga-style workflows and compensating actions.

---

# PHASE 14 — Failure Engineering

Create a failure matrix.

| Failure | Expected behavior |
|---|---|
| PostgreSQL unavailable | API fails gracefully |
| Redis unavailable | Fallback to DB where appropriate |
| Kafka unavailable | Define acceptable behavior |
| Kafka consumer crashes | Resume processing |
| Duplicate event | No duplicate effect |
| Worker crashes | Job recovered/retried |
| Provider timeout | Retry |
| Provider permanently fails | DLQ |
| DB deadlock | Retry transaction where appropriate |
| Network timeout | Bounded retry |
| One worker overloaded | Other workers continue |
| Scheduled worker duplicated | Only one executes |
| Application crashes mid-job | Recover safely |

The key question is:

> What state is the system in after the failure?

Do not merely catch exceptions.

---

# PHASE 15 — Circuit Breaker and Resilience

Make the payment or notification provider deliberately slow.

Implement:

```text
Timeout
Retry
Circuit breaker
Bulkhead
Rate limiter
```

Understand their differences.

Example:

```text
Provider slow
     ↓
Timeout
     ↓
Retry
     ↓
Still failing
     ↓
Circuit opens
     ↓
Stop sending requests
```

Then:

```text
Wait
 ↓
Half-open
 ↓
Test request
 ↓
Success
 ↓
Closed
```

Understand why retries can sometimes make outages worse.

---

# PHASE 16 — Observability

This phase directly addresses the current weakness in production debugging.

## Spring Boot Actuator

Learn:

```text
Health
Metrics
Info
Beans
Thread dump
```

## Metrics

Use:

```text
Micrometer
Prometheus
Grafana
```

Monitor:

```text
HTTP request count
HTTP latency
p50
p95
p99
Error rate
JVM memory
GC
CPU
Thread pools
DB connection pool
Kafka consumer lag
Job queue depth
Job execution time
Notification failures
```

## Distributed tracing

Use:

```text
OpenTelemetry
```

Trace:

```text
POST /orders
     │
     ├── Order DB
     │
     ├── Kafka
     │
     ├── Inventory
     │
     ├── Payment
     │
     └── Notification
```

The objective is to identify where latency and failures originate rather than guessing.

---

# PHASE 17 — Performance Engineering

Introduce load.

Start with:

```text
10 req/s
```

Then:

```text
100
500
1000
5000
```

Measure:

```text
Throughput
Latency
p95
p99
Error rate
CPU
Memory
GC
DB connections
Kafka lag
Queue depth
```

Deliberately create bottlenecks.

## Example

Set:

```text
DB connection pool = 10
```

and send:

```text
500 concurrent requests
```

Observe the effect.

Then compare Kafka consumers:

```text
Consumer count = 1
```

versus:

```text
Consumer count = 10
```

Observe throughput, ordering and partition limitations.

---

# PHASE 18 — JVM Deep Dive

Now that the application is large enough, study the JVM through real problems.

Learn:

```text
JVM architecture
Stack
Heap
Metaspace
Object allocation
Garbage collection
Generational GC
G1
GC pauses
JIT
JIT compilation
Escape analysis
Class loading
Thread stacks
```

Use:

```text
jstack
jmap
jcmd
JFR
VisualVM
```

Investigate problems such as:

> Why are 1000 workers consuming huge amounts of memory?

> Why did latency increase after increasing concurrency?

> What is causing excessive GC activity?

---

# PHASE 19 — Split the Monolith

Only now consider microservices.

Potential services:

```text
API Gateway
     │
     ├── Order Service
     ├── Inventory Service
     ├── Job Service
     ├── Notification Service
     └── Payment Service
```

Each service can eventually own its own database.

This forces you to confront:

```text
Network communication
Serialization
Service failures
Distributed transactions
Deployment
Observability
Data ownership
```

Understand why microservices are harder rather than assuming:

> Microservices = better scalability.

---

# PHASE 20 — Service Discovery and Load Balancing

Run multiple instances.

Learn:

```text
Service discovery
Client-side load balancing
Server-side load balancing
Health checks
Instance registration
Deregistration
```

Do not jump immediately to Kubernetes.

Understand the underlying concepts first.

---

# PHASE 21 — Docker

Containerize the system.

Eventually use something like:

```text
docker-compose
│
├── forge-order
├── forge-inventory
├── forge-job
├── forge-notification
├── postgres
├── redis
├── kafka
├── prometheus
└── grafana
```

The entire local environment should eventually be reproducible.

---

# PHASE 22 — Kubernetes

Only after Docker and the architecture are understood.

Deploy multiple instances:

```text
Order Service × 3
Inventory Service × 3
Notification Service × 3
Job Worker × 5
```

Learn:

```text
Pod
Deployment
Service
ConfigMap
Secret
Ingress
Readiness probe
Liveness probe
Horizontal scaling
Resource limits
```

Then test:

> What happens when Kubernetes kills a worker halfway through a job?

The system should recover safely.

---

# PHASE 23 — Security

Implement:

```text
Authentication
Authorization
JWT
Roles
Permissions
API security
Rate limiting
Secret management
```

Example:

```text
USER
 ├── View products
 ├── Create order
 └── View own orders

ADMIN
 ├── Manage products
 ├── Manage inventory
 └── Inspect jobs
```

The goal is to understand why each security mechanism exists, not merely configure Spring Security.

---

# PHASE 24 — Testing

The project should have multiple levels of testing.

## Unit tests

Test:

```text
Domain logic
State transitions
Retry calculation
Priority logic
```

## Integration tests

Test real infrastructure using:

```text
Testcontainers
```

Test against:

```text
Spring Boot
PostgreSQL
Redis
Kafka
```

## Concurrency tests

For example:

```text
100 threads
↓
Same inventory
↓
100 simultaneous purchases
```

Expected:

```text
Successful orders <= available inventory
```

## Contract tests

Test service-to-service contracts.

## End-to-end tests

Example:

```text
Create order
 ↓
Reserve inventory
 ↓
Process payment
 ↓
Publish event
 ↓
Notification
 ↓
Notification delivered
```

---

# PHASE 25 — Chaos and Failure Testing

Deliberately:

```text
Kill worker
Kill Kafka
Kill Redis
Kill PostgreSQL
Delay payment service
Drop network requests
Make notification provider return 500
```

Then observe:

> Does the system recover?

This is the final practical distributed-systems level.

---

# 4. Final Architecture

By the end, a possible architecture is:

```text
                              ┌──────────────┐
                              │    Client    │
                              └──────┬───────┘
                                     │
                                     ▼
                              ┌──────────────┐
                              │ API Gateway  │
                              └──────┬───────┘
                                     │
                 ┌───────────────────┼───────────────────┐
                 │                   │                   │
                 ▼                   ▼                   ▼
          ┌─────────────┐    ┌──────────────┐    ┌──────────────┐
          │    Order    │    │  Inventory   │    │     Job      │
          │   Service   │    │   Service    │    │   Service    │
          └──────┬──────┘    └──────┬───────┘    └──────┬───────┘
                 │                  │                   │
                 └──────────────────┼───────────────────┘
                                    │
                                    ▼
                              ┌───────────┐
                              │   Kafka   │
                              └─────┬─────┘
                                    │
                    ┌───────────────┼────────────────┐
                    │               │                │
                    ▼               ▼                ▼
              ┌───────────┐  ┌──────────────┐  ┌─────────────┐
              │ Inventory │  │Notification  │  │    Job      │
              │ Consumer  │  │   Service    │  │   Workers   │
              └───────────┘  └──────┬───────┘  └─────────────┘
                                     │
                              ┌──────┼──────┐
                              ▼      ▼      ▼
                            Email    SMS   Push

            ┌────────────┐
            │ PostgreSQL │
            └────────────┘

            ┌────────────┐
            │   Redis    │
            └────────────┘

            ┌────────────────────────────┐
            │ Prometheus + Grafana       │
            │ OpenTelemetry              │
            └────────────────────────────┘
```

Remember:

**This is the destination, not the starting architecture.**

---

# 5. Technology Progression

Do not install everything at the beginning.

| Stage | Technology |
|---|---|
| Start | Java 21 |
| Start | Spring Boot |
| Start | PostgreSQL |
| Start | JUnit 5 |
| Start | Maven |
| Later | Testcontainers |
| Later | Redis |
| Later | Kafka |
| Later | Prometheus |
| Later | Grafana |
| Later | OpenTelemetry |
| Later | Docker |
| Much later | Kubernetes |

Avoid adding technologies just because they are popular.

The objective is depth, not technology collection.

---

# 6. Skills Covered

## Java

```text
OOP
SOLID
Generics
Collections
Streams
Functional programming
Exceptions
Concurrency
Threads
Executors
Futures
CompletableFuture
Locks
Atomics
JVM
GC
JIT
Memory
```

## Spring Boot

```text
Dependency Injection
AOP
Transactions
Spring Data
JPA/Hibernate
Security
Validation
Caching
Scheduling
Actuator
Async processing
Configuration
Profiles
Testing
```

## Database

```text
Schema design
Normalization
Indexes
Composite indexes
Query plans
Transactions
MVCC
Isolation
Locks
Deadlocks
Optimistic locking
Pessimistic locking
Connection pools
Replication
```

## Distributed Systems

```text
Kafka
Eventual consistency
Idempotency
Retries
Backoff
Jitter
DLQ
Ordering
Consumer groups
Partitions
Consumer lag
Distributed locks
Rate limiting
Circuit breakers
Bulkheads
Load balancing
Service discovery
Failure recovery
Distributed transactions
Saga
```

## Engineering

```text
Observability
Logging
Metrics
Tracing
Profiling
Performance testing
Load testing
Debugging
Chaos testing
```

## Infrastructure

```text
Docker
Docker Compose
Kubernetes
CI/CD
Health checks
Horizontal scaling
Configuration
Secrets
```

---

# 7. Engineering Challenges

Do not treat the project as:

> Phase 1 → finish → Phase 2 → finish → Phase 3.

Each phase should contain challenges where the design must be reasoned about before implementation.

## Challenge 1 — Inventory race condition

> 100 users attempt to purchase the last 10 items simultaneously.

Design and implement a solution.

---

## Challenge 2 — Distributed scheduler

> Two application instances execute the same scheduled job.

Prevent duplicate execution.

---

## Challenge 3 — Duplicate Kafka event

> Kafka delivers the same `OrderCreated` event three times.

Prevent duplicate effects.

---

## Challenge 4 — Payment uncertainty

> Payment succeeds, but your application crashes before recording success.

Determine how the system recovers.

---

## Challenge 5 — Slow provider

> Notification provider takes 30 seconds to respond.

Prevent the entire application from becoming unavailable.

---

## Challenge 6 — Noisy customer

> One customer submits 1 million jobs.

Prevent them from starving other customers.

---

## Challenge 7 — Database degradation

> PostgreSQL becomes 10× slower.

Find out why using observability rather than guessing.

---

## Challenge 8 — Worker crash

> A worker crashes after performing the actual job but before acknowledging the message.

Determine what happens next.

---

## Challenge 9 — Massive worker fleet

> You now have 100 worker instances.

Identify what breaks and how to redesign it.

---

## Challenge 10 — Large-scale redesign

> The system needs to process 100 million jobs/day.

Redesign the system and justify your trade-offs.

---

# 8. Final Capstone

At the end, design Forge for:

> **100 million jobs/day, correct inventory under extreme concurrency, reliable notifications, individual service failures, and complete end-to-end observability.**

Produce:

```text
1. Requirements
2. Domain model
3. API design
4. Database schema
5. Architecture
6. Data flow
7. Concurrency strategy
8. Consistency strategy
9. Failure strategy
10. Scaling strategy
11. Observability strategy
12. Security strategy
13. Deployment architecture
14. Trade-offs
```

The final design should be something you can defend technically.

---

# 9. How to Execute the Project

Do not start by creating the full final architecture.

Start with **Phase 0**.

Write:

```text
/docs
    requirements.md
    domain-model.md
    architecture.md
    decisions/
```

Your first engineering task is to define the domain and requirements.

Do not introduce:

- Kafka
- Redis
- Microservices
- Kubernetes
- API Gateway

until the project has a reason to need them.

The architecture should emerge from the problems.

---

# 10. Desired Learning Outcome

The project should transform your thinking from:

> "I know Spring Boot."

to:

> "I understand why Spring Boot applications behave the way they do."

And eventually:

> "I can reason about a backend system before writing the code."

The most valuable skill is not knowing Kafka, Redis, Kubernetes or Spring annotations individually.

It is being able to reason through:

```text
Requirement
    ↓
Domain
    ↓
Correctness
    ↓
Concurrency
    ↓
Consistency
    ↓
Failure
    ↓
Performance
    ↓
Observability
    ↓
Scalability
    ↓
Architecture
```

That is the central objective of Forge.
