# Forge --- Phase 3 Implementation Plan

## PostgreSQL Deep Dive

> **Phase:** 3\
> **Primary subject:** PostgreSQL mastery\
> **Project:** Forge Commerce Platform\
> **Prerequisites:** Phase 0 requirements/engineering fundamentals and
> Phase 1--2 domain/modular-monolith implementation\
> **Primary outcome:** Understand what PostgreSQL and Hibernate are
> actually doing underneath the Spring Boot application, especially
> around query execution, transactions, concurrency, locking, and
> connection management.

------------------------------------------------------------------------

# 1. Phase Objective

Phase 3 is deliberately **not** about learning PostgreSQL syntax or
simply using Spring Data JPA.

The goal is to move from:

> "I know how to persist an entity with JPA."

to:

> "I understand how PostgreSQL executes my query, how indexes affect
> that execution, how transactions interact under concurrency, what
> locks are acquired, and how Hibernate/JDBC/connection pooling affect
> the database."

The roadmap explicitly requires experiments with:

-   `EXPLAIN ANALYZE`
-   B-tree indexes
-   Composite indexes
-   Index selectivity
-   Covering indexes
-   Index scans
-   Sequential scans
-   `READ COMMITTED`
-   `REPEATABLE READ`
-   `SERIALIZABLE`
-   `SELECT ... FOR UPDATE`
-   Optimistic locking
-   HikariCP
-   Concurrent transactions
-   Deadlocks

The required deliverable is:

``` text
/docs/database-experiments.md
```

The document must contain **your actual experiments and observations**,
rather than copied definitions.

------------------------------------------------------------------------

# 2. What You Should Have Before Starting

Before Phase 3, Forge should already be a working Spring Boot modular
monolith.

Expected high-level structure:

``` text
forge
├── api
├── order
├── inventory
├── notification
├── job
├── user
└── common
```

The project should still be:

``` text
ONE Spring Boot application
ONE PostgreSQL database
```

Do **not** introduce:

-   Kafka
-   Redis
-   microservices
-   Kubernetes
-   API Gateway

as part of this phase.

Those technologies belong to later phases. The purpose of Phase 3 is to
understand the database deeply enough that later distributed-system
decisions are based on actual database behavior.

------------------------------------------------------------------------

# 3. Phase Learning Outcomes

By the end of Phase 3, you should be able to explain and demonstrate:

## Database fundamentals

-   How PostgreSQL stores relational data at a high level
-   What a table scan is
-   Why PostgreSQL may choose a sequential scan
-   What an index provides
-   Why an index is not automatically beneficial
-   What a B-tree index is useful for
-   How composite indexes work
-   How column order affects composite indexes
-   What index selectivity means
-   What a covering/index-only scan is
-   Why indexes have a write/storage/maintenance cost

## Query execution

-   How to use `EXPLAIN`
-   How to use `EXPLAIN ANALYZE`
-   How to compare query plans
-   How to identify sequential scans
-   How to identify index scans
-   How to interpret estimated vs actual rows
-   How to identify expensive operations
-   How data distribution affects the planner
-   Why a query that looks simple can still be expensive

## Transactions

-   What a database transaction guarantees
-   What transaction isolation means
-   How concurrent transactions interact
-   What `READ COMMITTED` allows
-   What `REPEATABLE READ` changes
-   What `SERIALIZABLE` provides
-   What anomalies can occur under different isolation levels

## Locking

-   What row-level locking is
-   What `SELECT ... FOR UPDATE` does
-   When locks are acquired
-   When locks are released
-   How transactions can block one another
-   How deadlocks occur
-   How to investigate a deadlock

## Optimistic concurrency

-   Why optimistic locking exists
-   How a version column works
-   How Hibernate implements optimistic locking
-   What happens when two transactions update the same entity
-   How an optimistic locking failure is detected
-   When optimistic locking is preferable to pessimistic locking

## Connection pooling

-   What JDBC connections are
-   Why applications use connection pools
-   How HikariCP works conceptually
-   What happens when request concurrency exceeds pool size
-   Why increasing the connection pool indefinitely is not a solution
-   How connection-pool saturation affects application latency

------------------------------------------------------------------------

# 4. Phase Structure

Execute the phase in the following order:

``` text
0. Establish the database baseline
        ↓
1. Build a realistic dataset
        ↓
2. Understand query execution
        ↓
3. Study sequential scans
        ↓
4. Introduce B-tree indexes
        ↓
5. Study composite indexes
        ↓
6. Study selectivity
        ↓
7. Study covering/index-only scans
        ↓
8. Study transactions
        ↓
9. Compare isolation levels
        ↓
10. Study row locking
        ↓
11. Create and investigate deadlocks
        ↓
12. Implement optimistic locking
        ↓
13. Study HikariCP
        ↓
14. Run concurrent application experiments
        ↓
15. Document findings
        ↓
16. Phase review / engineering challenge
```

