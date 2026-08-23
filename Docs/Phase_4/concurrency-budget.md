# Concurrency Budget

Initial local budget:

| Layer | Budget | Reason |
|---|---:|---|
| Job worker core/max | 4/8 | Bound in-process work |
| Job queue | 1000 | Bound memory and expose overload |
| PostgreSQL connections | 5 local profile | Downstream bottleneck |
| Provider calls | Semaphore per provider | Protect external capacity |
| HTTP requests | Environment-dependent | Must not exceed downstream capacity indefinitely |

The worker pool must not be selected independently of the database pool. More workers than connections can increase waiting and contention without increasing throughput.

An in-process queue belongs to one JVM instance. Instance A's queue is not visible to Instance B; distributed ownership belongs to Phase 5.
