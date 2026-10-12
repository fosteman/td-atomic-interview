package com.atomicfx.trade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trades")
@CrossOrigin(origins = "http://localhost:5173")
public class TradeController {
    private static final Logger log = LoggerFactory.getLogger(TradeController.class);
    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    /**
     * POST /api/trades - Submit a new trade
     *
     * Request body: {
     *   "pair": "EUR/USD",
     *   "side": "BUY",
     *   "notional": 1000000,
     *   "rate": 1.0950,
     *   "settlementDate": "2026-10-14",
     *   "clientOrderId": "optional-dedup-key"
     * }
     *
     * Response: 201 Created with the trade object and immediate status = SUBMITTED.
     * The trade has been durable persisted. Asynchronous processing will begin immediately.
     */
    @PostMapping
    public ResponseEntity<TradeResponse> submitTrade(@RequestBody SubmitTradeRequest request) {
        try {
            // Extract account ID from request header or security context.
            // For now, use a mock account ID.
            String accountId = "account-123";
            log.info("Submitting trade: pair={} side={} notional={}",
                request.pair(), request.side(), request.notional());

            TradeResponse trade = tradeService.submitTrade(accountId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(trade);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid trade request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to submit trade", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/trades/{tradeId} - Get a single trade
     */
    @GetMapping("/{tradeId}")
    public ResponseEntity<TradeResponse> getTrade(@PathVariable String tradeId) {
        return tradeService.getTrade(tradeId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/trades - Get all trades for the account
     */
    @GetMapping
    public ResponseEntity<List<TradeResponse>> getTrades() {
        String accountId = "account-123"; // Mock
        List<TradeResponse> trades = tradeService.getTradesByAccount(accountId);
        return ResponseEntity.ok(trades);
    }

    /**
     * Error handler for validation errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationError(IllegalArgumentException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
}
