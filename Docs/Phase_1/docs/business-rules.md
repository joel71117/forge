# Forge Business Rules and Invariants

## Product

-   SKU is unique.
-   Product price cannot be negative.
-   Existing order items retain their accepted purchase price.
-   Inactive/discontinued products cannot be newly ordered.

## Inventory

-   Available inventory can never be negative.
-   The system can never successfully oversell inventory.
-   A reservation belongs to exactly one logical order.
-   Reservation transitions are controlled:
    `PENDING → RESERVED → CONSUMED|RELEASED|EXPIRED`
-   A consumed reservation cannot be released.
-   A released/expired reservation cannot be consumed.
-   Releasing a reservation twice has one logical effect.
-   Concurrent reservations must preserve inventory correctness.
-   Reservation expiry cannot release inventory twice.
-   Inventory changes must be auditable.

## Orders

-   Quantity \> 0.
-   An order contains at least one item.
-   Every order belongs to one customer.
-   Every item references a valid product.
-   An order cannot complete without required conditions.
-   COMPLETED cannot transition to CREATED.
-   COMPLETED cannot be cancelled unless a future business rule
    explicitly permits it.
-   Failed orders cannot retain an active reservation indefinitely.
-   Repeating the same idempotent create request produces one logical
    order.
-   Idempotency keys are scoped appropriately.

## Payments

-   A payment belongs to one logical order.
-   One logical payment cannot have multiple successful outcomes.
-   Timeout does not necessarily mean failure.
-   UNKNOWN must be reconciled before an unsafe retry.
-   Provider results are recorded for investigation.

## Jobs

State machine:

``` text
QUEUED → RUNNING → COMPLETED
             │
             └→ FAILED → RETRYING → QUEUED
                         └→ DEAD_LETTERED

QUEUED → CANCELLED
```

Rules:

-   Job IDs are unique.
-   Completed jobs do not return to QUEUED.
-   Cancelled jobs cannot start after cancellation is committed.
-   Retry count cannot exceed the configured maximum.
-   Permanent failures are not retried indefinitely.
-   Worker memory must remain bounded.
-   One customer cannot monopolize all capacity.
-   Duplicate execution must not create duplicate business effects where
    possible.

## Notifications

``` text
PENDING → PROCESSING → SENT
                     ↘ FAILED → RETRYING → PROCESSING
                                  └→ DEAD_LETTERED

PENDING → CANCELLED
```

Rules:

-   Every logical notification has a unique ID.
-   SENT requires an appropriate successful provider result.
-   Transient failures may be retried.
-   Permanent failures are not retried indefinitely.
-   Provider failover must not bypass idempotency.
-   Attempts are recorded.

## Events

-   Every event has a unique event ID.
-   Events are immutable after publication.
-   Consumers assume duplicate delivery.
-   Consumers cannot rely only on in-memory duplicate tracking.
-   Ordering is only guaranteed where explicitly designed.

## Reliability

-   Accepted jobs cannot silently disappear.
-   Retry must not blindly repeat unsafe external operations.
-   Recovery is safe to run more than once.
-   Business invariants remain valid after failure.

## Security

-   Customers can access only their own resources.
-   Admin operations require elevated authorization.
-   Secrets are never logged.
-   Credentials are never committed to source control.

## Fairness

-   One customer cannot consume unlimited worker capacity.
-   Priority must have bounded effects.
-   Low-priority work must not be permanently starved.

## Design Principle

Keep business invariants separate from implementation mechanisms.

Example:

> Inventory must never be oversold.

Possible implementations include atomic SQL, pessimistic locking,
optimistic locking, serializable transactions or partitioned command
processing. The invariant stays stable while the mechanism may evolve.
