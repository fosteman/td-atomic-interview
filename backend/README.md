# FX Trading Backend - Phase 1: Order API

## What You're Building

A **synchronous order submission endpoint** that demonstrates the transactional outbox pattern. The key insight:

```
┌─────────────────────────────────────────────────────────────┐
│ Client submits trade via REST                               │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ Spring Boot API validates & checks permissions              │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ ONE PostgreSQL transaction:                                 │
│   1. INSERT trade (status = SUBMITTED)                      │
│   2. INSERT outbox event (published = false)                │
│   COMMIT or ROLLBACK (both or nothing)                      │
└─────────────────────────────────────────────────────────────┘
                           ↓
         Respond 201 with trade ID to client
                           ↓
     ┌─────────────────────────────────────┐
     │ Scheduled outbox publisher polls DB  │
     │ Publishes events to Solace           │
     │ Marks as published = true            │
     │ (happens in background, async)       │
     └─────────────────────────────────────┘
```

**Why?** If the API crashes between receiving the order and publishing to Solace, the event is still durably stored and will be published on recovery. The client gets a fast 201 response without waiting for Solace.

---

## Architecture

### Entities

#### `Trade`
- Represents a submitted FX trade
- Statuses: SUBMITTED → ACCEPTED → ROUTED → PARTIALLY_FILLED → FILLED (or REJECTED)
- Has `@Version` for optimistic locking (prevents lost updates in concurrent scenarios)
- Unique constraint on (accountId, clientOrderId) for idempotency

#### `OutboxEvent`
- Event sourcing table: each business event is immutably stored here
- Polled by a scheduled publisher; marked as published only after Solace ACKs
- Guarantees durability: if Solace is down, events wait in the DB

#### `Position`
- Portfolio by currency
- Mock data seeded on startup; eventually updated by execution consumers

### Services

#### `TradeService`
The core business logic. **Key method: `submitTrade()`**

```java
@Transactional
public TradeResponse submitTrade(String accountId, SubmitTradeRequest request) {
    // Validate
    validateTradeRequest(request);

    // Idempotency: if clientOrderId exists, return existing trade
    if (request.clientOrderId() != null) {
        Optional<Trade> existing = tradeRepository
            .findByAccountIdAndClientOrderId(accountId, request.clientOrderId());
        if (existing.isPresent()) return TradeResponse.from(existing.get());
    }

    // Create trade
    Trade trade = new Trade(...);
    Trade savedTrade = tradeRepository.save(trade);

    // Create outbox event IN SAME TRANSACTION
    OutboxEvent event = new OutboxEvent(...);
    outboxRepository.save(event);

    // Transaction commits here: both Trade and OutboxEvent are persisted or both rollback.
    return TradeResponse.from(savedTrade);
}
```

**Why `@Transactional`?**
- All writes (trade + event) are atomic.
- If either INSERT fails, both rollback.
- If the server crashes mid-commit, PostgreSQL recovers the transaction state on restart.

#### `TradeController`
REST endpoints:
- `POST /api/trades` — submit a trade (calls `TradeService.submitTrade()`)
- `GET /api/trades/{tradeId}` — fetch a single trade
- `GET /api/trades` — list all trades for the account

---

## Key Interview Concepts Embedded Here

1. **ACID Transactions**
   - The Trade + OutboxEvent insertion is atomic at the database level.
   - If the API crashes between `save(trade)` and `save(event)`, the transaction rolls back and neither is persisted.

2. **Idempotency**
   - Unique constraint on (accountId, clientOrderId) prevents duplicate trades.
   - If the client retries with the same clientOrderId, the service returns the existing trade (idempotent response).

3. **Durability**
   - The API responds 201 immediately after the transaction commits.
   - The client doesn't wait for Solace; they get a fast response.
   - The event is safely in PostgreSQL, to be published async.

4. **Eventual Consistency**
   - React may briefly see status = SUBMITTED.
   - The asynchronous consumer will update it to ACCEPTED after validation.
   - React discovers the new status via WebSocket or polling.

5. **Version/Optimistic Locking**
   - The `@Version` field on Trade is incremented on every update.
   - If two threads try to update the same trade simultaneously, the second one throws `OptimisticLockingFailureException`.
   - Discussed in Phase 4 (resilience).

---

## Running Locally