Do not skip directly to JPA annotations.

The database behavior should be understood first, then connected back to
Spring/Hibernate.

------------------------------------------------------------------------

# 5. Step 0 --- Establish the Database Baseline

## Objective

Create a controlled environment in which experiments can be repeated.

Record:

-   PostgreSQL version
-   Java version
-   Spring Boot version
-   Hibernate version
-   JDBC driver version
-   HikariCP version
-   CPU
-   RAM
-   operating system
-   database configuration relevant to the experiments

The purpose is not to create a production database configuration.

The purpose is to know what environment produced your observations.

## Verify PostgreSQL

Run:

``` sql
SELECT version();
```

Also inspect basic configuration where useful:

``` sql
SHOW server_version;
SHOW max_connections;
SHOW shared_buffers;
SHOW work_mem;
```

Do not attempt to tune everything yet.

The objective is observation, not premature optimization.

## Record the baseline

Create a section in:

``` text
/docs/database-experiments.md
```

with:

``` text
Environment
-----------
PostgreSQL:
Java:
Spring Boot:
Hibernate:
JDBC:
HikariCP:
CPU:
RAM:
OS:
```

------------------------------------------------------------------------

# 6. Step 1 --- Prepare a Realistic Dataset

Small datasets are poor for query-plan experiments.

If a table contains only 20 rows, PostgreSQL may reasonably choose a
sequential scan even when an index exists.

Therefore, create enough data to make query planning meaningful.

## Minimum recommended dataset

Use the Forge domain.

At minimum, create substantial data for:

``` text
products
inventory
orders
order_items
users
jobs
notifications
```

A reasonable starting point is:

``` text
users            100,000+
products          50,000+
orders           500,000+
order_items     1,000,000+
jobs             500,000+
notifications    500,000+
```

You do not need these exact numbers if your machine cannot comfortably
handle them.

The important requirement is:

> Large enough data to produce meaningful query-plan differences.

## Generate data

You may use:

-   SQL
-   Java data generation
-   Spring Boot test utilities
-   scripts

Do not make every value identical.

Use realistic distributions.

For example:

``` text
Product categories:
electronics
books
home
fitness
gaming
```

User activity should also be uneven.

Some users should have:

``` text
1 order
```

while others have:

``` text
100+ orders
```

This will become useful when investigating selectivity and query
planning.

------------------------------------------------------------------------

# 7. Step 2 --- Establish Query Baselines

Before adding indexes, measure queries.

Choose queries that correspond to real Forge operations.

Examples:

``` sql
SELECT *
FROM products
WHERE name = 'Mechanical Keyboard';
```

``` sql
SELECT *
FROM orders
WHERE user_id = ?;
```

``` sql
SELECT *
FROM orders
WHERE status = 'PROCESSING';
```

``` sql
SELECT *
FROM orders
WHERE user_id = ?
AND status = 'COMPLETED';
```

``` sql
SELECT *
FROM jobs
WHERE status = 'PENDING'
ORDER BY scheduled_at
LIMIT 100;
```

These are intentionally different.

They allow you to investigate:

-   equality predicates
-   low-selectivity predicates
-   composite predicates
-   filtering + ordering
-   pagination/limiting

------------------------------------------------------------------------

# 8. Step 3 --- Learn EXPLAIN

Start with:

``` sql
EXPLAIN
SELECT *
FROM orders
WHERE user_id = 100;
```

Then:

``` sql
EXPLAIN ANALYZE
SELECT *
FROM orders
WHERE user_id = 100;
```

Understand the difference.

## EXPLAIN

Primarily shows PostgreSQL's planned execution strategy.

## EXPLAIN ANALYZE

Actually executes the query and reports observed execution information.

Do not blindly copy the output.

Learn to identify:

``` text
Seq Scan
Index Scan
Index Only Scan
Bitmap Index Scan
Bitmap Heap Scan
Sort
Aggregate
Nested Loop
Hash Join
Merge Join
```

You do not need mastery of every operator in this phase.

Focus on understanding what PostgreSQL is doing for your own queries.

------------------------------------------------------------------------

# 9. Step 4 --- Sequential Scan Experiment

Choose a query that initially uses a sequential scan.

For example:

``` sql
EXPLAIN ANALYZE
SELECT *
FROM orders
WHERE user_id = 100;
```

Record:

-   execution time
-   planning time
-   rows examined
-   rows returned
-   scan type
-   estimated rows
-   actual rows

Create a baseline table in your documentation:

  Query               Index   Scan         Execution Time   Rows Returned
  ------------------- ------- ---------- ---------------- ---------------
  orders by user_id   No      Seq Scan      record result   record result

The exact numbers must come from your own experiment.

Do not fabricate benchmark values.

------------------------------------------------------------------------

# 10. Step 5 --- B-tree Index Experiment

Create:

``` sql
CREATE INDEX idx_orders_user_id
ON orders(user_id);
```

Run the same query again:

``` sql
EXPLAIN ANALYZE
SELECT *
FROM orders
WHERE user_id = 100;
```

Compare:

``` text
Before
------
Seq Scan

After
-----
Index Scan / Bitmap-based plan
```

Do not assume PostgreSQL must use the index.

The planner decides.

Investigate why.

## Questions to answer

-   Did PostgreSQL choose the index?
-   Why?
-   Did execution time improve?
-   How many rows were returned?
-   What happens when the predicate matches a large percentage of the
    table?
-   Does PostgreSQL eventually prefer a sequential scan?

This experiment is important because it demonstrates:

> An index is an optimization opportunity, not an instruction to
> PostgreSQL.

------------------------------------------------------------------------

# 11. Step 6 --- Understand B-tree Behavior

Investigate the types of predicates for which a B-tree is useful.

Experiment with:

``` sql
=
<
<=
>
>=
ORDER BY
```

For example:

``` sql
CREATE INDEX idx_products_price
ON products(price);
```

Test:

``` sql
SELECT *
FROM products
WHERE price > 1000;
```

Then:

``` sql
SELECT *
FROM products
ORDER BY price;
```

Then combine filtering and ordering.

Record which queries benefit and which do not.

------------------------------------------------------------------------

# 12. Step 7 --- Index Selectivity

This is a critical concept.

Compare a highly selective column with a low-selectivity column.

Example:

``` text
user_id
```

may have many distinct values.

Whereas:

``` text
status
```

may contain only:

``` text
PENDING
PROCESSING
COMPLETED
FAILED
```

Create indexes on both.

Example:

``` sql
CREATE INDEX idx_orders_status
ON orders(status);
```

Then compare:

``` sql
EXPLAIN ANALYZE
SELECT *
FROM orders
WHERE status = 'COMPLETED';
```

with:

``` sql
EXPLAIN ANALYZE
SELECT *
FROM orders
WHERE user_id = 100;
```

Investigate:

-   cardinality
-   selectivity
-   percentage of rows matched
-   planner choice
-   execution time

The important lesson is:

> An index on a column with very low selectivity may not provide the
> expected benefit.

------------------------------------------------------------------------

# 13. Step 8 --- Composite Indexes

Create a realistic Forge query:

``` sql
SELECT *
FROM orders
WHERE user_id = ?
AND status = ?
ORDER BY created_at DESC;
```

First test it without a composite index.

Then create:

``` sql
CREATE INDEX idx_orders_user_status_created
ON orders(user_id, status, created_at DESC);
```

Run:

``` sql
EXPLAIN ANALYZE
SELECT *
FROM orders
WHERE user_id = 100
AND status = 'COMPLETED'
ORDER BY created_at DESC;
```

Compare the plans.

## Then investigate column order

Compare:

``` text
(user_id, status, created_at)
```

against:

``` text
(status, user_id, created_at)
```

and, where useful:

``` text
(user_id, created_at, status)
```

Do not just memorize the "leftmost prefix" rule.

Prove the behavior with experiments.

Test queries using:

``` text
user_id only
status only
user_id + status
user_id + created_at
status + created_at
all three
```

Document the observed behavior.

------------------------------------------------------------------------

# 14. Step 9 --- Covering / Index-Only Scan

Create a query that returns a small set of columns.

Example:

``` sql
SELECT user_id, status, created_at
FROM orders
WHERE user_id = 100
AND status = 'COMPLETED';
```

Create an appropriate index.

Where supported by the chosen design, experiment with included columns:

``` sql
CREATE INDEX idx_orders_user_status
ON orders(user_id, status)
INCLUDE (created_at);
```

Run:

``` sql
EXPLAIN ANALYZE
SELECT user_id, status, created_at
FROM orders
WHERE user_id = 100
AND status = 'COMPLETED';
```

Investigate whether PostgreSQL can use an:

``` text
Index Only Scan
```

Document why an index-only scan may still need heap access in some
situations.

Do not turn this into a PostgreSQL internals deep dive yet.

The objective is understanding the practical consequence.

------------------------------------------------------------------------

# 15. Step 10 --- Index Maintenance Cost

Indexes are not free.

Measure or reason about the effect of indexes on:

``` text
INSERT
UPDATE
DELETE
```

Create a test table with:

``` text
0 indexes
```

Then add:

``` text
1 index
```

Then:

``` text
multiple indexes
```

Measure write performance.

The objective is to understand the trade-off:

``` text
Faster reads
      vs
More expensive writes
      +
More storage
      +
More maintenance
```

Document this explicitly.

------------------------------------------------------------------------

# 16. Step 11 --- Transactions

Now move from query performance to correctness.

Create two separate database sessions.

Conceptually:

``` text
Transaction A
Transaction B
```

