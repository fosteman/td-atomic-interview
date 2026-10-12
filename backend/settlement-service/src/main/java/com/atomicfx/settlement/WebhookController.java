package com.atomicfx.settlement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * WebhookController receives events from Order API.
 *
 * Order API publishes trade.submitted events to Solace, then POSTs to this webhook
 * so Settlement Service can immediately process the trade.
 *
 * This creates two pathways:
 * 1. Async via Solace (for distributed systems, fan-out to multiple consumers)
 * 2. Sync via webhook (for immediate processing in tightly coupled services)
 *
 * In production, use only the Solace pathway (pub/sub, not webhooks).
 */
@RestController
@RequestMapping("/webhooks")
public class WebhookController {
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final SolaceListener solaceListener;
    private final ObjectMapper objectMapper;

    public WebhookController(SolaceListener solaceListener, ObjectMapper objectMapper) {
        this.solaceListener = solaceListener;
        this.objectMapper = objectMapper;
    }

    /**
     * POST /webhooks/trade-submitted
     * Receives a trade.submitted event from Order API (as JSON string)
     */
    @PostMapping("/trade-submitted")
    public ResponseEntity<Map<String, String>> onTradeSubmitted(@RequestBody String payload) {
        try {
            log.info("Webhook: trade.submitted received. Payload: {}", payload);

            // Deserialize the JSON payload to TradeSubmittedEvent
            TradeSubmittedEvent event = objectMapper.readValue(payload, TradeSubmittedEvent.class);

            log.info("Deserialized event: tradeId={}", event.tradeId());
            solaceListener.onTradeSubmitted(event);

            Map<String, String> response = new HashMap<>();
            response.put("status", "processed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to process webhook event: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
