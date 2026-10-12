package com.atomicfx.settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, String> {
    Optional<Trade> findByTradeId(String tradeId);
}