Use the same row or related rows.

Start with PostgreSQL's default isolation level:

``` text
READ COMMITTED
```

Verify it:

``` sql
SHOW transaction_isolation;
```

or:

``` sql
SELECT current_setting('transaction_isolation');
```

------------------------------------------------------------------------

# 17. Step 12 --- READ COMMITTED Experiment

Construct a scenario where:

``` text
Transaction A
    |
    | SELECT
    |
Transaction B
    |
    | UPDATE
    |
Transaction A
    |
    | SELECT again
```

Observe what Transaction A sees.

Do not rely on a textbook explanation.

Run the actual concurrent transactions.

Document:

-   transaction boundaries
-   SQL statements
-   ordering
-   observed results
-   explanation

Create a timeline such as:

``` text
T1: BEGIN
T1: SELECT ...

T2: BEGIN
T2: UPDATE ...
T2: COMMIT

T1: SELECT ...
T1: COMMIT
```

------------------------------------------------------------------------

# 18. Step 13 --- REPEATABLE READ

Repeat a similar experiment using:

``` sql
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

Compare with `READ COMMITTED`.

Document:

``` text
What did Transaction A see?
What changed?
What did not change?
```

The objective is to understand snapshot behavior rather than memorize
isolation-level names.

------------------------------------------------------------------------

# 19. Step 14 --- SERIALIZABLE

Repeat the experiment under:

``` sql
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
```

Create a conflict.

Observe whether PostgreSQL:

-   blocks
-   aborts a transaction
-   raises a serialization failure

Then answer:

> What must the application do when a serializable transaction fails?

This naturally introduces the concept of retrying transactions.

------------------------------------------------------------------------

# 20. Step 15 --- Build an Isolation-Level Comparison

Create a table based on your actual experiments.

  -----------------------------------------------------------------------
  Property          READ COMMITTED    REPEATABLE READ   SERIALIZABLE
  ----------------- ----------------- ----------------- -----------------
  Default           Verify            No                No
  PostgreSQL                                            
  isolation                                             

  Snapshot behavior Experiment        Experiment        Experiment

  Concurrent        Experiment        Experiment        Experiment
  modification                                          
  behavior                                              

  Serialization     Observe           Observe           Observe
  failures                                              

  Application retry Analyze           Analyze           Analyze
  requirement                                           
  -----------------------------------------------------------------------

Do not fill the table from memory.

The purpose of the table is to summarize your experiments.

------------------------------------------------------------------------

# 21. Step 16 --- Row-Level Locking

Introduce:

``` sql
SELECT ...
FROM inventory
WHERE product_id = ?
FOR UPDATE;
```

Use the Forge inventory domain.

Create:

``` text
Product A
Quantity = 10
```

Open two transactions.

Transaction A:

``` sql
BEGIN;

SELECT quantity
FROM inventory
WHERE product_id = 1
FOR UPDATE;
```

Do not commit yet.

Transaction B executes:

``` sql
BEGIN;

SELECT quantity
FROM inventory
WHERE product_id = 1
FOR UPDATE;
```

Observe the behavior.

Transaction B should encounter the lock held by Transaction A.

Document:

-   who owns the lock
-   who waits
-   when the lock is released
-   what happens after commit
-   what happens after rollback

------------------------------------------------------------------------

# 22. Step 17 --- Inventory Locking Experiment

Use the actual Forge inventory operation.

Start with:

``` text
quantity = 10
```

Run concurrent purchase attempts.

First implement a deliberately unsafe version:

``` text
read quantity
check quantity
write quantity - requested
```

Then break it with concurrency.

Document the failure.

This connects Phase 3 to the future Phase 4 inventory-concurrency
challenge.

The roadmap specifically states that Phase 4 will compare:

-   naive read/update
-   transaction
-   pessimistic locking
-   optimistic locking
-   atomic SQL

Therefore, Phase 3 should establish the database concepts required to
understand those later comparisons.

------------------------------------------------------------------------

# 23. Step 18 --- Deadlock Experiment

Deliberately create a deadlock.

Use two rows:

``` text
Row A
Row B
```

Transaction 1:

``` text
Lock A
↓
Attempt Lock B
```

Transaction 2:

``` text
Lock B
↓
Attempt Lock A
```

Conceptually:

``` text
Transaction A
    locks Row A
        ↓
    waits for Row B

Transaction B
    locks Row B
        ↓
    waits for Row A
```

PostgreSQL should eventually detect the deadlock and abort one
transaction.

## Do not stop at reproducing it

Investigate:

-   which transaction failed
-   what PostgreSQL reported
-   how the application sees the error
-   how lock ordering could prevent the deadlock
-   whether retrying is appropriate

Document the actual PostgreSQL error.

------------------------------------------------------------------------

# 24. Step 19 --- Deadlock Prevention Experiment

Change the implementation so both transactions acquire locks in the same
order.

For example:

``` text
Always lock Product A before Product B
```

instead of:

``` text
Transaction A: A → B
Transaction B: B → A
```

Use:

``` text
Transaction A: A → B
Transaction B: A → B
```

Run the experiment again.

Compare:

``` text
Before:
Deadlock

