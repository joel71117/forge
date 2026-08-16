# Forge --- Phase 2 Implementation

## Spring Boot Application Architecture

**Objective:** Introduce Spring Boot around the Phase 1 domain model
without coupling the domain to Spring. Learn dependency injection,
application architecture, REST APIs, validation, exception handling,
configuration, Actuator, observability basics, and Spring testing.

------------------------------------------------------------------------

## 1. Phase 2 Goal

Phase 1 established pure Java domain logic.

Phase 2 turns it into a real application:

``` text
HTTP
 ↓
REST Controller
 ↓
Application Service
 ↓
Domain Model
 ↓
Repository Interface
 ↓
In-Memory Repository
```

Spring supports this architecture; Spring does not define the business
architecture.

------------------------------------------------------------------------

## 2. Learning Objectives

By the end of Phase 2, understand and implement:

### Spring Boot

-   application startup
-   component scanning
-   auto-configuration
-   dependency injection
-   bean lifecycle
-   bean scopes
-   configuration
-   profiles
-   `@ConfigurationProperties`
-   Actuator
-   health/readiness/liveness
-   graceful shutdown basics

### Architecture

-   controller/application/domain boundaries
-   DTOs versus domain objects
-   dependency inversion
-   repository ports and adapters
-   application services
-   domain services
-   infrastructure boundaries

### REST

-   resource-oriented API design
-   HTTP methods and status codes
-   request/response DTOs
-   validation
-   error contracts
-   idempotency
-   pagination and filtering
-   API versioning
-   OpenAPI documentation

### Testing

-   unit tests
-   application-service tests
-   controller tests
-   Spring context tests
-   integration-test boundaries
-   test doubles

### Operations

-   Actuator
-   structured logging
-   correlation IDs
-   JVM/thread observation
-   basic performance measurement

------------------------------------------------------------------------

# 3. Explicitly Out of Scope

Do not introduce:

-   PostgreSQL
-   JPA/Hibernate
-   Redis
-   Kafka
-   Kubernetes
-   microservices
-   distributed locks
-   distributed transactions
-   real external providers
-   full authentication/JWT

Phase 2 uses in-memory persistence.

This is deliberate: the next phase will introduce PostgreSQL and force
you to solve real persistence and transaction problems.

------------------------------------------------------------------------

# 4. Target Architecture

``` text
                    HTTP
                     │
                     ▼
              REST Controllers
                     │
                     ▼
             Application Services
                     │
             ┌───────┴────────┐
             ▼                ▼
        Domain Model     Repository Ports
                              │
                              ▼
                    In-Memory Adapters
```

Dependency direction:

``` text
api → application → domain
                       ↑
                       │
               infrastructure
```

The domain must not depend on Spring.

------------------------------------------------------------------------

# 5. Package Structure

Recommended:

``` text
com.forge.commerce

├── common
├── user
│   ├── domain
│   ├── application
│   └── api
├── catalog
│   ├── domain
│   ├── application
│   └── api
├── inventory
│   ├── domain
│   ├── application
│   └── api
├── order
│   ├── domain
│   ├── application
│   └── api
├── payment
│   ├── domain
│   ├── application
│   └── api
├── job
│   ├── domain
│   ├── application
│   └── api
├── notification
│   ├── domain
│   ├── application
│   └── api
└── infrastructure
    ├── configuration
    ├── persistence
    └── observability
```

Do not create empty architectural packages just to satisfy a diagram.

------------------------------------------------------------------------

# 6. Spring Boot Setup

Add:

``` text
Spring Web
Spring Validation
Spring Boot Actuator
Spring Boot Test
```

Optional:

``` text
Spring Boot DevTools
```

Do not add database or messaging dependencies yet.

------------------------------------------------------------------------

# 7. Application Entry Point

Create `ForgeApplication`.

Its responsibility is bootstrapping only.

Do not put:

-   business logic
-   repository logic
-   configuration logic
-   startup workflows

inside the main class.

------------------------------------------------------------------------

# 8. Dependency Injection

Use constructor injection.

Preferred:

``` java
public CreateOrderService(
        OrderRepository orderRepository,
        InventoryRepository inventoryRepository) {
    ...
}
```

Avoid field injection:

``` java
@Autowired
private OrderRepository repository;
```

Understand why constructor injection gives:

-   explicit dependencies
-   stronger construction guarantees
-   easier unit testing
-   better immutability
-   less framework coupling

------------------------------------------------------------------------

# 9. Spring Beans

Understand:

``` text
@Component
@Service
@Repository
@RestController
@Configuration
@Bean
```

Know what each annotation actually does rather than using them
mechanically.

Understand:

``` text
component scanning
bean registration
dependency resolution
bean creation
```

------------------------------------------------------------------------

# 10. Application Services

Convert Phase 1 orchestration classes into Spring beans.

Examples:

``` text
CreateProductService
GetProductService
CreateOrderService
GetOrderService
CancelOrderService
ReserveInventoryService
SubmitJobService
GetJobService
CreateNotificationService
```

Application services coordinate operations.

They must not become giant classes containing every business rule.

------------------------------------------------------------------------

# 11. Domain Protection

A controller must not bypass the domain.

Bad:

``` text
Controller
 ↓
repository.save(orderWithStatusFromRequest)
```

Good:

``` text
Controller
 ↓
Application Service
 ↓
Order.confirm()
```

The API must never be able to directly assign an arbitrary domain state.

------------------------------------------------------------------------

# 12. DTOs

Do not expose domain entities directly through HTTP.

Create:

``` text
CreateProductRequest
ProductResponse

CreateOrderRequest
OrderResponse
OrderItemRequest
OrderItemResponse

SubmitJobRequest
JobResponse

CreateNotificationRequest
NotificationResponse
```

Remember:

``` text
API contract != domain model
```

The API should evolve independently of internal object structure.

------------------------------------------------------------------------

# 13. DTO Mapping

Keep mapping explicit.

For example:

``` text
CreateProductRequest
        ↓
Sku
Money
Product
```

and:

``` text
Product
        ↓
ProductResponse
```

Do not let JSON serialization accidentally become your domain mapping
strategy.

------------------------------------------------------------------------

# 14. Product API

Implement:

``` http
POST   /api/v1/products
GET    /api/v1/products/{id}
GET    /api/v1/products
PATCH  /api/v1/products/{id}
```

Only expose operations supported by the domain.

------------------------------------------------------------------------

# 15. Inventory API

Implement behavior-oriented operations such as:

``` http
GET  /api/v1/products/{productId}/inventory
POST /api/v1/products/{productId}/inventory
```

Reservation operations can be:

``` http
POST /api/v1/inventory/reservations
POST /api/v1/inventory/reservations/{id}/release
POST /api/v1/inventory/reservations/{id}/consume
```

Avoid a generic endpoint that lets clients arbitrarily mutate inventory
state.

------------------------------------------------------------------------

# 16. Order API

Implement:

``` http
POST /api/v1/orders
GET  /api/v1/orders/{id}
GET  /api/v1/orders
POST /api/v1/orders/{id}/cancel
```

Cancellation is intentionally represented as an operation rather than:

``` text
PATCH status=CANCELLED
```

This prevents the API from becoming a backdoor around the state machine.

------------------------------------------------------------------------

# 17. Idempotency Header

Order creation should accept:

``` http
Idempotency-Key: <key>
```

Phase 2 can implement the behavior in memory.

The durable implementation comes later.

You must understand the failure case:

``` text
Client
  ↓
POST order
  ↓
Server creates order
  ↓
response lost
  ↓
client retries
```

The second request must be recognized as the same logical operation.

------------------------------------------------------------------------

# 18. Job API

Implement:

``` http
POST /api/v1/jobs
GET  /api/v1/jobs/{id}
POST /api/v1/jobs/{id}/cancel
```

Submission returns after the job is accepted.

Do not execute jobs asynchronously yet.

The actual worker engine is a later phase.

------------------------------------------------------------------------

# 19. Notification API

Implement:

``` http
POST /api/v1/notifications
GET  /api/v1/notifications/{id}
POST /api/v1/notifications/{id}/cancel
```

Use fake/in-memory delivery behavior only.

------------------------------------------------------------------------

# 20. HTTP Status Codes

Use semantics deliberately.

Typical mappings:

``` text
200 OK
201 Created
204 No Content
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
429 Too Many Requests
500 Internal Server Error
503 Service Unavailable
```

Do not return `200` for every outcome.

Document important status-code decisions.

