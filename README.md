# AN FX - Settlement System

in lieu of my interview with td team, let me do some prep work and understand the tech stack. 

so i built a distributed FX (foreign exchange) trading and settlement system built with Spring Boot 3.x, PostgreSQL, and Solace JMS messaging. not without use of Ai, indeed, a great learning tool. 

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Order API (8080)                         │
│  - Accepts trade requests (POST /api/trades)                   │
│  - Creates Trade records in PostgreSQL                         │
│  - Publishes TradeSubmittedEvent to Solace queue              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   Solace Message Queue                          │
│           (trading.fx.trade.submitted @ localhost:55554)        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  Settlement Service (8081)                      │
│  - Consumes TradeSubmittedEvent from Solace queue             │
│  - Creates Settlement records (with 5-second delay)            │
│  - Updates Trade status to SETTLED in PostgreSQL              │
│  - Provides settlement query APIs (GET /api/settlements)       │
└─────────────────────────────────────────────────────────────────┘
```

## Key Features

- **Transactional Outbox Pattern**: Order API publishes events to Solace via JCSMP (native Solace Java API)
- **Queue-Based Consumption**: Settlement Service automatically reads messages from queue on startup
- **Idempotent Processing**: Duplicate messages are safely ignored
- **Settlement Delay**: 5-second timeout before settlements are finalized (allows for validation)
- **Dual Status Updates**: Both `settlements` and `trades` tables updated atomically
- **Auto-Settlement**: Settlements ≤ $5000 are auto-settled; larger amounts require manual approval

## Prerequisites

- **Java 17+**
- **PostgreSQL 12+** (running on `localhost:5432`)
- **Solace PubSub+ Broker** (running on `localhost:55554` with SMF protocol)
- **Maven 3.8+**

## Database Setup

PostgreSQL must be running with database `fx` and user `postgres:password`.

```bash
createdb fx
```

Database tables are auto-created by Hibernate on first run (see `application.yml` configs).

## Services

### 1. Order API (`backend/order-api`)

Handles trade creation and publishes events to Solace.

**Start:**
```bash
cd backend/order-api
mvn spring-boot:run
```

**API Endpoints:**

- `POST /api/trades` - Create a new trade
  ```json
  {
    "pair": "EUR/USD",
    "side": "BUY",
    "notional": 1000000,
    "rate": 1.08,
    "accountId": "account-123"
  }
  ```

- `GET /api/trades/{tradeId}` - Get trade details
- `GET /api/trades` - List all trades

**Event Publishing:**
- Implements transactional outbox pattern via `OutboxPublisher`
- Polls database every 1 second for unpublished events
- Publishes `TradeSubmittedEvent` to Solace queue `trading.fx.trade.submitted`

### 2. Settlement Service (`backend/settlement-service`)

Consumes trade events from Solace and creates settlements.

**Start:**
```bash
cd backend/settlement-service
mvn spring-boot:run
```

**Key Features:**
- Connects to Solace on startup
- Automatically reads all queued messages (both historical and new)
- Creates Settlement records with 5-second processing delay
- Updates Trade status in `trades` table

**API Endpoints:**

- `GET /api/settlements/{tradeId}` - Get settlement for a trade
- `GET /api/settlements` - List all settlements
- `POST /api/settlements/{settlementId}/instruct` - Mark as instructed
- `POST /api/settlements/{settlementId}/settle` - Manually mark as settled

**Message Flow:**
1. `SolaceListener` binds to queue via `JCSMPSession.createFlow()`
2. `onReceive()` callback fires for each message
3. `SettlementService.processTradeSubmitted()` creates Settlement
4. `publishSettlementInstructed()` waits 5 seconds, then updates both tables

## Configuration

### Order API (`backend/order-api/src/main/resources/application.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fx
    username: postgres
    password: password
  jpa:
    hibernate:
      ddl-auto: update
server:
  port: 8080
```

### Settlement Service (`backend/settlement-service/src/main/resources/application.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fx
    username: postgres
    password: password
  jpa:
    hibernate:
      ddl-auto: validate
server:
  port: 8081
