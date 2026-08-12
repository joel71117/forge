# Forge Domain Model

## Overview

``` text
User
 └─ Order
     ├─ OrderItem → Product → Inventory
     ├─ InventoryReservation
     └─ Payment

Order → Domain Events → Jobs / Notifications
Job → JobAttempt
Notification → NotificationAttempt → Provider
```

## User

``` text
id
email
displayName
role
status
createdAt
updatedAt
```

Roles: CUSTOMER, ADMIN, OPERATIONS.

Statuses: ACTIVE, SUSPENDED, DELETED.

## Product

``` text
id
sku
name
description
price
currency
status
createdAt
updatedAt
```

Statuses: ACTIVE, INACTIVE, DISCONTINUED.

## Inventory

Conceptually:

``` text
productId
availableQuantity
reservedQuantity
version
updatedAt
```

The final persistence model is intentionally open to investigation.

## InventoryReservation

``` text
id
orderId
productId
quantity
status
expiresAt
createdAt
updatedAt
```

Statuses:

``` text
PENDING
RESERVED
CONSUMED
RELEASED
EXPIRED
CANCELLED
```

## Order

``` text
id
customerId
status
currency
totalAmount
idempotencyKey
createdAt
updatedAt
```

Statuses:

``` text
CREATED
CONFIRMED
PROCESSING
COMPLETED
FAILED
CANCELLED
```

## OrderItem

``` text
id
orderId
productId
quantity
unitPrice
subtotal
```

`unitPrice` is a historical snapshot.

## Payment

``` text
id
orderId
amount
currency
status
provider
providerReference
idempotencyKey
attemptCount
createdAt
updatedAt
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

## Job

``` text
id
type
tenantId
payload
priority
status
scheduledAt
retryCount
maxRetries
nextAttemptAt
leaseUntil
createdAt
startedAt
completedAt
updatedAt
```

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

## Notification

``` text
id
customerId
type
channel
priority
template
payload
status
scheduledAt
idempotencyKey
createdAt
updatedAt
```

Channels:

``` text
EMAIL
SMS
PUSH
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

## DomainEvent

``` text
eventId
eventType
aggregateId
aggregateType
occurredAt
version
correlationId
causationId
payload
```

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

## Relationships

``` text
User 1 ── * Order
Order 1 ── * OrderItem
OrderItem * ── 1 Product
Product 1 ── 1 Inventory

Order 1 ── * InventoryReservation
Order 1 ── * Payment
Job 1 ── * JobAttempt
Notification 1 ── * NotificationAttempt
NotificationAttempt * ── 1 Provider
```

## Aggregate Candidates

Initial candidates:

``` text
Order
Inventory
Job
Notification
Payment
```

Do not assume every table is an aggregate. Aggregates exist to protect
consistency boundaries.

## Potential Bounded Contexts

``` text
Catalog
Inventory
Order
Payment
Job Processing
Notification
Identity
```

These are potential boundaries, not immediate microservices.

## State Machines

### Order

``` text
CREATED → CONFIRMED → PROCESSING → COMPLETED
   │
   └→ FAILED

CONFIRMED → CANCELLED
```

### Reservation

``` text
PENDING → RESERVED → CONSUMED
                  ↘ RELEASED
                  ↘ EXPIRED
```

### Job

``` text
QUEUED → RUNNING → COMPLETED
              ↘ FAILED → RETRYING → QUEUED
                         └→ DEAD_LETTERED

QUEUED → CANCELLED
```

### Notification

``` text
PENDING → PROCESSING → SENT
                   ↘ FAILED → RETRYING → PROCESSING
                              └→ DEAD_LETTERED

PENDING → CANCELLED
```

### Payment

``` text
PENDING → PROCESSING → SUCCEEDED
                   ↘ FAILED
                   ↘ UNKNOWN → reconciliation → SUCCEEDED|FAILED
```
