# Forge — Phase 6 Implementation
## Production-Grade Distributed Platform, Observability, Kubernetes & Reliability

**Objective:** Take Forge from a distributed application you understand into a system you can **deploy, observe, scale, secure, troubleshoot, and operate like a production backend platform**.

Phase 5 taught:

```text
How distributed components behave.
```

Phase 6 teaches:

```text
How to operate a distributed system reliably.
```

The emphasis is on architecture, deployment, operability, observability, reliability, security, scaling, failure recovery, and production debugging.

---

# 1. Learning Objectives

Master through implementation:

- service boundaries
- modular monolith vs microservices
- API gateway
- service-to-service communication
- service discovery
- load balancing
- containers
- Kubernetes
- Deployments and Services
- ConfigMaps and Secrets
- readiness/liveness/startup probes
- graceful shutdown
- rolling deployments
- resource requests and limits
- horizontal scaling
- autoscaling
- PodDisruptionBudget
- affinity and topology spreading
- centralized configuration
- centralized logging
- OpenTelemetry
- distributed tracing
- metrics
- RED/USE methodology
- SLIs, SLOs and error budgets
- alerting
- incident response
- runbooks
- chaos engineering
- dependency failure handling
- security boundaries
- service-to-service authentication
- secrets management
- least privilege
- network policies
- production debugging
- capacity planning
- disaster recovery
- backup/restore
- deployment strategies
- zero-downtime deployments

---

# 2. Target Architecture

```text
                         Internet / Clients
                                │
                                ▼
                         Ingress / Gateway
                                │
                 ┌──────────────┼──────────────┐
                 ▼              ▼              ▼
             API Service   Job Service    Admin Service
                 │              │              │
                 └──────────────┼──────────────┘
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
         PostgreSQL           Redis              Kafka
              │                 │                 │
              │                 │       ┌─────────┼─────────┐
              │                 │       ▼         ▼         ▼
              │                 │     Jobs   Notifications Events
              │                 │
              └─────────────────┘

                         Observability
                              │
             ┌────────────────┼────────────────┐
             ▼                ▼                ▼
           Metrics          Logs            Traces
             │                │                │
             └────────────────┼────────────────┘
                              ▼
                    Observability Platform
```

Do **not** split every Forge module into a microservice. Extract a service only when independent data ownership, scaling, deployment, or failure isolation provides a real benefit.

---

# 3. Service Boundary Review

Create:

```text
this consolidated document
```

Evaluate:

```text
Inventory
Orders
Jobs
Notifications
Users/Auth
Administration
```

For each document:

```text
data ownership
API ownership
events produced/consumed
transaction boundaries
scaling characteristics
failure profile
deployment independence
```

Then choose what remains in the modular monolith and what is extracted.

---

# 4. First Service Extraction

Extract notification processing into an independent service:

```text
Forge API
    │
    ▼
Kafka
    │
    ▼
Notification Service
    │
    ├── Email
    ├── SMS
    └── Push
```

Notification Service owns its own:

```text
notification attempts
provider state
retry state
templates
```

Forge must not directly access its database.

---

# 5. Database-per-Service

Introduce separate ownership:

```text
Forge DB
Notification DB
```

Prohibit:

```text
Forge API → Notification DB
Notification Service → Forge DB
```

Cross-service communication must use APIs or events.

This forces explicit distributed data ownership.

---

# 6. Distributed Transactions and Sagas

Implement a workflow such as:

```text
OrderCreated
 ↓
InventoryReserved
 ↓
PaymentAuthorized
 ↓
OrderConfirmed
```

Failure:

```text
Payment fails
 ↓
InventoryReleased
 ↓
OrderCancelled
```

First implement event-driven choreography.

Then implement the same workflow using an orchestrator and compare:

```text
coupling
visibility
debuggability
failure handling
complexity
```

Document the tradeoff.

---

# 7. API Gateway and Service Communication

Introduce an ingress/API gateway for:

```text
TLS termination
routing
request limits
authentication integration
correlation IDs
```

