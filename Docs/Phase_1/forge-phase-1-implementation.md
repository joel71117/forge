# Forge --- Phase 1 Implementation

## Java Domain Foundation

**Objective:** Turn the Phase 0 specification into a clean, executable
Java domain model before introducing Spring Boot, PostgreSQL, Kafka,
Redis, HTTP APIs, Docker, or Kubernetes.

------------------------------------------------------------------------

## 1. Why Phase 1 Exists

Phase 1 is deliberately **pure Java**.

It should improve your understanding of:

-   encapsulation and object design
-   immutability
-   value objects
-   entities and aggregates
-   collections
-   generics
-   streams
-   lambdas and functional interfaces
-   exception design
-   equality and hashing
-   state machines
-   interfaces
-   design patterns
-   unit testing
-   package boundaries
-   JVM/object behavior

The goal is not to create many classes. The goal is to make the domain
model enforce meaningful business rules.

The central principle is:

> Make invalid domain states difficult or impossible to represent.

------------------------------------------------------------------------

## 2. Explicitly Out of Scope

Do **not** add:

-   Spring Boot
-   Spring Data JPA
-   Hibernate
-   PostgreSQL
-   Redis
-   Kafka
-   REST controllers
-   HTTP
-   JWT
-   Spring Security
-   Docker/Kubernetes
-   external providers
-   executors
-   CompletableFuture

Phase 1 should run as an ordinary Java project:

``` text
Java
  ↓
Domain model
  ↓
Application services
  ↓
Unit tests
```

------------------------------------------------------------------------

## 3. Package Structure

Use explicit domain boundaries:

``` text
src/main/java/com/forge/commerce/

├── common/
├── user/
├── catalog/
├── inventory/
├── order/
├── payment/
├── job/
└── notification/
```

Tests should mirror the structure.

Do not create packages merely because a conventional architecture
diagram contains them.

------------------------------------------------------------------------

# 4. Implementation Order

Implement in this order:

``` text
1. Common IDs/value objects
2. Money
3. Quantity
4. User
5. Product
6. Inventory
7. Reservation
8. OrderItem
9. Order
10. Payment
11. Job
12. Notification
13. Domain exceptions
14. Repository interfaces
15. In-memory repositories
16. Application services
17. Domain event types
18. Tests and verification
```

Build one meaningful slice at a time.

------------------------------------------------------------------------

# 5. Common Identifiers

Create strongly typed identifiers:

``` text
UserId
ProductId
OrderId
ReservationId
PaymentId
JobId
NotificationId
EventId
```

Use UUID internally.

Do not create a generic identifier abstraction unless it genuinely
improves the design.

The purpose is type safety:

``` text
OrderId != ProductId
```

even though both may wrap UUID.

Prefer immutable identifier objects.

------------------------------------------------------------------------

# 6. User Domain

Create:

``` text
User
UserRole
UserStatus
UserId
```

Roles:

``` text
CUSTOMER
ADMIN
OPERATIONS
```

Statuses:

``` text
ACTIVE
SUSPENDED
DELETED
```

The user object should reject invalid identity data.

Customers and administrators must be distinguishable by role, but
authorization logic itself belongs to a later application/security
layer.

------------------------------------------------------------------------

# 7. Product Domain

Create:

``` text
Product
ProductStatus
Sku
Money
ProductId
```

Product attributes:

``` text
ProductId id
Sku sku
String name
String description
Money price
ProductStatus status
```

Product statuses:

``` text
ACTIVE
INACTIVE
DISCONTINUED
```

Rules:

-   ID cannot be null.
-   SKU cannot be blank.
-   Name cannot be blank.
-   Price cannot be negative.
-   Product status must be valid.
-   Inactive/discontinued products cannot be newly ordered.

Do not add public setters.

------------------------------------------------------------------------

# 8. Value Object: Sku

Create a small immutable value object.

It should:

-   reject null
-   reject blank input
-   implement value-based equality
-   have stable hashCode
-   have useful `toString()`

Do not make it a mutable wrapper around String.

------------------------------------------------------------------------

# 9. Value Object: Money