```

### Solace Connection
Both services connect to Solace at `localhost:55554` with credentials `admin:admin` on VPN `default`.

## Database Schema

### `trades` table
```
- trade_id (PK, UUID)
- account_id
- pair (e.g., EUR/USD)
- side (BUY/SELL)
- notional (amount)
- rate (FX rate)
- status (SUBMITTED, ACCEPTED, ROUTED, PARTIALLY_FILLED, FILLED, REJECTED, SETTLED)
- settlement_date
- created_at
- version (optimistic locking)
```

### `settlements` table
```
- settlement_id (PK, UUID)
- trade_id (FK to trades, unique)
- account_id
- pair
- side
- notional
- settlement_amount (calculated: notional * rate)
- status (PENDING, INSTRUCTED, SETTLED, FAILED)
- nostro_account (routing)
- created_at
- settled_at
```

### `outbox_events` table
```
- event_id (PK, UUID)
- event_type (queue name, e.g., "trading.fx.trade.submitted")
- payload (JSON event data)
- published (boolean, default false)
- created_at
```

## Event Model

### TradeSubmittedEvent
Published by Order API when a new trade is created.

```json
{
  "tradeId": "uuid",
  "accountId": "account-123",
  "pair": "EUR/USD",
  "side": "BUY",
  "notional": 1000000,
  "rate": 1.08,
  "settlementDate": "2026-10-14",
  "createdAt": "2026-10-12T14:36:34.162521Z"
}
```

## Settlement Workflow

1. **Trade Created** (Order API)
   - `POST /api/trades` → Trade created with status `SUBMITTED`
   - `OutboxPublisher` polls DB, finds unpublished event
   - Publishes to Solace queue `trading.fx.trade.submitted`

2. **Settlement Service Consumes**
   - On startup, `SolaceListener` binds to queue
   - Reads all historical messages immediately
   - New messages trigger `onReceive()` callback

3. **Settlement Created**
   - `processTradeSubmitted()` creates Settlement record
   - Initial status: `PENDING`
   - Nostro account routing determined by currency pair

4. **Delay & Finalization** (5 second wait)
   - `publishSettlementInstructed()` waits 5 seconds
   - Updates Settlement: `PENDING` → `SETTLED`
   - Updates Trade: `SUBMITTED` → `SETTLED`

5. **Auto-Settlement Logic**
   - Amount ≤ $5000: Auto-settled (marked SETTLED)
   - Amount > $5000: Requires manual approval (stays INSTRUCTED)

## Idempotency

Both services handle duplicate messages gracefully:
- **Order API**: `OutboxPublisher` marks events as published after sending (won't re-send)
- **Settlement Service**: Checks if settlement exists for trade before creating (won't duplicate)

## Development

### Running Tests
```bash
cd backend/order-api
mvn test

cd backend/settlement-service
mvn test
```

### Restarting Services
```bash
# Kill all Java processes
pkill -9 java

# Start Order API
cd backend/order-api && mvn spring-boot:run &

# Start Settlement Service (in new terminal)
cd backend/settlement-service && mvn spring-boot:run &
```

### Clearing Data
```bash
PGPASSWORD=password psql -U postgres -d fx -h localhost << EOF
DELETE FROM settlements;
DELETE FROM trades;
DELETE FROM outbox_events;
EOF
```

## Technologies

- **Java 17** - Language
- **Spring Boot 3.2.0** - Framework
- **Spring Data JPA** - ORM
- **PostgreSQL 12+** - Database
- **Solace JCSMP 10.18.0** - Message broker (native API)
- **Tomcat 10.1** - Web server
- **Maven 3.8+** - Build tool

## Key Implementation Details

### JCSMP (Solace Native API)
- Avoids Spring JMS abstraction layer (javax.jms vs jakarta.jms conflicts)
- Uses `JCSMPSession.createFlow()` for queue binding
- Push-based consumption via `XMLMessageListener` callbacks
- Auto-acknowledgment of messages

### Transaction Management
- `@Transactional` on service methods ensures atomicity
- Settlement creation and trade status update in single transaction
- Outbox polling uses transaction-per-batch pattern

### Error Handling
- Detailed logging with emojis for visibility
- Graceful degradation on missing trades
- Exception logging includes full stack trace
- Continues processing other messages on failure

## Monitoring

Check logs for key messages:
```
# Settlement Service startup
✅ Connected and listening to queue: trading.fx.trade.submitted

# Message consumption
📨 Received from queue: {...}
⏳ Waiting 5 seconds before settlement finalization...

# Settlement updates
✅ Trade marked as SETTLED: tradeId=...
✅ Settlement marked as SETTLED: settlementId=...
```