Keep business logic out of the gateway.

Implement both:

```text
REST → immediate response required
Kafka → asynchronous workflow
```

Document why each communication path exists.

---

# 8. Kubernetes Fundamentals

Deploy Forge using:

```text
Namespace
Deployment
Service
ConfigMap
Secret
Ingress
```

Understand:

```text
Pod
ReplicaSet
Deployment
Service
Endpoint
Namespace
```

Run at least three API replicas.

Kill a pod and observe Kubernetes recreate it and route traffic to healthy replicas.

---

# 9. Containerization

Create production-oriented images:

```text
multi-stage build where appropriate
small runtime image
non-root user
immutable image
configuration via environment/config
no secrets in image
```

Scan images for vulnerabilities and remove unnecessary runtime packages.

---

# 10. Configuration and Secrets

Separate:

```text
application configuration
environment configuration
secrets
```

Use:

```text
ConfigMap
Secret
```

Never commit:

```text
DB passwords
JWT keys
provider credentials
Kafka credentials
```

Evaluate a dedicated secret manager such as Vault or a cloud secret manager and document the tradeoff.

---

# 11. Health Probes

Implement:

```text
startup
readiness
liveness
```

Understand:

```text
startup → has initialization completed?
readiness → should traffic be sent here?
liveness → should the process be restarted?
```

Do not make liveness fail merely because an external dependency is temporarily unavailable unless restart is actually the correct recovery action.

Test:

```text
slow startup
DB unavailable
Kafka unavailable
internal deadlock
```

---

# 12. Graceful Shutdown

Implement:

```text
SIGTERM
 ↓
stop accepting traffic
 ↓
mark unready
 ↓
stop accepting new jobs
 ↓
finish safe in-flight work
 ↓
ack/commit only completed Kafka work
 ↓
shutdown executors
 ↓
exit
```

Configure an appropriate Kubernetes termination grace period.

Kill a worker during message processing and verify idempotency protects against duplicate delivery.

---

# 13. Resource Management

Configure and measure:

```text
CPU requests
memory requests
CPU limits
memory limits
```

Run deliberate experiments for:

```text
OOM
CPU throttling
high allocation
GC pressure
```

Use measurements rather than arbitrary values.

---

# 14. Horizontal Scaling

Configure HPA for APIs and workers.

Test:

```text
2 replicas → increasing load → scale up → scale down
```

Use CPU initially, then evaluate workload-specific signals such as:

```text
Kafka lag
queue depth
requests/sec
```

Demonstrate why scaling the wrong layer does not fix the actual bottleneck.

---

# 15. Availability and Scheduling

Configure and understand:

```text
PodDisruptionBudget
pod anti-affinity
topology spread
```

Simulate:

```text
node drain
rolling maintenance
```

Ensure critical replicas are not voluntarily disrupted simultaneously.

---

# 16. Zero-Downtime Deployments

Perform:

```text
v1
 ↓
v1 + v2
 ↓
v2
```

Verify:

```text
readiness
connection handling
Kafka consumer behavior
DB compatibility
```

Implement expand/contract database migrations:

```text
expand
 ↓
compatible application
 ↓
backfill
 ↓
switch reads/writes
 ↓
contract later
```

---

# 17. Observability Stack

Build a local observability environment using an appropriate combination of:

```text
OpenTelemetry Collector
Prometheus
Grafana
Loki
Tempo or Jaeger
```

Instrument:

```text
API
Kafka producers/consumers
PostgreSQL
Redis
notification provider calls
```

---

# 18. Metrics

Track:

```text
HTTP rate
HTTP errors
HTTP p50/p95/p99
JVM heap/non-heap
GC
thread count
DB pool
Kafka lag
outbox backlog
job execution
notification latency
provider failures
Redis hit ratio
```

Use bounded labels such as:

```text
route
method
status
service
job_type
```

Avoid high-cardinality labels such as `eventId`, `jobId`, or `requestId`.

---

# 19. RED and USE

Apply RED to APIs:

```text
Rate
Errors
Duration
```