Money must not use:

``` java
double
float
```

Use:

``` text
BigDecimal amount
Currency currency
```

Support:

``` text
add
subtract
multiply
compareTo
```

Rules:

-   amount cannot be null
-   currency cannot be null
-   negative money should be rejected where the domain does not allow it
-   currency mismatch must be handled explicitly
-   arithmetic must use decimal semantics

Think carefully about:

``` text
10 USD + 5 USD
10 USD + 5 EUR
10 USD × 3
10.00 USD == 10.0 USD
```

Document your decisions.

------------------------------------------------------------------------

# 10. Value Object: Quantity

Create `Quantity`.

It should represent a strictly positive quantity:

``` text
1, 2, 3 ... valid
0 invalid
-1 invalid
```

This eliminates repeated validation across order and inventory code.

------------------------------------------------------------------------

# 11. IdempotencyKey

Create an immutable:

``` text
IdempotencyKey
```

It should reject:

-   null
-   blank strings

Phase 1 only models the concept.

Duplicate detection will be implemented later with persistence.

------------------------------------------------------------------------

# 12. Inventory

Create:

``` text
Inventory
```

Conceptual state:

``` text
ProductId
availableQuantity
reservedQuantity
```

Expose behavior rather than setters:

``` text
increase()
reserve()
release()
consume()
```

Enforce:

``` text
availableQuantity >= 0
reservedQuantity >= 0
```

and:

``` text
cannot reserve more than available
```

The exact representation is deliberately simple in Phase 1.

Do not attempt distributed locking yet.

------------------------------------------------------------------------

# 13. Inventory Reservation

Create:

``` text
InventoryReservation
ReservationId
ReservationStatus
```

Attributes:

``` text
ReservationId id
OrderId orderId
ProductId productId
Quantity quantity
ReservationStatus status
Instant expiresAt
```

States:

``` text
PENDING
RESERVED
CONSUMED
RELEASED
EXPIRED
CANCELLED
```

Use explicit methods:

``` text
reserve()
consume()
release()
expire()
cancel()
```

Do not expose:

``` text
setStatus(...)
```

Test every important legal and illegal transition.

------------------------------------------------------------------------

# 14. OrderItem

Create:

``` text
OrderItem
OrderItemId
ProductId
Quantity
Money unitPrice
```

The item should calculate:

``` text
subtotal = unitPrice × quantity
```

Use BigDecimal-backed Money.

The item should preserve the historical purchase price rather than
reading the current product price later.

------------------------------------------------------------------------

# 15. Order Aggregate

Create:

``` text
Order
OrderItem
OrderStatus
OrderId
```

Attributes:

``` text
OrderId
CustomerId
List<OrderItem>
OrderStatus
Currency
Money total
IdempotencyKey
```

States:

``` text
CREATED
CONFIRMED
PROCESSING
COMPLETED
FAILED
CANCELLED
```

Behavior:

``` text
addItem()
removeItem()
calculateTotal()
confirm()
startProcessing()
complete()
fail()
cancel()
```

Rules:

-   an order needs at least one item before confirmation
-   quantities must be positive
-   invalid state transitions fail
-   completed orders cannot return to CREATED
-   completed orders cannot normally be cancelled
-   order totals must be internally consistent

The aggregate should control its collection.

Do not return a mutable internal list that lets callers bypass
invariants.

------------------------------------------------------------------------

# 16. Payment

Create:

``` text
Payment
PaymentStatus
PaymentId
```

States:

``` text
PENDING
PROCESSING
SUCCEEDED
FAILED
UNKNOWN
CANCELLED
```

Behavior:

``` text
startProcessing()
succeed()
fail()
markUnknown()
cancel()
```

Important rule:

``` text
UNKNOWN != FAILED
```

A timeout can create uncertainty without proving failure.

Do not model provider HTTP behavior yet.

------------------------------------------------------------------------

# 17. Job

Create:

``` text
Job
JobStatus
JobPriority
JobType
JobId
```

Statuses:

``` text
QUEUED
RUNNING
COMPLETED
FAILED
RETRYING
CANCELLED
DEAD_LETTERED
```