------------------------------------------------------------------------

# 21. Global Exception Handling

Create:

``` text
GlobalExceptionHandler
```

using:

``` java
@RestControllerAdvice
```

Handle:

``` text
validation errors
domain rule violations
not found
malformed requests
unexpected exceptions
```

Controllers should not contain large try/catch blocks.

------------------------------------------------------------------------

# 22. Error Response

Create one consistent error representation.

Recommended:

``` text
timestamp
status
code
message
path
traceId
details
```

Example:

``` json
{
  "timestamp": "...",
  "status": 409,
  "code": "INSUFFICIENT_INVENTORY",
  "message": "Insufficient inventory",
  "path": "/api/v1/orders",
  "traceId": "...",
  "details": []
}
```

Never expose stack traces to clients.

------------------------------------------------------------------------

# 23. Domain-to-HTTP Mapping

Define mappings explicitly.

Examples:

``` text
InsufficientInventoryException → 409
OrderNotCancellableException → 409
InvalidQuantityException → 400
ProductNotFoundException → 404
OrderNotFoundException → 404
```

The domain should not know that these become HTTP status codes.

The mapping belongs at the API boundary.

------------------------------------------------------------------------

# 24. Bean Validation

Use Jakarta Validation:

``` text
@NotNull
@NotBlank
@Positive
@PositiveOrZero
@Size
@Email
```

Validate request shape at the API boundary.

But remember:

> Bean Validation does not replace domain invariants.

The domain must remain valid even when created without HTTP.

------------------------------------------------------------------------

# 25. Custom Validation

Only introduce custom validators where standard constraints cannot
express the rule.

Do not create a custom validation framework.

------------------------------------------------------------------------

# 26. Validation Boundary

Understand:

``` text
HTTP validation
      ↓
Application validation
      ↓
Domain invariants
```

Example:

``` text
@NotBlank
```

is request validation.

But:

``` text
A discontinued product cannot be ordered
```

is domain/business logic.

------------------------------------------------------------------------

# 27. Configuration

Use externalized configuration.

Examples:

``` text
forge.order.idempotency.enabled
forge.inventory.reservation-duration
forge.job.default-max-retries
forge.notification.default-priority
```

Prefer `@ConfigurationProperties` for grouped configuration.

Avoid scattering `@Value` across dozens of classes.

------------------------------------------------------------------------

# 28. Profiles

Create:

``` text
application.yml
application-local.yml
application-test.yml
```

Use profiles deliberately.

Do not duplicate the complete configuration for every environment.

------------------------------------------------------------------------

# 29. Actuator

Add Spring Boot Actuator.

Expose useful endpoints such as:

``` text
/actuator/health
/actuator/info
/actuator/metrics
```

Do not expose every actuator endpoint publicly.

Learn:

``` text
health
liveness
readiness
metrics
application information
```

------------------------------------------------------------------------

# 30. Liveness vs Readiness

Understand the difference.

**Liveness:**

> Should this process remain alive?

**Readiness:**

> Should this instance receive traffic?

This distinction becomes essential when Forge later runs in Kubernetes.

------------------------------------------------------------------------

# 31. Logging

Use useful application logs for important lifecycle events:

``` text
order created
order cancelled
inventory reservation failed
job submitted
notification created
```

Do not log:

-   secrets
-   passwords
-   tokens
-   unnecessary sensitive data
-   huge request payloads

Prefer structured logs.

------------------------------------------------------------------------

# 32. Correlation ID

Introduce:

``` text
X-Correlation-Id
```

Behavior:

``` text
client provides ID
        ↓
use it

no ID
        ↓
generate one
```

Include it in logs.

Later this evolves into distributed tracing:

``` text
traceId
spanId
correlationId
causationId
```

------------------------------------------------------------------------

# 33. Filters and Interceptors

Implement correlation handling at the HTTP boundary.

Understand the difference between:

``` text
Filter
HandlerInterceptor
ControllerAdvice
AOP
```

Use the simplest appropriate mechanism.

Do not create an interceptor for every cross-cutting concern.

------------------------------------------------------------------------

# 34. API Versioning

Use:

``` text
/api/v1/...
```

from the beginning.

The goal is simply to make the external contract explicit.

Do not build complicated version negotiation yet.

------------------------------------------------------------------------

# 35. Pagination

