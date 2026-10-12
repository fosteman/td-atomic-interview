package com.atomicfx.settlement;

import java.math.BigDecimal;
import java.time.Instant;

public record SettlementResponse(
    String settlementId,
    String tradeId,
    String accountId,
    String pair,
    String side,
    BigDecimal notional,
    String settlementDate,
    BigDecimal settlementAmount,
    String status,
    String nostroAccount,
    Instant createdAt,
    Instant settledAt
) {
    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
            settlement.getSettlementId(),
            settlement.getTradeId(),
            settlement.getAccountId(),
            settlement.getPair(),
            settlement.getSide().name(),
            settlement.getNotional(),
            settlement.getSettlementDate(),
            settlement.getSettlementAmount(),
            settlement.getStatus().name(),
            settlement.getNostroAccount(),
            settlement.getCreatedAt(),
            settlement.getSettledAt()
        );
    }
}