Priorities:

``` text
HIGH
NORMAL
LOW
```

Initial job types can be:

``` text
SEND_NOTIFICATION
RECONCILE_PAYMENT
EXPIRE_RESERVATION
GENERATE_REPORT
```

Attributes:

``` text
JobId
JobType
TenantId
payload
priority
status
scheduledAt
retryCount
maxRetries
nextAttemptAt
```

Do not build the scheduler yet.

------------------------------------------------------------------------

# 18. Job State Machine

Valid transitions:

``` text
QUEUED → RUNNING
RUNNING → COMPLETED
RUNNING → FAILED
FAILED → RETRYING
RETRYING → QUEUED
FAILED → DEAD_LETTERED
QUEUED → CANCELLED
```

Invalid transitions must fail explicitly.

For example:

``` text
COMPLETED → QUEUED      invalid
COMPLETED → RUNNING     invalid
CANCELLED → RUNNING     invalid
```

------------------------------------------------------------------------

# 19. RetryPolicy

Create a small abstraction:

``` text
RetryPolicy
```

It should represent:

``` text
maximumAttempts
baseDelay
maximumDelay
```

Do not implement the complete distributed retry system yet.

The purpose is to prepare a clean domain abstraction that later phases
can extend with:

``` text
exponential backoff
jitter
retryable exceptions
dead-letter handling
```

------------------------------------------------------------------------

# 20. Notification

Create:

``` text
Notification
NotificationChannel
NotificationStatus
NotificationPriority
NotificationId
```

Channels:

``` text
EMAIL
SMS
PUSH
```

Statuses:

``` text
PENDING
PROCESSING
SENT
FAILED
RETRYING
DEAD_LETTERED
CANCELLED
```

Behavior:

``` text
startProcessing()
markSent()
markFailed()
scheduleRetry()
deadLetter()
cancel()
```

Do not add actual provider implementations.

------------------------------------------------------------------------

# 21. Job Attempt and Notification Attempt

Create these as separate domain concepts.

## JobAttempt

``` text
id
jobId
workerId
attemptNumber
startedAt
finishedAt
status
errorCode
errorMessage
```

## NotificationAttempt

``` text
id
notificationId
provider
attemptNumber
startedAt
finishedAt
status
providerReference
errorCode
errorMessage
```

The important design idea is:

``` text
Logical operation
        ↓
multiple attempts
```

This distinction becomes important when retries and worker crashes are
introduced.

------------------------------------------------------------------------

# 22. Domain Exceptions

Create a small meaningful exception hierarchy.

Candidates:

``` text
InvalidStateTransitionException
InsufficientInventoryException
InvalidQuantityException
InvalidMoneyException
InvalidSkuException
InvalidIdempotencyKeyException
OrderNotCancellableException
```

Do not create hundreds of exception classes.

Understand the distinction between:

``` text
domain rule violation
programming error
infrastructure failure
external failure
```

Phase 1 should primarily contain domain failures.

------------------------------------------------------------------------

# 23. Equality and Hashing

Investigate and explicitly decide equality semantics.

For value objects:

``` text
Money
Sku
Quantity
Ids
IdempotencyKey
```

use value equality.

For entities, do not blindly include every mutable field in `equals()`
and `hashCode()`.

Think about:

``` text
What identifies this object?
Can that identity change?
Can this object safely be used as a HashMap key?
```

Write tests for the chosen behavior.

------------------------------------------------------------------------

# 24. Immutability

Prefer immutable:

``` text
IDs
Money
Sku
Quantity
IdempotencyKey
```

Entities can have mutable lifecycle state, but mutation must happen
through domain behavior.

Avoid public setters.

Instead of:

``` java
order.setStatus(COMPLETED);
```

use:

``` java
order.complete();
```

This is one of the most important design decisions of Phase 1.

------------------------------------------------------------------------

# 25. Collections

Use collections deliberately.

For Order:

``` text
List<OrderItem>
```

Investigate:

-   whether callers can mutate the internal collection
-   whether duplicates are allowed
-   whether lookup should be by product
-   whether insertion order matters
-   time complexity of common operations

