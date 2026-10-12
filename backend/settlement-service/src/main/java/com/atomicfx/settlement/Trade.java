package com.atomicfx.settlement;

import jakarta.persistence.*;

@Entity
@Table(name = "trades")
public class Trade {
    @Id
    @Column(name = "trade_id")
    private String tradeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TradeStatus status;

    public Trade() {
    }

    public Trade(String tradeId, TradeStatus status) {
        this.tradeId = tradeId;
        this.status = status;
    }

    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }

    public TradeStatus getStatus() { return status; }
    public void setStatus(TradeStatus status) { this.status = status; }

    public enum TradeStatus {
        SUBMITTED, ACCEPTED, ROUTED, PARTIALLY_FILLED, FILLED, REJECTED, SETTLED
    }
}
