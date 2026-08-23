# CompletableFuture Notes

- `thenApply`: transform `T` into `U`.
- `thenCompose`: flatten `T` into `CompletableFuture<U>` for dependent asynchronous work.
- `thenCombine`: combine two independent futures.
- `allOf`: wait for many futures, with result collection handled separately.
- `exceptionally`: recover with a fallback.
- `handle`: transform either success or failure.
- `whenComplete`: observe completion without changing the result.
- `orTimeout`: fail on timeout; `completeOnTimeout`: provide a fallback.

Use an explicit executor for the workload. Do not put blocking database or provider work on the common pool by default. Cancellation and timeout are signals; underlying work must cooperate for prompt termination.
