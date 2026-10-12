import { useState, useEffect, useReducer, useCallback } from 'react'
import { Trade, Position, Rate, submitTrade, getTrades, getPositions } from './api'
import { useWebSocket } from './useWebSocket'
import TradeForm from './components/TradeForm'
import RateDisplay from './components/RateDisplay'
import TradeList from './components/TradeList'
import Portfolio from './components/Portfolio'

interface AppState {
  trades: Trade[]
  positions: Position[]
  rates: Map<string, Rate>
  loading: boolean
  error: string | null
}

type Action =
  | { type: 'SET_TRADES'; payload: Trade[] }
  | { type: 'SET_POSITIONS'; payload: Position[] }
  | { type: 'UPDATE_TRADE'; payload: Trade }
  | { type: 'UPDATE_RATE'; payload: Rate }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'SET_LOADING'; payload: boolean }

function reducer(state: AppState, action: Action): AppState {
  switch (action.type) {
    case 'SET_TRADES':
      return { ...state, trades: action.payload }
    case 'SET_POSITIONS':
      return { ...state, positions: action.payload }
    case 'UPDATE_TRADE':
      return {
        ...state,
        trades: state.trades.map(t =>
          t.tradeId === action.payload.tradeId ? action.payload : t
        ),
      }
    case 'UPDATE_RATE':
      const newRates = new Map(state.rates)
      newRates.set(action.payload.pair, action.payload)
      return { ...state, rates: newRates }
    case 'SET_ERROR':
      return { ...state, error: action.payload }
    case 'SET_LOADING':
      return { ...state, loading: action.payload }
    default:
      return state
  }
}

function App() {
  const [state, dispatch] = useReducer(reducer, {
    trades: [],
    positions: [],
    rates: new Map(),
    loading: false,
    error: null,
  })

  const handleTradeUpdate = useCallback((trade: Trade) => {
    dispatch({ type: 'UPDATE_TRADE', payload: trade })
  }, [])

  const handleRateUpdate = useCallback((rate: Rate) => {
    dispatch({ type: 'UPDATE_RATE', payload: rate })
  }, [])

  const { connected } = useWebSocket(handleTradeUpdate, handleRateUpdate)

  useEffect(() => {
    const loadData = async () => {
      try {
        dispatch({ type: 'SET_LOADING', payload: true })
        const [trades, positions] = await Promise.all([getTrades(), getPositions()])
        dispatch({ type: 'SET_TRADES', payload: trades })
        dispatch({ type: 'SET_POSITIONS', payload: positions })
        dispatch({ type: 'SET_ERROR', payload: null })
      } catch (e) {
        dispatch({
          type: 'SET_ERROR',
          payload: e instanceof Error ? e.message : 'Unknown error',
        })
      } finally {
        dispatch({ type: 'SET_LOADING', payload: false })
      }
    }

    loadData()
    const interval = setInterval(loadData, 10000)
    return () => clearInterval(interval)
  }, [])

  const handleSubmitTrade = async (
    pair: string,
    side: 'BUY' | 'SELL',
    notional: number,
    rate: number,
    settlementDate: string
  ) => {
    try {
      dispatch({ type: 'SET_ERROR', payload: null })
      const newTrade = await submitTrade(pair, side, notional, rate, settlementDate)
      dispatch({ type: 'UPDATE_TRADE', payload: newTrade })
    } catch (e) {
      dispatch({
        type: 'SET_ERROR',
        payload: e instanceof Error ? e.message : 'Trade submission failed',
      })
    }
  }

  const rates = Array.from(state.rates.values())

  return (
    <div className="container">
      <h1>
        Atomic FX Trading{' '}
        <span
          id="ws-status"
          className={connected ? 'ws-connected' : 'ws-disconnected'}
        >
          {connected ? '🟢 Connected' : '🔴 Disconnected'}
        </span>
      </h1>

      {state.error && <div className="error">❌ {state.error}</div>}

      <div className="grid">
        <div className="panel">
          <h2>Rate Display</h2>
          <RateDisplay rates={rates} />
        </div>
        <div className="panel">
          <h2>Submit Trade</h2>
          <TradeForm onSubmit={handleSubmitTrade} rates={rates} />
        </div>
      </div>

      <div className="grid">
        <div className="panel">
          <h2>Portfolio</h2>
          <Portfolio positions={state.positions} loading={state.loading} />
        </div>
        <div className="panel">
          <h2>Trade History</h2>
          <TradeList trades={state.trades} loading={state.loading} />
        </div>
      </div>
    </div>
  )
}

export default App
