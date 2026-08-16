# Forge Commerce Platform: Phase 3 Implementation Report

## 1. Purpose

Phase 3 focused on understanding PostgreSQL as the persistence and concurrency boundary of the Forge Commerce Platform. The work was intentionally broader than adding JPA annotations. It connected database behavior to the modular monolith and measured:

- Query planning and scan selection
- B-tree, composite, and covering indexes
- Selectivity and data distribution
- Transaction isolation
- Row locks and deadlocks
- Hibernate optimistic locking
- Inventory reservation under concurrent buyers
- JDBC connection-pool saturation
- The trade-offs between application-level and database-level concurrency strategies

The implementation keeps Forge as one Spring Boot modular monolith using one PostgreSQL database. Kafka, Redis, microservices, Kubernetes, and an API gateway were not introduced because they belong to later phases.

## 2. Starting Point

Before this phase, Forge already contained the main domain modules and in-memory persistence adapters. The relevant structure included catalog, inventory, order, user, job, notification, payment, and common modules.

The main Phase 3 constraint was to preserve the existing default behavior while adding a real PostgreSQL path for local experiments. This resulted in two persistence modes:

- Default and test profiles continue to use in-memory repositories where those adapters exist.
- The `local` profile uses PostgreSQL, Hibernate/JPA, Flyway, and HikariCP for the inventory persistence slice.

This allowed the existing unit and web tests to remain fast and deterministic while making the database-backed behavior reproducible when the live PostgreSQL container is available.

## 3. Implementation Sequence

The work proceeded in the following order:

1. Inspect the existing modular-monolith structure, domain invariants, ports, repositories, and tests.
2. Establish the live PostgreSQL environment in the `forge-postgres` Docker container.
3. Add the PostgreSQL, JPA, Flyway, and H2 dependencies.
4. Add a `local` Spring profile with Docker database credentials and Hikari settings.
5. Map the inventory and reservation aggregates to relational tables.
6. Add the first Flyway migration and verify that Hibernate validation succeeds.
7. Run SQL experiments against a deliberately skewed one-million-row dataset.
8. Add application-level experiments for pessimistic, optimistic, and atomic concurrency control.
9. Add a Hibernate `@Version` stale-update experiment.
10. Add a HikariCP saturation experiment and compare pool sizes.
11. Fix startup, mapping, concurrency-harness, and profile regressions discovered during live execution.
12. Record actual measurements and conclusions in `Docs/database-experiments.md`.
13. Run the default suite, gated live tests, readiness checks, diagnostics, and whitespace validation.

All database shell experiments were run through Docker, for example:

```fish
docker exec -i forge-postgres psql -U forge -d forge < database/experiments/indexes/01_setup.sql
```

This avoided requiring `psql` or `pg_isready` to be installed on the host. Database readiness was checked with:

```fish
docker exec forge-postgres pg_isready -U forge -d forge
```

## 4. Build and Runtime Foundation

### 4.1 Maven dependencies

`pom.xml` was extended with:

- `spring-boot-starter-data-jpa`
- PostgreSQL JDBC driver
- `spring-boot-starter-flyway`
- Flyway core
- Flyway PostgreSQL database support
- H2 for test/runtime compatibility where needed

The project uses Java 21, Spring Boot 4.1.0, and Hibernate 7.4.1.Final.

### 4.2 Local profile

`src/main/resources/application-local.properties` configures:

- JDBC URL: `jdbc:postgresql://localhost:5432/forge`
- Database user: `forge`
- Database password: `forge_dev_password`
- `spring.jpa.hibernate.ddl-auto=validate`
- `spring.flyway.enabled=true`
- Open Session in View disabled
- Hibernate SQL and bind-value logging
- Hikari maximum pool size of 5
- Hikari minimum idle connections of 2
- A 30-second connection acquisition timeout

The schema is owned by Flyway. Hibernate validates the schema rather than creating or altering it automatically.