Prefer read-only views to leaking mutable internal collections.

------------------------------------------------------------------------

# 26. Generics

Use generics where a real abstraction exists.

Possible future candidates:

``` text
Repository<T, ID>
DomainEventHandler<T>
JobHandler<T>
```

Do not create generic abstractions just to demonstrate generics.

During Phase 1, explicitly review:

-   generic classes
-   generic methods
-   bounded types
-   `? extends`
-   `? super`
-   type inference
-   type erasure

------------------------------------------------------------------------

# 27. Functional Interfaces and Lambdas

Use a few meaningful functional abstractions.

Candidates:

``` text
DomainEventHandler<T>
RetryPredicate
JobHandler
```

Know when these are appropriate:

``` text
Function
Predicate
Consumer
Supplier
```

Do not replace ordinary readable code with lambdas merely because it is
possible.

------------------------------------------------------------------------

# 28. Streams

Use streams selectively.

A good candidate is order total calculation:

``` text
items
  → map subtotal
  → reduce into Money
```

But also understand the imperative alternative.

Investigate:

-   lazy evaluation
-   intermediate operations
-   terminal operations
-   collectors
-   side effects
-   debugging
-   parallel streams

Do not use `parallelStream()` in Phase 1 as an optimization.

------------------------------------------------------------------------

# 29. Optional

Use `Optional` when absence is meaningful, especially as a return type.

Avoid:

``` text
Optional fields
Optional parameters
Optional everywhere
```

Understand why.

------------------------------------------------------------------------

# 30. Design Patterns to Learn Through the Project

Use patterns only when they solve an actual problem.

### Value Object

``` text
Money
Sku
Quantity
IDs
```

### State Pattern / Controlled State Transitions

``` text
Order
Reservation
Payment
Job
Notification
```

### Strategy

Potentially:

``` text
RetryPolicy
JobHandler
```

### Factory

Only if object creation becomes complex.

### Registry

Potentially later:

``` text
JobType → JobHandler
```

Do not build a framework for patterns.

------------------------------------------------------------------------

# 31. Domain Services

Use a domain service only when behavior genuinely spans multiple
aggregates.

Possible examples:

``` text
InventoryReservationService
OrderPricingService
```

Do not turn every entity method into a service method.

Avoid the anti-pattern:

``` text
OrderService
    containing every piece of order logic
```

while `Order` becomes an anemic DTO.

------------------------------------------------------------------------

# 32. Repository Interfaces

Introduce repository abstractions only at the application/domain
boundary.

Potential interfaces:

``` text
ProductRepository
InventoryRepository
OrderRepository
PaymentRepository
JobRepository
NotificationRepository
```

Phase 1 implementations may be:

``` text
InMemoryProductRepository
InMemoryOrderRepository
...
```

Later:

``` text
JPA implementation
```

The domain must not know about JPA.

------------------------------------------------------------------------

# 33. Application Services

Create orchestration services such as:

``` text
CreateOrderService
ReserveInventoryService
CancelOrderService
ProcessPaymentService
SubmitJobService
SendNotificationService
```

Their responsibility is to coordinate objects and repositories.

They should not contain:

-   SQL
-   HTTP
-   Kafka
-   Redis
-   Spring annotations

------------------------------------------------------------------------

# 34. Domain Events

Introduce simple event types representing business facts.

Examples:

``` text
OrderCreated
InventoryReserved
InventoryReleased
PaymentSucceeded
PaymentFailed
OrderCompleted
NotificationRequested
JobCompleted
```

Create a common event envelope if useful:

``` text
eventId
aggregateId
aggregateType
occurredAt
version
correlationId
causationId
payload
```

Do not publish to Kafka yet.

------------------------------------------------------------------------

# 35. Testing Strategy

Tests should verify **behavior and invariants**, not private
implementation details.

Example:

``` text
Given inventory = 10
When reserving 4
Then available = 6
And reserved = 4
```

Prefer this over:

``` text
verify private field was assigned 6
```

------------------------------------------------------------------------

