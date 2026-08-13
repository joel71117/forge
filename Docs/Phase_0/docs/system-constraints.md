# Forge System Constraints and Failure Model

## Architecture

-   Start as a modular monolith.
-   Introduce technologies only when requirements justify them.
-   Kafka, Redis and microservices are not assumptions; they are tools
    to be evaluated.

## Failure assumptions

Any of these may fail independently:

``` text
Application
Worker
PostgreSQL
Redis
Kafka
Network
External providers
Scheduler
```

Failures include:

-   crashes
-   timeouts
-   slow responses
-   connection loss
-   duplicate requests
-   duplicate messages
-   consumer lag
-   rebalances
-   provider 5xx
-   provider rate limiting
-   ambiguous external outcomes

## Database

The database may be unavailable, slow, deadlock, reject transactions or
restart. Code must not assume DB calls are cheap or always successful.

## Redis

Redis may be unavailable or lose cached state. Critical durable business
truth must remain recoverable without Redis unless explicitly justified.

## Kafka

Consumers must tolerate duplicate delivery. Ordering exists only where
explicitly designed. Consumer lag and rebalance are normal operating
conditions to be measured.

## External providers

A provider may succeed but the response may be lost. A timeout therefore
does not necessarily mean failure.

## Crash consistency

Important workflows must consider crash points:

``` text
before DB write
after DB write
before event publication
after event publication
before external API call
after external API call
before acknowledgement
after acknowledgement
```

## Duplicate execution

Duplicates can arise from client retries, HTTP retries, Kafka
redelivery, scheduler races, worker recovery and provider uncertainty.

## Time

Distinguish:

``` text
event time
processing time
DB commit time
scheduled time
lease expiry
```

Do not assume perfectly synchronized clocks.

## Ordering

Where ordering matters, define:

-   ordering key,
-   ordering scope,
-   partitioning,
-   consumer behavior.

Do not assume global ordering.

## Concurrency

Consider concurrency at:

``` text
thread level
instance level
worker level
consumer level
transaction level
external request level
```

A thread-safe solution is not automatically distributed-safe.

## Recovery

Recovery must cover:

``` text
orphaned jobs
expired leases
expired reservations
unknown payments
failed notifications
dead-letter work
```

Recovery must itself be safe to retry.

## Resource limits

Treat these as finite:

``` text
threads
memory
DB connections
Kafka partitions
network bandwidth
provider quotas
queue capacity
```

Backpressure and bounded resource use are first-class requirements.

## Security

Never log credentials or secrets, commit credentials, trust client
authorization claims without server validation, or assume internal
traffic is automatically trusted.

## Learning constraints

This is a production-oriented learning system:

-   fake providers are preferred initially,
-   failures should be reproducible,
-   load tests should be repeatable,
-   decisions should be documented,
-   intentionally flawed implementations may be built and measured,
-   depth is preferred over feature breadth.

## Out of scope

Do not expand into real banking, shipping, tax, recommendation systems,
advanced search, social features or marketplace functionality without a
deliberate engineering reason.
