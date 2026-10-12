package com.atomicfx.trade;

import com.atomicfx.outbox.OutboxEvent;
import com.atomicfx.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TradeService {
    private static final Logger log = LoggerFactory.getLogger(TradeService.class);
    private final TradeRepository tradeRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public TradeService(TradeRepository tradeRepository,
                       OutboxRepository outboxRepository,
                       ObjectMapper objectMapper) {
        this.tradeRepository = tradeRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Submit a new trade. This method demonstrates the transactional outbox pattern:
     * 1. Validate the trade
     * 2. Create and persist the trade
     * 3. Create and persist an outbox event in THE SAME transaction
     *
     * The outbox event will be published to Solace by a separate scheduled publisher.
     * This ensures durability: if the API crashes after returning 200, the event is
     * still safely in the database and will be published on recovery.
     */
    @Transactional
    public TradeResponse submitTrade(String accountId, SubmitTradeRequest request) {
        // Step 1: Validate request
        validateTradeRequest(request);

        // Step 2: Check for duplicate (idempotency)
        if (request.clientOrderId() != null) {
            Optional<Trade> existing = tradeRepository
                .findByAccountIdAndClientOrderId(accountId, request.clientOrderId());
            if (existing.isPresent()) {
                log.info("Duplicate trade detected: {} / {}", accountId, request.clientOrderId());
                return TradeResponse.from(existing.get());
            }
        }

        // Step 3: Create the trade (status = SUBMITTED)
        Trade trade = new Trade(
            accountId,
            request.pair(),
            Trade.TradeSide.valueOf(request.side()),
            request.notional(),
            request.rate(),
            request.settlementDate()
        );
        if (request.clientOrderId() != null) {
            trade.setClientOrderId(request.clientOrderId());
        }

        // Step 4: Save the trade
        Trade savedTrade = tradeRepository.save(trade);
        log.info("Trade created: {} status=SUBMITTED", savedTrade.getTradeId());

        // Step 5: Create and save the outbox event IN THE SAME TRANSACTION
        OutboxEvent outboxEvent = new OutboxEvent(
            savedTrade.getTradeId(),
            "trading.fx.trade.submitted",
            serializeTradeEvent(savedTrade)
        );
        outboxRepository.save(outboxEvent);
        log.info("Outbox event created: {}", outboxEvent.getEventId());

        // Transaction commits here. If it succeeds, both trade AND event are persisted.
        // If it fails, BOTH are rolled back. Neither happens partially.
        return TradeResponse.from(savedTrade);
    }

    /**
     * Get all trades for an account. Called by React to display history.
     */
    public List<TradeResponse> getTradesByAccount(String accountId) {
        return tradeRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
            .stream()
            .map(TradeResponse::from)
            .toList();
    }

    /**
     * Get a single trade by ID.
     */
    public Optional<TradeResponse> getTrade(String tradeId) {
        return tradeRepository.findById(tradeId)
            .map(TradeResponse::from);
    }

    /**
     * Update trade status (called by consumers after validation/acceptance).
     */
    @Transactional
    public void updateTradeStatus(String tradeId, Trade.TradeStatus newStatus) {
        Trade trade = tradeRepository.findById(tradeId)
            .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + tradeId));

        // Validate state transition
        if (!isValidTransition(trade.getStatus(), newStatus)) {
            throw new IllegalArgumentException(
                String.format("Invalid state transition: %s -> %s",
                    trade.getStatus(), newStatus)
            );
        }

        trade.setStatus(newStatus);
        tradeRepository.save(trade);
        log.info("Trade status updated: {} -> {}", tradeId, newStatus);

        // Publish an event for this status change
        OutboxEvent event = new OutboxEvent(
            tradeId,
            "trading.fx.trade." + newStatus.name().toLowerCase(),
            serializeTradeEvent(trade)
        );
        outboxRepository.save(event);
    }

    /**
     * Serialize a trade to JSON for the outbox event payload.
     */
    private String serializeTradeEvent(Trade trade) {
        try {
            Map<String, Object> event = Map.of(
                "tradeId", trade.getTradeId(),
                "accountId", trade.getAccountId(),
                "pair", trade.getPair(),
                "side", trade.getSide().name(),
                "notional", trade.getNotional(),
                "rate", trade.getRate(),
                "status", trade.getStatus().name(),
                "settlementDate", trade.getSettlementDate(),
                "createdAt", trade.getCreatedAt()
            );
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize trade event", e);
        }
    }

    /**
     * Validate the trade submission request.
     */
    private void validateTradeRequest(SubmitTradeRequest request) {
        if (request.pair() == null || request.pair().isBlank()) {
            throw new IllegalArgumentException("Pair is required");
        }
        if (request.side() == null || request.side().isBlank()) {
            throw new IllegalArgumentException("Side is required");
        }
        if (request.notional() == null || request.notional().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Notional must be > 0");
        }
        if (request.rate() == null || request.rate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Rate must be > 0");
        }
    }

    /**
     * Define which state transitions are allowed. Prevents out-of-order updates.
     */
    private boolean isValidTransition(Trade.TradeStatus current, Trade.TradeStatus next) {
        return switch (current) {
            case SUBMITTED -> next == Trade.TradeStatus.ACCEPTED || next == Trade.TradeStatus.REJECTED;
            case ACCEPTED -> next == Trade.TradeStatus.ROUTED || next == Trade.TradeStatus.REJECTED;
            case ROUTED -> next == Trade.TradeStatus.PARTIALLY_FILLED || next == Trade.TradeStatus.FILLED
                || next == Trade.TradeStatus.REJECTED;
            case PARTIALLY_FILLED -> next == Trade.TradeStatus.PARTIALLY_FILLED || next == Trade.TradeStatus.FILLED;
            case FILLED, REJECTED, SETTLED -> false; // Terminal states
        };
    }
}
