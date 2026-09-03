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
Docs/Phase_6/production-operations.md
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
runbooks/high-api-latency.md
runbooks/high-kafka-lag.md
runbooks/outbox-backlog.md
runbooks/database-exhaustion.md
runbooks/redis-outage.md
runbooks/provider-outage.md
runbooks/pod-crash-loop.md
runbooks/oom-kill.md
runbooks/dlt-growth.md
runbooks/failed-deployment.md
runbooks/kafka-recovery.md
runbooks/database-restore.md
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
Docs/Phase_6/production-operations.md
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
Docs/Phase_6/production-operations.md
```

ADRs:

```text
ADR-024-service-boundaries.md
ADR-025-notification-service.md
ADR-026-service-data-ownership.md
ADR-027-saga-strategy.md
ADR-028-api-gateway.md
ADR-029-kubernetes.md
ADR-030-observability-stack.md
ADR-031-slo-and-error-budget.md
ADR-032-deployment-strategy.md
ADR-033-secrets-management.md
ADR-034-service-to-service-security.md
ADR-035-autoscaling.md
ADR-036-disaster-recovery.md
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