### 4.3 Profile-specific repository selection

The in-memory inventory and reservation adapters are active for profiles other than `local`. The JPA adapters are active for `local`.

This profile separation is important because it prevents the production-shaped database adapter from replacing the simpler unit-test adapter in every test. It also makes the live database path explicit when running the application or gated integration experiments.

## 5. Relational Model and Migration

The migration at `src/main/resources/db/migration/V1__create_inventory_table.sql` creates two tables.

### 5.1 Inventory table

The `inventory` table contains:

- `id UUID PRIMARY KEY`
- `product_id UUID NOT NULL`
- `available_quantity BIGINT NOT NULL`
- `reserved_quantity BIGINT NOT NULL`
- `version BIGINT NOT NULL DEFAULT 0`

Database constraints enforce non-negative available and reserved quantities. A unique constraint ensures one inventory row per product.

### 5.2 Reservation table

The `inventory_reservations` table contains:

- Reservation id
- Order id
- Product id
- Quantity
- Reservation status
- Expiration timestamp

A composite index on `(product_id, status)` supports product/status reservation lookups.

### 5.3 Migration verification

Flyway successfully applied version `V1`. Hibernate startup validation then confirmed that the mapped entity structure matched the migrated schema.

## 6. Hibernate and Domain Mapping

`Inventory` is now a JPA entity while retaining its domain behavior. The aggregate still owns validation and state transitions:

- `reserve(quantity)` decreases available quantity and increases reserved quantity.
- `release(quantity)` moves quantity back to available inventory.
- `increase(quantity)` adds stock.
- Invalid quantities and impossible transitions raise domain exceptions.

The entity has a JPA `@Version` field backed by the `inventory.version` column. Hibernate includes the version in update predicates and increments it after a successful update.

### 6.1 Hibernate 7 identifier issue

The first implementation attempted to use an `AttributeConverter` on an id value object. Hibernate 7 rejected this mapping because converters are not allowed on attributes annotated with `@Id`.

The fix was:

- Persist the identifier as a raw `UUID`.
- Expose the domain-specific id wrapper through the getter.
- Keep the id value object in the domain API without applying a converter to the JPA id field.

The same approach was applied to reservation identifiers.

### 6.2 Quantity conversion

The domain quantity type is converted for persistence through `QuantityConverter`. A missing `jakarta.persistence.Convert` import caused a startup failure during the first mapping attempt and was restored before the live profile was revalidated.

## 7. Persistence Adapters

The persistence boundary remains expressed through application ports rather than leaking Spring Data types into the application service.

The local JPA inventory adapter delegates to a Spring Data repository. The Spring Data repository provides:

- Normal lookup by product id
- Save support
- A `findByProductIdForUpdate` query using `@Lock(LockModeType.PESSIMISTIC_WRITE)`

The reservation adapter uses a UUID-keyed Spring Data repository and maps persistence entities back to the application-facing repository contract.

## 8. Inventory Reservation Application Flow

`InventoryReservationService` remains responsible for the reservation use case:

1. Find inventory for the product.
2. Fail with not-found behavior when inventory does not exist.
3. Apply the domain `reserve` operation.
4. Translate an invalid reservation into a conflict response.
5. Save the changed inventory.
6. Create and save a pending reservation with a one-hour expiration.

Reservation release and consumption are transactional state transitions on the reservation aggregate.

The service uses `@Transactional` for the database-backed path. For the in-memory adapter, a synchronized branch preserves the atomic behavior expected by the existing unit tests. PostgreSQL concurrency is deliberately left to database transactions and row/version control rather than a JVM-wide lock.

## 9. SQL Experiment Dataset

The SQL experiment setup under `database/experiments/indexes/01_setup.sql` creates one million intentionally skewed order-like rows.

Observed distribution:

- 900,000 rows with status `COMPLETED`
- 100,000 rows with status `PROCESSING`
- A rare user lookup returning one row
- A small group of users associated with the common data

The skew makes planner decisions visible. A predicate returning one row and a predicate returning most of the table should not use the same execution strategy.

The experiment scripts are organized as follows:

- `indexes/`: setup, query plans, write cost, and column-order regression
- `transactions/`: `READ COMMITTED`, `REPEATABLE READ`, and `SERIALIZABLE`
- `locking/`: `SELECT FOR UPDATE`
- `deadlocks/`: opposing and consistent lock order
- `connection-pool/`: `pg_sleep` workload and pool instructions

## 10. Query Planning and Index Experiments

### 10.1 Sequential scan baseline

Before indexing, the rare `user_id` query used a parallel sequential scan and returned one row in 25.033 ms.

The common `status = 'COMPLETED'` query also used a sequential scan, returned 900,000 rows, and took 77.080 ms. Reading most of the table made the sequential plan reasonable even though an index existed later.

### 10.2 B-tree index

After adding a B-tree index on the selective user column, the rare lookup changed to a bitmap heap/index scan and returned one row in 0.032 ms.

The result demonstrates that an index is valuable when it eliminates most table reads, but the planner can correctly reject an index when the predicate matches a large portion of the relation.

### 10.3 Composite and covering indexes

The composite index `(user_id, status, created_at DESC)` supported the combined predicate and ordering. It produced an index-only scan with one heap fetch and took 0.145 ms.

After `VACUUM ANALYZE`, the covering index produced an index-only scan with zero heap fetches and took 0.034 ms.

The important observation is that an index-only scan still depends on visibility-map state. Included columns can provide a narrow read model, but the table may still need heap visits when PostgreSQL cannot prove that the relevant pages are visible to the transaction.

### 10.4 Column order and distribution regression

The reordered `(status, user_id, created_at)` index produced an index-only scan for the combined predicate at 0.043 ms.

The status-only `PROCESSING` query used the status-leading index and returned 100,000 rows in 14.964 ms.

After updating all 100,000 `PROCESSING` rows to `COMPLETED` and running `ANALYZE`, the `COMPLETED` query returned one million rows through a sequential scan in 80.312 ms.

This shows that column order matters for the predicates being served, but selectivity and current data distribution still control whether an index is worthwhile.

### 10.5 Index write cost

The indexed workload measured:

- Insert of 100,000 rows: 480.757 ms
- Delete of 50,000 rows: 28.723 ms
- Table plus index relation size: 24 MB

The experiment records the cost of maintaining secondary indexes during writes. Those indexes improve reads only when the access pattern and selectivity justify their storage and maintenance overhead.

## 11. Transaction Isolation Experiments

The transaction scripts were executed using Docker-backed PostgreSQL sessions.

### 11.1 READ COMMITTED

Under PostgreSQL's default `READ COMMITTED` isolation:

1. Transaction A read quantity 10.
2. Session B committed an increment.
3. Transaction A read the row again and saw 11.

Each statement received a fresh committed snapshot.

### 11.2 REPEATABLE READ

Under `REPEATABLE READ`:

1. Transaction A read quantity 10.
2. Session B committed an increment.
3. Transaction A read again and still saw 10.

The transaction retained its original snapshot instead of seeing the later committed value.

### 11.3 SERIALIZABLE

The conflicting serializable workload allowed one transaction to commit and caused the other to fail with:

```text
could not serialize access due to concurrent update
```

This demonstrates that `SERIALIZABLE` protects the serializable execution contract by aborting conflicting work. A production caller would need a bounded retry policy for retryable serialization failures; the experiment records the failure behavior rather than hiding it.

## 12. Row Locks and Deadlocks

### 12.1 `SELECT FOR UPDATE`

The row-lock experiment showed that the second session waited while the first transaction held its lock for two seconds. The second session continued only after the first transaction committed.

