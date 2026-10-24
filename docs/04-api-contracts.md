# 04 — API Contracts

> Agency: Ministry of Citizen Services (MCS)
> Phase: Contract-First Design

---

## 1. Conventions

| Convention | Example |
|---|---|
| Citizen ID | `RN-xxxxx` (RealMe format, string) |
| Date | ISO-8601 `YYYY-MM-DD` (RAML `date-only`) |
| Timestamp | ISO-8601 with timezone `2026-10-22T12:00:00Z` |
| Enum values | UPPER_SNAKE_CASE (`ACTIVE`, `IN_PROGRESS`) |
| Error response | RFC 7807 `application/problem+json` |
| All time fields | UTC |

---

## 2. Experience API (citizen-360-api, port 8081)

OAuth2 protected. For service centre staff and future citizen portal.

### `GET /citizens/{citizenId}/360`

**Request:**
```http
GET /citizens/RN-12345/360 HTTP/1.1
Authorization: Bearer <staff-token>
Accept: application/json
```

**200 Response:**
```json
{
  "citizenId": "RN-12345",
  "identity": {
    "firstName": "Jinyu",
    "lastName": "Liang",
    "dateOfBirth": "1983-05-12",
    "address": {
      "street": "330 Cashel Street",
      "city": "Christchurch",
      "postcode": "8011"
    },
    "identityVerified": true,
    "riskBand": "LOW"
  },
  "cases": [
    {
      "caseId": "CASE-2026-001",
      "type": "BENEFIT",
      "status": "IN_PROGRESS",
      "openedDate": "2026-09-15",
      "officer": "J. Smith"
    }
  ],
  "licenses": [
    {
      "licenseId": "LIC-44556",
      "type": "DRIVING",
      "status": "ACTIVE",
      "expiryDate": "2027-03-01"
    }
  ],
  "asOf": "2026-10-22T12:00:00Z"
}
```

### Field Definitions

| Field | Type | Required | Description |
|---|---|---|---|
| `citizenId` | string | yes | RealMe citizen number |
| `identity.firstName` | string | yes | |
| `identity.lastName` | string | yes | |
| `identity.dateOfBirth` | date | yes | |
| `identity.address.street` | string | yes | |
| `identity.address.city` | string | yes | |
| `identity.address.postcode` | string | yes | NZ postcode, 4 digits |
| `identity.identityVerified` | boolean | yes | RealMe verification status |
| `identity.riskBand` | enum | yes | `LOW` / `MEDIUM` / `HIGH` |
| `cases[]` | array | yes | May be empty |
| `cases[].caseId` | string | yes | |
| `cases[].type` | enum | yes | `BENEFIT` / `HOUSING` / `DISABILITY` |
| `cases[].status` | enum | yes | `NEW` / `IN_PROGRESS` / `DECIDED` / `CLOSED` |
| `cases[].openedDate` | date | yes | |
| `cases[].officer` | string | no | Assigned officer |
| `licenses[]` | array | yes | May be empty |
| `licenses[].licenseId` | string | yes | |
| `licenses[].type` | enum | yes | `DRIVING` / `FIREARM` / `ALCOHOL` |
| `licenses[].status` | enum | yes | `ACTIVE` / `EXPIRED` / `SUSPENDED` |
| `licenses[].expiryDate` | date | yes | |
| `asOf` | datetime | yes | Aggregation timestamp |

---

## 3. Process API (citizen-360-process-api, port 8084)

Internal mesh call only. Encapsulates the citizen view business rules.

### `GET /citizens/{citizenId}/aggregate`

Same response structure as Experience API, without channel wrapper.

---

## 4. System APIs

### 4.1 Identity System API (port 8082)

Wraps RealMe. Mocked in POC, connects to DIA in production.

#### `GET /identity/citizens/{citizenId}`

```json
{
  "citizenId": "RN-12345",
  "firstName": "Jinyu",
  "lastName": "Liang",
  "dateOfBirth": "1983-05-12",
  "address": {
    "street": "330 Cashel Street",
    "city": "Christchurch",
    "postcode": "8011"
  },
  "identityVerified": true,
  "riskBand": "LOW"
}
```

### 4.2 Case System API (port 8083, Spring Boot mock)

Simulates legacy Case Management.

#### `GET /cases?citizenId={citizenId}`

```json
{
  "cases": [
    {
      "caseId": "CASE-2026-001",
      "citizenId": "RN-12345",
      "type": "BENEFIT",
      "status": "IN_PROGRESS",
      "openedDate": "2026-09-15",
      "officer": "J. Smith",
      "lastModifiedDate": "2026-10-20T08:30:00Z"
    }
  ]
}
```

`lastModifiedDate` is used for batch incremental sync and is not exposed to Experience.

### 4.3 License System API (port 8085)

Wraps cross-agency Licensing API.

#### `GET /licenses?citizenId={citizenId}`

```json
{
  "licenses": [
    {
      "licenseId": "LIC-44556",
      "citizenId": "RN-12345",
      "type": "DRIVING",
      "status": "ACTIVE",
      "expiryDate": "2027-03-01"
    }
  ]
}
```

---

## 5. Kafka Events (citizen.events)

JSON format for POC. Production would use Avro + Schema Registry.

### Message Schema

```json
{
  "eventType": "CitizenViewed",
  "citizenId": "RN-12345",
  "timestamp": "2026-10-22T12:00:00Z",
  "correlationId": "abc-123-def",
  "source": "citizen-360-process-api"
}
```

| Field | Type | Description |
|---|---|---|
| `eventType` | enum | `CitizenViewed` / `CaseUpdated` / `CitizenSynced` |
| `citizenId` | string | Associated citizen |
| `timestamp` | datetime | Event time |
| `correlationId` | string | Request trace ID for audit |
| `source` | string | Producing service |

**Kafka key**: `citizen-{citizenId}` — ensures same-citizen events land on the same partition for ordering.

---

## 6. Error Responses (RFC 7807)

All APIs return `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Citizen not found",
  "status": 404,
  "detail": "No identity record for citizen RN-99999",
  "instance": "/citizens/RN-99999/360"
}
```

### Error Code Table

| HTTP | Title | When |
|---|---|---|
| 400 | Bad Request | Invalid citizenId format |
| 401 | Unauthorized | Missing or expired Bearer token |
| 403 | Forbidden | Token valid but no permission for this citizen |
| 404 | Citizen Not Found | Not found in any System API |
| 500 | Internal Error | Unexpected error, detail includes correlationId |
| 503 | Downstream Degraded | Two or more downstreams unavailable |

---

## 7. Fallback Rules (Process Layer)

| Failed Downstream | Response |
|---|---|
| Identity | `identity: { "status": "UNAVAILABLE" }`, cases and licenses still returned |
| Cases | `cases: []`, identity and licenses still returned |
| Licenses | `licenses: []`, identity and cases still returned |
| Two or more downstreams | 503 problem+json |