# 36. Required Value Object Tests

Test:

### IDs

-   valid UUID
-   null rejected
-   equality
-   hashCode

### Money

-   positive amount
-   zero
-   invalid negative amount
-   addition
-   subtraction
-   multiplication
-   currency mismatch
-   equality

### Quantity

-   positive values
-   zero rejected
-   negative rejected

### Sku

-   valid
-   blank rejected
-   equality

### IdempotencyKey

-   valid
-   blank rejected
-   equality

------------------------------------------------------------------------

# 37. Required Product Tests

Test:

-   valid creation
-   invalid SKU
-   blank name
-   invalid price
-   product status
-   lifecycle behavior
-   equality semantics if applicable

------------------------------------------------------------------------

# 38. Required Inventory Tests

At minimum:

``` text
reserve available quantity
reserve exact remaining quantity
reject insufficient quantity
increase inventory
release reservation
consume reservation
reject invalid release
reject invalid consume
```

After every operation verify the inventory invariants.

------------------------------------------------------------------------

# 39. Required Order Tests

Test:

``` text
create valid order
add item
remove item
calculate total
reject confirmation with no items
confirm
start processing
complete
fail
cancel
reject illegal transitions
```

Every important legal and illegal state transition should have a test.

------------------------------------------------------------------------

# 40. Required Payment Tests

Test:

``` text
PENDING → PROCESSING
PROCESSING → SUCCEEDED
PROCESSING → FAILED
PROCESSING → UNKNOWN
invalid transitions
```

Explicitly verify:

``` text
UNKNOWN != FAILED
```

------------------------------------------------------------------------

# 41. Required Job Tests

Test:

``` text
create queued job
start
complete
fail
retry
maximum attempts
dead-letter
cancel
invalid transitions
```

Test boundary conditions around retry counts.

------------------------------------------------------------------------

# 42. Required Notification Tests

Test:

``` text
create
start processing
send
fail
retry
dead-letter
cancel
invalid transitions
```

------------------------------------------------------------------------

# 43. Repository Tests

For in-memory repositories test:

``` text
save
findById
duplicate ID behavior
delete if supported
```

Do not test that the repository internally uses a HashMap.

Test the contract.

------------------------------------------------------------------------

# 44. Application Service Tests

For `CreateOrderService`, test orchestration such as:

``` text
product lookup
inventory reservation
order creation
order persistence
```

Test failure of each dependency.

Use simple test doubles first. Mockito may be used when it genuinely
improves a test, but do not make mocking the architecture.

------------------------------------------------------------------------

# 45. Mutation-Style Test Exercise

After tests pass, intentionally introduce bugs.

Examples:

``` text
inventory -= quantity - 1
```

or:

``` text
allow COMPLETED → CANCELLED
```

or:

``` text
allow retryCount == maxRetries + 1
```

Your tests should catch the bug.

This is more useful than chasing a coverage percentage.

------------------------------------------------------------------------

# 46. Java Learning Checklist

During implementation, deliberately investigate:

## Collections

-   ArrayList vs LinkedList
-   HashMap internals
-   HashSet
-   immutable collections
-   iteration
-   concurrent collections conceptually

## Generics

-   generic classes
-   generic methods
-   bounds
-   wildcards
-   extends
-   super
-   type erasure

## Streams

-   lazy execution
-   terminal operations
-   collectors
-   side effects
-   parallel streams

## Functional Java

-   functional interfaces
-   lambdas
-   method references
-   composition

## Exceptions

-   checked vs unchecked
-   domain exceptions
-   exception hierarchy
-   exception translation

## JVM

Start investigating:

``` text
stack
heap
object references
allocation
GC roots
young/old generations
JIT
escape analysis
```

Do not try to master the JVM in Phase 1. Establish the foundation for
deeper experiments later.

------------------------------------------------------------------------

# 47. Cognitive Exercises

Before moving on, answer these in your own words:

1.  Why is `Money` a value object?
2.  Why should `Order.status` not have a public setter?
3.  Should `OrderItem` be mutable?
4.  Why should an aggregate control its collection?
5.  Why wrap UUID in `OrderId`?
6.  What makes two Orders equal?
7.  Should `Money(10, USD)` equal `Money(10.00, USD)`?
8.  What happens when two orders reserve the last inventory unit?
9.  Which rule belongs inside Inventory?
10. Which behavior belongs in an application service?
11. Why does the domain not depend on Spring?
12. Why will in-memory inventory not be enough for later distributed
    execution?

Write down your answers. Do not just keep them in your head.

------------------------------------------------------------------------

# 48. Git Strategy

Use small feature-oriented commits.

Suggested sequence:

``` text
feat(domain): add common identifiers and value objects
feat(domain): add product model
feat(domain): add inventory and reservation model
feat(domain): add order aggregate
feat(domain): add payment model
feat(domain): add job model
feat(domain): add notification model
feat(domain): add repository abstractions
feat(domain): add application services
test(domain): strengthen invariant coverage
```

Keep tests with the feature they validate unless separating them makes
the history clearer.

------------------------------------------------------------------------

# 49. Definition of Done

Phase 1 is complete when:

-   [ ] Core identifiers exist.
-   [ ] Money and Quantity are value objects.
-   [ ] SKU and IdempotencyKey are value objects.
-   [ ] Product exists.
-   [ ] Inventory exists.
-   [ ] Reservation state machine is enforced.
-   [ ] Order aggregate exists.
-   [ ] Order state machine is enforced.
-   [ ] Payment state machine is enforced.
-   [ ] Job state machine is enforced.
-   [ ] Notification state machine is enforced.
-   [ ] Invalid transitions have tests.
-   [ ] Equality/hashCode semantics are deliberate.
-   [ ] No public setters bypass important invariants.
-   [ ] Domain classes have no Spring dependencies.
-   [ ] Domain classes have no JPA dependencies.
-   [ ] Domain classes have no Kafka/Redis dependencies.
-   [ ] Repository abstractions hide persistence.
-   [ ] Application services orchestrate rather than own all business
    rules.
-   [ ] Tests verify behavior.
-   [ ] Mutation-style exercises expose weak tests.
-   [ ] Clean build passes.
-   [ ] Static analysis passes.
-   [ ] Formatting passes.
-   [ ] All tests pass.

------------------------------------------------------------------------

# 50. Phase 1 Exit Review

You should be able to explain:

## Java

-   How HashMap works at a high level.
-   `equals()` and `hashCode()`.
-   Type erasure.
-   `? extends` vs `? super`.
-   Stream laziness.
-   Why parallel streams are not a default optimization.
-   Stack vs heap.
-   Reachability and garbage collection.
-   Immutable vs mutable objects.

## Domain Design

-   Entity vs value object.
-   Aggregate.
-   Aggregate invariant.
-   Domain service.
-   Application service.
-   Repository abstraction.
-   State transition.
-   Idempotency concept.

## Architecture

-   Why Forge starts as a modular monolith.
-   Why domain code should not depend on Spring.
-   Which problems Phase 1 solves.
-   Which problems require a database.
-   Which problems require concurrency control.
-   Which problems require distributed messaging.

If you cannot answer everything, document the gaps. Those gaps become
explicit learning goals for the next phase.

------------------------------------------------------------------------

# 51. Expected Phase 1 Architecture

At the end:

``` text
                    Application Services
                            │
                            ▼
                     Domain Model
                 ┌──────────┴──────────┐
                 ▼                     ▼
           Repository Ports       Domain Events
                 │
                 ▼
        In-Memory Implementations
```

There should still be no infrastructure framework in the domain.

------------------------------------------------------------------------

# 52. Transition to Phase 2

Phase 2 should introduce Spring Boot **without destroying the domain
model**.

The next architectural step becomes:

``` text
HTTP
 ↓
Spring Controller
 ↓
Application Service
 ↓
Domain Model
 ↓
Repository Interface
 ↓
In-Memory Implementation
```

Only after that should persistence be introduced.

The most important rule of Phase 1 is:

> Do not rush to Spring. Build a domain model that is good enough that
> Spring has something meaningful to support.
