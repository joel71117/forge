# PostgreSQL Experiments

This journal is intentionally observation-first. Replace each `Not yet run` entry only with output observed from the local PostgreSQL environment.

## Environment

| Setting | Observed value |
| --- | --- |
| PostgreSQL | PostgreSQL 17.11 (Debian 17.11-1.pgdg13+2) |
| Java | OpenJDK 21.0.12 |
| Spring Boot | 4.1.0 |
| Hibernate | 7.4.1.Final |
| JDBC driver | PostgreSQL JDBC Driver |
| HikariCP | Spring Boot-managed HikariDataSource, configured maximum pool 5 and minimum idle 2 |
| CPU / RAM / OS | 12th Gen Intel Core i5-12450H; 15.40 GiB RAM; CachyOS x86_64 |
| `max_connections`, `shared_buffers`, `work_mem` | 100, 128MB, 4MB |

## Dataset

`database/experiments/indexes/01_setup.sql` creates one million intentionally skewed experimental order rows. Ninety percent are `COMPLETED` and attached to a small group of users; the remainder has unique user ids. Record the machine-specific load duration and table size here.

**Observed result:** The script inserted 1,000,000 rows. The distribution was 900,000 `COMPLETED` rows and 100,000 `PROCESSING` rows. The rare-user query target returned one row.

## Experiment 1: Sequential Scan and B-tree Index

**Hypothesis:** A rare `user_id` predicate should become cheaper after a B-tree index is added, while PostgreSQL may retain a sequential scan for the highly common `COMPLETED` status.

**Setup and execution:** Run `indexes/01_setup.sql`, then `indexes/02_query_plans.sql`.

**Observed result:** Before indexing, `user_id = 999950` used a parallel sequential scan and returned one row in 25.033 ms. After the B-tree index, it used a bitmap heap/index scan and returned one row in 0.032 ms. The common `status = 'COMPLETED'` predicate remained a sequential scan, returned 900,000 rows, and took 77.080 ms. The index was not useful enough to avoid reading most of the table.

**Engineering implication:** Selectivity and result size influence the planner more than the mere existence of an index.

## Experiment 2: Composite and Covering Indexes

**Hypothesis:** `(user_id, status, created_at DESC)` supports the combined predicate and order, while an index with `created_at` included can allow an index-only scan when visibility permits.

**Setup and execution:** Run the final three plan blocks in `indexes/02_query_plans.sql`. Repeat after changing the composite index column order.

**Observed result:** The composite query used an index-only scan with one heap fetch and took 0.145 ms. After `VACUUM ANALYZE`, the covering index used an index-only scan with zero heap fetches and took 0.034 ms.

**Engineering implication:** Included columns can remove heap visits for narrow read models, but visibility-map state determines whether the scan is truly heap-free.

Observed regression result: `(status, user_id, created_at)` still produced an index-only scan for the combined predicate at 0.043 ms. The `PROCESSING`-only query used the status-leading index and returned 100,000 rows in 14.964 ms. After changing all 100,000 `PROCESSING` rows to `COMPLETED` and running `ANALYZE`, the `COMPLETED` query used a sequential scan, returned 1,000,000 rows, and took 80.312 ms.

## Experiment 3: Index Write Cost

**Hypothesis:** Additional indexes increase write work and relation size.

**Setup and execution:** Run `indexes/03_write_cost.sql`. Compare the timed insert/delete statements with the same workload on a table without the three secondary indexes.

**Observed result:** Not yet run. The script reports timing and combined table/index size for the indexed case.

**Engineering implication:** Not yet determined from this environment.

## Experiment 4: READ COMMITTED

**Hypothesis:** A transaction at PostgreSQL's default isolation level sees a later committed update on its second statement.

**Setup and execution:** Insert an inventory row, start session A with `transactions/read-committed-session-a.sql`, run session B between A's two reads, and record the timeline.

**Observed result:** Not yet run. Record both values from session A and the commit ordering.

**Engineering implication:** Not yet determined from this environment.