The application-side pessimistic strategy uses the same principle: lock the inventory row inside the transaction before checking and changing the quantity.

### 12.2 Deadlock reproduction

Two sessions locking two rows in opposite order produced:

```text
ERROR: deadlock detected
```

PostgreSQL identified the waiting processes and aborted one transaction. Re-running both sessions with the same lock order completed without a deadlock.

The engineering lesson is that a transaction can be individually correct and still participate in a deadlock when multiple transactions acquire shared resources in inconsistent orders.

## 13. Optimistic Locking Experiment

The Hibernate experiment loaded two detached `Inventory` instances at version 0.

1. The first update committed and changed the row version to 1.
2. The second stale update still carried version 0.
3. Hibernate's version predicate matched zero rows.
4. Hibernate raised `OptimisticLockException` instead of silently overwriting the first update.

This verifies the behavior of the `@Version` mapping and demonstrates why optimistic locking is useful when conflicts are possible but not expected on every request.

## 14. Concurrent Inventory Strategies

`PostgresInventoryConcurrencyExperimentTest` is a live Spring Boot test enabled only with:

```fish
./mvnw -q -Dtest=PostgresInventoryConcurrencyExperimentTest -Dforge.postgres.tests=true test
```

The test creates inventory with quantity 10 and runs 100 buyers through a 20-thread executor. It compares four strategies.

| Strategy | Successful purchases | Final quantity | Elapsed time |
| --- | ---: | ---: | ---: |
| Naive read/check/write | 93 | 3 | 221 ms |
| Pessimistic row lock | 10 | 0 | 255 ms |
| Optimistic version update/retry | 10 | 0 | 136 ms |
| Atomic conditional SQL update | 10 | 0 | 36 ms |

### 14.1 Naive strategy

The naive strategy performs a read, checks the value in application code, and writes the calculated value. Concurrent requests can read the same quantity and overwrite one another's changes. It did not oversell in this run because the final value remained non-negative, but it lost successful decrements: only 93 buyers reported success while the final quantity was 3.

### 14.2 Pessimistic strategy

The pessimistic strategy uses a transaction and `SELECT ... FOR UPDATE`. It serialized access to the inventory row and produced exactly 10 successful purchases with no remaining quantity.

The trade-off is lock waiting. This approach is straightforward when contention is expected and the protected operation must be performed against the latest row state.

### 14.3 Optimistic strategy

The optimistic strategy reads the quantity and version, then updates only when the version is unchanged. Conflicting updates retry up to a bounded limit.

It also produced exactly 10 successful purchases. It completed faster than the pessimistic run in this measurement, but its cost moves into retry handling and conflict management.

### 14.4 Atomic conditional update

The atomic strategy performs the business condition and decrement in one SQL statement:

```sql
UPDATE inventory
SET available_quantity = available_quantity - 1,
    version = version + 1
WHERE product_id = ?
  AND available_quantity >= 1
```

The affected-row count determines success. It produced exactly 10 successful purchases and was the fastest measured strategy at 36 ms.

The statement is concise and efficient for this single invariant, but it is specialized to the operation. More complex aggregate changes may still require a row lock or an optimistic transaction.

## 15. HikariCP Saturation Experiment

`PostgresHikariSaturationExperimentTest` runs 20 concurrent one-second `pg_sleep` requests and records Hikari pool metrics. The local profile uses a maximum pool size of 5 and a minimum idle size of 2.

Observed p95 latencies:

| Maximum pool size | p95 latency |
| ---: | ---: |
| 2 | 10,017 ms |
| 5 | 4,050 ms |
| 10 | 2,249 ms |
| 20 | 1,617 ms |

Each run observed 18 waiting threads at the peak measurement point.

The result shows that a pool smaller than the concurrent slow workload creates queueing and increases request latency. Increasing the pool reduces this particular workload's wait time, but it is not an unlimited solution: larger pools consume more database connections and can move contention into PostgreSQL itself.

