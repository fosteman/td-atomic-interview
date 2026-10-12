package com.atomicfx.trade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, String> {
    List<Trade> findByAccountIdOrderByCreatedAtDesc(String accountId);
    Optional<Trade> findByAccountIdAndClientOrderId(String accountId, String clientOrderId);
}
