# Forge Use Cases

## UC-001 --- Create Order

**Preconditions:** active customer, valid/orderable products, positive
quantities.

**Flow:** 1. Validate request and authorization. 2. Validate products
and prices. 3. Reserve inventory. 4. Create order. 5. Record state. 6.
Initiate downstream processing. 7. Return order ID.

**Failures:** insufficient stock, duplicate request, DB failure,
application crash.

A duplicate idempotency key represents one logical operation.

## UC-002 --- Cancel Order

Validate ownership and state, transition the order, release applicable
reservations, publish downstream events and return the new state.

Cancellation must be safe to retry.

## UC-003 --- Reserve Inventory

Validate product and quantity, atomically protect the inventory
operation, create a reservation and return the result.

For:

``` text
Inventory = 1
A buys 1
B buys 1
```

at most one reservation succeeds.

## UC-004 --- Release Reservation

Validate ownership/state, transition the reservation and restore
available inventory. Repeating the operation must have one logical
effect.

## UC-005 --- Consume Reservation

A successful order transitions RESERVED to CONSUMED. A consumed
reservation can never be released.

## UC-006 --- Process Payment

Create a payment attempt, call the simulated provider, record the
response and transition payment state.

A timeout may result in UNKNOWN. The system must not blindly charge
again.

## UC-007 --- Submit Job

Validate, assign ID, determine priority/time, persist and make the job
eligible for execution. The API does not wait for completion.

## UC-008 --- Execute Job

Worker discovers a job, claims it, transitions to RUNNING, executes it,
records the result and transitions to COMPLETED or failure.

If the worker crashes after the business effect but before
acknowledgement, duplicate execution must be safe.

## UC-009 --- Retry Job

Classify failure, decide whether it is retryable, increment attempts,
calculate backoff and requeue after the delay. Permanent failures are
not retried indefinitely.

## UC-010 --- Schedule Job

Support immediate, delayed and recurring execution. Multiple application
instances must not unintentionally execute the same scheduled
occurrence.

## UC-011 --- Send Notification

Persist the logical notification, select a worker, call the provider,
record the attempt and transition to SENT or retry/failure state.

## UC-012 --- Retry Notification

Retry transient failures such as timeout, 503 or rate limiting. Do not
endlessly retry permanent failures such as invalid recipients.

## UC-013 --- Provider Failover

If Provider A fails transiently, the system may route to Provider B.
Failover must preserve idempotency and remain observable.

## UC-014 --- Duplicate Event

If an event is delivered twice, the consumer must produce one logical
business effect.

## UC-015 --- Scheduled Notification

Persist a future notification and deliver it at or after its scheduled
time according to the defined lateness policy. Restarting the
application must not silently lose it.

## UC-016 --- Reconcile Unknown Payment

A reconciliation job queries the provider and resolves UNKNOWN to
SUCCEEDED or FAILED. Reconciliation is itself idempotent.

## UC-017 --- Recover Orphaned Job

Detect an expired lease, determine whether the job can be safely
retried, and requeue or finalize it. The design must consider the
possibility that the original business effect already occurred.

## UC-018 --- Inspect Dead-Letter Work

Admins can inspect failed jobs/notifications, attempts, error codes,
provider information and relevant correlation IDs.

## UC-019 --- Graceful Worker Shutdown

Stop accepting new work, safely finish/release in-flight work, persist
state and exit without silently losing ownership.

## UC-020 --- High-Concurrency Inventory

With inventory 100 and 1,000 concurrent one-unit purchase requests:

-   successful consumption \<= 100,
-   final inventory \>= 0,
-   no duplicate reservation effect,
-   no impossible inventory state.

## UC-021 --- Noisy Customer

Customer A submits 1,000,000 jobs while B submits 10. The scheduler must
enforce a defined fairness/tenant-capacity policy.

## UC-022 --- Provider Outage

If a provider returns 503 for ten minutes, use bounded retries,
exponential backoff and jitter, isolate capacity, expose metrics and
eventually dead-letter according to policy.
