# Reliable Webhook Delivery Platform

A high-performance, resilient webhook delivery engine built with **Java 21**, **Spring Boot 3.3**, and **PostgreSQL**.

Designed to handle mission-critical event distribution with at-least-once delivery guarantees, HMAC payload signing, idempotent ingestion, asynchronous virtual-thread execution, and an exponential backoff retry engine.

---

## 🌟 Key Architecture & Features

- **Asynchronous Virtual Thread Dispatcher:** Leverages Java 21 Virtual Threads (`spring.threads.virtual.enabled=true`) with non-blocking native `HttpClient` for concurrent outbound webhook dispatches.
- **HMAC-SHA256 Payload Signing:** Every outgoing request is cryptographically signed using an auto-generated per-endpoint secret (`whsec_...`) and stamped with a UNIX timestamp to protect receivers against tampering and replay attacks.
- **Idempotent Ingestion:** Event ingestion requires an `Idempotency-Key` header. Duplicate keys safely return the cached response without generating redundant deliveries.
- **Exponential Backoff & Full Jitter:** Failed attempts automatically reschedule with exponential backoff (e.g., +1m, +5m, +30m, +2h) plus randomized jitter to avoid thundering-herd issues on downstream servers.
- **Dead-Letter Queue (DLQ) & Manual Replay:** Deliveries exceeding maximum retry thresholds transition to `DEAD_LETTER` state and can be re-triggered on demand via `POST /api/v1/deliveries/{id}/replay`.
- **SSRF Protection:** Built-in validation blocks registration of internal/private IP targets (RFC 1918 ranges, loopback, and cloud metadata `169.254.169.254`).
- **Comprehensive Audit Trail:** Every HTTP attempt captures response codes, latency (ms), timestamp, and error descriptions.
- **Built-in Mock Receiver:** Includes embedded test endpoints (`/api/v1/test-receiver/webhook`, `/flaky`, `/always-fail`) to test success and failure recoveries without external tools.
- **OpenAPI & Swagger UI:** Interactive documentation available at `/swagger-ui.html`.

---

## 🏗️ Architecture Flow

```mermaid
flowchart TD
    Client["Client Application"] -->|1. POST /api/v1/events (Idempotency Key)| API["Event Ingestion Controller"]
    API -->|2. Deduplicate & Store| DB[("PostgreSQL\n(Events, Deliveries, Endpoints)")]
    API -->|3. Enqueue Dispatch| Worker["Virtual Thread Worker"]
    
    Worker -->|4. Sign Payload| Signer["HMAC-SHA256 Signer"]
    Worker -->|5. SSRF Validation| SSRF["SSRF Guard"]
    SSRF -->|6. POST with X-Webhook-Signature| Receiver["Receiver Server"]
    
    Receiver -->|2xx OK| Success["Mark Delivery SUCCESS"]
    Receiver -->|4xx / 5xx / Timeout| RetryEngine["Exponential Backoff Engine"]
    
    RetryEngine -->|Attempt < Max| Schedule["Schedule next_retry_at (1m, 5m, 30m, 2h)"]
    RetryEngine -->|Attempt >= Max| DLQ["Transition to DEAD_LETTER (DLQ)"]
    
    Admin["Admin / Developer"] -->|POST /api/v1/deliveries/{id}/replay| Worker
```

---

## 🚀 Quick Start

### Prerequisites
* **Java 21** or higher
* **Docker & Docker Compose** (for PostgreSQL)

### 1. Start the Database
```powershell
docker compose up -d postgres
```

### 2. Run the Application
```powershell
.\mvnw.cmd spring-boot:run
```
The application will start on `http://localhost:8080`.

Interactive Swagger UI documentation is available at:
👉 **`http://localhost:8080/swagger-ui.html`**

---

## 🧪 Interactive Walkthrough (Step-by-Step)

Default API Key for management operations: `wh_live_adminkey_2026_secret`

### 1. Register a Webhook Endpoint
Target our built-in test receiver (`/api/v1/test-receiver/webhook`):

