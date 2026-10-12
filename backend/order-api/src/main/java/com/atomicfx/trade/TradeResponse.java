package com.atomicfx.trade;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeResponse(
    String tradeId,
    String accountId,
    String pair,
    String side,
    BigDecimal notional,
    BigDecimal rate,
    String status,
    String settlementDate,
    Instant createdAt
) {
    public static TradeResponse from(Trade trade) {
        return new TradeResponse(
            trade.getTradeId(),
            trade.getAccountId(),
            trade.getPair(),
            trade.getSide().name(),
            trade.getNotional(),
            trade.getRate(),
            trade.getStatus().name(),
            trade.getSettlementDate(),
            trade.getCreatedAt()
        );
    }
}
