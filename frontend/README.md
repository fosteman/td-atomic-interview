# FX Trading UI

Minimal React FX trading interface. No frameworks, just React + TypeScript + Vite.

## Setup

```bash
npm install
npm run dev
```

Runs on `http://localhost:5173`, proxies `/api/*` to `http://localhost:8080`.

## Components

- **RateDisplay**: Shows bid/ask for currency pairs
- **TradeForm**: Submit BUY/SELL trades with notional and settlement date
- **TradeList**: History of submitted trades with live status updates
- **Portfolio**: Multi-currency position summary

## WebSocket

Listens on `/ws/trades` for:
- `trade.update`: Order status changes (SUBMITTED → ACCEPTED → SETTLED)
- `rate.update`: Live rate streaming

If disconnected, falls back to polling `/api/trades` every 10 seconds.

## Features

- Submit trades (POST `/api/trades`)
- View portfolio (GET `/api/positions`)
- Real-time updates via WebSocket
- Graceful reconnection
- Pure HTML + monospace styling