Collection endpoints should have a basic pagination contract.

For example:

``` text
GET /api/v1/products?page=0&size=20
```

Use offset pagination for now.

Document that very large datasets may eventually require cursor/keyset
pagination.

------------------------------------------------------------------------

# 36. Filtering

Support meaningful filters:

``` text
product status
order status
job status
notification status
```

Do not build a generic search language.

------------------------------------------------------------------------

# 37. Sorting

Support a small set of explicit sortable fields.

For example:

``` text
createdAt
```

Do not expose arbitrary internal property names without consideration.

------------------------------------------------------------------------

# 38. In-Memory Repositories

Implement:

``` text
InMemoryProductRepository
InMemoryInventoryRepository
InMemoryOrderRepository
InMemoryPaymentRepository
InMemoryJobRepository
InMemoryNotificationRepository
```

behind interfaces.

Example:

``` text
OrderRepository
      ↑
InMemoryOrderRepository
```

Later:

``` text
OrderRepository
      ↑
JpaOrderRepository
```

The application should not know which implementation it is using.

------------------------------------------------------------------------

# 39. Concurrency Warning

Spring Boot introduces concurrent HTTP request handling.

Do not assume:

``` text
ConcurrentHashMap
```

makes every operation safe.

This is still unsafe:

``` java
if (!map.containsKey(id)) {
    map.put(id, value);
}
```

The combined operation can race.

Investigate:

``` text
atomic operations
check-then-act
compound operations
locks
ConcurrentHashMap
```

This is an intentional preparation for Phase 4.

------------------------------------------------------------------------

# 40. Inventory Concurrency Experiment

Create an integration test:

``` text
Inventory = 10
100 concurrent requests
each requests quantity = 1
```

Expected:

``` text
successful reservations <= 10
available inventory >= 0
```

Then increase concurrency:

``` text
100
500
1,000
5,000
```

Measure behavior.

Do not consider this the final distributed solution.

The goal is to expose the problem before PostgreSQL.

------------------------------------------------------------------------

# 41. Event Publisher Abstraction

Create:

``` text
EventPublisher
```

Phase 2 implementation:

``` text
InMemoryEventPublisher
```

It may store or expose events to tests.

Do not add Kafka.

The abstraction will later allow:

``` text
InMemoryEventPublisher
        ↓
KafkaEventPublisher
```

without changing application/domain code.

------------------------------------------------------------------------

# 42. Event Envelope

Standardize events around:

``` text
eventId
eventType
aggregateId
aggregateType
occurredAt
correlationId
causationId
payload
```

This creates the conceptual foundation for event-driven architecture.

------------------------------------------------------------------------

# 43. Application Boundary Rules

Application services may:

-   accept commands
-   coordinate repositories
-   invoke domain behavior
-   publish domain events through an abstraction
-   return application results

They should not:

-   execute SQL
-   know HTTP
-   serialize JSON
-   call Kafka directly
-   call Redis directly

------------------------------------------------------------------------

# 44. Repository Boundary Rules

Repositories should represent persistence intent.

Examples:

``` text
findById
save
existsById
delete
```

Do not expose:

``` text
HashMap
JPA EntityManager
SQL
Spring Data types
```

through the application interface.

------------------------------------------------------------------------

# 45. Future Transaction Boundaries

Phase 2 has no real DB transactions.

Still identify likely transaction boundaries.

For example:

``` text
CreateOrderService
```

may eventually need to atomically coordinate:

``` text
order creation
inventory reservation
idempotency record
```

Do not create fake `TransactionManager` abstractions merely to imitate a
database.

Document the future boundary instead.

------------------------------------------------------------------------

# 46. Spring AOP Learning Exercise

Create one small experiment to understand:

``` text
Spring proxy
     ↓
method invocation
     ↓
interceptor
     ↓
target object
```

This is important because later features such as:

``` text
@Transactional
@Cacheable
@Async
@Retryable
```

often rely on proxies.

Do not introduce AOP into business logic merely for demonstration.

------------------------------------------------------------------------

# 47. Bean Lifecycle Exercise

Create a small learning bean and observe:

``` text
constructor
@PostConstruct
normal invocation
@PreDestroy
```

Understand when Spring creates and destroys beans.

Do not place business workflows in lifecycle hooks.

------------------------------------------------------------------------

