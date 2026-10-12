const API_BASE = '/api'

export interface Trade {
  tradeId: string
  accountId: string
  pair: string
  side: 'BUY' | 'SELL'
  notional: number
  rate: number
  status: 'SUBMITTED' | 'ACCEPTED' | 'SETTLED' | 'REJECTED'
  createdAt: string
  settlementDate: string
}

export interface Position {
  currency: string
  balance: number
}

export interface Rate {
  pair: string
  bid: number
  ask: number
  timestamp: number
}

export async function submitTrade(
  pair: string,
  side: 'BUY' | 'SELL',
  notional: number,
  rate: number,
  settlementDate: string
): Promise<Trade> {
  const res = await fetch(`${API_BASE}/trades`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      pair,
      side,
      notional,
      rate,
      settlementDate,
    }),
  })
  if (!res.ok) throw new Error(`Trade submission failed: ${res.statusText}`)
  return res.json()
}

export async function getTrades(): Promise<Trade[]> {
  const res = await fetch(`${API_BASE}/trades`)
  if (!res.ok) throw new Error(`Failed to fetch trades: ${res.statusText}`)
  return res.json()
}

export async function getPositions(): Promise<Position[]> {
  const res = await fetch(`${API_BASE}/positions`)
  if (!res.ok) throw new Error(`Failed to fetch positions: ${res.statusText}`)
  return res.json()
}

export async function getTrade(tradeId: string): Promise<Trade> {
  const res = await fetch(`${API_BASE}/trades/${tradeId}`)
  if (!res.ok) throw new Error(`Failed to fetch trade: ${res.statusText}`)
  return res.json()
}
