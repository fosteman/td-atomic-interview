package com.atomicfx.trade;

import java.math.BigDecimal;

public record SubmitTradeRequest(
    String pair,
    String side,
    BigDecimal notional,
    BigDecimal rate,
    String settlementDate,
    String clientOrderId
) {
}
