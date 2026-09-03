# Forge Phase 6 - Production Operations Guide

This guide combines the Phase 6 operational architecture, deployment,
security, observability, reliability, incident response, capacity, and
recovery guidance. It is the canonical home for these topics.

## Contents

- Service boundaries and communication
- Saga design
- Production and Kubernetes architecture
- Security
- Observability and reliability targets
- Capacity and deployment
- Incident response, chaos engineering, and disaster recovery

## Service Boundaries and Communication

Forge remains a modular monolith for the API, catalog, inventory, orders,
jobs, users, and administration. These modules share the Forge database and
are released together because their transactions and scaling needs are
currently tightly coupled.

| Area | Data owner | Events | Scaling/failure profile | Decision |
| --- | --- | --- | --- | --- |
| Catalog | Forge DB | Product changes | Read-heavy, API-bound | Monolith |
| Inventory | Forge DB | Reservation events | Contention-sensitive | Monolith |
| Orders | Forge DB | Order lifecycle | Transactional API | Monolith |
| Jobs | Forge DB | Job submitted/completed | Worker-bound | Monolith initially |
| Notifications | Notification DB (target) | Order confirmed | Provider latency/failure isolated | First extraction |
| Users/Auth | Forge DB | Auth events | Security boundary | Monolith initially |
| Administration | Forge DB | Operational commands | Low volume, privileged | Monolith initially |

Notification processing is the first extraction candidate because provider
outages and retry load should not consume API capacity. The current
repository implements the boundary as an isolated module and Kafka consumer;
the next extraction step is a separately built service with its own migrations
and database. No Forge code may access that database directly.

Use REST when the caller needs a bounded, immediate result, such as a query or
validated command. Use Kafka when work may be delayed, retried, replayed, or
processed by multiple consumers. Every event carries an event ID and is
published through the transactional outbox. Consumers must be idempotent and
must not read another service's database.

## Saga Design

The order workflow is an event choreography:

```text
OrderCreated
    -> InventoryReserved
    -> PaymentAuthorized
    -> OrderConfirmed
```

When payment fails:

```text
Payment failure
    -> InventoryReleased
    -> OrderCancelled
```

Choreography keeps the participants autonomous but makes visibility and
recovery harder. An orchestrator would centralize state and compensation at
the cost of coupling. The current outbox and processed-event stores provide
the delivery and idempotency foundations for either implementation.

## Production and Kubernetes Architecture

Forge is currently a production-oriented modular monolith deployed as three
or more Kubernetes API replicas. PostgreSQL owns transactional state, Redis
owns recreatable cache and coordination state, and Kafka carries durable
asynchronous events. The outbox closes the database-to-Kafka publication gap.

`forge-api` is a three-replica Deployment behind a Service and Ingress. A
ConfigMap carries non-secret settings and a Secret carries database
credentials. Startup protects slow initialization, readiness controls
traffic, and liveness controls process restart. HPA scales on CPU until a
workload metric such as Kafka lag is available. A PodDisruptionBudget and
topology spreading protect availability during voluntary disruption.

Apply the manifests with a provisioned secret:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl -n forge create secret generic forge-secrets --from-env-file=.env.prod
kubectl apply -k k8s/
```

Build with `docker build -t forge:phase-6 .`. Before production, replace the
placeholder infrastructure names with managed services, configure TLS,
install metrics-server, and validate backup restore and failure drills.

The container should use a multi-stage build where appropriate, a small
runtime image, a non-root user, an immutable image, and configuration through
the environment or platform configuration. Secrets must not be included in
the image.

Health probes have distinct responsibilities:

```text
startup    -> has initialization completed?
readiness  -> should traffic be sent here?
liveness   -> should the process be restarted?
```

Liveness should not fail merely because an external dependency is temporarily
unavailable unless restarting is the correct recovery action. On shutdown:

```text
SIGTERM
    -> stop accepting traffic
    -> mark unready
    -> stop accepting new jobs
    -> finish safe in-flight work
    -> acknowledge only completed Kafka work
    -> shut down executors
    -> exit
