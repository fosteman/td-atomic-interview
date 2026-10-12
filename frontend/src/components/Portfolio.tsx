import { Position } from '../api'

interface Props {
  positions: Position[]
  loading: boolean
}

export default function Portfolio({ positions, loading }: Props) {
  if (loading) return <div>Loading...</div>

  if (positions.length === 0)
    return <div style={{ color: '#999' }}>No positions</div>

  return (
    <table>
      <thead>
        <tr>
          <th>Currency</th>
          <th>Balance</th>
        </tr>
      </thead>
      <tbody>
        {positions.map(p => (
          <tr key={p.currency}>
            <td>{p.currency}</td>
            <td style={{ color: p.balance >= 0 ? '#90ee90' : '#ff6b6b' }}>
              {p.balance.toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