## Experiment 5: REPEATABLE READ and SERIALIZABLE

**Hypothesis:** `REPEATABLE READ` retains its initial snapshot; a conflicting `SERIALIZABLE` workload can abort and requires retry handling.

**Setup and execution:** Repeat Experiment 3 with `BEGIN ISOLATION LEVEL REPEATABLE READ` and then construct two conflicting `SERIALIZABLE` writes.

**Observed result:** Not yet run. The repeatable-read scripts are under `transactions/`; the serializable pair should record the actual SQLSTATE and retry decision.

**Engineering implication:** Not yet determined from this environment.

## Experiment 6: SELECT FOR UPDATE and Deadlock

**Hypothesis:** The second transaction waits for the row lock until the first transaction finishes; opposing lock order produces a PostgreSQL deadlock error.

**Setup and execution:** Use the two locking scripts for one inventory row. Then use `deadlocks/session-a.sql` and `deadlocks/session-b.sql` against two inventory rows.

**Observed result:** Not yet run. Capture the deadlock error text and rerun with the same lock order in both sessions.

**Engineering implication:** Not yet determined from this environment.

## Experiment 7: Hibernate Optimistic Locking

**Hypothesis:** The `inventory.version` column makes a stale update fail rather than silently overwrite a concurrent change.

**Setup and execution:** Load one inventory entity in two transactions without `PESSIMISTIC_WRITE`, commit the first update, then commit the stale second update. Capture Hibernate SQL and the exception.

**Observed result:** Not yet run.

**Engineering implication:** Not yet determined from this environment.

## Experiment 8: HikariCP Saturation

**Hypothesis:** More concurrent slow requests than the five configured pool connections causes connection acquisition waits and eventually timeouts, rather than increased database throughput.

**Setup and execution:** With the local profile, invoke `connection-pool/pg-sleep.sql` through more than five concurrent JDBC requests. Compare pool sizes 2, 5, 10, and 20 while recording throughput, p95 latency, errors, active connections, and waiters.

**Observed result:** The live Spring test uses 20 concurrent `pg_sleep(1)` tasks against the configured five-connection Hikari pool. Run `PostgresHikariSaturationExperimentTest` with `-Dforge.postgres.tests=true` and record its `maxWaiting`, p95 latency, active/idle connections, and durations. Repeat with `-Dspring.datasource.hikari.maximum-pool-size=2`, `5`, `10`, and `20`.

**Engineering implication:** Not yet determined from this environment.

## Live Observations Summary

| Experiment | Observation |
| --- | --- |
| `READ COMMITTED` | Transaction A read 10, session B committed an increment, then A read 11. |
| `REPEATABLE READ` | Transaction A read 10 before and after session B committed an increment; its snapshot remained at 10. |
| `SERIALIZABLE` | One conflicting transaction committed and the other failed with `could not serialize access due to concurrent update`. |
| `SELECT FOR UPDATE` | The second session returned only after the first session committed its two-second lock hold. |
| Deadlock | Opposite row-lock order produced `ERROR: deadlock detected`; PostgreSQL identified the two waiting processes. Same order completed without deadlock. |
| Hibernate optimistic locking | Two detached `Inventory` entities loaded at version 0; the first committed as version 1 and the stale second update raised `OptimisticLockException`. |
| Inventory strategies | Naive: 93 successes, final quantity 3, 221 ms. Pessimistic: 10, 0, 255 ms. Optimistic: 10, 0, 136 ms. Atomic SQL: 10, 0, 36 ms. |
| Index write cost | Indexed second insert of 100,000 rows: 480.757 ms. Delete of 50,000 rows: 28.723 ms. Relation plus indexes: 24 MB. |
| Hikari pool sizes | 2: p95 10,017 ms; 5: p95 4,050 ms; 10: p95 2,249 ms; 20: p95 1,617 ms. Each run observed 18 waiting threads. |

## Final Findings

Not yet run. Summarize only measured query plans, transaction timelines, contention behavior, retries, and the chosen strategy trade-offs.