After:
No deadlock
```

The key engineering lesson is:

> Consistent lock ordering is one practical technique for reducing
> deadlock risk.

------------------------------------------------------------------------

# 25. Step 20 --- Optimistic Locking

Add a version field to a suitable Forge entity.

Example:

``` java
@Version
private Long version;
```

Use a domain where concurrent modification is meaningful.

Inventory is a strong candidate, but an order or product can also be
used for a controlled experiment.

Create:

``` text
Entity version = 1
```

Load the same entity into two application transactions.

Conceptually:

``` text
Transaction A → version 1
Transaction B → version 1
```

Transaction A updates first:

``` text
version 1 → 2
```

Transaction B then attempts to update its stale version.

Observe the failure.

------------------------------------------------------------------------

# 26. Step 21 --- Understand Hibernate's Role

Now connect the database experiment to Spring Data JPA/Hibernate.

Investigate:

``` text
Entity
  ↓
Hibernate
  ↓
SQL
  ↓
JDBC
  ↓
HikariCP
  ↓
PostgreSQL
```

Enable SQL logging carefully.

Do not rely only on:

``` properties
spring.jpa.show-sql=true
```

Use appropriate Hibernate logging/configuration to understand generated
SQL and parameters in your development environment.

Observe:

-   when SQL is generated
-   when SQL is actually executed
-   when transactions begin
-   when transactions commit
-   what SQL is issued for updates
-   how the version field appears in the update condition

The objective is to stop treating JPA as a black box.

------------------------------------------------------------------------

# 27. Step 22 --- HikariCP Investigation

Now investigate the connection pool.

Find the Forge HikariCP configuration.

Record the effective pool settings.

At minimum understand:

``` text
maximumPoolSize
minimumIdle
connectionTimeout
idleTimeout
maxLifetime
```

Do not tune them yet.

First understand what each one controls.

------------------------------------------------------------------------

# 28. Step 23 --- Connection Pool Saturation

Create a deliberately slow database operation.

For example, in a controlled experiment:

``` sql
SELECT pg_sleep(5);
```

Do not use this in production application logic.

Configure a deliberately small pool, for example:

``` text
maximumPoolSize = 5
```

Generate more concurrent requests than the pool can satisfy.

Example:

``` text
50 concurrent requests
5 DB connections
```

Observe:

``` text
Requests
    ↓
Threads
    ↓
Waiting for DB connection
    ↓
HikariCP pool
    ↓
PostgreSQL
```

Measure:

-   request latency
-   connection acquisition time
-   active connections
-   idle connections
-   waiting requests
-   timeout behavior

------------------------------------------------------------------------

# 29. Step 24 --- Pool Size Experiment

Run controlled tests with different pool sizes.

For example:

``` text
2
5
10
20
```

Keep the workload as similar as possible.

Record:

    Pool Size   Concurrency   Throughput   p95 Latency    Errors Notes
  ----------- ------------- ------------ ------------- --------- -------------
            2        record      measure       measure   measure observation
            5        record      measure       measure   measure observation
           10        record      measure       measure   measure observation
           20        record      measure       measure   measure observation

Do not assume:

``` text
larger pool = better performance
```

Your goal is to observe the point where increasing database concurrency
stops helping.

------------------------------------------------------------------------

# 30. Step 25 --- Application-Level Concurrency Experiment

Create a test that performs many concurrent database operations.

Use Java concurrency tools you learned earlier.

Possible tools:

``` text
ExecutorService
CountDownLatch
CyclicBarrier
CompletableFuture
```

For example:

``` text
100 concurrent tasks
        ↓
same inventory record
        ↓
database
```

The test should be deterministic enough to reproduce the behavior.

Record:

-   successful operations
-   failed operations
-   exceptions
-   final database state
-   execution time

This is a bridge between:

``` text
Java concurrency
```

and:

``` text
Database concurrency
```

------------------------------------------------------------------------

# 31. Step 26 --- Compare Concurrency Strategies

Perform a controlled comparison using the same workload.

Compare:

## Strategy A --- Naive

``` text
read
check
write
```

## Strategy B --- Transaction only

``` text
@Transactional
read
check
write
```

## Strategy C --- Pessimistic locking

``` text
SELECT ... FOR UPDATE
```

## Strategy D --- Optimistic locking

``` text
@Version
```

## Strategy E --- Atomic SQL

Example:

``` sql
UPDATE inventory
SET quantity = quantity - 1
WHERE product_id = ?
AND quantity >= 1;
```

For each strategy record:

``` text
Correctness
Throughput
Latency
Failure behavior
Lock contention
Complexity
```

Do not decide which strategy is "best" universally.

Identify the conditions under which each approach is appropriate.

------------------------------------------------------------------------

# 32. Step 27 --- Query Plan Regression

Create a query that initially performs well.

Then alter the data distribution.

For example:

``` text
Before:
status values evenly distributed

