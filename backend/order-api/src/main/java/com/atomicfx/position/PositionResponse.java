package com.atomicfx.position;

import java.math.BigDecimal;

public record PositionResponse(
    String currency,
    BigDecimal balance
) {
    public static PositionResponse from(Position position) {
        return new PositionResponse(position.getCurrency(), position.getBalance());
    }
}
