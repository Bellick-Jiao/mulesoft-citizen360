# Citizen360 — API-Led Mule 4 Integration

A reference Mule 4 project that integrates three independent government systems
(identity, case management, licensing) into a unified citizen 360 view using the
**API-led connectivity** architecture pattern.

## Architecture

```
Staff Portal / Mobile App
        │
        ▼
┌─────────────────────────────────┐
│  Experience Layer               │
│  citizen-360-api (port 8081)     │
│  OAuth2, audit, thin facade    │
└──────────────┬──────────────────┘
               │
        ┌──────▼──────┐
        │ Process Layer
        │ citizen-360-process-api (port 8084)
        │ Scatter-Gather + DataWeave aggregation
        │ On Error Continue fallback
        │ Async Kafka event publish
        └──┬───────┬───────┬────────┘
           ▼       ▼       ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│ System    │ │ System    │ │ System    │
│ Identity  │ │ Case      │ │ License   │
│ API 8082  │ │ API 8083  │ │ API 8085  │
└────┬──────┘ └────┬─────┘ └────┬─────┘
     ▼             ▼             ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│ DIA      │ │ Legacy   │ │ MBIE     │
│ RealMe   │ │ Case Mgmt│ │ Licensing│
│ mock 9082│ │ mock 9083│ │ mock 9085│
└──────────┘ └──────────┘ └──────────┘

Kafka (citizen.events topic, port 9092)
Kafka UI: http://localhost:8090
```

## Ports

| Service | Port | Description |
|---|---|---|
| citizen-360-api (Experience) | 8081 | Public-facing facade |
| system-identity-api (System) | 8082 | Wraps DIA RealMe |
| system-case-api (System) | 8083 | Wraps legacy Case Mgmt |
| citizen-360-process-api (Process) | 8084 | Aggregation + orchestration |
| system-license-api (System) | 8085 | Wraps MBIE Licensing |
| external-dia-realme (mock) | 9082 | Spring Boot DIA stub |
| external-case-mgmt (mock) | 9083 | Spring Boot case system stub |
| external-mbie-licensing (mock) | 9085 | Spring Boot licensing stub |
| Kafka broker | 9092 | KRaft mode |
| Kafka UI | 8090 | http://localhost:8090 |

## Prerequisites

- Anypoint Studio 7.x with Mule 4.4+ runtime
- Docker Desktop (for Kafka + Kafka UI)
- Java 17 (for Spring Boot mocks)
- A Java IDE (IntelliJ IDEA recommended) for running the Spring Boot mocks

## Quick Start

### 1. Start Kafka

```powershell
cd D:\workspace\doubao\mulesoft-poc
docker compose -f infra/docker-compose.yml up -d
```

Verify Kafka UI is running: http://localhost:8090

### 2. Start Spring Boot mock services

Run these three apps from IntelliJ IDEA (one per run configuration):

| Service | Directory | Port |
|---|---|---|
| external-dia-realme | `external-services/external-dia-realme` | 9082 |
| external-case-mgmt | `external-services/external-case-mgmt` | 9083 |
| external-mbie-licensing | `external-services/external-mbie-licensing` | 9085 |

### 3. Start Mule applications

Import the five projects from `mule-apps/` into Anypoint Studio
(File → Import → Anypoint Studio project from File System) and run in order:

1. system-identity-api-impl (8082)
2. system-case-api-impl (8083)
3. system-license-api-impl (8085)
4. citizen-360-process-api-impl (8084)
5. citizen-360-api-impl (8081)

### 4. Test the API

```bash
# Direct Process API call
curl http://localhost:8084/api/citizens/RN-12345/aggregate

# Through Experience API
curl http://localhost:8081/api/citizens/RN-12345/360
```

### 5. Check Kafka events

Open http://localhost:8090 → Topics → citizen.events → Messages
You should see a `CitizenViewed` event published asynchronously.

## Test Data

| RN Number | Name | City | Risk | Cases | Licenses |
|---|---|---|---|---|---|
| RN-12345 | Jinyu Liang | Christchurch | LOW | 2 | 2 |
| RN-22222 | Maria Garcia | Wellington | MEDIUM | 1 | 1 |
| RN-33333 | Hiro Tanaka | Auckland | HIGH | 0 | 0 |

## Key Patterns Demonstrated

- **API-led connectivity**: Experience / Process / System layers
- **Scatter-Gather**: parallel fan-out to three System APIs
- **DataWeave 2.0**: response aggregation and transformation
- **On Error Continue**: per-route fallback when a downstream system is unavailable
- **Async scope**: non-blocking Kafka event publishing
- **MUnit**: unit testing with mocked HTTP dependencies
- **RAML 1.0**: API contract-first design
- **Kafka KRaft**: event streaming without ZooKeeper

## Project Structure

```
mulesoft-citizen360/
├── mule-apps/                       # Mule 4 applications
│   ├── citizen-360-api-impl/        # Experience layer (port 8081)
│   ├── citizen-360-process-api-impl/ # Process layer (port 8084)
│   ├── system-identity-api-impl/    # System: RealMe (port 8082)
│   ├── system-case-api-impl/        # System: Cases (port 8083)
│   └── system-license-api-impl/     # System: Licensing (port 8085)
├── external-services/               # Spring Boot mock backends
│   ├── external-dia-realme/         # DIA mock (port 9082)
│   ├── external-case-mgmt/          # Case Mgmt mock (port 9083)
│   └── external-mbie-licensing/     # MBIE mock (port 9085)
├── docs/
│   ├── 01-requirements.md
│   ├── 02-as-is.md
│   ├── 03-to-be.md
│   ├── 04-api-contracts.md
│   ├── business-process.drawio
│   └── contracts/                   # RAML 1.0 API definitions
├── infra/
│   ├── docker-compose.yml           # Kafka KRaft + Kafka UI
│   └── cloudhub.yaml                # CloudHub 2.0 deployment descriptor
├── .github/workflows/
│   └── mule-pipeline.yml            # GitHub Actions CI
└── pom.xml                          # Parent Maven POM
```
