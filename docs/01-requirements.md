# 01 — Requirements

> Project: Citizen360 Integration Hub
> Agency: Ministry of Citizen Services (MCS, fictional New Zealand government agency)
> Version: v1.0

---

## 1. Business Context

MCS is a government agency responsible for citizen benefits, housing registrations, and
licensing. Citizen data is scattered across independent systems built in different eras
by different vendors:

| System | Holds | Current State |
|---|---|---|
| **RealMe / Identity** | Identity data (name, address, verification status) | Central government SaaS, OAuth2 protected, REST API available |
| **Case Management** | Benefits applications, case status, approvals | 2012 Java monolith, single coarse HTTP endpoint, original vendor departed |
| **Permits & Licensing** | Licences and permits held by citizens | Maintained by another agency (MBIE-like), cross-agency IAM required |
| **Data Warehouse** | Reporting, BI, policy analysis | Nightly ETL batch, T+1 latency |

**Three business drivers:**

1. **Service centre staff** need a single screen showing identity + active cases + licences.
   Currently staff log into three separate systems, adding ~8 minutes per citizen.
2. **Data team** needs near-real-time case change events pushed to a government data bus
   (Kafka) for risk, audit, and notification consumers. Current T+1 batch is too slow.
3. **Future citizen self-service portal** must reuse the same citizen view without
   rebuilding the integration logic.

---

## 2. As-Is Pain Points

| # | Problem | Business Impact |
|---|---|---|
| P1 | No unified citizen view API | Staff open 3 systems, slow and error-prone |
| P2 | No orchestration layer for cross-agency calls | Every new channel requires direct IAM and audit setup |
| P3 | Case data sync is T+1 | Audit and risk cannot see same-day changes |
| P4 | Case Management has no SLA or versioning | Frontline staff cannot rely on it |
| P5 | Credentials scattered across app configs | Fails Privacy Act 2020 compliance audit |
| P6 | No unified access audit log | Cannot trace who viewed which citizen and when |

---

## 3. Goals

| # | Goal | Success Criteria |
|---|---|---|
| G1 | Unified Citizen360 REST API | Single call returns identity + cases + licences, P95 < 1s |
| G2 | Reusable API-led layers | New channel (portal) integrates in days without touching system layer |
| G3 | Near-real-time events to Kafka | Change to visible < 5 minutes |
| G4 | Centralised authentication | All cross-agency calls use OAuth2, secrets in Anypoint Secrets |
| G5 | Full audit trail | Every data access logged with who/when/correlationId |
| G6 | Automated testing and CI/CD | MUnit green on main, auto-deploy to Sandbox |

---

## 4. Scope

### In Scope

- `system-identity-api` — wraps RealMe identity lookup (local mock)
- `system-case-api` — wraps Case Management (Spring Boot mock simulating legacy)
- `system-license-api` — wraps Permits & Licensing (local mock)
- `citizen-360-process-api` — Scatter-Gather parallel aggregation
- `citizen-360-api` — Experience layer thin facade, OAuth2 protected
- Local Docker middleware (Kafka + Kafka UI)
- Spring Boot mock services
- GitHub Actions CI/CD
- CloudHub deployment descriptor

### Out of Scope

- Real RealMe integration (mocked in POC; production requires government IAM approval)
- Real cross-agency MBIE integration
- Citizen self-service portal (future phase, architecture reserved)
- Bidirectional sync (read-only in this phase)
- Monitoring/alerting (PagerDuty, ChatOps)
- Field-level PII encryption (production requirement under Privacy Act)
- Bilingual (Te Reo Māori / English) support

---

## 5. Functional Requirements

| ID | Requirement | Acceptance Criteria |
|---|---|---|
| FR-01 | `GET /citizens/{id}/360` | 200 + JSON with identity + cases + licenses + asOf |
| FR-02 | Parallel downstream calls | Scatter-Gather, latency ≈ max(three) not sum |
| FR-03 | Single downstream failure does not break the API | Identity down → fallback identity, cases/licenses still return, HTTP 200 |
| FR-04 | Async Kafka event on view | topic=`citizen.events`, payload has citizenId + viewedAt + correlationId |
| FR-05 | Audit log | Every access logs correlationId + citizenId + timestamp |
| FR-08 | OAuth2 protected | No Bearer token → 401; invalid token → 401 problem+json |
| FR-09 | RFC 7807 error responses | `application/problem+json` with status/title/detail |
| FR-10 | RAML contract for every System API | Types and examples defined |
| FR-11 | MUnit coverage | Happy path + single-downstream fallback both green |

---

## 6. Non-Functional Requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Performance | P95 < 1s (government network), < 2s local dev |
| NFR-02 | Availability | Experience API 99.5% (POC) |
| NFR-03 | Security | No secrets in git, `${secure::...}`, OAuth2 client-credentials, no PII in logs |
| NFR-04 | Observability | Every processor has `doc:name`, correlation ID carried through Kafka headers |
| NFR-05 | Compliance | Access audit per Privacy Act 2020 |
| NFR-06 | Portability | Environment-specific properties, no hardcoded URLs |
| NFR-07 | Testability | MUnit runs in CI without real backends or Kafka |