Apply USE to infrastructure/resources:

```text
Utilization
Saturation
Errors
```

Build Grafana dashboards around both.

---

# 20. JVM Production Diagnostics

Practice:

```text
jcmd
jstack
jmap
JFR
```

Investigate deliberately created:

```text
CPU spike
thread starvation
deadlock
allocation pressure
GC pressure
heap growth
```

Use Java Flight Recorder for at least one real performance investigation.

---

# 21. Distributed Tracing

Trace end-to-end:

```text
HTTP request
 ↓
Forge API
 ↓
PostgreSQL
 ↓
outbox
 ↓
Kafka
 ↓
consumer
 ↓
job
 ↓
provider
```

Propagate:

```text
traceId
spanId
correlationId
causationId
eventId
jobId
```

Verify asynchronous context propagation rather than assuming it works.

---

# 22. Structured Logging

Use structured JSON logs containing appropriate context:

```text
timestamp
level
service
traceId
spanId
correlationId
eventId
jobId
message
```

Never log:

```text
passwords
JWTs
API keys
sensitive payloads
```

Centralize logs and perform investigations using `traceId`, `correlationId`, `eventId`, and `jobId`.

---

# 23. SLIs, SLOs and Error Budgets

Define SLIs for:

```text
API availability and latency
job success and queue delay
notification success and latency
Kafka lag
```

Define realistic SLOs and calculate error budgets.

Example:

```text
99.9% availability
→ 0.1% error budget
```

Use error budgets to reason about reliability work versus feature velocity.

---

# 24. Alerting

Create actionable alerts for:

```text
API error rate
API p95/p99 latency
Kafka lag
outbox backlog
DLT growth
DB connection exhaustion
Redis outage
provider failure
pod crash loops
OOM
CPU/memory saturation
```

An alert should mean:

> Someone needs to take action.

---

# 25. Runbooks and Incident Response

Create:

```text
the embedded runbook sections below
```

Each runbook contains:

```text
symptoms
first checks
diagnostic tools
likely causes
mitigation
recovery
verification
escalation
```

Simulate at least five incidents and create postmortems with:

```text
T0 detection
T1 investigation
T2 mitigation
T3 recovery
T4 root cause
what went well
what went badly
corrective actions
```

---

# 26. Chaos Engineering

Start with controlled experiments:

```text
kill pod
stop Redis
stop Kafka
restart PostgreSQL
delay provider
fail provider
fill queue
exhaust DB connections
trigger OOM
CPU throttle
```

Then progress to:

```text
network latency
packet loss
partial dependency failure
consumer interruption
resource exhaustion
```

Every experiment must define:

```text
hypothesis
steady state
failure injection
expected behavior
observed behavior
rollback
follow-up
```

---

# 27. Disaster Recovery

Define:

```text
RPO
RTO
```

for:

```text
PostgreSQL
Kafka
Redis
application
```

Implement and verify PostgreSQL:

```text
backup
restore
verification
```

A backup that has never been restored is not a verified backup strategy.

Determine which Redis state is recreatable and which Kafka data can rebuild projections.

---

# 28. Capacity Planning and Load Testing

Create:

```text
this consolidated document
```

Estimate:

```text
requests/sec
events/sec
jobs/sec
DB writes/sec
Redis ops/sec
Kafka throughput
storage growth
```

Run load tests for:

```text
baseline
normal
peak
overload
recovery
```

Measure:

```text
throughput
p50
p95
p99
errors
CPU
memory
DB connections
Kafka lag
```

Find the breaking point and identify the actual bottleneck before adding capacity.

---

# 29. Security Architecture

Review:

```text
authentication
authorization
JWT
service-to-service authentication
secrets
TLS
DB credentials
Kafka credentials
Redis credentials
admin APIs
Kubernetes permissions
```

Apply least privilege.

Do not assume an internal network is automatically trusted.

Evaluate:

```text
mTLS
service tokens
network policies
RBAC
```

---

# 30. Kubernetes RBAC and Network Policies

Create separate service accounts.