# 48. Bean Scope Exercise

Understand:

``` text
singleton
prototype
request
session
```

For the main application, prefer singleton beans unless there is a
specific reason otherwise.

Understand why mutable state in singleton beans can create concurrency
bugs.

------------------------------------------------------------------------

# 49. Configuration Bean Exercise

Create one `@Configuration` class containing a `@Bean`.

Compare:

``` text
@Component
```

with:

``` text
@Bean
```

Understand when each is appropriate.

------------------------------------------------------------------------

# 50. Testing Architecture

Use several testing levels:

``` text
Pure domain unit tests
        ↓
Application service tests
        ↓
Controller tests
        ↓
Spring context tests
        ↓
Full integration tests
```

Do not use `@SpringBootTest` for every test.

Choose the smallest test scope that proves the behavior.

------------------------------------------------------------------------

# 51. Domain Unit Tests

Phase 1 tests must remain framework-independent:

``` text
MoneyTest
SkuTest
QuantityTest
ProductTest
InventoryTest
OrderTest
PaymentTest
JobTest
NotificationTest
```

Spring must not be required to test domain behavior.

------------------------------------------------------------------------

# 52. Application Service Tests

Test orchestration without starting Spring.

Use:

``` text
real domain objects
fake/test repositories
test event publisher
```

Verify:

``` text
correct dependencies are called
domain operations are invoked
failures propagate correctly
events are generated appropriately
```

Use Mockito only where it improves the test.

------------------------------------------------------------------------

# 53. Controller Tests

Use Spring MVC testing facilities.

Verify:

``` text
routing
request deserialization
validation
status codes
response JSON
headers
exception mapping
```

Do not retest every domain rule through every controller.

------------------------------------------------------------------------

# 54. Full Spring Context Test

Use `@SpringBootTest` selectively.

At minimum verify:

``` text
application starts
beans are discovered
dependencies are wired
configuration loads
```

Do not make every test a full-context test.

------------------------------------------------------------------------

# 55. API Contract Tests

For every endpoint verify:

``` text
success
validation failure
not found
domain conflict
unexpected error
```

Also verify the JSON response contract.

------------------------------------------------------------------------

# 56. OpenAPI Documentation

Add OpenAPI documentation.

Document:

-   endpoints
-   request schemas
-   response schemas
-   status codes
-   error responses
-   idempotency behavior
-   pagination
-   important headers

Do not settle for automatically generated method lists.

The API documentation should communicate the contract.

------------------------------------------------------------------------

# 57. Security Boundary Preparation

Do not implement full JWT authentication yet.

Keep domain concepts:

``` text
User
UserRole
```

separate from:

``` text
SecurityContext
JWT
GrantedAuthority
```

Security infrastructure belongs outside the domain.

Full security will be introduced later.

------------------------------------------------------------------------

# 58. API Failure Matrix

For every endpoint test:

``` text
Scenario                  Expected
-------------------------------------------
valid request             success
missing field             400
invalid field             400
unknown resource          404
domain conflict           409
duplicate request         defined idempotent behavior
unexpected failure        500
```

Do this before declaring an endpoint complete.

------------------------------------------------------------------------

# 59. Recommended API Surface

Keep the API deliberately small.

### Products

``` text
POST   /api/v1/products
GET    /api/v1/products/{id}
GET    /api/v1/products
PATCH  /api/v1/products/{id}
```

### Inventory

``` text
GET  /api/v1/products/{productId}/inventory
POST /api/v1/products/{productId}/inventory
```

### Orders

``` text
POST /api/v1/orders
GET  /api/v1/orders/{id}
GET  /api/v1/orders
POST /api/v1/orders/{id}/cancel
```

### Jobs

``` text
POST /api/v1/jobs
GET  /api/v1/jobs/{id}
POST /api/v1/jobs/{id}/cancel
```

### Notifications

``` text
POST /api/v1/notifications
GET  /api/v1/notifications/{id}
POST /api/v1/notifications/{id}/cancel
```

Do not create endpoints simply to demonstrate CRUD.

------------------------------------------------------------------------

# 60. JVM and Runtime Investigation

Phase 2 is the first time Forge is a real long-running Spring process.

Begin observing:

``` text
heap usage
thread count
GC activity
CPU
startup time
HTTP latency
```

