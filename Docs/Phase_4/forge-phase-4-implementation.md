# Forge --- Phase 4 Implementation

## Java Concurrency, Parallelism, Thread Safety & JVM Execution

**Objective:** Develop deep practical understanding of Java concurrency
and connect it to the database concurrency problems solved in Phase 3.
Forge must become capable of safely performing concurrent in-process
work before Redis, Kafka, distributed workers, and other distributed
coordination mechanisms are introduced.

------------------------------------------------------------------------

# 1. Purpose

Phase 3 focused on database concurrency:

``` text
Transactions
Locks
MVCC
Optimistic locking
Pessimistic locking
Deadlocks
Atomic SQL
```

Phase 4 moves inside one JVM:

``` text
Java process
 ↓
Threads
 ↓
Shared memory
 ↓
Java Memory Model
 ↓
Synchronization
 ↓
Executors
 ↓
Concurrent collections
 ↓
Async computation
 ↓
Virtual threads
```

The central question is:

> Can I design, test, debug and measure correct concurrent code inside
> one JVM?

------------------------------------------------------------------------

# 2. Learning Objectives

Master through implementation and experiments:

-   Thread lifecycle and interruption
-   Runnable, Callable and Future
-   Race conditions
-   `synchronized`
-   intrinsic monitors
-   `volatile`
-   Java Memory Model
-   happens-before
-   atomicity vs visibility vs ordering
-   CAS and atomic variables
-   `Lock` and `ReentrantLock`
-   `ReadWriteLock`
-   `StampedLock`
-   `ConcurrentHashMap`
-   `BlockingQueue`
-   `Semaphore`
-   `CountDownLatch`
-   `CyclicBarrier`
-   `Phaser`
-   `ExecutorService`
-   `ThreadPoolExecutor`
-   rejection policies
-   thread-pool sizing
-   `CompletableFuture`
-   cancellation and timeouts
-   backpressure
-   deadlock, starvation and livelock
-   virtual threads
-   concurrency vs parallelism
-   JVM thread inspection
-   graceful shutdown
-   worker isolation
-   database-pool interaction

# 3. Explicitly Out of Scope

Do not introduce yet:

-   Redis distributed locks
-   Kafka consumers
-   distributed job workers
-   Kubernetes
-   service discovery
-   load balancing
-   distributed tracing infrastructure
-   microservices
-   distributed transactions
-   cross-process coordination

Phase 4 answers:

> Can I write correct concurrent code inside one JVM?

------------------------------------------------------------------------

# 4. Architecture Position

Current architecture:

``` text
HTTP
 ↓
Spring Application
 ↓
Application Services
 ↓
Domain
 ↓
PostgreSQL
```

Phase 4 adds controlled in-process concurrency:

``` text
HTTP
 ↓
Application Service
 ↓
Concurrency Boundary
 ↓
Executor / Worker
 ↓
Domain Operation
 ↓
PostgreSQL
```

Concurrency mechanisms should remain implementation details unless
asynchronous behavior is part of the application contract.

------------------------------------------------------------------------

# 5. Implementation Order

Implement in this order:

``` text
1. Thread fundamentals
2. Java Memory Model
3. Race-condition laboratory
4. synchronized
5. volatile
6. atomic variables
7. locks
8. concurrent collections
9. coordination primitives
10. executors
11. ThreadPoolExecutor
12. BlockingQueue
13. CompletableFuture
14. exceptions/cancellation/timeouts
15. backpressure
16. virtual threads
17. Forge job executor
18. notification concurrency
19. performance experiments
20. failure experiments
21. production review
```

Do not jump directly to `CompletableFuture`.

------------------------------------------------------------------------

# 6. Concurrency Laboratory

Create:

``` text
com.forge.concurrency.lab
```

This module/package is for controlled experiments.

Use ordinary Java where possible instead of Spring.

Create experiments for:

``` text
Thread
Runnable
Callable
Future
```

Learn thread states:

``` text
NEW
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
TERMINATED
```

Use `Thread.currentThread()` and JDK tools to inspect execution.

------------------------------------------------------------------------

# 7. Thread Lifecycle Experiments

Implement an experiment that:

1.  Creates a thread.
2.  Starts it.
3.  Makes it sleep.
4.  Blocks it on another operation.
5.  Interrupts it.
6.  Allows it to terminate.

Inspect the resulting thread states with:

``` text
jstack
jcmd
```

Document the lifecycle.

Also explicitly demonstrate:

``` java
thread.start();
```

versus:

``` java
thread.run();
```

Understand that `start()` creates concurrent execution while `run()` is
an ordinary method call.

------------------------------------------------------------------------

# 8. Interruption

Learn:

``` java
Thread.interrupt()
Thread.isInterrupted()
Thread.interrupted()
```

Implement cooperative cancellation.

A worker should:

``` text
detect interruption
 ↓
clean up
 ↓
restore interruption when appropriate
 ↓
terminate
```

Do not blindly swallow `InterruptedException`.

Document the difference between:

``` text
interrupting a thread
cancelling a Future
stopping underlying work
```

------------------------------------------------------------------------

# 9. Race-Condition Laboratory

Create:

``` text
Counter
```

with:

``` java
int value;
```

Run:

``` text
100 threads
10,000 increments each
```

Expected:

``` text
1,000,000
```

Observe incorrect results.

Explain why:

``` java
value++;
```

is conceptually:

``` text
read
add
write
```

and therefore not atomic.

------------------------------------------------------------------------

# 10. synchronized

Fix the counter using:

``` java
synchronized
```

Compare:

``` text
synchronized method
synchronized block
```

Measure:

``` text
correctness
throughput
contention
```

Understand which object acts as the monitor.

Investigate:

``` text
synchronized(this)
synchronized(lockObject)
synchronized(SomeClass.class)
```

and why lock choice matters.

------------------------------------------------------------------------

# 11. Lock Scope

Compare:

``` text
large synchronized critical section
small synchronized critical section
```

Measure contention.

The principle should become:

> Protect the smallest amount of shared state necessary while preserving
> the invariant.

Never hold a lock unnecessarily across slow I/O.

------------------------------------------------------------------------

# 12. volatile

Create a stop-flag experiment.

Compare:

``` java
boolean running;
```

with:

``` java
volatile boolean running;
```

Understand memory visibility.

Then demonstrate that:

``` java
volatile int counter;
counter++;
```

is still not a safe concurrent increment.

The distinction must be clear:

``` text
volatile → visibility/order
atomic operation → indivisible update
```

------------------------------------------------------------------------

# 13. Java Memory Model

Study:

``` text
visibility
reordering
happens-before
safe publication
```

Important happens-before relationships include:

``` text
unlock → subsequent lock
volatile write → subsequent volatile read
Thread.start() → actions in started thread
thread actions → successful join()
```

Create experiments comparing:

``` text
unsynchronized publication
volatile publication
synchronized publication
```

Document separately:

``` text
visibility
atomicity
ordering
```

------------------------------------------------------------------------

# 14. Atomic Variables and CAS

Implement:

``` text
AtomicInteger
AtomicLong
AtomicReference
LongAdder
```

Understand CAS:

``` text
read current value
 ↓
compare with expected value
 ↓
update only if unchanged
 ↓
retry if another thread changed it
```

Compare atomic counters with synchronized counters under increasing
contention.

Understand:

> Lock-free does not mean contention-free.

------------------------------------------------------------------------

# 15. Atomic State Machine

Use:

``` text
AtomicReference<JobState>
```

with:

``` text
QUEUED
RUNNING
COMPLETED
FAILED
```

Run multiple transition attempts concurrently.

Verify:

-   only valid transitions succeed
-   one transition cannot silently overwrite another
-   state cannot become inconsistent

Compare:

``` text
synchronized state transition
AtomicReference CAS
database optimistic locking
```

This connects Phase 1 domain state machines, Phase 3 database
concurrency and Phase 4 JVM concurrency.

------------------------------------------------------------------------

# 16. Locks

Learn:

``` text
Lock
ReentrantLock
```

Compare with `synchronized`.

Experiment with:

``` text
lock()
unlock()
tryLock()
lockInterruptibly()
```

Always release locks using:

``` java
try {
    lock.lock();
    ...
} finally {
    lock.unlock();
}
```

------------------------------------------------------------------------

# 17. tryLock and Fairness

Create a contention experiment using:

``` text
tryLock(timeout)
```

Compare:

``` text
success
timeout
retry
failure
```

Then compare:

``` java
new ReentrantLock()
```

with:

``` java
new ReentrantLock(true)
```

Understand fairness tradeoffs instead of assuming fair locks are always
better.

------------------------------------------------------------------------

# 18. ReadWriteLock and StampedLock

Create a workload with:

``` text
many readers
few writers
```

Compare:

``` text
ReentrantLock
ReadWriteLock
```

Measure read throughput and writer latency.

Then study:

``` text
StampedLock
optimisticRead
```

Do not put StampedLock into Forge production code unless measurements
justify it.

------------------------------------------------------------------------

# 19. ConcurrentHashMap

Study why this is unsafe as a compound operation:

``` java
if (!map.containsKey(id)) {
    map.put(id, value);
}
```

Replace it where appropriate with:

``` text
putIfAbsent
compute
computeIfAbsent
merge
```

