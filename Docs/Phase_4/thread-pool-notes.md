# Thread Pool Notes

A pool has worker capacity, a queue, and an overload policy. A bounded `ArrayBlockingQueue` prevents unbounded memory growth.

Forge's job executor uses:

- core size: 4
- maximum size: 8
- queue capacity: 1000
- named threads: `forge-job-worker-N`
- explicit abort on overload
- graceful shutdown with a timeout

`CallerRunsPolicy` is useful when producer backpressure is intentional. `AbortPolicy` is useful when the API must reject overload explicitly. Pool size must be measured against CPU, I/O wait, database connections, and provider limits.
