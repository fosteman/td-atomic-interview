import { Trade } from '../api'

interface Props {
  trades: Trade[]
  loading: boolean
}

export default function TradeList({ trades, loading }: Props) {
  if (loading) return <div>Loading...</div>

  if (trades.length === 0)
    return <div style={{ color: '#999' }}>No trades yet</div>

  return (
    <table>
      <thead>
        <tr>
          <th>Trade ID</th>
          <th>Pair</th>
          <th>Side</th>
          <th>Notional</th>
          <th>Rate</th>
          <th>Status</th>
          <th>Settlement</th>
        </tr>
      </thead>
      <tbody>
        {trades.map(t => (
          <tr key={t.tradeId}>
            <td style={{ fontSize: '11px' }}>{t.tradeId.slice(0, 8)}</td>
            <td>{t.pair}</td>
            <td style={{ color: t.side === 'BUY' ? '#90ee90' : '#ff6b6b' }}>
              {t.side}
            </td>
            <td>{t.notional.toLocaleString()}</td>
            <td>{t.rate.toFixed(4)}</td>
            <td className={`status-${t.status.toLowerCase()}`}>{t.status}</td>
            <td>{new Date(t.settlementDate).toLocaleDateString()}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
