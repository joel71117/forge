# ADR-001: Start with a Modular Monolith

## Status

Accepted

## Context

Forge has several conceptual domains: Catalog, Inventory, Orders,
Payments, Job Processing and Notifications.

Starting immediately with microservices would add network, deployment,
discovery and distributed-debugging complexity before the domain and
concurrency problems are understood.

Forge is primarily a backend-engineering learning project.

## Decision

Start as a modular monolith with explicit modules:

``` text
api
catalog
inventory
order
payment
job
notification
user
common
```

Modules should have clear responsibilities and minimize unnecessary
coupling.

## Alternatives

### Microservices from day one

Pros:

-   immediate distributed environment,
-   independent deployment,
-   independent scaling.

Cons:

-   high operational complexity,
-   harder local debugging,
-   distributed failures before core correctness is understood.

### Unstructured monolith

Pros:

-   easiest start.

Cons:

-   high coupling,
-   poor boundaries,
-   harder later extraction.

## Rationale

The modular monolith lets the project first solve:

``` text
domain correctness
transactions
database concurrency
Java concurrency
state machines
business invariants
```

before introducing network boundaries.

## Consequences

### Positive

-   simpler initial development,
-   easier debugging,
-   easier database/concurrency experiments,
-   lower infrastructure overhead,
-   clearer learning progression.

### Negative

-   no independent scaling by domain initially,
-   distributed challenges must be introduced deliberately,
-   later extraction requires disciplined boundaries.

## Revisit Conditions

Consider extracting a service when:

-   scaling requirements differ materially,
-   failure isolation is valuable,
-   deployment independence provides measurable benefit,
-   data ownership is clear,
-   performance measurements justify extraction,
-   a learning objective requires a distributed service boundary.
