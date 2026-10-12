import { useState } from 'react'
import { Rate } from '../api'

interface Props {
  onSubmit: (
    pair: string,
    side: 'BUY' | 'SELL',
    notional: number,
    rate: number,
    settlementDate: string
  ) => Promise<void>
  rates: Rate[]
}

export default function TradeForm({ onSubmit, rates }: Props) {
  const [pair, setPair] = useState('EUR/USD')
  const [side, setSide] = useState<'BUY' | 'SELL'>('BUY')
  const [notional, setNotional] = useState('1000000')
  const [settlementDate, setSettlementDate] = useState(
    new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  )
  const [loading, setLoading] = useState(false)

  const currentRate = rates.find(r => r.pair === pair)
  const rate = side === 'BUY' ? currentRate?.ask || 1.0 : currentRate?.bid || 1.0

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      await onSubmit(pair, side, parseFloat(notional), rate, settlementDate)
      setNotional('1000000')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <div>
        <label>
          Pair
          <select value={pair} onChange={e => setPair(e.target.value)}>
            <option>EUR/USD</option>
            <option>GBP/USD</option>
            <option>USD/JPY</option>
            <option>AUD/USD</option>
          </select>
        </label>
      </div>

      <div>
        <label>
          Side
          <select
            value={side}
            onChange={e => setSide(e.target.value as 'BUY' | 'SELL')}
          >
            <option value="BUY">BUY</option>
            <option value="SELL">SELL</option>
          </select>
        </label>
      </div>

      <div>
        <label>
          Notional (base currency units)
          <input
            type="number"
            value={notional}
            onChange={e => setNotional(e.target.value)}
            min="1000"
          />
        </label>
      </div>

      <div>
        <label>
          Settlement Date (T+2)
          <input
            type="date"
            value={settlementDate}
            onChange={e => setSettlementDate(e.target.value)}
          />
        </label>
      </div>

      <div style={{ marginTop: '10px' }}>
        <div style={{ fontSize: '12px', color: '#999' }}>
          Rate: {rate.toFixed(4)} | Amount: {(parseFloat(notional) * rate).toFixed(2)}
        </div>
      </div>

      <button type="submit" disabled={loading} style={{ width: '100%' }}>
        {loading ? 'Submitting...' : 'Submit Trade'}
      </button>
    </form>
  )
}