```

## Security

Production credentials enter through a secret manager or Kubernetes Secret and
never through images, source, or ConfigMaps. The checked-in
`k8s/secret.template.yaml` contains placeholders only.

The API runs as a non-root user with a tokenless service account. Network
policy restricts application egress to DNS, PostgreSQL, Redis, and Kafka.
Production should add TLS at ingress and service authentication for every
cross-service call; mTLS is preferred where the platform supports it. Separate
service accounts and namespace-scoped RBAC prevent application pods from
administering the cluster or reading unrelated secrets.

## Observability and Reliability

Forge exposes Actuator health, metrics, and Prometheus endpoints under
`/actuator`. Kubernetes uses `/actuator/health/readiness` for traffic
eligibility and `/actuator/health/liveness` for process recovery. Readiness
includes the database; liveness only reflects application liveness.

Logs use ECS JSON on stdout. Correlation IDs are accepted and returned through
`X-Correlation-Id`; trace fields are supplied when tracing is enabled. Never
log credentials, tokens, provider secrets, or sensitive payloads.

Initial metrics cover HTTP RED signals through Actuator/Micrometer, JVM and
connection-pool metrics, outbox backlog, and job queue depth/failures. Keep
labels bounded to route, method, status, service, and job type. Do not label
metrics with event, request, or job IDs.

The next deployment step is an OpenTelemetry Collector with Prometheus,
Grafana, Loki, and Tempo. Instrumentation must preserve correlation and
causation IDs across the outbox and Kafka consumer boundary.

Initial monthly targets:

| Workflow | SLI | SLO | Monthly budget |
| --- | --- | --- | --- |
| API | Successful requests | 99.9% | 43m 49s |
| API | Requests under 500 ms | 99% | 7h 18m |
| Jobs | Successful terminal execution | 99.5% | 3h 39m |
| Notifications | Provider delivery success | 99% | 7h 18m |
| Kafka | Consumer lag under 60 seconds | 99% | 7h 18m |

Availability is measured from server responses, not pod health alone. Latency
is measured at the API boundary. Error budgets are spent by failed or late
valid operations; alerts must link to a runbook and a measurable mitigation.
When a budget is exhausted, prioritize reliability work and freeze risky
changes to the affected workflow.

For an SLO of 99.9%, the monthly error budget is 0.1%, or approximately 43
minutes and 49 seconds. Burn rate should be calculated from the same SLI used
for the SLO. A fast burn pages the on-call engineer; a slow burn creates
planned reliability work. Do not page on symptoms that have no actionable
response.

## Capacity and Deployment

Measure API requests per second and p95/p99 latency, job throughput and queue
depth, database writes and pool utilization, Redis operations, Kafka
throughput and lag, CPU, memory, and storage growth. Establish baseline,
normal, peak, overload, and recovery runs. Find the first saturated resource
before adding replicas; scaling consumers cannot fix a partition, provider,
database, or downstream bottleneck.

The default deployment strategy is a rolling update with three replicas, zero
unavailable pods, one surge pod, startup/readiness/liveness probes, a 45-second
termination grace period, and a two-pod disruption budget. Database migrations
must follow expand, compatible application, backfill, switch, and later
contract.

Build and release gates are compile, tests, static analysis, dependency scan,
SBOM, image scan, staging smoke tests, then production rollout. A canary is a
future overlay that routes a small percentage of traffic and rolls back on
error rate, latency, or business-metric regression.

## Incident Response, Chaos Engineering, and Disaster Recovery

Declare an incident when an SLO or critical workflow is at risk. Record T0
detection, T1 investigation, T2 mitigation, T3 recovery, and T4 root cause.
Start with metrics, logs, traces, and dependency health; then inspect threads,
database state, Kafka lag, and Kubernetes events. Every incident ends with
corrective actions, an owner, and a verification date. Runbooks belong under
`runbooks/` and must include symptoms, first checks, mitigation, recovery,
verification, and escalation.

Every chaos experiment records a hypothesis, steady state, injection, expected
and observed behavior, rollback, and follow-up. The first experiment set is
pod kill, Redis/Kafka/PostgreSQL interruption, provider delay/failure, queue
fill, connection exhaustion, OOM, CPU throttling, consumer lag, faulty
deployment rollback, projection replay, and database restore. Run experiments
in an isolated environment with an explicit abort condition.

| System | RPO | RTO | Recovery source |
| --- | --- | --- | --- |
| PostgreSQL | 15 minutes | 1 hour | Verified backup and WAL archive |
| Kafka | 15 minutes | 1 hour | Replicated log or event replay |
| Redis | 5 minutes | 30 minutes | Recreate cache; restore only durable locks/state |
| Forge API | 0 | 15 minutes | Immutable image and manifests |

PostgreSQL backups are incomplete until a restore is performed in an isolated
environment and migrations, row counts, and critical workflow checks pass.
Kafka can rebuild projections and outbox-derived consumers; PostgreSQL remains
the source of truth for transactional orders, inventory, and job state.

Recorded failure scenarios include Kafka outage after commit, duplicate
consumer delivery, poison messages sent to a DLT, Redis outage, and notification
provider timeout. Expected recovery relies on the transactional outbox,
consumer idempotency, bounded retries, provider isolation, and PostgreSQL as
the authoritative business store.