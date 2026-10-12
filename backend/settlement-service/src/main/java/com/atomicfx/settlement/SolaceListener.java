package com.atomicfx.settlement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * SolaceListener processes Solace events.
 *
 * In production: @JmsListener(destination = "trading/fx/trade/submitted")
 * For this exercise: Called via webhook from Order API
 *
 * Responsibilities:
 * 1. Receive trade.submitted events
 * 2. Create settlement records (idempotent)
 * 3. Check for settlements ready to settle (daily task)
 */
@Component
public class SolaceListener {
    private static final Logger log = LoggerFactory.getLogger(SolaceListener.class);
    private final SettlementService settlementService;

    public SolaceListener(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    /**
     * Real event handler: receives trade.submitted event and creates settlement.
     * In production, Spring's @JmsListener would invoke this automatically.
     * For now, it's called via webhook from Order API.
     */
    public void onTradeSubmitted(TradeSubmittedEvent event) {
        try {
            log.info("Processing trade.submitted: tradeId={}", event.tradeId());
            settlementService.processTradeSubmitted(event);
            log.info("Settlement created for trade: {}", event.tradeId());
        } catch (Exception e) {
            log.error("Failed to process trade event: {}", event.tradeId(), e);
            // In production: publish to dead-letter queue for manual review
            throw new RuntimeException("Settlement processing failed", e);
        }
    }

    /**
     * Daily task: check for settlements that reached their settlement date.
     * Mark them as SETTLED (in production, after confirming with the bank).
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 5000) // Every minute
    public void checkSettlementDates() {
        log.debug("Checking for settlements due today...");
        // TODO: query settlements WHERE settlement_date <= today AND status = INSTRUCTED
        // TODO: mark them as SETTLED after bank confirmation
    }
}