Build a concurrent repository experiment and verify correctness under
load.

------------------------------------------------------------------------

# 20. Concurrent Collections

Experiment with:

``` text
ConcurrentHashMap
BlockingQueue
CopyOnWriteArrayList
```

For `CopyOnWriteArrayList`, compare:

``` text
many readers + rare writes
```

against a synchronized collection.

Understand why copy-on-write becomes expensive when writes are frequent.

------------------------------------------------------------------------

# 21. BlockingQueue

Build:

``` text
Producer
   ↓
Bounded BlockingQueue
   ↓
Consumers
```

Use:

``` text
ArrayBlockingQueue
```

and investigate:

``` text
LinkedBlockingQueue
```

Make the queue bounded.

A bounded queue is essential for learning backpressure.

------------------------------------------------------------------------

# 22. Backpressure Experiment

Make:

``` text
producer rate > consumer rate
```

Observe:

``` text
queue depth
memory
producer blocking
latency
throughput
```

Then compare policies:

``` text
block
reject
drop
CallerRuns
throttle producer
```

The system must have an explicit overload behavior.

------------------------------------------------------------------------

# 23. ExecutorService

Stop creating raw threads for ordinary application work.

Learn:

``` text
Executor
ExecutorService
ScheduledExecutorService
```

Understand:

``` text
execute
submit
Future
shutdown
shutdownNow
awaitTermination
```

Understand executor lifecycle and ownership.

------------------------------------------------------------------------

# 24. Thread Pool Types

Investigate:

``` text
FixedThreadPool
CachedThreadPool
SingleThreadExecutor
ScheduledThreadPool
WorkStealingPool
```

Understand the workload each is intended for.

Do not choose a pool merely because it is commonly used in tutorials.

------------------------------------------------------------------------

# 25. ThreadPoolExecutor

Understand:

``` text
corePoolSize
maximumPoolSize
keepAliveTime
workQueue
ThreadFactory
RejectedExecutionHandler
```

This is one of the most important Phase 4 topics.

------------------------------------------------------------------------

# 26. Rejection Policies

Experiment with:

``` text
AbortPolicy
CallerRunsPolicy
DiscardPolicy
DiscardOldestPolicy
```

Create:

``` text
workers busy
queue full
new task arrives
```

Observe behavior.

Pay particular attention to `CallerRunsPolicy` as a simple form of
producer backpressure.

------------------------------------------------------------------------

# 27. Thread Pool Sizing

Create:

### CPU-bound workload

``` text
expensive computation
```

### I/O-bound workload

``` text
simulated database/network wait
```

Compare pool sizes:

``` text
1
2
4
8
16
32
64
```

Measure:

``` text
throughput
p50
p95
p99
CPU
memory
```

Develop the intuition that CPU-bound and I/O-bound workloads require
different concurrency strategies.

------------------------------------------------------------------------

# 28. ThreadFactory

Create a custom `ThreadFactory`.

Use meaningful names:

``` text
forge-job-worker-1
forge-job-worker-2
forge-notification-email-1
```

Thread names must make thread dumps useful.

------------------------------------------------------------------------

# 29. Executor Shutdown

Implement:

``` text
stop accepting work
 ↓
finish running tasks
 ↓
drain queue where practical
 ↓
shutdown
 ↓
await termination
```

Define a timeout.

Decide what happens to unfinished work.

Do not silently lose queued jobs.

------------------------------------------------------------------------

# 30. ScheduledExecutorService

Create a reservation-expiration experiment using:

``` text
schedule()
scheduleAtFixedRate()
scheduleWithFixedDelay()
```

Understand:

``` text
fixed rate
vs
fixed delay
```

Do this with Java's scheduler before relying on Spring scheduling.

------------------------------------------------------------------------

# 31. CompletableFuture

Now introduce:

``` text
CompletableFuture
```

Learn:

``` text
supplyAsync
runAsync
thenApply
thenCompose
thenCombine
allOf
anyOf
exceptionally
handle
whenComplete
orTimeout
completeOnTimeout
```

Do not use CompletableFuture merely to make synchronous code look
complicated.

Use it where independent work can genuinely overlap.

------------------------------------------------------------------------

# 32. CompletableFuture Composition

Create a simulated order read:

``` text
load customer
load product information
load inventory information
```

Run independent operations concurrently.

Then combine the results.

Understand:

``` text
independent work → parallel composition
dependent work → sequential composition
```

------------------------------------------------------------------------

# 33. thenApply vs thenCompose

Explicitly demonstrate:

``` text
thenApply
```

versus:

``` text
thenCompose
```

Understand:

``` text
T → U
```

versus:

``` text
T → CompletableFuture<U>
```

