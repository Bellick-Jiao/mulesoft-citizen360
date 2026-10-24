# 02 — As-Is Architecture

> Agency: Ministry of Citizen Services (MCS)
> Phase: Discovery / Current State Analysis

---

## 1. System Inventory

| System | Purpose | Tech Stack | Owner | Interface | Can Modify? |
|---|---|---|---|---|---|
| **RealMe Identity** | Citizen identity verification | Government cloud SaaS, OAuth2 | DIA (external agency) | REST API with sandbox | No — consume as-is |
| **Case Management (CMS)** | Benefits applications, case status | Java 6 monolith (2012), on-prem VMware | MCS, vendor departed | One coarse HTTP endpoint, no versioning | No — wrap only |
| **Permits & Licensing** | Licences held by citizens | REST API | Another agency (MBIE-like) | REST, cross-agency IAM approval | No |
| **Data Warehouse** | Reporting, BI | Oracle Exadata, nightly ETL | MCS data team | Batch files only | Separate project for real-time |
| **Staff terminal** | Service centre workstation | Windows 10 + browser tabs | MCS | None | Frontend can be replaced |

---

## 2. Current State Diagram

```
                     Service Centre Staff
                              │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
        ┌─────────┐    ┌──────────┐   ┌────────────┐
        │ RealMe  │    │   CMS    │   │ Licensing  │
        │ Portal  │    │ Terminal │   │   Portal   │
        └────┬────┘    └────┬─────┘   └─────┬──────┘
             │              │                │
             │  Manual copy/paste, staff      │
             │  mentally aggregates (~8 min)  │
             ▼              ▼                ▼
        ┌──────────────────────────────────────────┐
        │  Staff's memory / paper / Excel           │
        └──────────────────────────────────────────┘

Data warehouse path:
   RealMe ──ETL batch file──┐
   CMS    ──ETL batch file──┼──► Oracle DW (T+1)
   Licensing ──ETL──────────┘
   (Nightly, independent, no shared ID mapping)
```

---

## 3. Current Flows

### Flow A: Citizen lookup (real-time but manual)

1. Staff opens RealMe Portal → enters citizen ID → sees identity
2. Staff switches to CMS Terminal → re-enters same ID → sees cases
3. Staff switches to Licensing Portal → re-enters ID → sees licences
4. Staff mentally assembles and tells the citizen what's active

**Problems:**
- Three logins, three sets of credentials
- Different ID formats (RealMe `RN-xxxx`, CMS numeric ID, Licensing number)
- No record of who accessed what
- Easy to misread rows across systems

### Flow B: Data warehouse sync (batch, T+1)

1. Nightly ETL exports CSV from each system
2. DW loads at 6am; BI reports show yesterday's data only
3. No shared citizen ID mapping across feeds
4. A failed ETL silently leaves a gap

---

## 4. Pain Point Heat Map

| Pain Point | Location | Why It Hurts |
|---|---|---|
| **P1** No unified citizen view | Flow A: three manual UIs | ~8 min per citizen, throughput bottleneck |
| **P2** No orchestration layer | Direct staff-terminal connections to external systems | Every new channel rebuilds cross-agency auth |
| **P3** T+1 data sync | Flow B: nightly batch ETL | Audit and risk blind to same-day changes |
| **P4** CMS has no SLA | Flow A: CMS terminal is a single point of failure | Vendor gone, cannot fix |
| **P5** Credentials scattered | Flow A: staff hold 3 credential sets | Fails Privacy Act audit |
| **P6** No access audit | Flow A: no access record | Cannot answer privacy complaints |

---

## 5. Constraints

| # | Constraint | Impact |
|---|---|---|
| C1 | RealMe belongs to DIA, MCS cannot modify it | Wrap only, no API changes |
| C2 | CMS vendor departed, code unmaintainable | Wrap in a System API; do not modify CMS |
| C3 | Licensing API requires cross-agency IAM approval | Mock in POC; production requires formal approval |
| C4 | Citizen PII must not appear in logs | Use correlation IDs, field-level redaction |
| C5 | Anypoint Platform is the mandated integration platform | Mule is the prescribed ESB |
| C6 | Deployment must be on government cloud | Docker for local; CloudHub/GovCloud for production |

---

## 6. Key Takeaway

> The problem is not too few systems — it's the absence of a layer that **assembles
> those systems from a citizen's perspective**.
>
> Manual assembly is slow, error-prone, and unauditable. Batch ETL is T+1.
>
> The solution is an **API orchestration layer**:
> - For staff (Experience): one unified citizen view
> - For systems (System): each system wrapped as a reusable API
> - For data (Event): real-time changes to Kafka, replacing nightly ETL
>
> This is exactly what MuleSoft Anypoint is designed for.
