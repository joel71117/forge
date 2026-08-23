# Job Executor Design

`JobExecutor` is a single-process bounded `ThreadPoolExecutor` around the existing `Job` state machine.

Flow:

`submit -> bounded queue -> named worker -> handler registry -> domain transition -> repository`

Workers call `start`, invoke the handler, then call `complete`. Exceptions call `fail`; jobs with remaining retry capacity call `retry` and are resubmitted after bounded exponential backoff. Exhausted jobs become `DEAD_LETTERED` through the domain method.

Overload is explicit: a full queue raises `RejectedExecutionException`. Shutdown stops acceptance, waits for active work, and uses a timeout before interrupting remaining tasks.

The current handlers are placeholders because payment, reservation, report, and notification execution ports are not yet present. Connecting those handlers to real application operations is the next business-specific refinement, not a reason to bypass the domain state machine.
