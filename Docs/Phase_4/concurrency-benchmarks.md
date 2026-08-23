# Concurrency Benchmarks

## Environment

- JDK: Java 21
- Build: Maven wrapper
- Hardware: record CPU, cores, memory, and operating system before each run

## Baseline workload

- Counter: 100 threads, 10,000 increments each
- Blocking tasks: 20 tasks, 1 ms wait in the automated test; benchmark target is 10,000 tasks at 100 ms
- Pool variables: worker sizes 1, 2, 4, 8, 16, 32, 64 where the workload permits

## Measurements to record

Completion time, throughput, p50, p95, p99, CPU, memory, queue depth, rejected tasks, and errors.

The current repository contains correctness experiments, not statistically rigorous JMH results. Use JMH for serious microbenchmarks and repeat runs after correctness tests pass.