## 16. Problems Found and Fixed

### 16.1 Host PostgreSQL tools were unavailable

The host did not provide the required PostgreSQL client tools. All SQL and readiness commands were moved into the running Docker container through `docker exec`.

### 16.2 Database password mismatch

The container environment showed `POSTGRES_PASSWORD=forge_dev_password`, while the first local configuration used a different password. The local profile was corrected to match the container.

### 16.3 Hibernate id converter rejection

Hibernate 7 rejected converters on `@Id` fields. Inventory and reservation ids were changed to persist raw UUIDs while retaining domain id wrappers at the API boundary.

### 16.4 Quantity converter startup error

A missing JPA `Convert` import prevented the quantity mapping from compiling or starting correctly. The import was restored and the local profile was rechecked.

### 16.5 Missing schema support

The application initially had no database migration support. Flyway dependencies and `V1__create_inventory_table.sql` were added. Flyway then created the tables and Hibernate validation succeeded.

### 16.6 Concurrency harness deadlock

The first 100-buyer harness used a readiness latch that required all 100 tasks to arrive before proceeding, but the executor had only 20 threads. The first 20 tasks occupied every worker while waiting, so the remaining tasks could never reach the latch.

The latch was removed and replaced with a start signal that lets submitted workers begin without requiring all tasks to be simultaneously resident in the executor.

### 16.7 In-memory concurrency regression

Removing global synchronization from the service broke an existing in-memory concurrency test. Synchronization was restored only for the in-memory repository branch. The PostgreSQL branch remains transaction- and database-controlled.

### 16.8 Hikari diagnostic warning

A method-reference helper created an IDE nullability warning while reading pool metrics. The code was changed to an explicit loop, which preserved the measurement behavior and removed the warning.

## 17. Validation Performed

Default validation:

```fish
./mvnw test
```

Result:

```text
30 tests passed, 0 failures, 0 errors, 3 gated live experiments skipped
```

Focused live validations:

```fish
./mvnw -q -Dtest=PostgresInventoryConcurrencyExperimentTest -Dforge.postgres.tests=true test
./mvnw -q -Dtest=PostgresHikariSaturationExperimentTest -Dforge.postgres.tests=true test
./mvnw -q -Dtest=HibernateOptimisticLockingExperimentTest -Dforge.postgres.tests=true test
```

Additional checks:

```fish
docker exec forge-postgres pg_isready -U forge -d forge
git diff --check
```

The PostgreSQL readiness check reported:

```text
/var/run/postgresql:5432 - accepting connections
```

Static diagnostics were clean for the edited inventory service and live experiment tests.

## 18. Files Added or Changed

### Application and configuration

- `pom.xml`
- `src/main/resources/application-local.properties`
- `src/main/resources/application-test.properties`
- `src/main/resources/db/migration/V1__create_inventory_table.sql`
- `src/main/java/com/forge/inventory/domain/Inventory.java`
- `src/main/java/com/forge/inventory/domain/InventoryReservation.java`
- `src/main/java/com/forge/inventory/domain/InventoryIdConverter.java`
- `src/main/java/com/forge/inventory/domain/InventoryReservationIdConverter.java`
- `src/main/java/com/forge/inventory/domain/QuantityConverter.java`
- `src/main/java/com/forge/inventory/application/InventoryReservationService.java`
- `src/main/java/com/forge/inventory/infrastructure/persistence/JpaInventoryRepository.java`
- `src/main/java/com/forge/inventory/infrastructure/persistence/JpaInventorySpringDataRepository.java`
- `src/main/java/com/forge/inventory/infrastructure/persistence/JpaInventoryReservationRepository.java`
- `src/main/java/com/forge/inventory/infrastructure/persistence/JpaInventoryReservationSpringDataRepository.java`

### Tests

