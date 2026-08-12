# Forge Commerce Platform --- Phase 0

Forge is a production-oriented distributed commerce backend combining
**distributed inventory/order processing**, **distributed job
processing**, and **reliable notifications**.

The goal is not CRUD. The project is designed to develop practical
ability in Java, Spring Boot, databases, concurrency, distributed
systems, system design, failure handling, observability, performance and
security.

## Core capabilities

-   Product/catalog management
-   Concurrent inventory reservation and release
-   Order lifecycle and cancellation
-   Simulated payment with ambiguous outcomes
-   Immediate, delayed and recurring jobs
-   Worker pools, retries, priorities and dead-letter handling
-   Email/SMS/push notification abstraction
-   Provider failover
-   Kafka-based asynchronous events
-   Redis for justified caching, coordination and rate limiting
-   PostgreSQL for durable transactional state
-   Metrics, logs and distributed tracing
-   Docker and later Kubernetes

## Engineering philosophy

The architecture evolves from requirements:

``` text
Requirements
  ↓
Domain model
  ↓
Modular monolith
  ↓
Database correctness
  ↓
Concurrency
  ↓
Async jobs
  ↓
Redis
  ↓
Kafka
  ↓
Distributed workflows
  ↓
Failure engineering
  ↓
Observability
  ↓
Performance
  ↓
Service decomposition
  ↓
Kubernetes
```

For every major capability:

``` text
Build → Break → Investigate → Redesign → Rebuild
```

## Actors

### Customer

Browse products, create/cancel eligible orders, view orders, receive
notifications.

### Administrator

Manage products/inventory and inspect orders, jobs and failed
notifications.

### Worker

Claims and executes asynchronous work, records attempts, retries
failures and recovers safely.

### External provider

Payment, email, SMS or push provider. Providers are assumed to be
unreliable.

## Initial domain

``` text
User
Product
Inventory
InventoryReservation
Order
OrderItem
Payment
Job
JobAttempt
Notification
NotificationAttempt
Provider
DomainEvent
```

## Important invariants

-   Inventory must never become negative.
-   The system must never successfully oversell inventory.
-   Duplicate logical order requests must not create duplicate orders.
-   Duplicate events must not create duplicate business effects.
-   A completed order cannot return to CREATED.
-   A consumed reservation cannot be released.
-   A timeout is not automatically equivalent to failure when an
    external operation may have succeeded.
-   Accepted jobs must not silently disappear.
-   One customer must not monopolize all worker capacity.
-   Recovery operations must themselves be safe to retry.

## V1 out of scope

Real payments, real SMS/email/push delivery, shipping, tax,
recommendations, advanced search, promotions, multi-currency and
marketplace functionality.

## Phase 0 exit criteria

Before coding, the repository must contain:

-   requirements
-   use cases
-   business rules/invariants
-   domain model
-   state machines
-   non-functional requirements
-   system constraints/failure model
-   glossary
-   architecture attack scenarios
-   at least one ADR

The project should deliberately leave implementation questions
unresolved where investigation is more valuable than premature
decisions.
