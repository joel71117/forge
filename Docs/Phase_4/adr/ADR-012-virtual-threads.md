# ADR-012: Virtual Threads

## Decision

Evaluate virtual threads for blocking I/O experiments, but do not make them the default production executor yet.

## Rationale

They improve concurrency economics for suitable blocking workloads, not CPU throughput. Database and provider limits still apply, and adoption requires workload measurements.
