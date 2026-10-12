package com.atomicfx.position;

import jakarta.persistence.*; // jakartaEE a java spec, defines apis.
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "positions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"account_id", "currency"})
})
public class Position {
    @Id
    @Column(name = "position_id")
    private String positionId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Position() {
    }

    // Getters
    public String getPositionId() { return positionId; }
    public String getAccountId() { return accountId; }
    public String getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public Instant getUpdatedAt() { return updatedAt; }
}