Learn the purpose of:

``` text
jps
jstack
jcmd
jmap
jstat
```

You do not need JVM mastery yet.

The objective is to stop thinking of the application as only source
code.

------------------------------------------------------------------------

# 61. Thread/Load Exercise

Create a deliberately slow endpoint.

Run increasing concurrent load:

``` text
10
50
100
500
1,000
```

Observe:

``` text
latency
throughput
CPU
memory
thread count
errors
```

Build the mental model:

``` text
HTTP request
   ↓
server thread
   ↓
application work
   ↓
CPU / memory / I/O
```

------------------------------------------------------------------------

# 62. Spring Startup Investigation

Run the application with Spring debug information and investigate:

``` text
auto-configuration
component scanning
conditional configuration
bean creation
```

You should be able to explain:

> Why does this bean exist?

and:

> Why did this configuration activate?

------------------------------------------------------------------------

# 63. Dependency Injection Exercise

Compare conceptually:

``` text
constructor injection
field injection
```

Evaluate:

-   dependency visibility
-   testability
-   immutability
-   construction guarantees
-   framework coupling

Keep constructor injection in production code.

------------------------------------------------------------------------

# 64. Configuration Management

Use:

``` text
application.yml
```

for defaults.

Use environment variables for environment-specific values.

Never commit:

``` text
passwords
API keys
JWT secrets
provider credentials
```

------------------------------------------------------------------------

# 65. Docker

Do not make Docker a major Phase 2 objective.

It becomes much more valuable once Forge needs:

``` text
PostgreSQL
Redis
Kafka
OpenTelemetry
Prometheus
Grafana
```

The focus of Phase 2 is Spring architecture.

------------------------------------------------------------------------

# 66. Git Commit Strategy

Use focused commits such as:

``` text
feat(app): bootstrap spring boot application
feat(app): add constructor-injected application services
feat(api): add product endpoints
feat(api): add inventory endpoints
feat(api): add order endpoints
feat(api): add job endpoints
feat(api): add notification endpoints
feat(api): add validation and error handling
feat(app): add in-memory repositories
feat(app): add event publisher abstraction
feat(obs): add actuator and correlation ids
docs(api): add openapi documentation
test(api): add controller contract coverage
test(app): add spring context coverage
```

Avoid one giant Phase 2 commit.

------------------------------------------------------------------------

# 67. Definition of Done

Phase 2 is complete only when:

-   [ ] Spring Boot starts successfully.
-   [ ] Phase 1 domain tests still pass without Spring.
-   [ ] Domain code remains framework-independent.
-   [ ] Application services are Spring-managed.
-   [ ] Constructor injection is used.
-   [ ] REST controllers use DTOs.
-   [ ] Domain objects are not exposed directly as API contracts.
-   [ ] Product API works.
-   [ ] Inventory API works.
-   [ ] Order API works.
-   [ ] Job API works.
-   [ ] Notification API works.
-   [ ] Validation works.
-   [ ] Global exception handling works.
-   [ ] Error responses are consistent.
-   [ ] HTTP status codes are intentional.
-   [ ] Order creation has an idempotency contract.
-   [ ] In-memory repositories sit behind interfaces.
-   [ ] EventPublisher abstraction exists.
-   [ ] Actuator is configured.
-   [ ] Health/readiness/liveness behavior is understood.
-   [ ] Basic metrics are visible.
-   [ ] Correlation IDs appear in logs.
-   [ ] API documentation exists.
-   [ ] Controller tests exist.
-   [ ] Application-service tests exist.
-   [ ] Spring context test exists.
-   [ ] Concurrent inventory behavior has been experimentally tested.
-   [ ] Profiles/configuration are working.
-   [ ] Secrets are not committed.
-   [ ] Clean build passes.
-   [ ] Static analysis passes.
-   [ ] All tests pass.

------------------------------------------------------------------------

# 68. Required Exit Questions

Before Phase 3, answer these yourself.

## Spring

1.  What happens when Spring Boot starts?
2.  How does component scanning work?
3.  What is a Spring bean?
4.  Why is constructor injection preferred?
5.  What is singleton scope?
6.  Why is mutable state dangerous in singleton beans?
7.  Difference between `@Component`, `@Service`, `@Repository` and
    `@Bean`?
