import { Rate } from '../api'

interface Props {
  rates: Rate[]
}

export default function RateDisplay({ rates }: Props) {
  const defaultPairs = ['EUR/USD', 'GBP/USD', 'USD/JPY', 'AUD/USD']
  const displayedRates = rates.length > 0 ? rates : defaultPairs.map(pair => ({
    pair,
    bid: 1.0,
    ask: 1.01,
    timestamp: Date.now(),
  }))

  return (
    <div>
      {displayedRates.map(r => (
        <div key={r.pair} style={{ marginBottom: '15px' }}>
          <div style={{ marginBottom: '5px' }}>{r.pair}</div>
          <div className="rate">
            Bid: {r.bid.toFixed(4)} | Ask: {r.ask.toFixed(4)}
          </div>
          <div style={{ fontSize: '11px', color: '#999' }}>
            Spread: {(r.ask - r.bid).toFixed(4)} pips
          </div>
        </div>
      ))}
    </div>
  )
}