Demonstrate:

```text
Service A cannot read Service B secrets
application pod cannot administer the cluster
```

Define network policies so only required communication paths are allowed.

---

# 31. CI/CD and Supply Chain

Build a pipeline:

```text
commit
 ↓
compile
 ↓
unit tests
 ↓
integration tests
 ↓
static analysis
 ↓
security scan
 ↓
build image
 ↓
image scan
 ↓
publish artifact
 ↓
deploy staging
 ↓
smoke tests
 ↓
production deployment
```

Add:

```text
dependency scanning
container scanning
SBOM generation
```

Failed critical quality/security gates should block deployment where appropriate.

---

# 32. Deployment Strategies

Implement or simulate:

```text
rolling
blue/green
canary
```

Compare:

```text
risk
complexity
rollback speed
resource cost
```

Start with rolling deployment and then implement a canary experiment.

---

# 33. Canary Release

Deploy a new version to a small percentage of traffic.

Observe:

```text
error rate
latency
business metrics
```

Rollback if unhealthy.

A technically healthy service can still have incorrect business behavior, so include business-level signals.

---

# 34. Feature Flags

Introduce feature flags supporting:

```text
off
internal
small percentage
all users
```

Every flag needs:

```text
owner
purpose
creation date
removal plan
```

Do not let feature flags become permanent architecture.

---

# 35. Production Debugging Drill

Create a mystery incident:

```text
API p99 latency increases
```

Potential causes:

```text
GC
DB pool exhaustion
slow query
Kafka lag
Redis latency
provider slowdown
thread pool starvation
CPU throttling
```

Diagnose using:

```text
metrics
logs
traces
thread dumps
database tools
Kubernetes tools
```

Do not inspect source code first.

---

# 36. End-to-End Performance Investigation

Create a deliberately slow workflow:

```text
HTTP
 ↓
service
 ↓
DB
 ↓
Kafka
 ↓
consumer
 ↓
provider
```

Use traces to determine where time is spent, then validate with component metrics.

---

# 37. Reliability Review

For each critical workflow:

```text
Order
Inventory
Job
Notification
```

Answer:

```text
What can fail?
What is the source of truth?
What can duplicate?
What can be delayed?
What can be lost?
How is it retried?
How is it recovered?
How is it observed?
How is it rolled back?
```

---

# 38. Required Documentation

Create:

```text
this consolidated document
```

ADRs:

```text
the embedded ADR sections below
```

---

# 39. Required Chaos Experiments

Complete at least:

1. Kill one API pod.
2. Kill one notification worker.
3. Stop Redis.
4. Stop Kafka.
5. Restart PostgreSQL.
6. Introduce provider latency.
7. Introduce provider failure.
8. Fill worker queues.
9. Exhaust database connections.
10. Trigger OOM.
11. Trigger CPU throttling.
12. Create Kafka consumer lag.
13. Deploy an intentionally faulty version.
14. Roll back the faulty version.
15. Rebuild a projection from Kafka.
16. Restore PostgreSQL from backup.

---

# 40. Definition of Done