Do not move on until this distinction is intuitive.

------------------------------------------------------------------------

# 34. CompletableFuture Exceptions

Create failures at different stages.

Compare:

``` text
exceptionally
handle
whenComplete
```

Understand:

``` text
recover
transform
observe
```

Ensure exceptions do not disappear silently.

------------------------------------------------------------------------

# 35. Timeouts

Experiment with:

``` text
orTimeout
completeOnTimeout
```

Compare:

``` text
fail on timeout
fallback on timeout
```

Understand that a timeout does not necessarily mean underlying work has
stopped.

------------------------------------------------------------------------

# 36. Cancellation

Test:

``` text
Future.cancel()
CompletableFuture cancellation
Thread.interrupt()
```

Understand:

``` text
cancel signal
vs
actual task termination
```

Cancellation is cooperative and must be designed into the task.

------------------------------------------------------------------------

# 37. CompletableFuture Executor Choice

Do not use:

``` text
ForkJoinPool.commonPool()
```

for every asynchronous task.

Create explicit executors for appropriate workloads.

Understand why:

``` text
CPU executor
I/O executor
```

should not necessarily be the same pool.

Blocking the wrong executor can cause starvation.

------------------------------------------------------------------------

# 38. Coordination Primitives

Implement experiments using:

``` text
Semaphore
CountDownLatch
CyclicBarrier
Phaser
```

Understand their different purposes.

### CountDownLatch

One-time coordination.

### CyclicBarrier

Reusable barrier where parties wait for each other.

### Phaser

Dynamic/multi-phase coordination.

### Semaphore

Limit concurrent access to a resource.

------------------------------------------------------------------------

# 39. Semaphore Provider Limit

Simulate an external provider:

``` text
maximum 10 concurrent requests
```

Use:

``` text
Semaphore(10)
```

and test 100 concurrent requests.

Measure:

``` text
throughput
latency
provider concurrency
```

This prepares Forge for notification/payment provider limits.

------------------------------------------------------------------------

# 40. Deadlock Laboratory

Create:

``` text
lockA
lockB
```

and intentionally reproduce:

``` text
T1: A → B
T2: B → A
```

Capture with:

``` text
jstack
jcmd
```

Identify:

``` text
thread
held lock
waiting lock
```

Fix using:

``` text
consistent lock ordering
tryLock
higher-level coordination
```

------------------------------------------------------------------------

# 41. Starvation and Livelock

Create a starvation experiment caused by:

``` text
heavy contention
long critical section
unfair resource acquisition
```

Create a livelock experiment where two workers continually react to one
another but make no progress.

Then add:

``` text
randomized backoff
```

and compare.

This prepares you for distributed retry algorithms later.

------------------------------------------------------------------------

# 42. JVM Thread Inspection

Use:

``` text
jps
jcmd
jstack
```

Learn to identify:

``` text
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
deadlocks
thread names
stack traces
```

Create a test process with:

``` text
10 blocked threads
5 waiting threads
2 running workers
1 deadlocked pair
```

Capture and analyze the thread dump.

------------------------------------------------------------------------

# 43. Virtual Threads

Only after platform-thread fundamentals are understood, study:

``` text
virtual threads
carrier threads
blocking I/O
thread-per-request
```

Do not think:

> Virtual threads are faster threads.

The important benefit is allowing very high concurrency for suitable
blocking workloads without requiring one expensive platform thread per
task.

------------------------------------------------------------------------

# 44. Virtual Thread Benchmark

Create:

``` text
10,000 tasks
each blocks for 100 ms
```

Compare:

``` text
platform thread pool
virtual threads
```

Measure:

``` text
completion time
memory
CPU
thread count
```

Then repeat with CPU-heavy work.

Confirm that virtual threads do not magically accelerate CPU
computation.

------------------------------------------------------------------------

# 45. Concurrency vs Parallelism

You should be able to explain:

### Concurrency

Multiple tasks make progress during overlapping periods.

### Parallelism

Multiple tasks execute simultaneously.

Example:

``` text
1 CPU core + many tasks
→ concurrency

8 CPU cores + 8 CPU-heavy tasks
→ parallelism
```

This distinction should become fundamental.

------------------------------------------------------------------------

# 46. Forge In-Process Job Executor

Apply the concepts to Forge.

Build:

``` text
Job
 ↓
Bounded Queue
 ↓
ThreadPoolExecutor
 ↓
JobHandler
 ↓
Domain
 ↓
PostgreSQL
```

Do not introduce Kafka yet.

The purpose is to build the local version of the distributed job engine.

------------------------------------------------------------------------

# 47. Job Handler Registry

Create:

``` text
JobHandler<T>
```

and:

``` text
JobType → JobHandler
```

