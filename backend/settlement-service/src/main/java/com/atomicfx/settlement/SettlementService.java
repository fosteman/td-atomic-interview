package com.atomicfx.settlement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * SettlementService handles the settlement workflow:
 * 1. Receive trade.submitted events
 * 2. Create a settlement record
 * 3. Calculate settlement details (amount, date, nostro routing)
 * 4. Publish settlement.instructed event
 * 5. On settlement date, execute the settlement
 *
 * Settlement is the actual exchange of currency. In FX:
 * - T (trade date) = today
 * - T+2 (settlement date) = 2 business days later
 * - On T+2, currency is exchanged via nostro/vostro banks
 */
@Service
public class SettlementService {
    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);
    private final SettlementRepository settlementRepository;
    private final RestTemplate restTemplate;

    public SettlementService(SettlementRepository settlementRepository, RestTemplate restTemplate) {
        this.settlementRepository = settlementRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Handle incoming trade.submitted event.
     * Create a settlement record for the trade.
     */
    @Transactional
    public void processTradeSubmitted(TradeSubmittedEvent event) {
        log.info("Processing trade submission: tradeId={}", event.tradeId());

        // Check if settlement already exists (idempotency)
        if (settlementRepository.findByTradeId(event.tradeId()).isPresent()) {
            log.info("Settlement already exists for trade: {}", event.tradeId());
            return;
        }

        // Calculate settlement amount
        BigDecimal settlementAmount = event.notional().multiply(event.rate());

        // Create settlement
        Settlement settlement = new Settlement(
            event.tradeId(),
            event.accountId(),
            event.pair(),
            Settlement.TradeSide.valueOf(event.side()),
            event.notional(),
            event.settlementDate(),
            settlementAmount
        );

        // Route to nostro account (simplified: based on currency)
        settlement.setNostroAccount(routeToNostro(event.pair(), event.side()));

        Settlement savedSettlement = settlementRepository.save(settlement);
        log.info("Settlement created: settlementId={} status=PENDING", savedSettlement.getSettlementId());

        // Publish settlement.instructed event (would go to outbox in real system)
        publishSettlementInstructed(savedSettlement);
    }

    /**
     * Route a trade to the appropriate nostro/vostro account.
     * In reality, this queries a routing table based on currency, counterparty, etc.
     * For now, simplified routing.
     */
    private String routeToNostro(String pair, String side) {
        // Example: EUR/USD, BUY → we receive EUR, send USD via USD nostro
        String[] currencies = pair.split("/");
        String baseCurrency = currencies[0];
        String quoteCurrency = currencies[1];

        if ("BUY".equals(side)) {
            // Buying base currency, paying quote currency
            return "nostro-" + quoteCurrency.toLowerCase();
        } else {
            // Selling base currency, receiving quote currency
            return "nostro-" + baseCurrency.toLowerCase();
        }
    }

    /**
     * Publish settlement.instructed event.
     * In production, this would use the outbox pattern (insert to DB, async publisher).
     * For now, we mock it.
     */
    private void publishSettlementInstructed(Settlement settlement) {
        log.info("Publishing settlement.instructed: settlementId={}", settlement.getSettlementId());
        // In a real system:
        // 1. Insert OutboxEvent into database (same transaction as settlement save)
        // 2. Separate publisher polls and publishes to Solace
        //
        // For now, just log that it would be published.
    }

    /**
     * Get settlement by trade ID
     */
    public Settlement getSettlement(String tradeId) {
        return settlementRepository.findByTradeId(tradeId)
            .orElseThrow(() -> new IllegalArgumentException("Settlement not found for trade: " + tradeId));
    }

    /**
     * Mark settlement as instructed (sent to bank)
     */
    @Transactional
    public void markInstructed(String settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
            .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + settlementId));

        if (settlement.getStatus() != Settlement.SettlementStatus.PENDING) {
            throw new IllegalStateException("Can only instruct PENDING settlements");
        }

        settlement.setStatus(Settlement.SettlementStatus.INSTRUCTED);
        settlementRepository.save(settlement);
        log.info("Settlement instructed: {} status=INSTRUCTED", settlementId);
    }

    /**
     * Mark settlement as settled (currency exchanged)
     * Called on or after settlement date
     */
    @Transactional
    public void markSettled(String settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
            .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + settlementId));

        if (settlement.getStatus() != Settlement.SettlementStatus.INSTRUCTED) {
            throw new IllegalStateException("Can only settle INSTRUCTED settlements");
        }

        settlement.setStatus(Settlement.SettlementStatus.SETTLED);
        settlement.setSettledAt(Instant.now());
        settlementRepository.save(settlement);
        log.info("Settlement completed: {} status=SETTLED", settlementId);

        // Publish settlement.settled event
        publishSettlementSettled(settlement);
    }

    private void publishSettlementSettled(Settlement settlement) {
        log.info("Publishing settlement.settled: settlementId={}", settlement.getSettlementId());
    }
}
