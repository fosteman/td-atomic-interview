package com.atomicfx.trade;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trades", indexes = {
    @Index(name = "idx_account_id", columnList = "account_id"),
    @Index(name = "idx_status", columnList = "status")
})
public class Trade {
    @Id
    @Column(name = "trade_id")
    private String tradeId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "client_order_id")
    private String clientOrderId;

    @Column(name = "pair", nullable = false)
    private String pair;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false)
    private TradeSide side;

    @Column(name = "notional", nullable = false)
    private BigDecimal notional;

    @Column(name = "rate", nullable = false)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TradeStatus status = TradeStatus.SUBMITTED;

    @Column(name = "settlement_date", nullable = false)
    private String settlementDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Version
    private Long version;

    public Trade() {
    }

    public Trade(String accountId, String pair, TradeSide side,
                 BigDecimal notional, BigDecimal rate, String settlementDate) {
        this.tradeId = UUID.randomUUID().toString();
        this.accountId = accountId;
        this.pair = pair;
        this.side = side;
        this.notional = notional;
        this.rate = rate;
        this.settlementDate = settlementDate;
        this.status = TradeStatus.SUBMITTED;
        this.createdAt = Instant.now();
    }

    // Getters & Setters
    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }

    public String getAccountId() { return accountId; }
    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }

    public String getPair() { return pair; }
    public TradeSide getSide() { return side; }
    public BigDecimal getNotional() { return notional; }
    public BigDecimal getRate() { return rate; }
    public TradeStatus getStatus() { return status; }
    public void setStatus(TradeStatus status) { this.status = status; }

    public String getSettlementDate() { return settlementDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }

    public enum TradeSide {
        BUY, SELL
    }

    public enum TradeStatus {
        SUBMITTED, ACCEPTED, ROUTED, PARTIALLY_FILLED, FILLED, REJECTED, SETTLED
    }
}