Handlers:

``` text
SEND_NOTIFICATION
EXPIRE_RESERVATION
RECONCILE_PAYMENT
GENERATE_REPORT
```

Use generics only where they improve type safety.

Avoid building a framework around four handlers.

------------------------------------------------------------------------

# 48. Job Worker Lifecycle

Enforce:

``` text
QUEUED
 ↓
RUNNING
 ↓
COMPLETED
```

Failure:

``` text
RUNNING
 ↓
FAILED
 ↓
RETRYING
 ↓
QUEUED
```

Terminal:

``` text
FAILED
 ↓
DEAD_LETTERED
```

Workers must invoke domain transitions rather than directly mutating job
status.

------------------------------------------------------------------------

# 49. In-Process Job Ownership

For Phase 4:

``` text
submit job
 ↓
one queue
 ↓
one worker consumes it
```

The queue provides in-process ownership.

Document the limitation:

``` text
Instance A queue != Instance B queue
```

This limitation becomes the motivation for Phase 5 distributed job
processing.

------------------------------------------------------------------------

# 50. Job Retry

Implement:

``` text
retryCount
maxRetries
nextAttemptAt
```

Use exponential backoff with jitter.

Conceptually:

``` text
delay = min(maxDelay, baseDelay × 2^attempt)
```

then add bounded randomness.

Never allow:

``` text
unbounded retries
instant retry loops
```

------------------------------------------------------------------------

# 51. Job Timeout

Introduce job execution timeouts.

Understand the distinction between:

``` text
Future timeout
thread interruption
task termination
```

Do not assume:

``` java
future.cancel(true)
```

guarantees arbitrary blocking work has stopped.

------------------------------------------------------------------------

# 52. Job Backpressure

Make the queue bounded.

When full, define an explicit API behavior.

Possible choices:

``` text
reject
wait
run caller
drop
```

A reasonable initial API behavior is to reject overload rather than
silently consuming unlimited memory.

Document the choice.

------------------------------------------------------------------------

# 53. Job Metrics

Add metrics:

``` text
jobs.submitted
jobs.completed
jobs.failed
jobs.retried
jobs.dead_lettered
jobs.queue.depth
jobs.execution.duration
jobs.wait.duration
jobs.active_workers
jobs.rejected
```

Use bounded metric dimensions such as:

``` text
job_type
priority
status
```

Do not use high-cardinality values such as:

``` text
job_id
request_id
user_id
```

as metric tags.

------------------------------------------------------------------------

# 54. Graceful Job Shutdown

Implement:

``` text
stop accepting new jobs
 ↓
finish active jobs
 ↓
drain queue where practical
 ↓
shutdown executor
```

Set a shutdown timeout.

Define what happens to jobs that remain incomplete.

This becomes critical when the job engine becomes distributed.

------------------------------------------------------------------------

# 55. Notification Concurrency

Use:

``` text
Notification
 ↓
Job
 ↓
Worker
 ↓
NotificationHandler
```

Simulate:

``` text
EmailProvider
SmsProvider
PushProvider
```

Assign provider concurrency limits:

``` text
Email = 20
SMS = 5
Push = 50
```

Use `Semaphore` or separate executor/queue strategies.

------------------------------------------------------------------------

# 56. Provider Isolation / Bulkheads

Make one provider intentionally slow.

Compare:

``` text
shared executor
```

against:

``` text
email executor
sms executor
push executor
```

Observe whether one provider can starve the others.

Document the tradeoff:

``` text
isolation
complexity
resource utilization
```

This is the first practical implementation of the bulkhead pattern.

------------------------------------------------------------------------

# 57. Database Pool vs Worker Pool

Workers eventually use PostgreSQL.

Experiment with:

``` text
worker count > DB connection pool
worker count < DB connection pool
worker count ≈ DB connection pool
```

Observe:

``` text
throughput
waiting
latency
connection utilization
database contention
```

This should teach:

> Concurrency at one layer can overwhelm the next layer.

------------------------------------------------------------------------

# 58. Concurrency Budget

Create:

``` text
docs/concurrency-budget.md
```

Document:

``` text
HTTP concurrency
job workers
notification workers
DB connections
provider concurrency
queue capacity
```

Model:

``` text
HTTP
 ↓
Application
 ↓
Worker pool
 ↓
DB pool
 ↓
PostgreSQL
```

Identify the bottleneck at each stage.

------------------------------------------------------------------------

# 59. ThreadLocal

Study:

``` text
ThreadLocal
```

Create a correlation-context experiment.

Then investigate why ThreadLocal becomes more complicated with:

``` text
thread pools
async execution
virtual threads
```

Do not use ThreadLocal as a general-purpose state store.