8.  What does `@Configuration` do?
9.  What is auto-configuration?
10. What is a Spring proxy?
11. Why do many Spring features rely on proxies?
12. What happens during bean initialization and destruction?

## REST

13. Why should domain objects not be API DTOs?
14. When should you return 400 versus 409?
15. What does idempotency mean for HTTP?
16. Why is POST not automatically idempotent?
17. Why represent cancellation as an operation?
18. What belongs in request validation versus the domain?

## Architecture

19. What belongs in controller, application service and domain?
20. Why do repositories exist as interfaces?
21. Why is infrastructure outside the domain?
22. What does dependency inversion achieve?
23. What happens when two instances use in-memory repositories?
24. Why can Phase 2 not provide distributed inventory correctness?
25. Where should a future database transaction boundary sit?

## Observability

26. Liveness versus readiness?
27. What is a correlation ID?
28. Why are p95/p99 useful?
29. What does a server thread do during a request?
30. How would you investigate increased API latency?

------------------------------------------------------------------------

# 69. Required Failure Experiments

Before leaving Phase 2, deliberately investigate:

## Experiment 1 --- Repository Race

Test concurrent access to in-memory repositories.

Investigate whether:

``` text
check → modify
```

is atomic.

## Experiment 2 --- Inventory Race

Run:

``` text
inventory = 10
1,000 concurrent reservations
```

Record:

``` text
successful operations
final inventory
failures
latency
```

## Experiment 3 --- Duplicate Order

Submit the same idempotency key concurrently.

Document exactly what your in-memory implementation can and cannot
guarantee.

## Experiment 4 --- Restart

Create data, restart the application and observe the data disappearing.

This is the practical demonstration of why durable persistence is
required.

## Experiment 5 --- Two Instances

Run two application instances.

Observe that:

``` text
Instance A memory != Instance B memory
```

## Experiment 6 --- Slow Endpoint

Add artificial delay and measure:

``` text
throughput
latency
thread count
CPU
memory
```

------------------------------------------------------------------------

# 70. Engineering Notes to Produce

Before Phase 3, write a short engineering note covering:

``` text
1. Spring bean lifecycle
2. Dependency injection
3. Controller/application/domain boundaries
4. REST design decisions
5. Error-handling design
6. Validation boundaries
7. Actuator observations
8. JVM/thread observations
9. Inventory concurrency experiment
10. In-memory persistence limitations
```

The goal is to improve reasoning, not just accumulate code.

------------------------------------------------------------------------

# 71. Expected Architecture at Phase 2 Completion

``` text
                         HTTP Client
                              │
                              ▼
                    ┌──────────────────┐
                    │ REST Controllers │
                    └────────┬─────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │ Application Services │
                  └──────────┬───────────┘
                             │
                 ┌───────────┴───────────┐
                 ▼                       ▼
          ┌──────────────┐       ┌──────────────┐
          │ Domain Model │       │ Repository   │
          │              │       │ Interfaces   │
          └──────────────┘       └──────┬───────┘
                                        │
                                        ▼
                               ┌─────────────────┐
                               │ In-Memory       │
                               │ Adapters        │
                               └─────────────────┘

                    Spring Infrastructure
                  ┌─────────┼────────────┐
                  ▼         ▼            ▼
              Actuator  Config       Logging
```

The dependency direction remains inward.

------------------------------------------------------------------------

# 72. Phase 2 → Phase 3

Phase 3 introduces PostgreSQL and persistence.

The key change:

``` text
Phase 2

Repository Interface
        ↓
In-Memory Repository


Phase 3

Repository Interface
        ↓
Persistence Adapter
        ↓
PostgreSQL
```

Phase 3 will force you to learn and implement:

``` text
transactions
indexes
composite indexes
query plans
constraints
normalization
MVCC
isolation levels
locking
optimistic locking
pessimistic locking
connection pools
transaction boundaries
```

The Phase 2 inventory race becomes the foundation for the database
concurrency work.

------------------------------------------------------------------------

# 73. Final Rule

> Do not let Spring become your architecture.

The target is:

``` text
Spring supports the architecture.
```

not:

``` text
Spring defines the architecture.
```

Your domain should still be understandable if the Spring annotations
were removed.

That is the standard Phase 2 must establish before Forge moves into
database-backed persistence and real transaction/concurrency
engineering.
