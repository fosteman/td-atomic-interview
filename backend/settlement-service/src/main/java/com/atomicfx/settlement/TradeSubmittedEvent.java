package com.atomicfx.settlement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event received from Solace: trading.fx.trade.submitted
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TradeSubmittedEvent(
    @JsonProperty("tradeId") String tradeId,
    @JsonProperty("accountId") String accountId,
    @JsonProperty("pair") String pair,
    @JsonProperty("side") String side,
    @JsonProperty("notional") BigDecimal notional,
    @JsonProperty("rate") BigDecimal rate,
    @JsonProperty("settlementDate") String settlementDate,
    @JsonProperty("createdAt") Instant createdAt
) {
}
