package com.atomicfx.settlement;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Settlement represents a pending or completed trade settlement.
 * Settlement is the process of exchanging actual currency on the settlement date.
 *
 * Workflow:
 * PENDING → INSTRUCTED → SETTLED (or FAILED)
 */
@Entity
@Table(name = "settlements", indexes = {
    @Index(name = "idx_trade_id", columnList = "trade_id"),
    @Index(name = "idx_status", columnList = "status")
})
public class Settlement {
    @Id
    @Column(name = "settlement_id")
    private String settlementId;

    @Column(name = "trade_id", nullable = false, unique = true)
    private String tradeId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "pair", nullable = false)
    private String pair;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false)
    private TradeSide side;

    @Column(name = "notional", nullable = false)
    private BigDecimal notional;

    @Column(name = "settlement_date", nullable = false)
    private String settlementDate;

    @Column(name = "settlement_amount", nullable = false)
    private BigDecimal settlementAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "nostro_account")
    private String nostroAccount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "settled_at")
    private Instant settledAt;

    public Settlement() {
    }

    public Settlement(String tradeId, String accountId, String pair, TradeSide side,
                     BigDecimal notional, String settlementDate, BigDecimal settlementAmount) {
        this.settlementId = UUID.randomUUID().toString();
        this.tradeId = tradeId;
        this.accountId = accountId;
        this.pair = pair;
        this.side = side;
        this.notional = notional;
        this.settlementDate = settlementDate;
        this.settlementAmount = settlementAmount;
        this.status = SettlementStatus.PENDING;
        this.createdAt = Instant.now();
    }

    // Getters
    public String getSettlementId() { return settlementId; }
    public String getTradeId() { return tradeId; }
    public String getAccountId() { return accountId; }
    public String getPair() { return pair; }
    public TradeSide getSide() { return side; }
    public BigDecimal getNotional() { return notional; }
    public String getSettlementDate() { return settlementDate; }
    public BigDecimal getSettlementAmount() { return settlementAmount; }
    public SettlementStatus getStatus() { return status; }
    public void setStatus(SettlementStatus status) { this.status = status; }
    public String getNostroAccount() { return nostroAccount; }
    public void setNostroAccount(String nostroAccount) { this.nostroAccount = nostroAccount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSettledAt() { return settledAt; }
    public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }

    public enum TradeSide {
        BUY, SELL
    }

    public enum SettlementStatus {
        PENDING,        // Created, waiting for settlement date
        INSTRUCTED,     // Sent to nostro/vostro bank
        SETTLED,        // Currency exchanged
        FAILED          // Settlement failed
    }
}