After:
90% of rows = COMPLETED
```

Run:

``` sql
EXPLAIN ANALYZE
```

again.

Investigate whether the planner changes its strategy.

This demonstrates that query performance is affected by:

``` text
data distribution
statistics
selectivity
indexes
query structure
```

------------------------------------------------------------------------

# 33. Step 28 --- Hibernate vs Native SQL

Choose one Forge operation.

Implement it using:

``` text
Spring Data repository
```

and compare with:

``` text
JPQL
```

and, where appropriate:

``` text
native SQL
```

Do not assume native SQL is automatically faster.

Compare:

-   generated SQL
-   execution plan
-   execution time
-   maintainability
-   correctness
-   database coupling

The objective is to learn when the abstraction is sufficient and when
understanding the SQL underneath becomes necessary.

------------------------------------------------------------------------

# 34. Step 29 --- Build the Database Experiment Journal

Create:

``` text
/docs/database-experiments.md
```

Use the following structure:

``` markdown
# PostgreSQL Experiments

## Environment

## Dataset

## Experiment 1 — Sequential Scan

### Hypothesis

### Setup

### Query

### EXPLAIN ANALYZE

### Observation

### Conclusion

## Experiment 2 — B-tree Index

...

## Experiment 3 — Selectivity

...

## Experiment 4 — Composite Index

...

## Experiment 5 — Covering Index

...

## Experiment 6 — READ COMMITTED

...

## Experiment 7 — REPEATABLE READ

...

## Experiment 8 — SERIALIZABLE

...

## Experiment 9 — SELECT FOR UPDATE

...

## Experiment 10 — Deadlock

...

## Experiment 11 — Optimistic Locking

...

## Experiment 12 — HikariCP

...

## Final Findings
```

Every experiment should contain:

``` text
Hypothesis
Setup
Execution
Observed result
Explanation
Engineering implication
```

------------------------------------------------------------------------

# 35. Required Experiment Matrix

Before declaring Phase 3 complete, the following experiments should
exist.

    \# Experiment                                  Required
  ---- ----------------------------------------- -------------
     1 PostgreSQL baseline                            Yes
     2 Sequential scan                                Yes
     3 B-tree index                                   Yes
     4 Index selectivity                              Yes
     5 Composite index                                Yes
     6 Composite-index column order                   Yes
     7 Covering/index-only scan                       Yes
     8 Index write cost                               Yes
     9 READ COMMITTED                                 Yes
    10 REPEATABLE READ                                Yes
    11 SERIALIZABLE                                   Yes
    12 `SELECT FOR UPDATE`                            Yes
    13 Deadlock reproduction                          Yes
    14 Deadlock prevention                            Yes
    15 Hibernate optimistic locking                   Yes
    16 HikariCP baseline                              Yes
    17 HikariCP saturation                            Yes
    18 Pool-size comparison                           Yes
    19 Concurrent application test                    Yes
    20 Concurrency strategy comparison                Yes
    21 Query-plan/data-distribution experiment    Recommended
    22 JPA/JPQL/native SQL comparison             Recommended

------------------------------------------------------------------------

# 36. Documentation Requirements

The database documentation should not look like copied tutorial
material.

Bad:

``` text
A transaction is a logical unit of work...
```

Better:

``` text
I ran Transaction A and Transaction B against the same inventory row.

Transaction A acquired the FOR UPDATE lock.
Transaction B blocked until Transaction A committed.

This demonstrated that the lock was held until the transaction boundary,
not until the SELECT statement completed.
```

The document should answer:

> What did I actually observe?

------------------------------------------------------------------------

# 37. SQL and Application Artifacts

Organize experiments cleanly.

Suggested structure:

``` text
forge
├── docs
│   └── database-experiments.md
│
├── src
│   ├── main
│   │   └── java
│   │
│   └── test
│       └── java
│           └── ...
│
└── database
    └── experiments
        ├── indexes
        ├── transactions
        ├── locking
        ├── deadlocks
        └── connection-pool