```powershell
curl.exe -X POST "http://localhost:8080/api/v1/endpoints" `
  -H "X-API-Key: wh_live_adminkey_2026_secret" `
  -H "Content-Type: application/json" `
  -d '{
    "url": "http://localhost:8080/api/v1/test-receiver/webhook",
    "description": "Order processing webhook",
    "subscribedEvents": "order.created,order.updated"
  }'
```
*Response will return an auto-generated HMAC secret (e.g. `whsec_dGVzdHNlY3Jld...`).*

---

### 2. Publish an Event (with Idempotency Key)
```powershell
curl.exe -X POST "http://localhost:8080/api/v1/events" `
  -H "X-API-Key: wh_live_adminkey_2026_secret" `
  -H "Idempotency-Key: evt_order_1001" `
  -H "Content-Type: application/json" `
  -d '{
    "eventType": "order.created",
    "payload": "{\"orderId\": \"ord_1001\", \"amount\": 149.99, \"currency\": \"USD\"}"
  }'
```

---

### 3. Verify Idempotency Deduplication
Run the exact same command again with the same `Idempotency-Key: evt_order_1001`:
```powershell
curl.exe -X POST "http://localhost:8080/api/v1/events" `
  -H "X-API-Key: wh_live_adminkey_2026_secret" `
  -H "Idempotency-Key: evt_order_1001" `
  -H "Content-Type: application/json" `
  -d '{
    "eventType": "order.created",
    "payload": "{\"orderId\": \"ord_1001\", \"amount\": 149.99, \"currency\": \"USD\"}"
  }'
```
*Notice that `"duplicate": true` is returned and no redundant deliveries are created.*

---

### 4. Inspect Captured Requests on Receiver
See the exact headers received by the receiver, including HMAC signature:
```powershell
curl.exe -X GET "http://localhost:8080/api/v1/test-receiver/requests"
```

Headers sent with every webhook:
* `X-Webhook-Id`: Unique delivery identifier
* `X-Webhook-Event`: The subscribed event name
* `X-Webhook-Timestamp`: UNIX timestamp of generation
* `X-Webhook-Signature`: `t=1700000000,v1=abcdef...` (HMAC-SHA256 hex digest)

---

### 5. Inspect Delivery History and Attempt Logs
```powershell
# List deliveries
curl.exe -X GET "http://localhost:8080/api/v1/deliveries" -H "X-API-Key: wh_live_adminkey_2026_secret"

# View specific attempt audit trail
curl.exe -X GET "http://localhost:8080/api/v1/deliveries/<DELIVERY_ID>/attempts" -H "X-API-Key: wh_live_adminkey_2026_secret"
```

---

### 6. Test Flaky Downstream & Retries
Register an endpoint pointing to the flaky simulator:
```powershell
curl.exe -X POST "http://localhost:8080/api/v1/endpoints" `
  -H "X-API-Key: wh_live_adminkey_2026_secret" `
  -H "Content-Type: application/json" `
  -d '{
    "url": "http://localhost:8080/api/v1/test-receiver/flaky",
    "description": "Flaky service endpoint",
    "subscribedEvents": "flaky.test"
  }'
```
Publish an event:
```powershell
curl.exe -X POST "http://localhost:8080/api/v1/events" `
  -H "X-API-Key: wh_live_adminkey_2026_secret" `
  -H "Idempotency-Key: flaky_001" `
  -H "Content-Type: application/json" `
  -d '{
    "eventType": "flaky.test",
    "payload": "{\"message\": \"testing retries\"}"
  }'
```
Inspect attempts: the delivery will record 500 responses on initial tries, schedule exponential retries, and recover cleanly to `SUCCESS` once the 3rd attempt passes!

---

### 7. Dead-Letter Queue (DLQ) & Manual Replay
If a delivery fails all max retry attempts, its status becomes `DEAD_LETTER`.
You can manually replay it at any time:
```powershell
curl.exe -X POST "http://localhost:8080/api/v1/deliveries/<DELIVERY_ID>/replay" `
  -H "X-API-Key: wh_live_adminkey_2026_secret"
```

---

## 🧪 Running Tests

```powershell
.\mvnw.cmd test
```

## 📜 License
MIT License.

