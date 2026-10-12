package com.atomicfx.settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, String> {
    Optional<Settlement> findByTradeId(String tradeId);
    List<Settlement> findByStatus(Settlement.SettlementStatus status);
}