------------------------------------------------------------------------

# 60. Context Propagation

Verify whether:

``` text
HTTP request
 ↓
CompletableFuture
 ↓
Executor
 ↓
worker
```

preserves correlation context.

If not, implement an explicit propagation strategy appropriate to the
project.

Do not assume thread-local context automatically crosses asynchronous
boundaries.

------------------------------------------------------------------------

# 61. Structured Concurrency Research

Study structured concurrency concepts available in the JDK version used
by Forge.

Understand:

``` text
parent task
 ├── child
 ├── child
 └── child
```

and lifecycle ownership.

If the selected JDK provides a production-ready API, evaluate it
experimentally.

Do not add a new concurrency abstraction just because it is modern.

------------------------------------------------------------------------

# 62. Parallel Streams

Compare:

``` text
stream()
parallelStream()
explicit ExecutorService
```

for CPU-bound work.

Measure:

``` text
throughput
CPU
contention
```

Understand why parallel streams can be inappropriate for server-side
workloads.

Do not use them in Forge merely because one benchmark wins.

------------------------------------------------------------------------

# 63. Concurrency Anti-Patterns

Create examples of:

``` text
shared mutable singleton state
check-then-act
unsafe publication
holding locks during I/O
inconsistent nested lock ordering
unbounded queues
unbounded thread creation
blocking the wrong executor
swallowing InterruptedException
blind parallelStream()
CompletableFuture everywhere
```

Document the failure mode and safer alternative for each.

------------------------------------------------------------------------

# 64. Testing Strategy

Use:

``` text
deterministic concurrency tests
stress tests
timing experiments
```

Avoid:

``` java
Thread.sleep(...)
```

for synchronization in tests.

Prefer:

``` text
CountDownLatch
CyclicBarrier
Phaser
Future.get(timeout)
```

Use barriers to force threads into controlled race windows.

------------------------------------------------------------------------

# 65. Deterministic Race Testing

Build tests where:

``` text
T1 reaches point A
T2 reaches point A
both proceed
```

rather than hoping the operating system scheduler produces the race.

This should become a reusable test utility.

------------------------------------------------------------------------

# 66. Concurrency Invariants

Define invariants before writing stress tests.

### Inventory

``` text
available >= 0
reserved >= 0
successful reservations <= stock
```

### Job

``` text
job cannot complete twice
```

### Notification

``` text
attempt numbers are unique
```

### Counter

``` text
final value == successful increments
```

Test properties rather than implementation details.

------------------------------------------------------------------------

# 67. Stress Testing

Create:

``` text
10,000 counter operations
1,000 job submissions
500 inventory operations
500 concurrent state transitions
```

Run repeatedly.

Track:

``` text
failures
invariant violations
timeouts
deadlocks
rejected tasks
```

Concurrency correctness must survive repetition.

------------------------------------------------------------------------

# 68. Failure Injection

Create a controlled failure-injection mechanism supporting:

``` text
fixed delay
random delay
failure
timeout
interruption
```

Use it for:

``` text
job retry
worker failure
queue saturation
notification failure
provider slowdown
```

Disable it during ordinary tests.

------------------------------------------------------------------------

# 69. Production-Style Debugging Exercise

Create a scenario where API latency suddenly increases.

Determine whether the cause is:

``` text
CPU saturation
thread pool exhaustion
DB pool exhaustion
database lock
slow query
queue saturation
worker starvation
external provider delay
```

Use:

``` text
logs
metrics
thread dumps
PostgreSQL activity
```

Do not inspect only application source code.

------------------------------------------------------------------------

# 70. Performance Benchmarking

Create:

``` text
docs/concurrency-benchmarks.md
```

For every experiment record:

``` text
environment
JDK version
CPU/memory
workload
thread count
pool size
queue size
result
p50
p95
p99
CPU
memory
errors
interpretation
limitations
```

For serious microbenchmarks, evaluate JMH rather than relying on naive
`System.nanoTime()` loops.

------------------------------------------------------------------------

# 71. Correctness Before Performance

For every optimization:

``` text
1. Establish correctness.
2. Add regression test.
3. Measure baseline.
4. Optimize.
5. Measure again.
6. Re-run correctness tests.
```

A faster incorrect implementation is a failed implementation.

------------------------------------------------------------------------

# 72. Definition of Done

Phase 4 is complete when:

-   [ ] Thread lifecycle is understood.
-   [ ] `start()` vs `run()` is understood.
-   [ ] Interruption is understood and correctly handled.
-   [ ] A race condition has been reproduced.
-   [ ] `synchronized` has been used to fix a race.
-   [ ] Intrinsic monitors are understood.
-   [ ] `volatile` visibility has been demonstrated.
-   [ ] `volatile` non-atomicity has been demonstrated.
-   [ ] Java Memory Model basics are understood.
-   [ ] happens-before is understood.
-   [ ] CAS is understood.
-   [ ] Atomic variables have been implemented.
-   [ ] `ReentrantLock` has been used.
-   [ ] `tryLock` has been tested.
-   [ ] Read/write locking has been evaluated.
-   [ ] `ConcurrentHashMap` compound operations are understood.
-   [ ] Blocking queues have been implemented.
-   [ ] Bounded queues have been used.
-   [ ] Backpressure has been demonstrated.
-   [ ] ExecutorService is understood.
-   [ ] ThreadPoolExecutor is understood.
-   [ ] Rejection policies have been compared.
-   [ ] Thread-pool sizing has been experimentally evaluated.
-   [ ] ThreadFactory is used.
-   [ ] Executor shutdown is graceful.
-   [ ] CompletableFuture composition is understood.
-   [ ] `thenApply` vs `thenCompose` is understood.
-   [ ] CompletableFuture failures are handled deliberately.
-   [ ] Timeouts are implemented.
-   [ ] Cancellation behavior is understood.
-   [ ] Deadlock has been reproduced.
-   [ ] Deadlock has been diagnosed with a thread dump.
-   [ ] Starvation and livelock have been demonstrated.
-   [ ] Virtual threads have been benchmarked.
-   [ ] CPU-bound vs I/O-bound behavior has been compared.
-   [ ] An in-process Forge job executor exists.
-   [ ] Job retries work.
-   [ ] Job backpressure works.
-   [ ] Graceful worker shutdown works.
-   [ ] Notification provider isolation has been tested.
-   [ ] Worker concurrency vs DB pool size has been measured.
-   [ ] Async correlation propagation is understood.
-   [ ] Concurrency stress tests exist.
-   [ ] Important invariants are tested.
-   [ ] JVM thread inspection has been practiced.
-   [ ] Performance measurements are documented.
-   [ ] Static analysis passes.
-   [ ] All tests pass.

# 73. Required Exit Questions

## Threads

1.  What happens when `Thread.start()` is called?
2.  Difference between `start()` and `run()`?
3.  What does interruption mean?
4.  Why should `InterruptedException` not simply be swallowed?
5.  What are the important Java thread states?

## Java Memory Model

6.  What is a race condition?
7.  Why is `i++` not atomic?
8.  What does `volatile` guarantee?
9.  What does `volatile` not guarantee?
10. What is happens-before?
11. What is memory visibility?
12. What is safe publication?
13. What is CAS?

## Locks

14. What does `synchronized` actually lock?
15. Difference between `synchronized` and `ReentrantLock`?
16. When is `tryLock()` useful?
17. What is lock contention?
18. What is deadlock?
19. What is starvation?
20. What is livelock?
21. How can lock ordering prevent deadlocks?

## Executors

22. Why use an executor instead of creating threads manually?
23. What determines thread-pool throughput?
24. Difference between CPU-bound and I/O-bound workloads?
25. What happens when a ThreadPoolExecutor queue fills?
26. What does CallerRunsPolicy do?
27. Why are unbounded queues dangerous?
28. Why can too many threads reduce performance?

## CompletableFuture

29. Difference between `thenApply` and `thenCompose`?
30. Difference between `thenCombine` and `allOf`?
31. How do exceptions propagate?
32. Difference between `exceptionally`, `handle`, and `whenComplete`?
33. What does a timeout actually cancel?
34. Why should executors be chosen deliberately?

## Virtual Threads

35. What is a virtual thread?
36. What is a carrier thread?
37. Why are virtual threads useful for blocking I/O?
38. Why do virtual threads not automatically speed up CPU work?
39. Concurrency versus parallelism?

## System Design

40. Why does an in-process lock not solve a multi-instance problem?
41. Why does a bounded queue provide backpressure?
42. Why can worker concurrency overwhelm a database?
43. Why should external provider calls have concurrency limits?
44. Why is graceful shutdown important for job processing?
45. What changes when the worker moves from one JVM to multiple JVMs?

------------------------------------------------------------------------

# 74. Required Failure Experiments

Before Phase 5, reproduce and document:

### Failure 1 --- Lost Increment

``` text
100 threads
10,000 increments each
```

Compare:

``` text
unsafe int
synchronized
AtomicInteger
```

### Failure 2 --- Visibility

Compare:

``` text
boolean
volatile boolean
```

for a stop signal.

### Failure 3 --- Check-Then-Act

Use `ConcurrentHashMap` with unsafe compound operations.

Fix with:

``` text
compute
putIfAbsent
merge
```

### Failure 4 --- Deadlock