```

If SQL scripts are useful, keep them reproducible.

Example:

``` text
database/experiments/indexes/01_baseline.sql
database/experiments/indexes/02_btree.sql
database/experiments/transactions/read-committed.sql
database/experiments/transactions/repeatable-read.sql
database/experiments/locking/for-update.sql
database/experiments/deadlocks/deadlock-a.sql
database/experiments/deadlocks/deadlock-b.sql
```

------------------------------------------------------------------------

# 38. Testing Strategy

Phase 3 should contain three complementary types of tests.

## 38.1 Database integration tests

Use the real PostgreSQL database where practical.

Verify:

-   transactions
-   locking
-   optimistic locking
-   constraints
-   query behavior

## 38.2 Concurrency tests

Run multiple threads against the same database state.

Test:

``` text
same row
same inventory
same order
same user
```

The expected final state must be explicit.

## 38.3 Manual database experiments

Some experiments are easier to understand through two database sessions.

Use:

``` text
psql
DBeaver
IntelliJ database tools
VS Code database tooling
```

The important thing is to make the transaction ordering visible.

------------------------------------------------------------------------

# 39. Debugging Workflow

When a database experiment produces an unexpected result, use this
workflow:

``` text
Unexpected behavior
        ↓
Reproduce
        ↓
Record exact SQL
        ↓
Record transaction boundaries
        ↓
Check isolation level
        ↓
Check locks
        ↓
Run EXPLAIN ANALYZE
        ↓
Inspect application logs
        ↓
Inspect Hibernate SQL
        ↓
Inspect connection-pool behavior
        ↓
Form hypothesis
        ↓
Change one variable
        ↓
Repeat experiment
```

Do not change five settings simultaneously.

The purpose of the phase is to develop investigative discipline.

------------------------------------------------------------------------

# 40. Engineering Questions You Must Be Able to Answer

Before moving to Phase 4, answer these without looking them up.

## Query performance

1.  Why might PostgreSQL choose a sequential scan when an index exists?
2.  What does `EXPLAIN ANALYZE` actually do?
3.  What makes an index selective?
4.  Why does composite-index column order matter?
5.  What is an index-only scan?
6.  Why do indexes slow down writes?

## Transactions

7.  What does transaction isolation control?
8.  What did you observe under `READ COMMITTED`?
9.  What changed under `REPEATABLE READ`?
10. What happens under `SERIALIZABLE` when transactions conflict?
11. Why might an application need to retry a transaction?

## Locking

12. What does `SELECT ... FOR UPDATE` do?
13. When is the lock released?
14. How does a deadlock occur?
15. How can consistent lock ordering reduce deadlocks?

## Optimistic locking

16. What does `@Version` protect against?
17. What happens when two transactions update the same version?
18. What is the trade-off between optimistic and pessimistic locking?

## Connection pools

19. Why does HikariCP exist?
20. What happens when all database connections are busy?
21. Why is increasing the pool size not an unlimited performance
    solution?
22. How can a database bottleneck appear as application latency?

------------------------------------------------------------------------

# 41. Phase 3 Engineering Challenge

Implement the following scenario:

> Forge has an inventory item with quantity 10. One hundred concurrent
> users attempt to purchase one unit each.

Requirements:

``` text
Initial inventory = 10
Concurrent requests = 100
```

Expected:

``` text
Successful purchases <= 10
Final inventory >= 0
```

Implement at least:

``` text
1. Naive implementation
2. Pessimistic locking implementation
3. Optimistic locking implementation
4. Atomic SQL implementation
```

Run the same concurrency test against each.

Record:

``` text
Successful purchases
Failed purchases
Final inventory
Execution time
Database contention
Exceptions
Retries
```

Then explain:

> Why did the implementations behave differently?

This challenge is the direct bridge into Phase 4 --- Inventory
Concurrency.

------------------------------------------------------------------------

# 42. Definition of Done

Phase 3 is complete only when all of the following are true.

## Database

-   [ ] PostgreSQL environment is documented.
-   [ ] Forge contains a sufficiently large experimental dataset.
-   [ ] `EXPLAIN ANALYZE` is used on real Forge queries.
-   [ ] Sequential scans have been observed.
-   [ ] B-tree indexes have been created and tested.
-   [ ] Selectivity has been experimentally investigated.
-   [ ] Composite indexes have been tested.
-   [ ] Composite-index column ordering has been tested.
-   [ ] Covering/index-only behavior has been investigated.
-   [ ] Index write cost has been measured or experimentally compared.

## Transactions

-   [ ] `READ COMMITTED` has been tested with concurrent transactions.
-   [ ] `REPEATABLE READ` has been tested.
-   [ ] `SERIALIZABLE` has been tested.
-   [ ] Observed behavior has been documented.
-   [ ] Serialization failure behavior has been observed where
    applicable.

## Locking

-   [ ] `SELECT ... FOR UPDATE` has been tested.
-   [ ] Concurrent lock behavior has been observed.
-   [ ] A deadlock has been deliberately reproduced.
-   [ ] The deadlock has been investigated.
-   [ ] A lock-ordering strategy has been tested.

## Hibernate

-   [ ] Hibernate-generated SQL has been inspected.
-   [ ] Optimistic locking using `@Version` has been implemented.
-   [ ] Concurrent updates have been tested.
-   [ ] Optimistic-lock failure has been observed.

## HikariCP

-   [ ] HikariCP configuration has been inspected.
-   [ ] Connection-pool saturation has been reproduced.
-   [ ] Different pool sizes have been compared.
-   [ ] Connection acquisition behavior is understood.

## Concurrency

-   [ ] A real concurrent Forge database test exists.
-   [ ] The inventory race condition has been reproduced.
-   [ ] Multiple concurrency strategies have been compared.
-   [ ] Final database correctness has been verified.

## Documentation

-   [ ] `/docs/database-experiments.md` exists.
-   [ ] Experiments contain actual observations.
-   [ ] SQL used for important experiments is reproducible.
-   [ ] Unexpected results are documented.
-   [ ] Trade-offs are documented.
-   [ ] Phase 3 engineering questions can be answered without relying on
    copied notes.

------------------------------------------------------------------------

# 43. Git / Commit Strategy

Keep the commits small enough to represent meaningful engineering
changes.

Suggested sequence:

``` text
feat(database): establish PostgreSQL experiment environment

