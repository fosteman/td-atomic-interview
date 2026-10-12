import { useEffect, useState, useCallback } from 'react'
import { Trade, Rate } from './api'

interface WebSocketMessage {
  type: 'trade.update' | 'rate.update'
  data: Trade | Rate
}

export function useWebSocket(
  onTradeUpdate: (trade: Trade) => void,
  onRateUpdate: (rate: Rate) => void
) {
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/trades`

    let ws: WebSocket | null = null
    let reconnectTimeout: NodeJS.Timeout | null = null

    const connect = () => {
      ws = new WebSocket(wsUrl)

      ws.onopen = () => {
        setConnected(true)
      }

      ws.onmessage = (event) => {
        try {
          const msg: WebSocketMessage = JSON.parse(event.data)
          if (msg.type === 'trade.update') {
            onTradeUpdate(msg.data as Trade)
          } else if (msg.type === 'rate.update') {
            onRateUpdate(msg.data as Rate)
          }
        } catch (e) {
          console.error('Failed to parse WS message:', e)
        }
      }

      ws.onerror = () => setConnected(false)

      ws.onclose = () => {
        setConnected(false)
        reconnectTimeout = setTimeout(connect, 3000)
      }
    }

    connect()

    return () => {
      if (reconnectTimeout) clearTimeout(reconnectTimeout)
      if (ws) ws.close()
    }
  }, [onTradeUpdate, onRateUpdate])

  return { connected }
}
