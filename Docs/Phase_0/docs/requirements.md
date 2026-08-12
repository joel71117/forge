# Forge Functional Requirements

Requirements describe **what** Forge must do, not how.

## User

-   **FR-USER-001:** Maintain unique customer identities.
-   **FR-USER-002:** Support ACTIVE, SUSPENDED and DELETED user states.
-   **FR-USER-003:** Customers may access only resources they own.

## Product

-   **FR-PROD-001:** Admins can create products with unique SKUs.
-   **FR-PROD-002:** Admins can update mutable product attributes.
-   **FR-PROD-003:** Products support ACTIVE, INACTIVE and DISCONTINUED
    states.
-   **FR-PROD-004:** Order items preserve the price applicable when the
    order is accepted.
-   **FR-PROD-005:** Inactive/discontinued products cannot be newly
    ordered.

## Inventory

-   **FR-INV-001:** Initialize inventory for products.
-   **FR-INV-002:** Increase available inventory.
-   **FR-INV-003:** Query availability.
-   **FR-INV-004:** Reserve inventory for an order.
-   **FR-INV-005:** Reservations expire after a configurable period.
-   **FR-INV-006:** Release reservations.
-   **FR-INV-007:** Consume reservations on successful purchase.
-   **FR-INV-008:** Concurrent reservations must not oversell.
-   **FR-INV-009:** Successful operations must never produce negative
    available inventory.
-   **FR-INV-010:** A reservation belongs to exactly one logical order.
-   **FR-INV-011:** Retrying a reservation must not create multiple
    effective reservations.

## Orders

-   **FR-ORD-001:** Customers can create orders containing one or more
    items.
-   **FR-ORD-002:** Quantities must be positive.
-   **FR-ORD-003:** Every item must reference an orderable product.
-   **FR-ORD-004:** An order is accepted only when inventory can be
    reserved according to policy.
-   **FR-ORD-005:** Orders use a controlled lifecycle: CREATED,
    CONFIRMED, PROCESSING, COMPLETED, FAILED, CANCELLED.
-   **FR-ORD-006:** Cancellation is allowed only in eligible states.
-   **FR-ORD-007:** Order creation supports an idempotency key.
-   **FR-ORD-008:** Customers can retrieve their own orders.
-   **FR-ORD-009:** Order state history is retained for investigation.
-   **FR-ORD-010:** COMPLETED requires all required downstream
    conditions.
-   **FR-ORD-011:** Failed orders enter an explicitly defined
    recoverable or terminal state.

## Payments

-   **FR-PAY-001:** Create a payment attempt for an order.
-   **FR-PAY-002:** Support PENDING, PROCESSING, SUCCEEDED, FAILED,
    UNKNOWN and CANCELLED.
-   **FR-PAY-003:** Simulated provider can produce success, rejection,
    timeout, transient failure, rate limiting and duplicate response.
-   **FR-PAY-004:** Payment operations are idempotent.
-   **FR-PAY-005:** Timeout may produce UNKNOWN rather than FAILED.
-   **FR-PAY-006:** UNKNOWN payments can be reconciled later.

## Jobs

-   **FR-JOB-001:** Submit asynchronous jobs.
-   **FR-JOB-002:** Support multiple job types without a growing central
    conditional dispatcher.
-   **FR-JOB-003:** Support QUEUED, RUNNING, COMPLETED, FAILED,
    RETRYING, CANCELLED and DEAD_LETTERED.
-   **FR-JOB-004:** Support HIGH, NORMAL and LOW priority.
-   **FR-JOB-005:** Support immediate, delayed and recurring jobs.
-   **FR-JOB-006:** Support configurable retry policies.
-   **FR-JOB-007:** Stop retries after maximum attempts.
-   **FR-JOB-008:** Dead-letter exhausted/permanent failures.
-   **FR-JOB-009:** Recover jobs after worker failure.
-   **FR-JOB-010:** Track worker ownership/leases.
-   **FR-JOB-011:** Prevent duplicate business effects from repeated
    execution.
-   **FR-JOB-012:** Support cancellation before irreversible execution.
-   **FR-JOB-013:** Support per-customer concurrency limits and
    fairness.

## Notifications

-   **FR-NOT-001:** Create notifications from business events or
    explicit requests.
-   **FR-NOT-002:** Support EMAIL, SMS and PUSH.
-   **FR-NOT-003:** Support PENDING, PROCESSING, SENT, FAILED, RETRYING,
    DEAD_LETTERED and CANCELLED.
-   **FR-NOT-004:** Support scheduled delivery.
-   **FR-NOT-005:** Support priority.
-   **FR-NOT-006:** Hide provider-specific implementation behind an
    abstraction.
-   **FR-NOT-007:** Support provider failover where applicable.
-   **FR-NOT-008:** Retry transient failures.
-   **FR-NOT-009:** Dead-letter permanent/exhausted failures.
-   **FR-NOT-010:** Prevent duplicate logical notification effects.
-   **FR-NOT-011:** Record delivery attempts and provider results.

## Events

-   **FR-EVT-001:** Important state changes are representable as domain
    events.
-   **FR-EVT-002:** Every event has a unique event ID.
-   **FR-EVT-003:** Events carry correlation/causation metadata where
    appropriate.
-   **FR-EVT-004:** Consumers tolerate duplicates.
-   **FR-EVT-005:** Eventual consistency is allowed where business
    correctness permits it.

## Operations

-   **FR-OPS-001:** Expose health information.
-   **FR-OPS-002:** Expose technical and business metrics.
-   **FR-OPS-003:** Use structured logs.
-   **FR-OPS-004:** Correlate synchronous and asynchronous workflows.
-   **FR-OPS-005:** Support graceful worker shutdown.
-   **FR-OPS-006:** Detect/recover orphaned jobs and reservations.

## Security

-   **FR-SEC-001:** Protected APIs require authentication.
-   **FR-SEC-002:** Enforce authorization server-side.
-   **FR-SEC-003:** Secrets are externalized.
-   **FR-SEC-004:** Validate external input.
-   **FR-SEC-005:** Rate-limit selected APIs.
-   **FR-SEC-006:** Audit sensitive administrative/business operations.