test(database): add realistic experimental dataset

docs(database): document PostgreSQL baseline

test(database): add sequential scan experiments

feat(database): add B-tree indexes for query experiments

docs(database): document index behavior

test(database): add transaction isolation experiments

test(database): add pessimistic locking experiments

test(database): reproduce database deadlock

docs(database): document deadlock investigation

feat(order): add optimistic locking

test(order): verify optimistic locking under concurrency

test(database): add HikariCP saturation experiment

test(inventory): compare concurrency control strategies

docs(database): complete PostgreSQL experiment journal
```

Avoid one enormous:

``` text
feat: complete phase 3
```

commit.

The history should demonstrate the engineering progression.

------------------------------------------------------------------------

# 44. What Not to Do

Do not:

-   memorize PostgreSQL isolation levels without running concurrent
    transactions
-   create indexes without checking query plans
-   assume an index is always faster
-   benchmark on tiny datasets and generalize the result
-   increase HikariCP pool size without understanding the bottleneck
-   hide database behavior behind JPA
-   skip deadlock reproduction because it is inconvenient
-   use copied `EXPLAIN ANALYZE` examples as your experiment results
-   tune PostgreSQL configuration before understanding the baseline
-   introduce Redis, Kafka, Kubernetes, or microservices during this
    phase
-   prematurely optimize

The roadmap's philosophy is:

``` text
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

Apply that philosophy here.

------------------------------------------------------------------------

# 45. Final Phase Review

At the end of Phase 3, sit down with the Forge codebase and explain this
entire path:

``` text
HTTP Request
     ↓
Spring Controller
     ↓
Service
     ↓
@Transactional
     ↓
Hibernate
     ↓
JDBC
     ↓
HikariCP
     ↓
PostgreSQL
     ↓
Query Planner
     ↓
Index / Scan
     ↓
Transaction
     ↓
Locks / MVCC
     ↓
Commit / Rollback
```

You should be able to reason about where a problem might exist.

For example:

``` text
API latency increased
        ↓
Is application code slow?
        ↓
Is a connection unavailable?
        ↓
Is HikariCP saturated?
        ↓
Is PostgreSQL slow?
        ↓
Is the query plan bad?
        ↓
Is there a missing/ineffective index?
        ↓
Is the query waiting on a lock?
        ↓
Is transaction contention high?
        ↓
Is the database overloaded?
```

That reasoning ability is more important than memorizing PostgreSQL
commands.

------------------------------------------------------------------------

# 46. Transition to Phase 4

Do not move to Phase 4 merely because the checklist is complete.

You should be able to explain the database behavior behind the inventory
problem.

Phase 4 begins with:

``` text
Product A
Inventory = 10

100 concurrent buyers
        ↓
Purchase 1 each
```

The next phase will systematically compare:

``` text
Naive read/update
        ↓
@Transactional
        ↓
Pessimistic locking
        ↓
Optimistic locking
        ↓
Atomic SQL
```

Phase 3 gives you the database knowledge needed to understand why each
approach behaves differently.

The transition should therefore be:

``` text
Phase 3
PostgreSQL
   ↓
Transactions
   ↓
Isolation
   ↓
Locks
   ↓
Optimistic concurrency
   ↓
Connection pooling
        ↓
Phase 4
Inventory concurrency
```

------------------------------------------------------------------------

# 47. Final Success Criterion

The phase succeeds when you can look at a Forge database problem and
reason about it from first principles:

``` text
Requirement
    ↓
SQL
    ↓
Query Plan
    ↓
Index
    ↓
Transaction
    ↓
Isolation
    ↓
Locking
    ↓
Concurrency
    ↓
Connection Pool
    ↓
Observed Performance
    ↓
Correctness
```

The intended outcome is not:

> "I know PostgreSQL."

It is:

> **"When my Spring Boot application behaves unexpectedly around data, I
> can investigate the database instead of treating JPA and PostgreSQL as
> a black box."**