Reproduce:

``` text
A → B
B → A
```

Diagnose with a thread dump.

### Failure 5 --- Starvation

Create a heavily contended resource where some workers rarely progress.

### Failure 6 --- Livelock

Create two workers that continually react without making progress.

### Failure 7 --- Queue Saturation

Create:

``` text
producer > consumer
```

and observe bounded vs unbounded behavior.

### Failure 8 --- Thread Pool Exhaustion

Submit more blocking tasks than the executor can handle.

Observe:

``` text
queue
rejections
latency
```

### Failure 9 --- CompletableFuture Failure

Create a multi-stage pipeline where the middle stage fails.

Trace exception propagation.

### Failure 10 --- Cancellation

Cancel a running task and determine whether underlying work actually
stops.

### Failure 11 --- Virtual Threads

Compare platform threads and virtual threads for:

``` text
I/O-bound workload
CPU-bound workload
```

### Failure 12 --- Database Pool Saturation

Run more workers than available database connections.

Observe the interaction between:

``` text
executor
DB pool
PostgreSQL
```

------------------------------------------------------------------------

# 75. Engineering Deliverables

Create:

``` text
docs/concurrency-notes.md
docs/java-memory-model.md
docs/thread-pool-notes.md
docs/completable-future-notes.md
docs/virtual-thread-notes.md
docs/concurrency-benchmarks.md
docs/concurrency-budget.md
docs/job-executor-design.md

docs/adr/ADR-009-concurrency-model.md
docs/adr/ADR-010-job-executor.md
docs/adr/ADR-011-worker-backpressure.md
docs/adr/ADR-012-virtual-threads.md
```

Every benchmark must include:

``` text
environment
workload
configuration
measurement
result
interpretation
limitations
```

------------------------------------------------------------------------

# 76. Suggested Git Commit Sequence

``` text
feat(concurrency): add concurrency laboratory
test(concurrency): reproduce shared-counter race
feat(concurrency): add synchronized counter
feat(concurrency): add volatile visibility experiment
feat(concurrency): add atomic counter
feat(concurrency): add lock experiments
feat(concurrency): add concurrent collection experiments
feat(concurrency): add producer-consumer queue
feat(concurrency): add executor infrastructure
feat(concurrency): add bounded worker pool
feat(concurrency): add completable-future experiments
feat(concurrency): add timeout and cancellation handling
feat(concurrency): add deadlock laboratory
feat(concurrency): add virtual-thread experiments
feat(job): add in-process job executor
feat(job): add retry and backpressure
feat(job): add graceful shutdown
feat(notification): add provider concurrency isolation
test(concurrency): add stress tests
perf(concurrency): add benchmark suite
docs(concurrency): document JVM and concurrency findings
```

------------------------------------------------------------------------

# 77. Expected Phase 4 Architecture

``` text
                         HTTP
                          │
                          ▼
                  Application Services
                          │
             ┌────────────┴────────────┐
             ▼                         ▼
       Synchronous Work          Job Submission
                                       │
                                       ▼
                                Bounded Queue
                                       │
                                       ▼
                                Worker Executor
                                       │
                         ┌─────────────┼─────────────┐
                         ▼             ▼             ▼
                    Job Handler   Job Handler   Job Handler
                         │             │             │
                         └─────────────┼─────────────┘
                                       ▼
                                  PostgreSQL
```

The worker system is still **single-process**.

That limitation is intentional.

------------------------------------------------------------------------

# 78. Phase 4 → Phase 5

Phase 5 should introduce:

``` text
Redis
Kafka
Distributed Job Processing
Distributed Idempotency
Distributed Locks
Caching
Rate Limiting
Retries
Dead Letter Queues
Eventual Consistency
```

The conceptual transition is:

``` text
Phase 4

Thread A
Thread B
Thread C
      ↓
same JVM


Phase 5+

Instance A
Instance B
Instance C
      ↓
shared distributed state
```

The concurrency concepts learned here become the foundation for
distributed systems.

------------------------------------------------------------------------

# 79. Final Rule

> Do not use concurrency abstractions until you understand the problem
> they solve.

When you see:

``` java
CompletableFuture.supplyAsync(...)
```

or:

``` java
executor.submit(...)
```

or:

``` java
synchronized
```

you should be able to reason about:

``` text
which thread executes
where state lives
what synchronization exists
what happens-before guarantee exists
what happens under contention
what happens on failure
what happens on cancellation
what happens when capacity is exhausted
what happens during shutdown
```

The goal is not:

> "I know Java concurrency APIs."

The goal is:

> **I can design, test, debug and measure concurrent Java systems, and I
> understand where JVM guarantees end and distributed-system
> coordination begins.**