- [ ] Service boundaries are documented.
- [ ] At least one service has been independently extracted.
- [ ] Service data ownership is enforced.
- [ ] Cross-service transactions are removed.
- [ ] Saga behavior has been implemented and analyzed.
- [ ] API gateway/ingress is operational.
- [ ] Service discovery works.
- [ ] Services are containerized.
- [ ] Images are security-scanned.
- [ ] Forge runs on Kubernetes.
- [ ] Deployments and Services are understood.
- [ ] Configuration is externalized.
- [ ] Secrets are externalized.
- [ ] Readiness/liveness/startup probes are implemented.
- [ ] Graceful shutdown works.
- [ ] Rolling deployments work.
- [ ] Database migrations support rolling deployments.
- [ ] Resource requests and limits are configured.
- [ ] HPA works.
- [ ] Kafka-lag-based scaling has been evaluated.
- [ ] Pod disruption behavior is understood.
- [ ] Metrics are collected.
- [ ] Structured logs are centralized.
- [ ] Distributed traces work.
- [ ] RED dashboards exist.
- [ ] JVM metrics are visible.
- [ ] Database metrics are visible.
- [ ] Kafka lag is visible.
- [ ] SLIs are defined.
- [ ] SLOs are defined.
- [ ] Error budgets are calculated.
- [ ] Actionable alerts exist.
- [ ] Runbooks exist.
- [ ] At least five incidents have been simulated.
- [ ] Chaos experiments have been completed.
- [ ] PostgreSQL backup/restore has been tested.
- [ ] Capacity testing has been performed.
- [ ] Security architecture is documented.
- [ ] Kubernetes RBAC is configured.
- [ ] Network policies are configured.
- [ ] CI/CD pipeline exists.
- [ ] Dependency/container scanning exists.
- [ ] Canary/rollback has been tested.
- [ ] Feature-flag lifecycle is documented.
- [ ] JVM production diagnostics have been practiced.
- [ ] Database production diagnostics have been practiced.
- [ ] End-to-end performance investigation has been completed.
- [ ] Production architecture review has been completed.

---

# 41. Exit Questions

1. When should a modular monolith remain a monolith?
2. What makes a good service boundary?
3. Why should each service own its data?
4. When should REST be used instead of Kafka?
5. What is the difference between a Pod, Deployment and Service?
6. What does readiness protect?
7. What does liveness protect?
8. Why is startup probing useful?
9. What are resource requests and limits?
10. Why can CPU limits affect latency?
11. What determines useful HPA scaling?
12. What is the difference between metrics, logs and traces?
13. What do RED and USE mean?
14. Why is high-cardinality telemetry dangerous?
15. How would you investigate a p99 latency increase?
16. What is an SLI, SLO and error budget?
17. What makes a good alert?
18. Why are runbooks important?
19. Why must database migrations be backward compatible?
20. What is expand/contract migration?
21. What is a rolling deployment?
22. What is a canary?
23. What makes rollback difficult when schema changes are involved?
24. What is Kubernetes RBAC?
25. Why should services have separate service accounts?
26. What are network policies?
27. What is RPO?
28. What is RTO?
29. Why is a backup without a restore test insufficient?
30. Which Forge data can be reconstructed from Kafka?
31. Which data must be restored from PostgreSQL?
32. How do you determine whether an incident is CPU, DB, Kafka, Redis, provider, or thread-pool related?
33. Why can scaling the consumer count fail to reduce Kafka lag?
34. Which workflows should continue when Redis fails?
35. Which workflows should stop when PostgreSQL fails?

---

# 42. Suggested Git Commit Sequence

```text
refactor(architecture): document service boundaries
refactor(notification): extract notification service
feat(service): add service-to-service communication
feat(gateway): add ingress and routing
feat(kubernetes): add namespace and deployments
feat(kubernetes): add services and configuration
feat(kubernetes): add probes
feat(kubernetes): add graceful shutdown
feat(kubernetes): add resource configuration
feat(kubernetes): add horizontal autoscaling
feat(observability): add prometheus metrics
feat(observability): add structured logging
feat(observability): add opentelemetry tracing
feat(observability): add dashboards
feat(reliability): define sli and slo
feat(reliability): add alerts
docs(operations): add runbooks
feat(security): add service accounts
feat(security): add network policies
feat(ci): add security scanning
feat(ci): add container build pipeline
feat(deployment): add rolling deployment
feat(deployment): add canary experiment
feat(deployment): add rollback procedure
test(chaos): kill api pod
test(chaos): simulate redis outage
test(chaos): simulate kafka outage
test(chaos): simulate database outage
test(chaos): simulate provider outage
test(recovery): verify database restore
perf(load): add capacity test
docs(platform): document production architecture
```

---

# 43. Phase 6 → Phase 7

After Phase 6, Forge should be a serious production-style distributed platform.

Potential advanced Phase 7 topics:

```text
advanced JVM performance
advanced PostgreSQL
advanced Kafka
advanced Kubernetes
advanced distributed algorithms
multi-region architecture
active-active systems
global traffic management
CQRS
event sourcing
Kafka Streams
schema registry
advanced caching
database sharding
read replicas
partitioning
online schema migration
distributed consensus
Raft concepts
leader election
CRDTs
vector clocks
cost optimization
platform engineering
```

Only introduce these after the production-platform fundamentals are solid.

---

# 44. Final Rule

> A production backend is not complete when its API works. It is complete when you can explain what happens when the system is overloaded, partially broken, deployed, restarted, scaled, attacked, or recovering.

By the end of Phase 6, when Forge fails, your first question should no longer be:

> "Which line of Java is wrong?"

It should be:

```text
What changed?
Where is the failure?
What does the telemetry show?
Which dependency is saturated?
What is the failure domain?
What is the blast radius?
What is the safest mitigation?
How does the system recover?
How do we prevent recurrence?
```

That is the transition from **backend developer** to **backend/system engineer**.


# Consolidated Phase 6 Supporting Documentation



---

# Consolidated Operations Guide


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

Notification processing is the first extraction because provider outages and
retry load should not consume API capacity. The repository includes a
separately built notification worker under `notification-service/`, with its
own schema and database deployment. Forge disables its local consumers when
`FORGE_NOTIFICATION_LOCAL_CONSUMER_ENABLED=false`. No Forge code may access
the notification database directly.

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

---

# ADR-024: Service Boundaries


Keep catalog, inventory, orders, jobs, users, and administration in the modular monolith. Extract notifications because provider latency and retry load have an independent failure and scaling profile.


---

# ADR-025: Notification Service


Notification processing is a separate Kafka consumer and deployment. It owns attempts, provider state, retries, and templates. Forge communicates through events and never reads its database.


---

# ADR-026: Service Data Ownership


Each service owns its schema and migrations. Cross-service reads use APIs or events; database credentials and network policy prevent direct access.


---

# ADR-027: Saga Strategy


Use choreography for the initial order flow. Event IDs and processed-event records provide idempotency. Introduce an orchestrator only when visibility and compensation complexity justify central coordination.


---

# ADR-028: API Gateway


Terminate TLS, route traffic, apply request limits, and attach correlation context at ingress. Business rules remain in Forge services.


---

# ADR-029: Kubernetes


Use Deployments, Services, probes, resource budgets, HPA, PDB, topology spreading, ConfigMaps, Secrets, and namespace isolation for production scheduling and recovery.


---

# ADR-030: Observability Stack


Expose Micrometer Prometheus metrics and ECS logs. Use Prometheus/Grafana for metrics, Loki for logs, and Tempo through OpenTelemetry for traces. Labels remain bounded.


---

# ADR-031: SLO and Error Budget


Measure API success and latency, job success and delay, notification delivery, and Kafka lag. Page only on actionable fast burn; use slow burn for planned reliability work.


---

# ADR-032: Deployment Strategy


Rolling updates are the default with backward-compatible expand/contract migrations. Canary is the release experiment for risky changes; rollback is gated by technical and business signals.


---

# ADR-033: Secrets Management


Keep secrets outside images, source, and ConfigMaps. Kubernetes Secret is the local contract; production should source it from a managed secret store with rotation and audit.


---

# ADR-034: Service-to-Service Security


Use TLS and authenticated service identity for cross-service calls. Network policy is defense in depth, not authentication. Prefer mTLS where platform support permits.


---

# ADR-035: Autoscaling


Scale APIs on CPU initially. Scale consumers on lag or queue depth only after proving partitions and downstream dependencies have capacity.


---

# ADR-036: Disaster Recovery


PostgreSQL is authoritative and requires tested backups and restore. Kafka supports replay and projection rebuild. Redis cache is recreatable. Targets are documented as RPO 15 minutes and RTO one hour for stateful services.


---

# Capacity Test Plan


