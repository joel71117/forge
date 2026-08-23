# Virtual Thread Notes

Java 21 supports virtual threads through `Executors.newVirtualThreadPerTaskExecutor()`.

They are useful for high-concurrency blocking I/O because many virtual threads can be scheduled over a smaller number of carrier platform threads. They do not make CPU-bound work faster; CPU parallelism remains limited by available processors.

The laboratory includes a blocking-task example. Production adoption should be based on measurements, provider limits, database-pool capacity, and pinning analysis.