- `src/test/java/com/forge/inventory/application/PostgresInventoryConcurrencyExperimentTest.java`
- `src/test/java/com/forge/inventory/application/PostgresHikariSaturationExperimentTest.java`
- `src/test/java/com/forge/inventory/application/HibernateOptimisticLockingExperimentTest.java`

### Database experiments

- `database/experiments/README.md`
- `database/experiments/indexes/01_setup.sql`
- `database/experiments/indexes/02_query_plans.sql`
- `database/experiments/indexes/03_write_cost.sql`
- `database/experiments/indexes/04_column_order_and_regression.sql`
- `database/experiments/transactions/*`
- `database/experiments/locking/*`
- `database/experiments/deadlocks/*`
- `database/experiments/connection-pool/*`

### Documentation

- `Docs/database-experiments.md` contains the observation-first experiment journal.
- This document explains the complete implementation sequence and the engineering decisions behind it.

## 19. What This Phase Demonstrates

The implementation demonstrates the following conclusions using measured behavior rather than definitions alone:

1. PostgreSQL chooses between sequential and index-based plans based on selectivity, result size, statistics, and physical visibility.
2. Composite index column order must reflect the leading predicates and ordering requirements of the query.
3. Covering indexes can eliminate heap visits, but visibility-map state matters.
4. Indexes improve selective reads while increasing write and storage costs.
5. `READ COMMITTED`, `REPEATABLE READ`, and `SERIALIZABLE` expose different snapshot and conflict behavior.
6. `SELECT FOR UPDATE` prevents conflicting row changes by making later transactions wait.
7. Inconsistent lock ordering can create deadlocks even when each individual transaction is valid.
8. Hibernate optimistic locking detects stale writes through a version predicate.
9. A naive read/check/write sequence loses updates under concurrency.
10. Pessimistic, optimistic, and atomic database strategies preserve the inventory invariant, with different contention and complexity trade-offs.
11. HikariCP limits concurrent database work and turns excess request concurrency into connection wait time.
12. Increasing a pool can reduce application queueing for a slow-query workload, but database connection capacity remains a finite resource.

## 20. Scope and Remaining Work

This phase establishes and measures the PostgreSQL foundation for the inventory slice. It does not yet convert every Forge aggregate to JPA or add a production-ready repository API for every concurrency strategy.

The following items remain intentionally outside the completed foundation or require further hardening:

- A larger dataset covering all Forge domain tables such as products, users, orders, jobs, and notifications.
- A production application port dedicated to atomic inventory decrement rather than keeping the atomic statement inside the experiment harness.
- A separate transaction-only comparison strategy in the live inventory test.
- Dedicated Spring integration tests for every isolation, locking, and deadlock script rather than the current runnable SQL scripts and focused live experiments.
- A production retry policy for serialization failures, including backoff and an explicit retry limit.
- A final paired no-index write-cost baseline and a broader predicate/order matrix.
- Final cleanup of stale placeholder wording in the older observation journal where the live summary already contains the result.

These are follow-up hardening tasks, not evidence that the completed experiments failed. The measured results and runnable scripts provide the baseline for implementing them safely in a later iteration.

## 21. Phase Outcome

Phase 3 successfully connected PostgreSQL internals to Forge application behavior. The project now has:

- A Docker-backed PostgreSQL development path
- Versioned schema migrations
- A Hibernate/JPA inventory mapping
- Profile-specific in-memory and PostgreSQL adapters
- Database constraints and indexes
- Reproducible query-plan and transaction scripts
- Live concurrency experiments
- Hibernate optimistic-locking verification
- HikariCP saturation measurements
- Recorded machine-specific observations and trade-offs

The most important implementation decision is that correctness-sensitive inventory changes are enforced at the database boundary. The live comparison showed why: application-level read/check/write logic is insufficient under concurrency, while row locks, version checks, or a conditional atomic update can preserve the inventory invariant when used with the appropriate transaction and retry behavior.
