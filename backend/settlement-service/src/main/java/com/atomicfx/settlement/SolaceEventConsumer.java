package com.atomicfx.settlement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * SolaceEventConsumer polls Solace REST API for events on the trade.submitted topic.
 *
 * How it works:
 * 1. Solace's REST API allows consuming messages from topics/queues
 * 2. We poll every 2 seconds
 * 3. For each message received, we deserialize and process it
 * 4. In production, this would be replaced with a proper JMS listener
 *
 * Solace REST Messaging API:
 * GET /api/v2/messagingServices/default/topics/{topicName}/messages
 */
@Component
public class SolaceEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(SolaceEventConsumer.class);
    private static final String SOLACE_BASE_URL = "http://localhost:8080";
    private static final String TRADE_TOPIC = "trading.fx.trade.submitted";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SolaceListener solaceListener;

    public SolaceEventConsumer(RestTemplate restTemplate, ObjectMapper objectMapper, SolaceListener solaceListener) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.solaceListener = solaceListener;
    }

    // Disabled - using native JMS in SolaceListener instead
    // Kept for backward webhook compatibility only

    /**
     * Webhook endpoint for receiving trade events from Order API.
     * Order API posts trade.submitted events here after publishing to Solace.
     * This creates a synchronous notification path alongside async Solace path.
     */
    public void receiveTradeSubmittedWebhook(TradeSubmittedEvent event) {
        log.info("Received trade.submitted webhook: tradeId={}", event.tradeId());
        solaceListener.onTradeSubmitted(event);
    }
}
