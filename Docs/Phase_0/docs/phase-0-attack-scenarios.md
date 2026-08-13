# Phase 0 --- Architecture Attack Scenarios

Do not implement these yet.

For every scenario answer:

1.  What happened?
2.  What states could the system be in?
3.  What must never happen?
4.  What should eventually happen?
5.  What information is needed for recovery?
6.  Which invariant is involved?
7.  Which requirements are affected?
8.  What implementation questions remain unanswered?

## 1. Last Item Race

``` text
Inventory = 1
A buys 1
B buys 1
```

At most one succeeds. Inventory remains valid.

## 2. Duplicate Order Request

The same create-order request is submitted twice because the first
response was lost.

Determine how one logical order is identified.

## 3. Reservation Then Crash

``` text
reserve inventory
↓
application crashes
```

Determine how reservation expiry/recovery works and how double-release
is prevented.

## 4. Payment Success Then Timeout

``` text
Forge → Provider
Provider succeeds
response is lost
```

Determine whether payment is UNKNOWN and how reconciliation works.

## 5. Duplicate Kafka Event

The same `OrderCreated` event is delivered twice. The consumer must not
create duplicate business effects.

## 6. Worker Crash Before Acknowledgement

A worker performs the business effect, then crashes before acknowledging
the message. Determine recovery and idempotency.

## 7. Slow Provider

A provider takes 30 seconds while hundreds of requests arrive. Analyze
thread exhaustion, timeout, retry and isolation.

## 8. Provider Outage

Provider returns 503 for ten minutes. Analyze retry storms, backoff,
jitter, failover and dead-letter behavior.

## 9. Noisy Customer

Customer A submits 1,000,000 jobs while B submits 10. Define fairness
and capacity isolation.

## 10. Distributed Scheduler

Three application instances see the same scheduled job. Determine
ownership, lease expiry and crash recovery.

## 11. Kafka Outage

Order DB transaction succeeds but Kafka is unavailable. Determine
whether the order can commit and how the event is eventually published.
This should eventually lead to evaluating the transactional outbox
pattern.

## 12. Database Deadlock

Two transactions lock related records in different orders. Determine
detection, rollback and safe retry.

## 13. Redis Failure

Redis becomes unavailable. Identify which features can degrade/fallback
and which should fail safely.

## 14. Orphaned Job

A job remains RUNNING after its worker disappears. Determine lease,
detection and duplicate-execution risks.

## 15. Missed Scheduled Notification

A notification is scheduled for 02:00, but the application is down until
03:00. Define whether it is sent late, expired or skipped.

## 16. Duplicate Recovery

Two recovery workers simultaneously process the same orphaned
job/reservation. Recovery itself must remain safe.

## 17. Load Increase

``` text
10 → 100 → 500 → 1,000 → 5,000 req/s
```

Identify what could become the bottleneck and what measurements are
required.

## 18. 100 Workers

Identify limitations involving DB contention, Kafka partitions,
coordination, locks and duplicate execution.

## 19. 100 Million Jobs/Day

Estimate throughput and peak load. Decide how to scale partitions,
workers, storage and databases, and identify which consistency
guarantees must remain strong.
