# Settlement Service

A Spring Boot microservice that handles FX trade settlement workflow.

## What It Does

Settlement is the actual exchange of currency between parties. After a trade is executed, it must be settled on the agreed settlement date (usually T+2 for FX).

```
Order API (publishes)
  ↓
  trading/fx/trade/submitted event
  ↓
Settlement Service (consumes) ← THIS SERVICE
  ↓
  1. Create settlement record
  2. Calculate settlement amount & routing
  3. Send instruction to nostro/vostro bank
  4. On settlement date: mark as SETTLED
  ↓
  trading/fx/settlement/settled event (published back)
```

## Settlement Workflow

```
PENDING
  ↓ (instruction sent to bank)
INSTRUCTED
  ↓ (settlement date reached, currency exchanged)
SETTLED (or FAILED)
```

## API Endpoints

- `GET /api/settlements/{tradeId}` — Get settlement for a trade
- `POST /api/settlements/{settlementId}/instruct` — Mark as instructed
- `POST /api/settlements/{settlementId}/settle` — Mark as settled

## Key Concepts

### Event-Driven
This service doesn't call the Order API. Instead, it **subscribes to events** published by it.

### Idempotency
If the same trade.submitted event is received twice, we check the database and return the existing settlement (no duplicate).

### Eventual Consistency
- Order API sees trade status immediately
- Settlement service processes asynchronously
- React polls for settlement status via REST

### Nostro/Vostro Routing
- **Nostro**: Our bank's account at a foreign bank (we hold foreign currency there)
- **Vostro**: Foreign bank's account at our bank (they hold our currency)
- Settlement routes currency through these accounts based on the currency pair and trade side

## Running Locally

```bash
# Build
mvn clean install

# Run (PostgreSQL must be up)
mvn spring-boot:run
```

Runs on port 8081.

## Data Isolation

This service has its **own PostgreSQL database connection** (same DB, separate tables).
It doesn't depend on Order API's tables. Coupling is through **events only**.

## Testing the Flow

1. **Submit a trade** (Order API):
   ```bash
   curl -X POST http://localhost:8080/api/trades \
     -H "Content-Type: application/json" \
     -d '{"pair":"EUR/USD","side":"BUY","notional":1000000,"rate":1.0950,"settlementDate":"2026-10-14"}'
   ```

2. **Manually trigger settlement creation** (since we don't have real Solace listener yet):
   ```bash
   # Simulate the trade.submitted event being received
   # Create a settlement via Spring integration test or manual SQL
   ```

3. **Check settlement status**:
   ```bash
   curl http://localhost:8081/api/settlements/{tradeId}
   ```

4. **Progress through settlement states**:
   ```bash
   curl -X POST http://localhost:8081/api/settlements/{settlementId}/instruct
   curl -X POST http://localhost:8081/api/settlements/{settlementId}/settle
   ```

## Interview Talking Points

**Q: Why does Settlement Service have its own database?**

A: Microservices are independently deployable. If Settlement Service fails, Order API still works. They share a database conceptually but are logically separate—Settlement Service only reads trade events, not the trades table directly.

**Q: How does Settlement Service know when a trade is submitted?**

A: Through the event published to Solace by Order API's OutboxPublisher. It's async and decoupled. In production, Settlement Service would be a proper JMS consumer listening to `trading/fx/trade/submitted`.

**Q: What if Settlement Service is offline when a trade comes in?**

A: Solace durably queues the event. When Settlement Service comes back up, it consumes all pending events and recreates settlements for them. The event is the source of truth, not the database.

**Q: How do you handle duplicate settlements?**

A: Check `settlements.trade_id` for uniqueness. If the same trade.submitted event is redelivered, the `SELECT * FROM settlements WHERE trade_id = ?` returns existing settlement.

## Next Steps

- Implement real JMS listener (consume Solace events)
- Publish settlement.settled events to outbox
- Add bank confirmation webhook
- Reconciliation against bank statement