Run baseline, normal, peak, overload, and recovery tests against an isolated environment. Record requests/sec, p50/p95/p99, errors, CPU, memory, Hikari active/pending, Kafka lag, outbox age, and provider latency. Increase one load dimension at a time until the first SLO or saturation threshold fails. The first saturated dependency is the bottleneck; replicas are not a default remedy.

The result record must include date, image, dataset, load profile, bottleneck, maximum sustainable rate, recovery time, and recommended requests/limits/HPA target. No production capacity claim is valid until this record is populated.


---

# Chaos Experiment Register


All experiments run in isolated infrastructure with an abort condition and a named observer.

| Experiment | Hypothesis | Injection | Expected steady state | Rollback/verification |
| --- | --- | --- | --- | --- |
| API pod kill | Service remains available | Delete one API pod | Two Ready replicas serve traffic | Pod recreated; error budget unchanged |
| Notification worker kill | Kafka redelivers safely | Delete worker during processing | No lost or duplicate attempt | Group recovers; event ID remains unique |
| Redis stop | Core transactions continue | Stop Redis | Cache/optional coordination degrades | PostgreSQL workflows succeed |
| Kafka stop | Outbox retains intent | Stop broker | API transactions remain bounded | Broker restore drains outbox |
| PostgreSQL restart | Application becomes unready | Restart database | No corrupt committed state | Readiness recovers; smoke workflow passes |
| Provider delay/failure | Bulkhead protects API | Inject timeout/error | Bounded retries and DLT | Provider restored; replay sample |
| Queue fill/OOM/CPU | Limits produce visible protection | Saturate worker/resource | Backpressure or restart, not silent loss | Scale/rollback and verify lag drains |

Each run records T0-T4 timestamps, metrics, traces, commands, observed behavior, and follow-up owner in an incident record.


---

# Deployment Strategies


Rolling is the default and is configured in `k8s/deployment.yaml`. Canary is represented by `k8s/canary.yaml`; route a small, explicit percentage at the gateway, compare technical and business SLIs, and delete the canary or promote it after the observation window. Blue/green is not selected for normal operation because it doubles capacity; it remains a rollback exercise using two versioned Deployments and a Service selector switch.

Before rollout, apply expand migrations. During rollout, both versions must read/write compatible fields. Contract migrations happen only after the old version is gone and the backfill is verified.


---

# Feature Flag Lifecycle


Flags support `off`, `internal`, `percentage`, and `all` states. Every flag must record an owner, purpose, creation date, expiry/removal date, default-off behavior, and rollback command. Percentage assignment uses a stable user or account hash, never a request ID. Flags are configuration, not a substitute for authorization or schema compatibility.


---

# Incident Record Template


- Incident:
- Date/environment:
- SLO or steady state:
- Hypothesis and injection:
- T0 detection:
- T1 investigation:
- T2 mitigation:
- T3 recovery:
- T4 root cause:
- What went well:
- What went badly:
- Corrective action, owner, verification date:


---

# Runbook: Database Connection Exhaustion


**Symptoms:** Hikari pending connections, acquisition timeouts, or elevated API latency.

**Checks:** Inspect pool metrics, PostgreSQL `pg_stat_activity`, long transactions, locks, and slow queries.

**Mitigation:** Stop the offending workload or rollout, cancel unsafe sessions, and reduce concurrency. Do not blindly increase the pool.

**Recovery:** Pending connections remain zero and transaction latency returns to baseline.


---

# Runbook: Database Restore


**Procedure:** Restore PostgreSQL into an isolated environment, apply migrations, compare row counts and checksums for critical tables, and execute order/inventory/job workflow checks.

**Acceptance:** Restore meets the one-hour RTO and fifteen-minute RPO targets, application health is Ready, and no secret or production endpoint is used by the test.

**Escalation:** Record backup ID, restore duration, verification output, gaps, and corrective owner.


---

# Runbook: Dead-Letter Topic Growth


**Checks:** Identify topic, exception class, event type, partition, first failure, and whether the payload is malformed or dependency-related.

**Mitigation:** Fix the dependency or quarantine the poison event. Replay only after the consumer fix and with idempotency verified.