### 1. Start Docker Services

```bash
docker-compose up -d
```

Waits for PostgreSQL to be healthy, then starts Solace.

### 2. Build & Run the API

```bash
cd backend/order-api
mvn clean install
mvn spring-boot:run
```

Or open in IDE and run `Application.main()`.

**Logs:**
```
2026-10-12 10:15:00 INFO  ... Application started in 3.4 seconds
2026-10-12 10:15:01 INFO  ... Started TradeService
Tomcat started on port(s): 8080
```

### 3. Verify Database

Connect to PostgreSQL:
```bash
psql -h localhost -U postgres -d atomicfx
```

Check the schema:
```sql
\dt+
SELECT * FROM trades;
SELECT * FROM outbox_events;
SELECT * FROM positions;
```

---

## Testing the API Manually

### Submit a trade
```bash
curl -X POST http://localhost:8080/api/trades \
  -H "Content-Type: application/json" \
  -d '{
    "pair": "EUR/USD",
    "side": "BUY",
    "notional": 1000000,
    "rate": 1.0950,
    "settlementDate": "2026-10-14",
    "clientOrderId": "client-order-1"
  }'
```

**Response (201 Created):**
```json
{
  "tradeId": "550e8400-e29b-41d4-a716-446655440000",
  "accountId": "account-123",
  "pair": "EUR/USD",
  "side": "BUY",
  "notional": 1000000,
  "rate": 1.0950,
  "status": "SUBMITTED",
  "settlementDate": "2026-10-14",
  "createdAt": "2026-10-12T10:15:00Z"
}
```

### Get a trade
```bash
curl http://localhost:8080/api/trades/550e8400-e29b-41d4-a716-446655440000
```

### List all trades
```bash
curl http://localhost:8080/api/trades
```

### Get portfolio
```bash
curl http://localhost:8080/api/positions
```

---

## What Happens Next (Phase 2: Consumers)

1. **OutboxPublisher** (scheduled task in same service)
   - Polls `SELECT * FROM outbox_events WHERE published = false` every 500ms
   - Publishes each event to Solace topic `trading/fx.trade.submitted`
   - Marks as published = true only after Solace ACKs

2. **Risk Validator Consumer** (separate Spring Boot service)
   - Consumes `trading/fx.trade.submitted`
   - Checks counterparty limits, notional size
   - Publishes `trading/fx.trade.accepted` or `trading/fx.trade.rejected`

3. **Settlement Orchestrator**
   - Consumes `trading/fx.trade.accepted`
   - Calculates settlement date (T+2, skipping weekends/holidays)
   - Publishes settlement instruction

---

## Interview Talking Points

**Q: Why not publish to Solace synchronously inside `submitTrade()`?**

A: Three reasons:
1. **Latency**: Solace call could take 100ms+. The REST response would be slow.
2. **Resilience**: If Solace is down, the trade submission fails. Instead, we accept the trade, commit it to the DB, and let the async publisher retry indefinitely.
3. **Decoupling**: The API doesn't know or care about message broker details. It just commits events to the outbox.

**Q: What if the server crashes between responding 201 and publishing to Solace?**

A: The OutboxEvent is already persisted. When the server restarts, the publisher will see unpublished events and publish them. The trade is never lost.

**Q: What if two clients submit the same clientOrderId concurrently?**

A: The unique constraint `(accountId, clientOrderId)` means the second INSERT will fail, triggering a rollback. The first client gets the trade saved; the second gets a 409 Conflict (or is handled by idempotency retry logic). In production, you'd return the existing trade to both clients.

**Q: Why `@Version` on Trade?**

A: Optimistic locking. If a consumer tries to update a trade that another consumer just updated, the version mismatch throws an exception instead of silently overwriting. Discussed in Phase 4.

---

## Next Steps for You

1. **Run the server locally**, hit the endpoints with curl, watch the logs.
2. **Inspect the database**: Check that trade and outbox_event rows are created in the same transaction.
3. **Simulate a crash**: Kill the server mid-transaction (modify the code to add a delay), restart, and verify the transaction rolled back cleanly.
4. **Write tests** (Phase 4): Unit tests for TradeService, integration tests with testcontainers for PostgreSQL.
5. **Outline the OutboxPublisher** scheduled task (Phase 2 preview).
