# 03 — To-Be Architecture

> Agency: Ministry of Citizen Services (MCS)
> Phase: Target State Design

---

## 1. Design Principles

1. **API-led connectivity**: System API wraps one system, Process API orchestrates business capabilities, Experience API shapes data for channels.
2. **Sync and async separated**: citizen lookup is synchronous HTTP; change notifications are Kafka events.
3. **Do not modify legacy systems**: wrap CMS in a System API, never touch its code.
4. **No PII in logs**: only correlation IDs in log output.
5. **Environment externalised**: dev/test/prod properties, no hardcoded URLs.

---

## 2. Target Architecture Overview

```
                     Staff / Citizen Portal
                              │ HTTPS + OAuth2
                              ▼
                ┌──────────────────────────────────────┐
                │  Experience Layer                     │
                │  citizen-360-api (port 8081)          │
                │  · APIkit router                       │
                │  · Thin facade: channel field shaping │
                │  · Audit log (who/when/correlationId) │
                └──────────────────┬────────────────────┘
                                   │ HTTP (internal mesh)
                                   ▼
                ┌──────────────────────────────────────┐
                │  Process Layer                         │
                │  citizen-360-process-api (port 8084) │
                │  · Scatter-Gather (3 parallel routes) │
                │  · DataWeave aggregation              │
                │  · On Error Continue fallback          │
                │  · Async Kafka publish                 │
                └─────┬──────────────┬──────────────┬────┘
                      │              │              │
          ┌───────────▼──┐  ┌───────▼────────┐  ┌──▼───────────────┐
          │ System Layer  │  │                │  │                  │
          │ system-       │  │ system-case-   │  │ system-license-  │
          │ identity-api  │  │ api             │  │ api              │
          │ (8082)        │  │ (8083)          │  │ (8085)           │
          └───────┬───────┘  └───────┬────────┘  └───────┬──────────┘
                  ▼                  ▼                   ▼
             RealMe SaaS       Case Mgmt (legacy)   Licensing API
             (DIA)             (unchanged)          (cross-agency)

Async event stream:
   Process ──► Kafka topic: citizen.events ──► BI / audit / notifications
```

---

## 3. Core Data Flows

### Flow A: Citizen 360 lookup (synchronous, < 1s)

```
Staff terminal
   │ GET /citizens/RN-12345/360 (Bearer token)
   ▼
citizen-360-api (Experience)
   │ 1. OAuth2 token validation
   │ 2. Audit log (staffId, citizenId, correlationId)
   │ 3. Call Process API
   ▼
citizen-360-process-api (Process)
   │ Scatter-Gather (parallel):
   ├─► system-identity-api  → RealMe
   ├─► system-case-api      → Case Mgmt
   └─► system-license-api   → Licensing
   │
   │ Each route has independent On Error Continue
   ▼
DataWeave aggregation → { identity, cases, licenses, asOf }
   │
   ├─► Return to Experience
   └─► Async publish CitizenViewed event to Kafka (non-blocking)
   ▼
Experience returns response to staff
```

**Key decisions:**
- Scatter-Gather over sequential HTTP: P95 ≈ max(3 downstream), not sum.
- Per-route `On Error Continue`: RealMe failure does not block cases.
- Kafka publish in `<async>`: staff does not wait for event write.

### Flow B: Case change event (async)

```
Case Mgmt (legacy)
   │ Case status changes
   ▼
batch-citizen-sync (nightly incremental pull)
   │ watermark = lastModifiedDate >= lastRun
   ▼
DataWeave normalisation
   ▼
Kafka topic: citizen.events
   │
   ├─► BI (replaces T+1 ETL)
   ├─► Audit service
   └─► Notification service
```

### Flow C: Batch trigger

```
Kafka topic: citizen.sync.request → batch-citizen-sync
   │ process-records: normalise → publish Kafka
   │ commit: every 100 records
   │ on-complete: log loaded/succeeded/failed
   ▼
Update watermark to Object Store
```

---

## 4. Pain Point Resolution

| Pain Point | To-Be Solution | Layer |
|---|---|---|
| **P1** No unified view | `citizen-360-api` single endpoint | Experience |
| **P2** No orchestration | `citizen-360-process-api` centralises cross-system calls | Process |
| **P3** T+1 data | Kafka `citizen.events` near-real-time | Batch → Kafka |
| **P4** CMS no SLA | `system-case-api` adds timeout, retry, fallback | System |
| **P5** Scattered credentials | All via `${secure::...}` in Anypoint Secrets | Global |
| **P6** No audit | Experience logs every access | Experience |

---

## 5. Deployment Topology

### Local Development

```
Docker Desktop
 ├─ kafka (KRaft, localhost:9092)
 └─ kafka-ui (localhost:8090)

Anypoint Studio (local):
 ├─ system-identity-api    :8082
 ├─ system-case-api        :8083
 ├─ system-license-api     :8085
 ├─ citizen-360-process-api:8084
 └─ citizen-360-api        :8081

Spring Boot mocks (IntelliJ):
 ├─ external-dia-realme    :9082
 ├─ external-case-mgmt     :9083
 └─ external-mbie-licensing:9085
```

### Production (CloudHub 2.0)

```
                    CloudHub Load Balancer
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
        Experience      Process       System APIs
        (auto-scale)    (auto-scale)  (fixed workers)
                            │
                    Anypoint Secrets
                            │
                    Managed Kafka (GovCloud)
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
             BI           Audit         Notifications
```

---

## 6. Module Inventory

| Module | Type | Port | Responsibility |
|---|---|---|---|
| `system-identity-api` | Mule | 8082 | Wraps RealMe identity |
| `system-case-api` | Mule | 8083 | Wraps legacy Case Mgmt |
| `system-license-api` | Mule | 8085 | Wraps cross-agency Licensing |
| `citizen-360-process-api` | Mule | 8084 | Scatter-Gather + DataWeave + fallback |
| `citizen-360-api` | Mule | 8081 | Experience facade + OAuth2 + audit |
| `external-dia-realme` | Spring Boot | 9082 | DIA RealMe mock |
| `external-case-mgmt` | Spring Boot | 9083 | Legacy Case Mgmt mock |
| `external-mbie-licensing` | Spring Boot | 9085 | MBIE Licensing mock |

---

## 7. Technology Choices

| Decision | Choice | Rationale |
|---|---|---|
| Integration platform | MuleSoft 4.4 | Mandated platform (C5) |
| Event bus | Kafka KRaft | Pub/sub, multiple consumers; KRaft removes ZooKeeper |
| Legacy mock | Spring Boot 3.2 | Real CMS is Java; familiar tooling |
| API contracts | RAML 1.0 | Mule-native, APIkit auto-routes |
| Auth | OAuth2 client-credentials | Government IAM standard |
| Error handling | Per-route On Error Continue | Single downstream failure does not block others |
| Batch | Mule Batch + watermark | Incremental, idempotent |
| Config | `${secure::...}` + env properties | Secrets out of git, compliance-ready |
