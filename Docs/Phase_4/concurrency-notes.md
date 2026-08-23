# Concurrency Notes

## Most-used features

1. `ExecutorService` and `ThreadPoolExecutor`: own task execution, capacity, naming, rejection, and shutdown.
2. `CompletableFuture`: compose genuinely independent or dependent asynchronous work.
3. `ConcurrentHashMap`: use `computeIfAbsent`, `putIfAbsent`, and `merge` for compound actions.
4. `BlockingQueue`: create bounded producer-consumer pipelines and explicit overload behavior.
5. `Semaphore`: cap calls to a database, provider, or other scarce resource.
6. `AtomicInteger` and `AtomicReference`: small lock-free state/counter operations with CAS.
7. `synchronized` and `ReentrantLock`: protect invariants and make visibility explicit.

## Selection rule

Start with the simplest mechanism that preserves the invariant. Prefer domain synchronization over parallelism for correctness, and measure before replacing a lock with a more specialized primitive.

The laboratory in `com.forge.concurrency.lab` contains focused examples for each mechanism.