**Recovery:** Replay a bounded sample, confirm business state once, then monitor DLT rate and lag.


---

# Runbook: Failed Deployment


**Checks:** `kubectl -n forge rollout status deployment/forge-api`, pod events, readiness failures, error rate, latency, and database migration state.

**Mitigation:** Halt rollout and run `kubectl -n forge rollout undo deployment/forge-api` when the previous version is schema-compatible. Never roll back across an incompatible contract.

**Recovery:** Confirm all replicas Ready, SLOs healthy, and migration state documented.


---

# Runbook: High API Latency


**Symptoms:** API p95/p99 SLO breach or rising timeout rate.

**Checks:** `kubectl -n forge top pods`; inspect `http_server_requests_seconds`, Hikari active/pending connections, JVM GC, and traces. Compare route and status dimensions.

**Mitigation:** Stop a bad rollout, reduce load at the gateway, and scale only after identifying CPU, pool, query, or dependency saturation.

**Recovery:** Confirm latency and errors return below SLO for 15 minutes. Escalate with dashboard links and a representative trace ID.


---

# Runbook: High Kafka Lag


**Symptoms:** Consumer lag exceeds 60 seconds or queue delay SLO.

**Checks:** Inspect consumer group offsets, partition count, rebalance state, handler duration, DLT growth, provider latency, and database pool saturation.

**Mitigation:** Pause noncritical producers, fix poison messages, or increase consumers only when partitions and downstream capacity allow it.

**Recovery:** Verify lag declines, no duplicate side effects occur, and DLT remains stable.


---

# Runbook: Kafka Recovery


**Checks:** Broker health, topic metadata, producer errors, outbox age, consumer group state, and replication status.

**Mitigation:** Restore brokers first, then allow the outbox dispatcher and consumers to recover. Keep duplicate processing protected by event IDs.

**Recovery:** Verify publication, consumer lag, DLT rate, and representative end-to-end events.


---

# Runbook: OOM Kill


**Checks:** Pod termination reason, container memory usage, heap/non-heap metrics, GC logs, allocation profile, and recent traffic.

**Mitigation:** Stop the rollout or reduce load. Correct heap sizing and leaks before increasing limits; check `-XX:MaxRAMPercentage`.

**Recovery:** Restarted pods stay below limit under peak load and no recurring OOM occurs.


---

# Runbook: Outbox Backlog


**Symptoms:** `forge.outbox.pending` grows or publication age breaches its target.

**Checks:** Confirm dispatcher health, Kafka availability, database locks, failed sends, and oldest pending event.

**Mitigation:** Restore Kafka connectivity or dispatcher capacity. Do not delete pending rows; use replay tooling after isolating poison events.

**Recovery:** Confirm pending count and event age trend down and sample events exist in Kafka.


---

# Runbook: Pod CrashLoopBackOff


**Checks:** `kubectl -n forge describe pod`, previous logs, termination reason, probe failures, recent rollout, and node events.

**Mitigation:** Roll back a bad image, correct configuration, or raise a measured resource limit for OOM. Preserve logs before deletion.

**Recovery:** New pods become Ready and restart count remains stable for 15 minutes.


---

# Runbook: Notification Provider Outage


**Symptoms:** Provider failures, timeouts, circuit breaker open, or notification SLO burn.

**Checks:** Provider status, error type, retry volume, notification attempts, and DLT growth.

**Mitigation:** Keep bounded retries and bulkhead isolation active; route to a configured fallback or pause delivery without blocking API traffic.

**Recovery:** Drain retries gradually and verify idempotency prevents duplicate delivery.


---

# Runbook: Redis Outage


**Symptoms:** Redis connection errors, cache misses, or coordination failures.

**Checks:** `redis-cli ping`, application errors, cache hit ratio, rate-limit and lock usage.

**Mitigation:** Disable optional cache/rate-limit features where configured; preserve PostgreSQL as source of truth. Do not treat Redis loss as data loss.

**Recovery:** Restore connectivity and verify stale cache/lock state is safely recreated.
