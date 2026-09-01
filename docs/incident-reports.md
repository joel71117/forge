# Incident Reports and Failure Experiments

This document combines the distributed failure reports and the recorded experiment results.

## Incident 01: Kafka Outage After Commit

### Trigger
Kafka becomes unavailable after a business transaction commits.

### Expected behavior
The transaction remains committed and its outbox event stays pending for later dispatch.

### Evidence
The outbox contains the event and the dispatcher records the Kafka failure.

### Recovery
Restore Kafka and allow the dispatcher to retry pending events.

## Incident 02: Duplicate Consumer Delivery

### Trigger
A consumer crashes after applying business effects but before committing its Kafka offset.

### Expected behavior
Kafka redelivers the event, but the consumer does not apply the business effect twice.

### Evidence
`processed_events` contains `(event_id, consumer_name)` and the duplicate is recorded as a no-op.

### Recovery
Continue consumption; investigate duplicates through structured logs and metrics.

## Incident 03: Poison Message to DLT

### Trigger
A malformed or permanently invalid event cannot be processed successfully.

### Expected behavior
The event receives bounded retries and is moved to a dead-letter topic.

### Evidence
Retry attempts and the DLT publication are observable.

### Recovery
Correct the payload or consumer issue, then deliberately replay the DLT message.

## Incident 04: Redis Outage

### Trigger
Redis becomes unavailable while the application is serving requests.

### Expected behavior
Core business state remains correct because PostgreSQL is authoritative. Cache reads fall back where safe.

### Evidence
Health and core requests remain available according to the endpoint’s failure policy.

### Recovery
Restore Redis; cache entries repopulate through normal cache-aside reads.

## Incident 05: Notification Provider Timeout

### Trigger
A notification provider is slow or stops responding.

### Expected behavior
Timeouts, bounded retries, bulkheads, and the circuit breaker prevent the provider from exhausting worker capacity.

### Evidence
Provider failures become durable notification failures and circuit state changes are observable.

### Recovery
Keep eligible work pending, allow the provider to recover, and dead-letter permanent failures.

## Experiment Results: 2026-08-23T17:10:26Z

### Setup
- Compose file: `infrastructure/distributed-compose.yaml`
- Services: three Forge instances, Kafka, PostgreSQL, and Redis

### Baseline
All health checks returned `UP` for liveness and readiness.

### Forge instance outage
`forge-a` and `forge-c` remained healthy while `forge-b` was stopped. `forge-b` recovered after restart.

### Redis outage
The application health endpoint remained available during the Redis outage and recovered after Redis restarted.

### Kafka outage
The application health endpoint remained available during the Kafka outage and recovered after Kafka restarted.

### Result
The recorded failure experiments passed. Raw container status from the run is retained in the project history; repeat experiments should record fresh timestamps and container state.
